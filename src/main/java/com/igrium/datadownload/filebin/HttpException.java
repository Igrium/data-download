package com.igrium.datadownload.filebin;

import java.io.IOException;

public class HttpException extends IOException {
    private final int statusCode;

    public HttpException(int statusCode) {
        super("Server returned HTTP status code " + statusCode);
        this.statusCode = statusCode;
    }
    
    public HttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpException(int statusCode, Throwable cause) {
        super("Server returned HTTP status code " + statusCode, cause);
        this.statusCode = statusCode;
    }

    public HttpException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
