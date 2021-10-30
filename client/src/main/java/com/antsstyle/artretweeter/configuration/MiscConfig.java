/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author antss
 */
public class MiscConfig extends ConfigurationModule {
    
    public static final String P_DEBUG_MODE = "artretweeter.debugmode";
    public static final String P_DEBUG_LAST_TWITTERAPI_REQUEST_OUTPUT_FILE_PATH = 
            "artretweeter.debug.lasttwitterapirequestoutputfilepath";
    public static final String P_DEBUG_LAST_SERVER_REQUEST_OUTPUT_FILE_PATH = 
            "artretweeter.debug.lastserverrequestoutputfilepath";

    public static Boolean DEBUG_MODE = false;

    public static Path DEBUG_LAST_TWITTERAPI_REQUEST_OUTPUT_FILE_PATH
            = Paths.get(System.getProperty("user.dir")).resolve("lasttwitterapirequest.txt");

    public static Path DEBUG_LAST_SERVER_REQUEST_OUTPUT_FILE_PATH
            = Paths.get(System.getProperty("user.dir")).resolve("lastserverrequest.txt");
    

}
