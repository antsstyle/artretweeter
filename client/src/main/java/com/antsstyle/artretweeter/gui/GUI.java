/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.gui.configuration.ConfigurationPrimaryPanel;
import com.antsstyle.artretweeter.gui.tweetpanels.MainManagementPanel;
import com.antsstyle.artretweeter.twitter.TwitterEndpoint;
import com.antsstyle.artretweeter.twitter.RESTAPI;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class GUI extends javax.swing.JFrame {

    private static final Logger LOGGER = LogManager.getLogger(GUI.class);

    private static GUI gui;

    /**
     * Creates new form GUI
     */
    private GUI() {
        initComponents();
    }

    public static GUI getInstance() {
        if (gui == null) {
            gui = new GUI();
        }
        return gui;
    }

    /**
     * Sets the ArtPoster image icon for a given frame, one of two images (one for live mode, one for debug mode.)
     *
     * @param frame The frame to set the icon for.
     */
    public static void setFrameImageIcon(JFrame frame) {
        SwingUtilities.invokeLater(() -> {
            frame.setIconImage(Toolkit.getDefaultToolkit().getImage(GUI.class.getResource("/MorriganTsuaii64.png")));
        });
    }

    public static void setFailedNotificationCount(JFrame frame, int numNotifications) {
        SwingUtilities.invokeLater(() -> {
            if (numNotifications == 0) {
                setFrameImageIcon(frame);
            } else if (numNotifications > 0 && numNotifications < 10) {
                String resourceString = "/MorriganTsuaii64-notiflarge".concat(String.valueOf(numNotifications)).concat(".png");
                frame.setIconImage(Toolkit.getDefaultToolkit().getImage(GUI.class.getResource(resourceString)));
            } else if (numNotifications >= 10) {
                String resourceString = "/MorriganTsuaii64-notiflarge9plus.png";
                frame.setIconImage(Toolkit.getDefaultToolkit().getImage(GUI.class.getResource(resourceString)));
            }
            getPrimaryDisplayPanel().setFailedRetweetsButtonText(numNotifications);
        });
    }

    private static final ArrayList<Container> ALL_CONTAINERS = new ArrayList<>();

    public static ArrayList<Container> getAllGUIContainers() {
        return ALL_CONTAINERS;
    }

    /**
     * Initialises the GUI and sets the appearance.
     */
    public static void preInitialisation() {
        setFrameImageIcon(getInstance());
        ALL_CONTAINERS.add(ACCOUNTS_PANEL);
        ALL_CONTAINERS.add(PRIMARY_DISPLAY_PANEL);
        ALL_CONTAINERS.add(FAILED_RETWEETS_PANEL);
        ALL_CONTAINERS.add(HELP_PANEL);
        ALL_CONTAINERS.add(CONFIGURATION_PANEL);
        ALL_CONTAINERS.add(MAIN_MANAGEMENT_PANEL);
        ACCOUNTS_PANEL.initialise();
        FAILED_RETWEETS_PANEL.initialise();
        CONFIGURATION_PANEL.initialise();
        MAIN_MANAGEMENT_PANEL.initialise();
        getInstance()
                .setGUISize(getAccountsPanel());
        getInstance()
                .switchPanels(getAccountsPanel());
        GUIHelperMethods.setAllGUIColours();
        SwingUtilities.invokeLater(() -> {
            getInstance()
                    .setContentPane(getPrimaryDisplayPanel());
        });
        getInstance().setTitle("ArtRetweeter");
    }

    /**
     * Switches the display panel the GUI is displaying to the given panel, and resizes the frame in accordance with the size of that panel.
     *
     * @param panel The panel to switch to.
     */
    public void switchPanels(JPanel panel) {
        if (SwingUtilities.isEventDispatchThread()) {
            switchPanelsOnEDT(panel);
        } else {
            SwingUtilities.invokeLater(() -> {
                switchPanelsOnEDT(panel);
            });
        }
    }

    private void switchPanelsOnEDT(JPanel panel) {
        panel.setSize(panel.getPreferredSize());
        setGUISize(panel);
        PRIMARY_DISPLAY_PANEL.getDisplayAreaPanel()
                .removeAll();
        PRIMARY_DISPLAY_PANEL.getDisplayAreaPanel()
                .add(panel);
    }

    /**
     * Sets the size of the main JFrame. Used to automatically resize ArtPoster according to the size of the panel being displayed.
     *
     * @param panelToDisplay The JPanel whose dimensions to adjust the JFrame's size with respect to.
     */
    public void setGUISize(JPanel panelToDisplay) {
        if (SwingUtilities.isEventDispatchThread()) {
            setGUISizeOnEDT(panelToDisplay);
        } else {
            SwingUtilities.invokeLater(() -> {
                setGUISizeOnEDT(panelToDisplay);
            });
        }
    }

    private void setGUISizeOnEDT(JPanel panelToDisplay) {
        Dimension frameSize = new Dimension(0, 0);
        Insets insets = this.getInsets();
        frameSize.width += insets.left;
        frameSize.width += insets.right;
        frameSize.height += insets.top;
        frameSize.height += insets.bottom;
        frameSize.height += mainMenuBar.getHeight();
        if (PRIMARY_DISPLAY_PANEL.mainMenuToolBar.isVisible()) {
            frameSize.width += PRIMARY_DISPLAY_PANEL.mainMenuToolBar.getWidth();
        }

        Dimension dimensions = panelToDisplay.getPreferredSize();
        frameSize.width += dimensions.getWidth();
        frameSize.height += dimensions.getHeight();
        this.setSize(frameSize);
        revalidate();
        repaint();
    }

    /**
     * Gets the Primary Display panel instance (the empty panel in which other panels are displayed).
     *
     * @return The Primary Display Panel JPanel instance.
     */
    public static PrimaryDisplayPanel getPrimaryDisplayPanel() {
        return PRIMARY_DISPLAY_PANEL;
    }

    public static AccountsPanel getAccountsPanel() {
        return ACCOUNTS_PANEL;
    }

    public static FailedRetweetsPanel getFailedRetweetsPanel() {
        return FAILED_RETWEETS_PANEL;
    }

    public static HelpPanel getHelpPanel() {
        return HELP_PANEL;
    }

    public static ConfigurationPrimaryPanel getConfigurationPanel() {
        return CONFIGURATION_PANEL;
    }

    public static MainManagementPanel getMainManagementPanel() {
        return MAIN_MANAGEMENT_PANEL;
    }

    private static final AccountsPanel ACCOUNTS_PANEL = new AccountsPanel();
    private static final FailedRetweetsPanel FAILED_RETWEETS_PANEL = new FailedRetweetsPanel();
    private static final HelpPanel HELP_PANEL = new HelpPanel();
    private static final ConfigurationPrimaryPanel CONFIGURATION_PANEL = new ConfigurationPrimaryPanel();
    private static final MainManagementPanel MAIN_MANAGEMENT_PANEL = new MainManagementPanel();

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainMenuBar = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        exitMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jMenu1.setText("File");

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exitMenuItem);

        mainMenuBar.add(jMenu1);

        setJMenuBar(mainMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PRIMARY_DISPLAY_PANEL, javax.swing.GroupLayout.DEFAULT_SIZE, 826, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PRIMARY_DISPLAY_PANEL, javax.swing.GroupLayout.DEFAULT_SIZE, 623, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        shutdownArtRetweeter();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void shutdownArtRetweeter() {
        int confirm = JOptionPane.showOptionDialog(this,
                "Close ArtRetweeter?",
                "Exit Confirmation", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (confirm == JOptionPane.YES_OPTION) {
            CoreDB.shutDown();
            System.exit(0);
        }
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        shutdownArtRetweeter();
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private static final com.antsstyle.artretweeter.gui.PrimaryDisplayPanel PRIMARY_DISPLAY_PANEL = new com.antsstyle.artretweeter.gui.PrimaryDisplayPanel();
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar mainMenuBar;
    // End of variables declaration//GEN-END:variables
}
