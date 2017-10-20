package com.felkertech.cumulustv.commons.helper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;
import com.felkertech.cumulustv.Intro.Intro;
import com.felkertech.cumulustv.activities.CumulusVideoPlayback;
import com.felkertech.cumulustv.activities.HomepageWebViewActivity;
import com.felkertech.cumulustv.mainscreen.MainActivity;
import com.felkertech.cumulustv.fileio.CloudStorageProvider;
import com.felkertech.cumulustv.data.model.ChannelDatabase;
import com.felkertech.cumulustv.data.model.JsonChannel;
import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.cumulustv.plugins.CumulusTvPlugin;
import com.felkertech.cumulustv.receivers.GoogleDriveBroadcastReceiver;
import com.felkertech.cumulustv.services.CumulusJobService;
import com.felkertech.n.cumulustv.R;
import com.felkertech.settingsmanager.SettingsManager;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;

import static com.felkertech.cumulustv.data.model.SuggestedChannels.getSuggestedChannels;

public class ActivityUtils {
  private static final String TAG = ActivityUtils.class.getSimpleName();
  private static final boolean DEBUG = false;

  public static final int RESOLVE_CONNECTION_REQUEST_CODE = 100;
  public static final int REQUEST_CODE_CREATOR = 102;
  public static final int REQUEST_CODE_OPENER = 104;
  public static final int PERMISSION_EXPORT_M3U = 201;
  public static final ComponentName TV_INPUT_SERVICE =
      new ComponentName("com.felkertech.n.cumulustv",
          "com.felkertech.cumulustv.tv.tvinputservice.CumulusTvTifService");

  public final static int LAST_GOOD_BUILD = 27;

  private static CloudStorageProvider mCloudStorageProvider = CloudStorageProvider.getInstance();
  private static GoogleApiClient mGoogleApiClient;

  public static GoogleApiClient getGoogleApiClient() {
    return mGoogleApiClient;
  }

