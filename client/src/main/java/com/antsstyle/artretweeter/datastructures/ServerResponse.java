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
public class ServerResponse extends Response {

    public ServerResponse(StatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public String getLogMessage() {
        if (statusCode == null) {
            return null;
        }
        String msg = "HTTP status code: ".concat(String.valueOf(httpStatusCode));
        msg = msg.concat("; ").concat(statusCode.getStatusMessage());
        if (extraStatusMessage != null) {
            msg = msg.concat(" ").concat(extraStatusMessage);
        }
        return msg;
    }

}
