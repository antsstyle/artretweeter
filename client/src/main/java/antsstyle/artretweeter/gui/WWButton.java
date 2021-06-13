/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package antsstyle.artretweeter.gui;

import javax.swing.JButton;
import javax.swing.JToolTip;

/**
 * Simple subclass of JButton that has a word-wrapped tooltip instead of the normal one.
 * 
 * @author Ant
 * @since 1.0
 */
// A button that has a word-wrapped tooltip.
public class WWButton extends JButton {

    /**
     * Default constructor.
     */
    public WWButton() {
        super();
    }

    /**
     * 
     * @return A new WWToolTip with its width set to the WordWrapToolTip.MAX_TOOLTIP_WIDTH amount.
     */
    @Override
    public JToolTip createToolTip() {
        WordWrapToolTip toolTip = new WordWrapToolTip();
        toolTip.setFixedWidth(WordWrapToolTip.MAX_TOOLTIP_WIDTH);
        return toolTip;
    }

}
