<?php

namespace Antsstyle\ArtRetweeter\DB;

use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\Core\LogManager;

class AdminDB {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function getAllAdminTwitterIDs() {
        $selectQuery = "SELECT * FROM adminaccounts";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute();
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get all admin info: " . print_r($e, true));
            return null;
        }

        $returnArray = [];
        while ($row = $selectStmt->fetch()) {
            $returnArray[] = $row['twitterid'];
        }
        return $returnArray;
    }

    public static function getAdminInfo($username) {
        $selectQuery = "SELECT * FROM adminaccounts WHERE username=?";
        $selectStmt = CoreDB::getConnection()->prepare($selectQuery);
        try {
            $selectStmt->execute([$username]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to get admin info: " . print_r($e, true));
            return null;
        }
        return $selectStmt->fetch();
    }

    public static function resetAdminUserLoginAttempts($username) {
        $updateQuery = "UPDATE adminaccounts SET failedloginattempts=0 WHERE username=?";
        $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
        try {
            $updateStmt->execute([$username]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to reset admin user login attempts: " . print_r($e, true));
            return false;
        }
        return true;
    }

    public static function incrementAdminUserLoginAttempts($username) {
        $updateQuery = "UPDATE adminaccounts SET failedloginattempts=failedloginattempts+1 WHERE username=?";
        $updateStmt = CoreDB::getConnection()->prepare($updateQuery);
        try {
            $updateStmt->execute([$username]);
        } catch (\PDOException $e) {
            self::$logger->critical("Failed to reset admin user login attempts: " . print_r($e, true));
            return false;
        }
    }

}

AdminDB::initialiseLogger();
