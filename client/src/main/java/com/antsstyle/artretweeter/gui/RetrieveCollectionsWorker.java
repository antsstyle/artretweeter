/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import com.antsstyle.artretweeter.db.ConfigDB;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.serverapi.APIQueryManager;
import com.antsstyle.artretweeter.twitter.RESTAPI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class RetrieveCollectionsWorker extends SwingWorker<Object, Pair<Integer, Integer>> {

    private static final Logger LOGGER = LogManager.getLogger(RetrieveCollectionsWorker.class);
    private Account account;
    private ArrayList<OperationResult> errorResults = new ArrayList<>();

    public static boolean retrieveForAllAccounts() {
        DBResponse resp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to get accounts from database.");
            return false;
        }
        if (resp.getReturnedRows().isEmpty()) {
            return true;
        }
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            Account account = ResultSetConversion.getAccount(row);
            GUI.getAccountsPanel().disableAllAccountButtons();
            RetrieveCollectionsWorker worker = new RetrieveCollectionsWorker();
            if (!worker.initialise(account, false)) {
                GUI.getAccountsPanel().enableAllAccountButtons();
            }
            while (!worker.isDone()) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return true;
    }

    public final boolean initialise(Account account, boolean showGUI) {
        this.account = account;
        Path tweetFolderPath = ConfigDB.getTweetFolderPath(account);
        if (tweetFolderPath == null) {
            String statusMessage = "Failed to get tweet image directory information from database!";
            GUIHelperMethods.showError(statusMessage, showGUI);
            GUI.getAccountsPanel().enableAllAccountButtons();
            return false;
        }
        try {
            Files.createDirectories(tweetFolderPath);
        } catch (Exception e) {
            LOGGER.error("Could not create tweet image base directories!", e);
            String statusMessage = "Could not create tweet image base directories!";
            GUIHelperMethods.showError(statusMessage, showGUI);
            GUI.getAccountsPanel().enableAllAccountButtons();
            return false;
        }
        GUI.getAccountsPanel().setRetrievingCollectionsGUIElements();
        this.execute();
        return true;
    }

    @Override
    protected Object doInBackground() {
        OperationResult res;
        boolean fatalError = false;
        int errorCount = 0;
        int successCount = 0;
        res = RESTAPI.getCollectionsByUserID(account.getTwitterID(), account);
        if (!res.wasSuccessful()) {
            fatalError = true;
            errorResults.add(res);
            return new Object[]{fatalError, errorCount, successCount};
        }
        ArrayList<TwitterCollectionHolder> collections
                = (ArrayList<TwitterCollectionHolder>) res.getTwitterResponse().getReturnedObject();
        int count = 1;
        int total = 1 + collections.size();
        publish(Pair.of(count, total));
        for (TwitterCollectionHolder collection : collections) {
            OperationResult hydrationResult = RESTAPI.getFullyHydratedCollectionByID(collection.getTwitterID(), account);
            if (!hydrationResult.wasSuccessful()) {
                errorCount++;
                errorResults.add(hydrationResult);
            } else {
                successCount++;
            }
            count++;
            publish(Pair.of(count, total));
        }
        setProgress(100);
        return new Object[]{fatalError, errorCount, successCount};
    }

    @Override
    protected void process(List<Pair<Integer, Integer>> chunks) {
        Pair<Integer, Integer> chunk = chunks.get(chunks.size() - 1);
        Integer count = chunk.getLeft();
        Integer total = chunk.getRight();
        int floorPercent = (int) (((double) count / (double) total) * 100.0);
        GUI.getAccountsPanel().getProgressBar().setValue(floorPercent);
    }

    @Override
    public void done() {
        APIQueryManager.releaseAPILock();
        Object[] results;
        try {
            results = (Object[]) get();
        } catch (Exception e) {
            LOGGER.error("Failed to acquire results!", e);
            GUI.getAccountsPanel().setRetrievingCollectionsGUIElements();
            GUI.getAccountsPanel().enableAllAccountButtons();
            return;
        }
        GUI.getAccountsPanel().setCollectionRetrievalResults(results, errorResults, account);
        GUI.getAccountsPanel().setFinishedRetrievingCollectionsGUIElements();
        Account selAcc = GUI.getCollectionsPanel().getCurrentlySelectedAccount();
        if (selAcc.getTwitterID().equals(account.getTwitterID())) {
            GUI.getCollectionsPanel().refreshCollectionBoxModel(false);
        }
        if (selAcc.getTwitterID().equals(account.getTwitterID())) {
            GUI.getMainManagementPanel().getCollectionsSubPanel().refreshCollectionBoxModel(false);
        }
        GUI.getAccountsPanel().enableAllAccountButtons();
    }

}
