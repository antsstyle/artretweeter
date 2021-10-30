/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.configuration.MiscConfig;
import com.antsstyle.artretweeter.configuration.TwitterConfig;
import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public abstract class TweetDisplayBasePanel extends JPanel {

    private static final Logger LOGGER = LogManager.getLogger(TweetDisplayBasePanel.class);

    protected Account currentlySelectedAccount;
    protected DefaultComboBoxModel selectAccountBoxModel = new DefaultComboBoxModel();
    protected String tweetsTableQuery = "SELECT * FROM tweets WHERE usertwitterid=?";

    private JTable mainTweetsTable;
    protected JComboBox mainSelectAccountComboBox;

    protected int STANDARD_PANEL_WIDTH = 420;
    protected int STANDARD_PANEL_HEIGHT = 641;
    protected int STANDARD_PANEL_MARGIN = 2;
    protected int STANDARD_PANEL_INSET = 1;

    protected static final DefaultTableModel METRICS_ENABLED_TABLE_MODEL = new DefaultTableModel(
            new Object[][]{},
            new String[]{
                "ID", "Tweet Text", "Date Posted", "Retweets", "Likes", "RT#", "Pending RT"
            }
    ) {
        Class[] types = new Class[]{
            java.lang.Long.class, java.lang.String.class, java.lang.Object.class, java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Long.class, java.lang.Boolean.class
        };
        boolean[] canEdit = new boolean[]{
            false, false, false, false, false, false, false
        };

        @Override
        public Class getColumnClass(int columnIndex) {
            return types[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit[columnIndex];
        }
    };

    protected static final DefaultTableModel METRICS_DISABLED_TABLE_MODEL = new DefaultTableModel(
            new Object[][]{},
            new String[]{
                "ID", "Tweet Text", "Date Posted", "RT#", "Pending RT"
            }
    ) {
        Class[] types = new Class[]{
            java.lang.Long.class, java.lang.String.class, java.lang.Object.class, java.lang.Long.class, java.lang.Boolean.class
        };
        boolean[] canEdit = new boolean[]{
            false, false, false, false, false
        };

        @Override
        public Class getColumnClass(int columnIndex) {
            return types[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit[columnIndex];
        }
    };

    protected final JScrollPane[] imagePanes = new JScrollPane[4];
    protected final JLabel[] imageLabels = new JLabel[4];

    public static final Account ALL_TWEETS_ACCOUNT = new Account()
            .setScreenName("<show all tweets>");

    public static final Account NO_ACCOUNTS = new Account()
            .setScreenName("<no accounts added>");

    public static final Account DB_ERROR_ACCOUNT = new Account()
            .setScreenName("<database error>");

    public static final TwitterCollectionHolder SELECT_ACCOUNT_FIRST = new TwitterCollectionHolder()
            .setName("<select an account first>");

    public static final TwitterCollectionHolder NO_COLLECTIONS = new TwitterCollectionHolder()
            .setName("<none>");

    public static final TwitterCollectionHolder DB_ERROR_COLLECTION = new TwitterCollectionHolder()
            .setName("<database error>");
    
    protected void assignVariables(JTable mainTweetsTable, JComboBox mainSelectAccountComboBox,
            DefaultComboBoxModel selectAccountBoxModel, JScrollPane[] imagePanes, JLabel[] imageLabels) {
        this.mainTweetsTable = mainTweetsTable;
        this.mainSelectAccountComboBox = mainSelectAccountComboBox;
        this.selectAccountBoxModel = selectAccountBoxModel;
        System.arraycopy(imagePanes, 0, this.imagePanes, 0, this.imagePanes.length);
        System.arraycopy(imageLabels, 0, this.imageLabels, 0, this.imageLabels.length);
    }
    
    protected void setTweetsTableMetricsEnabled() {
        mainTweetsTable.setModel(METRICS_ENABLED_TABLE_MODEL);
        //jScrollPane26.setViewportView(tweetsTable);
        setCommonColumnWidths();
        TableColumnModel tcm = mainTweetsTable.getColumnModel();
        int retweetsColumnIndex = tcm.getColumnIndex("Retweets");
        int likesColumnIndex = tcm.getColumnIndex("Likes");
        GUIHelperMethods.setAllColumnWidthSizes(tcm, retweetsColumnIndex, 70);
        GUIHelperMethods.setAllColumnWidthSizes(tcm, likesColumnIndex, 70);
    }

    protected void setTweetsTableMetricsDisabled() {
        mainTweetsTable.setModel(METRICS_DISABLED_TABLE_MODEL);
        setCommonColumnWidths();
    }

    private void setCommonColumnWidths() {
        TableColumnModel tcm = mainTweetsTable.getColumnModel();
        int idColumnIndex = tcm.getColumnIndex("ID");
        int datePostedColumnIndex = tcm.getColumnIndex("Date Posted");
        int rtNumColumnIndex = tcm.getColumnIndex("RT#");
        int pendingRTColumnIndex = tcm.getColumnIndex("Pending RT");
        GUIHelperMethods.setAllColumnWidthSizes(tcm, idColumnIndex, 40);
        GUIHelperMethods.setAllColumnWidthSizes(tcm, datePostedColumnIndex, 125);
        GUIHelperMethods.setAllColumnWidthSizes(tcm, rtNumColumnIndex, 40);
        GUIHelperMethods.setAllColumnWidthSizes(tcm, pendingRTColumnIndex, 90);
    }

    public Integer[] getPanelAttributes() {
        return new Integer[]{STANDARD_PANEL_WIDTH, STANDARD_PANEL_HEIGHT, STANDARD_PANEL_MARGIN, STANDARD_PANEL_INSET};
    }

    public void refreshAccountBoxModel(boolean initialRefresh) {
        mainSelectAccountComboBox.setEnabled(false);
        boolean noElementsBefore = noAccountsInBoxModel();
        selectAccountBoxModel.removeAllElements();
        DBResponse resp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to get collections data to refresh combo box model!");
            currentlySelectedAccount = DB_ERROR_ACCOUNT;
            selectAccountBoxModel.setSelectedItem(selectAccountBoxModel.getElementAt(0));
            mainSelectAccountComboBox.setEnabled(true);
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
        mainSelectAccountComboBox.setEnabled(true);
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

    public void refreshTweetsTable() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }
        DBResponse resp = CoreDB.customQuerySelect(tweetsTableQuery, currentlySelectedAccount.getTwitterID());
        if (!resp.wasSuccessful()) {
            String msg = "Failed to retrieve tweets for this user from DB!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DefaultTableModel dtm = (DefaultTableModel) mainTweetsTable.getModel();
        dtm.setRowCount(0);
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            TweetHolder tweet = ResultSetConversion.getTweet(row);
            String dateString = DATETIME_FORMAT.format(new Date(tweet.getCreatedAt().getTime()));
            if (TwitterConfig.DO_NOT_SHOW_METRICS_ANYWHERE) {
                dtm.addRow(new Object[]{tweet.getId(), tweet.getFullTweetText(), dateString});
            } else {
                dtm.addRow(new Object[]{tweet.getId(), tweet.getFullTweetText(), dateString, tweet.getRetweetCount(), tweet.getLikeCount()});
            }
        }
    }

}
