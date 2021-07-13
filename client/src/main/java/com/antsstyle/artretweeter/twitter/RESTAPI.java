/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.twitter;

import com.antsstyle.artretweeter.configuration.MiscConfig;
import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.ClientResponse;
import com.antsstyle.artretweeter.datastructures.CollectionCurateParamsJSON;
import com.antsstyle.artretweeter.datastructures.CollectionCurateRespJSON;
import com.antsstyle.artretweeter.datastructures.CollectionOrdering;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.TwitterResponse;
import com.antsstyle.artretweeter.datastructures.RequestToken;
import com.antsstyle.artretweeter.datastructures.ServerResponse;
import com.antsstyle.artretweeter.datastructures.StatusJSON;
import com.antsstyle.artretweeter.datastructures.TweetHolder;
import com.antsstyle.artretweeter.datastructures.TwitterCollectionHolder;
import com.antsstyle.artretweeter.db.CollectionsDB;
import com.antsstyle.artretweeter.db.ConfigDB;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.RateLimitsDB;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.db.TweetsDB;
import com.antsstyle.artretweeter.enumerations.StatusCode;
import com.antsstyle.artretweeter.main.ArtRetweeterMain;
import com.antsstyle.artretweeter.serverapi.ServerAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
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
 *
 * @author antss
 */
public class RESTAPI {

    private static final Logger LOGGER = LogManager.getLogger(RESTAPI.class);

    public static Boolean CURRENTLY_PROCESSING = false;

    /* Twitter API Status codes */
    public static final int PAGE_NOT_FOUND = 34;
    public static final int RATE_LIMIT_EXCEEDED = 88;
    public static final int INVALID_ACCESS_TOKEN = 89;
    public static final int CANNOT_VALIDATE_OAUTH_CREDENTIALS = 99;
    public static final int TWITTER_OVER_CAPACITY = 130;
    public static final int TWITTER_INTERNAL_ERROR = 131;
    public static final int TIMESTAMP_AUTH_ERROR = 135;
    public static final int BLOCKED_BY_TWEET_AUTHOR = 136;
    public static final int TWEET_NOT_FOUND = 144;
    public static final int TWEET_NOT_VIEWABLE = 179;
    public static final int TWEET_TEXT_TOO_LONG = 186;
    public static final int DUPLICATE_TWEET = 187;
    public static final int ALREADY_RETWEETED = 327;

    /**
     *
     * @param endpoint
     * @param userTwitterID
     * @param response
     * @return A null OperationResult on success, with the result JSON in the JsonObject. The OperationResult will be instantiated with a status code on failure.
     */
    public static Pair<OperationResult, JsonObject> processResponse(String endpoint,
            Long userTwitterID, CloseableHttpResponse response) {
        OperationResult res = new OperationResult();
        if (response.getStatusLine().getStatusCode() != 200) {
            res.setServerResponse(new ServerResponse(StatusCode.ARTRETWEETER_SERVER_ERROR));
            res.getServerResponse().setExtraStatusMessage("Failed to communicate with ArtRetweeter server!");
            return Pair.of(res, null);
        }
        HttpEntity entity = response.getEntity();
        Gson gson = new Gson();
        try ( InputStream is = entity.getContent();  InputStreamReader reader = new InputStreamReader(is, "UTF-8")) {
            String jsonString = IOUtils.toString(reader);
            if (MiscConfig.DEBUG_MODE) {
                try ( PrintWriter pw = new PrintWriter(MiscConfig.DEBUG_LAST_TWITTERAPI_REQUEST_OUTPUT_FILE_PATH.toString())) {
                    Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
                    JsonObject obj = prettyGson.fromJson(jsonString, JsonObject.class);
                    prettyGson.toJson(obj, pw);
                } catch (Exception e1) {
                    LOGGER.error("Failed to write debug file correctly!", e1);
                }
            }
            JsonObject responseJSON = gson.fromJson(jsonString, JsonObject.class);
            if (responseJSON.keySet().contains("headers")) {
                try {
                    JsonObject headerJSON = responseJSON.get("headers").getAsJsonObject();
                    processHeaders(headerJSON, endpoint, userTwitterID);
                } catch (Exception e1) {
                    LOGGER.error("Unable to process headers - empty array returned.", e1);
                }
            }
            ServerResponse result = ServerAPI.processArtRetweeterServerErrorResponse(responseJSON);
            EntityUtils.consume(entity);
            if (result != null) {
                res.setServerResponse(result);
                return Pair.of(res, responseJSON);
            }
            TwitterResponse twResult = processTwitterAPIErrorResponse(responseJSON);
            if (result != null) {
                res.setTwitterResponse(twResult);
                return Pair.of(res, responseJSON);
            }
            return Pair.of(null, responseJSON);
        } catch (Exception e) {
            LOGGER.error("Unable to read response entity!", e);
            ClientResponse clientRes = new ClientResponse(StatusCode.JSON_PARSE_ERROR);
            res.setClientResponse(clientRes);
            return Pair.of(res, null);
        }
    }

