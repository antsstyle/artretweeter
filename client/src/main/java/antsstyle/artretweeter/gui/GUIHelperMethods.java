/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package antsstyle.artretweeter.gui;

import antsstyle.artretweeter.datastructures.OperationResult;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
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

    public static void showErrorMessage(OperationResult result, Logger logger, String endText) {
        String error = result.getLogMessage();
        String msg = "<html>Error performing operation. Error was:<br/><br/>"
                + error;
        if (endText != null) {
            msg = msg.concat("<br/><br/>").concat(endText);
        msg = msg.concat(" </html>");
        }
        JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
        logger.error(msg);
    }

}
