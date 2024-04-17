package com.igrium.datadownload.filebin;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class FilebinMeta {
    public static class FilebinFileMeta {
        public String filename = "";

        @SerializedName("content-type")
        public String contentType;

        public long bytes;

        @SerializedName("bytes_readable")
        public String bytesReadable;

        public String md5;

        public String sha256;

        @SerializedName("updated_at")
        public String updatedAt;

        @SerializedName("updated_at_relative")
        public String updatedAtRelative;

        @SerializedName("created_at")
        public String createdAt;

        @SerializedName("created_at_relative")
        public String createdAtRelative;
    }

    public static class FilebinBinMeta {
        public String id;

        public boolean readonly;

        public int bytes;

        @SerializedName("bytes_readable")
        public String bytesReadable;

        public int files;

        @SerializedName("updated_at")
        public String updatedAt;

        @SerializedName("updated_at_relative")
        public String updatedAtRelative;

        @SerializedName("created_at")
        public String createdAt;

        @SerializedName("created_at_relative")
        public String createdAtRelative;

        @SerializedName("expired_at")
        public String expiredAt;

        @SerializedName("expired_at_relative")
        public String expiredAtRelatve;
    }

    public FilebinBinMeta bin = new FilebinBinMeta();
    public List<FilebinFileMeta> files = new ArrayList<>();
    
    // For gson
    public FilebinMeta() {};
}
