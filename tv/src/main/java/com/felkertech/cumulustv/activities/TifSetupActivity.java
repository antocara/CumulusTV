package com.felkertech.cumulustv.activities;

import android.app.Activity;
import android.os.Bundle;
import com.felkertech.cumulustv.R;

/**
 * Created by guest1 on 12/23/2016.
 */
public class TifSetupActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        getActionBar().hide();
    }
}