    public static TwitterResponse processTwitterAPIErrorResponse(JsonObject responseJSON) {
        Set<String> keys = responseJSON.keySet();
        if (!keys.contains("response")) {
            return null;
        }
        JsonElement responseElem = responseJSON.get("response");
        if (responseElem.isJsonArray()) {
            return null;
        }
        JsonObject responseObj = responseElem.getAsJsonObject();
        if (!responseObj.keySet().contains("errors")) {
            return null;
        }

        JsonArray errorObj = responseObj.get("errors").getAsJsonArray();
        // Use first error only
        TwitterResponse result = new TwitterResponse(StatusCode.TWITTER_API_ERROR);
        result.setTwitterErrorCode(errorObj.get(0).getAsJsonObject().get("code").getAsInt());
        result.setTwitterErrorMessage(errorObj.get(0).getAsJsonObject().get("message").getAsString());
        result.setHttpStatusCode(responseJSON.get("httpcode").getAsInt());
        return result;
    }

    public static void processHeaders(JsonObject headerJSON, String endpoint, Long userTwitterID) {
        Set<String> keys = headerJSON.keySet();
        Timestamp rateLimitReset;
        Integer rateLimitTotal;
        Integer rateLimitRemaining;
        if (keys.contains("x_rate_limit_reset")) {
            rateLimitReset = new Timestamp(headerJSON.get("x_rate_limit_reset").getAsLong());
        } else {
            // Headers have no rate limit information; no need to process.
            return;
        }
        try {
            rateLimitTotal = headerJSON.get("x_rate_limit_limit").getAsInt();
            rateLimitRemaining = headerJSON.get("x_rate_limit_remaining").getAsInt();
        } catch (Exception e) {
            LOGGER.error("Unable to parse rate limit headers!", e);
            return;
        }
        if (userTwitterID == null) {
            RateLimitsDB.mergeAppRateLimitInfo(endpoint, rateLimitTotal, rateLimitRemaining, rateLimitReset);
        } else {
            RateLimitsDB.mergeUserRateLimitInfo(userTwitterID, endpoint, rateLimitTotal, rateLimitRemaining, rateLimitReset);
        }
    }

