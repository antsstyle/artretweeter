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
public class RetweetQueueEntry {
    
    private Integer id;
    private Long tweetID;
    private Integer internalAccountID;
    private Timestamp retweetTime;
    private Integer errorCode;

    public Integer getErrorCode() {
        return errorCode;
    }

    public RetweetQueueEntry setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public String getFailReason() {
        return failReason;
    }

    public RetweetQueueEntry setFailReason(String failReason) {
        this.failReason = failReason;
        return this;
    }
    private String failReason;

    public Integer getId() {
        return id;
    }

    public RetweetQueueEntry setId(Integer id) {
        this.id = id;
        return this;
    }

    public Long getTweetID() {
        return tweetID;
    }

    public RetweetQueueEntry setTweetID(Long tweetID) {
        this.tweetID = tweetID;
        return this;
    }

    public Integer getInternalAccountID() {
        return internalAccountID;
    }

    public RetweetQueueEntry setInternalAccountID(Integer internalAccountID) {
        this.internalAccountID = internalAccountID;
        return this;
    }

    public Timestamp getRetweetTime() {
        return retweetTime;
    }

    public RetweetQueueEntry setRetweetTime(Timestamp retweetTime) {
        this.retweetTime = retweetTime;
        return this;
    }
    
}
