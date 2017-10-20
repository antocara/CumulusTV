package com.felkertech.cumulustv.commons.helper;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import com.felkertech.cumulustv.fileio.CloudStorageProvider;
import com.felkertech.cumulustv.data.model.ChannelDatabase;
import com.felkertech.cumulustv.data.model.JsonChannel;
import com.felkertech.n.cumulustv.R;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;

public class HelperDialogs {

  private HelperDialogs() {
  }

  public static void showAlertWithMoreOptions(final Activity activity, final String[] actions) {
    new AlertDialog.Builder(activity).setTitle(R.string.more_actions)
        .setItems(actions, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int position) {
            String optionSelected = actions[position];

            if (optionSelected.equals(activity.getString(R.string.settings_browse_plugins))) {
              ActivityUtils.browsePlugins(activity);
            } else if (optionSelected.equals(activity.getString(R.string.export_m3u))) {
              if (HelperPermissions.hasPermission(activity)) {
                ActivityUtils.exportM3uPlaylist(activity);
              }
            } else if (optionSelected.equals(
                activity.getString(R.string.settings_refresh_cloud_local))) {
              ActivityUtils.readDriveData(activity, CloudStorageProvider.getInstance().getClient());
            }
          }
        })
        .show();
  }

  public static void showAlertChannelsList(final List<JsonChannel> jsonChannels,
      String[] channelNames, final Activity activity, String label) {
    new AlertDialog.Builder(activity).setTitle(label)
        .setItems(channelNames, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            JsonChannel jsonChannel = jsonChannels.get(i);
            ActivityUtils.editChannel(activity, jsonChannel.getMediaUrl());
          }
        })
        .show();
  }

  public static void showAlertAllChannels(final Activity activity) {
    new AlertDialog.Builder(activity).setTitle(R.string.no_channels)
        .setMessage(R.string.no_channels_find)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.cancel();
            ActivityUtils.openSuggestedChannels(activity,
                CloudStorageProvider.getInstance().getClient());
          }
        })
        .setNegativeButton(R.string.no, null)
        .show();
  }

  public static void showAlertCategoriesChannels(final Activity activity,
      final String[] categoriesArray) {
    new AlertDialog.Builder(activity).setTitle(R.string.select_genres)
        .setItems(categoriesArray, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int position) {

            final ChannelDatabase channelDatabase = ChannelDatabase.getInstance(activity);
            String selectedGenre = categoriesArray[position];

            List<JsonChannel> jsonChannelList = new ArrayList<>();
            List<String> channelNames = new ArrayList<>();

            try {
              for (JsonChannel jsonChannel : channelDatabase.getJsonChannels()) {
                if (jsonChannel.getGenresString().contains(selectedGenre)) {
                  jsonChannelList.add(jsonChannel);
                  channelNames.add(jsonChannel.getNumber() + " " + jsonChannel.getName());
                }
              }

              HelperDialogs.showAlertChannelsList(jsonChannelList,
                  channelNames.toArray(new String[channelNames.size()]), activity, selectedGenre);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        })
        .show();
  }
}