    public static OperationResult apiCall(List<NameValuePair> nameValuePairs, TwitterEndpoint endpoint,
            Account account) {
        OperationResult opResult = new OperationResult();
        if (account == null && endpoint.getRequiresUserAuth()) {
            opResult.setClientResponse(new ClientResponse(StatusCode.MISSING_CREDENTIALS_ERROR));
            return opResult;
        }
        Long userTwitterID = null;
        if (account != null) {
            userTwitterID = account.getTwitterID();
            nameValuePairs.add(new BasicNameValuePair("access_token", account.getToken()));
            nameValuePairs.add(new BasicNameValuePair("access_token_secret", account.getTokenSecret()));
            nameValuePairs.add(new BasicNameValuePair("user_auth_twitter_id", String.valueOf(account.getTwitterID())));
            nameValuePairs.add(new BasicNameValuePair("twitter_endpoint", endpoint.getEndpointName()));
        }
        String url = ArtRetweeterMain.prop.getProperty("serverurl");
        try ( CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            int artRetweeterServerErrors = 0;
            int twitterAPIErrors = 0;
            while (artRetweeterServerErrors < 3 && twitterAPIErrors < 3) {
                try ( CloseableHttpResponse response = httpclient.execute(httpPost)) {
                    Pair<OperationResult, JsonObject> checkResult = processResponse(endpoint.getEndpointName(), userTwitterID, response);
                    int httpCode = response.getStatusLine().getStatusCode();
                    if (httpCode != 200 || checkResult.getLeft() != null) {
                        if (httpCode != 200) {
                            LOGGER.error("HTTP code: " + httpCode);
                            artRetweeterServerErrors++;
                        } else {
                            twitterAPIErrors++;
                        }

                        if (twitterAPIErrors == 3) {
                            OperationResult sResp = checkResult.getLeft();
                            if (sResp != null) {
                                if (sResp.getTwitterResponse() != null && (sResp.getTwitterResponse().getTwitterErrorCode() < 500
                                        || sResp.getTwitterResponse().getTwitterErrorCode() > 599)) {
                                    twitterAPIErrors++;
                                } else {
                                    return sResp;
                                }
                            } else {
                                return checkResult.getLeft();
                            }
                        }
                        if (artRetweeterServerErrors == 3) {
                            LOGGER.error("Max server errors reached for API request to endpoint: " + endpoint.getEndpointName());
                            if (checkResult.getLeft() != null) {
                                return checkResult.getLeft();
                            } else {
                                opResult.setServerResponse(new ServerResponse(StatusCode.ARTRETWEETER_SERVER_ERROR));
                                return opResult;
                            }
                        }
                        try {
                            Thread.sleep((artRetweeterServerErrors + twitterAPIErrors) * 1000);
                        } catch (Exception e) {
                            LOGGER.error("Interrupted while waiting to retry API request", e);
                            opResult.setClientResponse(new ClientResponse(StatusCode.INTERRUPTED_ERROR));
                            return opResult;
                        }
                        continue;
                    }
                    JsonObject responseJSON = checkResult.getRight();
                    opResult.setTwitterResponse(new TwitterResponse(StatusCode.SUCCESS));
                    opResult.getTwitterResponse().setHeaderJSON(responseJSON.get("headers").getAsJsonObject());
                    if (responseJSON.get("response").isJsonObject()) {
                        opResult.getTwitterResponse().setResponseJSONObject(responseJSON.get("response").getAsJsonObject());
                    } else if (responseJSON.get("response").isJsonArray()) {
                        opResult.getTwitterResponse().setResponseJSONArray(responseJSON.get("response").getAsJsonArray());
                    }
                    opResult.getTwitterResponse().setStatusCode(StatusCode.SUCCESS);
                    return opResult;
                } catch (Exception e) {
                    LOGGER.error("Failed to get HTTP response!", e);
                    opResult.setServerResponse(new ServerResponse(StatusCode.CONNECTION_ERROR));
                    return opResult;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create HTTP client!", e);
            opResult.setClientResponse(new ClientResponse(StatusCode.MISC_ERROR));
            return opResult;
        }
        return opResult;
    }

    public static OperationResult statusesDestroy(Long tweetID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", String.valueOf(tweetID)));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.STATUSES_DESTROY, account);
        return apiCallResult;
    }

    public static OperationResult statusesRetweet(Long tweetID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", String.valueOf(tweetID)));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.STATUSES_RETWEET, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        Gson gson = new Gson();
        StatusJSON retweetedStatus = gson.fromJson(responseJSON.get("retweeted_status").getAsJsonObject(), StatusJSON.class);
        apiCallResult.getTwitterResponse().setReturnedObject(retweetedStatus);
        return apiCallResult;
    }

    public static OperationResult statusesUnretweet(Long retweetID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", String.valueOf(retweetID)));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.STATUSES_UNRETWEET, account);
        return apiCallResult;
    }

    public static OperationResult getTweetsByIDs(ArrayList<Long> tweetIDs, Account account, Path tweetFolderPath) {
        List<NameValuePair> nvps = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (Long tweetID : tweetIDs) {
            sb = sb.append(String.valueOf(tweetID)).append(",");
        }
        sb.setLength(sb.length() - 1);
        nvps.add(new BasicNameValuePair("id", sb.toString()));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.STATUSES_LOOKUP, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        Gson gson = new Gson();
        JsonArray responseJSON = apiCallResult.getTwitterResponse().getResponseJSONArray();
        StatusJSON[] receivedStatuses;
        try {
            receivedStatuses = gson.fromJson(responseJSON, StatusJSON[].class);
        } catch (Exception e1) {
            LOGGER.error("Failed to parse returned statuses JSON!", e1);
            apiCallResult.setClientResponse(new ClientResponse(StatusCode.JSON_PARSE_ERROR));
            return apiCallResult;
        }
        ArrayList<Object[]> params = new ArrayList<>();
        ArrayList<StatusJSON> statuses = new ArrayList<>();
        for (StatusJSON status : receivedStatuses) {
            if (status.getIn_reply_to_screen_name() != null
                    && !status.getIn_reply_to_screen_name().equals(account.getScreenName())) {
                continue;
            }
            if (status.getExtended_entities() == null) {
                continue;
            }
            Integer extendedEntitiesLength = status.getExtended_entities().getMedia().length;
            if (extendedEntitiesLength == 0) {
                continue;
            } else if (!status.getExtended_entities().getMedia()[0].getType().equals("photo")) {
                continue;
            }
            if (status.getRetweeted_status() != null) {
                continue;
            }
            OperationResult tweetParamsResult = status.downloadAndGetDBParams(tweetFolderPath);
            if (tweetParamsResult.wasSuccessful()) {
                params.add((Object[]) tweetParamsResult.getClientResponse().getReturnedObject());
            } else {
                return tweetParamsResult;
            }
            statuses.add(status);
        }
        if (!params.isEmpty()) {
            if (!TweetsDB.parameterisedTweetMergeBatch(params)) {
                apiCallResult.setClientResponse(new ClientResponse(StatusCode.DB_ERROR));
                apiCallResult.getClientResponse().setExtraStatusMessage("Failed to update database with tweet batch!");
                return apiCallResult;
            }
        }
        apiCallResult.getTwitterResponse().setReturnedObject(statuses);
        return apiCallResult;
    }

    public static OperationResult getTweetByID(Long tweetID, Account account, Path tweetFolderPath,
            boolean mustBeUserAccount, boolean downloadAndStore) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", String.valueOf(tweetID)));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.STATUSES_SHOW, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        Gson gson = new Gson();
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        StatusJSON status = gson.fromJson(responseJSON, StatusJSON.class);
        if (!status.getUser().getId().equals(account.getTwitterID())) {
            apiCallResult.setClientResponse(new ClientResponse(StatusCode.DOWNLOAD_ERROR,
                    "You can only download tweets from your own account."));
            return apiCallResult;
        }
        apiCallResult.getTwitterResponse().setReturnedObject(status);
        if (downloadAndStore) {
            OperationResult tweetParamsResult = status.downloadAndGetDBParams(tweetFolderPath);
            if (tweetParamsResult.wasSuccessful()) {
                Object[] tweetParams = (Object[]) tweetParamsResult.getClientResponse().getReturnedObject();
                TweetsDB.insertTweet(tweetParams);
            }
        }
        return apiCallResult;
    }

    public static OperationResult collectionsEntriesAdd(String collectionID, Long tweetID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", collectionID));
        nvps.add(new BasicNameValuePair("tweet_id", String.valueOf(tweetID)));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.COLLECTIONS_ENTRIES_ADD, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        JsonObject response = responseJSON.get("response").getAsJsonObject();
        if (response.keySet().size() == 1 && response.keySet().contains("errors")) {
            JsonArray errors = response.get("errors").getAsJsonArray();
            if (errors.size() != 0) {
                apiCallResult.getTwitterResponse().setErrorJSON(errors.get(0).getAsJsonObject());
                apiCallResult.getTwitterResponse().setStatusCode(StatusCode.TWITTER_API_ERROR);
            }
        }
        return apiCallResult;
    }

    public static OperationResult collectionsEntriesRemove(String collectionID, Long tweetID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", collectionID));
        nvps.add(new BasicNameValuePair("tweet_id", String.valueOf(tweetID)));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.COLLECTIONS_ENTRIES_REMOVE, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        JsonObject response = responseJSON.get("response").getAsJsonObject();
        if (response.keySet().size() == 1 && response.keySet().contains("errors")) {
            JsonArray errors = response.get("errors").getAsJsonArray();
            if (errors.size() != 0) {
                apiCallResult.getTwitterResponse().setErrorJSON(errors.get(0).getAsJsonObject());
                apiCallResult.getTwitterResponse().setStatusCode(StatusCode.TWITTER_API_ERROR);
            }
        }
        return apiCallResult;
    }

    public static OperationResult collectionsDestroy(String collectionID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", collectionID));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.COLLECTIONS_DESTROY, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        String destroyed = responseJSON.get("destroyed").getAsString();
        if (destroyed.toLowerCase().equals("true")) {
            apiCallResult.getTwitterResponse().setStatusCode(StatusCode.SUCCESS);
        } else {
            apiCallResult.getTwitterResponse().setStatusCode(StatusCode.TWITTER_API_ERROR);
        }
        return apiCallResult;
    }

    public static OperationResult collectionsCreate(String name, String description, CollectionOrdering ordering, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("name", name));
        nvps.add(new BasicNameValuePair("description", description));
        nvps.add(new BasicNameValuePair("timeline_order", ordering.getParameterName()));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.COLLECTIONS_CREATE, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        TwitterCollectionHolder holder;
        String id = responseJSON.get("response").getAsJsonObject().get("timeline_id").getAsString();
        apiCallResult = collectionsShow(id, account);
        if (!apiCallResult.wasSuccessful()) {
            LOGGER.info("Failed to show collection - manually creating collection object.");
            String collectionURL = "https://twitter.com/".concat(account.getScreenName()).concat("/timelines/")
                    .concat(id.substring(id.indexOf("-") + 1));
            holder = new TwitterCollectionHolder()
                    .setTwitterID(id)
                    .setName(name)
                    .setCollectionURL(collectionURL)
                    .setDescription(description)
                    .setOrdering(CollectionOrdering.CURATION_REVERSE_CHRON);
            apiCallResult.getTwitterResponse().setReturnedObject(holder);
        }
        apiCallResult.getTwitterResponse().setStatusCode(StatusCode.SUCCESS);
        return apiCallResult;
    }

