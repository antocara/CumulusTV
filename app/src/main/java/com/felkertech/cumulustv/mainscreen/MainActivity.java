package com.felkertech.cumulustv.mainscreen;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.crashlytics.android.Crashlytics;
import com.felkertech.cumulustv.settings.SettingsActivity;
import com.felkertech.cumulustv.commons.helper.HelperDialogs;
import com.felkertech.cumulustv.fileio.CloudStorageProvider;
import com.felkertech.cumulustv.data.model.ChannelDatabase;
import com.felkertech.cumulustv.data.model.JsonChannel;
import com.felkertech.cumulustv.services.CumulusJobService;
import com.felkertech.cumulustv.tv.mainscreen.LeanbackActivity;
import com.felkertech.cumulustv.commons.helper.ActivityUtils;
import com.felkertech.cumulustv.commons.helper.AppUtils;
import com.felkertech.cumulustv.commons.helper.DriveSettingsManager;
import com.felkertech.cumulustv.widgets.ChannelShortcut;
import com.felkertech.n.cumulustv.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.MetadataChangeSet;
import io.fabric.sdk.android.Fabric;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private static final String TAG = MainActivity.class.getSimpleName();
  private static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
  private static final int REQUEST_CODE_CREATOR = 102;

  @BindView(R.id.version) TextView version;

  private DriveSettingsManager driveSettingsManager;
  private AlertDialog alertDialog;

  private AlertDialog.OnClickListener genreSelectionCallback =
      new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialogInterface, int i) {

        }
      };

  @Override protected void onResume() {
    super.onResume();
    ChannelShortcut.updateWidgets(this, ChannelShortcut.class);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    Fabric.with(this, new Crashlytics());

    driveSettingsManager = new DriveSettingsManager(this);
    ActivityUtils.openIntroIfNeeded(this);
    CloudStorageProvider.getInstance().autoConnect(this);

    try {

      if (AppUtils.isTV(this)) {
        // Go to tv activity
        Intent leanbackIntent = new Intent(this, LeanbackActivity.class);
        leanbackIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        startActivity(leanbackIntent);
      }
    } catch (ChannelDatabase.MalformedChannelDataException e) {
      ActivityUtils.handleMalformedChannelData(MainActivity.this,
          CloudStorageProvider.getInstance().getClient(), e);
    }

    setApplicationVersion();
    setBackgroundActionBar();
  }

  private void setBackgroundActionBar() {
    if (getSupportActionBar() != null) {

      int backColor = ContextCompat.getColor(this, R.color.colorPrimary);
      getSupportActionBar().show();
      getSupportActionBar().setBackgroundDrawable(new ColorDrawable(backColor));
    }
  }

  @Override public void onConnected(Bundle bundle) {
    ActivityUtils.onConnected(CloudStorageProvider.getInstance().getClient());

    if (alertDialog != null && alertDialog.isShowing()) {
      alertDialog.dismiss();
    }

    CloudStorageProvider.getInstance().onConnected(Activity.RESULT_OK);

    driveSettingsManager.setGoogleDriveSyncable(CloudStorageProvider.getInstance().getClient(),
        new DriveSettingsManager.GoogleDriveListener() {
          @Override public void onActionFinished(boolean cloudToLocal) {

            final String info = TvContract.buildInputId(ActivityUtils.TV_INPUT_SERVICE);
            CumulusJobService.requestImmediateSync1(MainActivity.this, info,
                CumulusJobService.DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS,
                new ComponentName(MainActivity.this, CumulusJobService.class));
            if (cloudToLocal) {
              Toast.makeText(MainActivity.this, R.string.downloaded, Toast.LENGTH_SHORT).show();
            } else {
              Toast.makeText(MainActivity.this, R.string.uploaded, Toast.LENGTH_SHORT).show();
            }
          }
        });

    if (driveSettingsManager.getString(R.string.sm_google_drive_id).isEmpty()) {

      new AlertDialog.Builder(MainActivity.this).setTitle(R.string.create_syncable_file)
          .setMessage(R.string.create_syncable_file_description)
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialogInterface, int i) {
              Drive.DriveApi.newDriveContents(ActivityUtils.getGoogleApiClient())
                  .setResultCallback(driveContentsCallback);
            }
          })
          .setNegativeButton(R.string.no, null)
          .show();
    } else {

      ActivityUtils.readDriveData(MainActivity.this, ActivityUtils.getGoogleApiClient());
    }
  }

  @Override protected void onStart() {
    super.onStart();
    CloudStorageProvider.getInstance().autoConnect(this);
  }

  private void setApplicationVersion() {
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      version.setText(getString(R.string.version, pInfo.versionName));
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Callback @{@link GoogleApiClient.ConnectionCallbacks}
   */
  @Override public void onConnectionSuspended(int i) {

  }

  @Override public void onConnectionFailed(ConnectionResult connectionResult) {

    if (alertDialog != null && alertDialog.isShowing()) {
      Toast.makeText(this, "Error connecting: " + connectionResult.toString(), Toast.LENGTH_SHORT)
          .show();
    }

    Toast.makeText(MainActivity.this, "Connection issue ("
        + connectionResult.getErrorCode()
        + "): "
        + connectionResult.toString(), Toast.LENGTH_SHORT).show();

    if (connectionResult.hasResolution()) {
      try {
        connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
      } catch (IntentSender.SendIntentException e) {
        Toast.makeText(MainActivity.this, "Cannot resolve issue", Toast.LENGTH_SHORT).show();
      }
    } else {
      try {
        GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), MainActivity.this, 0)
            .show();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    ActivityUtils.onActivityResult(MainActivity.this,
        CloudStorageProvider.getInstance().getClient(), requestCode, resultCode, data);
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {
      case ActivityUtils.PERMISSION_EXPORT_M3U:
        if (grantResults[0] == PERMISSION_GRANTED) {
          ActivityUtils.exportM3uPlaylist(MainActivity.this);
        }
        break;
    }
  }

  /** GDRIVE **/
  ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
      new ResultCallback<DriveApi.DriveContentsResult>() {
        @Override public void onResult(DriveApi.DriveContentsResult result) {
          MetadataChangeSet metadataChangeSet =
              new MetadataChangeSet.Builder().setTitle("cumulustv_channels.json")
                  .setDescription("JSON list of channels that can be imported using "
                      + "CumulusTV to view live streams")
                  .setMimeType("application/json")
                  .build();
          IntentSender intentSender = Drive.DriveApi.newCreateFileActivityBuilder()
              .setActivityTitle("cumulustv_channels.json")
              .setInitialMetadata(metadataChangeSet)
              .setInitialDriveContents(result.getDriveContents())
              .build(CloudStorageProvider.getInstance().getClient());
          try {
            startIntentSenderForResult(intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
          } catch (IntentSender.SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
          }
        }
      };

  public void displayChannelPicker(final List<JsonChannel> jsonChannels, String[] channelNames) {
    HelperDialogs.showAlertChannelsList(jsonChannels, channelNames, this,
        getString(R.string.my_channels));
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_mobile, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_add:
        ActivityUtils.openPluginPicker(true, MainActivity.this);
        break;
      case R.id.menu_add_suggested:
        ActivityUtils.openSuggestedChannels(MainActivity.this,
            CloudStorageProvider.getInstance().getClient());
        break;
      case R.id.menu_import:
        ActivityUtils.switchGoogleDrive(MainActivity.this,
            CloudStorageProvider.getInstance().getClient());
        break;
      case R.id.menu_licenses:
        ActivityUtils.oslClick(MainActivity.this);
        break;
      case R.id.menu_reset_data:
        ActivityUtils.deleteChannelData(MainActivity.this,
            CloudStorageProvider.getInstance().getClient());
        break;
      case R.id.menu_about:
        ActivityUtils.openAbout(MainActivity.this);
        break;
      case R.id.menu_settings:
        startActivity(new Intent(this, SettingsActivity.class));
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  @OnClick({ R.id.btn_all_channels, R.id.btn_categories, R.id.btn_drive, R.id.btn_more_actions })
  public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.btn_all_channels:
        handleBtnAllChannels();
        break;
      case R.id.btn_categories:
        handleBtnCategories();
        break;
      case R.id.btn_drive:
        handleBtnDrive();
        break;
      case R.id.btn_more_actions:
        handleBtnMoreActions();
        break;
    }
  }

  private void handleBtnAllChannels() {
    final ChannelDatabase channelDatabase = ChannelDatabase.getInstance(MainActivity.this);
    String[] channelnames = channelDatabase.getChannelNames();
    if (channelnames.length == 0) {
      HelperDialogs.showAlertAllChannels(this);
    } else {
      try {
        displayChannelPicker(channelDatabase.getJsonChannels(), channelnames);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  private void handleBtnCategories() {
    final ChannelDatabase channelDatabase = ChannelDatabase.getInstance(MainActivity.this);

    Set<String> genreSet = new HashSet<>();
    try {
      for (JsonChannel jsonChannel : channelDatabase.getJsonChannels()) {
        Collections.addAll(genreSet, jsonChannel.getGenres());
      }
      final String[] categoriesArray = genreSet.toArray(new String[genreSet.size()]);
      HelperDialogs.showAlertCategoriesChannels(MainActivity.this, categoriesArray);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private void handleBtnDrive() {
    CloudStorageProvider.getInstance().connect(MainActivity.this);
  }

  private void handleBtnMoreActions() {
    moreClick();
  }

  private void moreClick() {
    final String[] actions = new String[] {
        getString(R.string.settings_browse_plugins), getString(R.string.export_m3u),
        getString(R.string.settings_refresh_cloud_local)
    };
    HelperDialogs.showAlertWithMoreOptions(this, actions);
  }
}
