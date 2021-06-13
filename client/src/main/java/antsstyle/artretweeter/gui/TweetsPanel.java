/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package antsstyle.artretweeter.gui;

import antsstyle.artretweeter.datastructures.Account;
import antsstyle.artretweeter.datastructures.CollectionCurateParamsJSON;
import antsstyle.artretweeter.datastructures.CollectionOperation;
import antsstyle.artretweeter.datastructures.CollectionOrdering;
import antsstyle.artretweeter.datastructures.OperationResult;
import antsstyle.artretweeter.datastructures.StatusJSON;
import antsstyle.artretweeter.datastructures.TweetHolder;
import antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import antsstyle.artretweeter.db.CoreDB;
import antsstyle.artretweeter.db.DBResponse;
import antsstyle.artretweeter.db.DBTable;
import antsstyle.artretweeter.db.ResultSetConversion;
import antsstyle.artretweeter.tools.ImageTools;
import antsstyle.artretweeter.tools.PathTools;
import antsstyle.artretweeter.tools.RegularExpressions;
import antsstyle.artretweeter.tools.SwingTools;
import antsstyle.artretweeter.twitter.Endpoint;
import antsstyle.artretweeter.twitter.RESTAPI;
import com.google.gson.Gson;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class TweetsPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = LogManager.getLogger(TweetsPanel.class);

    private final DefaultComboBoxModel selectAccountBoxModel = new DefaultComboBoxModel();
    private final DefaultComboBoxModel selectCollectionBoxModel = new DefaultComboBoxModel();

    private static final int STANDARD_PANEL_WIDTH = 420;
    private static final int STANDARD_PANEL_HEIGHT = 641;
    private static final int STANDARD_PANEL_MARGIN = 2;
    private static final int STANDARD_PANEL_INSET = 1;

    private static final String PENDING_ADD = "Pending Add";
    private static final String PENDING_DELETE = "Pending Delete";
    private static final String IN_COLLECTION = "In Collection";

    private Account currentlySelectedAccount = null;
    private TwitterCollectionHolder currentlySelectedCollection = null;

    private static final Account NO_ACCOUNTS = new Account()
            .setScreenName("<no accounts added>");

    private static final Account DB_ERROR_ACCOUNT = new Account()
            .setScreenName("<database error>");

    private static final TwitterCollectionHolder SELECT_ACCOUNT_FIRST = new TwitterCollectionHolder()
            .setTwitterID("<select an account first>");

    private static final TwitterCollectionHolder NO_COLLECTIONS = new TwitterCollectionHolder()
            .setTwitterID("<no collections for this account>");

    private static final TwitterCollectionHolder DB_ERROR_COLLECTION = new TwitterCollectionHolder()
            .setTwitterID("<database error>");

    private static final HashMap<String, HashMap<Long, CollectionOperation>> curationChanges = new HashMap<>();

    /**
     * Creates new form TweetsPanel
     */
    public TweetsPanel() {
        initComponents();
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(3, SortOrder.DESCENDING));
        tweetsTable.getRowSorter().setSortKeys(sortKeys);
    }

    public void refreshAccountBoxModel(boolean initialRefresh) {
        selectAccountComboBox.setEnabled(false);
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
            if (initialRefresh) {
                refreshTweetsTable();
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
        selectCollectionBoxModel.removeAllElements();
        if (currentlySelectedAccount.equals(NO_ACCOUNTS)) {
            selectCollectionBoxModel.addElement(SELECT_ACCOUNT_FIRST);
            currentlySelectedCollection = SELECT_ACCOUNT_FIRST;
            selectCollectionBoxModel.setSelectedItem(selectCollectionBoxModel.getElementAt(0));
            selectCollectionComboBox.setEnabled(true);
            return;
        } else if (currentlySelectedCollection != null && currentlySelectedCollection.equals(NO_COLLECTIONS)) {
            selectCollectionBoxModel.addElement(NO_COLLECTIONS);
            currentlySelectedCollection = NO_COLLECTIONS;
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
            if (initialRefresh) {
                refreshCollectionTweetsTable();
            }
        } else {
            selectCollectionBoxModel.addElement(NO_COLLECTIONS);
            currentlySelectedCollection = NO_COLLECTIONS;
        }
        selectCollectionComboBox.setEnabled(true);
    }

    public void initialise() {
        tweetsTable.getSelectionModel().addListSelectionListener((ListSelectionEvent event) -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int row = tweetsTable.getSelectedRow();
            if (row == -1) {
                return;
            }
            showTweetPreview();
        });
        refreshAccountBoxModel(true);
        refreshCollectionBoxModel(true);
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
        retweetApprovalScrollPane1 = new javax.swing.JScrollPane();
        tweetImageLabel1 = new javax.swing.JLabel();
        retweetApprovalScrollPane2 = new javax.swing.JScrollPane();
        tweetImageLabel2 = new javax.swing.JLabel();
        retweetApprovalScrollPane3 = new javax.swing.JScrollPane();
        tweetImageLabel3 = new javax.swing.JLabel();
        retweetApprovalScrollPane4 = new javax.swing.JScrollPane();
        tweetImageLabel4 = new javax.swing.JLabel();
        jScrollPane27 = new javax.swing.JScrollPane();
        collectionTweetsTable = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        addTweetManuallyTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        addTweetManuallyButton = new javax.swing.JButton();
        addTweetManuallyStatusLabel = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        commitCollectionChangesButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        selectCollectionComboBox = new javax.swing.JComboBox<>();
        addTweetToCurrentlySelectedCollectionButton = new javax.swing.JButton();
        deleteTweetFromCollectionButton = new javax.swing.JButton();
        createNewCollectionButton = new javax.swing.JButton();
        deleteCollectionButton = new javax.swing.JButton();
        viewCollectionOnTwitterButton = new javax.swing.JButton();

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
                false, false, false, false, true
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

        retweetApprovalScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        retweetApprovalScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        retweetApprovalScrollPane1.setPreferredSize(new java.awt.Dimension(420, 641));

        tweetImageLabel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 3, true));
        tweetImageLabel1.setOpaque(true);
        retweetApprovalScrollPane1.setViewportView(tweetImageLabel1);

        retweetApprovalScrollPane2.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        retweetApprovalScrollPane2.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        retweetApprovalScrollPane2.setPreferredSize(new java.awt.Dimension(420, 641));

        tweetImageLabel2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 3, true));
        tweetImageLabel2.setOpaque(true);
        retweetApprovalScrollPane2.setViewportView(tweetImageLabel2);

        retweetApprovalScrollPane3.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        retweetApprovalScrollPane3.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        retweetApprovalScrollPane3.setPreferredSize(new java.awt.Dimension(420, 641));

        tweetImageLabel3.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 3, true));
        tweetImageLabel3.setOpaque(true);
        retweetApprovalScrollPane3.setViewportView(tweetImageLabel3);

        retweetApprovalScrollPane4.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        retweetApprovalScrollPane4.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        retweetApprovalScrollPane4.setPreferredSize(new java.awt.Dimension(420, 641));

        tweetImageLabel4.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 3, true));
        tweetImageLabel4.setOpaque(true);
        retweetApprovalScrollPane4.setViewportView(tweetImageLabel4);

        jScrollPane27.setMaximumSize(new java.awt.Dimension(898, 184));
        jScrollPane27.setMinimumSize(new java.awt.Dimension(898, 184));
        jScrollPane27.setPreferredSize(new java.awt.Dimension(898, 184));

        collectionTweetsTable.setAutoCreateRowSorter(true);
        collectionTweetsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Tweet Text", "Status"
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
        jScrollPane27.setViewportView(collectionTweetsTable);
        if (collectionTweetsTable.getColumnModel().getColumnCount() > 0) {
            collectionTweetsTable.getColumnModel().getColumn(0).setMinWidth(50);
            collectionTweetsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
            collectionTweetsTable.getColumnModel().getColumn(0).setMaxWidth(50);
            collectionTweetsTable.getColumnModel().getColumn(2).setMinWidth(150);
            collectionTweetsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
            collectionTweetsTable.getColumnModel().getColumn(2).setMaxWidth(150);
        }

        addTweetManuallyTextField.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel3.setText("Add tweet manually:");

        addTweetManuallyButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        addTweetManuallyButton.setText("Add");
        addTweetManuallyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTweetManuallyButtonActionPerformed(evt);
            }
        });

        jLabel6.setText("<html>Note: Adding or removing tweets from a collection is not finalised until you press the \"Commit collection changes\" button.</html>");

        commitCollectionChangesButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        commitCollectionChangesButton.setText("<html>Commit collection changes</html>");
        commitCollectionChangesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commitCollectionChangesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(commitCollectionChangesButton, javax.swing.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(commitCollectionChangesButton)
                    .addComponent(jLabel6))
                .addContainerGap())
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

        addTweetToCurrentlySelectedCollectionButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        addTweetToCurrentlySelectedCollectionButton.setText("Add tweet to currently selected collection");
        addTweetToCurrentlySelectedCollectionButton.setToolTipText("");
        addTweetToCurrentlySelectedCollectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTweetToCurrentlySelectedCollectionButtonActionPerformed(evt);
            }
        });

        deleteTweetFromCollectionButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        deleteTweetFromCollectionButton.setText("Delete tweet from collection");
        deleteTweetFromCollectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteTweetFromCollectionButtonActionPerformed(evt);
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
                                .addComponent(jScrollPane26, javax.swing.GroupLayout.PREFERRED_SIZE, 717, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(selectAccountComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(addTweetToCurrentlySelectedCollectionButton))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane27, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(createNewCollectionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(deleteTweetFromCollectionButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(deleteCollectionButton)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(selectCollectionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(viewCollectionOnTwitterButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(retweetApprovalScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(retweetApprovalScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)
                        .addComponent(retweetApprovalScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(4, 4, 4)
                        .addComponent(retweetApprovalScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(selectCollectionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(viewCollectionOnTwitterButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(selectAccountComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane27, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                    .addComponent(jScrollPane26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(addTweetToCurrentlySelectedCollectionButton)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(deleteTweetFromCollectionButton)
                        .addComponent(createNewCollectionButton)
                        .addComponent(deleteCollectionButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(retweetApprovalScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(retweetApprovalScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(retweetApprovalScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(retweetApprovalScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

    private void deleteTweetFromCollectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteTweetFromCollectionButtonActionPerformed
        deleteTweetFromCollectionButton.setEnabled(false);
        deleteTweetFromCollection();
        deleteTweetFromCollectionButton.setEnabled(true);
    }//GEN-LAST:event_deleteTweetFromCollectionButtonActionPerformed

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

    private void commitCollectionChangesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commitCollectionChangesButtonActionPerformed
        commitCollectionChangesButton.setEnabled(false);
        commitCollectionChanges();
        commitCollectionChangesButton.setEnabled(true);
    }//GEN-LAST:event_commitCollectionChangesButtonActionPerformed

    private void addTweetToCurrentlySelectedCollectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTweetToCurrentlySelectedCollectionButtonActionPerformed
        addTweetToCurrentlySelectedCollectionButton.setEnabled(false);
        addTweetToCollection();
        addTweetToCurrentlySelectedCollectionButton.setEnabled(true);
    }//GEN-LAST:event_addTweetToCurrentlySelectedCollectionButtonActionPerformed

    private void viewCollectionOnTwitterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewCollectionOnTwitterButtonActionPerformed
        viewCollectionOnTwitterButton.setEnabled(false);
        viewCollectionOnTwitter();
        viewCollectionOnTwitterButton.setEnabled(true);
    }//GEN-LAST:event_viewCollectionOnTwitterButtonActionPerformed

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

    private void addTweetToCollection() {
        if (currentlySelectedCollection.equals(NO_COLLECTIONS)) {
            String msg = "Select a collection to add this tweet to first.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        } else if (currentlySelectedCollection.equals(DB_ERROR_COLLECTION)) {
            String msg = "You cannot add tweets to collections until the database error is resolved.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        int row = tweetsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        int modelRow = tweetsTable.convertRowIndexToModel(row);
        int idColumnIndex = tweetsTable.getColumnModel().getColumnIndex("ID");
        int textColumnIndex = tweetsTable.getColumnModel().getColumnIndex("Tweet Text");
        Integer id = (Integer) tweetsTable.getModel().getValueAt(modelRow, idColumnIndex);
        if (checkTweetInCollectionTable(id)) {
            return;
        }

        DBResponse resp = CoreDB.selectFromTable(DBTable.TWEETS,
                new String[]{"id"},
                new Object[]{id});
        if (!resp.wasSuccessful()) {
            String msg = "Failed to retrieve tweet information from DB - check log output.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        } else if (resp.getReturnedRows().isEmpty()) {
            String msg = "Tweet record does not exist in DB - has the database file been modified?";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        TweetHolder tweet = ResultSetConversion.getTweet(resp.getReturnedRows().get(0));

        HashMap<Long, CollectionOperation> changes = curationChanges.get(currentlySelectedCollection.getTwitterID());
        if (changes == null) {
            changes = new HashMap<>();
            changes.put(tweet.getTweetID(), CollectionOperation.ADD);
            curationChanges.put(currentlySelectedCollection.getTwitterID(), changes);
        } else {
            changes.put(tweet.getTweetID(), CollectionOperation.ADD);
        }
        String tweetText = (String) tweetsTable.getModel().getValueAt(modelRow, textColumnIndex);
        DefaultTableModel dtm = (DefaultTableModel) collectionTweetsTable.getModel();
        dtm.addRow(new Object[]{id, tweetText, "Pending Add"});
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
            OperationResult res = RESTAPI.getTweetByID(tweetID, currentlySelectedAccount, tweetFolderPath);
            if (res.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
                StatusJSON status = (StatusJSON) res.getReturnedObject();
                DefaultTableModel dtm = (DefaultTableModel) tweetsTable.getModel();
                dtm.addRow(new Object[]{status.getInternalDatabaseID(), status.getText(),
                    status.getRetweet_count(), status.getFavorite_count()});
                refreshTweetsTable();
                addTweetManuallyTextField.setText("");
                addTweetManuallyStatusLabel.setText("Tweet added successfully.");
            } else {
                GUIHelperMethods.showErrorMessage(res, LOGGER, null);
            }
        } else {
            String msg = "The entered URL is not a valid Twitter status URL.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
        }
    }

    private void deleteTweetFromCollection() {
        int row = collectionTweetsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        int modelRow = collectionTweetsTable.convertRowIndexToModel(row);
        int idColumnIndex = collectionTweetsTable.getColumnModel().getColumnIndex("ID");
        int statusColumnIndex = collectionTweetsTable.getColumnModel().getColumnIndex("Status");
        Integer id = (Integer) collectionTweetsTable.getModel().getValueAt(modelRow, idColumnIndex);

        DBResponse resp = CoreDB.selectFromTable(DBTable.TWEETS,
                new String[]{"id"},
                new Object[]{id});
        if (!resp.wasSuccessful()) {
            String msg = "Failed to retrieve tweet information from DB - check log output.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        } else if (resp.getReturnedRows().isEmpty()) {
            String msg = "Tweet record does not exist in DB - has the database file been modified?";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        TweetHolder tweet = ResultSetConversion.getTweet(resp.getReturnedRows().get(0));

        String status = (String) collectionTweetsTable.getValueAt(modelRow, statusColumnIndex);
        if (status.equals(PENDING_ADD)) {
            DefaultTableModel dtm = (DefaultTableModel) collectionTweetsTable.getModel();
            dtm.removeRow(modelRow);
            HashMap<Long, CollectionOperation> changes = curationChanges.get(currentlySelectedCollection.getTwitterID());
            changes.remove(tweet.getTweetID());
            curationChanges.remove(currentlySelectedCollection.getTwitterID());
        } else if (status.equals(IN_COLLECTION)) {
            collectionTweetsTable.setValueAt("Pending Delete", modelRow, statusColumnIndex);
            HashMap<Long, CollectionOperation> changes = curationChanges.get(currentlySelectedCollection.getTwitterID());
            if (changes == null) {
                changes = new HashMap<>();
                changes.put(tweet.getTweetID(), CollectionOperation.REMOVE);
                curationChanges.put(currentlySelectedCollection.getTwitterID(), changes);
            } else {
                changes.put(tweet.getTweetID(), CollectionOperation.REMOVE);
            }
        }
    }

    private void commitCollectionChanges() {
        if (curationChanges.isEmpty()) {
            return;
        }
        Gson gson = new Gson();
        Set<String> collectionsToCurate = curationChanges.keySet();
        boolean errors = false;
        String endText = "If multiple collections were"
                + " curated, not all may have been updated.";
        OperationResult result = null;
        for (String k : collectionsToCurate) {
            CollectionCurateParamsJSON json = new CollectionCurateParamsJSON();
            json.setId(k);
            json.setChanges(curationChanges.get(k));
            LOGGER.debug("Number of changes to curate: " + curationChanges.get(k).size());
            String jsonString = gson.toJson(json, CollectionCurateParamsJSON.class);
            result = RESTAPI.collectionCurate(jsonString, currentlySelectedAccount);
            if (!result.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
                errors = true;
            }
            RESTAPI.getFullyHydratedCollectionByID(k, currentlySelectedAccount);
        }
        if (errors) {
            GUIHelperMethods.showErrorMessage(result, LOGGER, endText);
        } else {
            String msg = "<html>Collections curated successfully.</html>";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Success", JOptionPane.INFORMATION_MESSAGE);
        }

        curationChanges.clear();
        refreshCollectionTweetsTable();
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
            OperationResult opResult = RESTAPI.collectionDestroy(currentlySelectedCollection.getTwitterID(), currentlySelectedAccount);
            if (opResult.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
                CoreDB.deleteFromTable(DBTable.COLLECTIONS,
                        new String[]{"id"},
                        new Object[]{currentlySelectedCollection.getDatabaseID()});
                refreshCollectionBoxModel(false);
                DefaultTableModel dtm = (DefaultTableModel) collectionTweetsTable.getModel();
                dtm.setRowCount(0);
            } else {
                GUIHelperMethods.showErrorMessage(opResult, LOGGER, null);
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
        ArrayList<Object> results = SwingTools.askForUserInput("Create a new Twitter collection", message, "Name: ", "Description: ");
        if (results == null) {
            return;
        }

        String name = (String) results.get(0);
        String description = (String) results.get(1);
        if (description.trim().equals("")) {
            description = null;
        }
        if (name.trim().equals("")) {
            String msg = "Collection name cannot be empty.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        OperationResult res = RESTAPI.collectionCreate(name, description, CollectionOrdering.CURATION_REVERSE_CHRON, currentlySelectedAccount);
        if (res.getArtRetweeterStatusCode().equals(OperationResult.QUERY_OK)) {
            TwitterCollectionHolder holder = (TwitterCollectionHolder) res.getReturnedObject();
            Object[] params = new Object[]{currentlySelectedAccount.getTwitterID(), holder.getTwitterID(),
                holder.getCollectionURL(), holder.getName(), holder.getDescription(), holder.getOrdering().getParameterName()};
            CoreDB.insertCollection(params);
            String msg = "Collection added successfully!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshCollectionBoxModel(false);
        } else {
            GUIHelperMethods.showErrorMessage(res, LOGGER, null);
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
            dtm.addRow(new Object[]{tweet.getId(), tweet.getFullTweetText(), IN_COLLECTION});
        }
    }

    public Account getCurrentlySelectedAccount() {
        return currentlySelectedAccount;
    }

    public void refreshTweetsTable() {
        if (currentlySelectedAccount.equals(NO_ACCOUNTS) || currentlySelectedAccount.equals(DB_ERROR_ACCOUNT)) {
            return;
        }
        DBResponse resp = CoreDB.selectFromTable(DBTable.TWEETS,
                new String[]{"usertwitterid"},
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

    /**
     * Clears all of the image preview panels.
     */
    protected void clearImagePanels() {
        tweetImageLabel1.setIcon(null);
        tweetImageLabel2.setIcon(null);
        tweetImageLabel3.setIcon(null);
        tweetImageLabel4.setIcon(null);
    }

    /**
     * Displays the images related to the selected data entry in the management table.
     */
    protected void showTweetPreview() {
        int row = tweetsTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        int modelRow = tweetsTable.convertRowIndexToModel(row);
        TableColumnModel tcm = tweetsTable.getColumnModel();
        TableModel model = tweetsTable.getModel();
        int idColumnIndex = tcm.getColumnIndex("ID");
        Integer id = (Integer) model.getValueAt(modelRow, idColumnIndex);
        DBResponse resp = CoreDB.selectFromTable(DBTable.TWEETS,
                new String[]{"id"},
                new Object[]{id});
        if (!resp.wasSuccessful()) {
            String msg = "Failed to retrieve tweet information from database!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        if (resp.getReturnedRows().isEmpty()) {
            String msg = "Tweet was not found in the database!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return;
        }
        TweetHolder holder = ResultSetConversion.getTweet(resp.getReturnedRows().get(0));

        ArrayList<Path> filePaths = holder.getFilePaths();
        int numImages = filePaths.size();
        if (numImages < 1 || numImages > 4) {
            // Invalid number of images in tweet
            String msg = "Invalid number of images in tweet - check log output.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error("Number of images in tweet: " + numImages);
            return;
        }
        ArrayList<BufferedImage> images = new ArrayList<>();
        int combinedImageWidth = 0;
        for (int i = 0; i < numImages; i++) {
            Path filePath = filePaths.get(i);
            try {
                BufferedImage img = ImageIO.read(filePath.toFile());
                images.add(img);
                combinedImageWidth += img.getWidth();
            } catch (IOException e) {
                LOGGER.error("Failed to load image number " + i + "!", e);
                String msg = "Failed to load images for tweet - check log output.";
                JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        ArrayList<Double> widthRatios = new ArrayList<>();
        for (BufferedImage img : images) {
            int width = img.getWidth();
            double ratio = (double) width / combinedImageWidth;
            widthRatios.add(ratio);
        }
        int x;
        int y = retweetApprovalScrollPane1.getY();
        int fullWidthAvailable = 4 * (STANDARD_PANEL_WIDTH + STANDARD_PANEL_MARGIN + STANDARD_PANEL_INSET);
        switch (numImages) {
            case 1:
                Dimension d1 = new Dimension((int) (widthRatios.get(0) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                setAllSizes(retweetApprovalScrollPane1, d1);
                setAllSizes(tweetImageLabel1, d1);
                retweetApprovalScrollPane2.setVisible(false);
                retweetApprovalScrollPane3.setVisible(false);
                retweetApprovalScrollPane4.setVisible(false);
                break;
            case 2:
                d1 = new Dimension((int) (widthRatios.get(0) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                setAllSizes(retweetApprovalScrollPane1, d1);
                setAllSizes(tweetImageLabel1, d1);
                x = retweetApprovalScrollPane1.getX() + (int) d1.getWidth();
                Dimension d2 = new Dimension((int) (widthRatios.get(1) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                setAllSizes(retweetApprovalScrollPane2, d2);
                setAllSizes(tweetImageLabel2, d2);
                retweetApprovalScrollPane2.setLocation(x, y);
                retweetApprovalScrollPane2.setVisible(true);
                retweetApprovalScrollPane3.setVisible(false);
                retweetApprovalScrollPane4.setVisible(false);
                break;
            case 3:
                d1 = new Dimension((int) (widthRatios.get(0) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                setAllSizes(retweetApprovalScrollPane1, d1);
                setAllSizes(tweetImageLabel1, d1);
                x = retweetApprovalScrollPane1.getX() + (int) d1.getWidth();
                d2 = new Dimension((int) (widthRatios.get(1) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                setAllSizes(retweetApprovalScrollPane2, d2);
                setAllSizes(tweetImageLabel2, d2);
                retweetApprovalScrollPane2.setLocation(x, y);
                Dimension d3 = new Dimension((int) (widthRatios.get(2) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                x = retweetApprovalScrollPane2.getX() + (int) d2.getWidth();
                setAllSizes(retweetApprovalScrollPane3, d3);
                setAllSizes(tweetImageLabel3, d3);
                retweetApprovalScrollPane3.setLocation(x, y);
                retweetApprovalScrollPane2.setVisible(true);
                retweetApprovalScrollPane3.setVisible(true);
                retweetApprovalScrollPane4.setVisible(false);
                break;
            case 4:
                d1 = new Dimension((int) (widthRatios.get(0) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                setAllSizes(retweetApprovalScrollPane1, d1);
                setAllSizes(tweetImageLabel1, d1);
                x = retweetApprovalScrollPane1.getX() + (int) d1.getWidth();
                d2 = new Dimension((int) (widthRatios.get(1) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                setAllSizes(retweetApprovalScrollPane2, d2);
                setAllSizes(tweetImageLabel2, d2);
                retweetApprovalScrollPane2.setLocation(x, y);
                x = retweetApprovalScrollPane2.getX() + (int) d2.getWidth();
                d3 = new Dimension((int) (widthRatios.get(2) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                setAllSizes(retweetApprovalScrollPane3, d3);
                setAllSizes(tweetImageLabel3, d3);
                retweetApprovalScrollPane3.setLocation(x, y);
                x = retweetApprovalScrollPane3.getX() + (int) d3.getWidth();
                Dimension d4 = new Dimension((int) (widthRatios.get(3) * fullWidthAvailable), STANDARD_PANEL_HEIGHT);
                setAllSizes(retweetApprovalScrollPane4, d4);
                setAllSizes(tweetImageLabel4, d4);
                retweetApprovalScrollPane4.setLocation(x, y);
                retweetApprovalScrollPane2.setVisible(true);
                retweetApprovalScrollPane3.setVisible(true);
                retweetApprovalScrollPane4.setVisible(true);
                break;
        }
        int width = retweetApprovalScrollPane1.getWidth();
        int height = retweetApprovalScrollPane1.getHeight();
        ImageIcon icon = new ImageIcon(ImageTools.getScaledImageForViewing(images.get(0), width, height));
        tweetImageLabel1.setIcon(icon);
        if (numImages > 1) {
            width = retweetApprovalScrollPane2.getWidth();
            height = retweetApprovalScrollPane2.getHeight();
            icon = new ImageIcon(ImageTools.getScaledImageForViewing(images.get(1), width, height));
            tweetImageLabel2.setIcon(icon);
        }
        if (numImages > 2) {
            width = retweetApprovalScrollPane3.getWidth();
            height = retweetApprovalScrollPane3.getHeight();
            icon = new ImageIcon(ImageTools.getScaledImageForViewing(images.get(2), width, height));
            tweetImageLabel3.setIcon(icon);
        }
        if (numImages > 3) {
            width = retweetApprovalScrollPane4.getWidth();
            height = retweetApprovalScrollPane4.getHeight();
            icon = new ImageIcon(ImageTools.getScaledImageForViewing(images.get(3), width, height));
            tweetImageLabel4.setIcon(icon);
        }
        //tweetTextArea.setText(fullTweetText);
    }

    private void setAllSizes(JLabel label, Dimension d) {
        label.setMinimumSize(d);
        label.setSize(d);
        label.setMaximumSize(d);
        label.setPreferredSize(d);
    }

    private void setAllSizes(JScrollPane pane, Dimension d) {
        pane.setMinimumSize(d);
        pane.setSize(d);
        pane.setMaximumSize(d);
        pane.setPreferredSize(d);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addTweetManuallyButton;
    private javax.swing.JLabel addTweetManuallyStatusLabel;
    private javax.swing.JTextField addTweetManuallyTextField;
    private javax.swing.JButton addTweetToCurrentlySelectedCollectionButton;
    protected javax.swing.JTable collectionTweetsTable;
    private javax.swing.JButton commitCollectionChangesButton;
    private javax.swing.JButton createNewCollectionButton;
    private javax.swing.JButton deleteCollectionButton;
    private javax.swing.JButton deleteTweetFromCollectionButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane26;
    private javax.swing.JScrollPane jScrollPane27;
    protected javax.swing.JScrollPane retweetApprovalScrollPane1;
    protected javax.swing.JScrollPane retweetApprovalScrollPane2;
    protected javax.swing.JScrollPane retweetApprovalScrollPane3;
    protected javax.swing.JScrollPane retweetApprovalScrollPane4;
    private javax.swing.JComboBox<String> selectAccountComboBox;
    private javax.swing.JComboBox<String> selectCollectionComboBox;
    protected javax.swing.JLabel tweetImageLabel1;
    protected javax.swing.JLabel tweetImageLabel2;
    protected javax.swing.JLabel tweetImageLabel3;
    protected javax.swing.JLabel tweetImageLabel4;
    protected javax.swing.JTable tweetsTable;
    private javax.swing.JButton viewCollectionOnTwitterButton;
    // End of variables declaration//GEN-END:variables
}
