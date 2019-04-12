package tracking.image;

import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.rendering.ExternalRendering;
import main_src.WikitudeSDKConstants;
import rendering.external.CustomSurfaceView;
import rendering.external.Driver;
import rendering.external.GLRenderer;
import rendering.external.StrokedRectangle;
import util.DropDownAlert;
import com.wikitude.tracker.ImageTarget;
import com.wikitude.tracker.ImageTracker;
import com.wikitude.tracker.ImageTrackerConfiguration;
import com.wikitude.tracker.ImageTrackerListener;
import com.wikitude.tracker.TargetCollectionResource;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

import Poker.*;


public class MultipleTargetsImageTrackingActivity extends Activity implements ImageTrackerListener, ExternalRendering {

    private static final String TAG = "MultipleTargetsActivity";
    private WikitudeSDK wikitudeSDK;
    private CustomSurfaceView customSurfaceView;
    private Driver driver;
    private GLRenderer glRenderer;
    private Hand myHand;
    private DropDownAlert dropDownAlert;

    private final ImageTarget.OnDistanceBetweenTargetsListener distanceListener = new ImageTarget.OnDistanceBetweenTargetsListener() {
        @Override
        public void onDistanceBetweenTargetsChanged(int distance, ImageTarget firstTarget, ImageTarget secondTarget) {
            float r = 1.0f;
            float g = 0.58f;
            float b = 0.16f;

            if (distance < 300.0f) {
                if (firstTarget.getName().equals(secondTarget.getName())) {
                    r = 0.0f;
                    g = 0.0f;
                    b = 1.0f;
                } else {
                    r = 1.0f;
                    g = 0.0f;
                    b = 0.0f;
                }
            }

            StrokedRectangle firstStrokedRectangle = (StrokedRectangle) glRenderer.getRenderableForKey(firstTarget.getName() + firstTarget.getUniqueId());
            if (firstStrokedRectangle != null) {
                firstStrokedRectangle.setColor(r, g, b);
            }

            StrokedRectangle secondStrokedRectangle = (StrokedRectangle) glRenderer.getRenderableForKey(secondTarget.getName() + secondTarget.getUniqueId());
            if (secondStrokedRectangle != null) {
                secondStrokedRectangle.setColor(r, g, b);
            }
        }
    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(WikitudeSDKConstants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);
        startupConfiguration.setCameraResolution(CameraSettings.CameraResolution.AUTO);

        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        HashMap<String, Integer> physicalTargetImageHeights = new HashMap<>();
        physicalTargetImageHeights.put("pageOne", 252);
        physicalTargetImageHeights.put("pageTwo", 252);

        ImageTrackerConfiguration trackerConfiguration = new ImageTrackerConfiguration();
        trackerConfiguration.setMaximumNumberOfConcurrentlyTrackableTargets(5);
        trackerConfiguration.setDistanceChangedThreshold(10);
        trackerConfiguration.setPhysicalTargetImageHeights(physicalTargetImageHeights);

        final TargetCollectionResource targetCollectionResource = wikitudeSDK.getTrackerManager().createTargetCollectionResource("file:///android_asset/tracker.wtc");
        wikitudeSDK.getTrackerManager().createImageTracker(targetCollectionResource, this, trackerConfiguration);

        dropDownAlert = new DropDownAlert(this);
        dropDownAlert.setText("Scan Targets:");
        dropDownAlert.setTextWeight(0.5f);
        dropDownAlert.addImages("10_D.jpg");
        dropDownAlert.show();
        myHand = new Hand();
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
        setContentView(customSurfaceView);
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
        //Log.v(TAG, "Recognized target " + target.getName() + target.getUniqueId());
        int afterUnderscore = target.getName().indexOf("_")+1;
        myHand.putCard(target.getName().substring(afterUnderscore,afterUnderscore+1),target.getName().substring(0,1));
        //Log.v(TAG,"count: "+myHand.CardNumber());
        //Log.v(TAG, "Current Hand: "+myHand.print());
        if(myHand.CardNumber()==5) {
            myHand.compute();
            Log.v("*","******************************************");
            Log.v(TAG, myHand.display());
            Log.v("*","******************************************");
        }
        target.setOnDistanceBetweenTargetsListener(distanceListener);
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
        String theCard = target.getName();
        //Log.v(TAG, "Lost target " + target.getName() + target.getUniqueId());
        target.setOnDistanceBetweenTargetsListener(null);
        glRenderer.removeRenderablesForKey(target.getName() + target.getUniqueId());
        dropDownAlert.dismiss();
        int afterUnderscore = theCard.indexOf("_")+1;
        //Log.v(TAG,"afterUnderscore:"+afterUnderscore);
        myHand.removeCard(theCard.substring(afterUnderscore,afterUnderscore+1),theCard.substring(0,1));
        //Log.v(TAG,"Removed: "+theCard);
        //Log.v(TAG,"count: "+myHand.CardNumber());

    }

    @Override
    public void onExtendedTrackingQualityChanged(ImageTracker tracker, final ImageTarget target, final int oldTrackingQuality, final int newTrackingQuality) {

    }
}
