<?php

namespace ArtRetweeter;

require_once "core.php";

$databaseConnection->query("CREATE TABLE IF NOT EXISTS users (
		twitterid BIGINT NOT NULL PRIMARY KEY,
		accesstoken VARCHAR(255) NOT NULL,
                accesstokensecret VARCHAR(255) NOT NULL,
                CONSTRAINT users_uniquetwitterid UNIQUE (twitterid))");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS ratelimitrecords (
		id INT AUTO_INCREMENT PRIMARY KEY,
		usertwitterid BIGINT NOT NULL,
		endpoint VARCHAR(255) NOT NULL,
		maxlimit INT NOT NULL,
		remaininglimit INT NOT NULL,
		resettime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
                timetoresetseconds INT NOT NULL,
                CONSTRAINT ratelimitrecords_userauthunique UNIQUE (userid,endpoint))");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS retweetrecords (
		id INT AUTO_INCREMENT PRIMARY KEY,
		usertwitterid BIGINT NOT NULL,
		tweetid BIGINT NOT NULL,
                retweetid BIGINT NOT NULL,
		retweettime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS scheduledretweets (
                id INT AUTO_INCREMENT PRIMARY KEY,
                retweetingusertwitterid BIGINT NOT NULL,
                tweetid BIGINT NOT NULL,
                retweettime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                ADD CONSTRAINT scheduledretweets_uniquescheduledtweet UNIQUE (retweetinguserid,tweetid))");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS failedretweets (
                id INT AUTO_INCREMENT PRIMARY KEY,
                retweetingusertwitterid BIGINT NOT NULL,
                tweetid BIGINT NOT NULL,
                retweettime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                errorcode INT NOT NULL,
                failreason VARCHAR(255) NOT NULL)");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS tweets (
                id INT AUTO_INCREMENT PRIMARY KEY,
                tweetid BIGINT NOT NULL,
                usertwitterid BIGINT NOT NULL,
                createdat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                fulltweettext VARCHAR(500) NOT NULL,
                deletedflag VARCHAR(1) NOT NULL DEFAULT 'N',
                CONSTRAINT tweets_uniquetweetid UNIQUE (tweetid))");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS tweetmetrics (
                id INT AUTO_INCREMENT PRIMARY KEY,
                tweetstableid INT NOT NULL,
                retrievedtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                retrievalmetric INT NOT NULL,
                likes INT NOT NULL,
                retweets INT NOT NULL,
                CONSTRAINT tweetmetrics_tweettableidconstraint FOREIGN KEY (tweetstableid) REFERENCES tweets(id),
                CONSTRAINT tweetmetrics_uniquetweetmetric UNIQUE (tweetstableid,retrievalmetric))");

$databaseConnection->query("ALTER TABLE tweetmetrics ADD FOREIGN KEY (retrievalmetric) 
        REFERENCES retrievalmetrics(id) ON DELETE RESTRICT ON UPDATE RESTRICT");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS retrievalmetrics (
		id INT AUTO_INCREMENT PRIMARY KEY,
		description VARCHAR(255) NOT NULL,
                CONSTRAINT retrievalmetrics_uniqueretrievalmetric UNIQUE (description))");

$databaseConnection->prepare("INSERT INTO retrievalmetrics (description) VALUES (?)")->execute(["Latest Metrics"]);