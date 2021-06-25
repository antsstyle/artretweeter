/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import com.antsstyle.artretweeter.enumerations.StatusCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class OperationResult {

    private static final Logger LOGGER = LogManager.getLogger(OperationResult.class);

    private ClientResponse clientResponse;
    private ServerResponse serverResponse;
    private TwitterResponse twitterResponse;

    public StatusCode getErrorCode() {
        if (clientResponse != null && !clientResponse.wasSuccessful()) {
            return clientResponse.statusCode;
        }
        if (serverResponse != null && !serverResponse.wasSuccessful()) {
            return serverResponse.statusCode;
        }
        if (twitterResponse != null && !twitterResponse.wasSuccessful()) {
            return twitterResponse.statusCode;
        }
        return null;
    }

    public ClientResponse getClientResponse() {
        return clientResponse;
    }

    public OperationResult setClientResponse(ClientResponse clientResponse) {
        this.clientResponse = clientResponse;
        return this;
    }

    public Boolean wasSuccessful() {
        if (serverResponse != null && serverResponse.getStatusCode().isErrorStatus()) {
            return false;
        }
        if (twitterResponse != null && twitterResponse.getStatusCode().isErrorStatus()) {
            return false;
        }
        if (clientResponse != null && clientResponse.getStatusCode().isErrorStatus()) {
            return false;
        }
        return true;
    }

    public ServerResponse getServerResponse() {
        return serverResponse;
    }

    public OperationResult setServerResponse(ServerResponse serverResponse) {
        this.serverResponse = serverResponse;
        return this;
    }

    public TwitterResponse getTwitterResponse() {
        return twitterResponse;
    }

    public OperationResult setTwitterResponse(TwitterResponse twitterResponse) {
        this.twitterResponse = twitterResponse;
        return this;
    }

}
