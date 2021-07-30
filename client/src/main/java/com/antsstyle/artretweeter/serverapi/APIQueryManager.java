/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.serverapi;

import com.antsstyle.artretweeter.gui.GUIHelperMethods;
import java.util.concurrent.Semaphore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class APIQueryManager {

    private static final Logger LOGGER = LogManager.getLogger(APIQueryManager.class);
    
    private static final Semaphore apiLock = new Semaphore(1);

    public static boolean acquireAPILock(boolean showError) {
        synchronized (apiLock) {
            if (!apiLock.tryAcquire()) {
                if (showError) {
                    String message = "<html>Error: another query to the ArtRetweeter server or Twitter API is being performed. " 
                            + "<br/><br/>Wait until that has finished and then try again.</html>";
                    GUIHelperMethods.showError(message, showError);
                }
                return false;
            }
            return true;
        }
    }

    public static void releaseAPILock() {
        synchronized (apiLock) {
            apiLock.release();
        }
    }

}
