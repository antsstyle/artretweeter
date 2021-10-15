/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.datastructures;

import com.antsstyle.artretweeter.enumerations.ServerStatusCode;

/**
 *
 * @author antss
 */
public class ServerResponse extends Response {

    public ServerResponse(ServerStatusCode serverStatusCode) {
        this.serverStatusCode = serverStatusCode;
    }
    
    public Boolean wasSuccessful() {
        return !serverStatusCode.isErrorStatus();
    }

}
