/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.serverapi;

import com.antsstyle.artretweeter.enumerations.ServerStatusCode;
import com.antsstyle.artretweeter.configuration.MiscConfig;
import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.AutomationSettingsHolder;
import com.antsstyle.artretweeter.datastructures.ClientResponse;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.RetweetQueueEntry;
import com.antsstyle.artretweeter.datastructures.RetweetRecord;
import com.antsstyle.artretweeter.datastructures.ServerResponse;
import com.antsstyle.artretweeter.datastructures.TableTimestamp;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBResponseCode;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.db.TweetsDB;
import com.antsstyle.artretweeter.enumerations.ClientStatusCode;
import com.antsstyle.artretweeter.enumerations.StatusCode;
import com.antsstyle.artretweeter.gui.ChooseDatePanel;
import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.gui.GUIHelperMethods;
import com.antsstyle.artretweeter.json.TimestampTypeAdapter;
import com.antsstyle.artretweeter.main.ArtRetweeterMain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
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

    public static OperationResult getAutomationSettings(Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Timestamp.class, new TimestampTypeAdapter())
                .create();
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.GET_AUTOMATION_SETTINGS, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonElement resp = apiCallResult.getServerResponse().getResponseJSONObject().get("message");
        if (resp.isJsonNull()) {
            apiCallResult.getServerResponse().setReturnedObject(null);
        } else {

            AutomationSettingsHolder holder = gson.fromJson(resp, AutomationSettingsHolder.class);
            apiCallResult.getServerResponse().setReturnedObject(holder);
        }
        return apiCallResult;
    }

    public static OperationResult commitAutomationSettings(Account account, AutomationSettingsHolder holder) {
        List<NameValuePair> nvps = new ArrayList<>();
        Gson gson = new Gson();
        String json = gson.toJson(holder, AutomationSettingsHolder.class);
        nvps.add(new BasicNameValuePair("automation_settings", json));

        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.COMMIT_AUTOMATION_SETTINGS, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        return apiCallResult;
    }

    public static OperationResult getStoredTweetIDs(Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.GET_STORED_TWEET_IDS, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject resp = apiCallResult.getServerResponse().getResponseJSONObject().get("message").getAsJsonObject();
        JsonElement tweetIDsElement = resp.get("tweetids");
        ArrayList<Long> tweetIDs = new ArrayList<>();
        Integer lastReturnedServerID = null;
        if (tweetIDsElement.isJsonArray()) {
            JsonArray arr = tweetIDsElement.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                Long tweetID = obj.get("tweetid").getAsLong();
                tweetIDs.add(tweetID);
            }
            if (arr.size() != 0) {
                lastReturnedServerID = arr.get(arr.size() - 1).getAsJsonObject().get("id").getAsInt();
            }
        } else {
            apiCallResult.getServerResponse().setServerStatusCode(ServerStatusCode.SERVER_ERROR);
        }
        apiCallResult.getServerResponse().setReturnedObject(Pair.of(tweetIDs, lastReturnedServerID));
        return apiCallResult;
    }

    public static OperationResult getQueueStatus(Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.QUEUE_STATUS, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        ArrayList<RetweetQueueEntry> scheduledEntries = new ArrayList<>();
        ArrayList<RetweetQueueEntry> failedEntries = new ArrayList<>();
        JsonObject resp = apiCallResult.getServerResponse().getResponseJSONObject().get("message").getAsJsonObject();
        JsonElement scheduledRetweets = resp.get("scheduledretweets");
        JsonElement failedRetweets = resp.get("failedretweets");
        if (scheduledRetweets.isJsonArray()) {
            JsonArray arr = scheduledRetweets.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                RetweetQueueEntry entry = new RetweetQueueEntry()
                        .setTweetID(obj.get("tweetid").getAsLong())
                        .setRetweetingUserTwitterID(obj.get("retweetingusertwitterid").getAsLong())
                        .setAutomated(obj.get("automated").getAsString().equals("Y"))
                        .setRetweetTime(new Timestamp(obj.get("rttime").getAsLong() * 1000));
                scheduledEntries.add(entry);
            }
        } else {
            apiCallResult.getServerResponse().setServerStatusCode(ServerStatusCode.SERVER_ERROR);
        }
        if (failedRetweets.isJsonArray()) {
            JsonArray arr = failedRetweets.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                RetweetQueueEntry entry = new RetweetQueueEntry()
                        .setTweetID(obj.get("tweetid").getAsLong())
                        .setRetweetTime(new Timestamp(obj.get("rttime").getAsLong() * 1000))
                        .setRetweetingUserTwitterID(obj.get("retweetingusertwitterid").getAsLong())
                        .setErrorCode(obj.get("errorcode").getAsInt())
                        .setFailReason(obj.get("failreason").getAsString());
                failedEntries.add(entry);
            }
        } else {
            apiCallResult.getServerResponse().setServerStatusCode(ServerStatusCode.SERVER_ERROR);
        }
        apiCallResult.getServerResponse().setReturnedObject(Pair.of(scheduledEntries, failedEntries));
        return apiCallResult;
    }

    public static OperationResult getTweetRetweetStatus(Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.TWEET_RETWEET_STATUS, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        ArrayList<RetweetRecord> retweetRecordsOnServer = new ArrayList<>();
        JsonObject resp = apiCallResult.getServerResponse().getResponseJSONObject().get("message").getAsJsonObject();
        JsonElement retweetRecords = resp.get("retweetrecords");
        if (retweetRecords.isJsonArray()) {
            JsonArray arr = retweetRecords.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                RetweetRecord record = new RetweetRecord()
                        .setTweetID(obj.get("tweetid").getAsLong())
                        .setUserTwitterID(account.getTwitterID())
                        .setRetweetTime(new Timestamp(obj.get("rttime").getAsLong() * 1000));
                retweetRecordsOnServer.add(record);
            }
        } else {
            apiCallResult.getServerResponse().setServerStatusCode(ServerStatusCode.SERVER_ERROR);
        }
        apiCallResult.getServerResponse().setReturnedObject(retweetRecordsOnServer);
        return apiCallResult;
    }

    public static OperationResult queueRetweet(Account account, Long tweetID, Timestamp retweetTime) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("tweetid", String.valueOf(tweetID)));
        nvps.add(new BasicNameValuePair("retweettime", String.valueOf((long) Math.floor(retweetTime.getTime() / 1000.0))));
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.QUEUE_RETWEET, account);
        return apiCallResult;
    }

    public static OperationResult unqueueRetweet(Account account, Long tweetID) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("tweetid", String.valueOf(tweetID)));
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.UNQUEUE_RETWEET, account);
        return apiCallResult;
    }

    public static OperationResult deleteTweet(Account account, Long tweetID) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("tweetid", String.valueOf(tweetID)));
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.DELETE_TWEET, account);
        return apiCallResult;
    }

    public static OperationResult deleteTweets(Account account, String tweetIDs) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("tweetids", tweetIDs));
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.DELETE_MULTIPLE_TWEETS, account);
        return apiCallResult;
    }

    public static OperationResult removeAccount(Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        OperationResult apiCallResult = serverCall(nvps, ArtRetweeterEndpoint.REMOVE_ACCOUNT, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        Boolean failQueueCleared = apiCallResult.getServerResponse().getResponseJSONObject().get("failure_queue_cleared").getAsBoolean();
        Boolean scheduleQueueCleared = apiCallResult.getServerResponse().getResponseJSONObject().get("schedule_queue_cleared").getAsBoolean();
        Boolean userCleared = apiCallResult.getServerResponse().getResponseJSONObject().get("user_cleared").getAsBoolean();
        Boolean metricsCleared = apiCallResult.getServerResponse().getResponseJSONObject().get("metrics_cleared").getAsBoolean();
        Boolean tweetsCleared = apiCallResult.getServerResponse().getResponseJSONObject().get("tweets_cleared").getAsBoolean();
        Boolean automationSettingsCleared = apiCallResult.getServerResponse().getResponseJSONObject().get("automation_settings_cleared").getAsBoolean();
        if (!failQueueCleared || !scheduleQueueCleared || !userCleared || !metricsCleared || !tweetsCleared || !automationSettingsCleared) {
            apiCallResult.getServerResponse().setServerStatusCode(ServerStatusCode.SERVER_ERROR);
        }
        ArrayList<Boolean> bools = new ArrayList<>();
        bools.add(failQueueCleared);
        bools.add(scheduleQueueCleared);
        bools.add(userCleared);
        bools.add(metricsCleared);
        bools.add(tweetsCleared);
        bools.add(automationSettingsCleared);
        apiCallResult.getServerResponse().setReturnedObject(bools);
        return apiCallResult;
    }

    public static Pair<ServerResponse, JsonObject> processResponse(CloseableHttpResponse response) {
        LOGGER.debug("Processing response.");
        if (response.getStatusLine().getStatusCode() != 200) {
            ServerResponse res = new ServerResponse(ServerStatusCode.SERVER_ERROR);
            res.setExtraStatusMessage("Failed to communicate with ArtRetweeter server!");
            return Pair.of(res, null);
        }
        HttpEntity entity = response.getEntity();
        Gson gson = new Gson();
        try ( InputStream is = entity.getContent();  InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
            String jsonString = IOUtils.toString(reader);
            if (MiscConfig.DEBUG_MODE) {
                try ( PrintWriter pw = new PrintWriter(MiscConfig.DEBUG_LAST_SERVER_REQUEST_OUTPUT_FILE_PATH.toString())) {
                    Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject obj = prettyGson.fromJson(jsonString, JsonObject.class);
                    prettyGson.toJson(obj, pw);
                }
            }
            JsonObject responseJSON = gson.fromJson(jsonString, JsonObject.class);
            ServerResponse result = processArtRetweeterServerErrorResponse(responseJSON);
            EntityUtils.consume(entity);
            if (result != null) {
                return Pair.of(result, responseJSON);
            }
            return Pair.of(null, responseJSON);
        } catch (Exception e) {
            LOGGER.error("Unable to read response entity!", e);
            ServerResponse res = new ServerResponse(ServerStatusCode.SERVER_ERROR);
            res.setExtraStatusMessage("Unable to read response entity!");
            return Pair.of(res, null);
        }
    }

    public static OperationResult serverCall(List<NameValuePair> nameValuePairs, ArtRetweeterEndpoint endpoint,
            Account account) {
        OperationResult opResult = new OperationResult();
        if (account == null) {
            opResult.setClientResponse(new ClientResponse(ClientStatusCode.MISSING_CREDENTIALS));
            return opResult;
        }
        nameValuePairs.add(new BasicNameValuePair("access_token", account.getToken()));
        nameValuePairs.add(new BasicNameValuePair("access_token_secret", account.getTokenSecret()));
        nameValuePairs.add(new BasicNameValuePair("user_auth_twitter_id", String.valueOf(account.getTwitterID())));
        nameValuePairs.add(new BasicNameValuePair("artretweeter_endpoint", endpoint.getEndpointName()));
        String url = ArtRetweeterMain.prop.getProperty("serverurl");
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
                        artRetweeterServerErrors++;
                        if (artRetweeterServerErrors == 1) {
                            if (checkResult.getLeft() != null) {
                                opResult.setServerResponse(checkResult.getLeft());
                                return opResult;
                            } else {
                                opResult.setServerResponse(new ServerResponse(ServerStatusCode.SERVER_ERROR));
                                return opResult;
                            }
                        }
                        try {
                            Thread.sleep(artRetweeterServerErrors * 1000);
                        } catch (Exception e) {
                            LOGGER.error("Interrupted while waiting to retry server request", e);
                            opResult.setClientResponse(new ClientResponse(ClientStatusCode.INTERRUPTED));
                            return opResult;
                        }
                        continue;
                    }
                    opResult.setServerResponse(new ServerResponse(ServerStatusCode.QUERY_OK));
                    JsonObject responseJSON = checkResult.getRight();
                    opResult.getServerResponse().setResponseJSONObject(responseJSON.getAsJsonObject());
                    break;
                } catch (Exception e) {
                    LOGGER.error("Failed to get HTTP response!", e);
                    opResult.setServerResponse(new ServerResponse(ServerStatusCode.SERVER_ERROR));
                    return opResult;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create HTTP client!", e);
            opResult.setServerResponse(new ServerResponse(ServerStatusCode.SERVER_ERROR));
            return opResult;
        }
        return opResult;
    }

    public static ServerResponse processArtRetweeterServerErrorResponse(JsonObject responseJSON) {
        if (responseJSON.get("statuscode") != null) {
            Integer statusCode = responseJSON.get("statuscode").getAsInt();
            LOGGER.debug("Status code returned by server: " + statusCode);
            ServerStatusCode ssc = ServerStatusCode.getCodeByInteger(statusCode);
            if (!ssc.equals(ServerStatusCode.QUERY_OK)) {
                ServerResponse result = new ServerResponse(ssc);
                result.setExtraStatusMessage(responseJSON.get("message").getAsString());
                return result;
            }
        }
        return null;
    }

    public static TweetHolder checkTweetCanBeQueued(Account account, boolean changeTime, Integer id) {
        DBResponse selectResp = CoreDB.selectFromTable(DBTable.TWEETS, new String[]{"id"}, new Object[]{id});
        if (!selectResp.wasSuccessful()) {
            String msg = "Failed to query DB for tweet information!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        } else if (selectResp.getReturnedRows().isEmpty()) {
            String msg = "This tweet doesn't exist in DB - has the DB file been modified?";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        TweetHolder tweet = ResultSetConversion.getTweet(selectResp.getReturnedRows().get(0));
        selectResp = CoreDB.selectFromTable(DBTable.RETWEETQUEUE, new String[]{"tweetid", "retweetingusertwitterid"},
                new Object[]{tweet.getTweetID(), account.getTwitterID()});
        if (!selectResp.wasSuccessful()) {
            String msg = "Failed to query DB for retweet queue information!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (!selectResp.getReturnedRows().isEmpty() && !changeTime) {
            String msg = "This tweet is already queued for retweeting.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        DBResponse accountsResp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!accountsResp.wasSuccessful()) {
            String msg = "Failed to query DB for accounts information!";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return null;
        } else if (accountsResp.getReturnedRows().isEmpty()) {
            String msg = "You cannot queue a retweet without an account. Add one first.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return null;
        }
        boolean isAuthorised = false;
        ArrayList<HashMap<String, Object>> rows = accountsResp.getReturnedRows();
        for (HashMap<String, Object> dbRow : rows) {
            Account acc = ResultSetConversion.getAccount(dbRow);
            if (acc.getTwitterID().equals(tweet.getUserTwitterID())) {
                isAuthorised = true;
                break;
            }
        }
        if (!isAuthorised) {
            String msg = "ArtRetweeter will only queue retweets for tweets from accounts you own.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return null;
        }
        return tweet;
    }

    public static Timestamp getTimeFromUser() {
        ChooseDatePanel datePanel = new ChooseDatePanel();
        GUIHelperMethods.setGUIColours(datePanel);
        int selectionResult = JOptionPane.showConfirmDialog(GUI.getInstance(), datePanel, "Select Retweet Date", JOptionPane.OK_CANCEL_OPTION);
        if (selectionResult != JOptionPane.OK_OPTION) {
            return null;
        }
        Timestamp time = datePanel.getSelectedTime();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        if (cal.getTimeInMillis() > time.getTime()) {
            String msg = "You must choose a date at least one hour from the current time.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.error(msg);
            return null;
        }
        return time;
    }

    public static OperationResult queueRetweet(Account account, TweetHolder tweet, Timestamp time) {
        OperationResult opResult = ServerAPI.queueRetweet(account, tweet.getTweetID(), time);
        return opResult;
    }

}
