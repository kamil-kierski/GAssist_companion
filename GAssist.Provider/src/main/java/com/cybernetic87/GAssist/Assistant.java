package com.cybernetic87.GAssist;

import android.content.Context;
import android.util.Log;

import com.google.assistant.embedded.v1alpha2.AssistResponse;
import com.google.assistant.embedded.v1alpha2.DeviceLocation;
import com.google.assistant.embedded.v1alpha2.DialogStateOut;
import com.google.auth.oauth2.UserCredentials;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentValues.TAG;

class Assistant {


    private static final int SAMPLE_RATE = 16000;
    private static final String LANGUAGE_CODE = "en-US";
    private final Context context;
    private final ProviderService provider;
    private final JSONObject deviceConfigJson;

    Assistant(Context context, ProviderService provider) {
        this.context = context;
        this.provider = provider;

        DeviceConfiguration deviceConfig = new DeviceConfiguration(context);
        deviceConfigJson = deviceConfig.readDeviceConfiguration();
    }

    EmbeddedAssistant startAssistant() throws JSONException {

        UserCredentials userCredentials = null;

        String path = context.getFilesDir().getAbsolutePath() + "/credentials.json";

        InputStream is = null;
        try {
            is = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            userCredentials = UserCredentials.fromStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // List & adapter to store and display the history of Assistant Requests.
        EmbeddedAssistant mEmbeddedAssistant = new EmbeddedAssistant.Builder()
                .setCredentials(userCredentials)
                .setDeviceInstanceId(deviceConfigJson.getString("instance_id"))
                .setDeviceModelId(deviceConfigJson.getString("model_id"))
                .setDeviceLocation(DeviceLocation.newBuilder().build())
                .setLanguageCode(LANGUAGE_CODE)
                .setAudioSampleRate(SAMPLE_RATE)
                .setAudioVolume(100)
                .setContext(context)
                .setRequestCallback(new EmbeddedAssistant.RequestCallback() {
                })
                .setConversationCallback(new EmbeddedAssistant.ConversationCallback() {
                    @Override
                    public void onResponseStarted(byte[] response) {
                        provider.sendData(response);
                    }

//                    @Override
//                    public void onResponseFinished() {
//                        super.onResponseFinished();
//                    }

                    @Override
                    public void onError(Throwable throwable) {
                        DialogStateOut dso = DialogStateOut.newBuilder().setSupplementalDisplayText(throwable.getMessage()).build();

                        AssistResponse ar = AssistResponse
                                .newBuilder()
                                .setDialogStateOut(dso)
                                .setEventType(AssistResponse.EventType.END_OF_UTTERANCE)
                                .build();

                        provider.sendData(ar.toByteArray());
                        Log.e(TAG, "assist error: " + throwable.getMessage(), throwable);
                    }


                    @Override
                    public void onConversationFinished() {
                        Log.i(TAG, "assistant conversation finished");
                    }

                }).build();
        mEmbeddedAssistant.connect();
        return mEmbeddedAssistant;
    }
}
