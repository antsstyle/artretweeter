/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.queues;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.RetweetQueueEntry;
import com.antsstyle.artretweeter.datastructures.ServerResponse;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.serverapi.ServerAPI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class StatusRefreshQueue implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(StatusRefreshQueue.class);

    @Override
    public void run() {
        Calendar cal = getNextRefreshTime();
        DBResponse selectResp = CoreDB.selectFromTable(DBTable.CONFIGURATION,
                new String[]{"name"},
                new Object[]{"artretweeter.nextstatusrefreshtime"});
        if (!selectResp.wasSuccessful()) {
            return;
        }
        if (selectResp.getReturnedRows().isEmpty()) {
            CoreDB.insertIntoTable(DBTable.CONFIGURATION,
                    new String[]{"name", "value"},
                    new Object[]{"artretweeter.nextstatusrefreshtime", String.valueOf(cal.getTimeInMillis())});
        } else {
            cal.setTimeInMillis(Long.valueOf((String) selectResp.getReturnedRows().get(0).get("VALUE")));
        }
        //if (cal.getTime().before(new Date(System.currentTimeMillis()))) {
        refreshGUI();
        //}
        try {
            Thread.sleep(Math.max(cal.getTimeInMillis() - System.currentTimeMillis(), 15 * 60 * 1000));
        } catch (Exception e) {
            return;
        }
        while (true) {
            refreshGUI();
            cal = getNextRefreshTime();
            CoreDB.updateTable(DBTable.CONFIGURATION,
                    new String[]{"value"},
                    new Object[]{String.valueOf(cal.getTimeInMillis())},
                    new String[]{"name"},
                    new Object[]{"artretweeter.nextstatusrefreshtime"});
            try {
                Thread.sleep(Math.max(cal.getTimeInMillis() - System.currentTimeMillis(), 15 * 60 * 1000));
            } catch (Exception e) {
                return;
            }
        }
    }

    private Calendar getNextRefreshTime() {
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

    private void refreshGUI() {
        DBResponse accountsResp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!accountsResp.wasSuccessful()) {
            return;
        }
        if (accountsResp.getReturnedRows().isEmpty()) {
            return;
        }
        ArrayList<HashMap<String, Object>> rows = accountsResp.getReturnedRows();
        ArrayList<Object[]> deleteParams = new ArrayList<>();
        ArrayList<Object[]> insertParams = new ArrayList<>();
        for (HashMap<String, Object> row : rows) {
            Account account = ResultSetConversion.getAccount(row);
            DBResponse queueResp = CoreDB.selectFromTable(DBTable.RETWEETQUEUE,
                    new String[]{"retweetingusertwitterid"},
                    new Object[]{account.getTwitterID()});
            if (!queueResp.wasSuccessful()) {
                continue;
            }
            /*if (queueResp.getReturnedRows().isEmpty()) {
                continue;
            }*/
            ArrayList<HashMap<String, Object>> queueRows = queueResp.getReturnedRows();
            OperationResult result = ServerAPI.getQueueStatus(account);
            if (!result.wasSuccessful()) {
                LOGGER.error("Failed to get queue status from ArtRetweeter server for account: " + account.getTwitterID());
                continue;
            }
            ServerResponse response = result.getServerResponse();
            Pair<ArrayList<RetweetQueueEntry>, ArrayList<RetweetQueueEntry>> returnedPair
                    = (Pair<ArrayList<RetweetQueueEntry>, ArrayList<RetweetQueueEntry>>) response.getReturnedObject();
            ArrayList<RetweetQueueEntry> scheduledRetweetsOnServer = returnedPair.getLeft();
            ArrayList<RetweetQueueEntry> failedRetweetsOnServer = returnedPair.getRight();

            ArrayList<RetweetQueueEntry> scheduledRetweetsOnClient = new ArrayList<>();
            for (HashMap<String, Object> queueRow : queueRows) {
                RetweetQueueEntry entry = ResultSetConversion.getRetweetQueueEntry(queueRow);
                scheduledRetweetsOnClient.add(entry);
            }
            LOGGER.debug("SERVER:");
            for (RetweetQueueEntry e: scheduledRetweetsOnServer) {
                LOGGER.debug("ID: " + e.getId());
                LOGGER.debug("Tweet ID: " + e.getTweetID());
                LOGGER.debug("User Twitter ID: " + e.getRetweetingUserTwitterID());
                LOGGER.debug("Time: " + e.getRetweetTime());
            }
            LOGGER.debug("CLIENT:");
            for (RetweetQueueEntry e: scheduledRetweetsOnClient) {
                LOGGER.debug("ID: " + e.getId());
                LOGGER.debug("Tweet ID: " + e.getTweetID());
                LOGGER.debug("User Twitter ID: " + e.getRetweetingUserTwitterID());
                LOGGER.debug("Time: " + e.getRetweetTime());
            }
            for (RetweetQueueEntry entry : scheduledRetweetsOnClient) {
                if (failedRetweetsOnServer.contains(entry)) {
                    insertParams.add(new Object[]{entry.getTweetID(), entry.getRetweetingUserTwitterID(), entry.getRetweetTime(),
                        entry.getErrorCode(), entry.getFailReason()});
                    deleteParams.add(new Object[]{entry.getTweetID(), entry.getRetweetingUserTwitterID()});
                } else if (!scheduledRetweetsOnServer.contains(entry)) {
                    deleteParams.add(new Object[]{entry.getTweetID(), entry.getRetweetingUserTwitterID()});
                }
            }

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                return;
            }
        }
        String deleteQuery = "DELETE FROM retweetqueue WHERE tweetid=? AND retweetingusertwitterid=?";
        if (!deleteParams.isEmpty()) {
            CoreDB.runParameterisedUpdateBatch(deleteQuery, deleteParams);
        }

        String insertQuery = "INSERT INTO failedretweets (tweetid,retweetingusertwitterid,retweettime,errorcode,failreason) "
                + "VALUES (?,?,?,?,?)";
        if (!insertParams.isEmpty()) {
            CoreDB.runParameterisedUpdateBatch(insertQuery, insertParams);
        }

        SwingUtilities.invokeLater(() -> {
            GUI.getQueuingPanel().refreshQueueTable();
        });
    }

}
