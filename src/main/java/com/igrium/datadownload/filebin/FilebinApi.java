package com.igrium.datadownload.filebin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.igrium.datadownload.filebin.FilebinMeta.FilebinBinMeta;
import com.igrium.datadownload.filebin.FilebinMeta.FilebinFileMeta;

import net.minecraft.util.Util;

public class FilebinApi {
    private final URI url;
    private final HttpClient client;

    private final Gson gson = new GsonBuilder().create();

    private final Logger logger;

    public FilebinApi(URI url) {
        this.url = url;
        client = HttpClient.newBuilder()
                .followRedirects(Redirect.ALWAYS)
                .executor(Util.getIoWorkerExecutor())
                .build();

        logger = LoggerFactory.getLogger("Filebin API (" + url + ")");
    }

    public FilebinApi(String url) throws URISyntaxException {
        this(new URI(url));
    }

    public HttpClient getClient() {
        return client;
    }

    public URI getUrl() {
        return url;
    }

    public Logger getLogger() {
        return logger;
    }

    public URI getFile(String bin, String filename) {
        return getUrl().resolve(bin + "/" + filename);
    }

    public URI getBin(String bin) {
        return getUrl().resolve(bin);
    }

    public CompletableFuture<FilebinMeta> getBinMeta(String bin) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(getBin(bin))
                .GET()
                .header("accept", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(res -> {
            if (res.statusCode() >= 400) {
                throw new CompletionException(new HttpException(res.statusCode()));
            }
            return gson.fromJson(res.body(), FilebinMeta.class);
        });
    }

    public InputStream download(String bin, String file) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(getFile(bin, file))
                .GET()
                .build();
        
        HttpResponse<InputStream> res;
        try {
            res = client.send(request, BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            throw new IOException("HTTP request was interrupted.", e);
        }

        if (res.statusCode() >= 400) {
            throw new HttpException(res.statusCode());
        }
        return res.body();
    }

    public static class FilebinUploadResponse {
        public FilebinBinMeta bin = new FilebinBinMeta();
        public FilebinFileMeta file = new FilebinFileMeta();
    }
    
    public CompletableFuture<FilebinUploadResponse> upload(String bin, String file,
            BodyPublisher data) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(getFile(bin, file))
                .POST(data)
                .header("accept", "application/json")
                .build();

        return client.sendAsync(request, BodyHandlers.ofString()).thenApply(res -> {
            if (res.statusCode() >= 400) {
                throw new CompletionException(new HttpException(res.statusCode()));
            }
            return gson.fromJson(res.body(), FilebinUploadResponse.class);
        });
    }

    public CompletableFuture<FilebinUploadResponse> upload(String bin, String file, byte[] data) {
        return upload(bin, file, BodyPublishers.ofByteArray(data));
    }

    public static Throwable decomposeCompletionException(Throwable e) {
        if (e instanceof CompletionException) {
            Throwable cause = e.getCause();
            if (cause != null)
                return decomposeCompletionException(cause);
        }
        return e;
    }
}
