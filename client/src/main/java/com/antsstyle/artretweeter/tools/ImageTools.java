/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.tools;

import com.antsstyle.artretweeter.datastructures.ClientResponse;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.enumerations.ClientStatusCode;
import com.antsstyle.artretweeter.enumerations.StatusCode;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

/**
 *
 * @author antss
 */
public class ImageTools {

    private static final Logger LOGGER = LogManager.getLogger();

    public static BufferedImage getScaledImageForSaving(BufferedImage srcImg, int saveWidth, int saveHeight) {
        return getScaledImage(srcImg, saveWidth, saveHeight, Scalr.Method.ULTRA_QUALITY);
    }

    public static BufferedImage getScaledImage(BufferedImage srcImg, int paneWidth, int paneHeight, String scalrMethodString) {
        if (scalrMethodString.equals("Ultra")) {
            return getScaledImage(srcImg, paneWidth, paneHeight, Scalr.Method.ULTRA_QUALITY);
        } else if (scalrMethodString.equals("Quality")) {
            return getScaledImage(srcImg, paneWidth, paneHeight, Scalr.Method.QUALITY);
        } else if (scalrMethodString.equals("Balanced")) {
            return getScaledImage(srcImg, paneWidth, paneHeight, Scalr.Method.BALANCED);
        } else if (scalrMethodString.equals("Speed")) {
            return getScaledImage(srcImg, paneWidth, paneHeight, Scalr.Method.SPEED);
        } else if (scalrMethodString.equals("Automatic")) {
            return getScaledImage(srcImg, paneWidth, paneHeight, Scalr.Method.AUTOMATIC);
        } else {
            return getScaledImage(srcImg, paneWidth, paneHeight, Scalr.Method.ULTRA_QUALITY);
        }
    }

    public static BufferedImage getScaledImageForViewing(BufferedImage srcImg, int paneWidth, int paneHeight) {
        return getScaledImage(srcImg, paneWidth, paneHeight, Scalr.Method.ULTRA_QUALITY);
    }

    /**
     * Scales an image to fit it within both the given paneWidth and paneHeight.
     *
     * @param srcImg The image to scale.
     * @param paneWidth The width of the panel this image needs to fit into.
     * @param paneHeight The height of the panel this image needs to fit into.
     * @param scalingMethod
     * @return A scaled version of the image, with its original aspect ratio, with neither the width or height being greater than the provided paneWidth or paneHeight.
     */
    public static BufferedImage getScaledImage(BufferedImage srcImg, int paneWidth, int paneHeight, Method scalingMethod) {
        int srcWidth = srcImg.getWidth();
        int srcHeight = srcImg.getHeight();
        if (srcWidth <= paneWidth && srcHeight <= paneHeight) {
            return srcImg;
        }
        double widthRatio = (double) srcWidth / (double) paneWidth;
        double heightRatio = (double) srcHeight / (double) paneHeight;
        int newWidth;
        int newHeight;
        if (heightRatio > widthRatio) {
            newHeight = (int) Math.round(srcHeight / heightRatio);
            newWidth = (int) Math.round(srcWidth / heightRatio);
        } else {
            newHeight = (int) Math.round(srcHeight / widthRatio);
            newWidth = (int) Math.round(srcWidth / widthRatio);
        }
        BufferedImage returnImg = Scalr.resize(srcImg, scalingMethod, Scalr.Mode.FIT_EXACT, newWidth, newHeight);
        return returnImg;
    }

    public static OperationResult downloadImageFromSiteWithRetry(String imageURL, Path fullFilePath, boolean overwriteExisting) {
        return downloadImageFromSiteWithRetry(imageURL, fullFilePath, 3, false);
    }

    public static OperationResult downloadImageFromSiteWithRetry(String imageURL, Path fullFilePath, int retryLimit, boolean overwriteExisting) {
        int attempts = 0;
        while (attempts < retryLimit) {
            OperationResult result = downloadImageFromSite(imageURL, fullFilePath, overwriteExisting);
            if (result.wasSuccessful()) {
                return result;
            } else {
                int waitSecondsMultiplier = (int) Math.pow(2, attempts);
                attempts++;
                try {
                    Thread.sleep(Math.max((5 * waitSecondsMultiplier) * 1000, 1000));
                } catch (Exception e) {
                    LOGGER.error("Interrupted while waiting to retry image download - aborting.", e);
                    result.getClientResponse().setClientStatusCode(ClientStatusCode.INTERRUPTED);
                }
            }
        }
        LOGGER.error("Maximum retry limit reached - aborting image download.");
        ClientResponse result = new ClientResponse(ClientStatusCode.MAX_DOWNLOAD_RETRY_REACHED);
        OperationResult opResult = new OperationResult();
        opResult.setClientResponse(result);
        return opResult;
    }

    /**
     * Downloads an image from the given URL, and saves it to the given filepath.
     *
     * @param imageURL The URL pointing to the image to save.
     * @param fullFilePath The filepath to save the image to.
     * @param overwriteExisting Whether to overwrite the existing file at the given path, if one exists.
     * @return True on success, false otherwise.
     */
    public static OperationResult downloadImageFromSite(String imageURL, Path fullFilePath, boolean overwriteExisting) {
        OperationResult result = new OperationResult();
        ClientResponse clientResp;
        if (Files.exists(fullFilePath) && !overwriteExisting) {
            clientResp = new ClientResponse(ClientStatusCode.FILE_ALREADY_DOWNLOADED);
            result.setClientResponse(clientResp);
            return result;
        }
        HttpGet httpGet = new HttpGet(imageURL);
        httpGet
                .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36");
        httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.setHeader("Accept-Language", "en-US,en;q=0.5");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        try ( CloseableHttpClient httpclient = HttpClients.createDefault();  CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();

            try ( InputStream is = entity1.getContent();  OutputStream os = new FileOutputStream(fullFilePath.toFile())) {
                int bufferSize = 65536;
                byte[] byteBuffer = new byte[bufferSize];
                int length2;
                while ((length2 = is.read(byteBuffer)) != -1) {
                    os.write(byteBuffer, 0, length2);
                }
                clientResp = new ClientResponse(ClientStatusCode.QUERY_OK);
            } catch (Exception e) {
                LOGGER.error("Failed to download image from webpage!", e);
                clientResp = new ClientResponse(ClientStatusCode.DOWNLOAD_ERROR);
            }
            EntityUtils.consume(entity1);

            result.setClientResponse(clientResp);
            return result;
        } catch (Exception e) {
            LOGGER.error("Could not retrieve URL for image source!", e);
            clientResp = new ClientResponse(ClientStatusCode.MISC_ERROR);
            result.setClientResponse(clientResp);
            return result;
        }
    }

}
