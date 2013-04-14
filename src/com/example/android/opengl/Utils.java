package com.example.android.opengl;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Utils {

    public static final String TAG = "MyGLRendererShaderUtils";

    public static int compileShader(final int shaderType, final String shaderSource) {
        int shaderHandle = GLES20.glCreateShader(shaderType);

        if (shaderHandle != 0) {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource);

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shaderHandle;
    }

    public static int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            if (attributes != null) {
                final int size = attributes.length;
                for (int i = 0; i < size; i++) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

    public static int createShaderProgram(ResourceLoader loader, final int vertexShaderResourceId, final int fragmentShaderResourceId,
                                          final String[] attributes) {
        final int vertexShaderHandle = Utils.compileShader(GLES20.GL_VERTEX_SHADER, loader.loadShader(vertexShaderResourceId));
        final int fragmentShaderHandle = Utils.compileShader(GLES20.GL_FRAGMENT_SHADER, loader.loadShader(fragmentShaderResourceId));
        return Utils.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                attributes);
    }


    public static void checkGlError(String glOperation) {
        int error;
        //noinspection LoopStatementThatDoesntLoop
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    public static void checkGlError() {
        checkGlError("unnamed");
    }

    public static FloatBuffer newFloatBuffer(float[] verticesData) {
		FloatBuffer buffer = ByteBuffer.allocateDirect(verticesData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(verticesData).position(0);
		return buffer;
	}

    public static ShortBuffer newShortBuffer(short[] verticesData) {
        ShortBuffer buffer = ByteBuffer.allocateDirect(verticesData.length * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        buffer.put(verticesData).position(0);
        return buffer;
    }
}
