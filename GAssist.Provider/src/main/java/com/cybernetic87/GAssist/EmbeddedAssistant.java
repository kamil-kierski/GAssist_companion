/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cybernetic87.GAssist;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.assistant.embedded.v1alpha2.AssistConfig;
import com.google.assistant.embedded.v1alpha2.AssistRequest;
import com.google.assistant.embedded.v1alpha2.AssistResponse;
import com.google.assistant.embedded.v1alpha2.AudioInConfig;
import com.google.assistant.embedded.v1alpha2.AudioOutConfig;
import com.google.assistant.embedded.v1alpha2.DeviceConfig;
import com.google.assistant.embedded.v1alpha2.DeviceLocation;
import com.google.assistant.embedded.v1alpha2.DialogStateIn;
import com.google.assistant.embedded.v1alpha2.EmbeddedAssistantGrpc;
import com.google.assistant.embedded.v1alpha2.ScreenOutConfig;
import com.google.auth.oauth2.UserCredentials;
import com.google.type.LatLng;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import io.grpc.stub.StreamObserver;

public class EmbeddedAssistant {
    private static final String TAG = EmbeddedAssistant.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final String ASSISTANT_API_ENDPOINT = "embeddedassistant.googleapis.com";

    //public boolean isConversation = false;
    public StreamObserver<AssistRequest> mAssistantRequestObserver;
    public ManagedChannel channel;
    private Handler mAssistantHandler;
    // Device Actions
    private DeviceConfig mDeviceConfig;
    // Callbacks
    private Handler mRequestHandler;
    private RequestCallback mRequestCallback;
    private Handler mConversationHandler;
    private ConversationCallback mConversationCallback;

    private final StreamObserver<AssistResponse> mAssistantResponseObserver =
            new StreamObserver<AssistResponse>() {
                @Override
                public void onNext(final AssistResponse value) {
                    if (DEBUG) {
                        Log.d(TAG, "Received response: " + value.toString());
                    }

//                    if (value.hasScreenOut()) {
//                        FileWriter fw = null;
//                        try {
//                            fw = new FileWriter(Environment.getExternalStorageDirectory() + "/Credentials/response.html");
//                            fw.write(value.getScreenOut().getData().toStringUtf8());
//                            fw.close();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                    }
                    mConversationHandler.post(() -> mConversationCallback.onResponseStarted(value.toByteArray()));

                }


                @Override
                public void onError(final Throwable t) {
                    mConversationHandler.post(() -> {
                        stopConversation();
                        mConversationCallback.onError(t);
                    });
                }

                @Override
                public void onCompleted() {
                    //stopConversation();
                    mConversationHandler.post(() -> {
                        //stopConversation();
                        mConversationCallback.onConversationFinished();
                    });
                }
            };
    private String mLanguageCode = "en-US";
    private DeviceLocation mDeviceLocation;
    private AudioInConfig mAudioInConfig;
    private AudioOutConfig mAudioOutConfig;
    private int mVolume = 100; // Default to maximum volume.
    private ScreenOutConfig mScreenOutConfig;
    // gRPC client and stream observers.
    private HandlerThread mAssistantThread;
    private Context context;
    private EmbeddedAssistantGrpc.EmbeddedAssistantStub mAssistantService;
    private UserCredentials mUserCredentials;

    private EmbeddedAssistant() {
    }


    /**
     * Initializes the Assistant.
     */
    public void connect() {
        mAssistantThread = new HandlerThread("assistantThread");
        mAssistantThread.start();
        mAssistantHandler = new Handler(mAssistantThread.getLooper());

        channel = ManagedChannelBuilder.forTarget(ASSISTANT_API_ENDPOINT).build();
        mAssistantService = EmbeddedAssistantGrpc.newStub(channel)
                .withCallCredentials(MoreCallCredentials.from(mUserCredentials));
    }

