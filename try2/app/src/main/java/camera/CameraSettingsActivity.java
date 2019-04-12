package camera;

import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.camera.CameraManager;
import com.wikitude.camera.CameraManagerListener;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.rendering.RenderExtension;
//import com.wikitude.nativesdksampleapp.R;
import com.CS499UKY.Pokerhands.R ;
import com.wikitude.rendering.ExternalRendering;
import main_src.WikitudeSDKConstants;
import rendering.external.CustomSurfaceView;
import rendering.external.Driver;
import rendering.external.GLRenderer;
import rendering.external.StrokedRectangle;
import util.DropDownAlert;
import com.wikitude.tracker.ImageTarget;
import com.wikitude.tracker.ImageTracker;
import com.wikitude.tracker.ImageTrackerListener;
import com.wikitude.tracker.TargetCollectionResource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

public class CameraSettingsActivity extends Activity implements ExternalRendering, AdapterView.OnItemSelectedListener, CameraManagerListener, ImageTrackerListener {

    private static final String TAG = "CameraControlsActivity";

    private WikitudeSDK wikitudeSDK;
    private CameraManager cameraManager;
    private GLRenderer glRenderer;
    private CustomSurfaceView customSurfaceView;
    private Driver driver;

    private boolean isCameraOpen;

    private TableRow focusRow;
    private boolean camera2Enabled = true;
    private DropDownAlert dropDownAlert;
    private FrameLayout viewHolder;
    private Spinner cameraPositionSpinner;
    private Spinner focusModeSpinner;
    private Switch flashToggleButton;
    private SeekBar zoomSeekBar;
    private ImageView focusExposureIndicator;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        camera2Enabled = getIntent().getBooleanExtra("enableCamera2", true);

        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);
        startupConfiguration.setCameraResolution(CameraSettings.CameraResolution.AUTO);

        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        cameraManager = wikitudeSDK.getCameraManager();
        cameraManager.setListener(this);

        final TargetCollectionResource targetCollectionResource = wikitudeSDK.getTrackerManager().createTargetCollectionResource("file:///android_asset/magazine.wtc");
        wikitudeSDK.getTrackerManager().createImageTracker(targetCollectionResource, CameraSettingsActivity.this, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wikitudeSDK.onResume();
        customSurfaceView.onResume();
        driver.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        customSurfaceView.onPause();
        driver.stop();
        wikitudeSDK.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wikitudeSDK.onDestroy();
    }


    @Override
    public void onRenderExtensionCreated(final RenderExtension renderExtension) {
        glRenderer = new GLRenderer(renderExtension);
        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(glRenderer);
        customSurfaceView = new CustomSurfaceView(getApplicationContext(), glRenderer);
        driver = new Driver(customSurfaceView, 30);

        viewHolder = new FrameLayout(getApplicationContext());
        setContentView(viewHolder);

        viewHolder.addView(customSurfaceView);

        focusExposureIndicator = new ImageView(this);
        Drawable drawable = getResources().getDrawable(R.drawable.baseline_crop_free_white_24);
        drawable.setColorFilter(getResources().getColor(R.color.wikitude_primary), PorterDuff.Mode.MULTIPLY);
        focusExposureIndicator.setBackground(drawable);
        focusExposureIndicator.setVisibility(View.GONE);
        focusExposureIndicator.setLayoutParams(new FrameLayout.LayoutParams(dpToPx(50), dpToPx(50)));
        viewHolder.addView(focusExposureIndicator);

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        final FrameLayout controls = (FrameLayout) inflater.inflate(R.layout.activity_camera_control, null);
        viewHolder.addView(controls);

        cameraPositionSpinner = findViewById(R.id.cameraPosition);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(CameraSettingsActivity.this, R.array.camera_positions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cameraPositionSpinner.setAdapter(adapter);
        cameraPositionSpinner.setOnItemSelectedListener(CameraSettingsActivity.this);
        cameraPositionSpinner.setEnabled(false);

        focusModeSpinner = findViewById(R.id.focusMode);
        adapter = ArrayAdapter.createFromResource(CameraSettingsActivity.this, R.array.focus_mode, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        focusModeSpinner.setAdapter(adapter);
        focusModeSpinner.setOnItemSelectedListener(CameraSettingsActivity.this);
        focusModeSpinner.setEnabled(false);

        flashToggleButton = findViewById(R.id.flashlight);
        flashToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                if (isChecked) {
                    wikitudeSDK.getCameraManager().enableCameraFlashLight();
                } else {
                    wikitudeSDK.getCameraManager().disableCameraFlashLight();
                }

            }
        });
        flashToggleButton.setEnabled(false);

        zoomSeekBar = findViewById(R.id.zoomSeekBar);
        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                if (progress > 0) {
                    wikitudeSDK.getCameraManager().setZoomLevel((float) (progress + 100) / 100.0f);
                }
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {

            }
        });
        zoomSeekBar.setEnabled(false);

        focusRow = findViewById(R.id.tableRow4);

        SeekBar focusSeekBar = findViewById(R.id.focusSeekBar);
        focusSeekBar.setMax(100);
        focusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                cameraManager.setManualFocusDistance((float)progress/100.0f);
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
            }
        });

        final TableLayout tableLayout = findViewById(R.id.tableLayout1);
        tableLayout.post(new Runnable() {
            @Override
            public void run() {
                dropDownAlert = new DropDownAlert(CameraSettingsActivity.this);
                dropDownAlert.setText("Scan Target #1 (surfer) or #2 (biker):");
                dropDownAlert.addImages("surfer.png", "bike.png");
                dropDownAlert.setTextWeight(0.5f);
                dropDownAlert.setMarginTop(tableLayout.getHeight());
                dropDownAlert.show(viewHolder);
                controls.bringToFront();
            }
        });
    }

    @Override
    public void onItemSelected(final AdapterView<?> adapterView, final View view, final int position, final long id) {
        switch (adapterView.getId()) {
            case R.id.focusMode:
                switch (position){
                    case 0:
                        wikitudeSDK.getCameraManager().setFocusMode(CameraSettings.CameraFocusMode.CONTINUOUS);
                        if (focusRow != null) {
                            focusRow.setVisibility(View.GONE);
                        }
                        customSurfaceView.setOnTouchListener(null);
                        focusExposureIndicator.setVisibility(View.GONE);
                        break;
                    case 1:
                        wikitudeSDK.getCameraManager().setFocusMode(CameraSettings.CameraFocusMode.ONCE);
                        focusExposureIndicator.setVisibility(View.VISIBLE);
                        Point windowSize = new Point();
                        getWindowManager().getDefaultDisplay().getSize(windowSize);
                        focusExposureIndicator.setTranslationX(windowSize.x / 2 - dpToPx(25));
                        focusExposureIndicator.setTranslationY(windowSize.y / 2 - dpToPx(25));

                        customSurfaceView.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(final View v, final MotionEvent event) {
                                cameraManager.setFocusPointOfInterest(event.getX(), event.getY());
                                cameraManager.setExposurePointOfInterest(event.getX(), event.getY());
                                focusExposureIndicator.setTranslationX(event.getX() - dpToPx(25));
                                focusExposureIndicator.setTranslationY(event.getY() - dpToPx(25));
                                return false;
                            }
                        });
                        if (focusRow != null) {
                            focusRow.setVisibility(View.GONE);
                        }
                        break;
                    case 2:
                        wikitudeSDK.getCameraManager().setFocusMode(CameraSettings.CameraFocusMode.OFF);
                        if (wikitudeSDK.getCameraManager().isManualFocusAvailable()) {
                            if (focusRow != null) {
                                focusRow.setVisibility(View.VISIBLE);
                            }
                        } else if (!camera2Enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                            Toast.makeText(this, "Manual Focus is not supported by the old camera API. The focus will be fixed at infinity focus if the device supports it.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Manual Focus is not supported by this device. The focus will be fixed at infinity focus.", Toast.LENGTH_SHORT).show();
                        }
                        customSurfaceView.setOnTouchListener(null);
                        focusExposureIndicator.setVisibility(View.GONE);
                        break;
                }
                break;
            case R.id.cameraPosition:
                if (position == 0) {
                    wikitudeSDK.getCameraManager().setCameraPosition(CameraSettings.CameraPosition.BACK);
                } else {
                    wikitudeSDK.getCameraManager().setCameraPosition(CameraSettings.CameraPosition.FRONT);
                }
                break;
        }
    }

    @Override
    public void onNothingSelected(final AdapterView<?> adapterView) {

    }

    @Override
    public void onCameraOpen() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isCameraOpen) {
                    cameraPositionSpinner.setEnabled(true);
                    focusModeSpinner.setEnabled(true);
                    flashToggleButton.setEnabled(true);
                    float maxZoomLevel = wikitudeSDK.getCameraManager().getMaxZoomLevel();
                    if (maxZoomLevel > 1) {
                        zoomSeekBar.setEnabled(true);
                        zoomSeekBar.setMax((int) maxZoomLevel * 100 - 100);
                    }
                }
                isCameraOpen = true;
            }
        });
    }

    @Override
    public void onCameraReleased() {
    }

    @Override
    public void onError(WikitudeError error) {
        Log.e("Camera", error.getFormattedDescription());
        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        if (camera2Enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            AlertDialog alertDialog = new AlertDialog.Builder(CameraSettingsActivity.this).create();
            alertDialog.setTitle("Camera2 issue.");
            alertDialog.setMessage("There was an unexpected issue with this devices camera2. Should this activity be recreated with the old camera api?");
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(CameraSettingsActivity.this, CameraSettingsActivity.class);
                    intent.putExtra("enableCamera2", false);
                    finish();
                    startActivity(intent);
                }
            });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recreate();
                        }
                    });
                }
            });
            alertDialog.show();
        } else {
            Toast.makeText(this, "Camera could not be started.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onTargetsLoaded(ImageTracker tracker) {
        Log.v(TAG, "Image tracker loaded");
    }

    @Override
    public void onErrorLoadingTargets(ImageTracker tracker, WikitudeError error) {
        Log.v(TAG, "Unable to load image tracker. Reason: " + error.getMessage());
    }

    @Override
    public void onImageRecognized(ImageTracker tracker, final ImageTarget target) {
        Log.v(TAG, "Recognized target " + target.getName());
        dropDownAlert.dismiss();

        StrokedRectangle strokedRectangle = new StrokedRectangle(StrokedRectangle.Type.STANDARD);
        glRenderer.setRenderablesForKey(target.getName() + target.getUniqueId(), strokedRectangle, null);
    }

    @Override
    public void onImageTracked(ImageTracker tracker, final ImageTarget target) {
        StrokedRectangle strokedRectangle = (StrokedRectangle) glRenderer.getRenderableForKey(target.getName() + target.getUniqueId());

        if (strokedRectangle != null) {
            strokedRectangle.viewMatrix = target.getViewMatrix();

            strokedRectangle.setXScale(target.getTargetScale().x);
            strokedRectangle.setYScale(target.getTargetScale().y);
        }
    }

    @Override
    public void onImageLost(ImageTracker tracker, final ImageTarget target) {
        Log.v(TAG, "Lost target " + target.getName());
        glRenderer.removeRenderablesForKey(target.getName() + target.getUniqueId());
    }

    @Override
    public void onExtendedTrackingQualityChanged(ImageTracker tracker, final ImageTarget target, final int oldTrackingQuality, final int newTrackingQuality) {

    }

    private int dpToPx(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
