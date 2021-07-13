/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui.configuration.listeners;

import com.antsstyle.artretweeter.gui.configuration.ConfigDateJTextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JComponent;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author antss
 */
public class DateConfigListener implements KeyListener {

    private final ConfigDateJTextField component;
    private String initialValue;
    private final HashMap<JComponent, Object> valueMappings;
    private final ArrayList<JComponent> changedSettings;

    public void setInitialValue(String value) {
        initialValue = value;
    }

    public DateConfigListener(ConfigDateJTextField component, String value,
            HashMap<JComponent, Object> valueMappings, ArrayList<JComponent> changedSettings) {
        super();
        this.component = component;
        this.initialValue = value;
        this.valueMappings = valueMappings;
        this.changedSettings = changedSettings;
    }

    @Override
    public void keyPressed(KeyEvent ke) {

    }

    @Override
    public void keyReleased(KeyEvent ke) {
        String newValue = component.getText();
        if (!newValue.equals((String) initialValue)) {
            if (!changedSettings.contains(component)) {
                changedSettings.add(component);
            }
            newValue = StringUtils.replace(newValue, ":", "");
            try {
                Integer.parseInt(newValue);
                valueMappings.put(component, newValue);
            } catch (Exception e) {

            }
        } else {
            changedSettings.remove(component);
            newValue = StringUtils.replace(newValue, ":", "");
            try {
                Integer.parseInt(newValue);
                valueMappings.put(component, newValue);
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void keyTyped(KeyEvent ke) {

    }

}
