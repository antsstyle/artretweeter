/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.queues;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.RetweetQueueEntry;
import com.antsstyle.artretweeter.datastructures.StatusJSON;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.twitter.RESTAPI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class RetweetQueue implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(RetweetQueue.class);
    private static final RetweetQueue queue = new RetweetQueue();

    private Thread queueThread = new Thread();

    private boolean hasFinished = false;
    private boolean keepRunning = true;

    /**
     * Starts this queue, or safely shuts it down if it is running.
     */
    public void setQueueStatus() {
        if (isRunning()) {
            safelyShutDown();
        } else {
            startQueue();
        }
    }

    public static RetweetQueue getInstance() {
        return queue;
    }

    public boolean isRunning() {
        return queueThread.isAlive();
    }

    /**
     * Shuts down this queue by sending an interrupt to the thread and setting keepRunning to false, allowing it to finish its current task before terminating.
     *
     * @return True once the thread finishes.
     */
    public final boolean safelyShutDown() {
        if (!hasFinished) {
            keepRunning = false;
            interruptQueueThread();
            int millisElapsed = 0;
            while (!hasFinished) {
                if (millisElapsed % 10000 == 0 && millisElapsed > 0) {
                    LOGGER.info("Waiting for queue to terminate (" + (millisElapsed / 1000) + "seconds waited so far)...");
                }
                try {
                    Thread.sleep(10);
                    millisElapsed += 10;
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted during shutdown!", e);
                }
            }
        }
        return true;
    }

    protected void interruptQueueThread() {
        if (queueThread.isAlive()) {
            queueThread.interrupt();
        }
    }

    public void startQueue() {
        if (!queueThread.isAlive()) {
            if (hasFinished) {
                queueThread = new Thread(queue);
                queueThread.setName("Twitter Queue Thread");
                queueThread.start();
            } else {
                queueThread.setName("Twitter Queue Thread");
                queueThread.start();
            }
        }
    }

    @Override
    public void run() {
        boolean finishedWithError = false;
        hasFinished = false;
        String errorMessage = null;
        LOGGER.info("Retweet queue started.");
        String nextTimeQuery = "SELECT * FROM retweetqueue ORDER BY retweettime ASC";
        DBResponse resp = CoreDB.customQuerySelect(nextTimeQuery);
        Timestamp nextRetweetTime;
        Long millisToNextPost = Long.MAX_VALUE;
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to get first post for retweet queue - aborting.");
            hasFinished = true;
            return;
        }
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        if (!rows.isEmpty()) {
            nextRetweetTime = (Timestamp) rows.get(0).get("retweettime");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            if (now.after(nextRetweetTime)) {

            } else {
                millisToNextPost = nextRetweetTime.getTime() - now.getTime();
            }
        }
        int consecutiveErrorCount = 0;
        while (keepRunning) {
            if (consecutiveErrorCount >= 5) {
                keepRunning = false;
                finishedWithError = true;
                break;
            }
            try {
                Thread.sleep(Math.min(60 * 1000, millisToNextPost));
            } catch (Exception e) {
                continue;
            }
            resp = CoreDB.customQuerySelect(nextTimeQuery);
            if (!resp.wasSuccessful()) {
                LOGGER.error("Failed to get posts for retweet queue - aborting.");
                finishedWithError = true;
                keepRunning = false;
                continue;
            }
            rows = resp.getReturnedRows();
            if (rows.isEmpty()) {
                millisToNextPost = Long.MAX_VALUE;
                continue;
            }
            Timestamp now = new Timestamp(System.currentTimeMillis());
            boolean retweetFound = false;
            for (HashMap<String, Object> row : rows) {
                if (retweetFound) {
                    break;
                }
                RetweetQueueEntry entry = ResultSetConversion.getRetweetQueueEntry(row);
                if (entry.getRetweetTime().after(now) || entry.getRetweetTime().equals(now)) {
                    Long differenceMillis = entry.getRetweetTime().getTime() - now.getTime();
                    // If we are more than five minutes off, don't post this retweet - cancel it
                    if (differenceMillis >= (5 * 60 * 1000)) {
                        CoreDB.deleteFromTable(DBTable.RETWEETQUEUE,
                                new String[]{"id"},
                                new Object[]{entry.getId()});
                    } else {
                        DBResponse accountResp = CoreDB.selectFromTable(DBTable.ACCOUNTS,
                                new String[]{"id"},
                                new Object[]{entry.getInternalAccountID()});
                        Account account;
                        if (!accountResp.wasSuccessful()) {
                            keepRunning = false;
                            break;
                        } else if (accountResp.getReturnedRows().isEmpty()) {
                            keepRunning = false;
                            break;
                        }
                        account = ResultSetConversion.getAccount(accountResp.getReturnedRows().get(0));
                        DBResponse tweetResp = CoreDB.selectFromTable(DBTable.TWEETS,
                                new String[]{"tweetid"},
                                new Object[]{entry.getTweetID()});
                        if (!tweetResp.wasSuccessful()) {

                        } else if (tweetResp.getReturnedRows().isEmpty()) {

                        }
                        TweetHolder tweet = ResultSetConversion.getTweet(tweetResp.getReturnedRows().get(0));
                        OperationResult tweetResult = RESTAPI.getTweetByID(tweet.getTweetID(), account, null, false);
                        if (!tweetResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {

                        }

                        OperationResult retweetResult = RESTAPI.statusesRetweet(entry.getTweetID(), account);
                        if (!retweetResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
                            switch (retweetResult.getTwitterErrorCode()) {
                                case RESTAPI.ALREADY_RETWEETED:
                                case RESTAPI.DUPLICATE_TWEET:
                                    CoreDB.deleteFromTable(DBTable.RETWEETQUEUE,
                                            new String[]{"id"},
                                            new Object[]{entry.getId()});
                                    break;
                                case RESTAPI.TWITTER_INTERNAL_ERROR:
                                    // Wait one minute and try again
                                    consecutiveErrorCount++;
                                    break;
                                case RESTAPI.TWITTER_OVER_CAPACITY:
                                    // Wait one minute and try again
                                    consecutiveErrorCount++;
                                    break;
                                case RESTAPI.TWEET_NOT_FOUND:
                                    CoreDB.deleteFromTable(DBTable.RETWEETQUEUE,
                                            new String[]{"id"},
                                            new Object[]{entry.getId()});
                                    break;
                                case RESTAPI.RATE_LIMIT_EXCEEDED:
                                    keepRunning = false;
                                    LOGGER.error("Rate limit exceeded!");
                                    finishedWithError = true;
                                    break;
                                default:
                                    LOGGER.error("Unrecognised error - aborting retweet queue.");
                                    LOGGER.error("Error was: " + retweetResult.getTwitterErrorCode()
                                            + ", HTTP status code was: " + retweetResult.getHttpStatusCode());
                                    keepRunning = false;
                                    finishedWithError = true;
                                    break;
                            }

                            break;
                        } else {
                            StatusJSON retweetedStatus = (StatusJSON) tweetResult.getReturnedObject();
                            Object[] params = new Object[]{entry.getInternalAccountID(), entry.getTweetID(), tweet.getCreatedAt(),
                                retweetedStatus.getId(), entry.getRetweetTime()};
                            CoreDB.insertRetweetEntry(params);
                            CoreDB.deleteFromTable(DBTable.RETWEETQUEUE,
                                    new String[]{"id"},
                                    new Object[]{entry.getId()});
                            consecutiveErrorCount = 0;
                        }
                    }
                    break;
                }

            }

        }
        hasFinished = true;
        keepRunning = true;
        if (finishedWithError) {
            GUI.getQueuingPanel().setRetweetQueueStatus("Failed");
            
        } else {
            GUI.getQueuingPanel().setRetweetQueueStatus("Stopped");
        }
    }

}
