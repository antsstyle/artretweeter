/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

import com.antsstyle.artretweeter.datastructures.TweetHolder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class TweetsDB {

    private static final Logger LOGGER = LogManager.getLogger(TweetsDB.class);

    private static final String RETWEET_QUEUE_MERGE_QUERY = "MERGE INTO retweetqueue USING (VALUES (?,?,?))"
            + " AS vals(tweetid,retweetingusertwitterid,retweettime) ON (retweetqueue.tweetid = vals.tweetid"
            + " AND retweetqueue.retweetingusertwitterid = vals.retweetingusertwitterid)"
            + " WHEN MATCHED THEN UPDATE SET retweetqueue.tweetid=vals.tweetid,retweetqueue.retweetingusertwitterid=vals.retweetingusertwitterid,"
            + " retweetqueue.retweettime=vals.retweettime"
            + " WHEN NOT MATCHED THEN INSERT (tweetid,retweetingusertwitterid,retweettime)"
            + " VALUES (vals.tweetid, vals.retweetingusertwitterid, vals.retweettime)";

    private static final String RETWEET_RECORDS_INSERT_QUERY = "MERGE INTO retweetrecords USING (VALUES (?,?,?))"
            + " AS vals(usertwitterid,tweetid,retweettime) ON (retweetrecords.tweetid = vals.tweetid"
            + " AND retweetrecords.usertwitterid = vals.usertwitterid AND retweetrecords.retweettime=vals.retweettime)"
            + " WHEN NOT MATCHED THEN INSERT (usertwitterid,tweetid,retweettime)"
            + " VALUES (vals.usertwitterid, vals.tweetid, vals.retweettime)";

    private static final String TWEET_MERGE_QUERY = "MERGE INTO tweets USING (VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?))"
            + " AS vals(tweetid,usertwitterid,screen_name,filepath1,filepath2,filepath3,filepath4,url1,"
            + "url2,url3,url4,created_at,likecount,retweetcount,fulltweettext,source) ON (tweets.tweetid = vals.tweetid) "
            + "WHEN MATCHED THEN UPDATE SET tweets.tweetid=vals.tweetid,tweets.usertwitterid=vals.usertwitterid,"
            + "tweets.screen_name=vals.screen_name,tweets.filepath1=vals.filepath1,"
            + "tweets.filepath2=vals.filepath2,tweets.filepath3=vals.filepath3,tweets.filepath4=vals.filepath4,"
            + "tweets.url1=vals.url1,tweets.url2=vals.url2,tweets.url3=vals.url3,tweets.url4=vals.url4,"
            + "tweets.created_at=vals.created_at,tweets.likecount=vals.likecount,tweets.retweetcount=vals.retweetcount,"
            + "tweets.fulltweettext=vals.fulltweettext,tweets.source=vals.source "
            + "WHEN NOT MATCHED THEN INSERT (tweetid,usertwitterid,screen_name,filepath1,filepath2,filepath3,filepath4,url1,"
            + "url2,url3,url4,created_at,likecount,retweetcount,fulltweettext,source) VALUES "
            + "(vals.tweetid,vals.usertwitterid,vals.screen_name,vals.filepath1,"
            + "vals.filepath2,vals.filepath3,vals.filepath4,vals.url1,vals.url2,vals.url3,vals.url4,vals.created_at,"
            + "vals.likecount,vals.retweetcount,vals.fulltweettext,vals.source) ";

    public static boolean insertRetweetQueueEntry(Object[] params) {
        return CoreDB.runCustomUpdate(RETWEET_QUEUE_MERGE_QUERY, params);
    }

    public static boolean insertTweet(Object[] params) {
        return CoreDB.runCustomUpdate(TWEET_MERGE_QUERY, params);
    }

    public static boolean insertRetweetRecordEntry(Object[] params) {
        return CoreDB.runCustomUpdate(RETWEET_RECORDS_INSERT_QUERY, params);
    }

    public static boolean insertRetweetRecordEntryBatch(ArrayList<Object[]> params) {
        return CoreDB.runParameterisedUpdateBatch(RETWEET_RECORDS_INSERT_QUERY, params);
    }

    public static boolean parameterisedTweetMergeBatch(ArrayList<Object[]> params) {
        return CoreDB.runParameterisedUpdateBatch(TWEET_MERGE_QUERY, params);
    }

    public static ArrayList<Long> getTweetIDsByDatabaseIDs(ArrayList<Integer> tweetDatabaseIDs) {
        if (tweetDatabaseIDs.isEmpty()) {
            return new ArrayList<>();
        }
        ArrayList<Object> params = new ArrayList<>(tweetDatabaseIDs);
        Object[] paramsArray = params.toArray(new Object[params.size()]);
        StringBuilder query = new StringBuilder("SELECT tweetid FROM tweets WHERE id IN (?");
        for (int i = 0; i < params.size() - 1; i++) {
            query = query.append(",?");
        }
        query = query.append(")");
        DBResponse resp = CoreDB.customQuerySelect(query.toString(), paramsArray);
        if (!resp.wasSuccessful()) {
            return null;
        }
        ArrayList<Long> tweetIDs = new ArrayList<>();
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            tweetIDs.add((Long) row.get("TWEETID"));
        }
        return tweetIDs;
    }

    public static boolean deleteTweetsUsingDatabaseIDs(ArrayList<Integer> tweetDatabaseIDs, boolean deletedFromTwitter) {
        if (tweetDatabaseIDs.isEmpty()) {
            return true;
        }
        ArrayList<Long> tweetIDs = getTweetIDsByDatabaseIDs(tweetDatabaseIDs);
        if (tweetIDs == null) {
            return false;
        }
        return deleteTweets(tweetIDs, deletedFromTwitter);
    }

    public static boolean deleteTweets(ArrayList<Long> tweetIDs, boolean deletedFromTwitter) {
        if (tweetIDs.isEmpty()) {
            return true;
        }
        ArrayList<Object> objectParams = new ArrayList<>();
        objectParams.addAll(tweetIDs);
        Object[] paramsArray = objectParams.toArray(new Object[objectParams.size()]);
        StringBuilder selectQuery = new StringBuilder("SELECT * FROM tweets WHERE tweetid IN (?");
        StringBuilder deleteTweetsQuery = new StringBuilder("DELETE FROM tweets WHERE tweetid IN (?");
        StringBuilder deleteCollectionTweetsQuery = new StringBuilder("DELETE FROM collectiontweets WHERE tweetid IN (?");
        for (int i = 0; i < tweetIDs.size() - 1; i++) {
            selectQuery = selectQuery.append(",?");
            deleteTweetsQuery = deleteTweetsQuery.append(",?");
            deleteCollectionTweetsQuery = deleteCollectionTweetsQuery.append(",?");
        }
        if (!deletedFromTwitter) {
            selectQuery = selectQuery.append(") AND tweetid NOT IN (SELECT tweetid FROM collectiontweets)");
            deleteTweetsQuery = deleteTweetsQuery.append(") AND tweetid NOT IN (SELECT tweetid FROM collectiontweets)");
        } else {
            deleteCollectionTweetsQuery = deleteCollectionTweetsQuery.append(")");
            deleteTweetsQuery = deleteTweetsQuery.append(")");
            selectQuery = selectQuery.append(")");
        }

        DBResponse selectResp = CoreDB.customQuerySelect(selectQuery.toString(), paramsArray);
        if (!selectResp.wasSuccessful()) {
            LOGGER.error("Error retrieving tweets from database - aborting deletion.");
            return false;
        }
        if (selectResp.getReturnedRows().isEmpty()) {
            LOGGER.error("Tweets to delete could not be found in database - aborting deletion.");
            return true;
        }

        ArrayList<HashMap<String, Object>> rows = selectResp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            TweetHolder tweet = ResultSetConversion.getTweet(row);
            for (Path p : tweet.getFilePaths()) {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception e) {
                    LOGGER.error("Failed to delete image with path: " + p, e);
                }
            }
        }

        CoreDB.runCustomUpdate(deleteTweetsQuery.toString(), paramsArray);
        if (deletedFromTwitter) {
            CoreDB.runCustomUpdate(deleteCollectionTweetsQuery.toString(), paramsArray);
        }
        return true;
    }

}
