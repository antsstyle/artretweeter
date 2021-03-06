<?php

namespace Antsstyle\ArtRetweeter\Core;

use Monolog\Logger;
use Monolog\Handler\StreamHandler;
use Monolog\Processor\IntrospectionProcessor;
use Monolog\Formatter\LineFormatter;

class LogManager {

    public static function getLogger($channel) {
        $logger = new Logger($channel);
        $processor = new IntrospectionProcessor();
        $logger->pushProcessor($processor);
        $debugHandler = new StreamHandler('logs/debug.log', Logger::DEBUG);
        $errorHandler = new StreamHandler('logs/error.log', Logger::ERROR);
        $criticalHandler = new StreamHandler("logs/critical.log", Logger::CRITICAL);
        $formatter = new LineFormatter("%datetime%: %channel%: %level_name%: %message% %context% %extra%\n", "Y-m-d H:i:s", true, true);
        $debugHandler->setFormatter($formatter);
        $errorHandler->setFormatter($formatter);
        $criticalHandler->setFormatter($formatter);
        $logger->pushHandler($debugHandler);
        $logger->pushHandler($errorHandler);
        $logger->pushHandler($criticalHandler);
        return $logger;
    }

}
