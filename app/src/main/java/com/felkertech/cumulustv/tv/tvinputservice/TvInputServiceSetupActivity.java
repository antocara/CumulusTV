package com.felkertech.cumulustv.tv.tvinputservice;

import android.app.Activity;
import android.os.Bundle;

import com.felkertech.n.cumulustv.R;

public class TvInputServiceSetupActivity extends Activity {
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_setup);

    if (getActionBar() != null) {
      getActionBar().hide();
    }
  }
}