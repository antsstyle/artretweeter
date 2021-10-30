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

    public static final String P_CHECK_NEW_COLLECTIONS_ENABLED = "artretweeter.twitter.checknewcollectionsenabled";
    public static final String P_CHECK_NEW_TWEETS_ENABLED = "artretweeter.twitter.checknewtweetsenabled";
    public static final String P_CHECK_NEW_TWEETS_FREQUENCY = "artretweeter.twitter.checknewtweetsfrequency";
    public static final String P_CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS = "artretweeter.twitter.checknewtweetsfrequencytimeunits";
    public static final String P_CHECK_NEW_COLLECTIONS_FREQUENCY = "artretweeter.twitter.checknewcollectionsfrequency";
    public static final String P_CHECK_NEW_COLLECTIONS_FREQUENCY_TIME_UNITS = "artretweeter.twitter.checknewcollectionsfrequencytimeunits";

    public static Boolean CHECK_NEW_COLLECTIONS_ENABLED = false;
    public static Boolean CHECK_NEW_TWEETS_ENABLED = false;
    public static Integer CHECK_NEW_TWEETS_FREQUENCY = 1;
    public static String CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS = "Hours";
    public static Integer CHECK_NEW_COLLECTIONS_FREQUENCY = 1;
    public static String CHECK_NEW_COLLECTIONS_FREQUENCY_TIME_UNITS = "Days";
    public static final String P_DO_NOT_SHOW_METRICS_ANYWHERE = "artretweeter.misc.donotshowmetricsanywhere";
    public static Boolean DO_NOT_SHOW_METRICS_ANYWHERE = true;
}
