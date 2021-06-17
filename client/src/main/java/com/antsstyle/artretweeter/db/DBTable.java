/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.db;

/**
 *
 * @author Ant
 */
public enum DBTable {

    ACCOUNTS("accounts"),
    APPRATELIMITS("appratelimits"),
    TWEETS("tweets"),
    CONFIGURATION("configuration"),
    COLLECTIONS("collections"),
    COLLECTIONTWEETS("collectiontweets"),
    RETWEETQUEUE("retweetqueue"),
    RETWEETS("retweets"),
    USERRATELIMITS("userratelimits");

    private final String tableName;

    public String getTableName() {
        return tableName;
    }

    private DBTable(String tableName) {
        this.tableName = tableName;
    }

}
