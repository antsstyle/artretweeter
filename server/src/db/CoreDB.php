<?php

namespace Antsstyle\ArtRetweeter\DB;

use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Credentials\DB;

class CoreDB {

    const options = [
        \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
    ];

    private static $databaseConnection;
    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function getConnection() {
        return self::$databaseConnection;
    }

    public static function initialiseConnection() {
        try {
            $params = "mysql:host=" . DB::server_name . ";dbname=" . DB::database . ";port=" . DB::port . ";charset=UTF8MB4";
            self::$databaseConnection = new \PDO($params, DB::username, DB::password, self::options);
        } catch (\Exception $e) {
            self::$logger->error("Failed to create database connection: " . print_r($e, true));
            exit();
        }
        self::$databaseConnection->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);
    }

    public static function checkNFTCryptoBlockerCentralDB() {
        $nftCryptoBlockerDBConn = null;
        try {
            $params = "mysql:host=" . DB::server_name . ";dbname=" . DB::nftcryptoblocker_database . ";port=" . DB::port . ";charset=UTF8MB4";
            $nftCryptoBlockerDBConn = new \PDO($params, DB::username, DB::password, self::options);
        } catch (\Exception $e) {
            self::$logger->error("Failed to create database connection: " . print_r($e, true));
            return null;
        }
        $nftCryptoBlockerDBConn->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);

        $selectQuery = "SELECT dateadded FROM nftcryptoblockercentralisedblocklist ORDER BY dateadded DESC LIMIT 5";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        try {
            $selectStmt->execute();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to check NFT Artist & Cryptobro Blocker's DB: " . print_r($e, true));
            return null;
        }

        $latestDates = [];
        while ($row = $selectStmt->fetch()) {
            $latestDates[] = $row['dateadded'];
        }

        $insertQuery = "INSERT INTO nftcryptoblockercentralisedblocklist (blockableusertwitterid,matchedfiltertype,matchedfiltercontent,"
                . "dateadded,addedfrom,markedfordeletion,markedfordeletiondate,markedfordeletionreason,followercount,twitterhandle,matchcount,"
                . "lastcheckeddate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        $insertStmt = self::$databaseConnection->prepare($insertQuery);
        $offset = 0;
        $rowCount = 1;
        try {
            self::$databaseConnection->beginTransaction();
            while ($rowCount > 0) {
                $stmt = $nftCryptoBlockerDBConn->prepare("SELECT * FROM centralisedblocklist ORDER BY dateadded DESC LIMIT 1000 OFFSET $offset");
                try {
                    $stmt->execute();
                } catch (\PDOException $e) {
                    self::$logger->error("Failed to get list of centralised blocklist entries: " . print_r($e, true));
                }

                $rowCount = $stmt->rowCount();
                while ($row = $stmt->fetch()) {
                    $dateAdded = $row['dateadded'];
                    foreach ($latestDates as $latestDate) {
                        if ($latestDate >= $dateAdded) {
                            self::$databaseConnection->commit();
                            return;
                        }
                    }
                    $insertStmt->execute([$row['blockableusertwitterid'], $row['matchedfiltertype'], $row['matchedfiltercontent'], $row['dateadded'],
                        $row['addedfrom'], $row['markedfordeletion'], $row['markedfordeletiondate'], $row['markedfordeletionreason'],
                        $row['followercount'], $row['twitterhandle'], $row['matchcount'], $row['lastcheckeddate']]);
                }
            }
            self::$databaseConnection->commit();
        } catch (\PDOException $e) {
            self::$logger->error("Failed to commit new NFT/crypto banned entries to DB: " . print_r($e, true));
            return null;
        }
        return true;
    }

    public static function getCachedVariable($name) {
        $selectQuery = "SELECT * FROM cachedvariables WHERE name=?";
        $selectStmt = self::$databaseConnection->prepare($selectQuery);
        try {
            $selectStmt->execute([$name]);
        } catch (\PDOException $e) {
            self::$logger->critical("Database error retrieving cached variable with name: $name. PDO error: " . print_r($e, true));
            return null;
        }

        $row = $selectStmt->fetch();
        if ($row === false) {
            self::$logger->error("Cached variable with name: $name does not exist.");
            return false;
        }
        return $row['value'];
    }

    public static function updateCachedVariable($name, $value) {
        $row = self::getCachedVariable($name);
        if ($row === false) {
            $insertQuery = "INSERT INTO cachedvariables (name,value) VALUES (?,?)";
            $insertStmt = self::$databaseConnection->prepare($insertQuery);
            try {
                $insertStmt->execute([$name, $value]);
            } catch (\PDOException $e) {
                self::$logger->error("Could not insert cached variable with name: $name, value: $value. " . print_r($e, true));
                return false;
            }
        } else {
            $updateQuery = "UPDATE cachedvariables SET value=? WHERE name=?";
            $updateStmt = self::$databaseConnection->prepare($updateQuery);
            try {
                $updateStmt->execute([$value, $name]);
            } catch (\PDOException $e) {
                self::$logger->error("Could not insert cached variable with name: $name, value: $value. " . print_r($e, true));
                return false;
            }
        }
        return true;
    }

    public static function updateUserArtistEligibleTweetsCache($userTwitterID, $cacheArray) {
        $cachedResult = json_encode($cacheArray);
        $insertQuery = "INSERT INTO userartisteligibletweetscache (usertwitterid,cachedresult) VALUES (?,?) "
                . "ON DUPLICATE KEY UPDATE cachedresult=?";
        $insertStmt = self::$databaseConnection->prepare($insertQuery);
        try {
            $insertStmt->execute([$userTwitterID, $cachedResult, $cachedResult]);
        } catch (\PDOException $e) {
            self::$logger->error("Unable to update user artist eligible tweets cache for user twitter ID "
                    . "$userTwitterID: " . print_r($e, true));
        }
    }

    public static function getLatestMetricsTypeID() {
        $metricTypesSelectQuery = "SELECT * FROM retrievalmetrics";
        $metricTypesSelectStmt = self::$databaseConnection->prepare($metricTypesSelectQuery);
        try {
            $metricTypesSelectStmt->execute();
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get metrics type, cannot calculate metrics: " . print_r($e, true));
            return null;
        }

        while ($row = $metricTypesSelectStmt->fetch()) {
            if ($row['description'] === "Latest Metrics") {
                $metricID = $row['id'];
            }
        }
        if (!isset($metricID)) {
            self::$logger->critical("Could not find ID for latest metrics retrieval metric type - cannot compute adaptive analytics.");
            return null;
        }
        return $metricID;
    }

}

CoreDB::initialiseLogger();
CoreDB::initialiseConnection();

