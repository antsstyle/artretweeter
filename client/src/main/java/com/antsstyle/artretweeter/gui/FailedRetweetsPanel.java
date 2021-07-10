/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class FailedRetweetsPanel extends TweetDisplayBasePanel {

    private static final Logger LOGGER = LogManager.getLogger(FailedRetweetsPanel.class);

    private final DefaultComboBoxModel selectAccountBoxModel = new DefaultComboBoxModel();
    private Account currentlySelectedAccount = null;

    private boolean noAccountsInBoxModel() {
        if (selectAccountBoxModel.getSize() == 0) {
            return true;
        } else if (selectAccountBoxModel.getSize() == 1) {
            Account account = (Account) selectAccountBoxModel.getSelectedItem();
            return (account.equals(NO_ACCOUNTS) || account.equals(DB_ERROR_ACCOUNT));
        }
        return false;
    }

    /**
     * Creates new form FailedTweetsPanel
     */
    public FailedRetweetsPanel() {
        initComponents();
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
        refreshAccountBoxModel(true);
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
                refreshTweetsTable();
            }
        } else {
            selectAccountBoxModel.addElement(NO_ACCOUNTS);
            currentlySelectedAccount = NO_ACCOUNTS;
            selectAccountBoxModel.setSelectedItem(selectAccountBoxModel.getElementAt(0));
        }
        selectAccountComboBox.setEnabled(true);
    }

    public void refreshTweetsTable() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }
        DBResponse resp = CoreDB.selectFromTable(DBTable.FAILEDRETWEETS,
                new String[]{"retweetingusertwitterid"},
                new Object[]{currentlySelectedAccount.getTwitterID()});
        if (!resp.wasSuccessful()) {
            String msg = "Failed to retrieve tweets for this user from DB!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DefaultTableModel dtm = (DefaultTableModel) tweetsTable.getModel();
        dtm.setRowCount(0);
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            TweetHolder tweet = ResultSetConversion.getTweet(row);
            String dateString = DATETIME_FORMAT.format(new Date(tweet.getCreatedAt().getTime()));
            dtm.addRow(new Object[]{tweet.getId(), tweet.getFullTweetText(), dateString, tweet.getRetweetCount(), tweet.getLikeCount()});
        }
    }

    public Integer[] getPanelAttributes() {
        return new Integer[]{STANDARD_PANEL_WIDTH, STANDARD_PANEL_HEIGHT, STANDARD_PANEL_MARGIN, STANDARD_PANEL_INSET};
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane26 = new javax.swing.JScrollPane();
        tweetsTable = new javax.swing.JTable();
        selectAccountComboBox = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        tweetImageScrollPane1 = new javax.swing.JScrollPane();
        tweetImageLabel1 = new javax.swing.JLabel();
        tweetImageScrollPane2 = new javax.swing.JScrollPane();
        tweetImageLabel2 = new javax.swing.JLabel();
        tweetImageScrollPane3 = new javax.swing.JScrollPane();
        tweetImageLabel3 = new javax.swing.JLabel();
        tweetImageScrollPane4 = new javax.swing.JScrollPane();
        tweetImageLabel4 = new javax.swing.JLabel();
        requeueFailedRetweetButton = new javax.swing.JButton();
        clearFailedRetweetButton = new javax.swing.JButton();
        clearAllFailedRetweetsButton = new javax.swing.JButton();

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setText("Tweets");

        jScrollPane26.setMaximumSize(new java.awt.Dimension(898, 184));
        jScrollPane26.setMinimumSize(new java.awt.Dimension(898, 184));
        jScrollPane26.setPreferredSize(new java.awt.Dimension(898, 184));

        tweetsTable.setAutoCreateRowSorter(true);
        tweetsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Tweet Text", "Date Posted", "Error Code", "Fail Reason"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Long.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
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
            tweetsTable.getColumnModel().getColumn(1).setMinWidth(300);
            tweetsTable.getColumnModel().getColumn(1).setPreferredWidth(300);
            tweetsTable.getColumnModel().getColumn(1).setMaxWidth(300);
            tweetsTable.getColumnModel().getColumn(2).setMinWidth(120);
            tweetsTable.getColumnModel().getColumn(2).setPreferredWidth(120);
            tweetsTable.getColumnModel().getColumn(2).setMaxWidth(120);
            tweetsTable.getColumnModel().getColumn(3).setMinWidth(70);
            tweetsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
            tweetsTable.getColumnModel().getColumn(3).setMaxWidth(70);
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

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel2.setText("Select account: ");

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

        requeueFailedRetweetButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        requeueFailedRetweetButton.setText("Requeue retweet");
        requeueFailedRetweetButton.setMaximumSize(new java.awt.Dimension(150, 33));
        requeueFailedRetweetButton.setMinimumSize(new java.awt.Dimension(150, 33));
        requeueFailedRetweetButton.setPreferredSize(new java.awt.Dimension(150, 33));
        requeueFailedRetweetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                requeueFailedRetweetButtonActionPerformed(evt);
            }
        });

        clearFailedRetweetButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        clearFailedRetweetButton.setText("Clear retweet from table");
        clearFailedRetweetButton.setMaximumSize(new java.awt.Dimension(199, 33));
        clearFailedRetweetButton.setMinimumSize(new java.awt.Dimension(199, 33));
        clearFailedRetweetButton.setPreferredSize(new java.awt.Dimension(199, 33));
        clearFailedRetweetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearFailedRetweetButtonActionPerformed(evt);
            }
        });

        clearAllFailedRetweetsButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        clearAllFailedRetweetsButton.setText("Clear all");
        clearAllFailedRetweetsButton.setMaximumSize(new java.awt.Dimension(84, 33));
        clearAllFailedRetweetsButton.setMinimumSize(new java.awt.Dimension(84, 33));
        clearAllFailedRetweetsButton.setPreferredSize(new java.awt.Dimension(84, 33));
        clearAllFailedRetweetsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearAllFailedRetweetsButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(selectAccountComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tweetImageScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(tweetImageScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(tweetImageScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jScrollPane26, javax.swing.GroupLayout.PREFERRED_SIZE, 1057, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(4, 4, 4)
                        .addComponent(tweetImageScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(requeueFailedRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearFailedRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clearAllFailedRetweetsButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(selectAccountComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane26, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(requeueFailedRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearFailedRetweetButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(clearAllFailedRetweetsButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
            }
        }
    }//GEN-LAST:event_selectAccountComboBoxActionPerformed

    private void clearFailedRetweet() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }
        int row = tweetsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        int modelRow = tweetsTable.convertRowIndexToModel(row);
        int idColumnIndex = tweetsTable.getColumnModel().getColumnIndex("ID");
        Integer id = (Integer) tweetsTable.getModel().getValueAt(modelRow, idColumnIndex);
        CoreDB.deleteFromTable(DBTable.FAILEDRETWEETS,
                new String[]{"id"},
                new Object[]{id});
        DefaultTableModel dtm = (DefaultTableModel) tweetsTable.getModel();
        dtm.removeRow(modelRow);
    }

    private void clearAllFailedRetweets() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }
        CoreDB.deleteFromTable(DBTable.FAILEDRETWEETS,
                new String[]{"retweetingusertwitterid"},
                new Object[]{currentlySelectedAccount.getTwitterID()});
        DefaultTableModel dtm = (DefaultTableModel) tweetsTable.getModel();
        dtm.setRowCount(0);
    }

    private void requeueFailedRetweet() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }
        boolean queuedSuccessfully = GUIHelperMethods.queueRetweet(tweetsTable, currentlySelectedAccount, false);
        if (queuedSuccessfully) {
            clearFailedRetweet();
        }
    }

    private void clearFailedRetweetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearFailedRetweetButtonActionPerformed
        clearFailedRetweetButton.setEnabled(false);
        clearFailedRetweet();
        clearFailedRetweetButton.setEnabled(true);
    }//GEN-LAST:event_clearFailedRetweetButtonActionPerformed

    private void clearAllFailedRetweetsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearAllFailedRetweetsButtonActionPerformed
        clearAllFailedRetweetsButton.setEnabled(false);
        clearAllFailedRetweets();
        clearAllFailedRetweetsButton.setEnabled(true);
    }//GEN-LAST:event_clearAllFailedRetweetsButtonActionPerformed

    private void requeueFailedRetweetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_requeueFailedRetweetButtonActionPerformed
        requeueFailedRetweetButton.setEnabled(false);
        requeueFailedRetweet();
        requeueFailedRetweetButton.setEnabled(true);
    }//GEN-LAST:event_requeueFailedRetweetButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clearAllFailedRetweetsButton;
    private javax.swing.JButton clearFailedRetweetButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane26;
    private javax.swing.JButton requeueFailedRetweetButton;
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
    // End of variables declaration//GEN-END:variables
}
