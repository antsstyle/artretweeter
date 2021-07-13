/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

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
    
    protected JTable mainTweetsTable;
    protected JComboBox mainSelectAccountComboBox;

    protected int STANDARD_PANEL_WIDTH = 420;
    protected int STANDARD_PANEL_HEIGHT = 641;
    protected int STANDARD_PANEL_MARGIN = 2;
    protected int STANDARD_PANEL_INSET = 1;

    protected final JScrollPane[] imagePanes = new JScrollPane[4];
    protected final JLabel[] imageLabels = new JLabel[4];

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
            dtm.addRow(new Object[]{tweet.getId(), tweet.getFullTweetText(), dateString, tweet.getRetweetCount(), tweet.getLikeCount()});
        }
    }

}
