package org.nypl.simplified.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.google.common.collect.ImmutableList;
import com.io7m.jfunctional.FunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

/**
 * The type of all activities in the app.
 */

@SuppressWarnings("boxing") public abstract class SimplifiedActivity extends
  Activity implements DrawerListener, OnItemClickListener
{
  private static int          activity_count;
  private static final String NAVIGATION_DRAWER_OPEN_ID;
  private static final String TAG;

  static {
    TAG = "Main";
    NAVIGATION_DRAWER_OPEN_ID =
      "org.nypl.simplified.app.SimplifiedActivity.drawer_open";
  }

  public static void setActivityArguments(
    final Bundle b,
    final boolean open_drawer)
  {
    NullCheck.notNull(b);
    b.putBoolean(SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID, open_drawer);
  }

  private @Nullable FrameLayout                             content_frame;
  private @Nullable DrawerLayout                            drawer;
  private @Nullable Map<String, FunctionType<Bundle, Unit>> drawer_arg_funcs;
  private @Nullable Map<String, Class<? extends Activity>>  drawer_classes;
  private @Nullable ArrayList<String>                       drawer_items;
  private @Nullable ListView                                drawer_list;
  private boolean                                           finishing;
  private int                                               selected;

  protected final FrameLayout getContentFrame()
  {
    return NullCheck.notNull(this.content_frame);
  }

  @Override public void onBackPressed()
  {
    Log.d(SimplifiedActivity.TAG, "onBackPressed: " + this);

    final DrawerLayout d = NullCheck.notNull(this.drawer);
    if (d.isDrawerOpen(GravityCompat.START)) {
      this.finishing = true;
      d.closeDrawer(GravityCompat.START);
    } else {
      this.finish();

      /**
       * If this activity is the last activity, do not override the closing
       * transition animation.
       */

      if (SimplifiedActivity.activity_count > 1) {
        this.overridePendingTransition(0, 0);
      }
    }
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    Log.d(SimplifiedActivity.TAG, "onCreate: " + this);
    this.setContentView(R.layout.main);

    boolean open_drawer = true;

    final Intent i = NullCheck.notNull(this.getIntent());
    Log.d(SimplifiedActivity.TAG, "non-null intent");
    final Bundle a = i.getExtras();
    if (a != null) {
      Log.d(SimplifiedActivity.TAG, "non-null intent extras");
      open_drawer =
        a.getBoolean(SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID);
      Log.d(SimplifiedActivity.TAG, "drawer requested: " + open_drawer);
    }

    if (state != null) {
      Log.d(SimplifiedActivity.TAG, "reinitializing");
      open_drawer =
        state.getBoolean(
          SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID,
          open_drawer);
    }

    final Simplified app = Simplified.get();
    final Resources rr = NullCheck.notNull(this.getResources());

    /**
     * Configure the navigation drawer.
     */

    final DrawerLayout d =
      NullCheck.notNull((DrawerLayout) this.findViewById(R.id.drawer_layout));
    final ListView dl =
      NullCheck.notNull((ListView) this.findViewById(R.id.left_drawer));
    final FrameLayout fl =
      NullCheck.notNull((FrameLayout) this.findViewById(R.id.content_frame));

    d.setDrawerListener(this);
    dl.setOnItemClickListener(this);

    final String app_name =
      NullCheck.notNull(rr.getString(R.string.app_name));
    final String catalog_name =
      NullCheck.notNull(rr.getString(R.string.catalog));
    final String holds_name = NullCheck.notNull(rr.getString(R.string.holds));
    final String books_name = NullCheck.notNull(rr.getString(R.string.books));
    final String settings_name =
      NullCheck.notNull(rr.getString(R.string.settings));

    final ArrayList<String> di = new ArrayList<String>();
    di.add(catalog_name);
    di.add(holds_name);
    di.add(books_name);
    di.add(settings_name);
    dl.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_item, di));

    /**
     * Set up a map of names to classes.
     */

    final Map<String, Class<? extends Activity>> dc =
      new HashMap<String, Class<? extends Activity>>();
    dc.put(books_name, CatalogFeedActivity.class);
    dc.put(catalog_name, CatalogFeedActivity.class);
    dc.put(holds_name, HoldsActivity.class);
    dc.put(settings_name, SettingsActivity.class);

    /**
     * Set up a map of part names to functions that configure argument
     * bundles.
     */

    final Map<String, FunctionType<Bundle, Unit>> da =
      new HashMap<String, FunctionType<Bundle, Unit>>();

    da.put(books_name, new FunctionType<Bundle, Unit>() {
      @Override public Unit call(
        final Bundle b)
      {
        final CatalogFeedArgumentsLocalBooks local =
          new CatalogFeedArgumentsLocalBooks(books_name);
        CatalogFeedActivity.setActivityArguments(b, local);
        return Unit.unit();
      }
    });

    da.put(catalog_name, new FunctionType<Bundle, Unit>() {
      @Override public Unit call(
        final Bundle b)
      {
        final ImmutableList<CatalogUpStackEntry> empty = ImmutableList.of();
        final CatalogFeedArgumentsRemote remote =
          new CatalogFeedArgumentsRemote(
            false,
            NullCheck.notNull(empty),
            app_name,
            app.getFeedInitialURI());
        CatalogFeedActivity.setActivityArguments(b, remote);
        return Unit.unit();
      }
    });

    da.put(holds_name, new FunctionType<Bundle, Unit>() {
      @Override public Unit call(
        final Bundle b)
      {
        SimplifiedActivity.setActivityArguments(b, false);
        return Unit.unit();
      }
    });

    da.put(settings_name, new FunctionType<Bundle, Unit>() {
      @Override public Unit call(
        final Bundle b)
      {
        SimplifiedActivity.setActivityArguments(b, false);
        return Unit.unit();
      }
    });

    /**
     * If the drawer should be open, open it.
     */

    if (open_drawer) {
      d.openDrawer(GravityCompat.START);
    }

    this.drawer_items = di;
    this.drawer_classes = dc;
    this.drawer_arg_funcs = da;
    this.drawer = d;
    this.drawer_list = dl;
    this.content_frame = fl;
    this.selected = -1;
    SimplifiedActivity.activity_count = SimplifiedActivity.activity_count + 1;
    Log.d(SimplifiedActivity.TAG, "activity count: "
      + SimplifiedActivity.activity_count);
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    Log.d(SimplifiedActivity.TAG, "onDestroy: " + this);
    SimplifiedActivity.activity_count = SimplifiedActivity.activity_count - 1;
  }

  @Override public final void onDrawerClosed(
    final @Nullable View drawerView)
  {
    Log.d(
      SimplifiedActivity.TAG,
      String.format("drawer closed, selected: %s", this.selected));

    /**
     * If the drawer is closing because the user pressed the back button, then
     * finish the activity.
     */

    if (this.finishing) {
      this.finish();
      return;
    }

    /**
     * If the drawer is closing because the user selected an entry, start the
     * relevant activity.
     */

    if (this.selected != -1) {
      final ArrayList<String> di = NullCheck.notNull(this.drawer_items);
      final Map<String, Class<? extends Activity>> dc =
        NullCheck.notNull(this.drawer_classes);
      final Map<String, FunctionType<Bundle, Unit>> fas =
        NullCheck.notNull(this.drawer_arg_funcs);

      final String name = NullCheck.notNull(di.get(this.selected));
      final Class<? extends Activity> c = NullCheck.notNull(dc.get(name));
      final FunctionType<Bundle, Unit> fa = NullCheck.notNull(fas.get(name));

      final Bundle b = new Bundle();
      SimplifiedActivity.setActivityArguments(b, false);
      fa.call(b);

      final Intent i = new Intent();
      i.setClass(this, c);
      i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      i.putExtras(b);
      this.startActivity(i);
    }

    this.selected = -1;
  }

  @Override public final void onDrawerOpened(
    final @Nullable View drawerView)
  {
    this.selected = -1;
  }

  @Override public final void onDrawerSlide(
    final @Nullable View drawerView,
    final float slideOffset)
  {
    // Nothing
  }

  @Override public final void onDrawerStateChanged(
    final int newState)
  {
    // Nothing
  }

  @Override public void onItemClick(
    final @Nullable AdapterView<?> parent,
    final @Nullable View view,
    final int position,
    final long id)
  {
    Log.d(
      SimplifiedActivity.TAG,
      String.format("selected navigation item %d", position));

    final DrawerLayout d = NullCheck.notNull(this.drawer);
    d.closeDrawer(GravityCompat.START);
    this.selected = position;
  }

  @Override protected void onSaveInstanceState(
    final @Nullable Bundle state)
  {
    super.onSaveInstanceState(state);
    assert state != null;
    final DrawerLayout d = NullCheck.notNull(this.drawer);
    state.putBoolean(
      SimplifiedActivity.NAVIGATION_DRAWER_OPEN_ID,
      d.isDrawerOpen(GravityCompat.START));
  }
}
