<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\DB\UserDB;
use Antsstyle\ArtRetweeter\Core\Config;
use Antsstyle\ArtRetweeter\Core\LogManager;

class Session {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function regenerateSession($reload = false) {
        // This token is used by forms to prevent cross site forgery attempts
        if (!isset($_SESSION['nonce']) || $reload) {
            $_SESSION['nonce'] = bin2hex(openssl_random_pseudo_bytes(32));
        }
        if (!isset($_SESSION['IPaddress']) || $reload) {
            $_SESSION['IPaddress'] = filter_input(INPUT_SERVER, 'REMOTE_ADDR');
        }
        if (!isset($_SESSION['userAgent']) || $reload) {
            $_SESSION['userAgent'] = filter_input(INPUT_SERVER, 'HTTP_USER_AGENT');
        }

        // Set current session to expire in 1 minute
        $_SESSION['OBSOLETE'] = true;
        $_SESSION['EXPIRES'] = time() + 60;

        // Create new session without destroying the old one
        session_regenerate_id(false);

        // Grab current session ID and close both sessions to allow other scripts to use them
        $newSession = session_id();
        session_write_close();

        // Set session ID to the new one, and start it back up again
        session_id($newSession);
        session_start([
            'cookie_lifetime' => 0,
            'gc_maxlifetime' => 86400,
            'use_strict_mode' => 1,
            'cookie_secure' => "On",
        ]);

        // Don't want this one to expire
        unset($_SESSION['OBSOLETE']);
        unset($_SESSION['EXPIRES']);
    }

    public static function checkSession() {
        session_start([
            'cookie_lifetime' => 0,
            'gc_maxlifetime' => 86400,
            'use_strict_mode' => 1,
            'cookie_secure' => "On",
        ]);
        try {
            if ($_SESSION['OBSOLETE'] && ($_SESSION['EXPIRES'] < time())) {
                throw new \Exception('Attempt to use expired session.');
            }
            if ($_SESSION['IPaddress'] != filter_input(INPUT_SERVER, 'REMOTE_ADDR')) {
                throw new \Exception('IP Address mixmatch (possible session hijacking attempt).');
            }
            if ($_SESSION['userAgent'] != filter_input(INPUT_SERVER, 'HTTP_USER_AGENT')) {
                throw new \Exception('Useragent mixmatch (possible session hijacking attempt).');
            }
            if (!$_SESSION['OBSOLETE'] && mt_rand(1, 100) == 1) {
                self::regenerateSession();
            }
        } catch (\Exception $e) {
            self::$logger->error("Session error! " . print_r($e, true));
        }
    }

    public static function validateUserLoggedIn() {
        if (!$_SESSION['usertwitterid'] || !$_SESSION['artretweeterlogin']) {
            $errorURL = Config::HOMEPAGE_URL . "error";
            header("Location: $errorURL", true, 302);
            exit();
        }
        $userTwitterID = $_SESSION['usertwitterid'];

        $userInfo = UserDB::getUserInfo($userTwitterID);
        if ($userInfo === false) {
            $errorURL = Config::HOMEPAGE_URL . "error";
            header("Location: $errorURL", true, 302);
            exit();
        } else if ($userInfo === null) {
            $errorURL = Config::HOMEPAGE_URL . "error";
            header("Location: $errorURL", true, 302);
            exit();
        }
    }

}

Session::initialiseLogger();