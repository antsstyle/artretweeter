/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import com.antsstyle.artretweeter.enumerations.ServerStatusCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 *
 * @author antss
 */
public class TwitterResponse extends Response {

    private Integer receivedTweetCount = 0;
    private Integer storedTweetCount = 0;
    private JsonArray responseJSONArray;
    private JsonElement errorJSON;
    private JsonElement headerJSON;
    private String twitterErrorMessage;
    private Integer twitterErrorCode;
    
    public TwitterResponse(ServerStatusCode twitterStatusCode) {
        this.serverStatusCode = twitterStatusCode;
    }

    public Integer getTwitterErrorCode() {
        return twitterErrorCode;
    }

    public void setTwitterErrorCode(Integer twitterErrorCode) {
        this.twitterErrorCode = twitterErrorCode;
    }
    
    public JsonArray getResponseJSONArray() {
        return responseJSONArray;
    }

    public void setResponseJSONArray(JsonArray responseJSONArray) {
        this.responseJSONArray = responseJSONArray;
    }

    public JsonElement getErrorJSON() {
        return errorJSON;
    }

    public void setErrorJSON(JsonElement errorJSON) {
        this.errorJSON = errorJSON;
    }

    public JsonElement getHeaderJSON() {
        return headerJSON;
    }

    public void setHeaderJSON(JsonElement headerJSON) {
        this.headerJSON = headerJSON;
    }

    public Integer getReceivedTweetCount() {
        return receivedTweetCount;
    }

    public void setReceivedTweetCount(Integer receivedTweetCount) {
        this.receivedTweetCount = receivedTweetCount;
    }

    public Integer getStoredTweetCount() {
        return storedTweetCount;
    }

    public void setStoredTweetCount(Integer storedTweetCount) {
        this.storedTweetCount = storedTweetCount;
    }

    public String getTwitterErrorMessage() {
        return twitterErrorMessage;
    }

    public void setTwitterErrorMessage(String twitterErrorMessage) {
        this.twitterErrorMessage = twitterErrorMessage;
    }

}
