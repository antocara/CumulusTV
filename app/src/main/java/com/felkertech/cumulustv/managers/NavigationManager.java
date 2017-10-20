package com.felkertech.cumulustv.managers;

import android.app.Activity;
import android.content.Intent;
import com.felkertech.cumulustv.mainscreen.MainActivity;
import com.felkertech.cumulustv.tv.mainscreen.LeanbackActivity;
import com.felkertech.cumulustv.commons.helper.AppUtils;

public class NavigationManager {

  private NavigationManager() {
  }

  public static void navigateToMainActivity(Activity activity) {
    if (AppUtils.isTV(activity)) {
      openTvMainActivity(activity);
    } else {
      openPhoneMainActivity(activity);
    }
  }

  static private void openPhoneMainActivity(Activity activity) {
    Intent intent = new Intent(activity, MainActivity.class);
    activity.startActivity(intent);
  }

  static private void openTvMainActivity(Activity activity) {
    Intent intent = new Intent(activity, LeanbackActivity.class);
    activity.startActivity(intent);
  }
}
