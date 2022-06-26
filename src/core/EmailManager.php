<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Core\LogManager;
use Antsstyle\ArtRetweeter\Credentials\EmailCredentials;
use PHPMailer\PHPMailer\PHPMailer;

class EmailManager {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function getMailer() {
        $mail = new PHPMailer;
        $mail->isSMTP();
        $mail->SMTPDebug = 0;
        $mail->Host = EmailCredentials::OUTGOING_SERVER;
        $mail->Port = EmailCredentials::SMTP_OUTGOING_PORT;
        $mail->SMTPAuth = true;
        $mail->SMTPSecure = 'tls';
        $mail->Username = EmailCredentials::ADMIN_EMAIL_USERNAME;
        $mail->Password = EmailCredentials::ADMIN_EMAIL_PASSWORD;
        $mail->setFrom(EmailCredentials::ADMIN_EMAIL_USERNAME, EmailCredentials::ADMIN_FROM_NAME);
        $mail->addReplyTo(EmailCredentials::ADMIN_EMAIL_USERNAME, EmailCredentials::ADMIN_REPLY_TO_NAME);
        return $mail;
    }

    public static function sendPendingArtistSubmissionsEmail($pendingSubmissionsCount) {
        $mail = self::getMailer();
        $mail->addAddress(EmailCredentials::REPORT_RECEIVER_EMAIL_ADDRESS, 'Receiver Name');
        if (!is_null($pendingSubmissionsCount)) {
            $mail->Subject = "ArtRetweeter: $pendingSubmissionsCount artist submissions awaiting approval";
            $mail->Body = "<html>You have $pendingSubmissionsCount awaiting approval. Log into "
                    . "<a href=\"https://antsstyle.com/artretweeter/admin/login\">https://antsstyle.com/artretweeter/admin/login</a> to decide on them.</html>";
        } else {
            $mail->Subject = "ArtRetweeter: Error determining pending artist submissions count";
            $mail->Body = "ArtRetweeter was unable to determine how many artist submissions are pending approval.";
        }

        if (!$mail->send()) {
            self::$logger->error('Mailer Error: ' . $mail->ErrorInfo);
        } else {
            self::$logger->info('The email message was sent.');
        }
    }

}

EmailManager::initialiseLogger();
