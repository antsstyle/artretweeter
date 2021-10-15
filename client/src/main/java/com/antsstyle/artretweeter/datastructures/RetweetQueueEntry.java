/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import java.sql.Timestamp;
import java.util.Objects;

/**
 *
 * @author antss
 */
public class RetweetQueueEntry {
    
    private Integer id;
    private Long tweetID;
    private Long retweetingUserTwitterID;
    private Boolean automated;

    public Boolean getAutomated() {
        return automated;
    }

    public RetweetQueueEntry setAutomated(Boolean automated) {
        this.automated = automated;
        return this;
    }

    public Long getRetweetingUserTwitterID() {
        return retweetingUserTwitterID;
    }

    public RetweetQueueEntry setRetweetingUserTwitterID(Long retweetingUserTwitterID) {
        this.retweetingUserTwitterID = retweetingUserTwitterID;
        return this;
    }
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

    public Timestamp getRetweetTime() {
        return retweetTime;
    }

    public RetweetQueueEntry setRetweetTime(Timestamp retweetTime) {
        this.retweetTime = retweetTime;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RetweetQueueEntry)) {
            return false;
        }
        RetweetQueueEntry otherEntry = (RetweetQueueEntry) o;
        return (this.retweetTime.equals(otherEntry.getRetweetTime()) && this.getTweetID().equals(otherEntry.getTweetID())
                && this.getRetweetingUserTwitterID().equals(otherEntry.getRetweetingUserTwitterID()));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.tweetID);
        hash = 29 * hash + Objects.hashCode(this.retweetingUserTwitterID);
        hash = 29 * hash + Objects.hashCode(this.retweetTime);
        return hash;
    }
    
}
