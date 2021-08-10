/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.ClientResponse;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.StatusJSON;
import com.antsstyle.artretweeter.db.ConfigDB;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.enumerations.StatusCode;
import com.antsstyle.artretweeter.serverapi.APIQueryManager;
import com.antsstyle.artretweeter.twitter.RESTAPI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class RetrieveTweetsWorker extends SwingWorker<Object, Pair<Integer, Integer>> {

    private static final Logger LOGGER = LogManager.getLogger(RetrieveTweetsWorker.class);
    private Account account;
    private Path tweetFolderPath;

    private Long finalMaxID;
    private Long finalSinceID;

    private Integer storedTweetCount = 0;
    private Integer receivedTweetCount = 0;

    private Integer countBeforeStart;

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
            RetrieveTweetsWorker worker = new RetrieveTweetsWorker();
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
        tweetFolderPath = ConfigDB.getTweetFolderPath(account);
        if (tweetFolderPath == null) {
            String statusMessage = "Failed to get tweet image directory information from database!";
            GUIHelperMethods.showError(statusMessage, showGUI);
            return false;
        }
        try {
            Files.createDirectories(tweetFolderPath);
        } catch (Exception e) {
            LOGGER.error("Directory creation exception: ", e);
            String statusMessage = "Could not create tweet image base directories!";
            GUIHelperMethods.showError(statusMessage, showGUI);
            return false;
        }
        LOGGER.debug("Tweet folder path: " + tweetFolderPath);
        Long maxID = account.getMaxID();
        Long sinceID = account.getSinceID();
        if (maxID == null) {
            finalMaxID = Long.MAX_VALUE;
        } else {
            finalMaxID = maxID;
        }
        if (sinceID == null) {
            finalSinceID = 0L;
        } else {
            finalSinceID = sinceID;
        }
        Long count;
        String cQuery = "SELECT COUNT(*) AS C FROM tweets WHERE usertwitterid=?";
        DBResponse countResp = CoreDB.customQuerySelect(cQuery, account.getTwitterID());
        if (!countResp.wasSuccessful()) {
            String statusMessage = "Could not retrieve previous tweet count for this user from database!";
            GUIHelperMethods.showError(statusMessage, showGUI);
            return false;
        } else {
            count = (Long) countResp.getReturnedRows().get(0).get("C");
            if (count == null) {
                count = 0L;
            }
        }
        countBeforeStart = count.intValue();
        GUI.getAccountsPanel().setRetrievingTweetsGUIElements();
        this.execute();
        return true;
    }

    @Override
    protected Object doInBackground() {
        Pair<OperationResult, ArrayList<StatusJSON>> returnResults;
        ArrayList<StatusJSON> statuses = new ArrayList<>();
        Long maxID = finalMaxID;
        Long sinceID = finalSinceID;
        Boolean finished = false;
        OperationResult finalResult = new OperationResult();
        int consecutiveErrors = 0;
        try ( CloseableHttpClient httpclient = HttpClients.createDefault()) {
            while (!finished) {
                try {
                    LOGGER.debug("Params: Max ID: " + maxID + " Since ID: " + sinceID);
                    returnResults = RESTAPI.getUnrecordedUserTweetsByDate(httpclient, account,
                            maxID, sinceID, tweetFolderPath);
                    OperationResult lastResult = returnResults.getLeft();
                    finalResult = lastResult;
                    if (lastResult.wasSuccessful()) {
                        consecutiveErrors = 0;
                        ArrayList<StatusJSON> returnedStatuses = returnResults.getRight();
                        statuses.addAll(returnedStatuses);
                        Pair<Long, Long> resultPair = (Pair<Long, Long>) lastResult.getTwitterResponse().getReturnedObject();
                        if (returnedStatuses.isEmpty()) {
                            finished = true;
                            LOGGER.debug("No more statuses to retrieve.");
                            if (resultPair != null) {
                                if (!maxID.equals(0L)) {
                                    maxID = 0L;
                                    CoreDB.updateTable(DBTable.ACCOUNTS,
                                            new String[]{"max_id"},
                                            new Object[]{maxID},
                                            new String[]{"twitterid"},
                                            new Object[]{account.getTwitterID()});
                                }

                                Long newSinceID = resultPair.getRight();
                                if (newSinceID > sinceID) {
                                    sinceID = newSinceID;
                                    CoreDB.updateTable(DBTable.ACCOUNTS,
                                            new String[]{"since_id"},
                                            new Object[]{newSinceID},
                                            new String[]{"twitterid"},
                                            new Object[]{account.getTwitterID()});
                                }
                            }
                        } else {
                            maxID = resultPair.getLeft();
                            CoreDB.updateTable(DBTable.ACCOUNTS,
                                    new String[]{"max_id"},
                                    new Object[]{maxID},
                                    new String[]{"twitterid"},
                                    new Object[]{account.getTwitterID()});
                            Long newSinceID = resultPair.getRight();
                            if (newSinceID > sinceID) {
                                sinceID = newSinceID;
                                CoreDB.updateTable(DBTable.ACCOUNTS,
                                        new String[]{"since_id"},
                                        new Object[]{newSinceID},
                                        new String[]{"twitterid"},
                                        new Object[]{account.getTwitterID()});
                            }
                        }

                        storedTweetCount += lastResult.getTwitterResponse().getStoredTweetCount();
                        receivedTweetCount += lastResult.getTwitterResponse().getReceivedTweetCount();
                        publish(Pair.of(storedTweetCount, receivedTweetCount));

                    } else if (lastResult.getErrorCode().equals(StatusCode.RATE_LIMIT_EXCEEDED_ERROR)) {
                        return Pair.of(lastResult, statuses);
                    } else {
                        LOGGER.error("Error: " + lastResult.getErrorCode().toString());
                        try {
                            Thread.sleep(10 * 1000);
                        } catch (Exception e) {
                            LOGGER.error("Interrupted while waiting", e);
                            finished = true;
                        }
                        consecutiveErrors++;
                        if (consecutiveErrors >= 3) {
                            finished = true;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Tweet retrieval encountered exception", e);
                    finalResult.setClientResponse(new ClientResponse(StatusCode.MISC_ERROR));
                    return Pair.of(finalResult, statuses);
                }
                if (!finished) {
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (Exception e) {
                        LOGGER.info("Interrupted while waiting - aborting tweet download.");
                        break;
                    }
                }
            }
            return Pair.of(finalResult, statuses);
        } catch (Exception e) {
            LOGGER.error("Failed to create httpclient!", e);
            finalResult.setClientResponse(new ClientResponse(StatusCode.MISC_ERROR));
            finalResult.getClientResponse().setExtraStatusMessage("Failed to create httpclient!");
            return Pair.of(finalResult, null);
        }
    }

    @Override
    protected void process(List<Pair<Integer, Integer>> chunks) {
        Pair<Integer, Integer> last = chunks.get(chunks.size() - 1);
        GUI.getAccountsPanel().updateAccountsTableForTweets(countBeforeStart, last, account.getScreenName());
    }

    @Override
    public void done() {
        APIQueryManager.releaseAPILock();
        Pair<OperationResult, ArrayList<StatusJSON>> results;
        try {
            results = (Pair<OperationResult, ArrayList<StatusJSON>>) get();
        } catch (Exception e) {
            LOGGER.error("Failed to acquire results!", e);
            GUI.getAccountsPanel().getProgressBar().setVisible(false);
            GUI.getAccountsPanel().enableAllAccountButtons();
            return;
        }
        GUI.getAccountsPanel().setTweetRetrievalResults(results, account, storedTweetCount, receivedTweetCount);
        Account selAcc = GUI.getMainManagementPanel().getMainTweetsPanel().getSelectedAccount();
        if (selAcc.getTwitterID().equals(account.getTwitterID())) {
            GUI.getMainManagementPanel().getQueueSubPanel().refreshQueueTable();
        }
        if (selAcc.getTwitterID().equals(account.getTwitterID())) {
            GUI.getMainManagementPanel().getMainTweetsPanel().refreshTweetsTable(false);
        }
        GUI.getAccountsPanel().getProgressBar().setVisible(false);
        GUI.getAccountsPanel().enableAllAccountButtons();
    }
}
