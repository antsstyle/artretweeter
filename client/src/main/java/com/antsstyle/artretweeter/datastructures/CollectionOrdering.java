/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

/**
 *
 * @author antss
 */
public enum CollectionOrdering {

    CURATION_REVERSE_CHRON("curation_reverse_chron", "Curation order (newest first)"),
    TWEET_CHRON("tweet_chron", "Chronological tweet order (oldest first)"),
    TWEET_REVERSE_CHRON("tweet_reverse_chron", "Chronological tweet order (newest first)");

    private final String parameterName;
    private final String description;

    public String getDescription() {
        return description;
    }

    public String getParameterName() {
        return parameterName;
    }

    private CollectionOrdering(String parameterName, String description) {
        this.parameterName = parameterName;
        this.description = description;
    }
    
    public static CollectionOrdering getOrdering(String parameterName) {
       if (parameterName.equals(CURATION_REVERSE_CHRON.getParameterName())) {
           return CURATION_REVERSE_CHRON;
       } else if (parameterName.equals(TWEET_CHRON.getParameterName())) {
           return TWEET_CHRON;
       } else if (parameterName.equals(TWEET_REVERSE_CHRON.getParameterName())) {
           return TWEET_REVERSE_CHRON;
       }
       return null;
    }
    
    @Override
    public String toString() {
        return description;
    }

}
