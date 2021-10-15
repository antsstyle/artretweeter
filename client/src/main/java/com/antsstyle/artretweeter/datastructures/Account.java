/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

/**
 *
 * @author antss
 */
public class Account {
    
    private Integer id;

    public Integer getId() {
        return id;
    }

    public Account setId(Integer id) {
        this.id = id;
        return this;
    }
    private String token;
    private String tokenSecret;
    private Long twitterID;
    private String screenName;
    private Long maxID;
    private Long sinceID;
    private Boolean retrievedOldTweetsLimitFromTwitter;
    private Boolean retrievedAllOldTweetsFromServer;

    public Boolean getRetrievedAllOldTweetsFromServer() {
        return retrievedAllOldTweetsFromServer;
    }

    public Account setRetrievedAllOldTweetsFromServer(Boolean retrievedAllOldTweetsFromServer) {
        this.retrievedAllOldTweetsFromServer = retrievedAllOldTweetsFromServer;
        return this;
    }

    public Boolean getRetrievedOldTweetsLimitFromTwitter() {
        return retrievedOldTweetsLimitFromTwitter;
    }

    public Account setRetrievedOldTweetsLimitFromTwitter(Boolean retrievedOldTweetsLimitFromTwitter) {
        this.retrievedOldTweetsLimitFromTwitter = retrievedOldTweetsLimitFromTwitter;
        return this;
    }

    public Long getSinceID() {
        return sinceID;
    }

    public Account setSinceID(Long sinceID) {
        this.sinceID = sinceID;
        return this;
    }

    public Long getMaxID() {
        return maxID;
    }

    public Account setMaxID(Long maxID) {
        this.maxID = maxID;
        return this;
    }

    public Long getTwitterID() {
        return twitterID;
    }

    public Account setTwitterID(Long twitterID) {
        this.twitterID = twitterID;
        return this;
    }

    public String getScreenName() {
        return screenName;
    }

    public Account setScreenName(String screenName) {
        this.screenName = screenName;
        return this;
    }

    public String getToken() {
        return token;
    }

    public Account setToken(String token) {
        this.token = token;
        return this;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public Account setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
        return this;
    }
    
    @Override
    public String toString() {
        return screenName;
    }
    
}