    /**
     * Starts a request to the Assistant.
     */
    public void startConversation() {
        mConversationHandler.removeCallbacksAndMessages(null);
        mRequestHandler.post(() -> mRequestCallback.onRequestStart());
        mAssistantHandler.post(() -> {

            DialogStateIn.Builder dialogStateInBuilder = DialogStateIn.newBuilder();
            getLocation();
            if (mDeviceLocation != null) {
                dialogStateInBuilder.setDeviceLocation(mDeviceLocation);
            }
            dialogStateInBuilder.setLanguageCode(mLanguageCode);

            AssistConfig.Builder assistConfigBuilder = AssistConfig.newBuilder()
                    .setAudioInConfig(mAudioInConfig)
                    .setAudioOutConfig(mAudioOutConfig)
                    .setDeviceConfig(mDeviceConfig);
            if (mScreenOutConfig != null) {
                assistConfigBuilder.setScreenOutConfig(mScreenOutConfig);
            }
            assistConfigBuilder.setDialogStateIn(dialogStateInBuilder.build());

            mAssistantRequestObserver = mAssistantService.assist(mAssistantResponseObserver);

            mAssistantRequestObserver.onNext(
                    AssistRequest.newBuilder()
                            .setConfig(assistConfigBuilder.build())
                            .build());
        });
    }

    public void stopConversation() {
        mAssistantRequestObserver = null;
        mConversationHandler.removeCallbacksAndMessages(null);
        mConversationHandler.post(() -> mConversationCallback.onConversationFinished());
    }

