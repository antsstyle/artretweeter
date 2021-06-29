/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.serverapi;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.ClientResponse;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.RetweetQueueEntry;
import com.antsstyle.artretweeter.datastructures.ServerResponse;
import com.antsstyle.artretweeter.enumerations.StatusCode;
import com.antsstyle.artretweeter.main.ArtRetweeterMain;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Contains methods that query the ArtRetweeter server, but that do not require any Twitter API calls.
 *
 * @author antss
 */
public class ServerAPI {

    private static final Logger LOGGER = LogManager.getLogger(ServerAPI.class);

    public static OperationResult getQueueStatus(Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.QUEUE_STATUS, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        ArrayList<RetweetQueueEntry> scheduledEntries = new ArrayList<>();
        ArrayList<RetweetQueueEntry> failedEntries = new ArrayList<>();
        JsonObject resp = apiCallResult.getServerResponse().getResponseJSONObject().get("dbresult").getAsJsonObject();
        JsonElement scheduledRetweets = resp.get("scheduledretweets");
        JsonElement failedRetweets = resp.get("failedretweets");
        if (scheduledRetweets.isJsonArray()) {
            JsonArray arr = scheduledRetweets.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                RetweetQueueEntry entry = new RetweetQueueEntry()
                        .setTweetID(obj.get("tweetid").getAsLong())
                        .setRetweetingUserTwitterID(obj.get("retweetingusertwitterid").getAsLong())
                        .setRetweetTime(new Timestamp(obj.get("rttime").getAsLong()*1000));
                scheduledEntries.add(entry);
            }
        } else {
            apiCallResult.getServerResponse().setStatusCode(StatusCode.ARTRETWEETER_SERVER_ERROR);
        }
        if (failedRetweets.isJsonArray()) {
            JsonArray arr = failedRetweets.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                RetweetQueueEntry entry = new RetweetQueueEntry()
                        .setTweetID(obj.get("tweetid").getAsLong())
                        .setRetweetTime(new Timestamp(obj.get("rttime").getAsLong()*1000))
                        .setRetweetingUserTwitterID(obj.get("retweetingusertwitterid").getAsLong())
                        .setErrorCode(obj.get("errorcode").getAsInt())
                        .setFailReason(obj.get("failreason").getAsString());
                failedEntries.add(entry);
            }
        } else {
            apiCallResult.getServerResponse().setStatusCode(StatusCode.ARTRETWEETER_SERVER_ERROR);
        }
        apiCallResult.getServerResponse().setReturnedObject(Pair.of(scheduledEntries, failedEntries));
        return apiCallResult;
    }

    public static OperationResult queueRetweet(Account account, Long tweetID, Timestamp retweetTime) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("tweetid", String.valueOf(tweetID)));
        nvps.add(new BasicNameValuePair("retweettime", String.valueOf((long) Math.floor(retweetTime.getTime() / 1000.0))));
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.QUEUE_RETWEET, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        Boolean success = apiCallResult.getServerResponse().getResponseJSONObject().get("dbresult").getAsBoolean();
        apiCallResult.getServerResponse().setReturnedObject(success);
        return apiCallResult;
    }

    public static OperationResult unqueueRetweet(Account account, Long tweetID) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("tweetid", String.valueOf(tweetID)));
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.UNQUEUE_RETWEET, account);
        return apiCallResult;
    }

    public static OperationResult removeAccount(Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.REMOVE_ACCOUNT, account);
        return apiCallResult;
    }

    public static Pair<ServerResponse, JsonObject> processResponse(CloseableHttpResponse response) {
        if (response.getStatusLine().getStatusCode() != 200) {
            ServerResponse res = new ServerResponse(StatusCode.ARTRETWEETER_SERVER_ERROR);
            res.setExtraStatusMessage("Failed to communicate with ArtRetweeter server!");
            return Pair.of(res, null);
        }
        HttpEntity entity = response.getEntity();
        Gson gson = new Gson();
        try ( InputStream is = entity.getContent();  InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
            String jsonString = IOUtils.toString(reader);
            PrintWriter pw = new PrintWriter("I:/zzz222testoutput.txt");
            pw.println(jsonString);
            pw.close();
            JsonObject responseJSON = gson.fromJson(jsonString, JsonObject.class);
            ServerResponse result = processArtRetweeterServerErrorResponse(responseJSON);
            EntityUtils.consume(entity);
            if (result != null) {
                return Pair.of(result, responseJSON);
            }
            return Pair.of(null, responseJSON);
        } catch (Exception e) {
            LOGGER.error("Unable to read response entity!", e);
            ServerResponse res = new ServerResponse(StatusCode.JSON_PARSE_ERROR);
            res.setExtraStatusMessage("Unable to read response entity!");
            return Pair.of(res, null);
        }
    }

    public static OperationResult serverCall(List<NameValuePair> nameValuePairs, ArtRetweeterEndpoint endpoint,
            Account account) {
        OperationResult opResult = new OperationResult();
        if (account == null) {
            opResult.setClientResponse(new ClientResponse(StatusCode.MISSING_CREDENTIALS_ERROR));
            return opResult;
        }
        nameValuePairs.add(new BasicNameValuePair("access_token", account.getToken()));
        nameValuePairs.add(new BasicNameValuePair("access_token_secret", account.getTokenSecret()));
        nameValuePairs.add(new BasicNameValuePair("user_auth_twitter_id", String.valueOf(account.getTwitterID())));
        nameValuePairs.add(new BasicNameValuePair("artretweeter_endpoint", endpoint.getEndpointName()));
        String url = ArtRetweeterMain.prop.getProperty("serverurl");
        LOGGER.debug("URL: " + url);
        try ( CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            int artRetweeterServerErrors = 0;
            while (artRetweeterServerErrors < 1) {
                if (artRetweeterServerErrors > 0) {
                    LOGGER.debug("Art retweeter server errors: " + artRetweeterServerErrors);
                }
                try ( CloseableHttpResponse response = httpclient.execute(httpPost)) {
                    Pair<ServerResponse, JsonObject> checkResult = processResponse(response);
                    int httpCode = response.getStatusLine().getStatusCode();
                    if (httpCode != 200 || checkResult.getLeft() != null) {
                        if (httpCode != 200) {
                            LOGGER.debug("HTTP code: " + httpCode);
                        }
                        if (checkResult.getLeft() != null) {
                            LOGGER.debug("Log message: " + checkResult.getLeft().getLogMessage());
                        }
                        artRetweeterServerErrors++;
                        if (artRetweeterServerErrors == 1) {
                            if (checkResult.getLeft() != null) {
                                opResult.setServerResponse(checkResult.getLeft());
                                return opResult;
                            } else {
                                opResult.setServerResponse(new ServerResponse(StatusCode.ARTRETWEETER_SERVER_ERROR));
                                return opResult;
                            }
                        }
                        try {
                            Thread.sleep(artRetweeterServerErrors * 1000);
                        } catch (Exception e) {
                            LOGGER.error("Interrupted while waiting to retry server request", e);
                            opResult.setClientResponse(new ClientResponse(StatusCode.INTERRUPTED_ERROR));
                            return opResult;
                        }
                        continue;
                    }
                    opResult.setServerResponse(new ServerResponse(StatusCode.SUCCESS));
                    JsonObject responseJSON = checkResult.getRight();
                    opResult.getServerResponse().setResponseJSONObject(responseJSON.getAsJsonObject());
                    break;
                } catch (Exception e) {
                    LOGGER.error("Failed to get HTTP response!", e);
                    opResult.setServerResponse(new ServerResponse(StatusCode.MISC_ERROR));
                    return opResult;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create HTTP client!", e);
            opResult.setServerResponse(new ServerResponse(StatusCode.MISC_ERROR));
            return opResult;
        }
        return opResult;
    }

    public static ServerResponse processArtRetweeterServerErrorResponse(JsonObject responseJSON) {
        if (responseJSON.keySet().size() >= 1 && responseJSON.get("artretweetererrors") != null) {
            String message = responseJSON.get("artretweetererrors").getAsString();
            ServerResponse result;
            if (message.equals("Rate limit reached.")) {
                result = new ServerResponse(StatusCode.RATE_LIMIT_EXCEEDED_ERROR);
                int timeToResetSeconds = responseJSON.get("timetoresetseconds").getAsInt();
                int minutes = timeToResetSeconds / 60;
                int seconds = timeToResetSeconds % 60;
                String timeMessage;
                if (minutes == 0) {
                    timeMessage = String.valueOf(seconds).concat(" seconds.");
                } else if (minutes == 1) {
                    timeMessage = String.valueOf(minutes).concat(" minute, ").concat(String.valueOf(seconds)).concat(" seconds.");
                } else {
                    timeMessage = String.valueOf(minutes).concat(" minutes, ").concat(String.valueOf(seconds)).concat(" seconds.");
                }
                result.setExtraStatusMessage("You can try this request again in ".concat(timeMessage));
                result.setReturnedObject(responseJSON.get("timetoresetseconds").getAsInt());
            } else {
                result = new ServerResponse(StatusCode.ARTRETWEETER_SERVER_ERROR);
                result.setExtraStatusMessage(message);
            }
            return result;
        } else {
            return null;
        }
    }

}
