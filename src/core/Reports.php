<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\Core\Users;
use Antsstyle\ArtRetweeter\Credentials\Email;
use Antsstyle\ArtRetweeter\Core\LogManager;
use PHPMailer\PHPMailer\PHPMailer;

class Reports {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function generateUserEligibleTweetsReport() {
        
    }

    public static function generateArtistEligibleTweetsReport() {
        
    }

    public static function generateUserArtistEligibleTweetsReport() {
        
    }

    public static function sendReportEmail($subject, $body) {
        $mail = new PHPMailer;
        $mail->isSMTP();
        $mail->SMTPDebug = 3;
        $mail->Host = Email::OUTGOING_SERVER;
        $mail->Port = Email::SMTP_OUTGOING_PORT;
        $mail->SMTPAuth = true;
        $mail->SMTPSecure = 'tls';
        $mail->Username = Email::ADMIN_EMAIL_USERNAME;
        $mail->Password = Email::ADMIN_EMAIL_PASSWORD;
        $mail->setFrom(Email::ADMIN_EMAIL_USERNAME, Email::ADMIN_FROM_NAME);
        $mail->addReplyTo(Email::ADMIN_EMAIL_USERNAME, Email::ADMIN_REPLY_TO_NAME);
        $mail->addAddress(Email::REPORT_RECEIVER_EMAIL_ADDRESS, 'Receiver Name');
        $mail->Subject = $subject;
        $mail->Body = $body;
        if (!$mail->send()) {
            self::$logger->error('Mailer Error: ' . $mail->ErrorInfo);
        } else {
            self::$logger->info('The email message was sent.');
        }
    }

}

Reports::initialiseLogger();
