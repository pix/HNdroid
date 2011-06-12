package com.gluegadget.hndroid.activities;

import com.gluegadget.hndroid.R;
import com.gluegadget.hndroid.R.xml;

import android.os.Bundle;

public class PreferenceActivity extends android.preference.PreferenceActivity {

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
  }

}