/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.instrument.cardboard;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.ScreenParams;
import com.google.vrtoolkit.cardboard.Viewport;
import com.instrument.stardome.StarDomeData;
import com.instrument.stardome.StarDomeData0;
import com.instrument.stardome.StarDomeData1;
import com.instrument.stardome.StarDomeData2;
import com.instrument.stardome.StarDomeData3;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * A Cardboard sample application.
 */
public class PearCardboardActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    private static final String TAG = "PearCardboardActivity";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 6000.0f;

    private float CAMERA_Z = 0.01f;
    private static final float TIME_DELTA = 0.3f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;

    private static final int COORDS_PER_VERTEX = 3;

    private static final WorldLayoutData DATA = new WorldLayoutData();

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] { 0.0f, 2.0f, 0.0f, 1.0f };

    private final float[] mLightPosInEyeSpace = new float[4];

    private FloatBuffer mFloorVertices;
    private FloatBuffer mFloorColors;
    private FloatBuffer mFloorNormals;

    private FloatBuffer mCubeVertices;
    private FloatBuffer mCubeColors;
    private FloatBuffer mCubeFoundColors;
    private FloatBuffer mCubeNormals;

    private FloatBuffer starDomeVertexBuffers[] = new FloatBuffer[5];
    private int starDomeVertexCounts[] = new int[5];

    private int mCubeProgram;
    private int mFloorProgram;

    private int mCubePositionParam;
    private int mCubeNormalParam;
    private int mCubeColorParam;
    private int mCubeModelParam;
    private int mCubeModelViewParam;
    private int mCubeModelViewProjectionParam;
    private int mCubeLightPosParam;

    private int mFloorPositionParam;
    private int mFloorNormalParam;
    private int mFloorColorParam;
    private int mFloorModelParam;
    private int mFloorModelViewParam;
    private int mFloorModelViewProjectionParam;
    private int mFloorLightPosParam;

    private float[] mModelCube;
    private float[] mCamera;
    private float[] mView;
    private float[] mHeadView;
    private float[] mModelViewProjection;
    private float[] mModelView;
    private float[] mModelFloor;

    private int mScore = 0;
    private float mObjectDistance = 12f;
    private float mFloorDepth = 20f;

    private Vibrator mVibrator;
    private CardboardOverlayView mOverlayView;



    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     */
    private static void checkGLError(String label) {

        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    /**
     * Sets the view to our CardboardView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);
        ScreenParams screenParams = cardboardView.getScreenParams();
        Log.d("PearCardboardActivity","opened Cardboard view width=" + screenParams.getWidth() + " height=" + screenParams.getHeight());

        mModelCube = new float[16];
        mCamera = new float[16];
        mView = new float[16];
        mModelViewProjection = new float[16];
        mModelView = new float[16];
        mModelFloor = new float[16];
        mHeadView = new float[16];
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mOverlayView = (CardboardOverlayView) findViewById(R.id.overlay);

    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    protected class StarDomeProgram {
        public int programId;
        // VERTEX
        public int uPMatrix;
        public int uMVMatrix;
        public int aVertexPosition;
    }

    protected static final int STARDOME_MAGNITUDE_COUNT = 4;

    private StarDomeProgram[] starMapPrograms = new StarDomeProgram[STARDOME_MAGNITUDE_COUNT];

    /**
     * Creates the buffers we use to store information about the 3D world.
     *
     * OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
     * Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.
    }
//        ByteBuffer bbVertices = ByteBuffer.allocateDirect(DATA.CUBE_COORDS.length * 4);
//        bbVertices.order(ByteOrder.nativeOrder());
//        mCubeVertices = bbVertices.asFloatBuffer();
//        mCubeVertices.put(DATA.CUBE_COORDS);
//        mCubeVertices.position(0);
//
//        ByteBuffer bbColors = ByteBuffer.allocateDirect(DATA.CUBE_COLORS.length * 4);
//        bbColors.order(ByteOrder.nativeOrder());
//        mCubeColors = bbColors.asFloatBuffer();
//        mCubeColors.put(DATA.CUBE_COLORS);
//        mCubeColors.position(0);
//
//        ByteBuffer bbFoundColors = ByteBuffer.allocateDirect(DATA.CUBE_FOUND_COLORS.length * 4);
//        bbFoundColors.order(ByteOrder.nativeOrder());
//        mCubeFoundColors = bbFoundColors.asFloatBuffer();
//        mCubeFoundColors.put(DATA.CUBE_FOUND_COLORS);
//        mCubeFoundColors.position(0);
//
//        ByteBuffer bbNormals = ByteBuffer.allocateDirect(DATA.CUBE_NORMALS.length * 4);
//        bbNormals.order(ByteOrder.nativeOrder());
//        mCubeNormals = bbNormals.asFloatBuffer();
//        mCubeNormals.put(DATA.CUBE_NORMALS);
//        mCubeNormals.position(0);
//
//        // make a floor
//        ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(DATA.FLOOR_COORDS.length * 4);
//        bbFloorVertices.order(ByteOrder.nativeOrder());
//        mFloorVertices = bbFloorVertices.asFloatBuffer();
//        mFloorVertices.put(DATA.FLOOR_COORDS);
//        mFloorVertices.position(0);
//
//        ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(DATA.FLOOR_NORMALS.length * 4);
//        bbFloorNormals.order(ByteOrder.nativeOrder());
//        mFloorNormals = bbFloorNormals.asFloatBuffer();
//        mFloorNormals.put(DATA.FLOOR_NORMALS);
//        mFloorNormals.position(0);
//
//        ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(DATA.FLOOR_COLORS.length * 4);
//        bbFloorColors.order(ByteOrder.nativeOrder());
//        mFloorColors = bbFloorColors.asFloatBuffer();
//        mFloorColors.put(DATA.FLOOR_COLORS);
//        mFloorColors.position(0);
//
//        // create star magnitude programs
//        for(int i=0;i < STARDOME_MAGNITUDE_COUNT;i++) {
//            starMapPrograms[i] = new StarDomeProgram();
//            starMapPrograms[i].programId = GLES20.glCreateProgram();
//            int starmapVShader = ShaderLoader.loadShaderAsset(GLES20.GL_VERTEX_SHADER, "starmap_v" + Integer.toString(i));
//
//            int starmapFragShader = ShaderLoader.loadShaderAsset(GLES20.GL_FRAGMENT_SHADER, "starmap_f" + Integer.toString(i));
//            GLES20.glAttachShader(starMapPrograms[i].programId, starmapVShader);
//            GLES20.glAttachShader(starMapPrograms[i].programId, starmapFragShader);
//            GLES20.glLinkProgram(starMapPrograms[i].programId);
//            GLES20.glUseProgram(starMapPrograms[i].programId);
//            starMapPrograms[i].aVertexPosition = GLES20.glGetAttribLocation(starMapPrograms[i].programId, "aVertexPosition");
//            GLES20.glEnableVertexAttribArray(starMapPrograms[i].aVertexPosition);
//            starMapPrograms[i].uPMatrix = GLES20.glGetUniformLocation(starMapPrograms[i].programId, "uPMatrix");
//            starMapPrograms[i].uMVMatrix = GLES20.glGetUniformLocation(starMapPrograms[i].programId, "uMVMatrix");
//
//            Log.d(TAG, "loaded starmap program " + i);
//            // load stars
//            // pass one how many mag i stars?
//            StarDomeData starDomeData = null;
//            switch(i) {
//                case 0:
//                    starDomeData = new StarDomeData0();
//                    break;
//                case 1:
//                    starDomeData = new StarDomeData1();
//                    break;
//                case 2:
//                    starDomeData = new StarDomeData2();
//                    break;
//                case 3:
//                    starDomeData = new StarDomeData3();
//                    break;
//            }
//
//            int c = 0;
//            int mi;
//            float[] starVertices = starDomeData.getVertexData();
//            for(int xi=0; xi < starVertices.length; xi+=4) {
//                float mag = starVertices[xi+3];
//                if(mag < 1) {
//                    mi = 0;
//                } else if (mag < 2) {
//                    mi = 1;
//                } else if (mag < 4) {
//                    mi = 2;
//                } else if (mag < 7) {
//                    mi = 3;
//                } else {
//                    mi = 4;
//                }
//                if(mi == i) {
//                    c++;
//                }
//            }
//            // mi is 0..4 (for magnitude) and c is the number of stars of that magnitude.
//            ByteBuffer starDomeVBB = ByteBuffer.allocateDirect(c * 4 * 3); // 4 bytes x 3 floats per star.
//            starDomeVBB.order(ByteOrder.nativeOrder());
//            FloatBuffer starDomeVFB = starDomeVBB.asFloatBuffer();
//
//            c = 0;
//            for(int xi=0;xi < starVertices.length;xi+=4) {
//                float mag = starVertices[xi+3];
//                if(mag < 1) {
//                    mi = 0;
//                } else if (mag < 2) {
//                    mi = 1;
//                } else if (mag < 4) {
//                    mi = 2;
//                } else if (mag < 7) {
//                    mi = 3;
//                } else {
//                    mi = 4;
//                }
//                if((mi == i) && (i < + STARDOME_MAGNITUDE_COUNT)) {
//                    float x = starVertices[xi];
//                    float y = starVertices[xi + 1];
//                    float z = starVertices[xi + 2];
//                    starDomeVFB.put(x);
//                    starDomeVFB.put(y);
//                    starDomeVFB.put(z);
//                    c++;
//                }
//            }
//            Log.d(TAG, "added " + c + " stars of magnitude " + i);
//            starDomeVFB.position(0);
//            starDomeVertexBuffers[i] = starDomeVFB;
//            starDomeVertexCounts[i] = c;
//        }

        int vertexShader = ShaderLoader.loadGLShader(getResources(), GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
//        int gridShader = ShaderLoader.loadGLShader(getResources(), GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
//        int passthroughShader = ShaderLoader.loadGLShader(getResources(), GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        // load star arrays


//        mCubeProgram = GLES20.glCreateProgram();
//        GLES20.glAttachShader(mCubeProgram, vertexShader);
//        GLES20.glAttachShader(mCubeProgram, passthroughShader);
//        GLES20.glLinkProgram(mCubeProgram);
//        GLES20.glUseProgram(mCubeProgram);
//
//        checkGLError("Cube program");
//
//        mCubePositionParam = GLES20.glGetAttribLocation(mCubeProgram, "a_Position");
//        mCubeNormalParam = GLES20.glGetAttribLocation(mCubeProgram, "a_Normal");
//        mCubeColorParam = GLES20.glGetAttribLocation(mCubeProgram, "a_Color");
//
//        mCubeModelParam = GLES20.glGetUniformLocation(mCubeProgram, "u_Model");
//        mCubeModelViewParam = GLES20.glGetUniformLocation(mCubeProgram, "u_MVMatrix");
//        mCubeModelViewProjectionParam = GLES20.glGetUniformLocation(mCubeProgram, "u_MVP");
//        mCubeLightPosParam = GLES20.glGetUniformLocation(mCubeProgram, "u_LightPos");
//
//        GLES20.glEnableVertexAttribArray(mCubePositionParam);
//        GLES20.glEnableVertexAttribArray(mCubeNormalParam);
//        GLES20.glEnableVertexAttribArray(mCubeColorParam);
//
//        checkGLError("Cube program params");

//        mFloorProgram = GLES20.glCreateProgram();
//        GLES20.glAttachShader(mFloorProgram, vertexShader);
//        GLES20.glAttachShader(mFloorProgram, gridShader);
//        GLES20.glLinkProgram(mFloorProgram);
//        GLES20.glUseProgram(mFloorProgram);
//
//        checkGLError("Floor program");
//
//        mFloorModelParam = GLES20.glGetUniformLocation(mFloorProgram, "u_Model");
//        mFloorModelViewParam = GLES20.glGetUniformLocation(mFloorProgram, "u_MVMatrix");
//        mFloorModelViewProjectionParam = GLES20.glGetUniformLocation(mFloorProgram, "u_MVP");
//        mFloorLightPosParam = GLES20.glGetUniformLocation(mFloorProgram, "u_LightPos");
//
//        mFloorPositionParam = GLES20.glGetAttribLocation(mFloorProgram, "a_Position");
//        mFloorNormalParam = GLES20.glGetAttribLocation(mFloorProgram, "a_Normal");
//        mFloorColorParam = GLES20.glGetAttribLocation(mFloorProgram, "a_Color");
//
//        GLES20.glEnableVertexAttribArray(mFloorPositionParam);
//        GLES20.glEnableVertexAttribArray(mFloorNormalParam);
//        GLES20.glEnableVertexAttribArray(mFloorColorParam);
//
//        checkGLError("Floor program params");
//
//        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//
//        // Object first appears directly in front of user.
//        Matrix.setIdentityM(mModelCube, 0);
//        Matrix.translateM(mModelCube, 0, 0, 0, -mObjectDistance);
//
//        Matrix.setIdentityM(mModelFloor, 0);
//        Matrix.translateM(mModelFloor, 0, 0, -mFloorDepth, 0); // Floor appears below user.
//
//        checkGLError("onSurfaceCreated");
//    }


    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the Model part of the ModelView matrix.
//        Matrix.rotateM(mModelCube, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);

        // Build the camera matrix and apply it to the ModelView.
        //CAMERA_Z -= 1;
//        Matrix.setLookAtM(mCamera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

//        headTransform.getHeadView(mHeadView, 0);

//        checkGLError("onReadyToDraw");
    }

    /**
     * Draws a frame for an eye.
     *
     * @param eye The eye to render. Includes all required transformations.
     */
    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGLError("mColorParam");

        // Apply the eye transformation to the camera.
  //      Matrix.multiplyMM(mView, 0, eye.getEyeView(), 0, mCamera, 0);

        // Set the position of the light
//        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mView, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
//        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
//        Matrix.multiplyMM(mModelView, 0, mView, 0, mModelCube, 0);
//        Matrix.multiplyMM(mModelViewProjection, 0, perspective, 0, mModelView, 0);
        //drawCube();

        // Set mModelView for the floor, so we draw floor in the correct location
//        Matrix.translateM(mModelFloor, 0, 0, -1, 0);
  //      Matrix.multiplyMM(mModelView, 0, mView, 0, mModelFloor, 0);
//        Matrix.multiplyMM(mModelViewProjection, 0, perspective, 0, mModelView, 0);
//        drawFloor();
//        drawStarDome();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    /**
     * Draw the cube.
     *
     * We've set all of our transformation matrices. Now we simply pass them into the shader.
     */
    public void drawCube() {
        GLES20.glUseProgram(mCubeProgram);

        GLES20.glUniform3fv(mCubeLightPosParam, 1, mLightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(mCubeModelParam, 1, false, mModelCube, 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(mCubeModelViewParam, 1, false, mModelView, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(mCubePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, mCubeVertices);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(mCubeModelViewProjectionParam, 1, false, mModelViewProjection, 0);

        // Set the normal positions of the cube, again for shading
        GLES20.glVertexAttribPointer(mCubeNormalParam, 3, GLES20.GL_FLOAT, false, 0, mCubeNormals);
        GLES20.glVertexAttribPointer(mCubeColorParam, 4, GLES20.GL_FLOAT, false, 0,
                isLookingAtObject() ? mCubeFoundColors : mCubeColors);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        checkGLError("Drawing cube");
    }

    /**
     * Draw the floor.
     *
     * This feeds in data for the floor into the shader. Note that this doesn't feed in data about
     * position of the light, so if we rewrite our code to draw the floor first, the lighting might
     * look strange.
     */
    public void drawFloor() {
        GLES20.glUseProgram(mFloorProgram);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(mFloorLightPosParam, 1, mLightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(mFloorModelParam, 1, false, mModelFloor, 0);
        GLES20.glUniformMatrix4fv(mFloorModelViewParam, 1, false, mModelView, 0);
        GLES20.glUniformMatrix4fv(mFloorModelViewProjectionParam, 1, false,
                mModelViewProjection, 0);
        GLES20.glVertexAttribPointer(mFloorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, mFloorVertices);
        GLES20.glVertexAttribPointer(mFloorNormalParam, 3, GLES20.GL_FLOAT, false, 0,
                mFloorNormals);
        GLES20.glVertexAttribPointer(mFloorColorParam, 4, GLES20.GL_FLOAT, false, 0, mFloorColors);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        checkGLError("drawing floor");
    }

    protected void drawStarDome() {
        for(int i=0;i < STARDOME_MAGNITUDE_COUNT;i++) {
            // use program for star magnitude i
            GLES20.glUseProgram(starMapPrograms[i].programId);
            GLES20.glUniformMatrix4fv(starMapPrograms[i].uPMatrix, 1, false, mModelViewProjection, 0);
            GLES20.glUniformMatrix4fv(starMapPrograms[i].uMVMatrix, 1, false, mModelFloor, 0);
            GLES20.glVertexAttribPointer(starMapPrograms[i].aVertexPosition, 3, GLES20.GL_FLOAT, false, starDomeVertexCounts[i], starDomeVertexBuffers[i]);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, starDomeVertexCounts[i]);
            if(GLES20.glGetError() != GLES20.GL_NO_ERROR) {
                int resultSize = 0;
                String logInfo = GLES20.glGetProgramInfoLog(starMapPrograms[i].programId);
                Log.e(TAG, logInfo);
            }
        }

        checkGLError("drawing starDome");

    }

    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");

//        if (isLookingAtObject()) {
//            mScore++;
//            mOverlayView.show3DToast("Found it! Look around for another one.\nScore = " + mScore);
//            hideObject();
//        } else {
//            mOverlayView.show3DToast("Look around to find the object!");
//        }

        // Always give user feedback.
        mVibrator.vibrate(50);
    }

    /**
     * Find a new random position for the object.
     *
     * We'll rotate it around the Y-axis so it's out of sight, and then up or down by a little bit.
     */
    private void hideObject() {
        float[] rotationMatrix = new float[16];
        float[] posVec = new float[4];

        // First rotate in XZ plane, between 90 and 270 deg away, and scale so that we vary
        // the object's distance from the user.
        float angleXZ = (float) Math.random() * 180 + 90;
        Matrix.setRotateM(rotationMatrix, 0, angleXZ, 0f, 1f, 0f);
        float oldObjectDistance = mObjectDistance;
        mObjectDistance = (float) Math.random() * 15 + 5;
        float objectScalingFactor = mObjectDistance / oldObjectDistance;
        Matrix.scaleM(rotationMatrix, 0, objectScalingFactor, objectScalingFactor,
                objectScalingFactor);
        Matrix.multiplyMV(posVec, 0, rotationMatrix, 0, mModelCube, 12);

        // Now get the up or down angle, between -20 and 20 degrees.
        float angleY = (float) Math.random() * 80 - 40; // Angle in Y plane, between -40 and 40.
        angleY = (float) Math.toRadians(angleY);
        float newY = (float) Math.tan(angleY) * mObjectDistance;

        Matrix.setIdentityM(mModelCube, 0);
        Matrix.translateM(mModelCube, 0, posVec[0], newY, posVec[2]);
    }

    /**
     * Check if user is looking at object by calculating where the object is in eye-space.
     *
     * @return true if the user is looking at the object.
     */
    private boolean isLookingAtObject() {
        float[] initVec = { 0, 0, 0, 1.0f };
        float[] objPositionVec = new float[4];

        // Convert object space to camera space. Use the headView from onNewFrame.
        Matrix.multiplyMM(mModelView, 0, mHeadView, 0, mModelCube, 0);
        Matrix.multiplyMV(objPositionVec, 0, mModelView, 0, initVec, 0);

        float pitch = (float) Math.atan2(objPositionVec[1], -objPositionVec[2]);
        float yaw = (float) Math.atan2(objPositionVec[0], -objPositionVec[2]);

        return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
    }
}
