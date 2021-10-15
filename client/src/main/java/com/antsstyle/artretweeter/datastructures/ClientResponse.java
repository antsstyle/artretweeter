/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import com.antsstyle.artretweeter.enumerations.ClientStatusCode;

/**
 *
 * @author antss
 */
public class ClientResponse extends Response {
    
    private ClientStatusCode clientStatusCode;    

    public ClientStatusCode getClientStatusCode() {
        return clientStatusCode;
    }

    public void setClientStatusCode(ClientStatusCode clientStatusCode) {
        this.clientStatusCode = clientStatusCode;
    }
    private Boolean imageWasDownloaded;

    public Boolean getImageWasDownloaded() {
        return imageWasDownloaded;
    }

    public void setImageWasDownloaded(Boolean imageWasDownloaded) {
        this.imageWasDownloaded = imageWasDownloaded;
    }

    public ClientResponse(ClientStatusCode statusCode) {
        this.clientStatusCode = statusCode;
    }
    
    public ClientResponse(ClientStatusCode statusCode, String extraStatusMessage) {
        this.clientStatusCode = statusCode;
        this.extraStatusMessage = extraStatusMessage;
    }
    
    public boolean wasSuccessful() {
        return clientStatusCode.equals(ClientStatusCode.QUERY_OK);
    }
    
}
