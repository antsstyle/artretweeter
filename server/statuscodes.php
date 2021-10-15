<?php

namespace ArtRetweeter;

class StatusCodes {
    const TWITTER_API_ERROR = -8;
    const SERVER_ERROR = -7;
    const INVALID_INPUT = -6;
    const DATABASE_ERROR = -5;
    const INVALID_ACCESS_TOKEN = -4;
    const TWITTER_RATE_LIMIT_EXCEEDED = -3;
    const ARTRETWEETER_RATE_LIMIT_EXCEEDED = -2;
    const USER_BANNED = -1;
    const QUERY_OK = 0;
    
    const errorArray = array (
        TWITTER_API_ERROR => "Twitter API returned an error.",
        SERVER_ERROR => "Internal server error.",
        INVALID_INPUT => "Invalid input or parameters.",
        DATABASE_ERROR => "A server database error occurred.",
        INVALID_ACCESS_TOKEN => "Invalid or expired access token.",
        TWITTER_RATE_LIMIT_EXCEEDED => "Twitter rate limit exceeded.",
        ARTRETWEETER_RATE_LIMIT_EXCEEDED => "ArtRetweeter rate limit exceeded.",
        USER_BANNED => "This user is banned from ArtRetweeter.",
        QUERY_OK => "Query OK."
    );
    
    function getStatusMessage($statusCode) {
        return errorArray[$statusCode];
    }
}