/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 *
 * @author antss
 */
public class OperationResult {

    public static final Integer QUERY_OK_NO_IMAGES_IN_TWEET = 2;
    public static final Integer JSON_PARSE_ERROR = -8;
    public static final Integer TWITTER_API_ERROR = -2;
    public static final Integer QUERY_OK = 1;
    public static final Integer DB_ERROR = -1;
    public static final Integer DIRECTORY_CREATION_ERROR = -4;
    public static final Integer ARTRETWEETER_SERVER_ERROR = -6;
    public static final Integer DOWNLOAD_ERROR = -3;
    public static final Integer INTERRUPTED_ERROR = -7;
    public static final Integer MISC_ERROR = -5;
    public static final Integer MISSING_CREDENTIALS_ERROR = -9;
    public static final Integer RATE_LIMIT_EXCEEDED_ERROR = -10;

    private Integer artRetweeterStatusCode;
    private Integer httpStatusCode;
    private Object returnedObject;
    private Integer receivedTweetCount = 0;
    private Integer storedTweetCount = 0;

    private JsonObject responseJSONObject;
    private JsonArray responseJSONArray;

    public String getLogMessage() {
        String msg = getReadableStatusCode();
        if (artRetweeterStatusMessage != null) {
            msg = msg.concat(" ").concat(artRetweeterStatusMessage);
        }
        return msg;
    }

    public String getReadableStatusCode() {
        if (artRetweeterStatusCode == null) {
            return "null";
        } else if (artRetweeterStatusCode.equals(QUERY_OK_NO_IMAGES_IN_TWEET)) {
            return "Query successful - no images were present in the returned tweet.";
        } else if (artRetweeterStatusCode.equals(JSON_PARSE_ERROR)) {
            return "Failed to parse JSON response.";
        } else if (artRetweeterStatusCode.equals(QUERY_OK)) {
            return "Query successful.";
        } else if (artRetweeterStatusCode.equals(DB_ERROR)) {
            return "Database error.";
        } else if (artRetweeterStatusCode.equals(DIRECTORY_CREATION_ERROR)) {
            return "Failed to create necessary directories in file system.";
        } else if (artRetweeterStatusCode.equals(ARTRETWEETER_SERVER_ERROR)) {
            return "ArtRetweeter server returned an error.";
        } else if (artRetweeterStatusCode.equals(DOWNLOAD_ERROR)) {
            return "Download failed.";
        } else if (artRetweeterStatusCode.equals(INTERRUPTED_ERROR)) {
            return "Process was interrupted before finishing.";
        } else if (artRetweeterStatusCode.equals(TWITTER_API_ERROR)) {
            return "Twitter API returned an error.";
        } else if (artRetweeterStatusCode.equals(MISC_ERROR)) {
            return "Miscellaneous error.";
        } else if (artRetweeterStatusCode.equals(MISSING_CREDENTIALS_ERROR)) {
            return "User credentials were required, but missing.";
        } else if (artRetweeterStatusCode.equals(RATE_LIMIT_EXCEEDED_ERROR)) {
            return "Rate limit for this endpoint has been exceeded.";
        } else {
            return "Unknown status code";
        }
    }

    public JsonArray getResponseJSONArray() {
        return responseJSONArray;
    }

    public OperationResult setResponseJSONArray(JsonArray responseJSONArray) {
        this.responseJSONArray = responseJSONArray;
        return this;
    }
    private JsonObject errorJSON;

    public JsonObject getResponseJSONObject() {
        return responseJSONObject;
    }

    public OperationResult setResponseJSONObject(JsonObject responseJSONObject) {
        this.responseJSONObject = responseJSONObject;
        return this;
    }

    public JsonObject getErrorJSON() {
        return errorJSON;
    }

    public OperationResult setErrorJSON(JsonObject errorJSON) {
        this.errorJSON = errorJSON;
        return this;
    }

    public JsonObject getHeaderJSON() {
        return headerJSON;
    }

    public OperationResult setHeaderJSON(JsonObject headerJSON) {
        this.headerJSON = headerJSON;
        return this;
    }
    private JsonObject headerJSON;

    public Integer getReceivedTweetCount() {
        return receivedTweetCount;
    }

    public OperationResult setReceivedTweetCount(Integer receivedTweetCount) {
        this.receivedTweetCount = receivedTweetCount;
        return this;
    }

    public Integer getStoredTweetCount() {
        return storedTweetCount;
    }

    public OperationResult setStoredTweetCount(Integer storedTweetCount) {
        this.storedTweetCount = storedTweetCount;
        return this;
    }

    public Object getReturnedObject() {
        return returnedObject;
    }

    public OperationResult setReturnedObject(Object returnedObject) {
        this.returnedObject = returnedObject;
        return this;
    }

    public Integer getArtRetweeterStatusCode() {
        return artRetweeterStatusCode;
    }

    public OperationResult setArtRetweeterStatusCode(Integer artRetweeterStatusCode) {
        this.artRetweeterStatusCode = artRetweeterStatusCode;
        return this;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public OperationResult setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
        return this;
    }

    public Integer getTwitterErrorCode() {
        return twitterErrorCode;
    }

    public OperationResult setTwitterErrorCode(Integer twitterErrorCode) {
        this.twitterErrorCode = twitterErrorCode;
        return this;
    }

    public String getArtRetweeterStatusMessage() {
        return artRetweeterStatusMessage;
    }

    public OperationResult setArtRetweeterStatusMessage(String artRetweeterStatusMessage) {
        this.artRetweeterStatusMessage = artRetweeterStatusMessage;
        return this;
    }
    private Integer twitterErrorCode;
    private String artRetweeterStatusMessage;
    private String twitterStatusMessage;

    public String getTwitterStatusMessage() {
        return twitterStatusMessage;
    }

    public OperationResult setTwitterStatusMessage(String twitterStatusMessage) {
        this.twitterStatusMessage = twitterStatusMessage;
        return this;
    }

}
