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
public enum ClientStatusCode {
    
    FILE_ALREADY_DOWNLOADED(-8, "File already downloaded and overwrite flag not set.", false),
    MAX_DOWNLOAD_RETRY_REACHED(-7, "Max number of download retries reached, download aborted.", true),
    DOWNLOAD_ERROR(-6, "Download error.", true),
    DATABASE_ERROR(-5, "Database error.", true),
    MISC_ERROR(-4, "Misc error.", true),
    INTERRUPTED(-3, "Interrupted.", true),
    MISSING_CREDENTIALS(-2, "Missing credentials.", true),
    JSON_PARSE_ERROR(-1, "JSON parse error.", true),
    QUERY_OK(0, "Query OK.", false);

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

    private ClientStatusCode(Integer statusCode, String statusMessage, Boolean isErrorStatus) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.isErrorStatus = isErrorStatus;
    }

}
