package com.igrium.datadownload;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataDownload implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("data-download");

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");
    }
}