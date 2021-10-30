<?php

namespace ArtRetweeter;

require_once "credentials/db.php";

$options = [
    \PDO::ATTR_DEFAULT_FETCH_MODE => \PDO::FETCH_ASSOC,
];

try {
    $databaseConnection = new \PDO("mysql:host=$servername;dbname=$database;port=$port", $username, $password, $options);
} catch (Exception $e) {
    echo encodeStatusInformation(StatusCodes::DATABASE_ERROR, "Failed to create database connection.");
    exit();
}

$databaseConnection->setAttribute(\PDO::ATTR_ERRMODE, \PDO::ERRMODE_EXCEPTION);