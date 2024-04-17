package com.igrium.datadownload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.datadownload.filebin.FilebinApi;

public class DataDownload implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("data-download");
    private static DataDownload instance;

    public static DataDownload getInstance() {
        return instance;
    }

    private FilebinApi filebin;

    @Override
    public void onInitialize() {
        instance = this;

        try {
            filebin = new FilebinApi("https://filebin.net");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        CommandRegistrationCallback.EVENT.register(DataCommandExtras::register);
    }

    public FilebinApi getFilebin() {
        return filebin;
    }
}