/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.enumerations;

/**
 *
 * @author antss
 */
public enum StatusCode {

    DB_ERROR(-2, "Database error", true),
    DIRECTORY_CREATION_ERROR(-3, "Failed to create necessary directories in file system", true),
    DOWNLOAD_ERROR(-4, "Download failed", true),
    INTERRUPTED_ERROR(-5, "Process was interrupted before finishing", true),
    JSON_PARSE_ERROR(-6, "Failed to parse JSON response", true),
    MAX_DOWNLOAD_RETRY_REACHED(-11, "Max number of download retries reached, download aborted", true),
    MISC_ERROR(-7, "Miscellaneous error", true),
    MISSING_CREDENTIALS_ERROR(-8, "User credentials were required, but missing", true),
    SUCCESS(1, "Operation successful", false),
    TWEET_HAS_NO_IMAGES(3, "Tweet has no images to download", false),
    RATE_LIMIT_EXCEEDED_ERROR(-9, "Rate limit for this endpoint has been exceeded", true),
    TWITTER_API_ERROR(-10, "Twitter API returned an error", true),
    FILE_ALREADY_DOWNLOADED(2, "File already downloaded and overwrite flag not set", false),
    CONNECTION_ERROR(-12, "Connection error", true);

    private final Integer statusCode;
    private final String statusMessage;
    private final Boolean isErrorStatus;

    public Boolean isErrorStatus() {
        return isErrorStatus;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    private StatusCode(Integer statusCode, String statusMessage, Boolean isErrorStatus) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.isErrorStatus = isErrorStatus;
    }

}
