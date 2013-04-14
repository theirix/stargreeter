package com.example.android.opengl;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: irix
 * Date: 14.04.2013
 * Time: 18:04
 */
@SuppressWarnings("FieldCanBeLocal")
class TriangleColored {

    private static final String TAG = "MyGLRendererTriangleColored";


    private final String vertexShaderCode =

            "uniform mat4 u_MVPMatrix;      \n"        // A constant representing the combined model/view/projection matrix.
                    + "uniform mat4 u_MVMatrix;       \n"        // A constant representing the combined model/view matrix.
                    + "uniform vec3 u_LightPos;       \n"        // The position of the light in eye space.

                    + "attribute vec4 a_Position;     \n"        // Per-vertex position information we will pass in.
                    + "attribute vec4 a_Color;        \n"        // Per-vertex color information we will pass in.
                    + "attribute vec3 a_Normal;       \n"        // Per-vertex normal information we will pass in.

                    + "varying vec4 v_Color;          \n"        // This will be passed into the fragment shader.

                    + "void main()                    \n"    // The entry point for our vertex shader.
                    + "{                              \n"
                    // Transform the vertex into eye space.
                    + "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);              \n"
                    // Transform the normal's orientation into eye space.
                    + "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));     \n"
                    // Will be used for attenuation.
                    + "   float distance = length(u_LightPos - modelViewVertex);             \n"
                    // Get a lighting direction vector from the light to the vertex.
                    + "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);        \n"
                    // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
                    // pointing in the same direction then it will get max illumination.
                    + "   float diffuse = max(dot(modelViewNormal, lightVector), 0.9);       \n"
                    // Attenuate the light based on distance.
                    + "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));  \n"
                    // Multiply the color by the illumination level. It will be interpolated across the triangle.
                    + "   v_Color = a_Color * diffuse ;                                       \n"
                    // gl_Position is a special variable used to store the final position.
                    // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
                    + "   gl_Position = u_MVPMatrix * a_Position;                            \n"
                    + "}                                                                     \n";


    private final String fragmentShaderCode =
            "precision mediump float; \n" +
                    "varying vec4 v_Color; \n" +
                    "void main() { \n" +
                    "  gl_FragColor = v_Color;       \n" +
                    "}\n";

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private FloatBuffer normalBuffer;
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
    static float triangleCoords[] = { // in counterclockwise order:
            1.0f, 2.622008459f, 0.0f,   // top
            -2.5f, -0.311004243f, 0.0f,   // bottom left
            2.5f, -0.311004243f, 0.0f    // bottom right
    };
    static float normalCoords[] = { // in counterclockwise order:
            0, 0, 1,
            0, 0, 1,
            0, 0, 1
    };
    static float colorsArray[] = { // in counterclockwise order:
            1.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f
    };
    private final int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;

    public TriangleColored() {
        // initialize vertex byte buffer for shape coordinates
        vertexBuffer = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                triangleCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(triangleCoords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);


        colorBuffer = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                colorsArray.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        // add the coordinates to the FloatBuffer
        colorBuffer.put(colorsArray);
        // set the buffer to read the first coordinate
        colorBuffer.position(0);


        normalBuffer = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                normalCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        // add the coordinates to the FloatBuffer
        normalBuffer.put(normalCoords);
        // set the buffer to read the first coordinate
        normalBuffer.position(0);


        final int vertexShaderHandle = Utils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        final int fragmentShaderHandle = Utils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = Utils.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_Color", "a_Normal"});

        // Define a simple shader program for our point.
        final String pointVertexShader =
                "uniform mat4 u_MVPMatrix;      \n"
                        + "attribute vec4 a_Position;     \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_Position = u_MVPMatrix * a_Position;   \n"
                        + "   gl_PointSize = 5.0;         \n"
                        + "}                              \n";

        final String pointFragmentShader =
                "precision mediump float;       \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);             \n"
                        + "}                              \n";

        final int pointVertexShaderHandle = Utils.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = Utils.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = Utils.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[]{"a_Position"});
    }

    private final float[] mLightPosInModelSpace = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    private final float[] mLightPosInWorldSpace = new float[4];
    private final float[] mLightPosInEyeSpace = new float[4];

    private float[] mLightModelMatrix = new float[16];


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
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
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
        //  MyGLRenderer.checkGlError();
    }
}
