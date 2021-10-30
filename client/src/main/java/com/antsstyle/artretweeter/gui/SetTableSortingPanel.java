/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.configuration.TwitterConfig;
import com.antsstyle.artretweeter.datastructures.CachedVariable;
import com.antsstyle.artretweeter.db.CachedVariableDB;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class SetTableSortingPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = LogManager.getLogger(SetTableSortingPanel.class);

    private static final List<String> metricsEnabledOptions
            = Arrays.asList(new String[]{"ID", "Tweet Text", "Date Posted", "Retweets", "Likes", "RT#", "Pending RT"});

    private static final List<String> metricsDisabledOptions
            = Arrays.asList(new String[]{"ID", "Tweet Text", "Date Posted", "RT#", "Pending RT"});

    /**
     * Creates new form SetTableSortingPanel
     */
    public SetTableSortingPanel() {
        initComponents();
        setSortOptions();
    }

    public final void setSortOptions() {
        firstColumnSortComboBox.removeAllItems();
        secondColumnSortComboBox.removeAllItems();
        thirdColumnSortComboBox.removeAllItems();
        if (TwitterConfig.DO_NOT_SHOW_METRICS_ANYWHERE) {
            ((DefaultComboBoxModel) firstColumnSortComboBox.getModel()).addAll(metricsDisabledOptions);
            ((DefaultComboBoxModel) secondColumnSortComboBox.getModel()).addAll(metricsDisabledOptions);
            ((DefaultComboBoxModel) thirdColumnSortComboBox.getModel()).addAll(metricsDisabledOptions);
        } else {
            ((DefaultComboBoxModel) firstColumnSortComboBox.getModel()).addAll(metricsEnabledOptions);
            ((DefaultComboBoxModel) secondColumnSortComboBox.getModel()).addAll(metricsEnabledOptions);
            ((DefaultComboBoxModel) thirdColumnSortComboBox.getModel()).addAll(metricsEnabledOptions);
        }
        setComboBoxSettings();
    }

    public Pair<String[], String[]> getUserSettings() {
        String[] items = new String[3];
        String[] itemOrders = new String[3];
        items[0] = (String) firstColumnSortComboBox.getSelectedItem();
        itemOrders[0] = (String) firstColumnSortOrderComboBox.getSelectedItem();
        items[1] = (String) secondColumnSortComboBox.getSelectedItem();
        itemOrders[1] = (String) secondColumnSortOrderComboBox.getSelectedItem();
        items[2] = (String) thirdColumnSortComboBox.getSelectedItem();
        itemOrders[2] = (String) thirdColumnSortOrderComboBox.getSelectedItem();
        return Pair.of(items, itemOrders);
    }

    public void setComboBoxSettings() {
        CachedVariable managementTweetTableSorting = CachedVariableDB.getCachedVariableByName("artretweeter.managementtweettablesorting");
        if (managementTweetTableSorting != null) {
            String[] items = StringUtils.split(managementTweetTableSorting.getValue(), ";");
            if (TwitterConfig.DO_NOT_SHOW_METRICS_ANYWHERE) {
                if (items[0].equals("Retweets") || items[0].equals("Likes")) {
                    firstColumnSortComboBox.setSelectedItem("Date Posted");
                } else {
                    firstColumnSortComboBox.setSelectedItem(items[0]);
                }
                if (items[2].equals("Retweets") || items[2].equals("Likes")) {
                    secondColumnSortComboBox.setSelectedItem("Pending RT");
                } else {
                    secondColumnSortComboBox.setSelectedItem(items[2]);
                }
                if (items[4].equals("Retweets") || items[4].equals("Likes")) {
                    thirdColumnSortComboBox.setSelectedItem("ID");
                } else {
                    thirdColumnSortComboBox.setSelectedItem(items[4]);
                }
            } else {
                firstColumnSortComboBox.setSelectedItem(items[0]);
                secondColumnSortComboBox.setSelectedItem(items[2]);
                thirdColumnSortComboBox.setSelectedItem(items[4]);
            }
            firstColumnSortOrderComboBox.setSelectedItem(items[1]);
            secondColumnSortOrderComboBox.setSelectedItem(items[3]);
            thirdColumnSortOrderComboBox.setSelectedItem(items[5]);
        } else {
            firstColumnSortComboBox.setSelectedItem("Date Posted");
            secondColumnSortComboBox.setSelectedItem("RT#");
            thirdColumnSortComboBox.setSelectedItem("Pending RT");
            firstColumnSortOrderComboBox.setSelectedItem("Descending");
            secondColumnSortOrderComboBox.setSelectedItem("Ascending");
            thirdColumnSortOrderComboBox.setSelectedItem("Descending");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        firstColumnSortOrderComboBox = new javax.swing.JComboBox<>();
        thirdColumnSortOrderComboBox = new javax.swing.JComboBox<>();
        firstColumnSortComboBox = new javax.swing.JComboBox<>();
        secondColumnSortOrderComboBox = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        thirdColumnSortComboBox = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        secondColumnSortComboBox = new javax.swing.JComboBox<>();

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setText("Sort tweets table by:");

        jLabel5.setText("<html>Tip: if you want to sort by one column, you can also click on the table header for that column (clicking it again will reverse the ordering).</html>");

        firstColumnSortOrderComboBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        firstColumnSortOrderComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Ascending", "Descending" }));
        firstColumnSortOrderComboBox.setMaximumSize(new java.awt.Dimension(109, 25));
        firstColumnSortOrderComboBox.setMinimumSize(new java.awt.Dimension(109, 25));
        firstColumnSortOrderComboBox.setPreferredSize(new java.awt.Dimension(109, 25));

        thirdColumnSortOrderComboBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        thirdColumnSortOrderComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Ascending", "Descending" }));
        thirdColumnSortOrderComboBox.setMaximumSize(new java.awt.Dimension(109, 25));
        thirdColumnSortOrderComboBox.setMinimumSize(new java.awt.Dimension(109, 25));
        thirdColumnSortOrderComboBox.setPreferredSize(new java.awt.Dimension(109, 25));

        firstColumnSortComboBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        firstColumnSortComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ID", "Tweet Text", "Date Posted", "Retweets", "Likes", "RT#", "Pending RT" }));
        firstColumnSortComboBox.setLightWeightPopupEnabled(false);
        firstColumnSortComboBox.setMaximumSize(new java.awt.Dimension(116, 25));
        firstColumnSortComboBox.setMinimumSize(new java.awt.Dimension(116, 25));
        firstColumnSortComboBox.setPreferredSize(new java.awt.Dimension(116, 25));

        secondColumnSortOrderComboBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        secondColumnSortOrderComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Ascending", "Descending" }));
        secondColumnSortOrderComboBox.setMaximumSize(new java.awt.Dimension(109, 25));
        secondColumnSortOrderComboBox.setMinimumSize(new java.awt.Dimension(109, 25));
        secondColumnSortOrderComboBox.setPreferredSize(new java.awt.Dimension(109, 25));

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel2.setText("First column:");
        jLabel2.setMaximumSize(new java.awt.Dimension(100, 25));
        jLabel2.setMinimumSize(new java.awt.Dimension(100, 25));
        jLabel2.setPreferredSize(new java.awt.Dimension(100, 25));

        thirdColumnSortComboBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        thirdColumnSortComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "ID", "Tweet Text", "Date Posted", "Retweets", "Likes", "RT#", "Pending RT" }));
        thirdColumnSortComboBox.setLightWeightPopupEnabled(false);
        thirdColumnSortComboBox.setMaximumSize(new java.awt.Dimension(116, 25));
        thirdColumnSortComboBox.setMinimumSize(new java.awt.Dimension(116, 25));
        thirdColumnSortComboBox.setPreferredSize(new java.awt.Dimension(116, 25));

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel3.setText("Second column:");
        jLabel3.setMaximumSize(new java.awt.Dimension(100, 25));
        jLabel3.setMinimumSize(new java.awt.Dimension(100, 25));
        jLabel3.setPreferredSize(new java.awt.Dimension(100, 25));

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel4.setText("Third column:");
        jLabel4.setToolTipText("");
        jLabel4.setMaximumSize(new java.awt.Dimension(100, 25));
        jLabel4.setMinimumSize(new java.awt.Dimension(100, 25));
        jLabel4.setPreferredSize(new java.awt.Dimension(100, 25));

        secondColumnSortComboBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        secondColumnSortComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "ID", "Tweet Text", "Date Posted", "Retweets", "Likes", "RT#", "Pending RT" }));
        secondColumnSortComboBox.setLightWeightPopupEnabled(false);
        secondColumnSortComboBox.setMaximumSize(new java.awt.Dimension(116, 25));
        secondColumnSortComboBox.setMinimumSize(new java.awt.Dimension(116, 25));
        secondColumnSortComboBox.setPreferredSize(new java.awt.Dimension(116, 25));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(secondColumnSortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(secondColumnSortOrderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(thirdColumnSortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(thirdColumnSortOrderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(firstColumnSortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(firstColumnSortOrderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(firstColumnSortOrderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(firstColumnSortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(secondColumnSortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(secondColumnSortOrderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(thirdColumnSortComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(thirdColumnSortOrderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 322, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 309, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(12, 12, 12)
                .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 53, Short.MAX_VALUE)
                .addGap(12, 12, 12))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> firstColumnSortComboBox;
    private javax.swing.JComboBox<String> firstColumnSortOrderComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JComboBox<String> secondColumnSortComboBox;
    private javax.swing.JComboBox<String> secondColumnSortOrderComboBox;
    private javax.swing.JComboBox<String> thirdColumnSortComboBox;
    private javax.swing.JComboBox<String> thirdColumnSortOrderComboBox;
    // End of variables declaration//GEN-END:variables
}
