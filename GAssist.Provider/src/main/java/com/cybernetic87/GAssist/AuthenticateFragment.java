package com.cybernetic87.GAssist;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;


public class AuthenticateFragment extends Fragment {
    private static final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    private ImageView imageCode;
    private ImageView imageCredentials;
    private ImageView imageDevice;
    private TextView textViewCodeStatus;
    private TextView textViewCredentialsStatus;
    private TextView textViewDeviceStatus;

    private Uri secretsUri;
    private Button buttonDone;

    public AuthenticateFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View authenticateView = inflater.inflate(R.layout.fragment_authenticate, container, false);
        imageCode = authenticateView.findViewById(R.id.imageCode);
        imageCredentials = authenticateView.findViewById(R.id.imageCredentials);
        imageDevice = authenticateView.findViewById(R.id.imageDevice);
        textViewCodeStatus = authenticateView.findViewById(R.id.textViewCodeStatus);
        textViewCredentialsStatus = authenticateView.findViewById(R.id.textViewCredentialsStatus);
        textViewDeviceStatus = authenticateView.findViewById(R.id.textViewDeviceStatus);
        buttonDone = authenticateView.findViewById(R.id.buttonDone);
        buttonDone.setOnClickListener(view -> Navigation.findNavController(authenticateView).navigate(R.id.action_authenticateFragment_to_fragment_main));


        Button buttonAuthenticate = authenticateView.findViewById(R.id.buttonAuthenticate);
        buttonAuthenticate.setOnClickListener(view -> {
            if (getArguments() != null) {
                secretsUri = Uri.parse(getArguments().getString("secretsUri"));
                enterCodePrompt();
//                GetCredentials(pathSecrets);
            }
        });

        return authenticateView;
    }

    public void enterCodePrompt() {

        GoogleClientSecrets clientSecrets = null;
        try {
            clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(getActivity().getContentResolver().openInputStream(secretsUri)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        CredentialManager cm = new CredentialManager(clientSecrets, Objects.requireNonNull(getContext()));

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cm.getAuthorizationUrl()));
        startActivity(browserIntent);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Paste code from Google Authentication here:");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> new Thread() {
            @Override
            public void run() {
                Credential cred = cm.retrieve(input.getText().toString());
                if (cred != null) {
                    new GetCredentialsTask(cred, cm).execute();
                    imageCode.setImageResource(R.mipmap.ic_tick_green);
                    textViewCodeStatus.setText(R.string.auth_success);
                } else {
                    imageCode.setImageResource(R.mipmap.ic_x_red);
                    textViewCodeStatus.setText(R.string.auth_error);
                }
            }
        }.start()).setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence c, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence c, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Will be called AFTER text has been changed.
                if (editable.toString().length() == 0) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });


        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private class GetCredentialsTask extends AsyncTask<Void, Integer, Void> {

        final Credential credential;
        final CredentialManager cm;

        public GetCredentialsTask(Credential credential, CredentialManager cm) {
            this.credential = credential;
            this.cm = cm;
        }

        @Override
        protected synchronized Void doInBackground(Void... voids) {

            if (cm.SaveCredentials(credential)) {
                publishProgress(1);
            } else {
                publishProgress(2);
            }

            if (cm.ConfigureDevice(credential)) {
                publishProgress(3);
            } else {
                publishProgress(4);
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            switch (progress[0]) {
                case 1: {
                    imageCredentials.setImageResource(R.mipmap.ic_tick_green);
                    textViewCredentialsStatus.setText(R.string.credentials_saved);
                    break;
                }
                case 2: {
                    imageCredentials.setImageResource(R.mipmap.ic_x_red);
                    textViewCredentialsStatus.setText(R.string.error_save_credentials);
                    break;
                }
                case 3: {
                    imageDevice.setImageResource(R.mipmap.ic_tick_green);
                    textViewDeviceStatus.setText(R.string.device_configured);
                    buttonDone.setEnabled(true);
                    break;
                }
                case 4: {
                    imageDevice.setImageResource(R.mipmap.ic_x_red);
                    textViewDeviceStatus.setText(R.string.failed_configure);
                }
            }
        }

    }
}
