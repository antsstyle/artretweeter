/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui.configuration.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

/**
 *
 * @author antss
 */
public class BooleanConfigListener implements ActionListener {

    private final JCheckBox component;
    private Boolean initialValue;
    private final HashMap<JComponent, Object> valueMappings;
    private final ArrayList<JComponent> changedSettings;

    public void setInitialValue(Boolean value) {
        initialValue = value;
    }

    public BooleanConfigListener(JCheckBox component, Boolean value,
            HashMap<JComponent, Object> valueMappings, ArrayList<JComponent> changedSettings) {
        super();
        this.component = component;
        this.initialValue = value;
        this.valueMappings = valueMappings;
        this.changedSettings = changedSettings;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        boolean selected = component.isSelected();
        if (selected != initialValue) {
            if (!changedSettings.contains(component)) {
                changedSettings.add(component);
            }
            valueMappings.put(component, selected);
        } else {
            changedSettings.remove(component);
            valueMappings.put(component, selected);
        }

    }

}