    public static OperationResult collectionsEntriesCurate(CollectionCurateParamsJSON jsonData, Account account) {
        Gson gson = new Gson();
        String jsonDataString = gson.toJson(jsonData, CollectionCurateParamsJSON.class);
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("json_data", jsonDataString));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.COLLECTIONS_ENTRIES_CURATE, account);
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        CollectionCurateRespJSON returnObject = gson.fromJson(responseJSON, CollectionCurateRespJSON.class);
        apiCallResult.getTwitterResponse().setReturnedObject(returnObject);
        return apiCallResult;
    }

    public static OperationResult oauthInvalidateToken(Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.OAUTH_INVALIDATE_TOKEN, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        return apiCallResult;
    }

    public static OperationResult oauthRequestToken() {
        List<NameValuePair> nvps = new ArrayList<>();
        String requestParameters = "['oauth_callback' => 'oob']";
        nvps.add(new BasicNameValuePair("twitter_endpoint", "oauth/request_token"));
        nvps.add(new BasicNameValuePair("requestparameters", requestParameters));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.OAUTH_REQUEST_TOKEN, null);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        RequestToken requestToken;

        String token = responseJSON.get("oauth_token").getAsString();
        String tokenSecret = responseJSON.get("oauth_token_secret").getAsString();
        Boolean callbackConfirmed = responseJSON.get("oauth_callback_confirmed").getAsBoolean();
        requestToken = new RequestToken(token, tokenSecret, callbackConfirmed);
        apiCallResult.getTwitterResponse().setReturnedObject(requestToken);
        return apiCallResult;
    }

    public static OperationResult oauthAccessToken(String pin, RequestToken token) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("oauth_token", token.getToken()));
        nvps.add(new BasicNameValuePair("oauth_verifier", pin));
        nvps.add(new BasicNameValuePair("twitter_endpoint", "oauth/access_token"));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.OAUTH_ACCESS_TOKEN, null);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        Account account;

        String accessTokenString = responseJSON.get("oauth_token").getAsString();
        String accessTokenSecret = responseJSON.get("oauth_token_secret").getAsString();
        Long userTwitterID = responseJSON.get("user_id").getAsLong();
        String screenName = responseJSON.get("screen_name").getAsString();
        account = new Account()
                .setScreenName(screenName)
                .setTwitterID(userTwitterID)
                .setToken(accessTokenString)
                .setTokenSecret(accessTokenSecret);
        apiCallResult.getTwitterResponse().setReturnedObject(account);
        return apiCallResult;
    }

    public static OperationResult getCollectionsByUserID(Long userID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("user_id", String.valueOf(userID)));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.COLLECTIONS_LIST, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        ArrayList<TwitterCollectionHolder> collections = new ArrayList<>();
        Set<String> keys = responseJSON.keySet();
        if (keys.contains("objects")) {
            JsonObject objects = responseJSON.get("objects").getAsJsonObject();
            if (objects.keySet().contains("timelines")) {
                JsonObject timelines = responseJSON.get("objects").getAsJsonObject().get("timelines").getAsJsonObject();
                Set<String> tweetIDs = timelines.keySet();
                ArrayList<Object[]> params = new ArrayList<>();
                for (String s : tweetIDs) {
                    JsonObject o = timelines.get(s).getAsJsonObject();
                    TwitterCollectionHolder holder = new TwitterCollectionHolder()
                            .setTwitterID(s)
                            .setDescription(o.get("description").getAsString())
                            .setName(o.get("name").getAsString())
                            .setCollectionURL(o.get("collection_url").getAsString())
                            .setOrdering(CollectionOrdering.getOrdering(o.get("timeline_order").getAsString()));
                    collections.add(holder);
                    params.add(new Object[]{account.getTwitterID(), s, holder.getCollectionURL(), holder.getName(),
                        holder.getDescription(), holder.getOrdering().getParameterName()});
                }
                String query = "DELETE FROM collectiontweets WHERE collectionid IN "
                        + "(SELECT collectionid FROM collections WHERE usertwitterid=?)";
                CoreDB.runCustomUpdate(query, new Object[]{account.getTwitterID()});
                CoreDB.deleteFromTable(DBTable.COLLECTIONS,
                        new String[]{"usertwitterid"},
                        new Object[]{account.getTwitterID()});
                if (!params.isEmpty()) {
                    if (!CollectionsDB.parameterisedCollectionMergeBatch(params)) {

                    }
                }
            }
        } else {
            apiCallResult.getTwitterResponse().setStatusCode(StatusCode.JSON_PARSE_ERROR);
            apiCallResult.getTwitterResponse().setExtraStatusMessage("Unable to parse JSON!");
            return apiCallResult;
        }

        apiCallResult.getTwitterResponse().setReturnedObject(collections);
        return apiCallResult;
    }

    public static OperationResult collectionsShow(String collectionID, Account account) {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", collectionID));
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.COLLECTIONS_SHOW, account);
        if (!apiCallResult.wasSuccessful()) {
            LOGGER.error("Failed to show collection with ID: " + collectionID);
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        try {
            JsonObject obj = responseJSON.get("objects").getAsJsonObject().get("timelines").getAsJsonObject();
            String[] keys = obj.keySet().toArray(new String[obj.keySet().size()]);
            if (keys.length == 1 && keys[0].equals(collectionID)) {
                JsonObject collectionObject = obj.get(keys[0]).getAsJsonObject();
                String collectionURL = collectionObject.get("collection_url").getAsString();
                String name = collectionObject.get("name").getAsString();
                String description = collectionObject.get("description").getAsString();
                CollectionOrdering ordering = CollectionOrdering.getOrdering(collectionObject.get("timeline_order").getAsString());
                TwitterCollectionHolder holder = new TwitterCollectionHolder()
                        .setTwitterID(collectionID)
                        .setDescription(description)
                        .setName(name)
                        .setCollectionURL(collectionURL)
                        .setOrdering(ordering);
                apiCallResult.getTwitterResponse().setReturnedObject(holder);
            } else {
                LOGGER.error("Collection not found in response JSON!");
                apiCallResult.getTwitterResponse().setStatusCode(StatusCode.MISC_ERROR);
                return apiCallResult;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse JSON response", e);
            apiCallResult.getTwitterResponse().setStatusCode(StatusCode.JSON_PARSE_ERROR);
            return apiCallResult;
        }
        apiCallResult.getTwitterResponse().setStatusCode(StatusCode.SUCCESS);
        return apiCallResult;
    }

    /**
     * Call this only when the collection is already present within the database.
     *
     * @param collectionID
     * @param account
     * @return
     */
    public static OperationResult getFullyHydratedCollectionByID(String collectionID, Account account) {
        OperationResult apiCallResult = new OperationResult();
        Path tweetFolderPath = ConfigDB.getTweetFolderPath(account);
        if (tweetFolderPath == null) {
            apiCallResult.setClientResponse(new ClientResponse(StatusCode.DB_ERROR));
            apiCallResult.getClientResponse().setExtraStatusMessage("Unable to retrieve tweet folder path from DB!");
            return apiCallResult;
        }
        DBResponse collectionCheckResp = CoreDB.selectFromTable(DBTable.COLLECTIONS,
                new String[]{"collectionid"},
                new Object[]{collectionID});
        if (!collectionCheckResp.wasSuccessful() || collectionCheckResp.getReturnedRows().isEmpty()) {
            apiCallResult.setClientResponse(new ClientResponse(StatusCode.DB_ERROR));
            apiCallResult.getClientResponse().setExtraStatusMessage("Unable to retrieve collection information from DB!");
            return apiCallResult;
        }

        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("id", collectionID));
        apiCallResult = apiCall(nvps, TwitterEndpoint.COLLECTIONS_ENTRIES, account);
        if (!apiCallResult.wasSuccessful()) {
            return apiCallResult;
        }
        JsonObject responseJSON = apiCallResult.getTwitterResponse().getResponseJSONObject();
        Set<String> keys = responseJSON.keySet();
        if (keys.contains("objects")) {
            JsonObject objects = responseJSON.get("objects").getAsJsonObject();
            if (objects.keySet().contains("tweets")) {
                JsonObject tweets = responseJSON.get("objects").getAsJsonObject().get("tweets").getAsJsonObject();
                Set<String> tweetIDs = tweets.keySet();
                ArrayList<Long> tweetIDsList = new ArrayList<>();
                for (String tweetID : tweetIDs) {
                    tweetIDsList.add(Long.valueOf(tweetID));
                }
                OperationResult statusesResult = RESTAPI.getTweetsByIDs(tweetIDsList, account, tweetFolderPath);
                if (!statusesResult.wasSuccessful()) {
                    return statusesResult;
                }
                ArrayList<StatusJSON> statuses = (ArrayList<StatusJSON>) statusesResult.getTwitterResponse().getReturnedObject();
                ArrayList<Object[]> tweetMergeParams = new ArrayList<>();
                ArrayList<Object[]> collectionTweetsMergeParams = new ArrayList<>();
                for (StatusJSON status : statuses) {
                    OperationResult res = status.downloadAndGetDBParams(tweetFolderPath);
                    if (res.wasSuccessful()) {
                        Object[] params = (Object[]) res.getClientResponse().getReturnedObject();
                        tweetMergeParams.add(params);
                        collectionTweetsMergeParams.add(new Object[]{status.getId(), collectionID});
                    }
                }
                CoreDB.deleteFromTable(DBTable.COLLECTIONTWEETS,
                        new String[]{"collectionid"},
                        new Object[]{collectionID});
                TweetsDB.parameterisedTweetMergeBatch(tweetMergeParams);
                CollectionsDB.parameterisedCollectionTweetsMergeBatch(collectionTweetsMergeParams);
            }
        } else {
            apiCallResult.setClientResponse(new ClientResponse(StatusCode.JSON_PARSE_ERROR));
            apiCallResult.getClientResponse().setExtraStatusMessage("Unable to parse JSON!");
            return apiCallResult;
        }

        return apiCallResult;
    }

    public static Pair<OperationResult, ArrayList<StatusJSON>> getUnrecordedUserTweetsByDate(CloseableHttpClient httpclient,
            Account account, Long maxID, Long sinceID, Path tweetFolderPath) {
        List<NameValuePair> nvps = new ArrayList<>();
        ArrayList<StatusJSON> statuses = new ArrayList<>();
        nvps.add(new BasicNameValuePair("screen_name", account.getScreenName()));
        boolean maxIDSet = false;
        if (!maxID.equals(Long.MAX_VALUE) && !maxID.equals(0L)) {
            nvps.add(new BasicNameValuePair("max_id", String.valueOf(maxID)));
            maxIDSet = true;
        }
        if (!maxIDSet && !sinceID.equals(0L)) {
            nvps.add(new BasicNameValuePair("since_id", String.valueOf(sinceID)));
        }
        OperationResult apiCallResult = apiCall(nvps, TwitterEndpoint.USER_TIMELINE, account);
        if (!apiCallResult.wasSuccessful()) {
            return Pair.of(apiCallResult, null);
        }
        JsonArray responseJSON = apiCallResult.getTwitterResponse().getResponseJSONArray();
        ArrayList<Object[]> params = new ArrayList<>();
        Gson gson = new Gson();

        StatusJSON[] receivedStatuses;
        try {
            receivedStatuses = gson.fromJson(responseJSON, StatusJSON[].class);
        } catch (Exception e1) {
            LOGGER.error("Failed to parse returned statuses JSON!", e1);
            apiCallResult.setClientResponse(new ClientResponse(StatusCode.JSON_PARSE_ERROR));
            apiCallResult.getClientResponse().setExtraStatusMessage("Failed to parse returned statuses JSON!");
            return Pair.of(apiCallResult, null);
        }
        LOGGER.debug("Number of statuses received: " + receivedStatuses.length);
        for (StatusJSON status : receivedStatuses) {
            if (maxID > status.getId() && !maxID.equals(0L)) {
                maxID = status.getId();
            }
            if (sinceID < status.getId()) {
                sinceID = status.getId();
            }
            if (status.getIn_reply_to_screen_name() != null
                    && !status.getIn_reply_to_screen_name().equals(account.getScreenName())) {
                continue;
            }
            if (status.getExtended_entities() == null) {
                continue;
            }
            Integer extendedEntitiesLength = status.getExtended_entities().getMedia().length;
            if (extendedEntitiesLength == 0) {
                continue;
            } else if (!status.getExtended_entities().getMedia()[0].getType().equals("photo")) {
                continue;
            }
            if (status.getRetweeted_status() != null) {
                continue;
            }
            OperationResult tweetParamsResult = status.downloadAndGetDBParams(tweetFolderPath, account);
            if (tweetParamsResult.wasSuccessful()) {
                params.add((Object[]) tweetParamsResult.getClientResponse().getReturnedObject());
            } else {
                apiCallResult.setTwitterResponse(tweetParamsResult.getTwitterResponse());
                return Pair.of(apiCallResult, statuses);
            }

            statuses.add(status);
        }
        if (receivedStatuses.length != 0) {
            maxID--;
        }
        if (!params.isEmpty()) {
            if (!TweetsDB.parameterisedTweetMergeBatch(params)) {
                apiCallResult.setClientResponse(new ClientResponse(StatusCode.DB_ERROR));
                apiCallResult.getClientResponse().setExtraStatusMessage("Failed to update database with tweet batch!");
                return Pair.of(apiCallResult, statuses);
            }
        }
        apiCallResult.getTwitterResponse().setReturnedObject(Pair.of(maxID, sinceID));
        apiCallResult.getTwitterResponse().setReceivedTweetCount(receivedStatuses.length);
        apiCallResult.getTwitterResponse().setStoredTweetCount(statuses.size());
        return Pair.of(apiCallResult, statuses);
    }

    public static void validateStoredTweets() {
        DBResponse accountsResp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!accountsResp.wasSuccessful()) {
            LOGGER.error("Failed to get accounts from DB - aborting tweet validation.");
            return;
        }
        ArrayList<HashMap<String, Object>> rows = accountsResp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            Account account = ResultSetConversion.getAccount(row);
            validateStoredTweets(account);
        }

    }

    public static void validateStoredTweets(Account account) {
        Path tweetFolderPath = ConfigDB.getTweetFolderPath(account);
        String query = "SELECT * FROM tweets WHERE usertwitterid=?";
        DBResponse resp = CoreDB.customQuerySelect(query, account.getTwitterID());
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to get tweets from DB - aborting tweet validation.");
            return;
        }
        if (resp.getReturnedRows().isEmpty()) {
            return;
        }
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        HashMap<Long, TweetHolder> fullTweetMap = new HashMap<>();
        ArrayList<Long> allReturnedIDs = new ArrayList<>();
        ArrayList<ArrayList<Long>> tweetIDLists = new ArrayList<>();
        ArrayList<Long> currentList = new ArrayList<>();
        for (HashMap<String, Object> row : rows) {
            TweetHolder tweet = ResultSetConversion.getTweet(row);
            fullTweetMap.put(tweet.getTweetID(), tweet);
            currentList.add(tweet.getTweetID());
            // Maximum number of statuses requestable via statuses/lookup endpoint
            if (currentList.size() == 100) {
                tweetIDLists.add(currentList);
                currentList = new ArrayList<>();
            }
        }
        if (!currentList.isEmpty()) {
            tweetIDLists.add(currentList);
        }
        for (ArrayList<Long> tweetIDList : tweetIDLists) {
            OperationResult result = getTweetsByIDs(tweetIDList, account, tweetFolderPath);
            if (!result.wasSuccessful()) {
                LOGGER.error("Failed to retrieve tweets from API for account: " + account.getScreenName());
                LOGGER.error("Number of tweets in request: " + tweetIDList.size());
                return;
            }
            ArrayList<StatusJSON> statuses = (ArrayList<StatusJSON>) result.getTwitterResponse().getReturnedObject();
            for (StatusJSON status : statuses) {
                allReturnedIDs.add(status.getId());
            }
        }

        Set<Long> fullTweetList = fullTweetMap.keySet();

        if (fullTweetList.size() == allReturnedIDs.size()) {
            // No action needed: all tweets are accounted for
            return;
        }
        ArrayList<Long> deleteParams = new ArrayList<>();
        for (Long tweetID : fullTweetList) {
            if (!allReturnedIDs.contains(tweetID)) {
                // Tweet is deleted or otherwise inaccessible: remove it and images associated with it
                deleteParams.add(tweetID);
            }
        }
        TweetsDB.deleteTweets(deleteParams, true);
    }

}
