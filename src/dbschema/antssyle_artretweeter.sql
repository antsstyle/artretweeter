-- phpMyAdmin SQL Dump
-- version 4.9.7
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Feb 20, 2022 at 08:50 AM
-- Server version: 5.7.37-log
-- PHP Version: 7.3.33

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `antssyle_artretweeter`
--

-- --------------------------------------------------------

--
-- Table structure for table `adminaccounts`
--

CREATE TABLE `adminaccounts` (
  `id` int(11) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `twitterid` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `artists`
--

CREATE TABLE `artists` (
  `id` int(11) NOT NULL,
  `twitterid` bigint(20) NOT NULL,
  `screenname` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `approved` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'U',
  `followercount` int(11) NOT NULL,
  `includetext` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `imagesenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `videosenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `gifsenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `nextcheckdate` datetime DEFAULT NULL,
  `oldtweetlimitretrieved` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `maxid` bigint(20) DEFAULT NULL,
  `sinceid` bigint(20) DEFAULT NULL,
  `retweetpercent` int(11) DEFAULT NULL,
  `adaptivertthreshold` int(11) DEFAULT NULL,
  `meanrtthreshold` int(11) DEFAULT NULL,
  `nextanalysis` datetime DEFAULT NULL,
  `nextupdateinfodate` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `artistsubmissions`
--

CREATE TABLE `artistsubmissions` (
  `id` int(11) NOT NULL,
  `screenname` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `submittedbyusertwitterid` bigint(20) NOT NULL,
  `datesubmitted` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `bannedusers`
--

CREATE TABLE `bannedusers` (
  `twitterid` bigint(20) NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `banneddate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `cachedvariables`
--

CREATE TABLE `cachedvariables` (
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lastmodified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `failedretweets`
--

CREATE TABLE `failedretweets` (
  `id` int(11) NOT NULL,
  `retweetingusertwitterid` bigint(20) NOT NULL,
  `tweetid` bigint(20) NOT NULL,
  `retweettime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `errorcode` int(11) NOT NULL,
  `failreason` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `patreontiers`
--

CREATE TABLE `patreontiers` (
  `patreon_id` bigint(20) NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount_cents` int(11) NOT NULL,
  `published` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `ratelimitrecords`
--

CREATE TABLE `ratelimitrecords` (
  `id` int(11) NOT NULL,
  `usertwitterid` bigint(20) NOT NULL,
  `endpoint` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `maxlimit` int(11) NOT NULL,
  `remaininglimit` int(11) NOT NULL,
  `resettime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `timetoresetseconds` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `rejectedartists`
--

CREATE TABLE `rejectedartists` (
  `id` int(11) NOT NULL,
  `twitterid` bigint(20) NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rejectiondate` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `retrievalmetrics`
--

CREATE TABLE `retrievalmetrics` (
  `id` int(11) NOT NULL,
  `description` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `timeseconds` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `retweetrecords`
--

CREATE TABLE `retweetrecords` (
  `id` int(11) NOT NULL,
  `usertwitterid` bigint(20) NOT NULL,
  `tweetauthorid` bigint(20) DEFAULT NULL,
  `tweetid` bigint(20) NOT NULL,
  `retweetid` bigint(20) NOT NULL,
  `retweettime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `scheduledretweettime` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `scheduledretweets`
--

CREATE TABLE `scheduledretweets` (
  `id` int(11) NOT NULL,
  `retweetingusertwitterid` bigint(20) NOT NULL,
  `tweetauthorid` bigint(20) DEFAULT NULL,
  `tweetid` bigint(20) NOT NULL,
  `retweettime` timestamp NULL DEFAULT NULL,
  `automated` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tweetmetrics`
--

CREATE TABLE `tweetmetrics` (
  `id` int(11) NOT NULL,
  `tweetstableid` int(11) NOT NULL,
  `retrievedtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `retrievalmetric` int(11) NOT NULL,
  `likes` int(11) NOT NULL,
  `retweets` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tweets`
--

CREATE TABLE `tweets` (
  `id` int(11) NOT NULL,
  `tweetid` bigint(20) NOT NULL,
  `usertwitterid` bigint(20) NOT NULL,
  `createdat` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fulltweettext` varchar(700) COLLATE utf8mb4_unicode_ci NOT NULL,
  `deletedflag` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `tweetjson` mediumtext COLLATE utf8mb4_unicode_ci,
  `mediatype` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fixed` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `userartistautomationsettings`
--

CREATE TABLE `userartistautomationsettings` (
  `id` int(11) NOT NULL,
  `usertwitterid` bigint(20) NOT NULL,
  `automationenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dayflags` varchar(7) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hourflags` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `includetext` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `excludetext` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `includetextcondition` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `excludetextcondition` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `oldtweetcutoffdate` datetime DEFAULT NULL,
  `oldtweetcutoffdateenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `includetextenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `excludetextenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `timezonehouroffset` int(11) NOT NULL,
  `timezoneminuteoffset` int(11) NOT NULL,
  `nextserverscheduledate` datetime DEFAULT NULL,
  `imagesenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `videosenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `gifsenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `userartistretweetsettings`
--

CREATE TABLE `userartistretweetsettings` (
  `id` int(11) NOT NULL,
  `usertwitterid` bigint(20) NOT NULL,
  `artisttwitterid` bigint(20) NOT NULL,
  `totalretweeted` int(11) NOT NULL DEFAULT '0',
  `enabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `userautomationsettings`
--

CREATE TABLE `userautomationsettings` (
  `id` int(11) NOT NULL,
  `usertwitterid` bigint(20) NOT NULL,
  `automationenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dayflags` varchar(7) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hourflags` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL,
  `minuteflags` varchar(4) COLLATE utf8mb4_unicode_ci NOT NULL,
  `includetext` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `excludetext` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `includetextcondition` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `excludetextcondition` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `retweetpercent` int(11) DEFAULT NULL,
  `oldtweetcutoffdate` datetime DEFAULT NULL,
  `oldtweetcutoffdateenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `includetextenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `excludetextenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `timezonehouroffset` int(11) NOT NULL,
  `timezoneminuteoffset` int(11) NOT NULL,
  `adaptivertthreshold` int(11) DEFAULT NULL,
  `metricsmeasurementtype` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `blockedhandlesenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `carryovercounter` double NOT NULL DEFAULT '0',
  `lastscheduleserverdate` datetime DEFAULT NULL,
  `imagesenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Y',
  `videosenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `gifsenabled` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `nextanalysis` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `userblockedhandles`
--

CREATE TABLE `userblockedhandles` (
  `id` int(11) NOT NULL,
  `usertwitterid` bigint(20) NOT NULL,
  `blockedhandle` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `twitterid` bigint(20) NOT NULL,
  `accesstoken` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `accesstokensecret` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dateadded` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `oldtweetlimitretrieved` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N',
  `sinceid` bigint(20) DEFAULT NULL,
  `maxid` bigint(20) DEFAULT NULL,
  `screenname` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `patreonid` bigint(20) DEFAULT NULL,
  `paiduser` varchar(1) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'N'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `usertweetresultcache`
--

CREATE TABLE `usertweetresultcache` (
  `usertwitterid` bigint(20) NOT NULL,
  `cachedresult` text COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `adminaccounts`
--
ALTER TABLE `adminaccounts`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`),
  ADD UNIQUE KEY `twitterid` (`twitterid`);

--
-- Indexes for table `artists`
--
ALTER TABLE `artists`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `twitterid` (`twitterid`);

--
-- Indexes for table `artistsubmissions`
--
ALTER TABLE `artistsubmissions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `screenname` (`screenname`,`submittedbyusertwitterid`);

--
-- Indexes for table `bannedusers`
--
ALTER TABLE `bannedusers`
  ADD PRIMARY KEY (`twitterid`);

--
-- Indexes for table `cachedvariables`
--
ALTER TABLE `cachedvariables`
  ADD PRIMARY KEY (`name`);

--
-- Indexes for table `failedretweets`
--
ALTER TABLE `failedretweets`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `patreontiers`
--
ALTER TABLE `patreontiers`
  ADD PRIMARY KEY (`patreon_id`);

--
-- Indexes for table `ratelimitrecords`
--
ALTER TABLE `ratelimitrecords`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `userauthunique` (`usertwitterid`,`endpoint`);

--
-- Indexes for table `rejectedartists`
--
ALTER TABLE `rejectedartists`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `usertwitterid` (`twitterid`);

--
-- Indexes for table `retrievalmetrics`
--
ALTER TABLE `retrievalmetrics`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `description` (`description`);

--
-- Indexes for table `retweetrecords`
--
ALTER TABLE `retweetrecords`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `scheduledretweets`
--
ALTER TABLE `scheduledretweets`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uniquescheduledtweet` (`retweetingusertwitterid`,`tweetid`) USING BTREE;

--
-- Indexes for table `tweetmetrics`
--
ALTER TABLE `tweetmetrics`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `tweetstableid` (`tweetstableid`,`retrievalmetric`),
  ADD KEY `tweetmetrics_ibfk_1` (`retrievalmetric`);

--
-- Indexes for table `tweets`
--
ALTER TABLE `tweets`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `tweets_uniquetweetid` (`tweetid`),
  ADD KEY `usertwitterid` (`usertwitterid`),
  ADD KEY `fulltweettext` (`fulltweettext`);

--
-- Indexes for table `userartistautomationsettings`
--
ALTER TABLE `userartistautomationsettings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `usertwitterid` (`usertwitterid`);

--
-- Indexes for table `userartistretweetsettings`
--
ALTER TABLE `userartistretweetsettings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `usertwitterid` (`usertwitterid`,`artisttwitterid`);

--
-- Indexes for table `userautomationsettings`
--
ALTER TABLE `userautomationsettings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uniquetwitterid` (`usertwitterid`);

--
-- Indexes for table `userblockedhandles`
--
ALTER TABLE `userblockedhandles`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `twitterid` (`twitterid`);

--
-- Indexes for table `usertweetresultcache`
--
ALTER TABLE `usertweetresultcache`
  ADD PRIMARY KEY (`usertwitterid`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `adminaccounts`
--
ALTER TABLE `adminaccounts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `artists`
--
ALTER TABLE `artists`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `artistsubmissions`
--
ALTER TABLE `artistsubmissions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `failedretweets`
--
ALTER TABLE `failedretweets`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `ratelimitrecords`
--
ALTER TABLE `ratelimitrecords`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `rejectedartists`
--
ALTER TABLE `rejectedartists`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `retrievalmetrics`
--
ALTER TABLE `retrievalmetrics`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `retweetrecords`
--
ALTER TABLE `retweetrecords`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `scheduledretweets`
--
ALTER TABLE `scheduledretweets`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `tweetmetrics`
--
ALTER TABLE `tweetmetrics`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `tweets`
--
ALTER TABLE `tweets`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `userartistautomationsettings`
--
ALTER TABLE `userartistautomationsettings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `userartistretweetsettings`
--
ALTER TABLE `userartistretweetsettings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `userautomationsettings`
--
ALTER TABLE `userautomationsettings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `userblockedhandles`
--
ALTER TABLE `userblockedhandles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `tweetmetrics`
--
ALTER TABLE `tweetmetrics`
  ADD CONSTRAINT `tweetmetrics_ibfk_1` FOREIGN KEY (`retrievalmetric`) REFERENCES `retrievalmetrics` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `tweetmetrics_tweettableidconstraint` FOREIGN KEY (`tweetstableid`) REFERENCES `tweets` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
