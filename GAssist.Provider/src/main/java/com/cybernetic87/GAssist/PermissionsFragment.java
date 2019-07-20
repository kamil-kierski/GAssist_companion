package com.cybernetic87.GAssist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.List;
import java.util.Objects;

import static android.content.Context.POWER_SERVICE;

public class PermissionsFragment extends Fragment {

    private Button mButtonNext;
    private ImageView mImagePermissionStatus;
    private TextView mTextViewPermissionStatus;

    private ImageView mImageBatteryStatus;
    private TextView mTextViewBatteryStatus;

    private final PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            Toast.makeText(getContext(), "PERMISSION GRANTED", Toast.LENGTH_LONG).show();
            mButtonNext.setEnabled(true);
            mImagePermissionStatus.setImageResource(R.mipmap.ic_tick_green);
            mTextViewPermissionStatus.setText("Permissions granted");
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            mImagePermissionStatus.setImageResource(R.mipmap.ic_x_red);
            mTextViewPermissionStatus.setText("Permissions denied");
        }
    };

    public PermissionsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View permissionView = inflater.inflate(R.layout.fragment_permissions, container, false);
        mButtonNext = permissionView.findViewById(R.id.buttonDone);
        mButtonNext.setOnClickListener(view -> Navigation.findNavController(permissionView).navigate(R.id.action_fragment_permissions_to_chooseFile));

        Button mButtonCheckPermission = permissionView.findViewById(R.id.button);
        mButtonCheckPermission.setOnClickListener(view -> {
            //check all needed permissions together
            TedPermission.with(Objects.requireNonNull(getContext()))
                    .setPermissionListener(permissionlistener)
                    .setDeniedMessage("If you reject permission,you can not use this application\n\nPlease turn on permissions at [Setting] > [Permission]")
                    .setRationaleMessage("This application needs permission to read credentials.json file from external storage")
                    .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)
                    .check();
        });

        Button mButtonRequestBattery = permissionView.findViewById(R.id.button2);
        mButtonRequestBattery.setOnClickListener(view -> requestIgnoreBatteryOptimization());

        mImagePermissionStatus = permissionView.findViewById(R.id.imagePermissionStatus);
        mTextViewPermissionStatus = permissionView.findViewById(R.id.textViewPermissionStatus);

        mImageBatteryStatus = permissionView.findViewById(R.id.imageBatteryStatus);
        mTextViewBatteryStatus= permissionView.findViewById(R.id.textViewBatteryStatus);
        return permissionView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void requestIgnoreBatteryOptimization(){
        PowerManager pm = (PowerManager) getContext().getSystemService(POWER_SERVICE);
        String packageName = getContext().getPackageName();
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Intent myIntent = new Intent();
            myIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(myIntent);
        }
    }

}
