package rendering.internal;

import com.wikitude.camera.CameraManager;
import com.wikitude.common.rendering.RenderExtension;
import rendering.external.StrokedRectangle;
import com.wikitude.tracker.Target;

import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CustomRenderExtension implements GLSurfaceView.Renderer, RenderExtension, CameraManager.FovChangedListener {

    private final float[] projectionMatrix = new float[16];

    private Target currentlyRecognizedTarget;
    private StrokedRectangle strokedRectangle;
    private int width;
    private int height;
    private float fieldOfView;

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        strokedRectangle = new StrokedRectangle();
        strokedRectangle.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
        this.width = width;
        this.height = height;
        if (fieldOfView > 0) {
            recalculateFov();
        }
    }

    @Override
    public void onDrawFrame(final GL10 unused) {
        if (currentlyRecognizedTarget != null) {
            strokedRectangle.viewMatrix = currentlyRecognizedTarget.getViewMatrix();
            strokedRectangle.onDrawFrame();
        }
    }

    public void onPause() {
    }

    public void onResume() {
    }

    @Override
    public void useSeparatedRenderAndLogicUpdates() {

    }

    public void onUpdate() {

    }

    public void setCurrentlyRecognizedTarget(final Target currentlyRecognizedTarget) {
        this.currentlyRecognizedTarget = currentlyRecognizedTarget;
    }

    @Override
    public void onFovChanged(final float fieldOfView) {
        this.fieldOfView = fieldOfView;
        if (width != 0 && height != 0) {
            recalculateFov();
        }
    }

    private void recalculateFov() {
        Matrix.perspectiveM(projectionMatrix, 0, fieldOfView, (float)width/(float)height, 0.05f, 5000f);
        strokedRectangle.projectionMatrix = projectionMatrix;
    }

    public StrokedRectangle getStrokedRectangle() {
        return strokedRectangle;
    }
}
