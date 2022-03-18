<?php

namespace Antsstyle\ArtRetweeter\Core;

class TwitterResponseStatus {

    const HTTP_SERVICE_UNAVAILABLE = 503;
    const HTTP_INTERNAL_SERVER_ERROR = 500;
    const HTTP_NOT_FOUND = 404;
    const HTTP_FORBIDDEN = 403;
    const HTTP_BAD_CREDENTIALS = 401;
    const HTTP_BAD_REQUEST = 400;
    const HTTP_TOO_MANY_REQUESTS = 429;
    const HTTP_QUERY_OK = 200;
    const TWITTER_NO_USER_MATCHES_FOR_TERMS = 17;
    const TWITTER_COULD_NOT_AUTHENTICATE = 32;
    const TWITTER_USER_NOT_FOUND = 50;
    const TWITTER_USER_SUSPENDED = 63;
    const TWITTER_ACCOUNT_SUSPENDED = 64;
    const TWITTER_INVALID_ACCESS_TOKEN = 89;
    const TWITTER_OAUTH_CREDENTIALS_ERROR = 99;
    const TWITTER_OVER_CAPACITY = 130;
    const TWITTER_UNKNOWN_ERROR = 131;
    const TWITTER_AUTOMATED_REQUEST_ERROR = 226;
    const TWITTER_USER_ALREADY_UNMUTED = 272;
    const TWITTER_USER_ACCOUNT_LOCKED = 326;
    const ARTRETWEETER_RATE_LIMIT_ZERO = -2;
    const ARTRETWEETER_DATABASE_ERROR = -1;
    const ARTRETWEETER_QUERY_OK = 0;

    private $httpCode;
    private $twitterCode;
    private $message = null;
    private $rateLimitRemaining = null;
    private $rateLimitResetTime = null;
    private $requestBody;
    private $headers;
    private $twitterAPIPath;
    private $userTwitterID = null;

    public function __construct() {
        
    }

    public static function initialise() {
        return new TwitterResponseStatus();
    }

    public function getHttpCode() {
        return $this->httpCode;
    }

    public function setHttpCode($httpCode) {
        $this->httpCode = $httpCode;
        return $this;
    }

    public function getTwitterCode() {
        return $this->twitterCode;
    }

    public function setTwitterCode($twitterCode) {
        $this->twitterCode = $twitterCode;
        return $this;
    }

    public function getMessage() {
        return $this->message;
    }

    public function setMessage($message) {
        $this->message = $message;
        return $this;
    }

    public function getRateLimitRemaining() {
        return $this->rateLimitRemaining;
    }

    public function setRateLimitRemaining($rateLimitRemaining) {
        $this->rateLimitRemaining = $rateLimitRemaining;
        return $this;
    }

    public function getRateLimitResetTime() {
        return $this->rateLimitResetTime;
    }

    public function setRateLimitResetTime($rateLimitResetTime) {
        $this->rateLimitResetTime = $rateLimitResetTime;
        return $this;
    }

    public function getRequestBody() {
        return $this->requestBody;
    }

    public function setRequestBody($requestBody) {
        $this->requestBody = $requestBody;
        return $this;
    }

    public function getHeaders() {
        return $this->headers;
    }

    public function setHeaders($headers) {
        $this->headers = $headers;
        return $this;
    }

    public function getTwitterAPIPath() {
        return $this->twitterAPIPath;
    }

    public function setTwitterAPIPath($twitterAPIPath) {
        $this->twitterAPIPath = $twitterAPIPath;
        return $this;
    }

    public function getUserTwitterID() {
        return $this->userTwitterID;
    }

    public function setUserTwitterID($userTwitterID) {
        $this->userTwitterID = $userTwitterID;
        return $this;
    }

}
