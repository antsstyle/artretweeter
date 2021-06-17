/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import com.antsstyle.artretweeter.configuration.GUIConfig;
import java.awt.Color;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 *
 * @author Antsstyle
 */
public class APComboBoxUI extends BasicComboBoxUI {

    private JComboBox box;

    public APComboBoxUI(JComboBox box) {
        super();
        this.box = box;
        box.setOpaque(true);
        box.setBackground(GUIConfig.CONTAINER_BG_COLOUR);
        box.setForeground(GUIConfig.JLABEL_FONT_COLOUR);
    }

    protected ComboBoxEditor createEditor() {
        ComboBoxEditor editor = super.createEditor();
        editor.getEditorComponent().setBackground(GUIConfig.CONTAINER_BG_COLOUR);
        editor.getEditorComponent().setForeground(GUIConfig.JLABEL_FONT_COLOUR);
        return editor;
    }

    @Override
    protected JButton createArrowButton() {
        return new BasicArrowButton(
                BasicArrowButton.SOUTH,
                GUIConfig.JBUTTON_BG_COLOUR, GUIConfig.JLABEL_BG_COLOUR,
                GUIConfig.JLABEL_BG_COLOUR, GUIConfig.JLABEL_BG_COLOUR);
    }

}
