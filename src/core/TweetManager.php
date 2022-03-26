<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\Core\LogManager;

class TweetManager {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function insertTweetsAndMetrics($results, $userTwitterID, $imagesEnabled = "Y", $gifsEnabled = "N", $videosEnabled = "N") {
        $tweets = $results->data;
        $includes = $results->includes->media;
        if (!$includes) {
            return true;
        }
        $selectQuery = "SELECT id FROM retrievalmetrics WHERE description=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute(["Latest Metrics"]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get metrics type, cannot insert tweets: " . print_r($e, true));
            return false;
        }
        $latestMetricsID = $selectStmt->fetchColumn();
        if ($latestMetricsID === false) {
            self::$logger->error("No metrics type found, cannot insert tweets.");
            return false;
        }

        $selectQuery = "SELECT tweetid FROM tweets WHERE usertwitterid=? AND deletedflag=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userTwitterID, "Y"]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get delete-flagged tweets for user, cannot insert tweets: " . print_r($e, true));
            return false;
        }

        while ($row = $selectStmt->fetch()) {
            $deletedTweetsArray[$row['tweetid']] = 1;
        }
        CoreDB::getConnection()->beginTransaction();
        $timeNow = date("Y-m-d H:i:s");
        $mediaMap = [];
        foreach ($includes as $mediaEntry) {
            $mediaMap[$mediaEntry->media_key] = $mediaEntry->type;
        }
        foreach ($tweets as $tweet) {
            $created_at = date("Y-m-d H:i:s", strtotime($tweet->created_at));
            if (!$tweet->attachments->media_keys) {
                continue;
            }
            $firstMediaKey = $tweet->attachments->media_keys[0];
            if (!array_key_exists($firstMediaKey, $mediaMap)) {
                self::$logger->error("Media key $firstMediaKey doesn't exist in includes map! Tweet ID: " . $tweet->id);
                continue;
            }
            $mediaType = $mediaMap[$firstMediaKey];
            if ($mediaType === "photo" && $imagesEnabled !== "Y") {
                continue;
            }
            if ($mediaType === "animated_gif" && $gifsEnabled !== "Y") {
                continue;
            }
            if ($mediaType === "video" && $videosEnabled !== "Y") {
                continue;
            }
            if ($tweet->in_reply_to_user_id && ($tweet->in_reply_to_user_id != $userTwitterID)) {
                continue;
            }
            if (isset($deletedTweetsArray) && isset($deletedTweetsArray[$tweet->id])) {
                continue;
            }
            $likeCount = $tweet->public_metrics->like_count;
            $retweetCount = $tweet->public_metrics->retweet_count;
            $fullText = $tweet->text;
            $urlentities = $tweet->entities->urls;
            for ($j = 0; $j < count($urlentities); $j++) {
                $entityDisplayURL = $urlentities[$j]->display_url;
                if (strpos($entityDisplayURL, "pic.twitter.com") !== false) {
                    $fullText = str_replace($urlentities[$j]->url, "", $fullText);
                } else {
                    $fullText = str_replace($urlentities[$j]->url, $urlentities[$j]->expanded_url, $fullText);
                }
            }
            try {
                $stmt = CoreDB::getConnection()->prepare("INSERT INTO tweets
            (tweetid,usertwitterid,createdat,fulltweettext,mediatype) 
	VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE fulltweettext=?");
                $stmt->execute([$tweet->id, $userTwitterID, $created_at, $fullText, $mediaType, $fullText]);
            } catch (\PDOException $e) {
                self::$logger->error("Failed to insert tweet: " . print_r($e, true));
            }

            try {
                $selectStmt = CoreDB::getConnection()->prepare("SELECT id FROM tweetmetrics WHERE tweetid=? AND retrievalmetric=?");
                $selectStmt->execute([$tweet->id, $latestMetricsID]);
                if ($selectStmt->fetchColumn() === false) {
                    $stmt = CoreDB::getConnection()->prepare("INSERT INTO tweetmetrics
                (tweetid,retrievedtime,retrievalmetric,likes,retweets) VALUES (?,?,?,?,?)");
                    $stmt->execute([$tweet->id, $timeNow, $latestMetricsID, $likeCount, $retweetCount]);
                } else {
                    $stmt = CoreDB::getConnection()->prepare("UPDATE tweetmetrics SET likes=?,retweets=?,retrievedtime=? 
                            WHERE tweetid=? AND retrievalmetric=?");
                    $stmt->execute([$likeCount, $retweetCount, $timeNow, $tweet->id, $latestMetricsID]);
                }
            } catch (\PDOException $e) {
                self::$logger->error("Failed to insert tweet metrics for new tweet: " . print_r($e, true));
            }
        }
        CoreDB::getConnection()->commit();
    }

    public static function getAllNewTweetsForAllUsers() {
        $now = date("Y-m-d H:i:s");
        $query = "SELECT * FROM users INNER JOIN userautomationsettings ON users.twitterid=userautomationsettings.usertwitterid "
                . "WHERE automationenabled=? AND locked=? AND (nextusertimelinecheck IS NULL OR nextusertimelinecheck <= ?)";
        $stmt = CoreDB::getConnection()->prepare($query);
        try {
            $stmt->execute(["Y", "N", $now]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to acquire list of users to schedule automated retweets for: " . print_r($e, true));
            return false;
        }

        while ($row = $stmt->fetch()) {
            TweetManager::getAllNewTweetsForUser($row);
        }
        return true;
    }

    public static function getAllNewTweetsForAllArtists() {
        $now = date("Y-m-d H:i:s");
        $query = "SELECT * FROM artists WHERE (nextcheckdate IS NULL OR nextcheckdate < ?)";
        $stmt = CoreDB::getConnection()->prepare($query);
        try {
            $stmt->execute([$now]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to acquire list of artists to get tweets for: " . print_r($e, true));
            return false;
        }

        while ($row = $stmt->fetch()) {
            $errorCode = TweetManager::getAllNewTweetsForArtist($row);
            if ($errorCode === 429) {
                break;
            }
        }
        return true;
    }

    public static function getAllNewTweetsForArtist($artistRow) {
        $params['max_results'] = 100;
        $params['exclude'] = "retweets";
        $params['expansions'] = "attachments.media_keys";
        $params['media.fields'] = "type";
        $params['tweet.fields'] = "public_metrics,author_id,in_reply_to_user_id,entities,created_at";
        if ($artistRow['oldtweetlimitretrieved'] === "N" && !is_null($artistRow['maxid'])) {
            $params['until_id'] = $artistRow['maxid'];
        } else if (!is_null($artistRow['sinceid'])) {
            $params['since_id'] = $artistRow['sinceid'];
        }
        $resultCount = 1;
        $queryCount = 0;
        $reachedEnd = false;
        $oldestID = null;
        $newestID = null;
        $totalResultCount = 0;
        while ($resultCount != 0 && !$reachedEnd) {
            $endpoint = "users/" . $artistRow['twitterid'] . "/tweets";
            $response = Core::queryTwitterUserAuth($endpoint, "users/:id/tweets", "GET", $params, APIKeys::bearer_token);
            $results = $response[0];
            $twitterResponseStatus = $response[1];
            $queryCount++;
            if ($twitterResponseStatus->getHttpCode() == 200) {
                $meta = $results->meta;
                $resultCount = $meta->result_count;
                if ($resultCount == 0) {
                    $reachedEnd = true;
                } else {
                    $totalResultCount += $resultCount;
                    TweetManager::insertTweetsAndMetrics($results, $artistRow['twitterid'], $artistRow['imagesenabled'],
                            $artistRow['gifsenabled'], $artistRow['videosenabled']);
                    if (!isset($meta->oldest_id) && !isset($meta->newest_id)) {
                        break;
                    }
                    if (isset($meta->oldest_id) && !is_null($meta->oldest_id)) {
                        $oldestID = $meta->oldest_id;
                    }
                    if (isset($meta->newest_id) && !is_null($meta->newest_id)) {
                        $newestID = $meta->newest_id;
                    }
                    if ($artistRow['oldtweetlimitretrieved'] == "N") {
                        $params['until_id'] = $meta->oldest_id - 1;
                    } else {
                        $params['since_id'] = $meta->newest_id;
                    }
                }
            } else {
                if ($twitterResponseStatus->getHttpCode() == 429) {
                    return 429;
                }
                break;
            }
            if ($queryCount > 35) {
                self::$logger->critical("35 users/:id/tweets queries!");
                break;
            }
        }
        $resultCountModifier = max(ceil($totalResultCount / 100), 1);
        $numHoursToNextCheck = ceil(24 / $resultCountModifier);
        $nextCheck = date("Y-m-d H:i:s", strtotime("+$numHoursToNextCheck hours"));
        $query = "UPDATE artists SET nextcheckdate=? ";
        $updParams[] = $nextCheck;
        if ($reachedEnd && !is_null($params['until_id'])) {
            $query .= ",oldtweetlimitretrieved=?, maxid=? ";
            array_push($updParams, "Y", $oldestID);
        } else if (!is_null($oldestID) && !is_null($params['until_id'])) {
            $query .= ",maxid=? ";
            $updParams[] = $oldestID;
        } else if (!is_null($newestID)) {
            $query .= ",sinceid=? ";
            $updParams[] = $newestID;
        }
        $query .= "WHERE twitterid=?";
        array_push($updParams, $artistRow['twitterid']);

        $stmt = CoreDB::getConnection()->prepare($query);
        try {
            $stmt->execute($updParams);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to update artist tweet parameters: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function getAllNewTweetsForUser($userRow) {
        $params['max_results'] = 100;
        $params['exclude'] = "retweets";
        $params['expansions'] = "attachments.media_keys";
        $params['media.fields'] = "type";
        $params['tweet.fields'] = "public_metrics,author_id,in_reply_to_user_id,entities,created_at";
        $resultCount = 1;
        $queryCount = 0;
        $reachedEnd = false;

        if ($userRow['oldtweetlimitretrieved'] == "Y") {
            if ($userRow['sinceid'] != null) {
                $params['since_id'] = $userRow['sinceid'];
            }
        } else {
            if ($userRow['maxid'] != null) {
                $params['until_id'] = $userRow['maxid'];
            }
        }
        $newestID = null;
        $oldestID = null;
        $totalResultCount = 0;
        while ($resultCount != 0 && !$reachedEnd) {
            $endpoint = "users/" . $userRow['usertwitterid'] . "/tweets";
            $response = Core::queryTwitterUserAuth($endpoint, "users/:id/tweets", "GET", $params, $userRow);
            $results = $response[0];
            $twitterResponseStatus = $response[1];
            $queryCount++;
            if ($twitterResponseStatus->getHttpCode() == 200) {
                $meta = $results->meta;
                $resultCount = $meta->result_count;
                if ($resultCount == 0) {
                    $reachedEnd = true;
                } else {
                    $totalResultCount += $resultCount;
                    TweetManager::insertTweetsAndMetrics($results, $userRow['usertwitterid'], $userRow['imagesenabled'],
                            $userRow['gifsenabled'], $userRow['videosenabled']);
                    if (!isset($meta->oldest_id) && !isset($meta->newest_id)) {
                        break;
                    }
                    if (isset($meta->oldest_id) && !is_null($meta->oldest_id)) {
                        $oldestID = $meta->oldest_id;
                    }
                    if (isset($meta->newest_id) && !is_null($meta->newest_id)) {
                        $newestID = $meta->newest_id;
                    }
                    if ($userRow['oldtweetlimitretrieved'] == "N") {
                        $params['until_id'] = $meta->oldest_id - 1;
                    } else {
                        $params['since_id'] = $meta->newest_id;
                    }
                }
            } else {
                break;
            }
            if ($queryCount > 35) {
                self::$logger->critical("35 users/:id/tweets queries!");
                break;
            }
        }
        $resultCountModifier = max(ceil($totalResultCount / 100), 1);
        $numHoursToNextCheck = ceil(24 / $resultCountModifier);
        $nextCheck = date("Y-m-d H:i:s", strtotime("+$numHoursToNextCheck hours"));
        $query = "UPDATE users SET nextusertimelinecheck=? ";
        $updParams[] = $nextCheck;
        if ($reachedEnd && !is_null($params['until_id'])) {
            $query .= ",oldtweetlimitretrieved=?, maxid=? ";
            array_push($updParams, "Y", $oldestID);
        } else if (!is_null($oldestID) && !is_null($params['until_id'])) {
            $query .= ",maxid=? ";
            $updParams[] = $oldestID;
        } else if (!is_null($newestID)) {
            $query .= ",sinceid=? ";
            $updParams[] = $newestID;
        }
        $query .= "WHERE twitterid=? AND accesstoken=? AND accesstokensecret=?";
        array_push($updParams, $userRow['twitterid'],
                $userRow['accesstoken'], $userRow['accesstokensecret']);

        $stmt = CoreDB::getConnection()->prepare($query);
        try {
            $stmt->execute($updParams);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to update user tweet parameters: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function revalidateScheduledRetweetsForAllUsers() {
        $selectQuery = "SELECT * FROM users WHERE twitterid IN (SELECT retweetingusertwitterid FROM scheduledretweets)";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get users to revalidate scheduled tweets for from DB: " . print_r($e, true));
            return false;
        }
        $totalRemovedTweetCount = 0;
        while ($row = $selectStmt->fetch()) {
            $totalRemovedTweetCount += self::revalidateScheduledRetweetsForUser($row);
        }
        if ($totalRemovedTweetCount > 0) {
            self::$logger->info("Removed $totalRemovedTweetCount invalid tweets from scheduled retweets.");
        }
    }

    public static function revalidateScheduledRetweetsForUser($userRow) {
        $selectQuery = "SELECT tweetid FROM scheduledretweets WHERE retweetingusertwitterid=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$userRow['twitterid']]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get scheduled retweets for user to revalidate from DB: " . print_r($e, true));
            return false;
        }
        $i = 0;
        $tweetIDsToValidate = "";
        $errorEncountered = false;
        while ($row = $selectStmt->fetch()) {
            $tweetID = $row['tweetid'];
            $tweetIDsToValidate .= $tweetID .= ",";
            $i++;
            if ($i === 100) {
                $results = self::revalidateTweets($tweetIDsToValidate, $userRow);
                if (!is_array($results)) {
                    $errorEncountered = true;
                    break;
                }
                $removedTweets = $results['removedtweets'];
                $totalRemovedTweetCount += count($removedTweets);
                $validatedTweets = $results['validatedtweets'];
                $validatedTweetCount = count($validatedTweets);
                if ($validatedTweetCount > 0) {
                    $validatedTweetsString = "";
                    $updateValidatedTweetsQuery = "UPDATE scheduledretweets SET verified=? WHERE tweetid IN (?";
                    for ($j = 0; $j < $validatedTweetCount - 1; $j++) {
                        $updateValidatedTweetsQuery .= ",?";
                    }
                    $validatedTweetsString .= ")";
                    array_unshift($validatedTweets, "Y");
                    $updateStmt = CoreDB::getConnection()->prepare($updateValidatedTweetsQuery);
                    try {
                        $updateStmt->execute($validatedTweets);
                    } catch (\PDOException $e) {
                        self::$logger->error("Failed to update verification status for scheduled retweets: " . print_r($e, true)
                                . " Validated tweets array: " . print_r($validatedTweets, true));
                        return false;
                    }
                }

                $i = 0;
                $tweetIDsToValidate = "";
            }
        }
        if ($errorEncountered === false && strlen($tweetIDsToValidate) > 0) {
            $results = self::revalidateTweets($tweetIDsToValidate, $userRow);
            if (is_array($results)) {
                $removedTweets = $results['removedtweets'];
                $totalRemovedTweetCount += count($removedTweets);
            }
        }
        return $totalRemovedTweetCount;
    }

    public static function revalidateOldTweets() {
        $now = date("Y-m-d H:i:s");
        $selectQuery = "SELECT tweetid FROM tweets WHERE (nextrevalidationdate IS NULL OR nextrevalidationdate <= ?) LIMIT 15000";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$now]);
        } catch (\PDOException $e) {
            self::$logger->error("Failed to get tweets to revalidate from DB: " . print_r($e, true));
            return;
        }
        $i = 0;
        $tweetIDsToValidate = "";
        $totalRemovedTweetCount = 0;
        $errorEncountered = false;
        while ($row = $selectStmt->fetch()) {
            $tweetID = $row['tweetid'];
            $tweetIDsToValidate .= $tweetID .= ",";
            $i++;
            if ($i === 100) {
                $results = self::revalidateTweets($tweetIDsToValidate, APIKeys::bearer_token);
                if (!is_array($results)) {
                    $errorEncountered = true;
                    break;
                }
                $removedTweets = $results["removedtweets"];
                $totalRemovedTweetCount += count($removedTweets);
                $i = 0;
                $tweetIDsToValidate = "";
            }
        }
        if ($errorEncountered === false && strlen($tweetIDsToValidate) > 0) {
            $results = self::revalidateTweets($tweetIDsToValidate, APIKeys::bearer_token);
            if (is_array($results)) {
                $removedTweets = $results['removedtweets'];
                $totalRemovedTweetCount += count($removedTweets);
            }
        }
        self::$logger->info("Removed $totalRemovedTweetCount invalid tweets from database.");
        return true;
    }

    private static function revalidateTweets($tweetIDsToValidate, $authParams) {
        $removedTweets = [];
        $validatedTweets = [];
        $tweetIDsToValidate = substr($tweetIDsToValidate, 0, -1);
        $params['ids'] = $tweetIDsToValidate;
        $response = Core::queryTwitterUserAuth("tweets", "tweets", "GET", $params, $authParams);
        $queryResult = $response[0];
        $twitterResponseStatus = $response[1];
        if ($twitterResponseStatus->getHttpCode() != 200) {
            self::$logger->error("Failed to revalidate tweets, Twitter returned an error: " . print_r($twitterResponseStatus, true));
            self::$logger->error("Tweet ID parameters: $tweetIDsToValidate");
            return $twitterResponseStatus->getHttpCode();
        }
        $tweetIDsToValidate = "";
        if (isset($queryResult->data)) {
            $data = $queryResult->data;
            $updateStmt = CoreDB::getConnection()->prepare("UPDATE tweets SET nextrevalidationdate=? WHERE tweetid=?");
            foreach ($data as $tweet) {
                $oneMonthFromNow = date("Y-m-d H:i:s", strtotime("+1 month"));
                $tweetID = $tweet->id;
                try {
                    $updateStmt->execute([$oneMonthFromNow, $tweetID]);
                } catch (\PDOException $e) {
                    self::$logger->error("Failed to select tweet from DB, cannot update artist. PDO error: " . $e->getMessage()
                            . " Twitter error body: " . print_r($e, true));
                    continue;
                }
                $validatedTweets[] = $tweetID;
            }
        }
        if (isset($queryResult->errors)) {
            $errors = $queryResult->errors;
            foreach ($errors as $error) {
                if ($error->title === "Authorization Error" && stripos($error->detail, "not authorized") !== false) {
                    // Tweet is protected: suspend this artist from being used by ArtRetweeter, remove all scheduled retweets for them
                    $tweetSelectStmt = CoreDB::getConnection()->prepare("SELECT * FROM tweets WHERE tweetid=?");
                    try {
                        $tweetSelectStmt->execute([$error->resource_id]);
                    } catch (\PDOException $e) {
                        self::$logger->error("Failed to select tweet from DB, cannot update artist: " . print_r($error, true));
                        continue;
                    }
                    $tweetRow = $tweetSelectStmt->fetch();
                    if ($tweetRow === false) {
                        self::$logger->error("Could not find tweet row in DB for tweet ID " . $error->resource_id . " : " . print_r($error, true));
                        continue;
                    }
                    $updateStmt = CoreDB::getConnection()->prepare("UPDATE artists SET protected=? WHERE twitterid=?");
                    try {
                        $updateStmt->execute(["Y", $tweetRow['usertwitterid']]);
                    } catch (\PDOException $e) {
                        self::$logger->error("Failed to update artist protected status in DB: " . print_r($error, true));
                        continue;
                    }
                    $updateStmt = CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE tweetid=?");
                    try {
                        $updateStmt->execute([$error->resource_id]);
                    } catch (\PDOException $e) {
                        self::$logger->error("Failed to delete scheduled retweets for tweet ID "
                                . $error->resource_id . ". PDO error: " . $e->getMessage() . " Twitter error body: " . print_r($error, true));
                        continue;
                    }
                } else if (stripos($error->title, "not found") !== false || stripos($error->detail, "not found") !== false) {
                    // Tweet has been deleted or does not exist: remove it from database, remove all scheduled retweets of it
                    $deleteStmt = CoreDB::getConnection()->prepare("DELETE FROM scheduledretweets WHERE tweetid=?");
                    try {
                        $deleteStmt->execute([$error->resource_id]);
                    } catch (\PDOException $e) {
                        self::$logger->error("Failed to delete scheduled tweets from database. PDO error: " . $e->getMessage()
                                . " Twitter error body: " . print_r($error, true));
                        continue;
                    }
                    $deleteStmt = CoreDB::getConnection()->prepare("DELETE FROM tweets WHERE tweetid=?");
                    try {
                        $deleteStmt->execute([$error->resource_id]);
                    } catch (\PDOException $e) {
                        self::$logger->error("Failed to delete tweet from database. PDO error: " . $e->getMessage()
                                . " Twitter error body: " . print_r($error, true));
                        continue;
                    }
                    $removedTweets[] = $error->resource_id;
                } else {
                    self::$logger->error("Unidentified tweet status when revalidating tweets: " . print_r($error, true));
                }
            }
        }
        return array("validatedtweets" => $validatedTweets, "removedtweets" => $removedTweets);
    }

}

TweetManager::initialiseLogger();
