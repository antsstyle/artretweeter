<?php

require dirname(__DIR__) . '/vendor/autoload.php';

use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\CoreDB;
use Antsstyle\ArtRetweeter\Core\Session;

Session::checkSession();

$username = htmlspecialchars($_POST['username']);
$password = htmlspecialchars($_POST['password']);

if ($username === "") {
    $location = Config::HOMEPAGE_URL . "admin/login?error=invalidusername";
    header("Location: $location", true, 302);
    exit();
}

if ($password === "") {
    $location = Config::HOMEPAGE_URL . "admin/login?error=invalidpassword";
    header("Location: $location", true, 302);
    exit();
}

$adminInfo = CoreDB::getAdminInfo($username);
if (is_null($adminInfo)) {
    $location = Config::HOMEPAGE_URL . "admin/login?error=dberror";
    header("Location: $location", true, 302);
    exit();
} else if ($adminInfo === false) {
    $location = Config::HOMEPAGE_URL . "admin/login?error=usernotfound";
    header("Location: $location", true, 302);
    exit();
}

if ($adminInfo['failedloginattempts'] >= 5) {
    $location = Config::HOMEPAGE_URL . "admin/login?error=userlocked";
    header("Location: $location", true, 302);
    exit();
}

$passwordHash = password_hash($password, PASSWORD_DEFAULT);
if (password_verify($password, $adminInfo['passwordhash'])) {
    error_log("Password verified.");
    $_SESSION['adminlogin'] = true;
    CoreDB::resetAdminUserLoginAttempts($username);
    $location = Config::HOMEPAGE_URL . "admin/admin";
    header("Location: $location", true, 302);
    exit();
} else {
    CoreDB::incrementAdminUserLoginAttempts($username);
    $location = Config::HOMEPAGE_URL . "admin/login?error=incorrectpassword";
    header("Location: $location", true, 302);
    exit();
}