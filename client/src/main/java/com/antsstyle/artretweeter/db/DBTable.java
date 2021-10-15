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

    ACCOUNTS("accounts",
            "CREATE TABLE IF NOT EXISTS accounts ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "twitterid BIGINT NOT NULL, "
            + "token VARCHAR(256) NOT NULL, "
            + "tokensecret VARCHAR(256) NOT NULL, "
            + "screen_name VARCHAR(256) NOT NULL, "
            + "max_id BIGINT, "
            + "since_id BIGINT, "
            + "retrievedoldtweetslimitfromtwitter VARCHAR(1) DEFAULT 'N' NOT NULL, "
            + "retrievedalloldtweetsfromserver VARCHAR(1) DEFAULT 'N' NOT NULL, "
            + "CONSTRAINT uniqueaccount UNIQUE (twitterid))"),
    APPRATELIMITS("appratelimits",
            "CREATE TABLE IF NOT EXISTS appratelimits ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "endpoint VARCHAR(255) NOT NULL, "
            + "maxlimit INTEGER NOT NULL, "
            + "remaininglimit INTEGER NOT NULL, "
            + "resettime DATETIME NOT NULL, "
            + "CONSTRAINT uniqueappratelimit UNIQUE (endpoint))"
    ),
    FAILEDRETWEETS("failedretweets",
            "CREATE TABLE IF NOT EXISTS failedretweets ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "tweetid BIGINT NOT NULL, "
            + "retweetingusertwitterid BIGINT NOT NULL, "
            + "retweettime DATETIME NOT NULL, "
            + "errorcode INTEGER NOT NULL, "
            + "failreason VARCHAR(255) NOT NULL, "
            + "CONSTRAINT uniquefailedretweet UNIQUE (tweetid,retweetingusertwitterid,retweettime))"),
    TWEETS("tweets",
            "CREATE TABLE IF NOT EXISTS tweets ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "tweetid BIGINT NOT NULL, "
            + "usertwitterid BIGINT NOT NULL, "
            + "screen_name VARCHAR(255) NOT NULL, "
            + "fulltweettext VARCHAR(500) NOT NULL, "
            + "filepath1 VARCHAR(255) NOT NULL,"
            + "filepath2 VARCHAR(255), "
            + "filepath3 VARCHAR(255), "
            + "filepath4 VARCHAR(255), "
            + "url1 VARCHAR(255) NOT NULL, "
            + "url2 VARCHAR(255), "
            + "url3 VARCHAR(255), "
            + "url4 VARCHAR(255), "
            + "created_at DATETIME NOT NULL, "
            + "likecount INTEGER NOT NULL, "
            + "retweetcount INTEGER NOT NULL, "
            + "source VARCHAR(500) NOT NULL, "
            + "deletedflag VARCHAR(1) DEFAULT 'N' NOT NULL, "
            + "CONSTRAINT uniquetweet UNIQUE (tweetid))"),
    CONFIGURATION("configuration",
            "CREATE TABLE IF NOT EXISTS configuration ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "name VARCHAR(255) NOT NULL, "
            + "value VARCHAR(255) NOT NULL, "
            + "CONSTRAINT uniqueconfigitem UNIQUE (name))"),
    CACHEDVARIABLES("cachedvariables",
            "CREATE TABLE IF NOT EXISTS cachedvariables ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "name VARCHAR(255) NOT NULL, "
            + "value VARCHAR(255) NOT NULL, "
            + "CONSTRAINT uniquecachedvariable UNIQUE (name))"),
    COLLECTIONS("collections",
            "CREATE TABLE IF NOT EXISTS collections ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "usertwitterid BIGINT NOT NULL, "
            + "collectionid VARCHAR(255) NOT NULL,"
            + "collectionurl VARCHAR(255) NOT NULL, "
            + "name VARCHAR(256) NOT NULL, "
            + "description VARCHAR(256), "
            + "ordering VARCHAR(50) NOT NULL, "
            + "CONSTRAINT uniquecollection UNIQUE (collectionid))"),
    COLLECTIONTWEETS("collectiontweets",
            "CREATE TABLE collectiontweets ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "tweetid BIGINT NOT NULL, "
            + "collectionid VARCHAR(255) NOT NULL, "
            + "ordernumber INTEGER, "
            + "CONSTRAINT uniquecollectiontweet UNIQUE (tweetid,collectionid))"),
    RETWEETQUEUE("retweetqueue",
            "CREATE TABLE IF NOT EXISTS retweetqueue ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "tweetid BIGINT NOT NULL, "
            + "retweetingusertwitterid BIGINT NOT NULL, "
            + "retweettime DATETIME NOT NULL, "
            + "automated VARCHAR(1) NOT NULL, "
            + "CONSTRAINT uniqueretweetqueueentry UNIQUE (tweetid,retweetingusertwitterid))"),
    RETWEETRECORDS("retweetrecords",
            "CREATE TABLE IF NOT EXISTS retweetrecords ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "usertwitterid BIGINT NOT NULL, "
            + "tweetid BIGINT NOT NULL, "
            + "retweettime DATETIME NOT NULL, "
            + "CONSTRAINT uniqueretweetentry UNIQUE (usertwitterid,tweetid,retweettime))"),
    USERAUTOMATIONSETTINGS("userautomationsettings",
            "CREATE TABLE IF NOT EXISTS userautomationsettings ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "usertwitterid BIGINT NOT NULL, "
            + "automationenabled VARCHAR(1) NOT NULL, "
            + "dayflags VARCHAR(7) NOT NULL, "
            + "hourflags VARCHAR(24) NOT NULL, "
            + "minuteflags VARCHAR(4) NOT NULL, "
            + "includedtext VARCHAR(255) DEFAULT NULL, "
            + "excludedtext VARCHAR(255) DEFAULT NULL, "
            + "retweetpercent INTEGER NOT NULL, "
            + "oldtweetcutoffdate DATETIME, "
            + "oldtweetcutoffdateenabled VARCHAR(1) NOT NULL, "
            + "includedtextenabled VARCHAR(1) NOT NULL, "
            + "excludedtextenabled VARCHAR(1) NOT NULL, "
            + "timezonehouroffset INTEGER NOT NULL, "
            + "timezoneminuteoffset INTEGER NOT NULL, "
            + "includetextcondition VARCHAR(50) NOT NULL, "
            + "excludetextcondition VARCHAR(50) NOT NULL, "
            + "CONSTRAINT uniquetwitterid UNIQUE (usertwitterid))"),
    USERRATELIMITS("userratelimits",
            "CREATE TABLE IF NOT EXISTS userratelimits ("
            + "id INTEGER IDENTITY PRIMARY KEY, "
            + "usertwitterid BIGINT NOT NULL, "
            + "endpoint VARCHAR(255) NOT NULL, "
            + "maxlimit INTEGER NOT NULL, "
            + "remaininglimit INTEGER NOT NULL, "
            + "resettime DATETIME NOT NULL, "
            + "CONSTRAINT uniqueuserratelimit UNIQUE (usertwitterid,endpoint))");

    private final String tableName;
    private final String createTableQuery;

    public String getCreateTableQuery() {
        return createTableQuery;
    }

    public String getTableName() {
        return tableName;
    }

    private DBTable(String tableName, String createTableQuery) {
        this.tableName = tableName;
        this.createTableQuery = createTableQuery;
    }

}
