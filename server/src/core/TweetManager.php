<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\Core;
use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Antsstyle\ArtRetweeter\Core\CoreDB;
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
            return;
        }
        $selectQuery = "SELECT id FROM retrievalmetrics WHERE description=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        $success = $selectStmt->execute(["Latest Metrics"]);
        if (!$success) {
            self::$logger->error("Failed to get metrics type, cannot insert tweets.");
            return;
        }
        $latestMetricsID = $selectStmt->fetchColumn();
        if (!$latestMetricsID) {
            self::$logger->error("No metrics type found, cannot insert tweets.");
            return;
        }

        $selectQuery = "SELECT tweetid FROM tweets WHERE usertwitterid=? AND deletedflag=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        $success = $selectStmt->execute([$userTwitterID, "Y"]);
        if (!$success) {
            self::$logger->error("Failed to get delete-flagged tweets for user, cannot insert tweets.");
            return;
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
                . "WHERE automationenabled=? AND (nextusertimelinecheck IS NULL OR nextusertimelinecheck <= ?)";
        $stmt = CoreDB::getConnection()->prepare($query);
        $success = $stmt->execute(["Y", $now]);
        if (!$success) {
            self::$logger->error("Failed to acquire list of users to schedule automated retweets for, cannot continue.");
            return;
        }
        while ($row = $stmt->fetch()) {
            TweetManager::getAllNewTweetsForUser($row);
        }
    }

    public static function getAllNewTweetsForAllArtists() {
        $now = date("Y-m-d H:i:s");
        $query = "SELECT * FROM artists WHERE (nextcheckdate IS NULL OR nextcheckdate < ?)";
        $stmt = CoreDB::getConnection()->prepare($query);
        $success = $stmt->execute([$now]);
        if (!$success) {
            self::$logger->error("Failed to acquire list of artists to get tweets for, cannot continue.");
            return;
        }
        while ($row = $stmt->fetch()) {
            $errorCode = TweetManager::getAllNewTweetsForArtist($row);
            if ($errorCode === 429) {
                break;
            }
        }
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
        return $stmt->execute($updParams);
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
        return $stmt->execute($updParams);
    }

}

TweetManager::initialiseLogger();