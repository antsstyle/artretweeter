/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.configuration;

import com.antsstyle.artretweeter.configuration.ConfigurationModule;
import java.awt.Color;

/**
 *
 * @author antss
 */
public class GUIConfig extends ConfigurationModule {

    public static final String P_WINDOW_BG_COLOUR = "artretweeter.windowbgcolour";
    public static final String P_JBUTTON_BG_COLOUR = "artretweeter.jbuttonbgcolour";
    public static final String P_WINDOW_FONT_COLOUR = "artretweeter.windowfontcolour";
    public static final String P_JBUTTON_FONT_COLOUR = "artretweeter.jbuttonfontcolour";

    public static Color WINDOW_BG_COLOUR = new Color(55, 60, 74);
    public static Color JBUTTON_BG_COLOUR = new Color(0, 102, 102);
    public static Color WINDOW_FONT_COLOUR = new Color(200, 200, 200);
    public static Color JBUTTON_FONT_COLOUR = new Color(200, 200, 200);

}
