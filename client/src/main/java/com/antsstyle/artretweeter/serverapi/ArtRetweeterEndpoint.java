/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.serverapi;

/**
 *
 * @author antss
 */
public enum ArtRetweeterEndpoint {

    QUEUE_STATUS("retweets/queuestatus"),
    QUEUE_RETWEET("retweets/queue"),
    REMOVE_ACCOUNT("accounts/remove"),
    TWEET_RETWEET_STATUS("retweets/tweetretweetstatus"),
    UNQUEUE_RETWEET("retweets/unqueue");

    private final String endpointName;

    public String getEndpointName() {
        return endpointName;
    }

    private ArtRetweeterEndpoint(String endpointName) {
        this.endpointName = endpointName;
    }

}
