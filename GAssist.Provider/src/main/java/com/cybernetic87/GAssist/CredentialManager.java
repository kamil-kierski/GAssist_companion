package com.cybernetic87.GAssist;
/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.ObjectUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Credential manager to get, save, delete user credentials.
 *
 * @author jbd@google.com (Burcu Dogan)
 */
class CredentialManager {

    private static final List<String> SCOPES = Collections.singletonList(
            // Required to access and manipulate files.
            "https://www.googleapis.com/auth/assistant-sdk-prototype");
    private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    private final GoogleClientSecrets clientSecrets;
    private final Context context;
    private final HttpTransport transport = new NetHttpTransport.Builder().build();
    private Handler mainHandler;

//    private static AppEngineCredentialStore credentialStore =
//            new AppEngineCredentialStore();

    /**
     * Credential Manager constructor.
     *
     * @param clientSecrets App client secrets to be used during OAuth2 exchanges.
     */
    public CredentialManager(GoogleClientSecrets clientSecrets, Context context) {
        this.clientSecrets = clientSecrets;
        this.context = context;
        mainHandler = new Handler(context.getMainLooper());
    }

    /**
     * Builds an empty credential object.
     *
     * @return An empty credential object.
     */
    private Credential buildEmpty() {
        return new GoogleCredential.Builder()
                .setClientSecrets(this.clientSecrets)
                .setTransport(transport)
                .setJsonFactory(jsonFactory)
                .build();
    }

    /**
     * Generates a consent page url.
     *
     * @return A consent page url string for user redirection.
     */
    public String getAuthorizationUrl() {
        GoogleAuthorizationCodeRequestUrl urlBuilder =
                new GoogleAuthorizationCodeRequestUrl(
                        clientSecrets.getDetails().getClientId(),
                        clientSecrets.getDetails().getRedirectUris().get(0), SCOPES);
        return urlBuilder.build();
    }

    /**
     * Retrieves a new access token by exchanging the given code with OAuth2
     * end-points.
     *
     * @param code Exchange code.
     * @return A credential object.
     */
    public Credential retrieve(String code) {
        GoogleTokenResponse response;
        try {
            response = new GoogleAuthorizationCodeTokenRequest(
                    transport,
                    jsonFactory,
                    clientSecrets.getDetails().getClientId(),
                    clientSecrets.getDetails().getClientSecret(),
                    code,
                    clientSecrets.getDetails().getRedirectUris().get(0)).execute();
            //return buildEmpty().setRefreshToken(response.getRefreshToken()).setAccessToken(response.getAccessToken());
        } catch (IOException e) {
            ShowToast("Failed to get credentials, try again.");
            //mainHandler.post(() -> enterCodePrompt());
            return null;
        }

        if (ObjectUtils.allNotNull(response.getRefreshToken(), response.getAccessToken())) {
            return buildEmpty().setRefreshToken(response.getRefreshToken()).setAccessToken(response.getAccessToken());
        } else {
            ShowToast("Failed to get credentials, try again.");
            //enterCodePrompt();
            return null;
        }
    }


    public boolean SaveCredentials(Credential cred) {
        //File dir = Environment.getExternalStorageDirectory();
        final String path = context.getFilesDir().getAbsolutePath() + "/credentials.json";
        try {
            JsonObject jObj = new JsonObject();
            jObj.addProperty("refresh_token", cred.getRefreshToken());
            jObj.addProperty("token_uri", cred.getTokenServerEncodedUrl());
            jObj.addProperty("client_id", clientSecrets.getDetails().getClientId());
            jObj.addProperty("client_secret", clientSecrets.getDetails().getClientSecret());
            jObj.addProperty("project_id", (String) clientSecrets.getDetails().get("project_id"));
            jObj.addProperty("access_token", cred.getAccessToken());
            jObj.addProperty("type", "authorized_user");
            JsonArray jArr = new JsonArray();
            jArr.add(SCOPES.get(0));
            jObj.add("scopes", jArr);

            FileWriter fw = new FileWriter(path);
            fw.write(jObj.toString());
            fw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean ConfigureDevice(Credential credential) {
        DeviceConfiguration deviceConfig = new DeviceConfiguration(context);
        boolean result = deviceConfig.configureDevice((String) clientSecrets.getDetails().get("project_id"), credential.getAccessToken());
        if (!result) {
            ShowToast("Failed to configure device.\nGoogle Assistant API NOT ENABLED.\nCheck Google Console project configuration.");
        }
        return result;
    }

    private void ShowToast(String text) {
        // This is your code
        Runnable myRunnable = () -> Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        mainHandler.post(myRunnable);
    }
}