/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import com.antsstyle.artretweeter.enumerations.StatusCode;

/**
 *
 * @author antss
 */
public class ClientResponse extends Response {
    
    private Boolean imageWasDownloaded;

    public Boolean getImageWasDownloaded() {
        return imageWasDownloaded;
    }

    public void setImageWasDownloaded(Boolean imageWasDownloaded) {
        this.imageWasDownloaded = imageWasDownloaded;
    }

    public ClientResponse(StatusCode statusCode) {
        this.statusCode = statusCode;
    }
    
    public ClientResponse(StatusCode statusCode, String extraStatusMessage) {
        this.statusCode = statusCode;
        this.extraStatusMessage = extraStatusMessage;
    }
    
}
