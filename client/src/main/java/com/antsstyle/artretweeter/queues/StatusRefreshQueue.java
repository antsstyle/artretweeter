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
        Calendar cal = Calendar.getInstance();
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
        if (cal.getTime().before(new Date(System.currentTimeMillis()))) {
            refreshGUI();
        }
        while (true) {
            refreshGUI();
            cal.add(Calendar.MINUTE, 15);
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
            if (queueResp.getReturnedRows().isEmpty()) {
                continue;
            }
            OperationResult result = ServerAPI.getQueueStatus(account);
            if (!result.wasSuccessful()) {
                continue;
            }
            ServerResponse response = result.getServerResponse();
            Pair<ArrayList<RetweetQueueEntry>, ArrayList<RetweetQueueEntry>> returnedPair
                    = (Pair<ArrayList<RetweetQueueEntry>, ArrayList<RetweetQueueEntry>>) response.getReturnedObject();
            ArrayList<RetweetQueueEntry> scheduledRetweets = returnedPair.getLeft();
            ArrayList<RetweetQueueEntry> failedRetweets = returnedPair.getLeft();
            for (RetweetQueueEntry entry : scheduledRetweets) {
                deleteParams.add(new Object[]{entry.getTweetID(), entry.getInternalAccountID()});
            }
            for (RetweetQueueEntry entry : failedRetweets) {
                insertParams.add(new Object[]{entry.getTweetID(), entry.getInternalAccountID(), entry.getRetweetTime(),
                    entry.getErrorCode(), entry.getFailReason()});
            }
        }
        String deleteQuery = "DELETE FROM retweetqueue WHERE tweetid=? AND internalaccountid=?";
        CoreDB.runParameterisedUpdateBatch(deleteQuery, deleteParams);
        String insertQuery = "INSERT INTO failedretweets (tweetid,internalaccountid,retweettime,errorcode,failreason) "
                + "VALUES (?,?,?,?,?)";
        CoreDB.runParameterisedUpdateBatch(insertQuery, insertParams);
        SwingUtilities.invokeLater(() -> {
            GUI.getQueuingPanel().refreshQueueTable();
        });
    }

}
