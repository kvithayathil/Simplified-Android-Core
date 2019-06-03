package org.nypl.simplified.opds.auth_document

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.mime.MIMEParser
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocument
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentError
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParseResult
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParseResult.Failure
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParserType
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentWarning
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject
import org.nypl.simplified.opds.auth_document.api.AuthenticationObjectLink
import java.io.InputStream
import java.net.URI

class AuthenticationDocumentParsers : AuthenticationDocumentParsersType {

  private val mapper = ObjectMapper()

  override fun createParser(
    uri: URI,
    stream: InputStream,
    warningsAsErrors: Boolean
  ): AuthenticationDocumentParserType =
    Parser(this.mapper, uri, stream, warningsAsErrors)

  private class Parser(
    private val mapper: ObjectMapper,
    private val uri: URI,
    private val stream: InputStream,
    private val warningsAsErrors: Boolean
  ) : AuthenticationDocumentParserType {

    override fun close() {
      this.stream.close()
    }

    private val errors =
      mutableListOf<AuthenticationDocumentError>()
    private val warnings =
      mutableListOf<AuthenticationDocumentWarning>()

    private fun publishWarning(warning: AuthenticationDocumentWarning) {
      if (this.warningsAsErrors) {
        this.errors.add(AuthenticationDocumentError(warning.message, warning.exception))
      } else {
        this.warnings.add(warning)
      }
    }

    override fun parse(): AuthenticationDocumentParseResult<AuthenticationDocument> {
      return try {
        val tree = this.mapper.readTree(this.stream)
        if (tree == null) {
          this.errors.add(AuthenticationDocumentError("Document is empty"))
          return Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
        }

        val root =
          JSONParserUtilities.checkObject(null, tree)
        val id =
          JSONParserUtilities.getURI(root, "id")
        val title =
          JSONParserUtilities.getString(root, "title")
        val description =
          JSONParserUtilities.getStringOrNull(root, "description")
        val authentication =
          this.parseAuthentications(root)
        val links =
          this.parseLinks(root)

        if (this.errors.isEmpty()) {
          return AuthenticationDocumentParseResult.Success(
            warnings = this.warnings.toList(),
            result = AuthenticationDocument(
              id = id,
              title = title,
              description = description,
              links = links,
              authentication = authentication))
        } else {
          Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
        }
      } catch (e: Exception) {
        this.errors.add(AuthenticationDocumentError(e.message ?: "", e))
        Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
      }
    }

    private fun parseLinks(tree: ObjectNode): List<AuthenticationObjectLink> {
      if (!tree.has("links")) {
        return listOf()
      }

      val linksNodes = try {
        JSONParserUtilities.getArray(tree, "links")
      } catch (e: Exception) {
        this.errors.add(AuthenticationDocumentError(e.message ?: "", e))
        this.mapper.createArrayNode()
      }

      return linksNodes.mapNotNull { node -> parseLink(node) }
    }

    private fun parseLink(node: JsonNode): AuthenticationObjectLink? {
      return try {
        val root =
          JSONParserUtilities.checkObject(null, node)
        val href =
          JSONParserUtilities.getURI(root, "href")
        val templated =
          JSONParserUtilities.getBooleanDefault(root, "templated", false)
        val mime =
          JSONParserUtilities.getStringOrNull(root, "type")
            ?.let { type -> MIMEParser.parseRaisingException(type) }
        val title =
          JSONParserUtilities.getStringOrNull(root, "title")
        val rel =
          JSONParserUtilities.getStringOrNull(root, "rel")
        val width =
          JSONParserUtilities.getStringOrNull(root, "width")
            ?.let { s -> Integer.parseInt(s, 10) }
        val height =
          JSONParserUtilities.getStringOrNull(root, "height")
            ?.let { s -> Integer.parseInt(s, 10) }
        val duration =
          JSONParserUtilities.getStringOrNull(root, "duration")?.toDouble()
        val bitrate =
          JSONParserUtilities.getStringOrNull(root, "bitrate")?.toDouble()

        AuthenticationObjectLink(
          href = href,
          templated = templated,
          type = mime,
          title = title,
          rel = rel,
          width = width,
          height = height,
          duration = duration,
          bitrate = bitrate)
      } catch (e: Exception) {
        this.errors.add(AuthenticationDocumentError(e.message ?: "", e))
        null
      }
    }

    private fun parseAuthentications(tree: ObjectNode): List<AuthenticationObject> {
      val authenticationNodes = try {
        JSONParserUtilities.getArray(tree, "authentication")
      } catch (e: Exception) {
        this.errors.add(AuthenticationDocumentError(e.message ?: "", e))
        this.mapper.createArrayNode()
      }

      return authenticationNodes.mapNotNull { node -> parseAuthentication(node) }
    }

    private fun parseAuthentication(node: JsonNode): AuthenticationObject? {
      return try {
        val root =
          JSONParserUtilities.checkObject(null, node)
        val type =
          JSONParserUtilities.getURI(root, "type")
        val links =
          JSONParserUtilities.getArrayOrNull(root, "links")
            ?.mapNotNull(this::parseLink)
            ?: listOf()
        val labels =
          JSONParserUtilities.getObjectOrNull(root, "labels")
            ?.let(this::parseLabels)
            ?: mapOf()
        AuthenticationObject(
          type = type,
          labels = labels,
          links = links)
      } catch (e: Exception) {
        this.errors.add(AuthenticationDocumentError(e.message ?: "", e))
        null
      }
    }

    private fun parseLabels(root: ObjectNode): Map<String, String>? {
      val values = mutableMapOf<String, String>()
      for (key in root.fieldNames()) {
        try {
          values[key] = JSONParserUtilities.getString(root, key)
        } catch (e: Exception) {
          this.errors.add(AuthenticationDocumentError(e.message ?: "", e))
          null
        }
      }
      return values.toMap()
    }
  }

}