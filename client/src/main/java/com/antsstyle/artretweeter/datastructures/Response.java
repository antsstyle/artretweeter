/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import com.antsstyle.artretweeter.enumerations.StatusCode;
import com.google.gson.JsonObject;

/**
 *
 * @author antss
 */
public abstract class Response {

    protected Integer httpStatusCode;
    protected StatusCode statusCode;
    protected String extraStatusMessage;
    protected JsonObject responseJSONObject;

    public Boolean wasSuccessful() {
        return !statusCode.isErrorStatus();
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public String getExtraStatusMessage() {
        return extraStatusMessage;
    }

    public void setExtraStatusMessage(String extraStatusMessage) {
        this.extraStatusMessage = extraStatusMessage;
    }

    public JsonObject getResponseJSONObject() {
        return responseJSONObject;
    }

    public void setResponseJSONObject(JsonObject responseJSONObject) {
        this.responseJSONObject = responseJSONObject;
    }

    public Object getReturnedObject() {
        return returnedObject;
    }

    public void setReturnedObject(Object returnedObject) {
        this.returnedObject = returnedObject;
    }
    protected Object returnedObject;

}
