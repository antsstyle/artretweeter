/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.twitter;

/**
 *
 * @author antss
 */
public enum Endpoint {

    ACCESS_TOKEN("oauth/access_token", "requestaccesstokenurl", "POST", false),
    COLLECTIONS_CREATE("collections/create", "collectionscreateurl", "POST", true),
    COLLECTIONS_CURATE("collections/entries/curate", "collectionscurateurl", "POST", true),
    COLLECTIONS_DESTROY("collections/destroy", "collectionsdestroyurl", "POST", true),
    COLLECTIONS_ENTRIES("collections/entries", "collectionsentriesurl", "GET", true),
    COLLECTIONS_LIST("collections/list", "collectionslisturl", "GET", true),
    COLLECTIONS_SHOW("collections/show", "collectionsshowurl", "GET", true),
    REQUEST_TOKEN("oauth/request_token", "requesttokenurl", "POST", false),
    STATUSES_LOOKUP("statuses/lookup", "statuseslookupurl", "GET", true),
    STATUSES_RETWEET("statuses/retweet", "statusesretweeturl", "POST", true),
    STATUSES_SHOW("statuses/show", "statusesshowurl", "GET", true),
    STATUSES_UNRETWEET("statuses/unretweet", "statusesunretweeturl", "POST", true),
    USER_TIMELINE("statuses/user_timeline", "requestusertimelineurl", "GET", true);

    private final String endpointName;
    private final String propertyName;
    private final String httpRequestType;

    /*
    Note this property doesn't denote if the API requires a user auth, but rather if we do (to ensure the user
    is utilising their access token rate limits, not app-wide limits.)
     */
    private final Boolean requiresUserAuth;

    public Boolean getRequiresUserAuth() {
        return requiresUserAuth;
    }

    public String getHttpRequestType() {
        return httpRequestType;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    private Endpoint(String name, String propertyName, String httpRequestType, Boolean requiresUserAuth) {
        this.endpointName = name;
        this.propertyName = propertyName;
        this.httpRequestType = httpRequestType;
        this.requiresUserAuth = requiresUserAuth;
    }

}
