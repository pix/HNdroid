package com.gluegadget.hndroid.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.gluegadget.hndroid.R;

public class KarmaWidgetConfigurationActivity extends Activity {

  private static final String PREFS_NAME = "com.gluegadget.hndroid.KarmaWidget";
  private static final String PREF_PREFIX_KEY = "prefix_";
  private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
  private EditText mAppWidgetUsername;

  public KarmaWidgetConfigurationActivity() {
    super();
  }

  @Override
  public void onCreate(final Bundle icicle) {
    super.onCreate(icicle);
    setResult(RESULT_CANCELED);
    setContentView(R.layout.karma_widget_configure);
    mAppWidgetUsername = (EditText) findViewById(R.id.username);
    findViewById(R.id.save_button).setOnClickListener(mOnClickListener);

    final Intent intent = getIntent();
    final Bundle extras = intent.getExtras();
    if (extras != null) {
      mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish();
    }
  }

  View.OnClickListener mOnClickListener = new View.OnClickListener() {
    @Override
    public void onClick(final View v) {
      final Context context = KarmaWidgetConfigurationActivity.this;

      final String username = mAppWidgetUsername.getText().toString();
      saveTitlePref(context, mAppWidgetId, username);
      final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
      KarmaWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

      final Intent resultValue = new Intent();
      resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
      setResult(RESULT_OK, resultValue);
      finish();
    }
  };

  static CharSequence loadUsername(final Context context, final int appWidgetId) {
    final SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
    final String username = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
    if (username != null)
      return username;
    else
      return "pg";
  }

  static void saveTitlePref(final Context context, final int appWidgetId, final String text) {
    final SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
    prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
    prefs.commit();
  }

  static void deleteUsername(final Context context, final int appWidgetId) {
  }
}
