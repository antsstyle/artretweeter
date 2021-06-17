/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Ant
 */
public abstract class RegularExpressions {
    
    final String whatever = "https://twitter.com/i/web/status/1400004661852078084";

    public static final String TWITTER_STATUS_REGEX
            = "^(http:\\/\\/|https:\\/\\/)?(www\\.)?(twitter\\.com\\/){1}"
            + "(([A-Za-z0-9_]{1,15})|(i/web){1})"
            + "(\\/status\\/){1}[0-9]+$";

    public static final String TWITTER_SITE_REGEX
            = "^(http:\\/\\/|https:\\/\\/)?(www\\.)?(twitter\\.com\\/){1}";

    private RegularExpressions() {

    }

    /**
     * Checks if a given string matches the given regex.
     *
     * @param stringToMatch The string to check for matches.
     * @param regex The regex to use.
     * @return True if the string matches the regex; false otherwise.
     */
    public static boolean matchesRegex(String stringToMatch, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(stringToMatch);
        return m.find();
    }

}
