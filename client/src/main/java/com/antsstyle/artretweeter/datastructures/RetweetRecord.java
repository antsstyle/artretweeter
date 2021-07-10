/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import java.sql.Timestamp;

/**
 *
 * @author antss
 */
public class RetweetRecord {
    
    private Integer id;
    private Long tweetID;
    private Long userTwitterID;
    private Timestamp retweetTime;

    public Integer getId() {
        return id;
    }

    public RetweetRecord setId(Integer id) {
        this.id = id;
        return this;
    }

    public Long getTweetID() {
        return tweetID;
    }

    public RetweetRecord setTweetID(Long tweetID) {
        this.tweetID = tweetID;
        return this;
    }

    public Long getUserTwitterID() {
        return userTwitterID;
    }

    public RetweetRecord setUserTwitterID(Long userTwitterID) {
        this.userTwitterID = userTwitterID;
        return this;
    }

    public Timestamp getRetweetTime() {
        return retweetTime;
    }

    public RetweetRecord setRetweetTime(Timestamp retweetTime) {
        this.retweetTime = retweetTime;
        return this;
    }
    
}
