package ru.omniverse.android.stargreeter;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.FloatBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: irix
 * Date: 14.04.2013
 * Time: 18:04
 */
@SuppressWarnings("FieldCanBeLocal")
class TriangleColored {

    private static final String TAG = "StarGreeterTriangleColored";

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer colorBuffer;
    private final FloatBuffer normalBuffer;
    private final int mProgram;
    private final int mPointProgramHandle;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    private int mMVMatrixHandle;

    private int mLightPosHandle;

    private int mNormalHandle;


    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static final int COLORS_PER_VERTEX = 4;
    static final int NORMALS_PER_VERTEX = 3;
    static final float[] triangleCoords = { // in counterclockwise order:
            1.0f, 2.622008459f, 0.0f,   // top
            -2.5f, -0.311004243f, 0.0f,   // bottom left
            2.5f, -0.311004243f, 0.0f    // bottom right
    };
    static final float[] normalCoords = { // in counterclockwise order:
            0, 0, 1,
            0, 0, 1,
            0, 0, 1
    };
    static final float[] colorsArray = { // in counterclockwise order:
            1.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f
    };
    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;

    public TriangleColored(ResourceLoader loader) {
        // initialize vertex byte buffer for shape coordinates
        vertexBuffer = Utils.newFloatBuffer(triangleCoords);
        colorBuffer = Utils.newFloatBuffer(colorsArray);
        normalBuffer = Utils.newFloatBuffer(normalCoords);

        mProgram = Utils.createShaderProgram(loader, R.raw.trianglecolored_vertex, R.raw.trianglecolored_fragment,
                new String[]{"a_Position", "a_Color", "a_Normal"});

        // Define a simple shader program for our point.
        mPointProgramHandle = Utils.createShaderProgram(loader, R.raw.triangcoloredpoint_vertex, R.raw.trianglecoloredpoint_fragment,
                new String[]{"a_Position"});
    }

    private final float[] mLightPosInModelSpace = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    private final float[] mLightPosInWorldSpace = new float[4];
    private final float[] mLightPosInEyeSpace = new float[4];

    private final float[] mLightModelMatrix = new float[16];


    public void draw(float[] mvpMatrix, float[] mvMatrix, float[] projMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        Utils.checkGlError();

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVPMatrix");
        Utils.checkGlError();
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "u_MVMatrix");
        Utils.checkGlError();
        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "u_LightPos");
        Utils.checkGlError();
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        Utils.checkGlError();
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");
        Utils.checkGlError();
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal");
        Utils.checkGlError();


        float tmp = (SystemClock.uptimeMillis() % (int) (2 * Math.PI * 1000)) / 100.0f;
        float xoffset = 0.5f + 3f * (float) Math.sin(tmp);
        float yoffset = 0;// 0.7f * (float) Math.sin(1 + tmp);


        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, xoffset, yoffset, 1.0f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mvMatrix, 0, mLightPosInWorldSpace, 0);

        // Prepare the triangle coordinate data
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        Utils.checkGlError();

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, vertexBuffer);
        Utils.checkGlError();

        // Pass in the color information
        GLES20.glEnableVertexAttribArray(mColorHandle);
        Utils.checkGlError();

        colorBuffer.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, COLORS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COLORS_PER_VERTEX * 4, colorBuffer);
        Utils.checkGlError();


        // get handle to fragment shader's vColor member
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        Utils.checkGlError();

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mNormalHandle, NORMALS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                NORMALS_PER_VERTEX * 4, normalBuffer);
        Utils.checkGlError();

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        Utils.checkGlError();

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);
        Utils.checkGlError();

        // Pass in the light position in eye space.
        GLES20.glUniform3fv(mLightPosHandle, 1, mLightPosInEyeSpace, 0);
        Utils.checkGlError();

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        Utils.checkGlError();


        // Lights

        GLES20.glUseProgram(mPointProgramHandle);

        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        final float dup[] = new float[16];
        Matrix.multiplyMM(dup, 0, mvMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(dup, 0, projMatrix, 0, dup, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, dup, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);

        // Disable vertex array
        //    GLES20.glDisableVertexAttribArray(mPositionHandle);
        //  StarGreeterRenderer.checkGlError();
    }
}
