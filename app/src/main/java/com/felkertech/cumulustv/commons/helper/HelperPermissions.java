package com.felkertech.cumulustv.commons.helper;

import android.Manifest;
import android.app.Activity;
import android.os.Build;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

public class HelperPermissions {

  private HelperPermissions() {
  }

  public static boolean hasPermission(Activity activity) {
    boolean hasPermission = true;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          == PERMISSION_DENIED) {
        hasPermission = false;
        activity.requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
            ActivityUtils.PERMISSION_EXPORT_M3U);
      }
    }

    return hasPermission;
  }
}
