package com.felkertech.cumulustv.addurlchannels;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.felkertech.cumulustv.data.model.ChannelDatabase;
import com.felkertech.cumulustv.data.model.ChannelDatabaseFactory;
import com.felkertech.cumulustv.data.model.JsonChannel;
import com.felkertech.cumulustv.data.model.JsonListing;
import com.felkertech.cumulustv.fileio.AbstractFileParser;
import com.felkertech.cumulustv.fileio.HttpFileParser;
import com.felkertech.cumulustv.fileio.M3uParser;
import com.felkertech.cumulustv.plugins.CumulusTvPlugin;
import com.felkertech.n.cumulustv.R;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public class AddUrlSourceChannelsPlugin extends CumulusTvPlugin {

  private static final String TAG = AddUrlSourceChannelsPlugin.class.getSimpleName();

  @BindView(R.id.edit_url) EditText editUrl;
  @BindView(R.id.intro_title) TextView introTitle;
  @BindView(R.id.channel_count) TextView channelCount;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.add_url_source_channels);
    ButterKnife.bind(this);

    setLabel("");
    setProprietaryEditing(false);

    intercepteData();
  }

  private void intercepteData() {
    Intent intent = getIntent();

    if (intent != null && intent.getAction() != null) {
      String intentAction = intent.getAction();
      if (intentAction.equals(Intent.ACTION_SEND) || intentAction.equals(Intent.ACTION_VIEW)) {
        final Uri uri = getIntent().getData();
        initImportChannels(uri);
      } else {
        handleActionOverChannel();
      }
    }
  }

  private void handleActionOverChannel() {
    if (areEditing()) {
      try {
        populate();
      } catch (JSONException e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        finish();
      }
    } else if (areAdding()) {
      try {
        populate();
      } catch (JSONException e) {
        e.printStackTrace();
      }
    } else if (areReadingAll()) {
      // Show items
      try {
        showLinks();
      } catch (JSONException e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        finish();
      }
    }
  }

  private void initImportChannels(final Uri uri) {
    new AlertDialog.Builder(this).setTitle(R.string.link_to_m3u)
        .setMessage(getString(R.string.json_link_confirmation, uri))
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            save(new JsonListing.Builder().setUrl(String.valueOf(uri)).build());
          }
        })
        .show();
  }

  private void populate() throws JSONException {
    if (getJson() != null) {
      JsonListing listing = new JsonListing.Builder(getJson()).build();
      editUrl.setText(listing.getUrl());
    }

    editUrl.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override public void afterTextChanged(Editable editable) {
        String url = editable.toString();

        new HttpFileParser(url, new AbstractFileParser.FileLoader() {
          @Override public void onFileLoaded(final InputStream inputStream) {
            if (inputStream == null) {
              channelCount.setText("");
            } else {
              parseChannelStream(inputStream);
            }
          }
        });
      }
    });
  }

  private void parseChannelStream(InputStream inputStream) {
    try {
      M3uParser.TvListing listing = M3uParser.parse(inputStream);
      if (listing != null) {
        final List<M3uParser.M3uTvChannel> channels = listing.channels;
        channelCount.post(new Runnable() {
          @Override public void run() {
            channelCount.setText(getString(R.string.x_channels_found, channels.size()));
          }
        });
      } else {
        channelCount.post(new Runnable() {
          @Override public void run() {
            channelCount.setText("");
          }
        });
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void showLinks() throws JSONException {
    Log.d(TAG, ChannelDatabase.getInstance(this).toString());
    final List<JsonListing> listings = new ArrayList<>();
    final List<String> urlList = new ArrayList<>();
    urlList.add(getString(R.string.close_pretty));
    JSONArray channelData = ChannelDatabase.getInstance(this).getJSONArray();
    for (int i = 0; i < channelData.length(); i++) {
      ChannelDatabaseFactory.parseType(channelData.getJSONObject(i),
          new ChannelDatabaseFactory.ChannelParser() {
            @Override public void ifJsonChannel(JsonChannel entry) {

            }

            @Override public void ifJsonListing(JsonListing entry) {
              listings.add(entry);
              urlList.add(entry.getUrl());
            }
          });
    }
    String[] urlArray = urlList.toArray(new String[urlList.size()]);

    new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Dialog)).setTitle(
        R.string.link_to_m3u).setItems(urlArray, new DialogInterface.OnClickListener() {
      @Override public void onClick(DialogInterface dialogInterface, int position) {
        if (position == 0) {
          finish();
        } else {
          showEditDialog(listings.get(position - 1));
        }
      }
    }).show();
  }

  private void showEditDialog(final JsonListing listing) {
    new AlertDialog.Builder(this).setTitle(R.string.link_to_m3u)
        .setMessage(listing.getUrl())
        .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialogInterface, int i) {
            try {
              ChannelDatabase.getInstance(AddUrlSourceChannelsPlugin.this).delete(listing);
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        })
        .setNeutralButton(R.string.cancel, null)
        .show();
  }

  @OnClick(R.id.button_update) public void onViewClicked() {

    String url = editUrl.getText().toString();
    if (!url.isEmpty()) {
      //JsonListing newListing = new JsonListing.Builder().setUrl(url).build();
      //save(newListing);

      new HttpFileParser(url, new AbstractFileParser.FileLoader() {
        @Override public void onFileLoaded(final InputStream inputStream) {
          if (inputStream == null) {
            channelCount.setText("");
          } else {
            AddUrlSourceChannelsPlugin.this.runOnUiThread(new Runnable() {
              @Override public void run() {
                parseChannelStream(inputStream);
              }
            });
          }
        }
      });
    } else {
      Toast.makeText(AddUrlSourceChannelsPlugin.this, R.string.msg_url_empty, Toast.LENGTH_SHORT)
          .show();
    }
  }
}
