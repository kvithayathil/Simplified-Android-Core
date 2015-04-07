package org.nypl.simplified.app;

import java.net.URI;

import com.google.common.collect.ImmutableList;
import com.io7m.jnull.NullCheck;

public final class CatalogFeedArgumentsRemote implements
  CatalogFeedArgumentsType
{
  private static final long                        serialVersionUID = 1L;

  private final boolean                            drawer_open;
  private final String                             title;
  private final ImmutableList<CatalogUpStackEntry> up_stack;
  private final URI                                uri;

  public CatalogFeedArgumentsRemote(
    final boolean in_drawer_open,
    final ImmutableList<CatalogUpStackEntry> in_up_stack,
    final String in_title,
    final URI in_uri)
  {
    this.drawer_open = in_drawer_open;
    this.up_stack = NullCheck.notNull(in_up_stack);
    this.title = NullCheck.notNull(in_title);
    this.uri = NullCheck.notNull(in_uri);
  }

  @Override public String getTitle()
  {
    return this.title;
  }

  public ImmutableList<CatalogUpStackEntry> getUpStack()
  {
    return this.up_stack;
  }

  public URI getURI()
  {
    return this.uri;
  }

  public boolean isDrawerOpen()
  {
    return this.drawer_open;
  }

  @Override public <A, E extends Exception> A matchArguments(
    final CatalogFeedArgumentsMatcherType<A, E> m)
    throws E
  {
    return m.onFeedArgumentsRemote(this);
  }
}
