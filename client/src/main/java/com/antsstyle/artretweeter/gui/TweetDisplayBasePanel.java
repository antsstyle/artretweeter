/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 *
 * @author antss
 */
public abstract class TweetDisplayBasePanel extends JPanel {

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

}
