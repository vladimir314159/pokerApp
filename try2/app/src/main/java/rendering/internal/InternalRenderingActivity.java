package rendering.internal;

import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.rendering.InternalRendering;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.common.rendering.RenderSettings;
import main_src.WikitudeSDKConstants;
import util.DropDownAlert;
import com.wikitude.tracker.ImageTarget;
import com.wikitude.tracker.ImageTracker;
import com.wikitude.tracker.ImageTrackerListener;
import com.wikitude.tracker.TargetCollectionResource;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class InternalRenderingActivity extends Activity implements InternalRendering, ImageTrackerListener {

    private static final String TAG = "InternalRendering";
    private WikitudeSDK wikitudeSDK;
    private CustomRenderExtension customRenderExtension;

    private DropDownAlert dropDownAlert;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);
        startupConfiguration.setCameraResolution(CameraSettings.CameraResolution.AUTO);
        startupConfiguration.setRenderingAPI(RenderSettings.RenderingAPI.OPENGL_ES_3, RenderSettings.RenderingAPI.OPENGL_ES_2);

        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(customRenderExtension);

        final TargetCollectionResource targetCollectionResource = wikitudeSDK.getTrackerManager().createTargetCollectionResource("file:///android_asset/magazine.wtc");
        wikitudeSDK.getTrackerManager().createImageTracker(targetCollectionResource, InternalRenderingActivity.this, null);

        setContentView(wikitudeSDK.setupWikitudeGLSurfaceView());

        dropDownAlert = new DropDownAlert(this);
        dropDownAlert.setText("Scan Target #1 (surfer) or #2 (biker):");
        dropDownAlert.addImages("surfer.png", "bike.png");
        dropDownAlert.setTextWeight(0.5f);
        dropDownAlert.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wikitudeSDK.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        wikitudeSDK.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wikitudeSDK.onDestroy();
    }

    @Override
    public RenderExtension provideRenderExtension() {
        customRenderExtension = new CustomRenderExtension();
        return customRenderExtension;
    }

    @Override
    public void onRenderingApiInstanceCreated(RenderSettings.RenderingAPI renderingAPI) {
        String renderingAPIName = renderingAPI == RenderSettings.RenderingAPI.OPENGL_ES_3 ?
                "OpenGL ES 3.0" : "OpenGL ES 2.0";
        Log.v(TAG, "Rendering connection was created with rendering API " + renderingAPIName);
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
    }

    @Override
    public void onImageTracked(ImageTracker tracker, final ImageTarget target) {
        customRenderExtension.setCurrentlyRecognizedTarget(target);
        customRenderExtension.getStrokedRectangle().setXScale(target.getTargetScale().x);
        customRenderExtension.getStrokedRectangle().setYScale(target.getTargetScale().y);
    }

    @Override
    public void onImageLost(ImageTracker tracker, final ImageTarget target) {
        Log.v(TAG, "Lost target " + target.getName());
        customRenderExtension.setCurrentlyRecognizedTarget(null);
    }

    @Override
    public void onExtendedTrackingQualityChanged(ImageTracker tracker, final ImageTarget target, final int oldTrackingQuality, final int newTrackingQuality) {

    }
}
