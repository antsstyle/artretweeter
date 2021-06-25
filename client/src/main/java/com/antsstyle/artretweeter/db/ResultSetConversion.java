/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.RetweetQueueEntry;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author antss
 */
public class ResultSetConversion {
    
    public static RetweetQueueEntry getRetweetQueueEntry(HashMap<String, Object> row) {
        RetweetQueueEntry entry = new RetweetQueueEntry()
                .setId((Integer) row.get("ID"))
                .setInternalAccountID((Integer) row.get("INTERNALACCOUNTID"))
                .setRetweetTime((Timestamp) row.get("RETWEETTIME"))
                .setTweetID((Long) row.get("TWEETID"));
        return entry;
    }
    
    public static RetweetQueueEntry getFailedRetweetQueueEntry(HashMap<String, Object> row) {
        RetweetQueueEntry entry = new RetweetQueueEntry()
                .setId((Integer) row.get("ID"))
                .setInternalAccountID((Integer) row.get("INTERNALACCOUNTID"))
                .setRetweetTime((Timestamp) row.get("RETWEETTIME"))
                .setTweetID((Long) row.get("TWEETID"))
                .setErrorCode((Integer) row.get("ERRORCODE"))
                .setFailReason((String) row.get("FAILREASON"));
        return entry;
    }

    /**
     * Note this method does not hydrate the tweets list for the collection.
     * @param row The database result row.
     * @return A TwitterCollectionHolder object representing the collection.
     */
    public static TwitterCollectionHolder getTwitterCollection(HashMap<String, Object> row) {
        TwitterCollectionHolder holder = new TwitterCollectionHolder()
                .setDatabaseID((Integer) row.get("ID"))
                .setDescription((String) row.get("DESCRIPTION"))
                .setName((String) row.get("NAME"))
                .setCollectionURL((String) row.get("COLLECTIONURL"))
                .setTwitterID((String) row.get("COLLECTIONID"));
        return holder;
    }

    public static Account getAccount(HashMap<String, Object> row) {
        Account account = new Account()
                .setId((Integer) row.get("ID"))
                .setScreenName((String) row.get("SCREEN_NAME"))
                .setTwitterID((Long) row.get("TWITTERID"))
                .setToken((String) row.get("TOKEN"))
                .setTokenSecret((String) row.get("TOKENSECRET"))
                .setHistoricalMaxID((Long) row.get("HISTORICALMAXID"))
                .setLatestMaxID((Long) row.get("LATESTMAXID"));
        return account;
    }

    public static TweetHolder getTweet(HashMap<String, Object> row) {
        ArrayList<Path> filePaths = new ArrayList<>();
        ArrayList<String> urls = new ArrayList<>();
        TweetHolder tweet = new TweetHolder()
                .setId((Integer) row.get("ID"))
                .setFullTweetText((String) row.get("FULLTWEETTEXT"))
                .setLikeCount((Integer) row.get("LIKECOUNT"))
                .setRetweetCount((Integer) row.get("RETWEETCOUNT"))
                .setCreatedAt((Timestamp) row.get("CREATED_AT"))
                .setTweetID((Long) row.get("TWEETID"))
                .setUserTwitterID((Long) row.get("USERTWITTERID"))
                .setScreenName((String) row.get("SCREEN_NAME"));
        filePaths.add(Paths.get((String) row.get("FILEPATH1")));
        String filePath2String = (String) row.get("FILEPATH2");
        String filePath3String = (String) row.get("FILEPATH3");
        String filePath4String = (String) row.get("FILEPATH4");
        if (filePath2String != null) {
            filePaths.add(Paths.get(filePath2String));
        }
        if (filePath3String != null) {
            filePaths.add(Paths.get(filePath3String));
        }
        if (filePath4String != null) {
            filePaths.add(Paths.get(filePath4String));
        }
        urls.add((String) row.get("URL1"));
        String url2 = (String) row.get("URL2");
        String url3 = (String) row.get("URL3");
        String url4 = (String) row.get("URL4");
        if (url2 != null) {
            urls.add(url2);
        }
        if (url3 != null) {
            urls.add(url2);
        }
        if (url4 != null) {
            urls.add(url2);
        }
        tweet.setFilePaths(filePaths);
        tweet.setMediaURLs(urls);
        return tweet;
    }

}