    private void getLocation() {
        FusedLocationProviderClient flpClient = LocationServices.getFusedLocationProviderClient(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        flpClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng newLatLng = LatLng
                                .newBuilder()
                                .setLatitude(location.getLatitude())
                                .setLongitude(location.getLongitude())
                                .build();
                        mDeviceLocation = mDeviceLocation
                                .toBuilder()
                                .setCoordinates(newLatLng)
                                .build();
                    }
                });
    }

    /**
     * Set desired assistant response format.
     */
    public void setResponseFormat(ScreenOutConfig.ScreenMode mode) {
        mScreenOutConfig = ScreenOutConfig.newBuilder()
                .setScreenMode(mode)
                .build();
    }

    public void destroy() {
        channel.shutdown();
        mAssistantThread.quitSafely();
    }

    /**
     * Used to build an AssistantManager object.
     */
    public static class Builder {
        private final EmbeddedAssistant mEmbeddedAssistant;
        private int mSampleRate;
        private String mDeviceModelId;
        private String mDeviceInstanceId;

        /**
         * Creates a Builder.
         */
        public Builder() {
            mEmbeddedAssistant = new EmbeddedAssistant();
        }


        /**
         * Sets a {@link RequestCallback}, which is when a request is being made to the Assistant.
         *
         * @param requestCallback The methods that will run during a request.
         * @return Returns this builder to allow for chaining.
         */
        public Builder setRequestCallback(RequestCallback requestCallback) {
            setRequestCallback(requestCallback, null);
            return this;
        }

        /**
         * Sets a {@link RequestCallback}, which is when a request is being made to the Assistant.
         *
         * @param requestCallback The methods that will run during a request.
         * @param requestHandler  Handler used to dispatch the callback.
         */
        void setRequestCallback(RequestCallback requestCallback,
                                @Nullable Handler requestHandler) {
            if (requestHandler == null) {
                requestHandler = new Handler();
            }
            mEmbeddedAssistant.mRequestCallback = requestCallback;
            mEmbeddedAssistant.mRequestHandler = requestHandler;
        }

        /**
         * Sets a {@link ConversationCallback}, which is when a response is being given from the
         * Assistant.
         *
         * @param responseCallback The methods that will run during a response.
         * @return Returns this builder to allow for chaining.
         */
        public Builder setConversationCallback(ConversationCallback responseCallback) {
            setConversationCallback(responseCallback, null);
            return this;
        }

        /**
         * Sets a {@link ConversationCallback}, which is when a response is being given from the
         * Assistant.
         *
         * @param responseCallback The methods that will run during a response.
         * @param responseHandler  Handler used to dispatch the callback.
         */
        void setConversationCallback(ConversationCallback responseCallback,
                                     @Nullable Handler responseHandler) {
            if (responseHandler == null) {
                responseHandler = new Handler();
            }
            mEmbeddedAssistant.mConversationCallback = responseCallback;
            mEmbeddedAssistant.mConversationHandler = responseHandler;
        }

        public Builder setCredentials(UserCredentials userCredentials) {
            mEmbeddedAssistant.mUserCredentials = userCredentials;
            return this;
        }

        /**
         * Sets the audio sampling rate for input and output streams
         *
         * @param sampleRate The audio sample rate
         * @return Returns this builder to allow for chaining.
         */
        public Builder setAudioSampleRate(int sampleRate) {
            mSampleRate = sampleRate;
            return this;
        }

        /**
         * Sets the volume for the Assistant response
         *
         * @param volume The audio volume in the range 0 - 100.
         * @return Returns this builder to allow for chaining.
         */
        public Builder setAudioVolume(int volume) {
            mEmbeddedAssistant.mVolume = volume;
            return this;
        }

        /**
         * Sets the model id for each Assistant request.
         *
         * @param deviceModelId The device model id.
         * @return Returns this builder to allow for chaining.
         */
        public Builder setDeviceModelId(String deviceModelId) {
            mDeviceModelId = deviceModelId;
            return this;
        }

        /**
         * Sets the instance id for each Assistant request.
         *
         * @param deviceInstanceId The device instance id.
         * @return Returns this builder to allow for chaining.
         */
        public Builder setDeviceInstanceId(String deviceInstanceId) {
            mDeviceInstanceId = deviceInstanceId;
            return this;
        }

        /**
         * Sets language code of the request using IETF BCP 47 syntax.
         * See <a href='https://tools.ietf.org/html/bcp47'>for the documentation</a>.
         * For example: "en-US".
         *
         * @param languageCode Code for the language. Only Assistant-supported languages are valid.
         * @return Returns this builder to allow for chaining.
         */
        public Builder setLanguageCode(String languageCode) {
            mEmbeddedAssistant.mLanguageCode = languageCode;
            return this;
        }

        public Builder setDeviceLocation(DeviceLocation deviceLocation) {
            mEmbeddedAssistant.mDeviceLocation = deviceLocation;
            return this;
        }

        public Builder setContext(Context context) {
            mEmbeddedAssistant.context = context;
            return this;
        }

        /**
         * Returns an AssistantManager if all required parameters have been supplied.
         *
         * @return An inactive AssistantManager. Call {@link EmbeddedAssistant#connect()} to start
         * it.
         */
        public EmbeddedAssistant build() {


            // Construct audio configurations.
            mEmbeddedAssistant.mAudioInConfig = AudioInConfig.newBuilder()
                    .setEncoding(AudioInConfig.Encoding.LINEAR16)
                    .setSampleRateHertz(mSampleRate)
                    .build();
            mEmbeddedAssistant.mAudioOutConfig = AudioOutConfig.newBuilder()
                    .setEncoding(AudioOutConfig.Encoding.MP3)
                    .setSampleRateHertz(24000)
                    .setVolumePercentage(mEmbeddedAssistant.mVolume)
                    .build();


            // Construct DeviceConfig
            mEmbeddedAssistant.mDeviceConfig = DeviceConfig.newBuilder()
                    .setDeviceId(mDeviceInstanceId)
                    .setDeviceModelId(mDeviceModelId)
                    .build();

            // Construct default ScreenOutConfig
            mEmbeddedAssistant.mScreenOutConfig = ScreenOutConfig.newBuilder()
                    .setScreenMode(ScreenOutConfig.ScreenMode.PLAYING)
                    .build();

            return mEmbeddedAssistant;
        }
    }

    /**
     * Callback for methods during a request to the Assistant.
     */
    public static abstract class RequestCallback {

        /**
         * Called when a request is first made.
         */
        void onRequestStart() {
        }

    }

    /**
     * Callback for methods during a conversation from the Assistant.
     */
    public static abstract class ConversationCallback {


        public void onResponseStarted(byte[] response) {
        }

        public void onResponseFinished() {
        }

        public void onError(Throwable throwable) {
        }

        public void onConversationFinished() {
        }
    }
}