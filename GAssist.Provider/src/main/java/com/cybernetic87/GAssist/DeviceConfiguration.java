package com.cybernetic87.GAssist;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

class DeviceConfiguration {

    private final String path;
    private String mDeviceModelID;
    private String mDeviceInstanceID;

    public DeviceConfiguration(Context context) {
        path = context.getFilesDir().getAbsolutePath() + "/device.json";
    }

    private String getOrCreateModelId(String projectID, String accessToken) throws IOException {
        URL url = new URL(String.format("https://embeddedassistant.googleapis.com/v1alpha2/projects/%s/deviceModels/", projectID));

        JsonObject jsonObject = HttpGet(url, accessToken);
        if (jsonObject.size() == 0) {
            Log.e("DEVICE MODEL", "NO DEVICE MODELS, NEED TO CREATE HERE");
            JSONObject newDevice = new JSONObject();
            try {
                newDevice.accumulate("project_id", projectID);
                newDevice.accumulate("device_model_id", projectID + "_galaxy_watch");
                newDevice.accumulate("manifest", new JSONObject()
                        .accumulate("manufacturer", "samsung")
                        .accumulate("product_name", "galaxy_watch"));
                newDevice.accumulate("device_type", "action.devices.types.PHONE");

                JsonObject jsonObjectPostResponse = HttpPost(url, newDevice, accessToken);
                mDeviceModelID = jsonObjectPostResponse.get("deviceModelId").getAsString();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            JsonArray deviceModels = jsonObject.getAsJsonArray("deviceModels");

            Log.i("MSG", deviceModels.get(0).getAsJsonObject().get("deviceModelId").getAsString());
            mDeviceModelID = deviceModels.get(0).getAsJsonObject().get("deviceModelId").getAsString();
        }
        return mDeviceModelID;
    }

    private String getOrCreateDeviceInstanceId(String projectID, String accessToken) throws IOException, JSONException {
        String deviceInstanceId;

        URL url = new URL(String.format("https://embeddedassistant.googleapis.com/v1alpha2/projects/%s/devices/", projectID));

        JsonObject jsonObject = HttpGet(url, accessToken);

        if (jsonObject.size() == 0) {
            Log.e("JSON DEVICES", "EMPTY, SHOULD CREATE ");

            JSONObject newInstance = new JSONObject();
            newInstance.accumulate("id", "galaxy_watch_instance");
            newInstance.accumulate("model_id", mDeviceModelID);
            newInstance.accumulate("client_type", "SDK_SERVICE");

            Log.e("NEWINSTANCE", newInstance.toString());

            JsonObject jsonObjectPostResponse = HttpPost(url, newInstance, accessToken);
            deviceInstanceId = jsonObjectPostResponse.get("id").getAsString();
        } else {
            Log.e("JSON DEVICES", "NOT EMPTY SHOULD GET INSTANCEID HERE");
            JsonArray devices = jsonObject.getAsJsonArray("devices");
            deviceInstanceId = devices.get(0).getAsJsonObject().get("id").getAsString();
            Log.e("JSON DEVICES", "FOUND INSTANCE ID " + deviceInstanceId);
        }
        return deviceInstanceId;
    }

    private JsonObject HttpGet(URL url, String accessToken) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        Log.i("STATUS", String.valueOf(conn.getResponseCode()));
        JsonObject response = new JsonParser().parse(IOUtils.toString(conn.getInputStream(), "UTF-8")).getAsJsonObject();

        conn.disconnect();

        return response;
    }

    private JsonObject HttpPost(URL url, JSONObject content, String accessToken) {
        HttpURLConnection connPost = null;
        JsonObject postResponse = null;

        try {
            connPost = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Objects.requireNonNull(connPost).setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        connPost.setRequestProperty("Content-Type", "application/json");
        connPost.setRequestProperty("Authorization", "Bearer " + accessToken);

        setPostRequestContent(connPost, content);
        try {
            connPost.connect();
            Log.i("STATUS", String.valueOf(connPost.getResponseCode()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            postResponse = new JsonParser().parse(IOUtils.toString(connPost.getInputStream(), "UTF-8")).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return postResponse;
    }


    private void setPostRequestContent(HttpURLConnection conn,
                                       JSONObject jsonObject) {

        OutputStream os;
        try {
            os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            writer.write(jsonObject.toString());
            writer.flush();
            writer.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean configureDevice(String projectID, String accessToken) {
        try {
            mDeviceModelID = getOrCreateModelId(projectID, accessToken);
            mDeviceInstanceID = getOrCreateDeviceInstanceId(projectID, accessToken);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
        saveDeviceConfiguration();
        return true;
    }

    private void saveDeviceConfiguration() {
        JSONObject deviceJson = new JSONObject();
        try {
            deviceJson.accumulate("model_id", mDeviceModelID);
            deviceJson.accumulate("instance_id", mDeviceInstanceID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            FileWriter fw = new FileWriter(path);
            fw.write(deviceJson.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject readDeviceConfiguration() {

        JSONObject json = null;
        try {
            InputStream is = new FileInputStream(path);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            json = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

}
