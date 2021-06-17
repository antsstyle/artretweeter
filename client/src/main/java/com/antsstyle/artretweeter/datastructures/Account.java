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
    private Long historicalMaxID;
    private Long latestMaxID;

    public Long getLatestMaxID() {
        return latestMaxID;
    }

    public Account setLatestMaxID(Long latestMaxID) {
        this.latestMaxID = latestMaxID;
        return this;
    }

    public Long getHistoricalMaxID() {
        return historicalMaxID;
    }

    public Account setHistoricalMaxID(Long historicalMaxID) {
        this.historicalMaxID = historicalMaxID;
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
