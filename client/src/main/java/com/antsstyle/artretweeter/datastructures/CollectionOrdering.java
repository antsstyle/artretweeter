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

    CURATION_REVERSE_CHRON("curation_reverse_chron"),
    TWEET_CHRON("tweet_chron"),
    TWEET_REVERSE_CHRON("tweet_reverse_chron");

    private final String parameterName;

    public String getParameterName() {
        return parameterName;
    }

    private CollectionOrdering(String parameterName) {
        this.parameterName = parameterName;
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

}
