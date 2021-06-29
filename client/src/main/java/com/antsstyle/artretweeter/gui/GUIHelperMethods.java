/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.configuration.GUIConfig;
import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.ServerResponse;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.datastructures.TwitterResponse;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBResponseCode;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.serverapi.ServerAPI;
import com.antsstyle.artretweeter.tools.ImageTools;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class GUIHelperMethods {

    private static final Logger LOGGER = LogManager.getLogger(GUIHelperMethods.class);

    public static void setAllGUIColours() {
        UIManager.put("ScrollBar.shadow", GUIConfig.JBUTTON_BG_COLOUR);
        UIManager.put("ScrollBar.thumb", GUIConfig.JBUTTON_BG_COLOUR);
        UIManager.put("ScrollBar.thumbShadow", GUIConfig.JBUTTON_BG_COLOUR);
        UIManager.put("ScrollBar.thumbHighlight", GUIConfig.JBUTTON_BG_COLOUR);
        UIManager.put("ScrollBar.darkShadow", GUIConfig.JBUTTON_BG_COLOUR);
        UIManager.put("ScrollBar.highlight", GUIConfig.JBUTTON_BG_COLOUR);
        ArrayList<Container> containers = GUI.getAllGUIContainers();
        for (Container c : containers) {
            setGUIColours(c);
        }
    }

    public static void setGUIColours(Container comp) {
        if (SwingUtilities.isEventDispatchThread()) {
            setColour(comp);
            Component[] components = comp.getComponents();
            for (Component c : components) {
                if (c instanceof Container) {
                    Container p = (Container) c;
                    setGUIColours(p);
                }
                setColour(c);
            }
        } else {
            SwingUtilities.invokeLater(() -> {
                setColour(comp);
                Component[] components = comp.getComponents();
                for (Component c : components) {
                    if (c instanceof Container) {
                        Container p = (Container) c;
                        setGUIColours(p);
                    }
                    setColour(c);
                }
            });
        }

    }

    private static void setColour(Component c) {
        if (SwingUtilities.isEventDispatchThread()) {
            setComponentColour(c);
        } else {
            SwingUtilities.invokeLater(() -> {
                setComponentColour(c);
            });
        }
    }

    public static void setComponentColour(Component c) {
        if (c instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) c;
            scrollPane.setBackground(GUIConfig.JLABEL_BG_COLOUR);
            scrollPane.getVerticalScrollBar().setOpaque(true);
            scrollPane.getVerticalScrollBar().setBackground(GUIConfig.JLABEL_BG_COLOUR);
            scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    this.thumbColor = GUIConfig.JBUTTON_BG_COLOUR;
                }
            });
        } else if (c instanceof JComboBox) {
            JComboBox comboBox = (JComboBox) c;
            c.getParent().setBackground(GUIConfig.CONTAINER_BG_COLOUR);
            comboBox.setOpaque(true);
            APComboBoxUI ui = new APComboBoxUI(comboBox);
            comboBox.setUI(ui);
        } else if (c instanceof JRadioButton) {
            c.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
            c.setForeground(GUIConfig.JBUTTON_FONT_COLOUR);
        } else if (c instanceof JTable) {
            JTable table = (JTable) c;
            table.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
            table.setForeground(GUIConfig.JLABEL_FONT_COLOUR);
            JViewport parent = (JViewport) table.getParent();
            parent.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
            table.getTableHeader().setOpaque(false);
            table.getTableHeader().setBackground(GUIConfig.JBUTTON_BG_COLOUR);
            table.getTableHeader().setForeground(GUIConfig.JBUTTON_FONT_COLOUR);
        } else if (c instanceof JTabbedPane) {
            JTabbedPane tabbedPane = (JTabbedPane) c;
            int count = tabbedPane.getTabCount();
            tabbedPane.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
            tabbedPane.setForeground(GUIConfig.CONTAINER_BG_COLOUR);
            for (int i = 0; i < count; i++) {
                tabbedPane.setBackgroundAt(i, Color.BLUE);
            }
        } else if (c instanceof JPanel) {
            c.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
        } else if (c instanceof WWButton) {
            c.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
            c.setForeground(GUIConfig.JBUTTON_FONT_COLOUR);
        } else if (c instanceof JButton) {
            c.setBackground(GUIConfig.JBUTTON_BG_COLOUR);
            //c.setForeground(GUIConfig.JBUTTON_FONT_COLOUR);
            JButton button = (JButton) c;
            //button.setContentAreaFilled(false);
            button.setUI(new APButtonUI());
            button.setBorder(BorderFactory.createRaisedBevelBorder());
        } else if (c instanceof JLabel) {
            JLabel label = (JLabel) c;
            label.setBackground(GUIConfig.JLABEL_BG_COLOUR);
            label.setForeground(GUIConfig.JLABEL_FONT_COLOUR);
            label.setOpaque(true);
        } else if (c instanceof JToolBar) {
            c.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
        } else if (c instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) c;
            checkBox.setBackground(GUIConfig.JLABEL_BG_COLOUR);
            checkBox.setForeground(GUIConfig.JLABEL_FONT_COLOUR);
            checkBox.setOpaque(true);
        } else if (c instanceof JTextArea) {
            c.setBackground(GUIConfig.JLABEL_BG_COLOUR);
            c.setForeground(GUIConfig.JLABEL_FONT_COLOUR);
        } else if (c instanceof JTextField) {
            c.setBackground(GUIConfig.JLABEL_BG_COLOUR);
            c.setForeground(GUIConfig.JLABEL_FONT_COLOUR);
        } else if (c instanceof JToggleButton) {
            Color oppositeColor = new Color(255 - GUIConfig.JBUTTON_BG_COLOUR.getRed(),
                    255 - GUIConfig.JBUTTON_BG_COLOUR.getGreen(), 255 - GUIConfig.JBUTTON_BG_COLOUR.getBlue());
            if (((JToggleButton) c).isSelected()) {
                c.setBackground(oppositeColor);
            } else {
                c.setBackground(GUIConfig.JBUTTON_BG_COLOUR);
            }
            c.setForeground(GUIConfig.JBUTTON_FONT_COLOUR);
            JToggleButton button = (JToggleButton) c;
            button.setContentAreaFilled(false);
            button.setBorder(BorderFactory.createEtchedBorder());
            ActionListener oldListener;
            if (button.getActionListeners().length != 0) {
                oldListener = button.getActionListeners()[0];
                button.removeActionListener(oldListener);
            }
            button.addActionListener((ActionEvent e) -> {
                JToggleButton source = (JToggleButton) e.getSource();
                if (source.isSelected()) {
                    source.setBackground(oppositeColor);
                } else {
                    source.setBackground(GUIConfig.JBUTTON_BG_COLOUR);
                }
            });
        } else if (c instanceof JList) {
            c.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
            c.setForeground(GUIConfig.JLABEL_FONT_COLOUR);
        } else if (c instanceof JDialog) {
            c.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
            c.setForeground(GUIConfig.JLABEL_FONT_COLOUR);
        } else if (c instanceof JOptionPane) {
            c.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
            c.setForeground(GUIConfig.JLABEL_FONT_COLOUR);
        }
    }

    public static void showErrors(OperationResult result, Logger logger, String endText) {
        String error;
        if (result.getClientResponse() != null && !result.getClientResponse().wasSuccessful()) {
            error = result.getClientResponse().getStatusCode().getStatusMessage();
            if (result.getClientResponse().getExtraStatusMessage() != null) {
                error = error.concat(": ").concat(result.getClientResponse().getExtraStatusMessage());
            }
        } else if (result.getServerResponse() != null && !result.getServerResponse().wasSuccessful()) {
            error = result.getServerResponse().getStatusCode().getStatusMessage();
            if (result.getServerResponse().getExtraStatusMessage() != null) {
                error = error.concat(": ").concat(result.getServerResponse().getExtraStatusMessage());
            }
        } else if (result.getTwitterResponse() != null && !result.getTwitterResponse().wasSuccessful()) {
            error = result.getTwitterResponse().getStatusCode().getStatusMessage();
            if (result.getTwitterResponse().getExtraStatusMessage() != null) {
                error = error.concat(": ").concat(result.getTwitterResponse().getExtraStatusMessage());
            }
        } else {
            return;
        }
        String msg = "<html>Error performing operation.<br/><br/>"
                + error;
        if (endText != null) {
            msg = msg.concat("<br/><br/>").concat(endText);
            msg = msg.concat(" </html>");
        }
        JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays the images related to the selected data entry in the management table.
     *
     * @param table
     * @param panelAttributes
     * @param panes
     * @param labels
     */
    protected static void showTweetPreview(JTable table, Integer[] panelAttributes, JScrollPane[] panes, JLabel[] labels) {
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        Integer standardPanelWidth = panelAttributes[0];
        Integer standardPanelHeight = panelAttributes[1];
        Integer standardPanelMargin = panelAttributes[2];
        Integer standardPanelInset = panelAttributes[3];

        int modelRow = table.convertRowIndexToModel(row);
        TableColumnModel tcm = table.getColumnModel();
        TableModel model = table.getModel();
        int idColumnIndex = tcm.getColumnIndex("ID");
        Integer id = (Integer) model.getValueAt(modelRow, idColumnIndex);
        DBResponse resp = CoreDB.selectFromTable(DBTable.TWEETS, new String[]{"id"}, new Object[]{id});
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
        int y = panes[0].getY();
        int fullWidthAvailable = 4 * (standardPanelWidth + standardPanelMargin + standardPanelInset);
        switch (numImages) {
            case 1:
                Dimension d1 = new Dimension((int) (widthRatios.get(0) * fullWidthAvailable), standardPanelHeight);
                setAllSizes(panes[0], d1);
                setAllSizes(labels[0], d1);
                panes[1].setVisible(false);
                panes[2].setVisible(false);
                panes[3].setVisible(false);
                break;
            case 2:
                d1 = new Dimension((int) (widthRatios.get(0) * fullWidthAvailable), standardPanelHeight);
                setAllSizes(panes[0], d1);
                setAllSizes(labels[0], d1);
                x = panes[0].getX() + (int) d1.getWidth();
                Dimension d2 = new Dimension((int) (widthRatios.get(1) * fullWidthAvailable), standardPanelHeight);
                setAllSizes(panes[1], d2);
                setAllSizes(labels[1], d2);
                panes[1].setLocation(x, y);
                panes[1].setVisible(true);
                panes[2].setVisible(false);
                panes[3].setVisible(false);
                break;
            case 3:
                d1 = new Dimension((int) (widthRatios.get(0) * fullWidthAvailable), standardPanelHeight);
                setAllSizes(panes[0], d1);
                setAllSizes(labels[0], d1);
                x = panes[0].getX() + (int) d1.getWidth();
                d2 = new Dimension((int) (widthRatios.get(1) * fullWidthAvailable), standardPanelHeight);
                setAllSizes(panes[1], d2);
                setAllSizes(labels[1], d2);
                panes[1].setLocation(x, y);
                Dimension d3 = new Dimension((int) (widthRatios.get(2) * fullWidthAvailable), standardPanelHeight);
                x = panes[1].getX() + (int) d2.getWidth();
                setAllSizes(panes[2], d3);
                setAllSizes(labels[2], d3);
                panes[2].setLocation(x, y);
                panes[1].setVisible(true);
                panes[2].setVisible(true);
                panes[3].setVisible(false);
                break;
            case 4:
                d1 = new Dimension((int) (widthRatios.get(0) * fullWidthAvailable), standardPanelHeight);
                setAllSizes(panes[0], d1);
                setAllSizes(labels[0], d1);
                x = panes[0].getX() + (int) d1.getWidth();
                d2 = new Dimension((int) (widthRatios.get(1) * fullWidthAvailable), standardPanelHeight);
                setAllSizes(panes[1], d2);
                setAllSizes(labels[1], d2);
                panes[1].setLocation(x, y);
                x = panes[1].getX() + (int) d2.getWidth();
                d3 = new Dimension((int) (widthRatios.get(2) * fullWidthAvailable), standardPanelHeight);
                setAllSizes(panes[2], d3);
                setAllSizes(labels[2], d3);
                panes[2].setLocation(x, y);
                x = panes[2].getX() + (int) d3.getWidth();
                Dimension d4 = new Dimension((int) (widthRatios.get(3) * fullWidthAvailable), standardPanelHeight);
                setAllSizes(panes[3], d4);
                setAllSizes(labels[3], d4);
                panes[3].setLocation(x, y);
                panes[1].setVisible(true);
                panes[2].setVisible(true);
                panes[3].setVisible(true);
                break;
        }
        int width = panes[0].getWidth();
        int height = panes[0].getHeight();
        ImageIcon icon = new ImageIcon(ImageTools.getScaledImageForViewing(images.get(0), width, height));
        labels[0].setIcon(icon);
        if (numImages > 1) {
            width = panes[1].getWidth();
            height = panes[1].getHeight();
            icon = new ImageIcon(ImageTools.getScaledImageForViewing(images.get(1), width, height));
            labels[1].setIcon(icon);
        }
        if (numImages > 2) {
            width = panes[2].getWidth();
            height = panes[2].getHeight();
            icon = new ImageIcon(ImageTools.getScaledImageForViewing(images.get(2), width, height));
            labels[2].setIcon(icon);
        }
        if (numImages > 3) {
            width = panes[3].getWidth();
            height = panes[3].getHeight();
            icon = new ImageIcon(ImageTools.getScaledImageForViewing(images.get(3), width, height));
            labels[3].setIcon(icon);
        }
        //tweetTextArea.setText(fullTweetText);
    }

    protected static void setAllSizes(JLabel label, Dimension d) {
        label.setMinimumSize(d);
        label.setSize(d);
        label.setMaximumSize(d);
        label.setPreferredSize(d);
    }

    protected static void setAllSizes(JScrollPane pane, Dimension d) {
        pane.setMinimumSize(d);
        pane.setSize(d);
        pane.setMaximumSize(d);
        pane.setPreferredSize(d);
    }

    public static boolean queueRetweet(JTable table, Account account, boolean changeTime) {
        int row = table.getSelectedRow();
        if (row == -1) {
            return false;
        }
        int modelRow = table.convertRowIndexToModel(row);
        int idColumnIndex = table.getColumnModel().getColumnIndex("ID");
        Integer id = (Integer) table.getModel().getValueAt(modelRow, idColumnIndex);
        DBResponse selectResp = CoreDB.selectFromTable(DBTable.TWEETS, new String[]{"id"}, new Object[]{id});
        if (!selectResp.wasSuccessful()) {
            String msg = "Failed to query DB for tweet information!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (selectResp.getReturnedRows().isEmpty()) {
            String msg = "This tweet doesn't exist in DB - has the DB file been modified?";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        TweetHolder tweet = ResultSetConversion.getTweet(selectResp.getReturnedRows().get(0));
        selectResp = CoreDB.selectFromTable(DBTable.RETWEETQUEUE,
                new String[]{"tweetid", "retweetingusertwitterid"},
                new Object[]{tweet.getTweetID(), account.getTwitterID()});
        if (!selectResp.wasSuccessful()) {
            String msg = "Failed to query DB for retweet queue information!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!selectResp.getReturnedRows().isEmpty() && !changeTime) {
            String msg = "This tweet is already queued for retweeting.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        DBResponse accountsResp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!accountsResp.wasSuccessful()) {
            String msg = "Failed to query DB for accounts information!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return false;
        } else if (accountsResp.getReturnedRows().isEmpty()) {
            String msg = "You cannot queue a retweet without an account. Add one first.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return false;
        }
        boolean isAuthorised = false;
        ArrayList<HashMap<String, Object>> rows = accountsResp.getReturnedRows();
        for (HashMap<String, Object> dbRow : rows) {
            Account acc = ResultSetConversion.getAccount(dbRow);
            if (acc.getTwitterID().equals(tweet.getUserTwitterID())) {
                isAuthorised = true;
                break;
            }
        }
        if (!isAuthorised) {
            String msg = "ArtRetweeter will only queue retweets for tweets from accounts you own.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return false;
        }
        ChooseDatePanel datePanel = new ChooseDatePanel();
        int selectionResult = JOptionPane.showConfirmDialog(GUI.getInstance(), datePanel, "Select Retweet Date", JOptionPane.OK_CANCEL_OPTION);
        if (selectionResult != JOptionPane.OK_OPTION) {
            return false;
        }
        Timestamp time = datePanel.getSelectedTime();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        if (cal.getTimeInMillis() > time.getTime()) {
            String msg = "You must choose a date at least one hour from the current time.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return false;
        }
        OperationResult opResult = ServerAPI.queueRetweet(account, tweet.getTweetID(), time);
        if (!opResult.wasSuccessful()) {
            showErrors(opResult, LOGGER, null);
            return false;
        }
        Boolean success = (Boolean) opResult.getServerResponse().getReturnedObject();
        if (success) {
            DBResponse updateResp;
            if (changeTime) {
                if (!CoreDB.insertRetweetQueueEntry(new Object[]{tweet.getTweetID(), account.getTwitterID(), time})) {
                    String msg = "<html>Time changed successfully, but an error occurred updating this queue entry "
                            + "<br/>in the ArtRetweeter client.</html>";
                    JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.error(msg);
                    return false;
                }

                int rowCount = QueuingPanel.queuedTweetsTable.getRowCount();
                Integer queueTableIDColumnIndex = QueuingPanel.queuedTweetsTable.getColumnModel().getColumnIndex("ID");
                Integer queueTableRTTimeColumnIndex = QueuingPanel.queuedTweetsTable.getColumnModel().getColumnIndex("Retweet Time");
                for (int i = 0; i < rowCount; i++) {
                    Integer tableID = (Integer) QueuingPanel.queuedTweetsTable.getModel().getValueAt(i, queueTableIDColumnIndex);
                    if (tableID.equals(tweet.getId())) {
                        QueuingPanel.queuedTweetsTable.getModel().setValueAt(time, i, queueTableRTTimeColumnIndex);
                        break;
                    }
                }

                return true;
            } else {
                updateResp = CoreDB.insertIntoTable(DBTable.RETWEETQUEUE,
                        new String[]{"tweetid", "retweetingusertwitterid", "retweettime"},
                        new Object[]{tweet.getTweetID(), account.getTwitterID(), time});
                if (updateResp.wasSuccessful()) {
                    DefaultTableModel dtm = (DefaultTableModel) QueuingPanel.queuedTweetsTable.getModel();
                    if (changeTime) {
                        int rowCount = QueuingPanel.queuedTweetsTable.getRowCount();
                        Integer queueTableIDColumnIndex = QueuingPanel.queuedTweetsTable.getColumnModel().getColumnIndex("ID");
                        Integer queueTableRTTimeColumnIndex = QueuingPanel.queuedTweetsTable.getColumnModel().getColumnIndex("Retweet Time");
                        for (int i = 0; i < rowCount; i++) {
                            Integer tableID = (Integer) QueuingPanel.queuedTweetsTable.getModel().getValueAt(i, queueTableIDColumnIndex);
                            if (tableID.equals(tweet.getId())) {
                                QueuingPanel.queuedTweetsTable.getModel().setValueAt(time, i, queueTableRTTimeColumnIndex);
                                break;
                            }
                        }
                    } else {
                        dtm.addRow(new Object[]{tweet.getId(), tweet.getFullTweetText(), time});
                    }
                    return true;
                } else if (updateResp.getStatusCode().equals(DBResponseCode.DUPLICATE_ERROR)) {
                    String msg = "<html>This tweet is already queued for retweeting.</html>";
                    JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.error(msg);
                    return false;
                } else {
                    String msg = "<html>Queued successfully, but an error occurred adding this queue entry "
                            + "<br/>to the ArtRetweeter client.</html>";
                    JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
                    LOGGER.error(msg);
                    return false;
                }
            }

        } else {
            String msg = "<html>ArtRetweeter server returned an error, check log output.</html>";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return false;
        }
    }

}
