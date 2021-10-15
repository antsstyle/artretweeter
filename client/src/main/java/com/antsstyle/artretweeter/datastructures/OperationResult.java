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
public class OperationResult {

    private ClientResponse clientResponse;
    private ServerResponse serverResponse;
    private TwitterResponse twitterResponse;

    public String getErrorMessage() {
        if (clientResponse != null) {
            return clientResponse.getClientStatusCode().getStatusMessage();
        } else if (serverResponse != null) {
            String msg = serverResponse.getServerStatusCode().getStatusMessage();
            if (serverResponse.getExtraStatusMessage() != null) {
                msg = msg.concat(" ").concat(serverResponse.getExtraStatusMessage());
            }
            return msg;
        } else if (twitterResponse != null) {
            String msg = twitterResponse.getServerStatusCode().getStatusMessage();
            if (twitterResponse.getTwitterErrorMessage() != null) {
                msg = msg.concat(" ").concat(twitterResponse.getTwitterErrorMessage());
            }
            if (twitterResponse.getExtraStatusMessage() != null) {
                msg = msg.concat(" ").concat(twitterResponse.getExtraStatusMessage());
            }
            return msg;
        } else {
            return null;
        }
    }

    public ClientResponse getClientResponse() {
        return clientResponse;
    }

    public OperationResult setClientResponse(ClientResponse clientResponse) {
        this.clientResponse = clientResponse;
        return this;
    }

    public Boolean wasSuccessful() {
        if (serverResponse != null && serverResponse.getServerStatusCode().isErrorStatus()) {
            return false;
        }
        if (twitterResponse != null && twitterResponse.getServerStatusCode().isErrorStatus()) {
            return false;
        }
        if (clientResponse != null && clientResponse.getClientStatusCode().isErrorStatus()) {
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
