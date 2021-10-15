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
public enum ServerStatusCode {

    TWEET_HAS_NO_IMAGES(-9, "Tweet has no images, cannot add to ArtRetweeter."),
    TWITTER_API_ERROR(-8, "Twitter API returned an error."),
    SERVER_ERROR(-7, "ArtRetweeter server returned an error."),
    INVALID_INPUT(-6, "Invalid input."),
    DATABASE_ERROR(-5, "Database error"),
    INVALID_ACCESS_TOKEN(-4, "Invalid or expired access token."),
    TWITTER_RATE_LIMIT_EXCEEDED(-3, "Twitter rate limit exceeded."),
    ARTRETWEETER_RATE_LIMIT_EXCEEDED(-2, "ArtRetweeter rate limit exceeded."),
    USER_BANNED(-1, "User is banned from ArtRetweeter."),
    QUERY_OK(0, "Query OK.");

    private final Integer statusCode;
    private final String statusMessage;

    public Integer getStatusCode() {
        return statusCode;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }

    private ServerStatusCode(Integer statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public static ServerStatusCode getCodeByInteger(Integer statusCode) {
        ServerStatusCode[] codes = ServerStatusCode.values();
        for (ServerStatusCode code : codes) {
            if (code.getStatusCode().equals(statusCode)) {
                return code;
            }
        }
        return null;
    }

    public boolean isErrorStatus() {
        return !this.equals(QUERY_OK);
    }

}
