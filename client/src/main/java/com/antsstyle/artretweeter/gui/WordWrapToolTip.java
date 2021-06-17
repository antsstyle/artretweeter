/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalToolTipUI;

/**
 * Subclass of JToolTip. Adapted from http://www.java2s.com/Code/Java/Swing-JFC/MultiLineToolTipExample.htm .
 * 
 * @author Ant
 * @since 1.0
 */
public class WordWrapToolTip extends JToolTip {
    
    /**
     * The maximum width of tooltip text before it wraps to the next line.
     */
    protected static final int MAX_TOOLTIP_WIDTH = 400;

    /**
     * Default constructor.
     */
    public WordWrapToolTip() {
        updateUI();
    }

    /**
     * Sets the UI for this ToolTip to be the multi-line version.
     */
    @Override
    public final void updateUI() {
        setUI(MultiLineToolTipUI.createUI(this));
    }

    /**
     *
     * @param columns The number of columns to display text on.
     */
    public void setColumns(int columns) {
        this.columns = columns;
        this.fixedwidth = 0;
    }

    /**
     *
     * @return The number of columns to display text on.
     */
    public int getColumns() {
        return columns;
    }

    /**
     *
     * @param width The width of this WordWrapToolTip.
     */
    public void setFixedWidth(int width) {
        this.fixedwidth = width;
        this.columns = 0;
    }

    /**
     *
     * @return The width of this WordWrapToolTip.
     */
    public int getFixedWidth() {
        return fixedwidth;
    }

    /**
     *
     */
    protected int columns = 0;

    /**
     *
     */
    protected int fixedwidth = 0;

}

class MultiLineToolTipUI extends MetalToolTipUI {

    private Font smallFont;
    private JToolTip tip;
    protected CellRendererPane rendererPane;

    private JTextArea textArea;

    public static ComponentUI createUI(JComponent c) {
        return new MultiLineToolTipUI();
    }

    public MultiLineToolTipUI() {
        super();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        tip = (JToolTip) c;
        rendererPane = new CellRendererPane();
        c.add(rendererPane);
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        c.remove(rendererPane);
        rendererPane = null;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Dimension size = c.getSize();
        textArea.setBackground(c.getBackground());
        rendererPane.paintComponent(g, textArea, c, 1, 1, size.width - 1, size.height - 1, true);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        String tipText = ((JToolTip) c).getTipText();
        if (tipText == null) {
            return new Dimension(0, 0);
        }
        textArea = new JTextArea(tipText);
        textArea.setFont(new java.awt.Font("Tahoma", 0, 12));
        rendererPane.removeAll();
        rendererPane.add(textArea);
        textArea.setWrapStyleWord(true);
        int width = ((WordWrapToolTip) c).getFixedWidth();
        int columns = ((WordWrapToolTip) c).getColumns();

        if (columns > 0) {
            textArea.setColumns(columns);
            textArea.setSize(0, 0);
            textArea.setLineWrap(true);
            textArea.setSize(textArea.getPreferredSize());
        } else if (width > 0) {
            textArea.setLineWrap(true);
            Dimension d = textArea.getPreferredSize();
            d.width = width;
            d.height++;
            textArea.setSize(d);
        } else {
            textArea.setLineWrap(false);
        }

        Dimension dim = textArea.getPreferredSize();

        dim.height += 1;
        dim.width += 1;
        return dim;
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }
}
