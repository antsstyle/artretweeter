/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.tools;

import java.nio.file.Path;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author antss
 */
public class PathTools {

    public static String convertPathToString(Path path) {
        if (path == null) {
            return null;
        }
        String pathString = StringUtils.replace(path.toString(), "\\", "/");
        return pathString;
    }

}
