/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.queues;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.ConfigItem;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.RetweetQueueEntry;
import com.antsstyle.artretweeter.datastructures.RetweetRecord;
import com.antsstyle.artretweeter.datastructures.ServerResponse;
import com.antsstyle.artretweeter.datastructures.StatusJSON;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
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

    @Override
    public void run() {
        Calendar nextTweetScheduleCal = getNextTweetScheduleStatusRefreshTime();
        Calendar nextTweetValidationCal = getNextTweetValidationRefreshTime();
        Calendar nextTweetRetweetRecordRefreshCal = getNextTweetRetweetRecordRefreshTime();
        ConfigItem nextRefreshStatusTime = ConfigDB.getConfigItemByName("artretweeter.nextstatusrefreshtime");
        if (nextRefreshStatusTime == null) {
            ConfigDB.updateConfigItem("artretweeter.nextstatusrefreshtime", String.valueOf(nextTweetScheduleCal.getTimeInMillis()));
        } else {
            nextTweetScheduleCal.setTimeInMillis(Long.valueOf(nextRefreshStatusTime.getValue()));
        }

        ConfigItem nextTweetValidationTime = ConfigDB.getConfigItemByName("artretweeter.nexttweetvalidationtime");
        if (nextTweetValidationTime == null) {
            ConfigDB.updateConfigItem("artretweeter.nexttweetvalidationtime", String.valueOf(nextTweetValidationCal.getTimeInMillis()));
        } else {
            nextTweetValidationCal.setTimeInMillis(Long.valueOf(nextTweetValidationTime.getValue()));
        }

        ConfigItem nextTweetRetweetRecordRefreshTime = ConfigDB.getConfigItemByName("artretweeter.nexttweetretweetrecordrefreshtime");
        if (nextTweetRetweetRecordRefreshTime == null) {
            ConfigDB.updateConfigItem("artretweeter.nexttweetretweetrecordrefreshtime",
                    String.valueOf(nextTweetRetweetRecordRefreshCal.getTimeInMillis()));
        } else {
            nextTweetRetweetRecordRefreshCal.setTimeInMillis(Long.valueOf(nextTweetRetweetRecordRefreshTime.getValue()));
        }

        while (true) {
            if (nextTweetScheduleCal.getTime().before(new Date(System.currentTimeMillis()))) {
                refreshTweetScheduleStatus();
                nextTweetScheduleCal = getNextTweetScheduleStatusRefreshTime();
                ConfigDB.updateConfigItem("artretweeter.nextstatusrefreshtime", String.valueOf(nextTweetScheduleCal.getTimeInMillis()));
            }
            if (nextTweetValidationCal.getTime().before(new Date(System.currentTimeMillis()))) {
                RESTAPI.validateStoredTweets();
                nextTweetValidationCal = getNextTweetValidationRefreshTime();
                ConfigDB.updateConfigItem("artretweeter.nexttweetvalidationtime", String.valueOf(nextTweetValidationCal.getTimeInMillis()));
            }
            if (nextTweetRetweetRecordRefreshCal.getTime().before(new Date(System.currentTimeMillis()))) {
                refreshTweetRetweetCounts();
                nextTweetRetweetRecordRefreshCal = getNextTweetRetweetRecordRefreshTime();
                ConfigDB.updateConfigItem("artretweeter.nexttweetretweetrecordrefreshtime",
                        String.valueOf(nextTweetRetweetRecordRefreshCal.getTimeInMillis()));
            }
            try {
                Thread.sleep(60 * 1000);
            } catch (Exception e) {
                return;
            }
        }
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
    
    private void getNewTweets() {
        
    }

}
