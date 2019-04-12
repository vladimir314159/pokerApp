
package main_src ;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;


import tracking.image.MultipleTargetsImageTrackingActivity;
import tracking.image.SimpleImageTrackingActivity;
import rendering.external.ExternalRenderingActivity;
import rendering.internal.InternalRenderingActivity;

import com.wikitude.WikitudeSDK;
import com.wikitude.common.permission.PermissionManager;
import com.wikitude.common.util.SDKBuildInformation;
//import com.wikitude.nativesdksampleapp.R;
import camera.CameraSettingsActivity;

import com.CS499UKY.Pokerhands.R;

import java.util.Arrays;


public class
MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WikitudeSDK.deleteRootCacheDirectory(this);

        Button start = (Button) findViewById(R.id.start);
        start.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
             //   Class activity = SimpleImageTrackingActivity.class;
               // activity = MultipleTargetsImageTrackingActivity.class;

             //   Intent myIntent = new Intent(CurrentActivity.this, NextActivity.class);
              //  myIntent.putExtra("key", value); //Optional parameters
               // CurrentActivity.this.startActivity(myIntent);
                WikitudeSDK.getPermissionManager().checkPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PermissionManager.WIKITUDE_PERMISSION_REQUEST, new PermissionManager.PermissionManagerCallback() {
                    @Override
                    public void permissionsGranted(int requestCode) {
                      //  Class activity = SimpleImageTrackingActivity.class;
                        //activity = MultipleTargetsImageTrackingActivity.class;

                        final Intent intent = new Intent(MainActivity.this, MultipleTargetsImageTrackingActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public void permissionsDenied(String[] deniedPermissions) {
                        Toast.makeText(MainActivity.this, "The Wikitude SDK needs the following permissions to enable an AR experience: " + Arrays.toString(deniedPermissions), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void showPermissionRationale(final int requestCode, final String[] permissions) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                        alertBuilder.setCancelable(true);
                        alertBuilder.setTitle("Wikitude Permissions");
                        alertBuilder.setMessage("The Wikitude SDK needs the following permissions to enable an AR experience: " + Arrays.toString(permissions));
                        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                WikitudeSDK.getPermissionManager().positiveRationaleResult(requestCode, permissions);
                            }
                        });

                        AlertDialog alert = alertBuilder.create();
                        alert.show();
                    }
                });

            }

        });

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        WikitudeSDK.getPermissionManager().onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

   /* private void moveExpandableIndicatorToRight() {
        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        listView.setIndicatorBoundsRelative(width - dpToPx(EXPANDABLE_INDICATOR_START_OFFSET), width - dpToPx(EXPANDABLE_INDICATOR_END_OFFSET));
        listView.setIndicatorBoundsRelative(width - dpToPx(EXPANDABLE_INDICATOR_START_OFFSET), width - dpToPx(EXPANDABLE_INDICATOR_END_OFFSET));
    } */

    private int dpToPx(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public void showSdkBuildInformation() {
        final SDKBuildInformation sdkBuildInformation = WikitudeSDK.getSDKBuildInformation();
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.build_information_title)
                .setMessage(
                        getString(R.string.build_information_config) + sdkBuildInformation.getBuildConfiguration() + "\n" +
                                getString(R.string.build_information_date) + sdkBuildInformation.getBuildDate() + "\n" +
                                getString(R.string.build_information_number) + sdkBuildInformation.getBuildNumber() + "\n" +
                                getString(R.string.build_information_version) + WikitudeSDK.getSDKVersion()
                )
                .show();
    }
}
