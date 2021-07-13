/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.configuration;

import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Antsstyle
 */
public class ConfigValidator {

    private static final Logger LOGGER = LogManager.getLogger(ConfigValidator.class);

    /**
     * Validates configuration entries, ensuring they are within bounds specific to each one.
     * <p>
     * If they are not, this function will set them to a default value.
     *
     * @param mappings The configuration name/value pairs to validate.
     */
    public static void validateInputMappings(HashMap<String, Object> mappings) {
        if (mappings.containsKey(TwitterConfig.P_CHECK_NEW_TWEETS_FREQUENCY)
                || mappings.containsKey(TwitterConfig.P_CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS)) {
            Integer timeValue;
            if (mappings.containsKey(TwitterConfig.P_CHECK_NEW_TWEETS_FREQUENCY)) {
                timeValue = Integer.parseInt((String) mappings.get(TwitterConfig.P_CHECK_NEW_TWEETS_FREQUENCY));
            } else {
                timeValue = TwitterConfig.CHECK_NEW_TWEETS_FREQUENCY;
            }
            String unitsValue;
            if (mappings.containsKey(TwitterConfig.P_CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS)) {
                unitsValue = (String) mappings.get(TwitterConfig.P_CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS);
            } else {
                unitsValue = TwitterConfig.CHECK_NEW_TWEETS_FREQUENCY_TIME_UNITS;
            }
            if (timeValue < 30 && unitsValue.toLowerCase().equals("minutes")) {
                LOGGER.info("Frequency of tweet checking is below minimum threshold. Reverting to default value of 30 minutes.");
                mappings.put(TwitterConfig.P_CHECK_NEW_TWEETS_FREQUENCY, 30);
                
            }
        }
    }

}
