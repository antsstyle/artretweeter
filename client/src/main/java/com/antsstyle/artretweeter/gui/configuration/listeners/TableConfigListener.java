/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui.configuration.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class TableConfigListener implements TableModelListener {

    private static final Logger LOGGER = LogManager.getLogger(TableConfigListener.class);

    private final JTable table;
    private final ArrayList<JComponent> changedSettings;

    public TableConfigListener(JTable component, ArrayList<JComponent> changedSettings) {
        super();
        this.table = component;
        this.changedSettings = changedSettings;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (!changedSettings.contains(table)) {
            changedSettings.add(table);
        }
    }

}
