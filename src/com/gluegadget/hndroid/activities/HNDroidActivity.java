package com.gluegadget.hndroid.activities;

import android.app.Activity;
import android.app.ProgressDialog;

import com.gluegadget.hndroid.Application;
import com.gluegadget.hndroid.HackerNewsClient;

/**
 * A single superclass for all of our activities, except those which inherit
 * from Android classes other than Activity.
 */
public abstract class HNDroidActivity extends Activity {

  private ProgressDialog dialog = null;

  protected void showProgressDialog(final String message) {
    dialog = ProgressDialog.show(this, "", message, true);
  }

  protected void hideProgressDialog() {
    if (dialog != null)
      dialog.dismiss();
    dialog = null;
  }

  protected HackerNewsClient getClient() {
    return ((Application) getApplication()).getClient();
  }

}