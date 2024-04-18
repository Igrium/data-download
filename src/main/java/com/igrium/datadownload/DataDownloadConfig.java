package com.igrium.datadownload;


import java.io.Reader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DataDownloadConfig {
    public String filebinUrl = "https://filebin.net";
    public int permissionLevel = 3;

    // For gson
    public DataDownloadConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static DataDownloadConfig fromJson(String json) {
        return GSON.fromJson(json, DataDownloadConfig.class);
    }

    public static DataDownloadConfig fromJson(Reader reader) {
        return GSON.fromJson(reader, DataDownloadConfig.class);
    }

    public String toJson() {
        return GSON.toJson(this);
    }
}
