<?php

namespace Antsstyle\ArtRetweeter\Core;

class OAuth {

    public static function base64url_encode($plainText) {
        $base64 = base64_encode($plainText);
        $base64 = trim($base64, "=");
        $base64url = strtr($base64, '+/', '-_');
        return ($base64url);
    }

    public static function generatePKCEVerifierAndChallenge() {
        $random = bin2hex(openssl_random_pseudo_bytes(128));
        $verifier = OAuth::base64url_encode(pack('H*', $random));
        $challenge = OAuth::base64url_encode(pack('H*', hash('sha256', $verifier)));
        return [$verifier, $challenge];
    }

}
