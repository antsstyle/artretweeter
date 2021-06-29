/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.CollectionCurateParamsJSON;
import com.antsstyle.artretweeter.datastructures.CollectionCurateRespJSON;
import com.antsstyle.artretweeter.datastructures.CollectionCurateRespJSON.CollectionCurateResponse.CollectionCurateEntry;
import com.antsstyle.artretweeter.datastructures.CollectionOperation;
import com.antsstyle.artretweeter.datastructures.CollectionOrdering;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.StatusJSON;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.tools.RegularExpressions;
import com.antsstyle.artretweeter.tools.SwingTools;
import com.antsstyle.artretweeter.twitter.RESTAPI;
import java.awt.Desktop;
import java.awt.Font;
import java.net.URI;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class CollectionsPanel extends TweetDisplayBasePanel {

    private static final Logger LOGGER = LogManager.getLogger(CollectionsPanel.class);

    private final DefaultComboBoxModel selectAccountBoxModel = new DefaultComboBoxModel();
    private final DefaultComboBoxModel selectCollectionBoxModel = new DefaultComboBoxModel();

    private Account currentlySelectedAccount = null;
    private TwitterCollectionHolder currentlySelectedCollection = null;

    /**
     * Creates new form TweetsPanel
     */
    public CollectionsPanel() {
        initComponents();
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(3, SortOrder.DESCENDING));
        tweetsTable.getRowSorter().setSortKeys(sortKeys);
    }

    private boolean noAccountsInBoxModel() {
        if (selectAccountBoxModel.getSize() == 0) {
            return true;
        } else if (selectAccountBoxModel.getSize() == 1) {
            Account account = (Account) selectAccountBoxModel.getSelectedItem();
            return (account.equals(NO_ACCOUNTS) || account.equals(DB_ERROR_ACCOUNT));
        }
        return false;
    }

    private boolean noCollectionsInBoxModel() {
        if (selectCollectionBoxModel.getSize() == 0) {
            return true;
        } else if (selectCollectionBoxModel.getSize() == 1) {
            TwitterCollectionHolder holder = (TwitterCollectionHolder) selectCollectionBoxModel.getSelectedItem();
            return (holder.equals(NO_COLLECTIONS) || holder.equals(DB_ERROR_COLLECTION) || holder.equals(SELECT_ACCOUNT_FIRST));
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
                refreshTweetsTable();
                refreshCollectionBoxModel(false);
            }
        } else {
            selectAccountBoxModel.addElement(NO_ACCOUNTS);
            currentlySelectedAccount = NO_ACCOUNTS;
            selectAccountBoxModel.setSelectedItem(selectAccountBoxModel.getElementAt(0));
        }
        selectAccountComboBox.setEnabled(true);
    }

    public void refreshCollectionBoxModel(boolean initialRefresh) {
        selectCollectionComboBox.setEnabled(false);
        boolean noElementsBefore = noCollectionsInBoxModel();
        selectCollectionBoxModel.removeAllElements();
        if (currentlySelectedAccount.equals(NO_ACCOUNTS)) {
            selectCollectionBoxModel.addElement(SELECT_ACCOUNT_FIRST);
            currentlySelectedCollection = SELECT_ACCOUNT_FIRST;
            selectCollectionBoxModel.setSelectedItem(selectCollectionBoxModel.getElementAt(0));
            selectCollectionComboBox.setEnabled(true);
            return;
        }

        DBResponse resp = CoreDB.selectFromTable(DBTable.COLLECTIONS,
                new String[]{"usertwitterid"},
                new Object[]{currentlySelectedAccount.getTwitterID()});
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to get accounts data to refresh combo box model!");
            selectCollectionBoxModel.addElement(DB_ERROR_COLLECTION);
            currentlySelectedCollection = DB_ERROR_COLLECTION;
            selectCollectionBoxModel.setSelectedItem(selectCollectionBoxModel.getElementAt(0));
            return;
        }
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            TwitterCollectionHolder holder = ResultSetConversion.getTwitterCollection(row);
            selectCollectionBoxModel.addElement(holder);
        }
        if (selectCollectionBoxModel.getSize() != 0) {
            selectCollectionBoxModel.setSelectedItem(selectCollectionBoxModel.getElementAt(0));
            currentlySelectedCollection = (TwitterCollectionHolder) selectCollectionBoxModel.getElementAt(0);
            if (initialRefresh || noElementsBefore) {
                refreshCollectionTweetsTable();
            }
        } else {
            selectCollectionBoxModel.addElement(NO_COLLECTIONS);
            currentlySelectedCollection = NO_COLLECTIONS;
        }
        selectCollectionComboBox.setEnabled(true);
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
            int[] rows = tweetsTable.getSelectedRows();
            if (rows.length != 1) {
                return;
            }
            GUIHelperMethods.showTweetPreview(tweetsTable, getPanelAttributes(), imagePanes, imageLabels);
        });
        collectionTweetsTable.getSelectionModel().addListSelectionListener((ListSelectionEvent event) -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int[] rows = collectionTweetsTable.getSelectedRows();
            if (rows.length != 1) {
                return;
            }
            GUIHelperMethods.showTweetPreview(collectionTweetsTable, getPanelAttributes(), imagePanes, imageLabels);
        });
        refreshAccountBoxModel(true);
        refreshCollectionBoxModel(true);
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
        jScrollPane27 = new javax.swing.JScrollPane();
        collectionTweetsTable = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        addTweetManuallyTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        addTweetManuallyButton = new javax.swing.JButton();
        addTweetManuallyStatusLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        selectCollectionComboBox = new javax.swing.JComboBox<>();
        addTweetsToCurrentlySelectedCollectionButton = new javax.swing.JButton();
        deleteTweetsFromCollectionButton = new javax.swing.JButton();
        createNewCollectionButton = new javax.swing.JButton();
        deleteCollectionButton = new javax.swing.JButton();
        viewCollectionOnTwitterButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        moveTweetUpButton = new javax.swing.JButton();
        moveTweetDownButton = new javax.swing.JButton();
        setTweetOrderButton = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();

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
                "ID", "Tweet Text", "Date Posted", "Retweets", "Likes"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Long.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class
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
        tweetsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tweetsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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

        jScrollPane27.setMaximumSize(new java.awt.Dimension(898, 184));
        jScrollPane27.setMinimumSize(new java.awt.Dimension(898, 184));
        jScrollPane27.setPreferredSize(new java.awt.Dimension(898, 184));

        collectionTweetsTable.setAutoCreateRowSorter(true);
        collectionTweetsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Tweet Text", "Order"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Long.class, java.lang.String.class, java.lang.Integer.class
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
        collectionTweetsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        collectionTweetsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jScrollPane27.setViewportView(collectionTweetsTable);
        if (collectionTweetsTable.getColumnModel().getColumnCount() > 0) {
            collectionTweetsTable.getColumnModel().getColumn(0).setMinWidth(50);
            collectionTweetsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
            collectionTweetsTable.getColumnModel().getColumn(0).setMaxWidth(50);
            collectionTweetsTable.getColumnModel().getColumn(2).setMinWidth(40);
            collectionTweetsTable.getColumnModel().getColumn(2).setPreferredWidth(40);
            collectionTweetsTable.getColumnModel().getColumn(2).setMaxWidth(40);
        }

        addTweetManuallyTextField.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel3.setText("Add tweet to ArtRetweeter database manually:");

        addTweetManuallyButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        addTweetManuallyButton.setText("Add");
        addTweetManuallyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTweetManuallyButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(addTweetManuallyStatusLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(addTweetManuallyTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addTweetManuallyButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(addTweetManuallyTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(addTweetManuallyButton, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addTweetManuallyStatusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(102, Short.MAX_VALUE))
        );

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Collections");

        jLabel5.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel5.setText("Select collection: ");

        selectCollectionComboBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        selectCollectionComboBox.setModel(selectCollectionBoxModel);
        selectCollectionComboBox.setMaximumSize(new java.awt.Dimension(250, 26));
        selectCollectionComboBox.setMinimumSize(new java.awt.Dimension(250, 26));
        selectCollectionComboBox.setPreferredSize(new java.awt.Dimension(250, 26));
        selectCollectionComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectCollectionComboBoxActionPerformed(evt);
            }
        });

        addTweetsToCurrentlySelectedCollectionButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        addTweetsToCurrentlySelectedCollectionButton.setText("Add tweets to currently selected collection");
        addTweetsToCurrentlySelectedCollectionButton.setToolTipText("");
        addTweetsToCurrentlySelectedCollectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTweetsToCurrentlySelectedCollectionButtonActionPerformed(evt);
            }
        });

        deleteTweetsFromCollectionButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        deleteTweetsFromCollectionButton.setText("Delete tweets from collection");
        deleteTweetsFromCollectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteTweetsFromCollectionButtonActionPerformed(evt);
            }
        });

        createNewCollectionButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        createNewCollectionButton.setText("Create new collection");
        createNewCollectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createNewCollectionButtonActionPerformed(evt);
            }
        });

        deleteCollectionButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        deleteCollectionButton.setText("Delete collection");
        deleteCollectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteCollectionButtonActionPerformed(evt);
            }
        });

        viewCollectionOnTwitterButton.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        viewCollectionOnTwitterButton.setText("View on Twitter");
        viewCollectionOnTwitterButton.setMaximumSize(new java.awt.Dimension(140, 31));
        viewCollectionOnTwitterButton.setMinimumSize(new java.awt.Dimension(140, 31));
        viewCollectionOnTwitterButton.setPreferredSize(new java.awt.Dimension(140, 31));
        viewCollectionOnTwitterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewCollectionOnTwitterButtonActionPerformed(evt);
            }
        });

        jPanel3.setMaximumSize(new java.awt.Dimension(86, 211));
        jPanel3.setMinimumSize(new java.awt.Dimension(86, 211));

        moveTweetUpButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        moveTweetUpButton.setText("Up");
        moveTweetUpButton.setEnabled(false);
        moveTweetUpButton.setMaximumSize(new java.awt.Dimension(74, 33));
        moveTweetUpButton.setMinimumSize(new java.awt.Dimension(74, 33));
        moveTweetUpButton.setPreferredSize(new java.awt.Dimension(74, 33));
        moveTweetUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveTweetUpButtonActionPerformed(evt);
            }
        });

        moveTweetDownButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        moveTweetDownButton.setText("Down");
        moveTweetDownButton.setToolTipText("");
        moveTweetDownButton.setEnabled(false);
        moveTweetDownButton.setMaximumSize(new java.awt.Dimension(74, 33));
        moveTweetDownButton.setMinimumSize(new java.awt.Dimension(74, 33));
        moveTweetDownButton.setPreferredSize(new java.awt.Dimension(74, 33));
        moveTweetDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveTweetDownButtonActionPerformed(evt);
            }
        });

        setTweetOrderButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        setTweetOrderButton.setText("Set");
        setTweetOrderButton.setEnabled(false);
        setTweetOrderButton.setMaximumSize(new java.awt.Dimension(74, 33));
        setTweetOrderButton.setMinimumSize(new java.awt.Dimension(74, 33));
        setTweetOrderButton.setPreferredSize(new java.awt.Dimension(74, 33));
        setTweetOrderButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setTweetOrderButtonActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel7.setText("<html>Collection tweet ordering</html>");
        jLabel7.setMaximumSize(new java.awt.Dimension(74, 52));
        jLabel7.setMinimumSize(new java.awt.Dimension(74, 52));
        jLabel7.setPreferredSize(new java.awt.Dimension(74, 52));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(moveTweetDownButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(setTweetOrderButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(moveTweetUpButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(moveTweetUpButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(moveTweetDownButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(setTweetOrderButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(selectAccountComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(addTweetsToCurrentlySelectedCollectionButton)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane26, javax.swing.GroupLayout.PREFERRED_SIZE, 625, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane27, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(createNewCollectionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(deleteTweetsFromCollectionButton)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(deleteCollectionButton))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel4)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(selectCollectionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(viewCollectionOnTwitterButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tweetImageScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(tweetImageScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(tweetImageScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(tweetImageScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(selectCollectionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(viewCollectionOnTwitterButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(selectAccountComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jScrollPane27, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                        .addComponent(jScrollPane26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(addTweetsToCurrentlySelectedCollectionButton)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(deleteTweetsFromCollectionButton)
                        .addComponent(createNewCollectionButton)
                        .addComponent(deleteCollectionButton)))
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
                refreshCollectionBoxModel(false);
            }
        }
    }//GEN-LAST:event_selectAccountComboBoxActionPerformed

    private void addTweetManuallyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTweetManuallyButtonActionPerformed
        addTweetManuallyButton.setEnabled(false);
        addTweetManually();
        addTweetManuallyButton.setEnabled(true);
    }//GEN-LAST:event_addTweetManuallyButtonActionPerformed

    private void selectCollectionComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectCollectionComboBoxActionPerformed
        if (selectCollectionComboBox.isEnabled()) {
            TwitterCollectionHolder collection = (TwitterCollectionHolder) selectCollectionComboBox.getSelectedItem();
            if (!collection.equals(currentlySelectedCollection)) {
                currentlySelectedCollection = collection;
                refreshCollectionTweetsTable();
            }
        }
    }//GEN-LAST:event_selectCollectionComboBoxActionPerformed

    private void setCollectionOrderButtonsEnabled() {
        boolean invalidCollection = currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedCollection.equals(NO_COLLECTIONS)
                || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)
                || currentlySelectedCollection.equals(DB_ERROR_COLLECTION);
        moveTweetUpButton.setEnabled(!invalidCollection);
        moveTweetDownButton.setEnabled(!invalidCollection);
        setTweetOrderButton.setEnabled(!invalidCollection);
    }

    private void deleteTweetsFromCollectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteTweetsFromCollectionButtonActionPerformed
        deleteTweetsFromCollectionButton.setEnabled(false);
        deleteTweetsFromCollection();
        deleteTweetsFromCollectionButton.setEnabled(true);
    }//GEN-LAST:event_deleteTweetsFromCollectionButtonActionPerformed

    private void createNewCollectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createNewCollectionButtonActionPerformed
        createNewCollectionButton.setEnabled(false);
        createNewCollection();
        createNewCollectionButton.setEnabled(true);
    }//GEN-LAST:event_createNewCollectionButtonActionPerformed

    private void deleteCollectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteCollectionButtonActionPerformed
        deleteCollectionButton.setEnabled(false);
        deleteCollection();
        deleteCollectionButton.setEnabled(true);
    }//GEN-LAST:event_deleteCollectionButtonActionPerformed

    private void addTweetsToCurrentlySelectedCollectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTweetsToCurrentlySelectedCollectionButtonActionPerformed
        addTweetsToCurrentlySelectedCollectionButton.setEnabled(false);
        addTweetsToCollection();
        addTweetsToCurrentlySelectedCollectionButton.setEnabled(true);
    }//GEN-LAST:event_addTweetsToCurrentlySelectedCollectionButtonActionPerformed

    private void viewCollectionOnTwitterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewCollectionOnTwitterButtonActionPerformed
        viewCollectionOnTwitterButton.setEnabled(false);
        viewCollectionOnTwitter();
        viewCollectionOnTwitterButton.setEnabled(true);
    }//GEN-LAST:event_viewCollectionOnTwitterButtonActionPerformed

    private void setTweetOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setTweetOrderButtonActionPerformed
        setTweetOrderButton.setEnabled(false);
        setTweetOrder();
        setTweetOrderButton.setEnabled(true);
    }//GEN-LAST:event_setTweetOrderButtonActionPerformed

    private void moveTweetDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveTweetDownButtonActionPerformed
        moveTweetDownButton.setEnabled(false);
        moveTweet(false);
        moveTweetDownButton.setEnabled(true);
    }//GEN-LAST:event_moveTweetDownButtonActionPerformed

    private void moveTweetUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveTweetUpButtonActionPerformed
        moveTweetUpButton.setEnabled(false);
        moveTweet(true);
        moveTweetUpButton.setEnabled(true);
    }//GEN-LAST:event_moveTweetUpButtonActionPerformed

    private void setTweetOrder() {
        int row = collectionTweetsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        if (collectionTweetsTable.getRowCount() < 2) {
            return;
        }
        int modelRow = collectionTweetsTable.convertRowIndexToModel(row);
        TableModel tm = collectionTweetsTable.getModel();
    }

    private void moveTweet(boolean up) {
        int row = collectionTweetsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        if (collectionTweetsTable.getRowCount() < 2) {
            return;
        }
        int modelRow = collectionTweetsTable.convertRowIndexToModel(row);
        TableModel tm = collectionTweetsTable.getModel();
        int orderColumnIndex = collectionTweetsTable.getColumnModel().getColumnIndex("Order");
        if (up && modelRow == 0) {
            return;
        }
        if (!up && (modelRow == collectionTweetsTable.getRowCount() - 1)) {
            return;
        }
        Integer otherRowOrderNumber;
        Integer otherRowNumber;
        if (up) {
            otherRowNumber = modelRow - 1;
        } else {
            otherRowNumber = modelRow + 1;
        }
        otherRowOrderNumber = (Integer) tm.getValueAt(otherRowNumber, orderColumnIndex);
        Integer orderNumber = (Integer) tm.getValueAt(modelRow, orderColumnIndex);
        tm.setValueAt(orderNumber, otherRowNumber, orderColumnIndex);
        tm.setValueAt(otherRowOrderNumber, modelRow, orderColumnIndex);

    }

    private void viewCollectionOnTwitter() {
        if (currentlySelectedCollection.equals(NO_COLLECTIONS)
                || currentlySelectedCollection.equals(DB_ERROR_COLLECTION)) {
            return;
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(currentlySelectedCollection.getCollectionURL()));
            } catch (Exception e) {
                String msg = "An error occurred attempting to direct your browser to the collection URL, check log output.";
                JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
                LOGGER.error(msg, e);
                LOGGER.error("Collection URL was: " + currentlySelectedCollection.getCollectionURL());
            }
        } else {
            Font font = new Font("Dialog", Font.PLAIN, 12);

            StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
            style.append("font-weight:").append(font.isBold() ? "bold" : "normal").append(";");
            style.append("font-size:").append(font.getSize()).append("pt;");

            JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">"
                    + "This action isn't supported on your system. You can copy the link manually to view the collection: "
                    + currentlySelectedCollection.getCollectionURL()
                    + "</body></html>");

            ep.setEditable(false);

            JOptionPane.showMessageDialog(GUI.getInstance(), ep, "View Collection", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void addTweetsToCollection() {
        if (currentlySelectedCollection.equals(NO_COLLECTIONS)) {
            String msg = "Select a collection to add this tweet to first.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (currentlySelectedCollection.equals(DB_ERROR_COLLECTION)) {
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
            if (!checkTweetInCollectionTable(id)) {
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
        DefaultTableModel dtm = (DefaultTableModel) collectionTweetsTable.getModel();
        ArrayList<Object[]> insertParams = new ArrayList<>();
        if (validTweets.size() == 1) {
            ArrayList<Long> keys = new ArrayList<>(validTweets.keySet());

            dtm.addRow(new Object[]{validTweets.get(keys.get(0)).getId(), validTweets.get(keys.get(0)).getFullTweetText()});
            insertParams.add(new Object[]{validTweets.get(keys.get(0)).getTweetID(), currentlySelectedCollection.getTwitterID()});
            CoreDB.parameterisedCollectionTweetsMergeBatch(insertParams);
        } else {
            CollectionCurateRespJSON json = (CollectionCurateRespJSON) result.getTwitterResponse().getReturnedObject();
            CollectionCurateEntry[] entries = json.getResponse().getEntries();
            if (entries == null) {
                entries = new CollectionCurateEntry[0];
            }
            HashMap<Long, String> failedEntries = new HashMap<>();
            for (CollectionCurateEntry entry : entries) {
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
                dtm.addRow(new Object[]{validTweets.get(k).getId(), validTweets.get(k).getFullTweetText()});
            }
            if (!insertParams.isEmpty()) {
                CoreDB.parameterisedCollectionTweetsMergeBatch(insertParams);
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

    private boolean checkTweetInCollectionTable(Integer dbIDToCheck) {
        int idColumnIndex = collectionTweetsTable.getColumnModel().getColumnIndex("ID");
        int rowCount = collectionTweetsTable.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            Integer dbID = (Integer) collectionTweetsTable.getModel().getValueAt(i, idColumnIndex);
            if (dbID.equals(dbIDToCheck)) {
                return true;
            }
        }
        return false;
    }

    private void addTweetManually() {
        String url = addTweetManuallyTextField.getText().trim();
        if (RegularExpressions.matchesRegex(url, RegularExpressions.TWITTER_STATUS_REGEX)) {
            Path tweetFolderPath = CoreDB.getTweetFolderPath(currentlySelectedAccount);
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
                dtm.addRow(new Object[]{status.getInternalDatabaseID(), status.getText(),
                    status.getRetweet_count(), status.getFavorite_count()});
                refreshTweetsTable();
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

    private void deleteTweetsFromCollection() {
        int[] rows = collectionTweetsTable.getSelectedRows();
        if (rows.length == 0) {
            return;
        }
        int[] modelRows = new int[rows.length];
        for (int i = 0; i < rows.length; i++) {
            modelRows[i] = collectionTweetsTable.convertRowIndexToModel(rows[i]);
        }
        Arrays.sort(modelRows);
        int idColumnIndex = collectionTweetsTable.getColumnModel().getColumnIndex("ID");
        HashMap<Long, CollectionOperation> curationParameters = new HashMap<>();
        ArrayList<Object> tweetDBParams = new ArrayList<>();
        ArrayList<Integer> tweetIDs = new ArrayList<>();
        HashMap<Integer, Integer> dbIDModelRowMap = new HashMap<>();
        for (int i = 0; i < modelRows.length; i++) {
            Integer id = (Integer) collectionTweetsTable.getModel().getValueAt(modelRows[i], idColumnIndex);
            tweetDBParams.add(id);
            tweetIDs.add(id);
            dbIDModelRowMap.put(id, modelRows[i]);
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
                curationParameters.put(tweet.getTweetID(), CollectionOperation.REMOVE);
                validTweets.put(tweet.getTweetID(), tweet);
            }
        }
        OperationResult result;
        if (validTweets.size() == 1) {
            ArrayList<Long> keys = new ArrayList<>(validTweets.keySet());
            result = RESTAPI.collectionsEntriesRemove(currentlySelectedCollection.getTwitterID(),
                    keys.get(0), currentlySelectedAccount);
        } else {
            CollectionCurateParamsJSON jsonData = new CollectionCurateParamsJSON()
                    .setId(currentlySelectedCollection.getTwitterID());
            jsonData.setChanges(curationParameters);
            result = RESTAPI.collectionsEntriesCurate(jsonData, currentlySelectedAccount);
        }

        if (!result.wasSuccessful()) {
            GUIHelperMethods.showErrors(result, LOGGER, null);
            return;
        }
        DefaultTableModel dtm = (DefaultTableModel) collectionTweetsTable.getModel();
        if (validTweets.size() == 1) {
            dtm.removeRow(modelRows[0]);
            ArrayList<Long> keys = new ArrayList<>(validTweets.keySet());
            CoreDB.deleteFromTable(DBTable.COLLECTIONTWEETS,
                    new String[]{"collectionid", "tweetid"},
                    new Object[]{currentlySelectedCollection.getTwitterID(), keys.get(0)});
        } else {
            CollectionCurateRespJSON json = (CollectionCurateRespJSON) result.getTwitterResponse().getReturnedObject();
            CollectionCurateEntry[] entries = json.getResponse().getEntries();
            if (entries == null) {
                entries = new CollectionCurateEntry[0];
            }
            HashMap<Long, String> failedEntries = new HashMap<>();
            for (CollectionCurateEntry entry : entries) {
                failedEntries.put(entry.getEntry().getTweetID(), entry.getEntry().getOp());
            }
            Set<Long> attemptedIDs = validTweets.keySet();
            Set<Long> failedEntryKeys = failedEntries.keySet();
            ArrayList<Integer> modelRowsToRemove = new ArrayList<>();
            ArrayList<Long> tweetIDsToDelete = new ArrayList<>();
            for (Long tweetID : attemptedIDs) {
                if (!failedEntryKeys.contains(tweetID)) {
                    modelRowsToRemove.add(dbIDModelRowMap.get(validTweets.get(tweetID).getId()));
                    tweetIDsToDelete.add(tweetID);
                }
            }
            Collections.sort(modelRowsToRemove);
            for (int i = 0; i < modelRowsToRemove.size(); i++) {
                dtm.removeRow(modelRowsToRemove.get(i) - i);
            }
            String deleteQuery = "DELETE FROM collectiontweets WHERE collectionid=? AND tweetid IN (?";
            Object[] deleteParams = new Object[tweetIDsToDelete.size() + 1];
            deleteParams[0] = currentlySelectedCollection.getTwitterID();
            deleteParams[1] = tweetIDsToDelete.get(0);
            for (int i = 1; i < tweetIDsToDelete.size(); i++) {
                deleteQuery = deleteQuery.concat(",?");
                deleteParams[i + 1] = tweetIDsToDelete.get(i);
            }
            deleteQuery = deleteQuery.concat(")");
            if (!tweetIDsToDelete.isEmpty()) {
                CoreDB.runCustomUpdate(deleteQuery, deleteParams);
            }
            if (!failedEntries.isEmpty()) {
                StringBuilder msg = new StringBuilder("<html>One or more entries were not deleted successfully.<br/><br/>");
                for (Long l : failedEntryKeys) {
                    String reason = failedEntries.get(l);
                    msg = msg.append("Tweet ID ").append(dbIDModelRowMap.get(validTweets.get(l).getId()))
                            .append(" failed with reason: ").append(reason).append("<br/>");
                }
                JOptionPane.showMessageDialog(GUI.getInstance(), msg.toString(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    private void deleteCollection() {
        if (currentlySelectedCollection.equals(NO_COLLECTIONS)
                || currentlySelectedCollection.equals(DB_ERROR_COLLECTION)) {
            String msg = "Failed to retrieve tweets for this user from DB!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        String msg = "<html>Deleting a collection cannot be undone. Are you sure?</html>";
        Integer result = JOptionPane.showConfirmDialog(GUI.getInstance(), msg, "Delete Collection", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            OperationResult opResult = RESTAPI.collectionsDestroy(currentlySelectedCollection.getTwitterID(), currentlySelectedAccount);
            if (opResult.wasSuccessful()) {
                CoreDB.deleteFromTable(DBTable.COLLECTIONS,
                        new String[]{"id"},
                        new Object[]{currentlySelectedCollection.getDatabaseID()});
                refreshCollectionBoxModel(false);
                DefaultTableModel dtm = (DefaultTableModel) collectionTweetsTable.getModel();
                dtm.setRowCount(0);
            } else {
                GUIHelperMethods.showErrors(opResult, LOGGER, null);
            }
        }
    }

    private void createNewCollection() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS)) {
            String msg = "<html>You cannot add a collection until you have added an account on the Accounts panel.</html>";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        if (currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            String msg = "<html>You cannot add a collection right now due to a database error.</html>";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }

        String message = "Please enter a name for your new collection, and (optionally) a description.";
        String[] inputNames = new String[]{"Name: ", "Description: ", "Ordering: "};
        JComboBox box = new JComboBox();
        box.addItem(CollectionOrdering.CURATION_REVERSE_CHRON);
        box.addItem(CollectionOrdering.TWEET_CHRON);
        box.addItem(CollectionOrdering.TWEET_REVERSE_CHRON);
        JComponent[] components = new JComponent[]{new JTextField(), new JTextField(), box};
        ArrayList<Object> results = SwingTools.askForUserInput("Create a new Twitter collection", message,
                inputNames, components);
        if (results == null) {
            return;
        }

        String name = (String) results.get(0);
        String description = (String) results.get(1);
        CollectionOrdering ordering = (CollectionOrdering) results.get(2);
        if (description.trim().equals("")) {
            description = null;
        }
        if (name.trim().equals("")) {
            String msg = "Collection name cannot be empty.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        OperationResult res = RESTAPI.collectionsCreate(name, description, ordering, currentlySelectedAccount);
        if (res.wasSuccessful()) {
            TwitterCollectionHolder holder = (TwitterCollectionHolder) res.getTwitterResponse().getReturnedObject();
            Object[] params = new Object[]{currentlySelectedAccount.getTwitterID(), holder.getTwitterID(),
                holder.getCollectionURL(), holder.getName(), holder.getDescription(), holder.getOrdering().getParameterName()};
            CoreDB.insertCollection(params);
            String msg = "Collection added successfully!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshCollectionBoxModel(false);
        } else {
            GUIHelperMethods.showErrors(res, LOGGER, null);
        }

    }

    private void refreshCollectionTweetsTable() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedCollection.equals(NO_COLLECTIONS)
                || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)
                || currentlySelectedCollection.equals(DB_ERROR_COLLECTION)) {
            return;
        }

        String query = "SELECT * FROM tweets WHERE tweetid IN (SELECT tweetid FROM collectiontweets WHERE collectionid=?)";
        DBResponse resp = CoreDB.customQuerySelect(query, currentlySelectedCollection.getTwitterID());
        if (!resp.wasSuccessful()) {
            String msg = "Failed to retrieve tweets for this collection from DB!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        DefaultTableModel dtm = (DefaultTableModel) collectionTweetsTable.getModel();
        dtm.setRowCount(0);
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            TweetHolder tweet = ResultSetConversion.getTweet(row);
            dtm.addRow(new Object[]{tweet.getId(), tweet.getFullTweetText()});
        }
    }

    public Account getCurrentlySelectedAccount() {
        return currentlySelectedAccount;
    }

    public void refreshTweetsTable() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }
        String query = "SELECT * FROM tweets WHERE usertwitterid=?";
        DBResponse resp = CoreDB.customQuerySelect(query, currentlySelectedAccount.getTwitterID());
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

    /**
     * Clears all of the image preview panels.
     */
    protected void clearImagePanels() {
        tweetImageLabel1.setIcon(null);
        tweetImageLabel2.setIcon(null);
        tweetImageLabel3.setIcon(null);
        tweetImageLabel4.setIcon(null);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addTweetManuallyButton;
    private javax.swing.JLabel addTweetManuallyStatusLabel;
    private javax.swing.JTextField addTweetManuallyTextField;
    private javax.swing.JButton addTweetsToCurrentlySelectedCollectionButton;
    protected javax.swing.JTable collectionTweetsTable;
    private javax.swing.JButton createNewCollectionButton;
    private javax.swing.JButton deleteCollectionButton;
    private javax.swing.JButton deleteTweetsFromCollectionButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane26;
    private javax.swing.JScrollPane jScrollPane27;
    private javax.swing.JButton moveTweetDownButton;
    private javax.swing.JButton moveTweetUpButton;
    private javax.swing.JComboBox<String> selectAccountComboBox;
    private javax.swing.JComboBox<String> selectCollectionComboBox;
    private javax.swing.JButton setTweetOrderButton;
    protected javax.swing.JLabel tweetImageLabel1;
    protected javax.swing.JLabel tweetImageLabel2;
    protected javax.swing.JLabel tweetImageLabel3;
    protected javax.swing.JLabel tweetImageLabel4;
    protected javax.swing.JScrollPane tweetImageScrollPane1;
    protected javax.swing.JScrollPane tweetImageScrollPane2;
    protected javax.swing.JScrollPane tweetImageScrollPane3;
    protected javax.swing.JScrollPane tweetImageScrollPane4;
    protected javax.swing.JTable tweetsTable;
    private javax.swing.JButton viewCollectionOnTwitterButton;
    // End of variables declaration//GEN-END:variables
}
