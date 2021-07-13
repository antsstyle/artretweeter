/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui.configuration.listeners;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;

/**
 *
 * @author antss
 */
public class ComboBoxListener implements ItemListener {

    private final JComboBox component;
    private String initialValue;
    private final HashMap<JComponent, Object> valueMappings;
    private final ArrayList<JComponent> changedSettings;

    public void setInitialValue(String value) {
        initialValue = value;
    }

    public ComboBoxListener(JComboBox component, String value,
            HashMap<JComponent, Object> valueMappings, ArrayList<JComponent> changedSettings) {
        super();
        this.component = component;
        this.initialValue = value;
        this.valueMappings = valueMappings;
        this.changedSettings = changedSettings;
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            String selectedItem = (String) component.getSelectedItem();
            if (!selectedItem.equals((String) initialValue)) {
                if (!changedSettings.contains(component)) {
                    changedSettings.add(component);
                }
                valueMappings.put(component, selectedItem);
            } else {
                changedSettings.remove(component);
                valueMappings.put(component, selectedItem);
            }
        }
    }

}
