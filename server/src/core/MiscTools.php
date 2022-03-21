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

    public static function convertServerTimeStringToUserTime($timeString, $userHourOffset, $userMinuteOffset) {
        $userOffsetSeconds = ($userHourOffset * 3600) + ($userMinuteOffset * 60);
        $now = new \DateTime();
        $serverTimeZone = new \DateTimeZone(date_default_timezone_get());
        $serverOffsetSeconds = $serverTimeZone->getOffset($now);
        $timeDiffSeconds = $serverOffsetSeconds - $userOffsetSeconds;
        $timeString = date("Y-m-d H:i:s", strtotime($timeString) - $timeDiffSeconds);
        return $timeString;
    }

    public static function convertUserTimeStringToServerTime($timeString, $userHourOffset, $userMinuteOffset) {
        $userOffsetSeconds = ($userHourOffset * 3600) + ($userMinuteOffset * 60);
        $now = new \DateTime();
        $serverTimeZone = new \DateTimeZone(date_default_timezone_get());
        $serverOffsetSeconds = $serverTimeZone->getOffset($now);
        $timeDiffSeconds = $serverOffsetSeconds - $userOffsetSeconds;
        $timeString = date("Y-m-d H:i:s", strtotime($timeString) + $timeDiffSeconds);
        return $timeString;
    }

}

MiscTools::initialiseLogger();
