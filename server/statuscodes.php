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
        StatusCodes::TWITTER_API_ERROR => "Twitter API returned an error.",
        StatusCodes::SERVER_ERROR => "Internal server error.",
        StatusCodes::INVALID_INPUT => "Invalid input or parameters.",
        StatusCodes::DATABASE_ERROR => "A server database error occurred.",
        StatusCodes::INVALID_ACCESS_TOKEN => "Invalid or expired access token.",
        StatusCodes::TWITTER_RATE_LIMIT_EXCEEDED => "Twitter rate limit exceeded.",
        StatusCodes::ARTRETWEETER_RATE_LIMIT_EXCEEDED => "ArtRetweeter rate limit exceeded.",
        StatusCodes::USER_BANNED => "This user is banned from ArtRetweeter.",
        StatusCodes::QUERY_OK => "Query OK."
    );
    
    function getStatusMessage($statusCode) {
        return StatusCodes::errorArray[$statusCode];
    }
}