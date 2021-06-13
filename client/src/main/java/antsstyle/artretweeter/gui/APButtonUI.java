/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package antsstyle.artretweeter.gui;

import antsstyle.artretweeter.gui.GUIConfig;
import java.awt.Graphics;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 *
 * @author Antsstyle
 */
public class APButtonUI extends BasicButtonUI {

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        super.paint(g, c);
        if (model.isPressed()) {
            b.setBackground(GUIConfig.JBUTTON_BG_COLOUR.darker());
        } else if (model.isRollover()) {
            b.setBackground(GUIConfig.JBUTTON_BG_COLOUR.brighter());
        } else {
            b.setBackground(GUIConfig.JBUTTON_BG_COLOUR);
        }

        b.setForeground(GUIConfig.JBUTTON_FONT_COLOUR);

        b.setBorder(BorderFactory.createRaisedBevelBorder());

    }

}
