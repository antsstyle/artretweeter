/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package antsstyle.artretweeter.datastructures;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 *
 * @author antss
 */
public class TweetHolder {

    private Integer id;
    private Long tweetID;
    private Long userTwitterID;
    private String screenName;
    private ArrayList<Path> filePaths;
    private String fullTweetText;
    private ArrayList<String> mediaURLs;
    private Integer likeCount;
    private Integer retweetCount;
    private Timestamp createdAt;
    
    public Long getUserTwitterID() {
        return userTwitterID;
    }
    public TweetHolder setUserTwitterID(Long userTwitterID) {
        this.userTwitterID = userTwitterID;
        return this;
    }
    public String getScreenName() {
        return screenName;
    }
    public TweetHolder setScreenName(String screenName) {
        this.screenName = screenName;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public TweetHolder setId(Integer id) {
        this.id = id;
        return this;
    }

    public Long getTweetID() {
        return tweetID;
    }

    public TweetHolder setTweetID(Long tweetID) {
        this.tweetID = tweetID;
        return this;
    }

    public ArrayList<Path> getFilePaths() {
        return filePaths;
    }

    public TweetHolder setFilePaths(ArrayList<Path> filePaths) {
        this.filePaths = filePaths;
        return this;
    }

    public String getFullTweetText() {
        return fullTweetText;
    }

    public TweetHolder setFullTweetText(String fullTweetText) {
        this.fullTweetText = fullTweetText;
        return this;
    }

    public ArrayList<String> getMediaURLs() {
        return mediaURLs;
    }

    public TweetHolder setMediaURLs(ArrayList<String> mediaURLs) {
        this.mediaURLs = mediaURLs;
        return this;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public TweetHolder setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
        return this;
    }

    public Integer getRetweetCount() {
        return retweetCount;
    }

    public TweetHolder setRetweetCount(Integer retweetCount) {
        this.retweetCount = retweetCount;
        return this;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public TweetHolder setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
        return this;
    }

}
