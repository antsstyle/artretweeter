/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.queues;

import com.antsstyle.artretweeter.configuration.TwitterConfig;
import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.CachedVariable;
import com.antsstyle.artretweeter.datastructures.ConfigItem;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.RetweetQueueEntry;
import com.antsstyle.artretweeter.datastructures.RetweetRecord;
import com.antsstyle.artretweeter.datastructures.ServerResponse;
import com.antsstyle.artretweeter.datastructures.StatusJSON;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.db.CachedVariableDB;
import com.antsstyle.artretweeter.db.ConfigDB;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.db.TweetsDB;
import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.serverapi.ServerAPI;
import com.antsstyle.artretweeter.twitter.RESTAPI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class ClientRefreshQueue implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(ClientRefreshQueue.class);

    private static final ClientRefreshQueue queue = new ClientRefreshQueue();
    private static Thread queueThread;
    private boolean keepRunning = true;

    private Calendar nextTweetScheduleCal;
    private Calendar nextTweetValidationCal;
    private Calendar nextTweetRetweetRecordRefreshCal;
    private Calendar nextTweetRetrievalCal;

    public static ClientRefreshQueue getInstance() {
        return queue;
    }

    public void startQueue() {
        queueThread = new Thread(queue);
        queueThread.start();
    }

    public void refreshTimers() {
        nextTweetScheduleCal = Calendar.getInstance();
        nextTweetValidationCal = Calendar.getInstance();
        nextTweetRetweetRecordRefreshCal = Calendar.getInstance();
        nextTweetRetrievalCal = Calendar.getInstance();
        queueThread.interrupt();
    }

    public void shutDown() {
        keepRunning = false;
        queueThread.interrupt();
    }

    @Override
    public void run() {
        nextTweetScheduleCal = Calendar.getInstance();
        nextTweetValidationCal = Calendar.getInstance();
        nextTweetRetweetRecordRefreshCal = Calendar.getInstance();
        nextTweetRetrievalCal = Calendar.getInstance();
        CachedVariable nextRefreshStatusTime = CachedVariableDB.getCachedVariableByName("artretweeter.nextstatusrefreshtime");
        if (nextRefreshStatusTime == null) {
            CachedVariableDB.updateConfigItem("artretweeter.nextstatusrefreshtime", String.valueOf(nextTweetScheduleCal.getTimeInMillis()));
        } else {
            nextTweetScheduleCal.setTimeInMillis(Long.valueOf(nextRefreshStatusTime.getValue()));
        }

        CachedVariable nextTweetValidationTime = CachedVariableDB.getCachedVariableByName("artretweeter.nexttweetvalidationtime");
        if (nextTweetValidationTime == null) {
            CachedVariableDB.updateConfigItem("artretweeter.nexttweetvalidationtime", String.valueOf(nextTweetValidationCal.getTimeInMillis()));
        } else {
            nextTweetValidationCal.setTimeInMillis(Long.valueOf(nextTweetValidationTime.getValue()));
        }

        CachedVariable nextTweetRetweetRecordRefreshTime = CachedVariableDB.getCachedVariableByName("artretweeter.nexttweetretweetrecordrefreshtime");
        if (nextTweetRetweetRecordRefreshTime == null) {
            CachedVariableDB.updateConfigItem("artretweeter.nexttweetretweetrecordrefreshtime",
                    String.valueOf(nextTweetRetweetRecordRefreshCal.getTimeInMillis()));
        } else {
            nextTweetRetweetRecordRefreshCal.setTimeInMillis(Long.valueOf(nextTweetRetweetRecordRefreshTime.getValue()));
        }

        CachedVariable nextTweetRetrievalTime = CachedVariableDB.getCachedVariableByName("artretweeter.nexttweetretrievaltime");
        if (nextTweetRetrievalTime == null) {
            CachedVariableDB.updateConfigItem("artretweeter.nexttweetretrievaltime",
                    String.valueOf(nextTweetRetrievalCal.getTimeInMillis()));
        } else {
            nextTweetRetrievalCal.setTimeInMillis(Long.valueOf(nextTweetRetrievalTime.getValue()));
        }

        while (keepRunning) {
            if (nextTweetScheduleCal.getTime().before(new Date(System.currentTimeMillis()))) {
                refreshTweetScheduleStatus();
                nextTweetScheduleCal = getNextTweetScheduleStatusRefreshTime();
                CachedVariableDB.updateConfigItem("artretweeter.nextstatusrefreshtime", String.valueOf(nextTweetScheduleCal.getTimeInMillis()));
            }
            if (nextTweetValidationCal.getTime().before(new Date(System.currentTimeMillis()))) {
                RESTAPI.validateStoredTweets();
                nextTweetValidationCal = getNextTweetValidationRefreshTime();
                CachedVariableDB.updateConfigItem("artretweeter.nexttweetvalidationtime", String.valueOf(nextTweetValidationCal.getTimeInMillis()));
            }
            if (nextTweetRetweetRecordRefreshCal.getTime().before(new Date(System.currentTimeMillis()))) {
                refreshTweetRetweetCounts();
                nextTweetRetweetRecordRefreshCal = getNextTweetRetweetRecordRefreshTime();
                CachedVariableDB.updateConfigItem("artretweeter.nexttweetretweetrecordrefreshtime",
                        String.valueOf(nextTweetRetweetRecordRefreshCal.getTimeInMillis()));
            }
            if (nextTweetRetrievalCal.getTime().before(new Date(System.currentTimeMillis())) && TwitterConfig.CHECK_NEW_TWEETS_ENABLED) {
                GUI.getAccountsPanel().retrieveTweets(false);
                nextTweetRetrievalCal = getNextTweetRetrievalTime();
                CachedVariableDB.updateConfigItem("artretweeter.nexttweetretrievaltime",
                        String.valueOf(nextTweetRetrievalCal.getTimeInMillis()));
            }
            try {
                sleepWithInterrupt();
            } catch (InterruptedException e) {
                LOGGER.debug("Interrupted.");
            }
        }
    }
    
    private void sleepWithInterrupt() throws InterruptedException {
        Thread.sleep(60 * 1000);
    }

    private Calendar getNextTweetRetrievalTime() {
        Calendar cal = Calendar.getInstance();
        Integer timeFreq = TwitterConfig.CHECK_NEW_TWEETS_FREQUENCY;
        if (TwitterConfig.CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS.toLowerCase().equals("minutes")) {
            cal.add(Calendar.MINUTE, timeFreq);
        } else if (TwitterConfig.CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS.toLowerCase().equals("hours")) {
            cal.add(Calendar.HOUR, timeFreq);
        } else {
            cal.add(Calendar.DAY_OF_MONTH, timeFreq);
        }
        return cal;
    }

    private Calendar getNextTweetValidationRefreshTime() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 6);
        return cal;
    }

    private Calendar getNextTweetRetweetRecordRefreshTime() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 24);
        return cal;
    }

    private Calendar getNextTweetScheduleStatusRefreshTime() {
        Calendar cal = Calendar.getInstance();
        Integer minutes = cal.get(Calendar.MINUTE);
        if (minutes <= 15) {
            cal.set(Calendar.MINUTE, 16);
        } else if (minutes <= 30) {
            cal.set(Calendar.MINUTE, 31);
        } else if (minutes <= 45) {
            cal.set(Calendar.MINUTE, 46);
        } else {
            cal.add(Calendar.HOUR_OF_DAY, 1);
            cal.set(Calendar.MINUTE, 1);
        }
        return cal;
    }

    private void refreshTweetScheduleStatus() {
        DBResponse accountsResp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!accountsResp.wasSuccessful()) {
            LOGGER.error("Failed to retrieve account information from database - cannot refresh database state.");
            return;
        }
        if (accountsResp.getReturnedRows().isEmpty()) {
            return;
        }
        DBResponse tweetResp = CoreDB.selectFromTable(DBTable.TWEETS);
        if (!tweetResp.wasSuccessful()) {
            LOGGER.error("Failed to retrieve tweet information from database - cannot refresh database state.");
            return;
        }
        HashMap<Long, TweetHolder> tweetsMap = new HashMap<>();
        Set<Long> tweetsMapKeyset = tweetsMap.keySet();
        ArrayList<HashMap<String, Object>> tweetRows = tweetResp.getReturnedRows();
        for (HashMap<String, Object> tweetRow : tweetRows) {
            TweetHolder tweet = ResultSetConversion.getTweet(tweetRow);
            tweetsMap.put(tweet.getTweetID(), tweet);
        }
        ArrayList<HashMap<String, Object>> rows = accountsResp.getReturnedRows();
        ArrayList<Object[]> deleteScheduledRetweetParams = new ArrayList<>();
        ArrayList<Object[]> insertFailedRetweetParams = new ArrayList<>();
        ArrayList<Object[]> insertScheduledRetweetParams = new ArrayList<>();
        for (HashMap<String, Object> row : rows) {
            deleteScheduledRetweetParams.clear();
            insertFailedRetweetParams.clear();
            insertScheduledRetweetParams.clear();
            Account account = ResultSetConversion.getAccount(row);
            DBResponse queueResp = CoreDB.selectFromTable(DBTable.RETWEETQUEUE,
                    new String[]{"retweetingusertwitterid"},
                    new Object[]{account.getTwitterID()});
            if (!queueResp.wasSuccessful()) {
                continue;
            }
            ArrayList<HashMap<String, Object>> queueRows = queueResp.getReturnedRows();
            OperationResult result = ServerAPI.getQueueStatus(account);
            if (!result.wasSuccessful()) {
                LOGGER.error("Failed to get queue status from ArtRetweeter server for account: " + account.getTwitterID());
                continue;
            }
            ArrayList<Long> tweetIDsToRetrieve = new ArrayList<>();
            ServerResponse response = result.getServerResponse();
            Pair<ArrayList<RetweetQueueEntry>, ArrayList<RetweetQueueEntry>> returnedPair
                    = (Pair<ArrayList<RetweetQueueEntry>, ArrayList<RetweetQueueEntry>>) response.getReturnedObject();
            ArrayList<RetweetQueueEntry> scheduledRetweetsOnServer = returnedPair.getLeft();
            ArrayList<RetweetQueueEntry> failedRetweetsOnServer = returnedPair.getRight();

            ArrayList<RetweetQueueEntry> scheduledRetweetsOnClient = new ArrayList<>();
            LOGGER.debug("Scheduled retweets on server count: " + scheduledRetweetsOnServer.size());
            LOGGER.debug("Failed retweets on server count: " + failedRetweetsOnServer.size());
            for (HashMap<String, Object> queueRow : queueRows) {
                RetweetQueueEntry entry = ResultSetConversion.getRetweetQueueEntry(queueRow);
                scheduledRetweetsOnClient.add(entry);
            }
            for (RetweetQueueEntry entry : scheduledRetweetsOnClient) {
                if (failedRetweetsOnServer.contains(entry)) {
                    insertFailedRetweetParams.add(new Object[]{entry.getTweetID(), entry.getRetweetingUserTwitterID(), entry.getRetweetTime(),
                        entry.getErrorCode(), entry.getFailReason()});
                    deleteScheduledRetweetParams.add(new Object[]{entry.getTweetID(), entry.getRetweetingUserTwitterID()});
                } else if (!scheduledRetweetsOnServer.contains(entry)) {
                    deleteScheduledRetweetParams.add(new Object[]{entry.getTweetID(), entry.getRetweetingUserTwitterID()});
                }
            }
            for (RetweetQueueEntry entry : scheduledRetweetsOnServer) {
                if (!scheduledRetweetsOnClient.contains(entry)) {
                    insertScheduledRetweetParams.add(new Object[]{entry.getTweetID(), entry.getRetweetingUserTwitterID(),
                        entry.getRetweetTime()});
                    if (!tweetsMapKeyset.contains(entry.getTweetID())) {
                        tweetIDsToRetrieve.add(entry.getTweetID());
                    }
                }
            }
            for (RetweetQueueEntry entry : failedRetweetsOnServer) {
                insertFailedRetweetParams.add(new Object[]{entry.getTweetID(), entry.getRetweetingUserTwitterID(), entry.getRetweetTime(),
                    entry.getErrorCode(), entry.getFailReason()});
                if (!tweetsMapKeyset.contains(entry.getTweetID())) {
                    tweetIDsToRetrieve.add(entry.getTweetID());
                }
            }

            Path tweetFolderPath = ConfigDB.getTweetFolderPath(account);
            if (!tweetIDsToRetrieve.isEmpty()) {
                OperationResult tweetOpResult = RESTAPI.getTweetsByIDs(tweetIDsToRetrieve, account, tweetFolderPath);
                if (!tweetOpResult.wasSuccessful()) {
                    StringBuilder msg = new StringBuilder("Failed to retrieve unrecorded tweet IDs from server! Tweet IDs were: ");
                    for (Long tweetID : tweetIDsToRetrieve) {
                        msg = msg.append(String.valueOf(tweetID)).append(",");
                    }
                    msg.setLength(msg.length() - 1);
                    LOGGER.error(msg.toString());
                    continue;
                }
                ArrayList<StatusJSON> statuses = (ArrayList<StatusJSON>) tweetOpResult.getTwitterResponse().getReturnedObject();
                ArrayList<Object[]> tweetMergeParams = new ArrayList<>();
                for (StatusJSON status : statuses) {
                    OperationResult res = status.downloadAndGetDBParams(tweetFolderPath);
                    if (res.wasSuccessful()) {
                        Object[] params = (Object[]) res.getClientResponse().getReturnedObject();
                        tweetMergeParams.add(params);
                    }
                }
                TweetsDB.parameterisedTweetMergeBatch(tweetMergeParams);
            }

            String deleteScheduledRetweetQuery = "DELETE FROM retweetqueue WHERE tweetid=? AND retweetingusertwitterid=?";
            if (!deleteScheduledRetweetParams.isEmpty()) {
                CoreDB.runParameterisedUpdateBatch(deleteScheduledRetweetQuery, deleteScheduledRetweetParams);
            }

            String insertFailedRetweetQuery = "INSERT INTO failedretweets (tweetid,retweetingusertwitterid,retweettime,errorcode,failreason) "
                    + "VALUES (?,?,?,?,?)";
            if (!insertFailedRetweetParams.isEmpty()) {
                CoreDB.runParameterisedUpdateBatch(insertFailedRetweetQuery, insertFailedRetweetParams);
            }

            String insertScheduledRetweetQuery = "INSERT INTO retweetqueue (tweetid,retweetingusertwitterid,retweettime) "
                    + "VALUES (?,?,?)";
            if (!insertScheduledRetweetParams.isEmpty()) {
                CoreDB.runParameterisedUpdateBatch(insertScheduledRetweetQuery, insertScheduledRetweetParams);
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                return;
            }
        }

        SwingUtilities.invokeLater(() -> {
            GUI.getQueuingPanel().refreshQueueTable();
        });
    }

    private void refreshTweetRetweetCounts() {
        DBResponse accountsResp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!accountsResp.wasSuccessful()) {
            LOGGER.error("Failed to retrieve account information from database - cannot refresh database state.");
            return;
        }
        if (accountsResp.getReturnedRows().isEmpty()) {
            return;
        }
        ArrayList<HashMap<String, Object>> rows = accountsResp.getReturnedRows();
        ArrayList<Object[]> insertParams = new ArrayList<>();
        for (HashMap<String, Object> row : rows) {
            Account account = ResultSetConversion.getAccount(row);
            OperationResult result = ServerAPI.getTweetRetweetStatus(account);
            if (!result.wasSuccessful()) {
                LOGGER.error("Failed to get tweet retweet status from ArtRetweeter server for account: " + account.getTwitterID());
                continue;
            }
            ArrayList<RetweetRecord> retweetRecordsOnServer = (ArrayList<RetweetRecord>) result.getServerResponse().getReturnedObject();
            for (RetweetRecord record : retweetRecordsOnServer) {
                insertParams.add(new Object[]{record.getUserTwitterID(), record.getTweetID(), record.getRetweetTime()});
            }
            TweetsDB.insertRetweetRecordEntryBatch(insertParams);
        }
        SwingUtilities.invokeLater(() -> {
            GUI.getQueuingPanel().refreshTweetsTable();
        });
    }
}
