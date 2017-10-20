package com.felkertech.cumulustv.data.model;

import android.content.ComponentName;
import android.media.tv.TvContract;
import android.support.annotation.NonNull;

import com.felkertech.cumulustv.plugins.CumulusChannel;
import com.felkertech.settingsmanager.common.CommaArray;
import com.google.android.media.tv.companionlibrary.model.Channel;
import com.google.android.media.tv.companionlibrary.model.InternalProviderData;
import com.google.android.media.tv.companionlibrary.utils.TvContractUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>A JsonChannel is a type of channel which is used in Cumulus TV. The data is serialized as a
 * {@link org.json.JSONObject} and parsed by the {@link ChannelDatabase}.</p>
 *
 * <p>It is an extension of the {@link CumulusChannel} class which provides custom methods specifically
 * for interfacing with the Tv Input Database.</p>
 *
 * @author Nick
 * @version 2016.09.04
 */
public class JsonChannel extends CumulusChannel implements Comparable {

    private JsonChannel() {
    }

    public String[] getGenres() {
        if(getGenresString() == null) {
            return new String[]{TvContract.Programs.Genres.MOVIES};
        }
        if(getGenresString().isEmpty()) {
            return new String[]{TvContract.Programs.Genres.MOVIES};
        }
        else {
            //Parse genres
            CommaArray ca = new CommaArray(getGenresString());
            Iterator<String> it = ca.getIterator();
            ArrayList<String> arrayList = new ArrayList<>();
            while(it.hasNext()) {
                String i = it.next();
                arrayList.add(i);
            }
            return arrayList.toArray(new String[arrayList.size()]);
        }
    }

    public Channel toChannel() {
        InternalProviderData ipd = new InternalProviderData();
        ipd.setVideoUrl(getMediaUrl());
        ipd.setVideoType(TvContractUtils.SOURCE_TYPE_HLS);
        return new Channel.Builder()
                .setDisplayName(getName())
                .setDisplayNumber(getNumber())
                .setChannelLogo(getLogo())
                .setInternalProviderData(ipd)
                .setOriginalNetworkId(getMediaUrl().hashCode())
                .build();
    }

    public Channel toChannel(InternalProviderData providerData) {
        providerData.setVideoUrl(getMediaUrl());
        // TODO Add app linking
        return new Channel.Builder()
                .setDisplayName(getName())
                .setDisplayNumber(getNumber())
                .setChannelLogo(getLogo())
                .setInternalProviderData(providerData)
                .setOriginalNetworkId(getMediaUrl().hashCode())
                .build();
    }

    public static JsonChannel getEmptyChannel() {
        return new JsonChannel();
    }

    @Override
    public int compareTo(Object o) {
        return getNumber().compareTo(((JsonChannel) o).getNumber());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JsonChannel && getMediaUrl().equals(((JsonChannel) o).getMediaUrl());
    }

    public static class Builder extends CumulusChannel.Builder {
        private JsonChannel jsonChannel;

        public Builder() {
            super();
        }

        public Builder(String string) throws JSONException {
            super(new JSONObject(string));
        }

        public Builder(CumulusChannel channel) {
            super(channel);
        }

        public Builder(JSONObject jsonObject) throws JSONException {
            super(jsonObject);
        }

        @Override
        public JsonChannel.Builder setAudioOnly(boolean audioOnly) {
            super.setAudioOnly(audioOnly);
            return this;
        }

        @Override
        public JsonChannel.Builder setEpgUrl(String epgUrl) {
            super.setEpgUrl(epgUrl);
            return this;
        }

        @Override
        public JsonChannel.Builder setGenres(String genres) {
            super.setGenres(genres);
            return this;
        }

        @Override
        public JsonChannel.Builder setLogo(String logo) {
            super.setLogo(logo);
            return this;
        }

        @Override
        public JsonChannel.Builder setMediaUrl(String mediaUrl) {
            super.setMediaUrl(mediaUrl);
            return this;
        }

        @Override
        public JsonChannel.Builder setName(String name) {
            super.setName(name);
            return this;
        }

        @Override
        public JsonChannel.Builder setNumber(String number) {
            super.setNumber(number);
            return this;
        }

        @Override
        public JsonChannel.Builder setPluginSource(@NonNull ComponentName pluginComponent) {
            super.setPluginSource(pluginComponent);
            return this;
        }

        @Override
        public JsonChannel.Builder setPluginSource(String pluginComponentName) {
            super.setPluginSource(pluginComponentName);
            return this;
        }

        @Override
        public JsonChannel.Builder setSplashscreen(String splashscreen) {
            super.setSplashscreen(splashscreen);
            return this;
        }

        @Override
        public JsonChannel build() {
            CumulusChannel cumulusChannel = super.build();
            jsonChannel = new JsonChannel();
            Builder builder = new Builder(cumulusChannel);
            builder.cloneInto(jsonChannel);
            return jsonChannel;
        }
    }
}
