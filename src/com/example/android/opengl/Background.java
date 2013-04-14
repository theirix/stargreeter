package com.example.android.opengl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: irix
 * Date: 14.04.2013
 * Time: 18:03
 */
class Background {

    private final String vertexShaderCode =
            "attribute vec4 a_Position;\n" +
                    "attribute vec2 a_TexCoordinate; \n" +
                    "varying vec2 v_TexCoordinate; \n" +
                    "uniform float u_Scale; \n" +
                    "void main()\n" +
                    "{\n" +
                    "    gl_Position = a_Position;\n" +
                    "    v_TexCoordinate = a_TexCoordinate * u_Scale;\n" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;\n" +
                    "uniform sampler2D u_Texture; \n" +
                    "varying vec2 v_TexCoordinate; \n" +
                    "void main() { \n" +
                    "  gl_FragColor = texture2D(u_Texture, v_TexCoordinate); \n" +
                    "} \n";

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private FloatBuffer textureBuffer;

    private final int mProgram;
    private int mPositionHandle;
    private int mTextureUniformHandle;
    private int mTextureCoordinateHandle;
    private int mScaleHandle;

    private final int[] textures = new int[1];

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static final int TEX_PER_VERTEX = 2;
    static float squareCoords[] =
            {-0.5f, 0.5f, 0.0f,   // top left
                    -0.5f, -0.5f, 0.0f,   // bottom left
                    0.5f, -0.5f, 0.0f,   // bottom right
                    0.5f, 0.5f, 0.0f}; // top right
    // texture coordinates according square: 01, 00, 10, 11
    private float[] textureCoordinates = new float[]{
            0, 0,
            0, 1,
            1, 1,
            1, 0
    };
    private final short drawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices
    private float mScale = 50f;

    public Background(Bitmap bitmap) {
        // initialize vertex byte buffer for shape coordinates
        vertexBuffer = ByteBuffer.allocateDirect(squareCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (int i = 0; i < squareCoords.length; i++) {
            squareCoords[i] *= mScale;
        }
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        drawListBuffer = ByteBuffer.allocateDirect(drawOrder.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        loadBackgroundTexture(bitmap);

        final int vertexShaderHandle = Utils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        final int fragmentShaderHandle = Utils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = Utils.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_TexCoordinate"});
    }

    private void loadBackgroundTexture(Bitmap bitmap) {

        // Create an int array with the number of textures we want, in this case 1.
        // Tell OpenGL to generate textures.
        GLES20.glGenTextures(1, textures, 0);
        Utils.checkGlError();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        Utils.checkGlError();

        // Scale up if the texture if smaller.
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        Utils.checkGlError();
        // scale linearly when image smalled than texture
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        Utils.checkGlError();
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        Utils.checkGlError();
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        Utils.checkGlError();

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        Utils.checkGlError();

        textureBuffer = ByteBuffer.allocateDirect(textureCoordinates.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.put(textureCoordinates);
        textureBuffer.position(0);

        bitmap.recycle();
    }

    public void draw() {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        Utils.checkGlError();

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        Utils.checkGlError();
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        Utils.checkGlError();
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
        Utils.checkGlError();
        mScaleHandle = GLES20.glGetUniformLocation(mProgram, "u_Scale");
        Utils.checkGlError();

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        Utils.checkGlError();

        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        Utils.checkGlError();

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);
        Utils.checkGlError();

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        Utils.checkGlError();

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, vertexBuffer);
        Utils.checkGlError();


        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        Utils.checkGlError();

        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, TEX_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                TEX_PER_VERTEX * 4, textureBuffer);
        Utils.checkGlError();

        GLES20.glUniform1f(mScaleHandle, mScale);
        Utils.checkGlError();

        // Draw the square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        Utils.checkGlError();

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}
