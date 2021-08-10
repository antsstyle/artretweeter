<?php

namespace ArtRetweeter;
require_once "core.php";

$databaseConnection->query("CREATE TABLE IF NOT EXISTS users (
		twitterid BIGINT NOT NULL PRIMARY KEY,
		accesstoken VARCHAR(255) NOT NULL,
                accesstokensecret VARCHAR(255) NOT NULL");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS ratelimitrecords (
		id INT AUTO_INCREMENT PRIMARY KEY,
		usertwitterid BIGINT NOT NULL,
		endpoint VARCHAR(255) NOT NULL,
		maxlimit INT NOT NULL,
		remaininglimit INT NOT NULL,
		resettime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, 
                timetoresetseconds INT NOT NULL)");

$databaseConnection->query("ALTER TABLE ratelimitrecords ADD CONSTRAINT 
        userauthunique UNIQUE KEY(userid,endpoint)");

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
                retweettime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

$databaseConnection->query("ALTER TABLE scheduledretweets ADD CONSTRAINT 
        uniquescheduledtweet UNIQUE KEY(retweetinguserid,tweetid)");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS failedretweets (
                id INT AUTO_INCREMENT PRIMARY KEY,
                retweetingusertwitterid BIGINT NOT NULL,
                tweetid BIGINT NOT NULL,
                retweettime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                errorcode INT NOT NULL,
                failreason VARCHAR(255) NOT NULL)");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS tweetmetrics (
                id INT AUTO_INCREMENT PRIMARY KEY,
                tweetid BIGINT NOT NULL,
                usertwitterid BIGINT NOT NULL,
                createdat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                fulltweettext VARCHAR(500) NOT NULL,
                retrievedtime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                retrievalmetric INT NOT NULL,
                likes INT NOT NULL,
                retweets INT NOT NULL)");

$databaseConnection->query("ALTER TABLE tweetmetrics ADD INDEX (tweetid)");

$databaseConnection->query("ALTER TABLE tweetmetrics ADD CONSTRAINT 
        uniquetweetmetric UNIQUE KEY(tweetid,retrievalmetric)");

$databaseConnection->query("CREATE TABLE IF NOT EXISTS retrievalmetrics (
		id INT AUTO_INCREMENT PRIMARY KEY,
		description VARCHAR(255) NOT NULL)");

$databaseConnection->query("ALTER TABLE retrievalmetrics ADD CONSTRAINT 
        uniqueretrievalmetric UNIQUE KEY(description)");