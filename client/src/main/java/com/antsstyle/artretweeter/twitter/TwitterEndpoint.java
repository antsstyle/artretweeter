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
public enum TwitterEndpoint {

    COLLECTIONS_CREATE("collections/create", "POST", true),
    COLLECTIONS_DESTROY("collections/destroy", "POST", true),
    COLLECTIONS_ENTRIES("collections/entries", "GET", true),
    COLLECTIONS_ENTRIES_ADD("collections/entries/add", "POST", true),
    COLLECTIONS_ENTRIES_CURATE("collections/entries/curate", "POST", true),
    COLLECTIONS_ENTRIES_REMOVE("collections/entries/remove", "POST", true),
    COLLECTIONS_LIST("collections/list", "GET", true),
    COLLECTIONS_MOVE("collections/entries/move", "POST", true),
    COLLECTIONS_SHOW("collections/show", "GET", true),
    OAUTH_ACCESS_TOKEN("oauth/access_token", "POST", false),
    OAUTH_INVALIDATE_TOKEN("oauth/invalidate_token", "POST", true),
    OAUTH_REQUEST_TOKEN("oauth/request_token", "POST", false),
    STATUSES_LOOKUP("statuses/lookup", "GET", true),
    STATUSES_RETWEET("statuses/retweet", "POST", true),
    STATUSES_SHOW("statuses/show", "GET", true),
    STATUSES_UNRETWEET("statuses/unretweet", "POST", true),
    USER_TIMELINE("statuses/user_timeline", "GET", true);

    private final String endpointName;
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

    private TwitterEndpoint(String name, String httpRequestType, Boolean requiresUserAuth) {
        this.endpointName = name;
        this.httpRequestType = httpRequestType;
        this.requiresUserAuth = requiresUserAuth;
    }

}
