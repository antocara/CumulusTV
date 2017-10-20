package com.felkertech.cumulustv.plugins;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.util.Log;

import com.felkertech.cumulustv.data.model.ChannelDatabaseFactory;
import com.felkertech.cumulustv.data.model.JsonListing;
import com.felkertech.cumulustv.services.CumulusJobService;
import com.felkertech.cumulustv.commons.helper.ActivityUtils;
import com.felkertech.cumulustv.commons.helper.DriveSettingsManager;
import com.felkertech.cumulustv.data.model.ChannelDatabase;
import com.felkertech.cumulustv.data.model.JsonChannel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import org.json.JSONException;
import org.json.JSONObject;

public class DataReceiver extends BroadcastReceiver
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = DataReceiver.class.getSimpleName();
    private static final boolean DEBUG = true;

    private GoogleApiClient gapi;
    private Context mContext;
    private Intent mIntent;

    public DataReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
        gapi = new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        if(intent != null) {
            String action = intent.getStringExtra(CumulusTvPlugin.INTENT_EXTRA_ACTION);
            String jsonString = "";
            if (intent.hasExtra(CumulusTvPlugin.INTENT_EXTRA_JSON)) {
                jsonString = intent.getStringExtra(CumulusTvPlugin.INTENT_EXTRA_JSON);
            }
            if (action.equals(CumulusTvPlugin.INTENT_EXTRA_ACTION_DATABASE_WRITE)) {
                Log.d(TAG, "Received write command");
                gapi.connect();
            } else if (action.equals(CumulusTvPlugin.INTENT_EXTRA_ACTION_WRITE)) {
                Log.d(TAG, "Received " + jsonString);
                DriveSettingsManager sm = new DriveSettingsManager(context);
                try {
                    JSONObject jo = new JSONObject(jsonString);
                    handle(jo);

                    gapi.connect();
                } catch (JSONException e) {
                    if (DEBUG) {
                        Log.e(TAG, e.getMessage() + "; Error while adding");
                    }
                    e.printStackTrace();
                }
            } else if (action.equals(CumulusTvPlugin.INTENT_EXTRA_ACTION_DELETE)) {
                ChannelDatabase cdn = ChannelDatabase.getInstance(context);
                try {
                    JSONObject jo = new JSONObject(jsonString);
                    JsonChannel jsonChannel = new JsonChannel.Builder(jo).build();
                    cdn.delete(jsonChannel);
                    if (DEBUG) {
                        Log.d(TAG, "Channel successfully deleted");
                    }
                    // Now sync
                    gapi.connect();
                } catch (JSONException e) {
                    if (DEBUG) {
                        Log.e(TAG, e.getMessage() + "; Error while adding");
                    }
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (DEBUG) {
            Log.d(TAG, "Connected - Sync w/ drive");
        }
        //Let's sync with GDrive
        ActivityUtils.writeDriveData(mContext, gapi);

        final String info = TvContract.buildInputId(ActivityUtils.TV_INPUT_SERVICE);
        CumulusJobService.requestImmediateSync1(mContext, info, CumulusJobService.DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS,
                new ComponentName(mContext, CumulusJobService.class));
    }

    private void handle(JSONObject jsonObject) {
        Log.d(TAG, "Handle " + jsonObject.toString());
        final ChannelDatabase cdn = ChannelDatabase.getInstance(mContext);
        ChannelDatabaseFactory.parseType(jsonObject, new ChannelDatabaseFactory.ChannelParser() {
            @Override
            public void ifJsonChannel(JsonChannel entry) {
                JsonChannel.Builder builder = new JsonChannel.Builder(entry);
                if (mIntent.hasExtra(CumulusTvPlugin.INTENT_EXTRA_ORIGINAL_JSON)) {
                    // Clearly edited a stream
                    try {
                        entry = builder.build();
                        if (cdn.channelExists(entry)) {
                            // Channel exists, so let's update.
                            cdn.update(entry);
                            if (DEBUG) {
                                Log.d(TAG, "Channel updated");
                            }
                        } else {
                            // No channel exists, so add it.
                            cdn.add(entry);
                            if (DEBUG) {
                                Log.d(TAG, "Channel added");
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // No previous data exists, so add it.
                    try {
                        cdn.add(entry);
                        if (DEBUG) {
                            Log.d(TAG, "Channel added");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void ifJsonListing(JsonListing entry) {
                Log.d(TAG, "Identified stream as JSON Listing");
                try {
                    cdn.add(entry);
                    // TODO Doesn't update
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
}
