/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui.configuration;

import com.antsstyle.artretweeter.gui.configuration.listeners.BooleanConfigListener;
import com.antsstyle.artretweeter.gui.configuration.listeners.ComboBoxListener;
import com.antsstyle.artretweeter.gui.configuration.listeners.TextConfigListener;
import com.antsstyle.artretweeter.configuration.Config;
import com.antsstyle.artretweeter.configuration.ConfigurationModule;
import com.antsstyle.artretweeter.gui.configuration.listeners.DateConfigListener;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.tools.SwingTools;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public abstract class ConfigurationSubPanel extends JPanel {

    private static final Logger LOGGER = LogManager.getLogger(ConfigurationSubPanel.class);

    protected static final Map<String, Integer> VALID_PUBLISH_DAY_INPUTS = Map.of("Monday", 1, "Tuesday", 2, "Wednesday",
            3, "Thursday", 4, "Friday", 5, "Saturday", 6, "Sunday", 7, "All", 8);

    protected final HashMap<JComponent, String> nameMappings = new HashMap<>();
    protected final HashMap<JComponent, Object> valueMappings = new HashMap<>();
    protected final ArrayList<JComponent> changedSettings = new ArrayList<>();
    protected final ConfigurationModule module;

    public ConfigurationSubPanel(ConfigurationModule module) {
        this.module = module;
    }

    protected abstract void addNameMappings();

    protected void addValueMappings() {
        for (JComponent c : nameMappings.keySet()) {
            String configName = nameMappings.get(c);
            Field f = Config.findValueField(configName);
            if (f != null) {
                try {
                    valueMappings.put(c, f.get(null));
                    if (c instanceof ConfigDateJTextField) {
                        ConfigDateJTextField field = (ConfigDateJTextField) c;
                        String initialValue = (String) f.get(null);
                        field.setText(initialValue);
                        KeyListener[] keys = field.getKeyListeners();
                        for (KeyListener k : keys) {
                            field.removeKeyListener(k);
                        }
                        field.addKeyListener(new DateConfigListener(field, initialValue, valueMappings, changedSettings));
                    } else if (c instanceof JTextField) {
                        JTextField field = (JTextField) c;
                        String initialValue = String.valueOf(f.get(null));
                        field.setText(initialValue);
                        KeyListener[] keys = field.getKeyListeners();
                        for (KeyListener k : keys) {
                            field.removeKeyListener(k);
                        }
                        field.addKeyListener(new TextConfigListener(field, initialValue, valueMappings, changedSettings));
                    } else if (c instanceof JCheckBox) {
                        JCheckBox checkBox = ((JCheckBox) c);
                        Boolean initialValue = (Boolean) f.get(null);
                        checkBox.setSelected(initialValue);
                        ActionListener[] keys = checkBox.getActionListeners();
                        for (ActionListener k : keys) {
                            checkBox.removeActionListener(k);
                        }
                        checkBox.addActionListener(new BooleanConfigListener(checkBox, initialValue, valueMappings, changedSettings));
                    } else if (c instanceof JComboBox) {
                        JComboBox comboBox = (JComboBox) c;
                        String initialValue = (String) f.get(null);
                        comboBox.setSelectedItem(initialValue);
                        ItemListener[] keys = comboBox.getItemListeners();
                        for (ItemListener k : keys) {
                            comboBox.removeItemListener(k);
                        }
                        comboBox.addItemListener(new ComboBoxListener(comboBox, initialValue, valueMappings, changedSettings));
                    } else if (c instanceof JTable) {
                        JTable table = (JTable) c;
                        initialiseTable(table, (String) f.get(null));
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not put field to value mapping into ValueMappings", e);
                }
            } else {
                LOGGER.warn("Couldn't find field in DefaultConfig to match name mapping. Config name: " + configName);
            }
        }
    }

    public void commitChanges() {
        HashMap<String, Object> changeMap = new HashMap<>();
        ArrayList<JComponent> componentsToRemove = new ArrayList<>();
        for (JComponent component : changedSettings) {
            if (component instanceof JTable) {
                continue;
            }
            String name = nameMappings.get(component);
            Object value = valueMappings.get(component);
            changeMap.put(name, value);
        }
        for (JComponent component : changedSettings) {
            if (component instanceof JTable) {
                JTable table = (JTable) component;
                commitTableChanges(table, nameMappings.get(component));
                componentsToRemove.add(table);
            }
        }
        for (JComponent c : componentsToRemove) {
            changedSettings.remove(c);
        }
        if (!changeMap.isEmpty() && module != null) {
            Boolean successful = Config.validateAndSaveAll(changeMap, module);
            if (successful) {
                changedSettings.clear();
            }
        }

    }

    public void initialisePanel() {
        addNameMappings();
        addValueMappings();
    }

    protected void initialiseTable(JTable table, String tableContents) {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0);
        String[] rows = StringUtils.split(tableContents, ";");
        for (String row : rows) {
            String[] columns = StringUtils.split(row, "#");
            dtm.addRow(columns);
        }
    }

    protected void commitTableChanges(JTable table, String configName) {
        LOGGER.debug(table.getRowCount() + "   " + configName);
        DBTable configTable = DBTable.CONFIGURATION;
        TableModel tm = table.getModel();
        int rowCount = table.getRowCount();
        int columnCount = table.getColumnCount();
        StringBuilder tableContentsString = new StringBuilder();
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                tableContentsString = tableContentsString.append((String) tm.getValueAt(i, j)).append("#");
            }
            tableContentsString.setLength(tableContentsString.length() - 1);
            tableContentsString.append(";");
        }
        if (tableContentsString.length() > 0) {
            tableContentsString.setLength(tableContentsString.length() - 1);
            String totalString = tableContentsString.toString();
            LOGGER.debug(totalString);
            if (!totalString.trim().equals("")) {
                CoreDB.updateTable(configTable,
                        new String[]{"value"},
                        new Object[]{totalString},
                        new String[]{"name"},
                        new Object[]{configName});
            }
        }
    }

    protected void addPublishDay(JTable table, JComboBox comboBox) {
        String publishDayToAdd = (String) comboBox.getSelectedItem();
        if (publishDayToAdd.equals("All") && table.getRowCount() != 0) {
            JOptionPane.showMessageDialog(GUI.getInstance(), "You can only use 'All' with no other days in the table.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ArrayList<String> publishDays = new ArrayList<>();
        int rowCount = table.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            publishDays.add((String) table.getModel().getValueAt(i, 0));
        }
        if (publishDays.contains("All")) {
            JOptionPane.showMessageDialog(GUI.getInstance(), "You cannot add any other days when 'All' is in the table.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!publishDays.contains(publishDayToAdd)) {
            int newIndex = VALID_PUBLISH_DAY_INPUTS.get(publishDayToAdd);
            TableModel tm = table.getModel();
            DefaultTableModel dtm = (DefaultTableModel) tm;
            int i;
            for (i = 0; i < rowCount; i++) {
                int dayIndex = VALID_PUBLISH_DAY_INPUTS.get(((String) tm.getValueAt(i, 0)));
                if (dayIndex > newIndex) {
                    break;
                }
            }
            dtm.insertRow(i, new Object[]{publishDayToAdd});
        } else {
            JOptionPane.showMessageDialog(GUI.getInstance(), "You cannot add the same day twice.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    protected void removeRowFromConfigTable(JTable table) {
        if (table.getRowCount() > 0) {
            SwingTools.removeSelectedRowAndShowNext(table);
        } else {
            String msg = "You cannot remove the last entry from this table. If you want to replace it, add the new one first, then remove"
                    + " this one.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
        if (!changedSettings.contains(table)) {
            changedSettings.add(table);
        }
    }

}
