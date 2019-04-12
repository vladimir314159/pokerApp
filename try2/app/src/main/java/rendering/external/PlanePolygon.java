package rendering.external;

import util.Vector3f;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class PlanePolygon extends Renderable {

    private final static String TAG = "PlanePolygon";

    private static final String FRAGMENT_SHADER_CODE =
        "precision mediump float;"+

        "uniform vec4 color;"+

        "varying vec4 f_color;"+
        "varying vec2 f_uv;"+

        "void main()"+
        "{"+
        "const float     sin30 = 0.5;"+
        "const float     sin60 = 0.866;"+
        "const float     cos30 = sin60;"+
        "const float     cos60 = sin30;"+
        "const float     pi = 3.14;"+
        "const mat2      rotation30 = mat2(vec2(cos30, -sin30), vec2(sin30, cos30));"+
        "const mat2      rotation60 = mat2(vec2(cos60, -sin60), vec2(sin60, cos60));"+
        "const float     thicknessFactor = 0.025;"+
        "const float     baseAlpha = 0.4;"+

            "vec4 outputColor = color;"+

            "vec2 uv = 2.0 * pi * f_uv * 10.0;"+
            "vec2 uvRotated30 = uv * rotation30;"+
            "vec2 uvRotated60 = uv * rotation60;"+

            "vec2 alphaFactorRotated30 = smoothstep(0.0, thicknessFactor, (0.5 + 0.5 * cos(uvRotated30)));"+
            "vec2 alphaFactorRotated60 = smoothstep(0.0, thicknessFactor, (0.5 + 0.5 * cos(uvRotated60)));"+

            "vec2 alphaFactor = smoothstep(0.0, sin30 * thicknessFactor, (0.5 + 0.5 * cos(sin30 * uv)));"+
            "vec2 alphaFactorPanned = smoothstep(0.0, sin30 * thicknessFactor, (0.5 + 0.5 * cos(sin30 * uv + pi)));"+

            "float combinedAlphaFactor = alphaFactorRotated30.x * alphaFactorRotated60.y * alphaFactor.y * alphaFactorPanned.y;"+
            "outputColor.a *= clamp(1.0 - combinedAlphaFactor + baseAlpha, 0.0, 1.0);" +
            "outputColor.a *= f_color.a;"+

            "gl_FragColor = outputColor;"+
        "}";


    private static final String VERTEX_SHADER_CODE =
        "attribute vec4 v_position;"+
        "attribute vec4 v_color;"+
        "attribute vec2 v_uv;"+

        "uniform mat4 projection;"+
        "uniform mat4 modelview;"+

        "varying vec4 f_color;"+
        "varying vec2 f_uv;"+

        "void main()"+
        "{"+
            "gl_Position = projection * modelview * v_position;"+
            "f_color = v_color;"+
            "f_uv = v_uv;"+
        "}";

    private static final int FLOAT_BYTE_SIZE = 4;
    private static final int SHORT_BYTE_SIZE = 2;

    private int augmentationProgram = -1;

    private FloatBuffer vertices = null;
    private FloatBuffer vertexColors = null;
    private FloatBuffer texCoords = null;
    private ShortBuffer indices = null;

    private int positionSlot = -1;
    private int colorSlot = -1;
    private int uvSlot = -1;

    private int projectionUniform = -1;
    private int modelViewUniform = -1;

    private int colorUniform = -1;

    private float red = 1.0f;
    private float green = 0.58f;
    private float blue = 0.16f;

    private FloatBuffer points = null;

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public static void checkGlError(String glOperation) {
        int error;
        if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    @Override
    public void onSurfaceCreated() {
        compileShaders();
    }

    @Override
    public void onDrawFrame() {
        if (augmentationProgram == -1) {
            compileShaders();
        }

        GLES20.glUseProgram(augmentationProgram);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        GLES20.glVertexAttribPointer(positionSlot, 3, GLES20.GL_FLOAT, false, 0, vertices);
        GLES20.glEnableVertexAttribArray(positionSlot);

        GLES20.glVertexAttribPointer(colorSlot, 4, GLES20.GL_FLOAT, false, 0, vertexColors);
        GLES20.glEnableVertexAttribArray(colorSlot);

        GLES20.glVertexAttribPointer(uvSlot, 2, GLES20.GL_FLOAT, false, 0, texCoords);
        GLES20.glEnableVertexAttribArray(uvSlot);

        GLES20.glUniformMatrix4fv(projectionUniform, 1, false, this.projectionMatrix, 0);
        GLES20.glUniformMatrix4fv(modelViewUniform, 1, false, this.viewMatrix, 0);

        GLES20.glUniform4f(colorUniform, red, green, blue, 1.0f);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc (GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.remaining(), GLES20.GL_UNSIGNED_SHORT, indices);
    }

    public void setColor(float r, float g, float b) {
        red = r;
        green = g;
        blue = b;
    }

    public void setPoints(FloatBuffer points) {
        this.points = points;
        setupVertices();
    }

    private void setupVertices() {
        float fadeDistance = 0.2f;
        FloatBuffer inset = computeInset(points, fadeDistance);

        updateMeshData(points, inset);
    }

    private void updateMeshData(FloatBuffer insidePolygon, FloatBuffer outsidePolygon) {
        int numberOfVertices = insidePolygon.remaining() / 2;

        vertices = ByteBuffer.allocateDirect(numberOfVertices * 3 * 2 * FLOAT_BYTE_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexColors = ByteBuffer.allocateDirect(numberOfVertices * 4 * 2 * FLOAT_BYTE_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoords = ByteBuffer.allocateDirect(numberOfVertices * 2 * 2 * FLOAT_BYTE_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();

        while (insidePolygon.hasRemaining()) {
            float x = insidePolygon.get();
            float y = insidePolygon.get();

            vertices.put(x);
            vertices.put(y);
            vertices.put(0.0f);

            texCoords.put(x);
            texCoords.put(y);

            vertexColors.put(1.0f);
            vertexColors.put(1.0f);
            vertexColors.put(1.0f);
            vertexColors.put(1.0f);
        }
        insidePolygon.position(0);

        while (outsidePolygon.hasRemaining()) {
            float x = outsidePolygon.get();
            float y = outsidePolygon.get();

            vertices.put(x);
            vertices.put(y);
            vertices.put(0.0f);

            texCoords.put(x);
            texCoords.put(y);

            vertexColors.put(1.0f);
            vertexColors.put(1.0f);
            vertexColors.put(1.0f);
            vertexColors.put(0.0f);
        }
        outsidePolygon.position(0);

        vertices.position(0);
        vertexColors.position(0);
        texCoords.position(0);

        int indexCount = 3 * (numberOfVertices - 2) + 3 * numberOfVertices * 2;
        indices = ByteBuffer.allocateDirect(indexCount * SHORT_BYTE_SIZE).order(ByteOrder.nativeOrder()).asShortBuffer();

        for (short i = 2; i < numberOfVertices; ++i) {
            indices.put((short)0);
            indices.put((short)(i - 1));
            indices.put(i);
        }

        for (short i = 0; i < numberOfVertices; ++i)
        {
            indices.put((short)(i + numberOfVertices));
            indices.put((short)((i + 1) % numberOfVertices + numberOfVertices));
            indices.put((i));

            indices.put((i));
            indices.put((short)((i + 1) % numberOfVertices + numberOfVertices));
            indices.put((short)((i + 1) % numberOfVertices));
        }

        indices.position(0);
    }

    private FloatBuffer computeInset(FloatBuffer convexHull, float insetDistance)
    {
        int numberOfVertices = convexHull.remaining() / 2;

        FloatBuffer inset = ByteBuffer.allocateDirect(numberOfVertices * 2 * FLOAT_BYTE_SIZE).order(ByteOrder.nativeOrder()).asFloatBuffer();

        for (int i = 0; i < numberOfVertices; ++i)
        {
            int prevIndex = (numberOfVertices + i - 1) % numberOfVertices;
            Vector3f prevPoint = new Vector3f(convexHull.get(2 * prevIndex), convexHull.get(2 * prevIndex + 1), 0.0f);

            Vector3f currPoint = new Vector3f(convexHull.get(2 * i), convexHull.get(2 * i + 1), 0.0f);
            int nextIndex = (i + 1) % numberOfVertices;
            Vector3f nextPoint = new Vector3f(convexHull.get(2 * nextIndex), convexHull.get(2 * nextIndex + 1), 0.0f);

            Vector3f prevDir = Vector3f.sub(currPoint, prevPoint);
            Vector3f nextDir = Vector3f.sub(nextPoint, currPoint);

            // ignore Java's warning; passing y for x  and x for y is what is desired here
            Vector3f prevPerp = new Vector3f(-prevDir.y, prevDir.x, 0.0f);
            prevPerp = Vector3f.normalized(prevPerp);
            Vector3f nextPerp = new Vector3f(-nextDir.y, nextDir.x, 0.0f);
            nextPerp = Vector3f.normalized(nextPerp);

            Vector3f prevOffsetStart = Vector3f.add(prevPoint, Vector3f.mul(prevPerp, insetDistance));
            Vector3f prevOffsetEnd = Vector3f.add(currPoint, Vector3f.mul(prevPerp, insetDistance));

            Vector3f nextOffsetStart = Vector3f.add(currPoint, Vector3f.mul(nextPerp, insetDistance));
            Vector3f nextOffsetEnd = Vector3f.add(nextPoint, Vector3f.mul(nextPerp, insetDistance));

            Vector3f prevOffsetDir = Vector3f.sub(prevOffsetEnd, prevOffsetStart);
            Vector3f nextOffsetDir = Vector3f.sub(nextOffsetEnd, nextOffsetStart);

            Vector3f intersection;
            float cross = Vector3f.cross(prevOffsetDir, nextOffsetDir).z;
            if (Math.abs(cross) < 0.0000001f) {
                intersection = prevOffsetEnd;
            } else {
                float a = Vector3f.cross(Vector3f.sub(nextOffsetEnd, prevOffsetStart), nextOffsetDir).z / cross;
                intersection = Vector3f.add(prevOffsetStart, Vector3f.mul(prevOffsetDir, a));
            }

            inset.put(intersection.x);
            inset.put(intersection.y);
        }

        inset.position(0);

        return inset;
    }

    private void compileShaders() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);
        augmentationProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(augmentationProgram, vertexShader);
        GLES20.glAttachShader(augmentationProgram, fragmentShader);
        GLES20.glLinkProgram(augmentationProgram);

        positionSlot = GLES20.glGetAttribLocation(augmentationProgram, "v_position");
        colorSlot = GLES20.glGetAttribLocation(augmentationProgram, "v_color");
        uvSlot = GLES20.glGetAttribLocation(augmentationProgram, "v_uv");

        modelViewUniform = GLES20.glGetUniformLocation(augmentationProgram, "modelview");
        projectionUniform = GLES20.glGetUniformLocation(augmentationProgram, "projection");

        colorUniform = GLES20.glGetUniformLocation(augmentationProgram, "color");
    }
}
