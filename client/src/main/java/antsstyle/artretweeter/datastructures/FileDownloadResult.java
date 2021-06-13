/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package antsstyle.artretweeter.datastructures;

/**
 *
 * @author antss
 */
public class FileDownloadResult {
    
    private boolean successful;
    private boolean wasDownloaded;
    private boolean wasInterrupted;
    private boolean maxRetryReached;

    public boolean isMaxRetryReached() {
        return maxRetryReached;
    }

    public FileDownloadResult setMaxRetryReached(boolean maxRetryReached) {
        this.maxRetryReached = maxRetryReached;
        return this;
    }

    public boolean isWasInterrupted() {
        return wasInterrupted;
    }

    public FileDownloadResult setWasInterrupted(boolean wasInterrupted) {
        this.wasInterrupted = wasInterrupted;
        return this;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public FileDownloadResult setSuccessful(boolean successful) {
        this.successful = successful;
        return this;
    }

    public boolean isWasDownloaded() {
        return wasDownloaded;
    }

    public FileDownloadResult setWasDownloaded(boolean wasDownloaded) {
        this.wasDownloaded = wasDownloaded;
        return this;
    }
    
}
