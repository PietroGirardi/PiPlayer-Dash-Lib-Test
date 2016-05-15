package com.example.pietrogirardi.piplayerexample;

import com.google.android.exoplayer.util.Util;

import java.util.Locale;

/**
 * Created by pietrogirardi on 14/05/16.
 */
public class Sample {

    public final String name;
    public final String uri;
    public final int type;
    public final String contentId;
    public final String provider;


    public Sample(String name, String uri) {
        this(name, uri, Util.TYPE_DASH, name.toLowerCase(Locale.US).replaceAll("\\s", ""), "");
    }

    public Sample(String name, String uri, int type, String contentId, String provider) {
        this.name = name;
        this.uri = uri;
        this.type = type;
        this.contentId = contentId;
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public int getType() {
        return type;
    }

    public String getContentId() {
        return contentId;
    }

    public String getProvider() {
        return provider;
    }
}
