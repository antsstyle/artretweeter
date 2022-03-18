<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\LogManager;

class MiscTools {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function shiftString($string, $shiftCount) {
        if ($shiftCount == 0) {
            return $string;
        }
        if ($shiftCount > 24 || $shiftCount < -24) {
            return $string;
        }
        if ($shiftCount < 0) {
            while ($shiftCount < 0) {
                $string = $string[strlen($string) - 1] . substr($string, 0, strlen($string) - 1);
                $shiftCount++;
            }
        } else {
            while ($shiftCount > 0) {
                $string = substr($string, 1, strlen($string)) . $string[0];
                $shiftCount--;
            }
        }
        return $string;
    }

}

MiscTools::initialiseLogger();