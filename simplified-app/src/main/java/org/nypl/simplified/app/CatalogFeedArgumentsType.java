package org.nypl.simplified.app;

import java.io.Serializable;

public interface CatalogFeedArgumentsType extends Serializable
{
  String getTitle();

  <A, E extends Exception> A matchArguments(
    CatalogFeedArgumentsMatcherType<A, E> m)
    throws E;
}
