/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

/**
 *
 * @author antss
 */
public class RequestToken {
    
    private String token;
    private String tokenSecret;
    private Boolean callbackConfirmed;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public Boolean getCallbackConfirmed() {
        return callbackConfirmed;
    }

    public void setCallbackConfirmed(Boolean callbackConfirmed) {
        this.callbackConfirmed = callbackConfirmed;
    }

    public RequestToken(String token, String tokenSecret, Boolean callbackConfirmed) {
        this.token = token;
        this.tokenSecret = tokenSecret;
        this.callbackConfirmed = callbackConfirmed;
    }
    
}
