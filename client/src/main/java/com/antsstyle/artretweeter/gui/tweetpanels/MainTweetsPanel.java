/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui.tweetpanels;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.CachedVariable;
import com.antsstyle.artretweeter.datastructures.CollectionCurateParamsJSON;
import com.antsstyle.artretweeter.datastructures.CollectionCurateRespJSON;
import com.antsstyle.artretweeter.datastructures.CollectionOperation;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.StatusJSON;
import com.antsstyle.artretweeter.datastructures.TableFilterEntry;
import com.antsstyle.artretweeter.datastructures.TableTimestamp;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import com.antsstyle.artretweeter.db.CachedVariableDB;
import com.antsstyle.artretweeter.db.CollectionsDB;
import com.antsstyle.artretweeter.db.ConfigDB;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.db.TweetsDB;
import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.gui.GUIHelperMethods;
import com.antsstyle.artretweeter.gui.SetTableFilteringPanel;
import com.antsstyle.artretweeter.gui.SetTableSortingPanel;
import com.antsstyle.artretweeter.serverapi.ServerAPI;
import com.antsstyle.artretweeter.tools.FormatTools;
import com.antsstyle.artretweeter.tools.RegularExpressions;
import com.antsstyle.artretweeter.twitter.RESTAPI;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class MainTweetsPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = LogManager.getLogger(MainTweetsPanel.class);

    private Account currentlySelectedAccount;
    private final DefaultComboBoxModel selectAccountBoxModel = new DefaultComboBoxModel();
    private final SetTableFilteringPanel setTableFilteringPanel = new SetTableFilteringPanel();
    private final SetTableSortingPanel setTableSortingPanel = new SetTableSortingPanel();
    private final ArrayList<TableFilterEntry> currentTableFilters = new ArrayList<>();

    protected static final Account ALL_TWEETS_ACCOUNT = new Account()
            .setScreenName("<show all tweets>");

    protected static final Account NO_ACCOUNTS = new Account()
            .setScreenName("<no accounts added>");

    protected static final Account DB_ERROR_ACCOUNT = new Account()
            .setScreenName("<database error>");

    protected static final TwitterCollectionHolder SELECT_ACCOUNT_FIRST = new TwitterCollectionHolder()
            .setName("<select an account first>");

    protected static final TwitterCollectionHolder NO_COLLECTIONS = new TwitterCollectionHolder()
            .setName("<none>");

    protected static final TwitterCollectionHolder DB_ERROR_COLLECTION = new TwitterCollectionHolder()
            .setName("<database error>");

    /**
     * Creates new form MainTweetPanel
     */
    public MainTweetsPanel() {
        initComponents();
        tweetsTable.getSelectionModel().addListSelectionListener((ListSelectionEvent event) -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int[] rows = tweetsTable.getSelectedRows();
            if (rows.length != 1) {
                return;
            }
            GUIHelperMethods.showTweetPreview(tweetsTable);
        });
    }

    public void initialise() {
        currentTableFilters.addAll(ConfigDB.getTweetManagementTableFilterSettings());
        refreshAccountBoxModel(true);
        GUIHelperMethods.setGUIColours(setTableSortingPanel);
        GUIHelperMethods.setGUIColours(setTableFilteringPanel);
        Pair<String[], String[]> settings = ConfigDB.getTweetManagementTableSortSettings();
        if (settings != null) {
            setTableSort(settings, false);
        }
    }

    public Account getSelectedAccount() {
        return currentlySelectedAccount;
    }

    public JTable getTweetsTable() {
        return tweetsTable;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jScrollPane26 = new javax.swing.JScrollPane();
        tweetsTable = new javax.swing.JTable();
        selectAccountComboBox = new javax.swing.JComboBox<>();
        queueRetweetButton = new javax.swing.JButton();
        addTweetsToCurrentlySelectedCollectionButton = new javax.swing.JButton();
        deleteTweetsFromArtRetweeterButton = new javax.swing.JButton();
        deleteTweetsFromTwitterButton = new javax.swing.JButton();
        addTweetManuallyTextField = new javax.swing.JTextField();
        addTweetManuallyButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        addTweetManuallyStatusLabel = new javax.swing.JLabel();
        showDescriptionOfTableColumnsButton = new javax.swing.JButton();
        setTableSortingButton = new javax.swing.JButton();
        setTableFilteringButton = new javax.swing.JButton();

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel2.setText("Select account: ");

        jScrollPane26.setMaximumSize(new java.awt.Dimension(898, 184));
        jScrollPane26.setMinimumSize(new java.awt.Dimension(898, 184));
        jScrollPane26.setPreferredSize(new java.awt.Dimension(898, 184));

        tweetsTable.setAutoCreateRowSorter(true);
        tweetsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Tweet Text", "Date Posted", "Retweets", "Likes", "RT#", "Pending RT"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Long.class, java.lang.String.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tweetsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tweetsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tweetsTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane26.setViewportView(tweetsTable);
        if (tweetsTable.getColumnModel().getColumnCount() > 0) {
            tweetsTable.getColumnModel().getColumn(0).setMinWidth(40);
            tweetsTable.getColumnModel().getColumn(0).setPreferredWidth(40);
            tweetsTable.getColumnModel().getColumn(0).setMaxWidth(40);
            tweetsTable.getColumnModel().getColumn(2).setMinWidth(125);
            tweetsTable.getColumnModel().getColumn(2).setPreferredWidth(125);
            tweetsTable.getColumnModel().getColumn(2).setMaxWidth(125);
            tweetsTable.getColumnModel().getColumn(3).setMinWidth(70);
            tweetsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
            tweetsTable.getColumnModel().getColumn(3).setMaxWidth(70);
            tweetsTable.getColumnModel().getColumn(4).setMinWidth(70);
            tweetsTable.getColumnModel().getColumn(4).setPreferredWidth(70);
            tweetsTable.getColumnModel().getColumn(4).setMaxWidth(70);
            tweetsTable.getColumnModel().getColumn(5).setMinWidth(40);
            tweetsTable.getColumnModel().getColumn(5).setPreferredWidth(40);
            tweetsTable.getColumnModel().getColumn(5).setMaxWidth(40);
            tweetsTable.getColumnModel().getColumn(6).setMinWidth(90);
            tweetsTable.getColumnModel().getColumn(6).setPreferredWidth(90);
            tweetsTable.getColumnModel().getColumn(6).setMaxWidth(90);
        }

        selectAccountComboBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        selectAccountComboBox.setModel(selectAccountBoxModel);
        selectAccountComboBox.setMaximumSize(new java.awt.Dimension(250, 26));
        selectAccountComboBox.setMinimumSize(new java.awt.Dimension(250, 26));
        selectAccountComboBox.setPreferredSize(new java.awt.Dimension(250, 26));
        selectAccountComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAccountComboBoxActionPerformed(evt);
            }
        });

        queueRetweetButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        queueRetweetButton.setText("Queue Retweet");
        queueRetweetButton.setMaximumSize(new java.awt.Dimension(147, 33));
        queueRetweetButton.setMinimumSize(new java.awt.Dimension(147, 33));
        queueRetweetButton.setPreferredSize(new java.awt.Dimension(147, 33));
        queueRetweetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queueRetweetButtonActionPerformed(evt);
            }
        });

        addTweetsToCurrentlySelectedCollectionButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        addTweetsToCurrentlySelectedCollectionButton.setText("Add tweets to currently selected collection");
        addTweetsToCurrentlySelectedCollectionButton.setToolTipText("");
        addTweetsToCurrentlySelectedCollectionButton.setMaximumSize(new java.awt.Dimension(323, 33));
        addTweetsToCurrentlySelectedCollectionButton.setMinimumSize(new java.awt.Dimension(323, 33));
        addTweetsToCurrentlySelectedCollectionButton.setPreferredSize(new java.awt.Dimension(323, 33));
        addTweetsToCurrentlySelectedCollectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTweetsToCurrentlySelectedCollectionButtonActionPerformed(evt);
            }
        });

        deleteTweetsFromArtRetweeterButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        deleteTweetsFromArtRetweeterButton.setText("Delete tweets from ArtRetweeter");
        deleteTweetsFromArtRetweeterButton.setToolTipText("");
        deleteTweetsFromArtRetweeterButton.setMaximumSize(new java.awt.Dimension(278, 33));
        deleteTweetsFromArtRetweeterButton.setMinimumSize(new java.awt.Dimension(278, 33));
        deleteTweetsFromArtRetweeterButton.setPreferredSize(new java.awt.Dimension(278, 33));
        deleteTweetsFromArtRetweeterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteTweetsFromArtRetweeterButtonActionPerformed(evt);
            }
        });

        deleteTweetsFromTwitterButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        deleteTweetsFromTwitterButton.setText("Delete tweets from Twitter");
        deleteTweetsFromTwitterButton.setToolTipText("");
        deleteTweetsFromTwitterButton.setMaximumSize(new java.awt.Dimension(278, 33));
        deleteTweetsFromTwitterButton.setMinimumSize(new java.awt.Dimension(278, 33));
        deleteTweetsFromTwitterButton.setPreferredSize(new java.awt.Dimension(278, 33));
        deleteTweetsFromTwitterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteTweetsFromTwitterButtonActionPerformed(evt);
            }
        });

        addTweetManuallyTextField.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        addTweetManuallyButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        addTweetManuallyButton.setText("Add");
        addTweetManuallyButton.setMaximumSize(new java.awt.Dimension(69, 33));
        addTweetManuallyButton.setMinimumSize(new java.awt.Dimension(69, 33));
        addTweetManuallyButton.setPreferredSize(new java.awt.Dimension(69, 33));
        addTweetManuallyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTweetManuallyButtonActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel4.setText("Add tweet URL to ArtRetweeter manually:");

        showDescriptionOfTableColumnsButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        showDescriptionOfTableColumnsButton.setText("Show description of table columns");
        showDescriptionOfTableColumnsButton.setToolTipText("");
        showDescriptionOfTableColumnsButton.setMaximumSize(new java.awt.Dimension(278, 33));
        showDescriptionOfTableColumnsButton.setMinimumSize(new java.awt.Dimension(278, 33));
        showDescriptionOfTableColumnsButton.setPreferredSize(new java.awt.Dimension(278, 33));
        showDescriptionOfTableColumnsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showDescriptionOfTableColumnsButtonActionPerformed(evt);
            }
        });

        setTableSortingButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        setTableSortingButton.setText("Set table sorting");
        setTableSortingButton.setToolTipText("");
        setTableSortingButton.setMaximumSize(new java.awt.Dimension(194, 33));
        setTableSortingButton.setMinimumSize(new java.awt.Dimension(194, 33));
        setTableSortingButton.setPreferredSize(new java.awt.Dimension(194, 33));
        setTableSortingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setTableSortingButtonActionPerformed(evt);
            }
        });

        setTableFilteringButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        setTableFilteringButton.setText("Set table filtering");
        setTableFilteringButton.setToolTipText("");
        setTableFilteringButton.setMaximumSize(new java.awt.Dimension(194, 33));
        setTableFilteringButton.setMinimumSize(new java.awt.Dimension(194, 33));
        setTableFilteringButton.setPreferredSize(new java.awt.Dimension(194, 33));
        setTableFilteringButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setTableFilteringButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addTweetManuallyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 276, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addTweetManuallyButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addTweetManuallyStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(selectAccountComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(showDescriptionOfTableColumnsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 287, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(queueRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(addTweetsToCurrentlySelectedCollectionButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(deleteTweetsFromArtRetweeterButton, javax.swing.GroupLayout.PREFERRED_SIZE, 278, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(deleteTweetsFromTwitterButton, javax.swing.GroupLayout.PREFERRED_SIZE, 278, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(setTableSortingButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(setTableFilteringButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 200, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(selectAccountComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(showDescriptionOfTableColumnsButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane26, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(queueRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addTweetsToCurrentlySelectedCollectionButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteTweetsFromArtRetweeterButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deleteTweetsFromTwitterButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(setTableSortingButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(setTableFilteringButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(addTweetManuallyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(addTweetManuallyButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(addTweetManuallyStatusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void selectAccountComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAccountComboBoxActionPerformed
        if (selectAccountComboBox.isEnabled()) {
            Account acc = (Account) selectAccountComboBox.getSelectedItem();
            if (!acc.equals(currentlySelectedAccount)) {
                currentlySelectedAccount = acc;
                refreshTweetsTable(true);
                GUI.getMainManagementPanel().getQueueSubPanel().refreshQueueTable();
            }
        }
    }//GEN-LAST:event_selectAccountComboBoxActionPerformed

    private void addTweetsToCollection() {
        TwitterCollectionHolder currentlySelectedCollection = GUI.getMainManagementPanel().getSelectedCollection();
        if (currentlySelectedCollection.equals(NO_COLLECTIONS)) {
            String msg = "Select a collection to add this tweet to first.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (currentlySelectedCollection.equals(DB_ERROR_COLLECTION)) {
            String msg = "You cannot add tweets to collections until the database error is resolved.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (currentlySelectedCollection.equals(SELECT_ACCOUNT_FIRST)) {
            String msg = "You cannot add tweets to collections until the database error is resolved.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int[] rows = tweetsTable.getSelectedRows();
        if (rows.length == 0) {
            return;
        }
        int[] modelRows = new int[rows.length];
        for (int i = 0; i < rows.length; i++) {
            modelRows[i] = tweetsTable.convertRowIndexToModel(rows[i]);
        }
        int idColumnIndex = tweetsTable.getColumnModel().getColumnIndex("ID");
        HashMap<Long, CollectionOperation> curationParameters = new HashMap<>();
        ArrayList<Object> tweetDBParams = new ArrayList<>();
        ArrayList<Integer> tweetIDs = new ArrayList<>();
        HashMap<Integer, Integer> dbIDModelRowMap = new HashMap<>();
        for (int i = 0; i < modelRows.length; i++) {
            Integer id = (Integer) tweetsTable.getModel().getValueAt(modelRows[i], idColumnIndex);
            if (!GUI.getMainManagementPanel().getCollectionsSubPanel().checkTweetInCollectionTable(id)) {
                tweetDBParams.add(id);
                tweetIDs.add(id);
                dbIDModelRowMap.put(id, modelRows[i]);
            }
        }
        if (tweetDBParams.isEmpty()) {
            String msg = "The tweets you have selected are already in the specified collection.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String query = "SELECT * FROM tweets WHERE id IN (?";
        for (int i = 1; i < tweetDBParams.size(); i++) {
            query = query.concat(",?");
        }
        query = query.concat(")");

        DBResponse resp = CoreDB.customQuerySelect(query, tweetDBParams.toArray(new Object[tweetDBParams.size()]));
        if (!resp.wasSuccessful()) {
            String msg = "Failed to retrieve tweet information from DB - check log output.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (resp.getReturnedRows().isEmpty()) {
            String msg = "Tweet records do not exist in DB - has the database file been modified?";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ArrayList<HashMap<String, Object>> dbRows = resp.getReturnedRows();
        HashMap<Long, TweetHolder> validTweets = new HashMap<>();
        for (HashMap<String, Object> dbRow : dbRows) {
            TweetHolder tweet = ResultSetConversion.getTweet(dbRow);
            if (tweetIDs.contains(tweet.getId())) {
                curationParameters.put(tweet.getTweetID(), CollectionOperation.ADD);
                validTweets.put(tweet.getTweetID(), tweet);
            }
        }
        OperationResult result;
        if (validTweets.size() == 1) {
            ArrayList<Long> keys = new ArrayList<>(validTweets.keySet());
            result = RESTAPI.collectionsEntriesAdd(currentlySelectedCollection.getTwitterID(),
                    validTweets.get(keys.get(0)).getTweetID(), currentlySelectedAccount);
        } else {
            CollectionCurateParamsJSON jsonData = new CollectionCurateParamsJSON()
                    .setId(currentlySelectedCollection.getTwitterID());
            jsonData.setChanges(curationParameters);
            result = RESTAPI.collectionsEntriesCurate(jsonData, currentlySelectedAccount);
        }

        if (!result.wasSuccessful()) {
            String msg = "Twitter API returned an error: " + result.getErrorCode().getStatusMessage();
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ArrayList<Object[]> insertParams = new ArrayList<>();
        if (validTweets.size() == 1) {
            ArrayList<Long> keys = new ArrayList<>(validTweets.keySet());
            Object[] rowToAdd = new Object[]{validTweets.get(keys.get(0)).getId(), validTweets.get(keys.get(0)).getFullTweetText()};
            GUI.getMainManagementPanel().getCollectionsSubPanel().addRowToCollectionsTable(rowToAdd);
            insertParams.add(new Object[]{validTweets.get(keys.get(0)).getTweetID(), currentlySelectedCollection.getTwitterID()});
            CollectionsDB.parameterisedCollectionTweetsMergeBatch(insertParams);
        } else {
            CollectionCurateRespJSON json = (CollectionCurateRespJSON) result.getTwitterResponse().getReturnedObject();
            CollectionCurateRespJSON.CollectionCurateResponse.CollectionCurateEntry[] entries = json.getResponse().getEntries();
            if (entries == null) {
                entries = new CollectionCurateRespJSON.CollectionCurateResponse.CollectionCurateEntry[0];
            }
            HashMap<Long, String> failedEntries = new HashMap<>();
            for (CollectionCurateRespJSON.CollectionCurateResponse.CollectionCurateEntry entry : entries) {
                failedEntries.put(entry.getEntry().getTweetID(), entry.getEntry().getOp());
            }
            Set<Long> attemptedIDs = validTweets.keySet();
            Set<Long> failedEntryKeys = failedEntries.keySet();
            for (Long tweetID : attemptedIDs) {
                if (!failedEntryKeys.contains(tweetID)) {
                    insertParams.add(new Object[]{tweetID, currentlySelectedCollection.getTwitterID()});
                }
            }
            ArrayList<Long> keys = new ArrayList<>(validTweets.keySet());
            for (Long k : keys) {
                Object[] rowToAdd = new Object[]{validTweets.get(k).getId(), validTweets.get(k).getFullTweetText()};
                GUI.getMainManagementPanel().getCollectionsSubPanel().addRowToCollectionsTable(rowToAdd);
            }
            if (!insertParams.isEmpty()) {
                CollectionsDB.parameterisedCollectionTweetsMergeBatch(insertParams);
            }
            if (!failedEntries.isEmpty()) {
                StringBuilder msg = new StringBuilder("<html>One or more entries were not added successfully.<br/><br/>");
                for (Long l : failedEntryKeys) {
                    String reason = failedEntries.get(l);
                    msg = msg.append("Tweet ID ").append(dbIDModelRowMap.get(validTweets.get(l).getId()))
                            .append(" failed with reason: ").append(reason).append("<br/>");
                }
                JOptionPane.showMessageDialog(GUI.getInstance(), msg.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    protected boolean noAccountsInBoxModel() {
        if (selectAccountBoxModel.getSize() == 0) {
            return true;
        } else if (selectAccountBoxModel.getSize() == 1) {
            Account account = (Account) selectAccountBoxModel.getSelectedItem();
            return (account.equals(NO_ACCOUNTS) || account.equals(DB_ERROR_ACCOUNT));
        }
        return false;
    }

    public void refreshAccountBoxModel(boolean initialRefresh) {
        selectAccountComboBox.setEnabled(false);
        boolean noElementsBefore = noAccountsInBoxModel();
        selectAccountBoxModel.removeAllElements();
        DBResponse resp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to get collections data to refresh combo box model!");
            currentlySelectedAccount = DB_ERROR_ACCOUNT;
            selectAccountBoxModel.setSelectedItem(selectAccountBoxModel.getElementAt(0));
            selectAccountComboBox.setEnabled(true);
            selectAccountBoxModel.removeAllElements();
            return;
        }
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            Account account = ResultSetConversion.getAccount(row);
            selectAccountBoxModel.addElement(account);
        }
        if (selectAccountBoxModel.getSize() != 0) {
            selectAccountBoxModel.setSelectedItem(selectAccountBoxModel.getElementAt(0));
            currentlySelectedAccount = (Account) selectAccountBoxModel.getElementAt(0);
            if (initialRefresh || noElementsBefore) {
                refreshTweetsTable(true);
                GUI.getMainManagementPanel().getCollectionsSubPanel().refreshCollectionBoxModel(false);
            }
        } else {
            selectAccountBoxModel.addElement(NO_ACCOUNTS);
            currentlySelectedAccount = NO_ACCOUNTS;
            selectAccountBoxModel.setSelectedItem(selectAccountBoxModel.getElementAt(0));
        }
        selectAccountComboBox.setEnabled(true);
    }

    private void addTweetManually() {
        String url = addTweetManuallyTextField.getText().trim();
        if (RegularExpressions.matchesRegex(url, RegularExpressions.TWITTER_STATUS_REGEX)) {
            Path tweetFolderPath = ConfigDB.getTweetFolderPath(currentlySelectedAccount);
            if (tweetFolderPath == null) {
                String statusMessage = "Failed to get tweet image directory information from database!";
                JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Long tweetID = Long.valueOf(url.substring(url.lastIndexOf("/") + 1));
            OperationResult res = RESTAPI.getTweetByID(tweetID, currentlySelectedAccount, tweetFolderPath, true, true);
            if (res.wasSuccessful()) {
                StatusJSON status = (StatusJSON) res.getTwitterResponse().getReturnedObject();
                if (!status.getUser().getId().equals(currentlySelectedAccount.getTwitterID())) {
                    String statusMessage = "Failed to get tweet image directory information from database!";
                    JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                DefaultTableModel dtm = (DefaultTableModel) tweetsTable.getModel();
                TableTimestamp tableTimestamp;
                try {
                    Date date = FormatTools.TWITTER_DATE_FORMAT.parse(status.getCreated_at());
                    tableTimestamp = new TableTimestamp(new Timestamp(date.getTime()));
                } catch (Exception e) {
                    LOGGER.error("Failed to parse Twitter date string! String was: " + status.getCreated_at(), e);
                    String statusMessage = "Failed to parse twitter date string - check log output.";
                    JOptionPane.showMessageDialog(GUI.getInstance(), statusMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                dtm.addRow(new Object[]{status.getInternalDatabaseID(), status.getText(), tableTimestamp,
                    status.getRetweet_count(), status.getFavorite_count(), 0, false});
                refreshTweetsTable(true);
                addTweetManuallyTextField.setText("");
                addTweetManuallyStatusLabel.setText("Tweet added successfully.");
            } else {
                GUIHelperMethods.showErrors(res, LOGGER, null);
            }
        } else {
            String msg = "The entered URL is not a valid Twitter status URL.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
        }
    }

    private void queueRetweetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_queueRetweetButtonActionPerformed
        queueRetweetButton.setEnabled(false);
        GUI.getMainManagementPanel().getQueueSubPanel().disableTableListener();
        try {
            ServerAPI.queueRetweet(currentlySelectedAccount, false);
        } catch (Exception e) {
            LOGGER.error("Failed to queue retweet", e);
        }
        GUI.getMainManagementPanel().getQueueSubPanel().enableTableListener();
        queueRetweetButton.setEnabled(true);
    }//GEN-LAST:event_queueRetweetButtonActionPerformed

    private void deleteTweetsFromArtRetweeterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteTweetsFromArtRetweeterButtonActionPerformed
        deleteTweetsFromArtRetweeterButton.setEnabled(false);
        deleteTweetsFromArtRetweeter();
        deleteTweetsFromArtRetweeterButton.setEnabled(true);
    }//GEN-LAST:event_deleteTweetsFromArtRetweeterButtonActionPerformed

    private void deleteTweetsFromTwitterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteTweetsFromTwitterButtonActionPerformed
        deleteTweetsFromTwitterButton.setEnabled(false);
        deleteTweetsFromTwitter();
        deleteTweetsFromTwitterButton.setEnabled(true);
    }//GEN-LAST:event_deleteTweetsFromTwitterButtonActionPerformed

    private void deleteTweetsFromArtRetweeter() {
        int[] rows = tweetsTable.getSelectedRows();
        if (rows.length == 0) {
            return;
        }
        String confirmMsg = "<html>This will delete the selected tweets from the ArtRetweeter client. They will remain on your Twitter account."
                + "<br/><br/>Press OK if you want to proceed.</html>";
        Integer result = JOptionPane.showConfirmDialog(GUI.getInstance(), confirmMsg, "Delete Tweets", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        deleteTweetsFromArtRetweeterButton.setText("Deleting...");
        Integer[] modelRows = new Integer[rows.length];
        for (int i = 0; i < rows.length; i++) {
            modelRows[i] = tweetsTable.convertRowIndexToModel(rows[i]);
        }
        int idColumnIndex = tweetsTable.getColumnModel().getColumnIndex("ID");
        ArrayList<Integer> ids = new ArrayList<>();
        for (int modelRow : modelRows) {
            ids.add((Integer) tweetsTable.getModel().getValueAt(modelRow, idColumnIndex));
        }
        ArrayList<Long> tweetIDs = TweetsDB.getTweetIDsByDatabaseIDs(ids);
        boolean success = false;
        try {
            success = TweetsDB.deleteTweetsUsingDatabaseIDs(ids, false);
        } catch (Exception e) {
            LOGGER.error("Failed to delete tweets from database", e);
        }
        if (success) {
            if (tweetIDs != null && !tweetIDs.isEmpty()) {
                String tweetString = "";
                for (Long tweetID : tweetIDs) {
                    tweetString = tweetString.concat(String.valueOf(tweetID)).concat(",");
                }
                tweetString = tweetString.substring(0, tweetString.length() - 1);
                OperationResult res = ServerAPI.deleteTweets(currentlySelectedAccount, tweetString);
                if (!res.wasSuccessful()) {
                    LOGGER.error("Deleting tweets on server failed. Error code: " + res.getErrorCode());
                    LOGGER.error(res.getServerResponse().getLogMessage());
                }
            }
        } else {
            LOGGER.error("Failed to delete tweets from DB, did not delete on server.");
        }
        deleteTweetsFromArtRetweeterButton.setText("Delete tweets from ArtRetweeter");
        refreshTweetsTable(true);
    }

    private void deleteTweetsFromTwitter() {
        int[] rows = tweetsTable.getSelectedRows();
        if (rows.length == 0) {
            return;
        }
        String confirmMsg = "<html>WARNING: This will delete the selected tweets from your Twitter account, not just this app."
                + "<br/><br/>This action cannot be reversed. Are you sure?</html>";
        Integer result = JOptionPane.showConfirmDialog(GUI.getInstance(), confirmMsg, "Delete Tweets", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        Integer[] modelRows = new Integer[rows.length];
        for (int i = 0; i < rows.length; i++) {
            modelRows[i] = tweetsTable.convertRowIndexToModel(rows[i]);
        }
        int idColumnIndex = tweetsTable.getColumnModel().getColumnIndex("ID");
        ArrayList<Integer> ids = new ArrayList<>();
        for (int modelRow : modelRows) {
            ids.add((Integer) tweetsTable.getModel().getValueAt(modelRow, idColumnIndex));
        }
        ArrayList<Long> tweetIDs = TweetsDB.getTweetIDsByDatabaseIDs(ids);
        if (tweetIDs == null) {
            String msg = "Error converting internal database IDs.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        int errorCount = 0;
        ArrayList<Long> tweetIDsToDeleteFromDB = new ArrayList<>();
        for (int i = 0; i < tweetIDs.size(); i++) {
            Long tweetID = tweetIDs.get(i);
            deleteTweetsFromTwitterButton.setText("Deleting tweet " + i + 1 + " of " + tweetIDs.size());
            OperationResult res = RESTAPI.statusesDestroy(tweetID, currentlySelectedAccount);
            if (!res.wasSuccessful()) {
                errorCount++;
            } else {
                tweetIDsToDeleteFromDB.add(tweetID);
            }
        }
        TweetsDB.deleteTweets(tweetIDsToDeleteFromDB, true);
        if (errorCount > 0) {
            String msg = String.valueOf(errorCount) + " tweets failed to delete correctly.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
        }
        deleteTweetsFromTwitterButton.setText("Delete tweets from Twitter");
        refreshTweetsTable(true);
    }

    public void refreshTweetsTable(boolean showGUI) {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }
        ArrayList<Object> params = new ArrayList<>();
        params.add(currentlySelectedAccount.getTwitterID());
        String query = "SELECT tweets.*,(SELECT COUNT(id) FROM retweetrecords WHERE "
                + "tweets.tweetid=retweetrecords.tweetid AND tweets.usertwitterid=retweetrecords.usertwitterid) AS numretweets"
                + " FROM tweets WHERE tweets.usertwitterid=?";

        String queuedTweetsQuery = "SELECT retweetqueue.retweettime,"
                + "(SELECT id FROM tweets WHERE tweets.tweetid=retweetqueue.tweetid) AS tweetdatabaseid,"
                + "(SELECT fulltweettext FROM tweets WHERE tweets.tweetid=retweetqueue.tweetid) AS text "
                + "FROM retweetqueue WHERE retweetingusertwitterid=? ORDER BY retweettime ASC";
        TableFilterEntry pendingRTEntry = null;
        TableFilterEntry numRetweetsEntry = null;
        for (TableFilterEntry entry : currentTableFilters) {
            if (!entry.getFilterEnabled()) {
                continue;
            }
            if (entry.getDatabaseFieldName().equals("pendingrt")) {
                pendingRTEntry = entry;
                continue;
            }
            if (entry.getDatabaseFieldName().equals("numretweets")) {
                numRetweetsEntry = entry;
                continue;
            }
            query = query.concat(" AND ").concat(entry.getDatabaseFieldName()).concat(entry.getOperator())
                    .concat("?");
            params.add(entry.getFieldValue());
        }
        if (numRetweetsEntry != null) {
            if (numRetweetsEntry.getFilterEnabled()) {
                String selectSubQuery = "(SELECT COUNT(id) FROM retweetrecords WHERE tweets.tweetid=retweetrecords.tweetid "
                        + "AND tweets.usertwitterid=retweetrecords.usertwitterid)";
                query = query.concat(" AND ").concat(selectSubQuery).concat(numRetweetsEntry.getOperator())
                        .concat("?");
                params.add(numRetweetsEntry.getFieldValue());
            }
        }

        LOGGER.debug(query);

        DBResponse resp = CoreDB.customQuerySelect(query, params);
        DBResponse queuedTweetsResp = CoreDB.customQuerySelect(queuedTweetsQuery, currentlySelectedAccount.getTwitterID());

        if (!resp.wasSuccessful()) {
            String msg = "Failed to retrieve tweets for this user from DB!";
            GUIHelperMethods.showError(msg, showGUI);
            return;
        }
        if (!queuedTweetsResp.wasSuccessful()) {
            String msg = "Failed to retrieve queued tweets for this user from DB!";
            GUIHelperMethods.showError(msg, showGUI);
            return;
        }
        ArrayList<HashMap<String, Object>> queuedTweetRows = queuedTweetsResp.getReturnedRows();
        ArrayList<Integer> queuedTweetIDs = new ArrayList<>();
        for (HashMap<String, Object> row : queuedTweetRows) {
            Integer id = (Integer) row.get("TWEETDATABASEID");
            queuedTweetIDs.add(id);
        }
        DefaultTableModel dtm = (DefaultTableModel) tweetsTable.getModel();
        dtm.setRowCount(0);
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            TweetHolder tweet = ResultSetConversion.getTweet(row);
            boolean queued = queuedTweetIDs.contains(tweet.getId());
            if (pendingRTEntry != null && pendingRTEntry.getFilterEnabled()) {
                if (pendingRTEntry.getFieldValue().equals("true") && !queued) {
                    continue;
                } else if (pendingRTEntry.getFieldValue().equals("false") && queued) {
                    continue;
                }
            }
            Long retweetCount = (Long) row.get("NUMRETWEETS");
            TableTimestamp tableTimestamp = new TableTimestamp(tweet.getCreatedAt());
            dtm.addRow(new Object[]{tweet.getId(), tweet.getFullTweetText(), tableTimestamp, tweet.getRetweetCount(), tweet.getLikeCount(),
                retweetCount, queued});
        }
    }

    private void addTweetManuallyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTweetManuallyButtonActionPerformed
        addTweetManuallyButton.setEnabled(false);
        addTweetManually();
        addTweetManuallyButton.setEnabled(true);
    }//GEN-LAST:event_addTweetManuallyButtonActionPerformed

    private void addTweetsToCurrentlySelectedCollectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTweetsToCurrentlySelectedCollectionButtonActionPerformed
        addTweetsToCurrentlySelectedCollectionButton.setEnabled(false);
        addTweetsToCollection();
        addTweetsToCurrentlySelectedCollectionButton.setEnabled(true);
    }//GEN-LAST:event_addTweetsToCurrentlySelectedCollectionButtonActionPerformed

    private void showDescriptionOfTableColumnsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showDescriptionOfTableColumnsButtonActionPerformed
        String message = "<html>ID: The internal ArtRetweeter ID for this tweet.<br/><br/>"
                + "Tweet Text: The text of the tweet.<br/><br/>"
                + "Date Posted: The date this tweet was originally posted on Twitter.<br/><br/>"
                + "Retweets: The number of retweets this tweet has on Twitter (at the time the tweet was retrieved).<br/><br/>"
                + "Likes: The number of likes this tweet has on Twitter (at the time the tweet was retrieved).<br/><br/>"
                + "RT#: The number of times this tweet has been retweeted using ArtRetweeter.<br/><br/>"
                + "Pending RT?: Whether this tweet is currently queued to be retweeted using ArtRetweeter.</html>";
        JOptionPane.showMessageDialog(GUI.getInstance(), message, "Table Columns Explanation", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_showDescriptionOfTableColumnsButtonActionPerformed

    private void setTableSortingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setTableSortingButtonActionPerformed
        setTableSortingButton.setEnabled(false);
        setTableSorting();
        setTableSortingButton.setEnabled(true);
    }//GEN-LAST:event_setTableSortingButtonActionPerformed

    private void setTableFilteringButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setTableFilteringButtonActionPerformed
        setTableFilteringButton.setEnabled(false);
        setTableFiltering();
        setTableFilteringButton.setEnabled(true);
    }//GEN-LAST:event_setTableFilteringButtonActionPerformed

    private void setTableFiltering() {
        setTableFilteringPanel.setComboBoxSettings();
        int result = JOptionPane.showConfirmDialog(GUI.getInstance(), setTableFilteringPanel, "Set Table Filtering", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            ArrayList<TableFilterEntry> userSettings = setTableFilteringPanel.getUserSettings();
            setTableFilters(userSettings, true);
            refreshTweetsTable(false);
        }
    }

    private void setTableFilters(ArrayList<TableFilterEntry> userSettings, boolean updateDB) {
        currentTableFilters.clear();
        currentTableFilters.addAll(userSettings);
        if (updateDB) {
            String updateString = "";
            for (TableFilterEntry entry : userSettings) {
                updateString = updateString.concat(entry.getDatabaseFieldName()).concat(";").concat(String.valueOf(entry.getFilterEnabled()))
                        .concat(";").concat(entry.getOperator()).concat(";")
                        .concat(entry.getFieldValue().equals("") ? "null" : entry.getFieldValue()).concat(";");
            }
            CachedVariableDB.updateConfigItem("artretweeter.managementtweettablefiltering", updateString);
        }
        refreshTweetsTable(false);
    }

    private void setTableSorting() {
        setTableSortingPanel.setComboBoxSettings();
        int result = JOptionPane.showConfirmDialog(GUI.getInstance(), setTableSortingPanel, "Set Table Sorting", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            Pair<String[], String[]> userSettings = setTableSortingPanel.getUserSettings();
            setTableSort(userSettings, true);
        }
    }

    private void setTableSort(Pair<String[], String[]> userSettings, boolean updateDB) {
        int idColumnIndex = tweetsTable.getColumnModel().getColumnIndex("ID");
        int tweetTextColumnIndex = tweetsTable.getColumnModel().getColumnIndex("Tweet Text");
        int datePostedColumnIndex = tweetsTable.getColumnModel().getColumnIndex("Date Posted");
        int retweetsColumnIndex = tweetsTable.getColumnModel().getColumnIndex("Retweets");
        int likesColumnIndex = tweetsTable.getColumnModel().getColumnIndex("Likes");
        int rtCountColumnIndex = tweetsTable.getColumnModel().getColumnIndex("RT#");
        int pendingRTColumnIndex = tweetsTable.getColumnModel().getColumnIndex("Pending RT");
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        String newSortOrderInDB = "";
        String[] sortColumns = userSettings.getLeft();
        String[] sortOrders = userSettings.getRight();
        for (int i = 0; i < sortColumns.length; i++) {
            String sortColumn = sortColumns[i];
            String sortOrderString = sortOrders[i];
            SortOrder sortOrder;
            if (sortOrderString.equals("Ascending")) {
                sortOrder = SortOrder.ASCENDING;
            } else {
                sortOrder = SortOrder.DESCENDING;
            }
            if (sortColumn.equals("ID")) {
                sortKeys.add(new RowSorter.SortKey(idColumnIndex, sortOrder));

            } else if (sortColumn.equals("Tweet Text")) {
                sortKeys.add(new RowSorter.SortKey(tweetTextColumnIndex, sortOrder));
            } else if (sortColumn.equals("Date Posted")) {
                sortKeys.add(new RowSorter.SortKey(datePostedColumnIndex, sortOrder));
            } else if (sortColumn.equals("Retweets")) {
                sortKeys.add(new RowSorter.SortKey(retweetsColumnIndex, sortOrder));
            } else if (sortColumn.equals("Likes")) {
                sortKeys.add(new RowSorter.SortKey(likesColumnIndex, sortOrder));
            } else if (sortColumn.equals("RT#")) {
                sortKeys.add(new RowSorter.SortKey(rtCountColumnIndex, sortOrder));
            } else if (sortColumn.equals("Pending RT")) {
                sortKeys.add(new RowSorter.SortKey(pendingRTColumnIndex, sortOrder));
            } else if (sortColumn.equals("None")) {

            }
            newSortOrderInDB = newSortOrderInDB.concat(sortColumn).concat(";").concat(sortOrderString).concat(";");
        }
        tweetsTable.getRowSorter().setSortKeys(sortKeys);
        if (updateDB) {
            CachedVariableDB.updateConfigItem("artretweeter.managementtweettablesorting", newSortOrderInDB);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addTweetManuallyButton;
    private javax.swing.JLabel addTweetManuallyStatusLabel;
    private javax.swing.JTextField addTweetManuallyTextField;
    private javax.swing.JButton addTweetsToCurrentlySelectedCollectionButton;
    private javax.swing.JButton deleteTweetsFromArtRetweeterButton;
    private javax.swing.JButton deleteTweetsFromTwitterButton;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane26;
    private javax.swing.JButton queueRetweetButton;
    private javax.swing.JComboBox<String> selectAccountComboBox;
    private javax.swing.JButton setTableFilteringButton;
    private javax.swing.JButton setTableSortingButton;
    private javax.swing.JButton showDescriptionOfTableColumnsButton;
    protected javax.swing.JTable tweetsTable;
    // End of variables declaration//GEN-END:variables
}
