<?php

require "core.php";

$database_connection->query("CREATE TABLE IF NOT EXISTS users (
		id INT AUTO_INCREMENT PRIMARY KEY,
		twitterid BIGINT NOT NULL,
		accesstoken VARCHAR(255) NOT NULL,
                accesstokensecret VARCHAR(255) NOT NULL");

$database_connection->query("ALTER TABLE users ADD CONSTRAINT 
        uniqueuser UNIQUE KEY(twitterid)");

$database_connection->query("CREATE TABLE IF NOT EXISTS ratelimitrecords (
		id INT AUTO_INCREMENT PRIMARY KEY,
		userid BIGINT NOT NULL,
		endpoint VARCHAR(255) NOT NULL,
		maxlimit INT NOT NULL,
		remaininglimit INT NOT NULL,
		resettime TIMESTAMP, 
                timetoresetseconds INT NOT NULL)");

$database_connection->query("ALTER TABLE ratelimitrecords ADD CONSTRAINT 
        userauthunique UNIQUE KEY(userid,endpoint)");

$database_connection->query("CREATE TABLE IF NOT EXISTS retweetrecords (
		id INT AUTO_INCREMENT PRIMARY KEY,
		userid BIGINT NOT NULL,
		tweetid VARCHAR(255) NOT NULL,
		retweettime TIMESTAMP)");

$database_connection->query("CREATE TABLE IF NOT EXISTS scheduledretweets (
                id INT AUTO_INCREMENT PRIMARY KEY,
                retweetinguserid BIGINT NOT NULL,
                tweetid BIGINT NOT NULL,
                retweettime TIMESTAMP)");

$database_connection->query("ALTER TABLE scheduledretweets ADD CONSTRAINT 
        uniquescheduledtweet UNIQUE KEY(retweetinguserid,tweetid)");

$database_connection->query("CREATE TABLE IF NOT EXISTS failedretweets (
                id INT AUTO_INCREMENT PRIMARY KEY,
                retweetinguserid BIGINT NOT NULL,
                tweetid BIGINT NOT NULL,
                retweettime TIMESTAMP,
                errorcode INT NOT NULL,
                failreason VARCHAR(255) NOT NULL)");
