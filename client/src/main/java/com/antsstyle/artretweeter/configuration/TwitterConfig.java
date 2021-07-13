/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.configuration;

/**
 *
 * @author antss
 */
public class TwitterConfig extends ConfigurationModule {

    public static final String P_CHECK_NEW_TWEETS_ENABLED = "artretweeter.twitter.checknewtweetsenabled";
    public static final String P_CHECK_NEW_TWEETS_FREQUENCY = "artretweeter.twitter.checknewtweetsfrequency";
    public static final String P_CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS = "artretweeter.twitter.checknewtweetsfrequencytimeunits";
    
    public static Boolean CHECK_NEW_TWEETS_ENABLED = false;
    public static Integer CHECK_NEW_TWEETS_FREQUENCY = 1;
    public static String CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS = "Hours";
}
