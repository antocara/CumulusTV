package com.felkertech.cumulustv.tv.mainscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.felkertech.cumulustv.fileio.CloudStorageProvider;
import com.felkertech.cumulustv.commons.helper.ActivityUtils;
import com.felkertech.n.cumulustv.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class LeanbackActivity extends Activity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private static final String TAG = LeanbackActivity.class.getSimpleName();

  public static final int RESULT_CODE_REFRESH_UI = 10;
  private LeanbackFragment leanbackFragment;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_leanback);

    leanbackFragment =
        (LeanbackFragment) getFragmentManager().findFragmentById(R.id.main_browse_fragment);

    ActivityUtils.openIntroIfNeeded(this);

    //Fabric.with(this, new Crashlytics());
  }

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

    ActivityUtils.onActivityResult(this, CloudStorageProvider.getInstance().getClient(),
        requestCode, resultCode, data);
    if (requestCode == RESULT_CODE_REFRESH_UI) {
      leanbackFragment.refreshUI();
    }
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    leanbackFragment.refreshUI();
  }

  @Override public void onConnected(@Nullable Bundle bundle) {
    leanbackFragment.onConnected(bundle);
  }

  @Override public void onConnectionSuspended(int i) {
    leanbackFragment.onConnectionSuspended(i);
  }

  @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    leanbackFragment.onConnectionFailed(connectionResult);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    leanbackFragment = null;
  }
}
