/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.serverapi.ServerAPI;
import com.antsstyle.artretweeter.tools.FormatTools;
import java.awt.Dimension;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class QueuingPanel extends TweetDisplayBasePanel {

    private static final Logger LOGGER = LogManager.getLogger(QueuingPanel.class);

    /**
     * Creates new form QueuingPanel
     */
    public QueuingPanel() {
        initComponents();
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(3, SortOrder.DESCENDING));
        tweetsTable.getRowSorter().setSortKeys(sortKeys);
        mainTweetsTable = tweetsTable;
        mainSelectAccountComboBox = selectAccountComboBox;
        tweetsTableQuery = "SELECT tweets.*,(SELECT COUNT(id) FROM retweetrecords WHERE "
                + "tweets.tweetid=retweetrecords.tweetid AND tweets.usertwitterid=retweetrecords.usertwitterid) AS numretweets"
                + " FROM tweets WHERE tweets.usertwitterid=?";
    }

    public void initialise() {
        imagePanes[0] = tweetImageScrollPane1;
        imagePanes[1] = tweetImageScrollPane2;
        imagePanes[2] = tweetImageScrollPane3;
        imagePanes[3] = tweetImageScrollPane4;
        imageLabels[0] = tweetImageLabel1;
        imageLabels[1] = tweetImageLabel2;
        imageLabels[2] = tweetImageLabel3;
        imageLabels[3] = tweetImageLabel4;
        tweetsTable.getSelectionModel().addListSelectionListener((ListSelectionEvent event) -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int row = tweetsTable.getSelectedRow();
            if (row == -1) {
                return;
            }
            GUIHelperMethods.showTweetPreview(tweetsTable, getPanelAttributes(), imagePanes, imageLabels);
        });
        queuedTweetsTable.getSelectionModel().addListSelectionListener((ListSelectionEvent event) -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int row = queuedTweetsTable.getSelectedRow();
            if (row == -1) {
                return;
            }
            GUIHelperMethods.showTweetPreview(queuedTweetsTable, getPanelAttributes(), imagePanes, imageLabels);
        });
        refreshAccountBoxModel(true);
        refreshQueueTable();
    }

    public Account getCurrentlySelectedAccount() {
        return currentlySelectedAccount;
    }

    public void refreshQueueTable() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }

        String query = "SELECT retweetqueue.retweettime,"
                + "(SELECT id FROM tweets WHERE tweets.tweetid=retweetqueue.tweetid) AS tweetdatabaseid,"
                + "(SELECT fulltweettext FROM tweets WHERE tweets.tweetid=retweetqueue.tweetid) AS text "
                + "FROM retweetqueue WHERE retweetingusertwitterid=? ORDER BY retweettime ASC";

        DBResponse resp = CoreDB.customQuerySelect(query, currentlySelectedAccount.getTwitterID());
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to query database for retweet queue information!");
            return;
        }
        DefaultTableModel dtm = (DefaultTableModel) queuedTweetsTable.getModel();
        dtm.setRowCount(0);
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            Integer id = (Integer) row.get("TWEETDATABASEID");
            String tweetText = (String) row.get("TEXT");
            Timestamp retweetTime = (Timestamp) row.get("RETWEETTIME");
            String retweetTimeString = FormatTools.DATETIME_FORMAT.format(new Date(retweetTime.getTime()));
            dtm.addRow(new Object[]{id, tweetText, retweetTimeString});
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tweetImageScrollPane1 = new javax.swing.JScrollPane();
        tweetImageLabel1 = new javax.swing.JLabel();
        tweetImageScrollPane2 = new javax.swing.JScrollPane();
        tweetImageLabel2 = new javax.swing.JLabel();
        tweetImageScrollPane3 = new javax.swing.JScrollPane();
        tweetImageLabel3 = new javax.swing.JLabel();
        tweetImageScrollPane4 = new javax.swing.JScrollPane();
        tweetImageLabel4 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane27 = new javax.swing.JScrollPane();
        queuedTweetsTable = new javax.swing.JTable();
        changeRetweetTimeButton = new javax.swing.JButton();
        unqueueRetweetButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane26 = new javax.swing.JScrollPane();
        tweetsTable = new javax.swing.JTable();
        selectAccountComboBox = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        queueRetweetButton = new javax.swing.JButton();

        tweetImageScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tweetImageScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        tweetImageScrollPane1.setPreferredSize(new java.awt.Dimension(420, 641));

        tweetImageLabel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 3, true));
        tweetImageLabel1.setOpaque(true);
        tweetImageScrollPane1.setViewportView(tweetImageLabel1);

        tweetImageScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tweetImageScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        tweetImageScrollPane2.setPreferredSize(new java.awt.Dimension(420, 641));

        tweetImageLabel2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 3, true));
        tweetImageLabel2.setOpaque(true);
        tweetImageScrollPane2.setViewportView(tweetImageLabel2);

        tweetImageScrollPane3.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tweetImageScrollPane3.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        tweetImageScrollPane3.setPreferredSize(new java.awt.Dimension(420, 641));

        tweetImageLabel3.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 3, true));
        tweetImageLabel3.setOpaque(true);
        tweetImageScrollPane3.setViewportView(tweetImageLabel3);

        tweetImageScrollPane4.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tweetImageScrollPane4.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        tweetImageScrollPane4.setPreferredSize(new java.awt.Dimension(420, 641));

        tweetImageLabel4.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 3, true));
        tweetImageLabel4.setOpaque(true);
        tweetImageScrollPane4.setViewportView(tweetImageLabel4);

        jPanel1.setMaximumSize(new java.awt.Dimension(606, 288));
        jPanel1.setMinimumSize(new java.awt.Dimension(606, 288));
        jPanel1.setPreferredSize(new java.awt.Dimension(606, 288));

        jScrollPane27.setMaximumSize(new java.awt.Dimension(898, 184));
        jScrollPane27.setMinimumSize(new java.awt.Dimension(898, 184));
        jScrollPane27.setPreferredSize(new java.awt.Dimension(898, 184));

        queuedTweetsTable.setAutoCreateRowSorter(true);
        queuedTweetsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Tweet Text", "Retweet Time"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Long.class, java.lang.String.class, java.lang.String.class
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
        queuedTweetsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane27.setViewportView(queuedTweetsTable);
        if (queuedTweetsTable.getColumnModel().getColumnCount() > 0) {
            queuedTweetsTable.getColumnModel().getColumn(0).setMinWidth(50);
            queuedTweetsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
            queuedTweetsTable.getColumnModel().getColumn(0).setMaxWidth(50);
            queuedTweetsTable.getColumnModel().getColumn(2).setMinWidth(150);
            queuedTweetsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
            queuedTweetsTable.getColumnModel().getColumn(2).setMaxWidth(150);
        }

        changeRetweetTimeButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        changeRetweetTimeButton.setText("Change retweet time");
        changeRetweetTimeButton.setMaximumSize(new java.awt.Dimension(176, 33));
        changeRetweetTimeButton.setMinimumSize(new java.awt.Dimension(176, 33));
        changeRetweetTimeButton.setPreferredSize(new java.awt.Dimension(176, 33));
        changeRetweetTimeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeRetweetTimeButtonActionPerformed(evt);
            }
        });

        unqueueRetweetButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        unqueueRetweetButton.setText("Unqueue retweet");
        unqueueRetweetButton.setMaximumSize(new java.awt.Dimension(151, 33));
        unqueueRetweetButton.setMinimumSize(new java.awt.Dimension(151, 33));
        unqueueRetweetButton.setPreferredSize(new java.awt.Dimension(151, 33));
        unqueueRetweetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unqueueRetweetButtonActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Queued retweets");
        jLabel3.setMaximumSize(new java.awt.Dimension(156, 26));
        jLabel3.setMinimumSize(new java.awt.Dimension(156, 26));
        jLabel3.setPreferredSize(new java.awt.Dimension(156, 26));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(unqueueRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(changeRetweetTimeButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(273, Short.MAX_VALUE))
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane27, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane27, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(unqueueRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(changeRetweetTimeButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jPanel2.setMaximumSize(new java.awt.Dimension(601, 288));
        jPanel2.setMinimumSize(new java.awt.Dimension(601, 288));

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
                "ID", "Tweet Text", "Date Posted", "Retweets", "Likes", "RT#"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Long.class, java.lang.String.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Long.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tweetsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tweetsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane26.setViewportView(tweetsTable);
        if (tweetsTable.getColumnModel().getColumnCount() > 0) {
            tweetsTable.getColumnModel().getColumn(0).setMinWidth(50);
            tweetsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
            tweetsTable.getColumnModel().getColumn(0).setMaxWidth(50);
            tweetsTable.getColumnModel().getColumn(2).setMinWidth(120);
            tweetsTable.getColumnModel().getColumn(2).setPreferredWidth(120);
            tweetsTable.getColumnModel().getColumn(2).setMaxWidth(120);
            tweetsTable.getColumnModel().getColumn(3).setMinWidth(70);
            tweetsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
            tweetsTable.getColumnModel().getColumn(3).setMaxWidth(70);
            tweetsTable.getColumnModel().getColumn(4).setMinWidth(70);
            tweetsTable.getColumnModel().getColumn(4).setPreferredWidth(70);
            tweetsTable.getColumnModel().getColumn(4).setMaxWidth(70);
            tweetsTable.getColumnModel().getColumn(5).setMinWidth(40);
            tweetsTable.getColumnModel().getColumn(5).setPreferredWidth(40);
            tweetsTable.getColumnModel().getColumn(5).setMaxWidth(40);
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

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setText("Retweet Queuing");

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

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(12, 12, 12)
                        .addComponent(jLabel2)
                        .addGap(6, 6, 6)
                        .addComponent(selectAccountComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(queueRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane26, javax.swing.GroupLayout.PREFERRED_SIZE, 696, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(62, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selectAccountComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addComponent(jScrollPane26, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(queueRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tweetImageScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(tweetImageScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(tweetImageScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(tweetImageScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(7, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tweetImageScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tweetImageScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tweetImageScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tweetImageScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void selectAccountComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAccountComboBoxActionPerformed
        if (selectAccountComboBox.isEnabled()) {
            Account acc = (Account) selectAccountComboBox.getSelectedItem();
            if (!acc.equals(currentlySelectedAccount)) {
                currentlySelectedAccount = acc;
                refreshTweetsTable();
                refreshQueueTable();
            }
        }
    }//GEN-LAST:event_selectAccountComboBoxActionPerformed

    private void unqueueRetweetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unqueueRetweetButtonActionPerformed
        unqueueRetweetButton.setEnabled(false);
        unqueueRetweet();
        unqueueRetweetButton.setEnabled(true);
    }//GEN-LAST:event_unqueueRetweetButtonActionPerformed

    private void changeRetweetTimeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeRetweetTimeButtonActionPerformed
        changeRetweetTimeButton.setEnabled(false);
        changeRetweetTime();
        changeRetweetTimeButton.setEnabled(true);
    }//GEN-LAST:event_changeRetweetTimeButtonActionPerformed

    private void queueRetweetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_queueRetweetButtonActionPerformed
        queueRetweetButton.setEnabled(false);
        ServerAPI.queueRetweet(tweetsTable, currentlySelectedAccount, false);
        queueRetweetButton.setEnabled(true);
    }//GEN-LAST:event_queueRetweetButtonActionPerformed

    private void changeRetweetTime() {
        ServerAPI.queueRetweet(queuedTweetsTable, currentlySelectedAccount, true);
    }

    private void unqueueRetweet() {
        int row = queuedTweetsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        int modelRow = queuedTweetsTable.convertRowIndexToModel(row);
        int idColumnIndex = queuedTweetsTable.getColumnModel().getColumnIndex("ID");
        Integer id = (Integer) queuedTweetsTable.getModel().getValueAt(modelRow, idColumnIndex);
        DBResponse selectResp = CoreDB.selectFromTable(DBTable.TWEETS,
                new String[]{"id"},
                new Object[]{id});
        if (!selectResp.wasSuccessful()) {
            String msg = "Failed to retrieve tweet information from DB!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectResp.getReturnedRows().isEmpty()) {
            String msg = "Tweet could not be found in DB - has the database folder been modified?";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        TweetHolder tweet = ResultSetConversion.getTweet(selectResp.getReturnedRows().get(0));
        OperationResult result = ServerAPI.unqueueRetweet(currentlySelectedAccount, tweet.getTweetID());
        if (!result.wasSuccessful()) {
            GUIHelperMethods.showErrors(result, LOGGER, null);
            return;
        }
        DBResponse deleteResp = CoreDB.deleteFromTable(DBTable.RETWEETQUEUE,
                new String[]{"tweetid"},
                new Object[]{tweet.getTweetID()});
        if (deleteResp.wasSuccessful()) {
            DefaultTableModel dtm = (DefaultTableModel) queuedTweetsTable.getModel();
            dtm.removeRow(modelRow);
        } else {
            String msg = "Failed to delete entry from queue - check log output.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refreshTweetsTable() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }
        String query = "SELECT tweets.*,(SELECT COUNT(id) FROM retweetrecords WHERE "
                + "tweets.tweetid=retweetrecords.tweetid AND tweets.usertwitterid=retweetrecords.usertwitterid) AS numretweets"
                + " FROM tweets WHERE tweets.usertwitterid=?";
        DBResponse resp = CoreDB.customQuerySelect(query, currentlySelectedAccount.getTwitterID());
        if (!resp.wasSuccessful()) {
            String msg = "Failed to retrieve tweets for this user from DB!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        DefaultTableModel dtm = (DefaultTableModel) tweetsTable.getModel();
        dtm.setRowCount(0);
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            TweetHolder tweet = ResultSetConversion.getTweet(row);
            Long retweetCount = (Long) row.get("NUMRETWEETS");
            String dateString = FormatTools.DATETIME_FORMAT.format(new Date(tweet.getCreatedAt().getTime()));
            dtm.addRow(new Object[]{tweet.getId(), tweet.getFullTweetText(), dateString, tweet.getRetweetCount(), tweet.getLikeCount(),
                retweetCount});
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton changeRetweetTimeButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane26;
    private javax.swing.JScrollPane jScrollPane27;
    private javax.swing.JButton queueRetweetButton;
    protected static javax.swing.JTable queuedTweetsTable;
    private javax.swing.JComboBox<String> selectAccountComboBox;
    protected javax.swing.JLabel tweetImageLabel1;
    protected javax.swing.JLabel tweetImageLabel2;
    protected javax.swing.JLabel tweetImageLabel3;
    protected javax.swing.JLabel tweetImageLabel4;
    protected javax.swing.JScrollPane tweetImageScrollPane1;
    protected javax.swing.JScrollPane tweetImageScrollPane2;
    protected javax.swing.JScrollPane tweetImageScrollPane3;
    protected javax.swing.JScrollPane tweetImageScrollPane4;
    protected javax.swing.JTable tweetsTable;
    private javax.swing.JButton unqueueRetweetButton;
    // End of variables declaration//GEN-END:variables

}
