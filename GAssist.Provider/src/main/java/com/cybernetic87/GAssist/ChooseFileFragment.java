package com.cybernetic87.GAssist;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class ChooseFileFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private ImageView imageFileStatus;
    private TextView textFileStatus;
    private Button mButtonNext;
    private String secretsPath;

    public ChooseFileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // TODO: Rename and change types of parameters
            String mParam1 = getArguments().getString(ARG_PARAM1);
            String mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View chooseFileView = inflater.inflate(R.layout.fragment_choose_file, container, false);

        TextView tv = chooseFileView.findViewById(R.id.textView);
        imageFileStatus = chooseFileView.findViewById(R.id.imageFileStatus);
        textFileStatus = chooseFileView.findViewById(R.id.textFileStatus);

        final SpannableString s =
                new SpannableString("\nTo run this application you need a secrets.json file, follow this guide to obtain the file: https://youtu.be/VfunEUzzFVU\n\n" +
                        "Once you already have the file, click browse and select the file.");
        //Linkify.addLinks(s, Linkify.WEB_URLS);

        tv.setText(s);

        Button browseButton = chooseFileView.findViewById(R.id.browse_button);
        browseButton.setOnClickListener(view -> PickSecretsFile());

        mButtonNext = chooseFileView.findViewById(R.id.buttonDone);
        mButtonNext.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            bundle.putString("secretsPath", secretsPath);

            Navigation.findNavController(chooseFileView).navigate(R.id.action_chooseFile_to_authenticateFragment, bundle);
        });


        // Inflate the layout for this fragment
        return chooseFileView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void PickSecretsFile() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.STORAGE_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = new String[]{"json"};

        FilePickerDialog dialog = new FilePickerDialog(getContext(), properties);
        dialog.setTitle("Select the secrets.json file:");
        dialog.show();

        dialog.setDialogSelectionListener(files -> {
            String pathSecrets = files[0];
            if (ValidateJsonFile(pathSecrets, "installed")) {
                textFileStatus.setText("File loaded successfully");
                imageFileStatus.setImageResource(R.mipmap.ic_tick_green);
                mButtonNext.setEnabled(true);
            } else {
                textFileStatus.setText("File not loaded");
                imageFileStatus.setImageResource(R.mipmap.ic_x_red);
            }
        });

    }

    private boolean ValidateJsonFile(String path, String property) {
        String text = null;
        JSONObject obj;
        if (new File(path).exists()) {
            try {
                text = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Toast.makeText(getContext(), "Wrong path", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            try {
                obj = new JSONObject(Objects.requireNonNull(text));
            } catch (JSONException e) {
                Toast.makeText(getContext(), "Not a valid JSON file", Toast.LENGTH_LONG).show();
                return false;
            }

            if (!obj.has(property)) {
                String message = String.format("Wrong file: %s", new File(path).getName());
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                return false;
            }
            secretsPath = path;
            return true;
        }
        return false;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
    }
}
