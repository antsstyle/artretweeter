<?php

namespace Antsstyle\ArtRetweeter\CredentialsTemplate;

class Email {
    
    const INCOMING_SERVER = "<your incoming mail server";
    const OUTGOING_SERVER = "<your outgoing mail server>";
    
    const INCOMING_POP3_PORT = "<your incoming POP3 port>";
    const INCOMING_IMAP_PORT = "<your incoming IMAP port>";
    
    const SMTP_OUTGOING_PORT = "<your outgoing SMTP port, usually 587 for TLS>";
    
    const ADMIN_EMAIL_USERNAME = "<admin email address>";
    const ADMIN_EMAIL_PASSWORD = "<admin email password>";
    const ADMIN_FROM_NAME = "<admin display name>";
    const ADMIN_REPLY_TO_NAME = "<admin display name>";
    
    const REPORT_RECEIVER_EMAIL_ADDRESS = "<email you want reports sent to>";
}