/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.configuration.TwitterConfig;
import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.ClientResponse;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.RequestToken;
import com.antsstyle.artretweeter.datastructures.StatusJSON;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import com.antsstyle.artretweeter.db.ConfigDB;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.enumerations.StatusCode;
import com.antsstyle.artretweeter.queues.ClientRefreshQueue;
import com.antsstyle.artretweeter.serverapi.APIQueryManager;
import com.antsstyle.artretweeter.serverapi.ServerAPI;
import com.antsstyle.artretweeter.tools.SwingTools;
import com.antsstyle.artretweeter.twitter.RESTAPI;
import java.awt.Desktop;
import java.awt.Font;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class AccountsPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = LogManager.getLogger(AccountsPanel.class);

    /**
     * Creates new form AccountsPanel
     */
    public AccountsPanel() {
        initComponents();
    }

    public void initialise() {
        DBResponse resp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to initialise accounts panel!");
            return;
        }
        DefaultTableModel dtm = (DefaultTableModel) accountsTable.getModel();
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            Account account = ResultSetConversion.getAccount(row);
            String tweetCountQuery = "SELECT COUNT(*) AS C FROM tweets WHERE usertwitterid=?";
            DBResponse countResp = CoreDB.customQuerySelect(tweetCountQuery, account.getTwitterID());
            Long tweetCount;
            if (!countResp.wasSuccessful()) {
                LOGGER.error("Failed to initialise accounts panel!");
                return;
            } else {
                tweetCount = (Long) countResp.getReturnedRows().get(0).get("C");
                if (tweetCount == null) {
                    tweetCount = 0L;
                }
            }
            String collectionCountQuery = "SELECT COUNT(*) AS C FROM collections WHERE usertwitterid=?";
            countResp = CoreDB.customQuerySelect(collectionCountQuery, account.getTwitterID());
            if (!countResp.wasSuccessful()) {
                LOGGER.error("Failed to initialise accounts panel!");
                return;
            } else {
                Long collectionCount = (Long) countResp.getReturnedRows().get(0).get("C");
                if (collectionCount == null) {
                    collectionCount = 0L;
                }
                dtm.addRow(new Object[]{account.getScreenName(), tweetCount.intValue(), collectionCount.intValue()});
            }
        }
        if (TwitterConfig.CHECK_NEW_TWEETS_ENABLED) {
            automaticNoteLabel.setText("");
        }
    }

    public void setRetrievingTweetsGUIElements() {
        SwingUtilities.invokeLater(() -> {
            retrieveTweetsButton.setText("Retrieving...");
            tweetDownloadProgressLabel.setText("Retrieving... ");
            tweetDownloadProgressBar.setVisible(true);
        });
    }

    public void setRetrievingCollectionsGUIElements() {
        SwingUtilities.invokeLater(() -> {
            retrieveCollectionsButton.setText("Retrieving...");
            tweetDownloadProgressLabel.setText("Retrieving... ");
            tweetDownloadProgressBar.setVisible(true);
            tweetDownloadProgressBar.setIndeterminate(false);
        });
    }

    public void setFinishedRetrievingCollectionsGUIElements() {
        SwingUtilities.invokeLater(() -> {
            tweetDownloadProgressBar.setVisible(false);
            tweetDownloadProgressBar.setValue(0);
            tweetDownloadProgressBar.setIndeterminate(true);
        });
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        accountsTable = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        addAccountButton = new javax.swing.JButton();
        removeAccountButton = new javax.swing.JButton();
        retrieveTweetsButton = new javax.swing.JButton();
        tweetDownloadProgressBar = new javax.swing.JProgressBar();
        tweetDownloadProgressBar.setVisible(false);
        tweetDownloadProgressLabel = new javax.swing.JLabel();
        retrieveCollectionsButton = new javax.swing.JButton();
        automaticNoteLabel = new javax.swing.JLabel();

        accountsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Account Name", "Tweets Retrieved", "Collections Retrieved"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(accountsTable);
        if (accountsTable.getColumnModel().getColumnCount() > 0) {
            accountsTable.getColumnModel().getColumn(1).setMinWidth(110);
            accountsTable.getColumnModel().getColumn(1).setPreferredWidth(110);
            accountsTable.getColumnModel().getColumn(1).setMaxWidth(110);
            accountsTable.getColumnModel().getColumn(2).setMinWidth(130);
            accountsTable.getColumnModel().getColumn(2).setPreferredWidth(130);
            accountsTable.getColumnModel().getColumn(2).setMaxWidth(130);
        }

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setText("Accounts");

        addAccountButton.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        addAccountButton.setText("Add Account");
        addAccountButton.setMaximumSize(new java.awt.Dimension(115, 31));
        addAccountButton.setMinimumSize(new java.awt.Dimension(115, 31));
        addAccountButton.setPreferredSize(new java.awt.Dimension(115, 31));
        addAccountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAccountButtonActionPerformed(evt);
            }
        });

        removeAccountButton.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        removeAccountButton.setText("Remove Account");
        removeAccountButton.setMaximumSize(new java.awt.Dimension(135, 31));
        removeAccountButton.setMinimumSize(new java.awt.Dimension(135, 31));
        removeAccountButton.setPreferredSize(new java.awt.Dimension(135, 31));
        removeAccountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAccountButtonActionPerformed(evt);
            }
        });

        retrieveTweetsButton.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        retrieveTweetsButton.setText("Retrieve Tweets");
        retrieveTweetsButton.setMaximumSize(new java.awt.Dimension(130, 31));
        retrieveTweetsButton.setMinimumSize(new java.awt.Dimension(130, 31));
        retrieveTweetsButton.setPreferredSize(new java.awt.Dimension(130, 31));
        retrieveTweetsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                retrieveTweetsButtonActionPerformed(evt);
            }
        });

        tweetDownloadProgressBar.setIndeterminate(true);
        tweetDownloadProgressBar.setMaximumSize(new java.awt.Dimension(542, 40));
        tweetDownloadProgressBar.setMinimumSize(new java.awt.Dimension(542, 40));
        tweetDownloadProgressBar.setPreferredSize(new java.awt.Dimension(542, 40));

        tweetDownloadProgressLabel.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        tweetDownloadProgressLabel.setToolTipText("");
        tweetDownloadProgressLabel.setMaximumSize(new java.awt.Dimension(542, 65));
        tweetDownloadProgressLabel.setMinimumSize(new java.awt.Dimension(542, 65));
        tweetDownloadProgressLabel.setName(""); // NOI18N
        tweetDownloadProgressLabel.setPreferredSize(new java.awt.Dimension(542, 65));

        retrieveCollectionsButton.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        retrieveCollectionsButton.setText("Retrieve Collections");
        retrieveCollectionsButton.setMaximumSize(new java.awt.Dimension(152, 31));
        retrieveCollectionsButton.setMinimumSize(new java.awt.Dimension(152, 31));
        retrieveCollectionsButton.setPreferredSize(new java.awt.Dimension(152, 31));
        retrieveCollectionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                retrieveCollectionsButtonActionPerformed(evt);
            }
        });

        automaticNoteLabel.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        automaticNoteLabel.setText("<html>Note: you can make ArtRetweeter download new tweets automatically via the Configuration menu.</html>");
        automaticNoteLabel.setMaximumSize(new java.awt.Dimension(550, 45));
        automaticNoteLabel.setMinimumSize(new java.awt.Dimension(550, 45));
        automaticNoteLabel.setPreferredSize(new java.awt.Dimension(550, 45));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(automaticNoteLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tweetDownloadProgressLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tweetDownloadProgressBar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 267, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(addAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(removeAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(retrieveTweetsButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(retrieveCollectionsButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(10, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(removeAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(retrieveTweetsButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(retrieveCollectionsButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(automaticNoteLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tweetDownloadProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tweetDownloadProgressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(59, 59, 59))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void removeAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAccountButtonActionPerformed
        removeAccountButton.setEnabled(false);
        removeAccount();
        removeAccountButton.setEnabled(true);
    }//GEN-LAST:event_removeAccountButtonActionPerformed

    private void addAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAccountButtonActionPerformed
        addAccountButton.setEnabled(false);
        addAccount();
        addAccountButton.setEnabled(true);
    }//GEN-LAST:event_addAccountButtonActionPerformed

    private void retrieveTweetsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_retrieveTweetsButtonActionPerformed
        disableAllAccountButtons();
        retrieveTweets(true);
    }//GEN-LAST:event_retrieveTweetsButtonActionPerformed

    private void retrieveCollectionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_retrieveCollectionsButtonActionPerformed
        disableAllAccountButtons();
        retrieveCollections();
    }//GEN-LAST:event_retrieveCollectionsButtonActionPerformed

    public void disableAllAccountButtons() {
        retrieveTweetsButton.setEnabled(false);
        removeAccountButton.setEnabled(false);
        addAccountButton.setEnabled(false);
        retrieveCollectionsButton.setEnabled(false);
    }

    public void enableAllAccountButtons() {
        retrieveTweetsButton.setEnabled(true);
        retrieveTweetsButton.setText("Retrieve Tweets");
        addAccountButton.setEnabled(true);
        retrieveCollectionsButton.setEnabled(true);
        retrieveCollectionsButton.setText("Retrieve Collections");
        removeAccountButton.setEnabled(true);
    }

    public JProgressBar getProgressBar() {
        return tweetDownloadProgressBar;
    }

    public Integer getModelRowForAccount(Account account) {
        int screenNameColumnIndex = accountsTable.getColumnModel().getColumnIndex("Account Name");
        int rowCount = accountsTable.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String screenName = (String) accountsTable.getModel().getValueAt(i, screenNameColumnIndex);
            if (screenName.equals(account.getScreenName())) {
                return i;
            }
        }
        return null;
    }

    public void setTweetRetrievalResults(Pair<OperationResult, ArrayList<StatusJSON>> results, Account account,
            Integer storedTweetCount, Integer receivedTweetCount) {
        OperationResult res = results.getLeft();
        TableModel tm = accountsTable.getModel();
        int modelRow = getModelRowForAccount(account);
        int tweetsRetrievedColumnIndex = accountsTable.getColumnModel().getColumnIndex("Tweets Retrieved");
        String query = "SELECT COUNT(*) AS C FROM tweets WHERE usertwitterid=?";
        DBResponse countResp = CoreDB.customQuerySelect(query, account.getTwitterID());
        if (!countResp.wasSuccessful()) {
            LOGGER.error("Failed to get count of user tweets!");
            GUI.getAccountsPanel().enableAllAccountButtons();
            return;
        } else {
            Long count = (Long) countResp.getReturnedRows().get(0).get("C");
            if (count == null) {
                count = 0L;
            }
            tm.setValueAt(count, modelRow, tweetsRetrievedColumnIndex);
        }

        StatusCode artRetweeterStatusCode = res.getTwitterResponse().getStatusCode();

        if (res.wasSuccessful()) {
            tweetDownloadProgressLabel.setText("<html>Tweet retrieval finished successfully. "
                    + String.valueOf(storedTweetCount) + " stored, out of "
                    + String.valueOf(receivedTweetCount) + " received.</html>");
        } else if (artRetweeterStatusCode.equals(StatusCode.RATE_LIMIT_EXCEEDED_ERROR)) {
            int resetTimeSeconds = (int) res.getTwitterResponse().getReturnedObject();
            tweetDownloadProgressLabel.setText("<html>Twitter rate limit exceeded. You must wait " + String.valueOf(resetTimeSeconds)
                    + " before attempting to retry.</html>");
        } else if (results.getRight() != null) {
            tweetDownloadProgressLabel.setText("<html>An error occurred, but some tweets were successfully retrieved.</html>");
        }
    }

    public void setCollectionRetrievalResults(Object[] results, ArrayList<OperationResult> errorResults, Account account) {
        boolean fatalError = (boolean) results[0];
        int errorCount = (int) results[1];
        int successCount = (int) results[2];

        TableModel tm = accountsTable.getModel();
        int modelRow = getModelRowForAccount(account);
        int collectionsRetrievedColumnIndex = accountsTable.getColumnModel().getColumnIndex("Collections Retrieved");
        String query = "SELECT COUNT(*) AS C FROM collections WHERE usertwitterid=?";
        DBResponse countResp = CoreDB.customQuerySelect(query, account.getTwitterID());
        if (!countResp.wasSuccessful()) {
            LOGGER.error("Failed to get count of user collections!");
            GUI.getAccountsPanel().enableAllAccountButtons();
            return;
        } else {
            Long count = (Long) countResp.getReturnedRows().get(0).get("C");
            if (count == null) {
                count = 0L;
            }
            tm.setValueAt(count, modelRow, collectionsRetrievedColumnIndex);
        }
        if (!fatalError && errorCount == 0) {
            tweetDownloadProgressLabel.setText("<html>Collection retrieval finished successfully.</html>");
        } else if (!fatalError) {
            tweetDownloadProgressLabel.setText("<html>Retrieval completed, but some errors occurred.<br/>"
                    + String.valueOf(successCount) + " collections were retrieved successfully, "
                    + "and " + String.valueOf(errorCount) + " failed and were not retrieved." + "</html>");
        } else {
            tweetDownloadProgressLabel.setText("<html>A fatal error occurred - no collections were retrieved.</html>");
            LOGGER.error("Fatal error: " + errorResults.get(0).getErrorCode().getStatusMessage());
        }
    }

    private void retrieveCollections() {
        if (accountsTable.getRowCount() == 0) {
            String statusMessage = "You must add an account before retrieving collections.";
            JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
            enableAllAccountButtons();
            return;
        }
        int row = accountsTable.getSelectedRow();
        if (row == -1) {
            String statusMessage = "Select an account in the table to retrieve collections for.";
            JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
            enableAllAccountButtons();
            return;
        }
        int modelRow = accountsTable.convertRowIndexToModel(row);
        int accountNameColumnIndex = accountsTable.getColumnModel().getColumnIndex("Account Name");
        String screenName = (String) accountsTable.getModel().getValueAt(modelRow, accountNameColumnIndex);
        Account account;
        DBResponse accountResp = CoreDB.selectFromTable(DBTable.ACCOUNTS,
                new String[]{"screen_name"},
                new Object[]{screenName});
        if (!accountResp.wasSuccessful()) {
            String statusMessage = "Failed to query database for access token!";
            JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
            enableAllAccountButtons();
            return;
        } else if (accountResp.getReturnedRows().isEmpty()) {
            String statusMessage = "Access token not found in database for this user!";
            JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
            enableAllAccountButtons();
            return;
        }
        if (!APIQueryManager.acquireAPILock(true)) {
            return;
        }
        account = ResultSetConversion.getAccount(accountResp.getReturnedRows().get(0));
        String msg = "<html>Retrieving collections might take a few minutes, if you have large collections. <br/>Press OK to proceed.</html>";
        Integer result = JOptionPane.showConfirmDialog(GUI.getInstance(), msg, "Add Account", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            RetrieveCollectionsWorker worker = new RetrieveCollectionsWorker();
            if (!worker.initialise(account, true)) {
                APIQueryManager.releaseAPILock();
                enableAllAccountButtons();
            }
        } else {
            APIQueryManager.releaseAPILock();
            enableAllAccountButtons();
        }
    }

    public void retrieveTweets(boolean showGUI) {
        if (accountsTable.getRowCount() == 0) {
            String statusMessage = "You must add an account before retrieving tweets.";
            GUIHelperMethods.showError(statusMessage, showGUI);
            enableAllAccountButtons();
            return;
        }
        int row = accountsTable.getSelectedRow();
        if (row == -1) {
            String statusMessage = "Select an account in the table to retrieve tweets for.";
            GUIHelperMethods.showError(statusMessage, showGUI);
            enableAllAccountButtons();
            return;
        }
        int modelRow = accountsTable.convertRowIndexToModel(row);
        int screenNameColumnIndex = accountsTable.getColumnModel().getColumnIndex("Account Name");
        String screenName = (String) accountsTable.getModel().getValueAt(modelRow, screenNameColumnIndex);
        DBResponse accountResp = CoreDB.selectFromTable(DBTable.ACCOUNTS,
                new String[]{"screen_name"},
                new Object[]{screenName});
        if (!accountResp.wasSuccessful()) {
            String statusMessage = "Failed to query database for access token!";
            GUIHelperMethods.showError(statusMessage, showGUI);
            enableAllAccountButtons();
            return;
        } else if (accountResp.getReturnedRows().isEmpty()) {
            String statusMessage = "Access token not found in database for this user!";
            GUIHelperMethods.showError(statusMessage, showGUI);
            enableAllAccountButtons();
            return;
        }
        if (!APIQueryManager.acquireAPILock(true)) {
            return;
        }
        Account account = ResultSetConversion.getAccount(accountResp.getReturnedRows().get(0));
        String msg = "<html>Retrieving tweets may take a while if you are retrieving them for the first time. <br/>Press OK to proceed.</html>";
        Integer result = JOptionPane.showConfirmDialog(GUI.getInstance(), msg, "Add Account", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            RetrieveTweetsWorker worker = new RetrieveTweetsWorker();
            if (!worker.initialise(account, showGUI)) {
                APIQueryManager.releaseAPILock();
                enableAllAccountButtons();
            }
        } else {
            APIQueryManager.releaseAPILock();
            enableAllAccountButtons();
        }

    }

    private void addAccount() {
        String query = "SELECT COUNT(*) AS C FROM accounts";
        DBResponse countResp = CoreDB.customQuerySelect(query);
        Long count;
        if (!countResp.wasSuccessful()) {
            LOGGER.error("Failed to initialise accounts panel!");
            return;
        } else {
            count = (Long) countResp.getReturnedRows().get(0).get("C");
            if (count == null) {
                count = 0L;
            }
        }
        if (count >= 2L) {
            String statusMessage = "<html>You may not add more than two accounts. Delete an existing account before adding a new one.</html>";
            JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String msg = "<html>To add a new account, a browser window will be opened and directed to Twitter for authentication."
                + "<br /><br />Press OK to continue.</html>";
        Integer result = JOptionPane.showConfirmDialog(GUI.getInstance(), msg, "Add Account", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        OperationResult reqTokenResult = RESTAPI.oauthRequestToken();
        if (!reqTokenResult.wasSuccessful()) {
            GUIHelperMethods.showErrors(reqTokenResult, LOGGER, null);
            return;
        }
        RequestToken token = (RequestToken) reqTokenResult.getTwitterResponse().getReturnedObject();
        if (token == null) {
            return;
        }

        String authURL = "https://api.twitter.com/oauth/authorize?oauth_token=".concat(token.getToken());
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(authURL));
            } catch (Exception e) {
                LOGGER.error("Unable to direct user to auth URL.", e);
                showAuthenticationURL(authURL);
            }
        } else {
            showAuthenticationURL(authURL);
        }

        boolean cancel = false;
        ArrayList<Object> results = null;
        while (results == null) {
            results = SwingTools.askForUserInput("Enter PIN",
                    "Please enter the PIN shown on the Twitter authentication page.", "PIN:");
            if (results == null) {
                String confirmCancel = "<html>Do you want to cancel adding this account?<br/><br/>"
                        + "Select <b>Yes</b> to go back to entering the PIN, or <b>No</b> to cancel account addition.</html>";
                Integer cancelResult = JOptionPane.showConfirmDialog(GUI.getInstance(), confirmCancel,
                        "Cancel Account Addition", JOptionPane.YES_NO_OPTION);
                if (cancelResult != JOptionPane.YES_OPTION) {
                    cancel = true;
                }
            }
            if (cancel) {
                return;
            }
        }

        String pin = (String) results.get(0);
        OperationResult authResult = RESTAPI.oauthAccessToken(pin, token);
        if (!authResult.wasSuccessful()) {
            GUIHelperMethods.showErrors(reqTokenResult, LOGGER, null);
            return;
        }

        Account account = (Account) authResult.getTwitterResponse().getReturnedObject();
        CoreDB.addAccountToDB(account);
        DefaultTableModel dtm = (DefaultTableModel) accountsTable.getModel();
        dtm.addRow(new Object[]{account.getScreenName(), 0, 0});
        GUI.getMainManagementPanel().getMainTweetsPanel().refreshAccountBoxModel(false);
        GUI.getFailedRetweetsPanel().refreshAccountBoxModel(false);
        ClientRefreshQueue.getInstance().refreshTimers();
        String statusMessage = "<html>Account added successfully!</html>";
        JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAuthenticationURL(String authURL) {
        Font font = new Font("Dialog", Font.PLAIN, 12);

        StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
        style.append("font-weight:").append(font.isBold() ? "bold" : "normal").append(";");
        style.append("font-size:").append(font.getSize()).append("pt;");

        JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">"
                + "Failed to open an Internet browser to authenticate. Please go to this URL to continue authentication: " + authURL
                + "</body></html>");

        ep.setEditable(false);

        JOptionPane.showMessageDialog(GUI.getInstance(), ep, "Authentication", JOptionPane.INFORMATION_MESSAGE);
    }

    private void removeAccount() {
        int row = accountsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        int modelRow = accountsTable.convertRowIndexToModel(row);
        int accountNameColumnIndex = accountsTable.getColumnModel().getColumnIndex("Account Name");
        String screenName = (String) accountsTable.getModel().getValueAt(modelRow, accountNameColumnIndex);

        DBResponse accountResp = CoreDB.selectFromTable(DBTable.ACCOUNTS,
                new String[]{"screen_name"},
                new Object[]{screenName});
        if (!accountResp.wasSuccessful()) {
            String statusMessage = "Failed to get account information from DB!";
            JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (accountResp.getReturnedRows().isEmpty()) {
            String statusMessage = "Could not find account in DB - has the database folder been modified?";
            JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Account account = ResultSetConversion.getAccount(accountResp.getReturnedRows().get(0));
        String confirmDelete = "<html>Are you sure you want to delete account '" + screenName
                + "' from ArtRetweeter? This action cannot be undone.<br/><br/>"
                + "Note that this method does not revoke access to this application. "
                + "To do that, you must log into the Twitter<br/>website and go to Settings->Security and "
                + "account access->Apps and sessions to revoke permissions."
                + "</html>";
        Integer deleteResult = JOptionPane.showConfirmDialog(GUI.getInstance(), confirmDelete,
                "Delete Account", JOptionPane.YES_NO_OPTION);
        if (deleteResult != JOptionPane.YES_OPTION) {
            return;
        }
        OperationResult res = ServerAPI.removeAccount(account);
        if (!res.wasSuccessful()) {
            String errorMessage = "An error occurred in deleting the account - check log output.";
            ArrayList<Boolean> bools = (ArrayList<Boolean>) res.getServerResponse().getReturnedObject();
            if (bools != null) {
                Boolean failQueueCleared = bools.get(0);
                Boolean scheduleQueueCleared = bools.get(1);
                Boolean userCleared = bools.get(2);
                LOGGER.error("Failed to delete account from ArtRetweeter server.");
                LOGGER.error("Failure queue cleared: " + failQueueCleared);
                LOGGER.error("Schedule queue cleared: " + scheduleQueueCleared);
                LOGGER.error("User cleared: " + userCleared);
            }
            JOptionPane.showMessageDialog(GUI.getInstance(), errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String deleteQuery = "DELETE FROM retweetsqueue WHERE retweetingusertwitterid=?";
        CoreDB.runCustomUpdate(deleteQuery, new Object[]{account.getTwitterID()});
        deleteQuery = "DELETE FROM tweets WHERE usertwitterid=? "
                + " AND tweetid NOT IN (SELECT tweetid FROM collections WHERE usertwitterid != ?)";
        CoreDB.runCustomUpdate(deleteQuery, new Object[]{account.getTwitterID(), account.getTwitterID()});
        deleteQuery = "DELETE FROM accounts WHERE id=?";
        CoreDB.runCustomUpdate(deleteQuery, new Object[]{account.getId()});
        String statusMessage = "Account removed successfully. All scheduled retweets have been deleted from the server.";
        JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateAccountsTableForTweets(Integer countBeforeStart, Pair<Integer, Integer> last, String screenName) {
        int accountNameColumnIndex = accountsTable.getColumnModel().getColumnIndex("Account Name");
        int tweetsRetrievedColumnIndex = accountsTable.getColumnModel().getColumnIndex("Tweets Retrieved");
        String tweetsRetrieved;
        if (last.getLeft() == 1) {
            tweetsRetrieved = "tweet";
        } else {
            tweetsRetrieved = "tweets";
        }
        TableModel tm = accountsTable.getModel();
        int rowCount = accountsTable.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String tableScreenName = (String) tm.getValueAt(i, accountNameColumnIndex);
            if (tableScreenName.equals(screenName)) {
                Integer newRetrievedCount = last.getLeft();
                Integer newReceivedCount = last.getRight();
                tm.setValueAt(countBeforeStart + newRetrievedCount, i, tweetsRetrievedColumnIndex);
                tweetDownloadProgressLabel.setText("<html>Retrieving... ".concat(String.valueOf(newRetrievedCount)).concat(" ").concat(tweetsRetrieved)
                        .concat(" added so far (out of ").concat(String.valueOf(newReceivedCount).concat(" received).</html>")));
                break;
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable accountsTable;
    private javax.swing.JButton addAccountButton;
    private javax.swing.JLabel automaticNoteLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton removeAccountButton;
    private javax.swing.JButton retrieveCollectionsButton;
    private javax.swing.JButton retrieveTweetsButton;
    private javax.swing.JProgressBar tweetDownloadProgressBar;
    private javax.swing.JLabel tweetDownloadProgressLabel;
    // End of variables declaration//GEN-END:variables

}
