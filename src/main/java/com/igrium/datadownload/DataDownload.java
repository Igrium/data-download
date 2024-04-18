package com.igrium.datadownload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private DataDownloadConfig config;

    @Override
    public void onInitialize() {
        instance = this;

       loadConfig();

        try {
            filebin = new FilebinApi(config.filebinUrl);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        CommandRegistrationCallback.EVENT.register(DataCommandExtras::register);
    }

    public DataDownloadConfig getConfig() {
        return config;
    }

    public FilebinApi getFilebin() {
        return filebin;
    }

    private void loadConfig() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("data-download.json");
        if (Files.isRegularFile(configFile)) {

            try(BufferedReader reader = Files.newBufferedReader(configFile)) {
                config = DataDownloadConfig.fromJson(reader);
                return;
            } catch (Exception e) {
                LOGGER.error("Unable to load Data Download config. Regenerating...", e);
            }
        } 
        config = new DataDownloadConfig();
        try(BufferedWriter writer = Files.newBufferedWriter(configFile)) {
            writer.write(config.toJson());
        } catch (Exception e) {
            LOGGER.error("Unable to save Data Download config.", e);
        }

    }
}