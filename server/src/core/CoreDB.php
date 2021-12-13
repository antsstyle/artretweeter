<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Credentials\DB;

class CoreDB {

    const options = [
        \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
    ];

    public static $databaseConnection;

    public static function initialiseConnection() {
        try {
            $params = "mysql:host=" . DB::server_name . ";dbname=" . DB::database . ";port=" . DB::port;
            CoreDB::$databaseConnection = new \PDO($params, DB::username, DB::password, CoreDB::options);
        } catch (Exception $e) {
            error_log("Failed to create database connection.");
            echo "Failed to create database connection.";
            exit();
        }

        CoreDB::$databaseConnection->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);
    }

}

CoreDB::initialiseConnection();

