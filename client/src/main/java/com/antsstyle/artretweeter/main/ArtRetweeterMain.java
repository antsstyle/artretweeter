/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.main;

import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.queues.StatusRefreshQueue;
import java.io.FileReader;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class ArtRetweeterMain {

    private static final Logger LOGGER = LogManager.getLogger(ArtRetweeterMain.class);

    public static Properties prop = new Properties();

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            LOGGER.warn("Could not initialise UI look and feel - reverting to default look and feel.");
        }
        try (FileReader reader = new FileReader("artretweeter.properties")) {
            prop.load(reader);
        } catch (Exception e) {
            LOGGER.error("Failed to load properties file!", e);
            String msg = "<html>ArtRetweeter was unable to find the properties file. Check that 'artretweeter.properties' exists in the same"
                    + " directory as the JAR file.</html>";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        CoreDB.initialise();
        if (!CoreDB.doesDBExist()) {
            CoreDB.initialiseTables();
        }
        GUI.preInitialisation();
        java.awt.EventQueue.invokeLater(() -> {
            GUI.getInstance()
                    .setVisible(true);
        });
        StatusRefreshQueue queue = new StatusRefreshQueue();
        queue.run();
    }

}
