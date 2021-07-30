/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.tools;

import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.gui.GUIHelperMethods;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Ant
 */
public class SwingTools {

    private static final Logger LOGGER = LogManager.getLogger(SwingTools.class);

    public static void setIgnoreKeyStroke(JComponent component, int keyEvent, int modifiers, String actionName) {
        Action doNothing = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //do nothing
            }
        };
        component.getInputMap().put(KeyStroke.getKeyStroke(keyEvent, modifiers),
                actionName);
        component.getActionMap().put(actionName,
                doNothing);
    }

    /**
     * Creates a JOptionPane dialog and displays it to the user with a series of labels and text fields, using the inputNames parameter.
     *
     * @param title The title for the dialog.
     * @param message The message to display before the input items.
     * @param inputNames The label text for each input field you want the user to submit.
     * @return A list of objects containing the values the user submitted for each field, or null if the user didn't press OK to close the dialog or no input names were
     * provided/the array was null.
     */
    public static ArrayList<Object> askForUserInput(String title, String message, String... inputNames) {
        if (inputNames == null || inputNames.length == 0) {
            LOGGER.error("No input names provided, cannot ask for user input.");
            return null;
        }
        JComponent[] components = new JComponent[inputNames.length];
        for (int i = 0; i < inputNames.length; i++) {
            JTextField field = new JTextField();
            field.setFont(new java.awt.Font("Tahoma", 1, 12));
            field.setText("");
            components[i] = field;
        }
        return askForUserInput(title, message, inputNames, components);
    }

    /**
     * Creates a JOptionPane dialog and displays it to the user with a series of labels and JComponents, using the inputNames parameter.
     *
     * @param title The title for the dialog.
     * @param message The message to display in the dialog box, above the input items.
     * @param inputNames The label text for each input field you want the user to submit.
     * @param inputComponents The JComponents to be used for retrieving each user input.
     * @return A list of objects containing the values the user submitted for each field, or null if the user didn't press OK to close the dialog or no input names were
     * provided/the array was null.
     */
    public static ArrayList<Object> askForUserInput(String title, String message, String[] inputNames, JComponent[] inputComponents) {
        if (inputNames == null || inputNames.length == 0 || (inputNames.length != inputComponents.length)) {
            JOptionPane.showMessageDialog(GUI.getInstance(),
                    "Internal error - requested form inputs not valid.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel messagePanel = new JPanel(new BorderLayout());
        JLabel messageLabel = new JLabel(message);
        JLabel emptyLabel = new JLabel();
        Dimension emptyLabelSize = new Dimension(10, 10);
        emptyLabel.setMinimumSize(emptyLabelSize);
        emptyLabel.setMaximumSize(emptyLabelSize);
        emptyLabel.setPreferredSize(emptyLabelSize);
        messagePanel.add(messageLabel, BorderLayout.NORTH);
        messagePanel.add(emptyLabel, BorderLayout.CENTER);
        messageLabel.setFont(new java.awt.Font("Tahoma", 1, 14));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel dialogPanel = new JPanel(new GridLayout(0, 2));
        mainPanel.add(messagePanel, BorderLayout.NORTH);
        mainPanel.add(dialogPanel, BorderLayout.CENTER);
        ArrayList<JComponent> components = new ArrayList<>();
        for (int i = 0; i < inputNames.length; i++) {
            String input = inputNames[i];
            JLabel label = new JLabel();
            label.setFont(new java.awt.Font("Tahoma", 1, 12));
            label.setText(input);
            dialogPanel.add(label);
            components.add(inputComponents[i]);
            dialogPanel.add(inputComponents[i]);
        }
        GUIHelperMethods.setGUIColours(mainPanel);
        int res = JOptionPane
                .showConfirmDialog(GUI.getInstance(), mainPanel, title, JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            ArrayList<Object> submittedInputs = new ArrayList<>();
            for (JComponent component : components) {
                if (component instanceof JTextField) {
                    submittedInputs.add(((JTextField) component).getText()
                            .trim());
                } else if (component instanceof JCheckBox) {
                    submittedInputs.add(((JCheckBox) component).isSelected());
                } else if (component instanceof JComboBox) {
                    submittedInputs.add(((JComboBox) component).getSelectedItem());
                } else {
                    LOGGER.error("Unsupported JComponent for this function.");
                    return null;
                }
            }
            return submittedInputs;
        }
        return null;
    }

    /**
     * Removes a row from a table and selects the next one (or the previous one, if the current row was the last entry in the table).
     *
     * @param table The table to remove the row and select the next one for.
     */
    public static void removeSelectedRowAndShowNext(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        selectNextRow(table);
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.removeRow(modelRow);
    }

    /**
     * Select the next row in a table (or the previous one if we're at the end of the table).
     *
     * @param table The table to select the next row in.
     */
    public static void selectNextRow(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        int rowCount = table.getRowCount();
        if (row == (rowCount - 1) && rowCount > 1) {
            table.setRowSelectionInterval(row - 1, row - 1);
        } else if (rowCount > 1) {
            table.setRowSelectionInterval(row + 1, row + 1);
        }
    }

}