  public static void openSuggestedChannels(final Activity mActivity, final GoogleApiClient gapi) {
    final CumulusChannel[] channels = getSuggestedChannels();
    ArrayList<String> channeltext = new ArrayList<String>();
    for (CumulusChannel j : channels) {
      channeltext.add(j.getName());
    }
    final String[] channelList = channeltext.toArray(new String[channeltext.size()]);
    new AlertDialog.Builder(mActivity).setTitle(R.string.here_are_suggested_channels)
        .setItems(channelList, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            CumulusChannel j = channels[i];
            addChannel(mActivity, gapi, j);
          }
        })
        .show();
  }

  public static void addChannel(final Activity activity, GoogleApiClient gapi,
      CumulusChannel jsonChannel) {

    ChannelDatabase cd = ChannelDatabase.getInstance(activity);
    if (cd.channelExists(jsonChannel)) {
      Toast.makeText(activity, R.string.channel_dupe, Toast.LENGTH_SHORT).show();
    } else {
      try {
        if (jsonChannel.getName() != null) {
          Toast.makeText(activity,
              activity.getString(R.string.channel_added, jsonChannel.getName()), Toast.LENGTH_SHORT)
              .show();
        }
        cd.add(jsonChannel);
        if (CloudStorageProvider.getInstance().isDriveConnected()) {
          ActivityUtils.writeDriveData(activity, gapi);
        }
        GoogleDriveBroadcastReceiver.changeStatus(activity,
            GoogleDriveBroadcastReceiver.EVENT_DOWNLOAD_COMPLETE);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Opens the correct intent to start editing the channel.
   *
   * @param activity The activity you're calling this from.
   * @param channelUrl The channel's media url.m
   */
  public static void editChannel(final Activity activity, final String channelUrl) {
    ChannelDatabase cdn = ChannelDatabase.getInstance(activity);
    final JsonChannel jsonChannel = cdn.findChannelByMediaUrl(channelUrl);
    if (channelUrl == null || jsonChannel == null) {
      try {
        Toast.makeText(activity, R.string.toast_error_channel_invalid, Toast.LENGTH_SHORT).show();
      } catch (RuntimeException e) {
        Log.e(TAG, activity.getString(R.string.toast_error_channel_invalid));
      }
      return;
    }
    if (jsonChannel.getPluginSource() != null) {
      // Search through all plugins for one of a given source
      PackageManager pm = activity.getPackageManager();

      try {
        pm.getPackageInfo(jsonChannel.getPluginSource().getPackageName(),
            PackageManager.GET_ACTIVITIES);
        // Open up this particular activity
        Intent intent = new Intent();
        intent.setComponent(jsonChannel.getPluginSource());
        intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);

        intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_JSON, jsonChannel.toString());
        activity.startActivity(intent);
      } catch (PackageManager.NameNotFoundException e) {
        new AlertDialog.Builder(activity).setTitle(
            activity.getString(R.string.plugin_not_installed_title,
                jsonChannel.getPluginSource().getPackageName()))
            .setMessage(R.string.plugin_not_installed_question)
            .setPositiveButton(R.string.download_app, new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialogInterface, int which) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(
                    "http://play.google.com/store/apps/details?id=" + jsonChannel.getPluginSource()
                        .getPackageName()));
                activity.startActivity(i);
              }
            })
            .setNegativeButton(R.string.open_in_another_plugin,
                new DialogInterface.OnClickListener() {
                  @Override public void onClick(DialogInterface dialogInterface, int i) {
                    openPluginPicker(false, channelUrl, activity);
                  }
                })
            .show();
        Toast.makeText(activity, activity.getString(R.string.toast_msg_pack_not_installed,
            jsonChannel.getPluginSource().getPackageName()), Toast.LENGTH_SHORT).show();
        openPluginPicker(false, channelUrl, activity);
      }
    } else {

      openPluginPicker(false, channelUrl, activity);
    }
  }

  /* DRIVE */
  public static void writeDriveData(final Activity context, GoogleApiClient gapi) {
    // Ask here for permission to storage
    PermissionUtils.requestPermissionIfDisabled(context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        context.getString(R.string.permission_storage_rationale));
    if (PermissionUtils.isDisabled(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      new AlertDialog.Builder(context).setTitle(R.string.permission_not_allowed_error)
          .setMessage(R.string.permission_not_allowed_text)
          .setPositiveButton(R.string.permission_action_settings,
              new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialogInterface, int i) {
                  Intent intent = new Intent();
                  intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                  Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                  intent.setData(uri);
                }
              })
          .setNegativeButton(R.string.ok, null)
          .show();
    } else {
      actuallyWriteData(context, gapi);
    }
  }

  public static void writeDriveData(final Context context, GoogleApiClient gapi) {
    // This can crash
    actuallyWriteData(context, gapi);
  }

  private static void actuallyWriteData(final Context context, GoogleApiClient gapi) {
    DriveSettingsManager sm = new DriveSettingsManager(context);
    sm.setGoogleDriveSyncable(gapi, new DriveSettingsManager.GoogleDriveListener() {
      @Override public void onActionFinished(boolean cloudToLocal) {
        Log.d(TAG, "Action finished " + cloudToLocal);
      }
    });
    try {
      sm.writeToGoogleDrive(DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id)),
          ChannelDatabase.getInstance(context).toString());
      GoogleDriveBroadcastReceiver.changeStatus(context,
          GoogleDriveBroadcastReceiver.EVENT_UPLOAD_COMPLETE);

      final String info = TvContract.buildInputId(TV_INPUT_SERVICE);
      CumulusJobService.requestImmediateSync1(context, info,
          CumulusJobService.DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS,
          new ComponentName(context, CumulusJobService.class));
      Log.d(TAG, "Data actually written");
      //            Toast.makeText(context, "Channels uploaded", Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
      // Probably invalid drive id. No worries, just let someone know
      Log.e(TAG, e.getMessage() + "");
      Toast.makeText(context, R.string.invalid_file, Toast.LENGTH_SHORT).show();
    }
  }

  public static void readDriveData(@NonNull Context context, GoogleApiClient gapi) {
    DriveSettingsManager sm = new DriveSettingsManager(context);
    sm.setGoogleDriveSyncable(gapi, null);
    DriveId did;
    try {
      did = DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id));
    } catch (Exception e) {
      Toast.makeText(context, R.string.invalid_file, Toast.LENGTH_SHORT).show();
      return;
    }
    try {
      sm.readFromGoogleDrive(did, ChannelDatabase.KEY);
    } catch (ChannelDatabase.MalformedChannelDataException e) {
      Toast.makeText(context, R.string.drive_error_not_valid_json, Toast.LENGTH_SHORT).show();
    }
    GoogleDriveBroadcastReceiver.changeStatus(context,
        GoogleDriveBroadcastReceiver.EVENT_DOWNLOAD_COMPLETE);

    final String info = TvContract.buildInputId(TV_INPUT_SERVICE);
    if (AppUtils.isTV(context)) {
      CumulusJobService.requestImmediateSync1(context, info,
          CumulusJobService.DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS,
          new ComponentName(context, CumulusJobService.class));
    }
  }

  public static void createDriveData(Activity activity, GoogleApiClient gapi,
      final ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback) {
    PermissionUtils.requestPermissionIfDisabled(activity,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        activity.getString(R.string.permission_storage_rationale));
    if (gapi == null) gapi = mCloudStorageProvider.connect(activity);
    try {
      final GoogleApiClient finalGapi = gapi;
      new AlertDialog.Builder(activity).setTitle(R.string.create_sync_file_title)
          .setMessage(R.string.create_sync_file_description)
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialogInterface, int i) {
              Drive.DriveApi.newDriveContents(finalGapi).setResultCallback(driveContentsCallback);
            }
          })
          .setNegativeButton(R.string.no, null)
          .show();
    } catch (Exception ignored) {
      Toast.makeText(activity, "Error creating drive data: " + ignored.getMessage(),
          Toast.LENGTH_SHORT).show();
    }
  }

  public static void switchGoogleDrive(Activity mActivity, GoogleApiClient gapi) {
    syncFile(mActivity, gapi);
  }

  public static void deleteChannelData(final Activity activity, GoogleApiClient gapi) {
    final DriveSettingsManager sm = new DriveSettingsManager(activity);
    sm.setGoogleDriveSyncable(gapi, null);
    new AlertDialog.Builder(activity).setTitle(R.string.title_delete_all_channels)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            ChannelDatabase.getInstance(activity).eraseData();
            GoogleDriveBroadcastReceiver.changeStatus(activity,
                GoogleDriveBroadcastReceiver.EVENT_DOWNLOAD_COMPLETE);
            if (CloudStorageProvider.getInstance().isDriveConnected()) {
              try {
                DriveId did = DriveId.decodeFromString(sm.getString(R.string.sm_google_drive_id));
                sm.writeToGoogleDrive(did, sm.getString(ChannelDatabase.KEY));
              } catch (Exception e) {
                Toast.makeText(activity, R.string.toast_error_driveid_invalid, Toast.LENGTH_SHORT)
                    .show();
              }
              sm.setString(R.string.sm_google_drive_id, "");
            }
            Toast.makeText(activity, R.string.toast_msg_channels_deleted, Toast.LENGTH_SHORT)
                .show();
          }
        })
        .setNegativeButton(R.string.no, null)
        .show();
  }

  public static void syncFile(Activity activity, GoogleApiClient gapi) {

    if (gapi == null && mCloudStorageProvider.connect(activity) != null) {
      gapi = mCloudStorageProvider.connect(activity);
    } else if (mCloudStorageProvider.connect(activity) == null) {
      // Is not existent
      Toast.makeText(activity, "There is no Google Play Service", Toast.LENGTH_SHORT).show();
      return;
    }
    if (gapi.isConnected()) {
      IntentSender intentSender = Drive.DriveApi.newOpenFileActivityBuilder()
          .setMimeType(new String[] { "application/json", "text/*" })
          .build(gapi);
      try {

        activity.startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
      } catch (IntentSender.SendIntentException e) {

        e.printStackTrace();
      }
    } else {
      Toast.makeText(activity, R.string.toast_msg_wait_google_api_client, Toast.LENGTH_SHORT)
          .show();
    }
  }

  /* LICENSES */
  public static void oslClick(Activity activity) {
    new AlertDialog.Builder(activity).setTitle(R.string.software_licenses)
        .setMessage(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(activity))
        .show();
  }

  /* PLUGIN INTERFACES */
  public static void openPluginPicker(final boolean newChannel, Activity activity) {
    openPluginPicker(newChannel, 0, activity);
  }

  public static void openPluginPicker(final boolean newChannel, final int index,
      final Activity activity) {
    try {
      ChannelDatabase cdn = ChannelDatabase.getInstance(activity);
      if (cdn.getJsonChannels().isEmpty()) {
        openPluginPicker(newChannel, JsonChannel.getEmptyChannel(), activity);
      } else {
        JsonChannel jsonChannel = cdn.getJsonChannels().get(index);
        openPluginPicker(newChannel, jsonChannel, activity);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public static void openPluginPicker(final boolean newChannel, final String channelUrl,
      final Activity activity) {
    ChannelDatabase cdn = ChannelDatabase.getInstance(activity);
    openPluginPicker(newChannel, cdn.findChannelByMediaUrl(channelUrl), activity);
  }

  public static void openPluginPicker(final boolean newChannel, final JsonChannel queriedChannel,
      final Activity activity) {
    final PackageManager pm = activity.getPackageManager();
    final Intent plugin_addchannel = new Intent(CumulusTvPlugin.ACTION_ADD_CHANNEL);
    final List<ResolveInfo> plugins = pm.queryIntentActivities(plugin_addchannel, 0);
    ArrayList<String> plugin_names = new ArrayList<String>();
    for (ResolveInfo ri : plugins) {
      plugin_names.add(ri.loadLabel(pm).toString());
    }
    String[] plugin_names2 = plugin_names.toArray(new String[plugin_names.size()]);
    if (DEBUG) {
      Log.d(TAG, "Load plugins " + plugin_names.toString());
    }
    if (plugin_names.size() == 1) {
      Intent intent = new Intent();
      if (newChannel) {

        ResolveInfo plugin_info = plugins.get(0);

        intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
            plugin_info.activityInfo.name);
        intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_ADD);
      } else {
        ResolveInfo plugin_info = plugins.get(0);

        intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
            plugin_info.activityInfo.name);
        intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
        intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_JSON, queriedChannel.toString());
      }
      activity.startActivity(intent);
    } else {
      new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.CompatTheme)).setTitle(
          R.string.choose_an_app)
          //                    .setMessage(R.string.choose_default_app)
          .setItems(plugin_names2, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
              Intent intent = new Intent();
              if (newChannel) {

                ResolveInfo plugin_info = plugins.get(which);

                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                    plugin_info.activityInfo.name);

                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_ADD);
              } else {
                ResolveInfo plugin_info = plugins.get(which);
                intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
                    plugin_info.activityInfo.name);
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EDIT);
                intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_JSON, queriedChannel.toString());
              }
              activity.startActivity(intent);
            }
          }).show();
    }
  }

  public static void browsePlugins(final Activity activity) {
    //Same opening
    final PackageManager pm = activity.getPackageManager();
    final Intent plugin_addchannel = new Intent(CumulusTvPlugin.ACTION_ADD_CHANNEL);
    final List<ResolveInfo> plugins = pm.queryIntentActivities(plugin_addchannel, 0);
    ArrayList<String> plugin_names = new ArrayList<>();
    for (ResolveInfo ri : plugins) {
      plugin_names.add(ri.loadLabel(pm).toString());
    }
    String[] plugin_names2 = plugin_names.toArray(new String[plugin_names.size()]);

    new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.CompatTheme)).setTitle(
        R.string.installed_plugins).setItems(plugin_names2, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int i) {
        // Load the given plugin with some additional info
        ChannelDatabase cd = ChannelDatabase.getInstance(activity);
        String s = cd.toString();
        Intent intent = new Intent();

        ResolveInfo plugin_info = plugins.get(i);

        intent.setClassName(plugin_info.activityInfo.applicationInfo.packageName,
            plugin_info.activityInfo.name);
        intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION, CumulusTvPlugin.INTENT_EXTRA_READ_ALL);
        intent.putExtra(CumulusTvPlugin.INTENT_EXTRA_ALL_CHANNELS, s);
        activity.startActivity(intent);
      }
    }).setPositiveButton(R.string.download_more_plugins, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int wh) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("http://play.google.com/store/search?q=cumulustv&c=apps"));
        activity.startActivity(i);
      }
    }).show();
  }

  /* ACTIVITY CLONES */
  public static void launchLiveChannels(Activity activity) {
    Intent i = new Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI);
    try {
      activity.startActivity(i);
    } catch (Exception e) {
      Toast.makeText(activity, R.string.no_live_channels, Toast.LENGTH_SHORT).show();
    }
  }

  public static void onActivityResult(final Activity activity, GoogleApiClient gapi,
      final int requestCode, final int resultCode, final Intent data) {
    SettingsManager sm = new SettingsManager(activity);
    if (gapi == null) {
      gapi = mCloudStorageProvider.connect(activity);
    }
    mGoogleApiClient = gapi;
    switch (requestCode) {
      case RESOLVE_CONNECTION_REQUEST_CODE:
        if (resultCode == Activity.RESULT_OK) {

          gapi.connect();
        } else {

          final GoogleApiClient finalGapi = gapi;
          new AlertDialog.Builder(activity).setTitle(R.string.connection_issue_title)
              .setMessage(R.string.connection_issue_description)
              .setPositiveButton(R.string.ok, null)
              .setNegativeButton(R.string.try_again, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialogInterface, int i) {
                  finalGapi.connect();
                }
              })
              .show();
        }
        break;
      case REQUEST_CODE_CREATOR:
        if (data == null || !gapi.isConnected()) {
          // If op was canceled
          return;
        }
        DriveId driveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

        sm.setString(R.string.sm_google_drive_id, driveId.encodeToString());

        DriveFile file =
            Drive.DriveApi.getFile(gapi, DriveId.decodeFromString(driveId.encodeToString()));
        //Write initial data
        ActivityUtils.writeDriveData(activity, gapi);
        break;
      case ActivityUtils.REQUEST_CODE_OPENER:
        if (data == null) { // If operation was canceled
          return;
        }
        driveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

        sm.setString(R.string.sm_google_drive_id, driveId.encodeToString());
        final GoogleApiClient finalGapi1 = gapi;
        new AlertDialog.Builder(activity).setTitle(R.string.sync_initial)
            .setPositiveButton(R.string.sync_cloud_local, new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialogInterface, int i) {
                ActivityUtils.readDriveData(activity, finalGapi1);
              }
            })
            .setNegativeButton(R.string.sync_local_cloud, new DialogInterface.OnClickListener() {
              @Override public void onClick(DialogInterface dialogInterface, int i) {
                ActivityUtils.writeDriveData(activity, finalGapi1);
              }
            })
            .show();
    }
  }

  /* MISC */
  public static void openAbout(final Activity activity) {
    try {
      PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
      new AlertDialog.Builder(activity).setTitle(R.string.app_name)
          .setMessage(activity.getString(R.string.about_app_description, pInfo.versionName))
          .setPositiveButton(R.string.website, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialogInterface, int i) {
              launchWebsite(activity);
            }
          })
          .setNegativeButton(R.string.help, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialogInterface, int i) {
              ActivityUtils.openIntroVoluntarily(activity);
            }
          })
          .show();
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static void launchWebsite(Activity activity) {
    String url = activity.getString(R.string.website_url);
    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
    CustomTabsIntent customTabsIntent = builder.build();
    try {
      customTabsIntent.launchUrl(activity, Uri.parse(url));
    } catch (Exception e) {
      // There is no way to view the website.
      activity.startActivity(new Intent(activity, HomepageWebViewActivity.class));
    }
  }

  public static void openStream(Activity activity, String url) {
    Intent i = new Intent(activity, CumulusVideoPlayback.class);
    i.putExtra(CumulusVideoPlayback.KEY_VIDEO_URL, url);
    activity.startActivity(i);
  }

  public static void openIntroIfNeeded(Activity activity) {
    SettingsManager sm = new SettingsManager(activity);
    if (sm.getInt(R.string.sm_last_version) < LAST_GOOD_BUILD) {
      activity.startActivity(new Intent(activity, Intro.class));
      activity.finish();
    }
  }

  public static void openIntroVoluntarily(Activity activity) {
    activity.startActivity(new Intent(activity, Intro.class));
    activity.finish();
  }

  public static void handleMalformedChannelData(final Activity activity,
      final GoogleApiClient googleApiClient, ChannelDatabase.MalformedChannelDataException e) {

    new AlertDialog.Builder(activity).setTitle(R.string.chdb_issue_title)
        .setMessage(activity.getString(R.string.chdb_issue_info, e.getMessage()))
        .setPositiveButton(R.string.action_clear_local_data, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            ActivityUtils.deleteChannelData(activity, googleApiClient);
            activity.startActivity(new Intent(activity, MainActivity.class));
          }
        })
        .setNegativeButton(R.string.exit_app, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            activity.finish();
          }
        })
        .show();
  }

  public static void exportM3uPlaylist(final Activity activity) {
    File dir = Environment.getExternalStorageDirectory();
    File userM3u = new File(dir.toString() + "/" + "cumulus_channels.m3u");
    int index = 0;

    while (userM3u.exists()) {
      userM3u = new File(dir.toString() + "/" + "cumulus_channels_" + index + ".m3u");

      index++;
    }

    try {

      FileOutputStream out = new FileOutputStream(userM3u);

      out.write(ChannelDatabase.getInstance(activity).toM3u().getBytes());
      out.flush();
      out.close();

      Toast.makeText(activity, "M3U Playlist written to " + userM3u.getCanonicalPath(),
          Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void onConnected(GoogleApiClient gapi) {
    mGoogleApiClient = gapi;
  }

  public static void onChannelsChanged(Context context) {
    // Send broadcast to update UI
    GoogleDriveBroadcastReceiver.changeStatus(context,
        GoogleDriveBroadcastReceiver.EVENT_DOWNLOAD_COMPLETE);
    // Re-request a TIF sync
    CumulusJobService.requestImmediateSync1(context,
        ActivityUtils.TV_INPUT_SERVICE.flattenToString(),
        CumulusJobService.DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS,
        new ComponentName(context, CumulusJobService.class));
  }
}