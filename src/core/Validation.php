<?php

namespace Antsstyle\ArtRetweeter\Core;

use Antsstyle\ArtRetweeter\Credentials\APIKeys;
use Antsstyle\ArtRetweeter\Core\CachedVariables;
use Antsstyle\ArtRetweeter\DB\CoreDB;
use Antsstyle\ArtRetweeter\Core\LogManager;

class Validation {

    private static $logger;

    public static function initialiseLogger() {
        self::$logger = LogManager::getLogger(self::class);
    }

    public static function getPayPalAccessToken() {
        $accessTokenExpiryDate = CoreDB::getCachedVariable(CachedVariables::PAYPAL_ACCESS_TOKEN_EXPIRY_DATE);
        $accessToken = CoreDB::getCachedVariable(CachedVariables::PAYPAL_ACCESS_TOKEN);
        if (is_null($accessToken)) {
            return false;
        } else if ($accessToken !== false) {
            if (is_null($accessTokenExpiryDate)) {
                return false;
            } else if ($accessTokenExpiryDate !== false) {
                $now = time();
                if (strtotime($accessTokenExpiryDate) > $now) {
                    return $accessToken;
                }
            }
        }

        $endpoint = APIKeys::paypal_sandbox_oauth_access_token_url;
        $curlHandle = curl_init($endpoint);
        curl_setopt($curlHandle, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($curlHandle, CURLOPT_HTTPHEADER, array(
            'Accept: application/json',
            'Accept-Language: en_US'
        ));
        $postFields = http_build_query(array(
            'grant_type' => 'client_credentials',
        ));
        curl_setopt($curlHandle, CURLOPT_POST, 1);
        curl_setopt($curlHandle, CURLOPT_USERPWD, APIKeys::paypal_sandbox_client_id . ":" . APIKeys::paypal_sandbox_client_secret);
        curl_setopt($curlHandle, CURLOPT_POSTFIELDS, $postFields);
        $content = curl_exec($curlHandle);
        error_log("Content: $content");
        $json = json_decode($content);
        $token = $json['access_token'];
        $expires_in = json['expires_in'];
        $expiryDate = date("Y-m-d H:i:s", strtotime("+$expires_in seconds"));
        CoreDB::updateCachedVariable(CachedVariables::PAYPAL_ACCESS_TOKEN, $token);
        CoreDB::updateCachedVariable(CachedVariables::PAYPAL_ACCESS_TOKEN_EXPIRY_DATE, $expiryDate);
        return $token;
    }

    public static function verifyPatreonWebhook($data, $headers) {
        $signatureHeader = $headers['X-Patreon-Signature'];
        $webhookHash = hash_hmac('md5', $data, APIKeys::patreon_webhook_secret);
        if (strtolower($webhookHash) === strtolower($signatureHeader)) {
            return true; // Verification succeeded
        } else {
            self::$logger->error("Patreon webhook verification failure.");
            return false; // Verification failed
        }
    }

    public static function verifyPayPalWebhook($data, $headers) {
        $accessToken = Validation::getPayPalAccessToken();
        if ($accessToken === false) {
            self::$logger->error("Unable to get paypal access token - cannot verify webhook");
            return false;
        }
        $headers = array_change_key_case($headers, CASE_UPPER);
        $json = json_decode($data, true);
        $crc32 = crc32($data);
        $signatureHeader = $headers['PAYPAL-TRANSMISSION-SIG'];
        $algoHeader = $headers['PAYPAL-AUTH-ALGO'];
        $certHeader = $headers['PAYPAL-CERT-URL'];
        $transmissionID = $headers['PAYPAL-TRANSMISSION-ID'];
        $transmissionTime = $headers['PAYPAL-TRANSMISSION-TIME'];
        $webhookID = APIKeys::paypal_sandbox_webhook_id;
        /* $postFields = http_build_query(array(
          'auth_algo' => $algoHeader,
          'cert_url' => $certHeader,
          'transmission_id' => $transmissionID,
          'transmission_time' => $transmissionTime,
          'transmission_sig' => $signatureHeader,
          'webhook_id' => APIKeys::paypal_sandbox_webhook_id,
          'webhook_event' => $json,
          )); */
        error_log("JSON: " . json_encode($json));
        $postFields = json_encode(array(
            'transmission_id' => $transmissionID,
            'transmission_time' => $transmissionTime,
            'cert_url' => $certHeader,
            'auth_algo' => $algoHeader,
            'transmission_sig' => $signatureHeader,
            'webhook_id' => APIKeys::paypal_sandbox_webhook_id,
            'webhook_event' => $json,
                ), JSON_UNESCAPED_SLASHES);
        error_log("POSTFIELDS: " . print_r($postFields, true));
        $curlHandle = curl_init("https://api-m.sandbox.paypal.com/v1/notifications/verify-webhook-signature");
        curl_setopt($curlHandle, CURLOPT_PORT, 443);
        curl_setopt($curlHandle, CURLOPT_SSLVERSION, 3);
        curl_setopt($curlHandle, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($curlHandle, CURLOPT_SSL_VERIFYPEER, true);
        curl_setopt($curlHandle, CURLOPT_SSL_VERIFYHOST, 2);
        curl_setopt($curlHandle, CURLOPT_HTTPHEADER, array(
            "Content-Type: application/json",
            "Authorization: Bearer $accessToken"
        ));
        curl_setopt($curlHandle, CURLOPT_POST, 1);
        curl_setopt($curlHandle, CURLOPT_POSTFIELDS, $postFields);
        $content = curl_exec($curlHandle);
        error_log("CONTENT: " . print_r($content, true));

        /*
          $headers = array_change_key_case($headers, CASE_UPPER);
          $json = json_decode($data);
          $curlHandle = curl_init($headers['PAYPAL-CERT-URL']);
          $options = array(
          CURLOPT_RETURNTRANSFER => true, // return web page
          CURLOPT_HEADER => false, // don't return headers
          CURLOPT_FOLLOWLOCATION => true, // follow redirects
          CURLOPT_ENCODING => "", // handle all encodings
          CURLOPT_USERAGENT => "spider", // who am i
          CURLOPT_AUTOREFERER => true, // set referer on redirect
          CURLOPT_CONNECTTIMEOUT => 120, // timeout on connect
          CURLOPT_TIMEOUT => 120, // timeout on response
          CURLOPT_MAXREDIRS => 10, // stop after 10 redirects
          );
          curl_setopt_array($curlHandle, $options);
          $signatureHeader = $headers['PAYPAL-TRANSMISSION-SIG'];
          $content = curl_exec($curlHandle);
          $publicKey = openssl_pkey_get_public($content);
          $details = openssl_pkey_get_details($publicKey);
          error_log("Open SSL public key: $publicKey");
          $webhookID = APIKeys::paypal_sandbox_webhook_id;
          $transmissionID = $headers['PAYPAL-TRANSMISSION-ID'];
          $transmissionTime = $headers['PAYPAL-TRANSMISSION-TIME'];
          error_log("Json ID: " . $json->id);
          error_log("Webhook ID: " . $webhookID);
          $validationString1 = $transmissionID . '|' . $transmissionTime . '|' . $json->id . '|' . $crc32;
          $validationString2 = $transmissionID . '|' . $transmissionTime . '|' . $webhookID . '|' . $crc32;
          error_log("Validation string1: $validationString1");
          error_log("Validation string2: $validationString2");
          error_log("Key: " . $details['key']);
          error_log("Signature header: " . $signatureHeader);
          $verification = openssl_verify($validationString1, $signatureHeader, $details['key'], "sha256WithRSAEncryption");
          $verification2 = openssl_verify($validationString1, base64_decode($signatureHeader), $details['key'], "sha256WithRSAEncryption");
          $verification3 = openssl_verify($validationString2, $signatureHeader, $details['key'], "sha256WithRSAEncryption");
          $verification4 = openssl_verify($validationString2, base64_decode($signatureHeader), $details['key'], "sha256WithRSAEncryption");
          error_log("Verification1: " . $verification);
          error_log("Verification2: " . $verification2);
          error_log("Verification3: " . $verification3);
          error_log("Verification4: " . $verification4);
          if ($verification === 0) {
          self::$logger->error("PayPal verification failed.");
          return false;
          } else if ($verification === -1) {
          self::$logger->error("PayPal verification error.");
          return false;
          }

          return true; */
    }

}

Validation::initialiseLogger();
