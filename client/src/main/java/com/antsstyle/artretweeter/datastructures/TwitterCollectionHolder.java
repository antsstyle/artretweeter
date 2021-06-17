/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import java.util.ArrayList;

/**
 *
 * @author antss
 */
public class TwitterCollectionHolder {
    
    private Integer databaseID;
    
    private String twitterURL;

    public String getTwitterURL() {
        return twitterURL;
    }

    public TwitterCollectionHolder setTwitterURL(String twitterURL) {
        this.twitterURL = twitterURL;
        return this;
    }

    public Integer getDatabaseID() {
        return databaseID;
    }

    public TwitterCollectionHolder setDatabaseID(Integer databaseID) {
        this.databaseID = databaseID;
        return this;
    }
    private String twitterID;
    private String name;
    private String description;
    private String collectionURL;
    private CollectionOrdering ordering;

    public CollectionOrdering getOrdering() {
        return ordering;
    }

    public TwitterCollectionHolder setOrdering(CollectionOrdering ordering) {
        this.ordering = ordering;
        return this;
    }

    public String getCollectionURL() {
        return collectionURL;
    }

    public TwitterCollectionHolder setCollectionURL(String collectionURL) {
        this.collectionURL = collectionURL;
        return this;
    }

    public String getName() {
        return name;
    }

    public TwitterCollectionHolder setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TwitterCollectionHolder setDescription(String description) {
        this.description = description;
        return this;
    }
    private ArrayList<TweetHolder> tweets;

    public ArrayList<TweetHolder> getTweets() {
        return tweets;
    }

    public TwitterCollectionHolder setTweets(ArrayList<TweetHolder> tweets) {
        this.tweets = tweets;
        return this;
    }

    public String getTwitterID() {
        return twitterID;
    }

    public TwitterCollectionHolder setTwitterID(String twitterID) {
        this.twitterID = twitterID;
        return this;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
