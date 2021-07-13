/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui.configuration.listeners;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 *
 * @author antss
 */
public class TextConfigListener implements KeyListener {

    private final JTextField component;
    private String initialValue;
    private final HashMap<JComponent, Object> valueMappings;
    private final ArrayList<JComponent> changedSettings;

    public void setInitialValue(String value) {
        initialValue = value;
    }

    public TextConfigListener(JTextField component, String value,
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
            valueMappings.put(component, newValue);
        } else {
            changedSettings.remove(component);
            valueMappings.put(component, newValue);
        }
    }

    @Override
    public void keyTyped(KeyEvent ke) {

    }

}
