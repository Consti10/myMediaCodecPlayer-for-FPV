package com.example.wilson.mymediacodecfpvplayer;


import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ByteOrder;

public class OpenGLHelper {
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    public static void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(op + ": glError " + error);
        }
    }
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }
    public static FloatBuffer getFloatBuffer(float[] floatBuffer){
        FloatBuffer bb = ByteBuffer.allocateDirect(
                floatBuffer.length*4) .order(ByteOrder.nativeOrder()).asFloatBuffer();
        bb.put(floatBuffer).position(0);
        return bb;
    }

    public static void makeRectangle(float[] array,int arrayOffset,float x,float y,float z,float height,float width,float r,float g,float b,float a){
        array[arrayOffset   ]=x;
        array[arrayOffset+ 1]=y;
        array[arrayOffset+ 2]=z;
        array[arrayOffset+ 3]=r;
        array[arrayOffset+ 4]=g;
        array[arrayOffset+ 5]=b;
        array[arrayOffset+ 6]=a;
        array[arrayOffset+ 7]=x+width;
        array[arrayOffset+ 8]=y;
        array[arrayOffset+ 9]=z;
        array[arrayOffset+10]=r;
        array[arrayOffset+11]=g;
        array[arrayOffset+12]=b;
        array[arrayOffset+13]=a;
        array[arrayOffset+14]=x+width;
        array[arrayOffset+15]=y-height;
        array[arrayOffset+16]=z;
        array[arrayOffset+17]=r;
        array[arrayOffset+18]=g;
        array[arrayOffset+19]=b;
        array[arrayOffset+20]=a;
        array[arrayOffset+21]=x;
        array[arrayOffset+22]=y;
        array[arrayOffset+23]=z;
        array[arrayOffset+24]=r;
        array[arrayOffset+25]=g;
        array[arrayOffset+26]=b;
        array[arrayOffset+27]=a;
        array[arrayOffset+28]=x+width;
        array[arrayOffset+29]=y-height;
        array[arrayOffset+30]=z;
        array[arrayOffset+31]=r;
        array[arrayOffset+32]=g;
        array[arrayOffset+33]=b;
        array[arrayOffset+34]=a;
        array[arrayOffset+35]=x;
        array[arrayOffset+36]=y-height;
        array[arrayOffset+37]=z;
        array[arrayOffset+38]=r;
        array[arrayOffset+39]=g;
        array[arrayOffset+40]=b;
        array[arrayOffset+41]=a;
    }
    public static void makeRectangle2(float[] array,int arrayOffset,float x,float y,float z,float height,float width,float xOff,float offset){
        array[arrayOffset   ]=x;
        array[arrayOffset+ 1]=y;
        array[arrayOffset+ 2]=z;
        array[arrayOffset+ 3]=xOff;
        array[arrayOffset+ 4]=0.0f;
        array[arrayOffset+ 5]=x+width;
        array[arrayOffset+ 6]=y;
        array[arrayOffset+ 7]=z;
        array[arrayOffset+ 8]=xOff+offset;
        array[arrayOffset+ 9]=0.0f;
        array[arrayOffset+10]=x+width;
        array[arrayOffset+11]=y-height;
        array[arrayOffset+12]=z;
        array[arrayOffset+13]=xOff+offset;
        array[arrayOffset+14]=1.0f;
        array[arrayOffset+15]=x;
        array[arrayOffset+16]=y;
        array[arrayOffset+17]=z;
        array[arrayOffset+18]=xOff;
        array[arrayOffset+19]=0.0f;
        array[arrayOffset+20]=x+width;
        array[arrayOffset+21]=y-height;
        array[arrayOffset+22]=z;
        array[arrayOffset+23]=xOff+offset;
        array[arrayOffset+24]=1.0f;
        array[arrayOffset+25]=x;
        array[arrayOffset+26]=y-height;
        array[arrayOffset+27]=z;
        array[arrayOffset+28]=xOff;
        array[arrayOffset+29]=1.0f;
    }
    public static void makeRectangle3(float[] array,int arrayOffset,
                                      float x,float y,float z,
                                      float x2, float y2,float z2,
                                      float x3,float y3, float z3,
                                      float x4,float y4,float z4,
                                      float xOff,float offset){
        array[arrayOffset   ]=x;
        array[arrayOffset+ 1]=y;
        array[arrayOffset+ 2]=z;
        array[arrayOffset+ 3]=xOff;
        array[arrayOffset+ 4]=0.0f;
        array[arrayOffset+ 5]=x2;
        array[arrayOffset+ 6]=y2;
        array[arrayOffset+ 7]=z2;
        array[arrayOffset+ 8]=xOff+offset;
        array[arrayOffset+ 9]=0.0f;
        array[arrayOffset+10]=x3;
        array[arrayOffset+11]=y3;
        array[arrayOffset+12]=z3;
        array[arrayOffset+13]=xOff+offset;
        array[arrayOffset+14]=1.0f;
        array[arrayOffset+15]=x;
        array[arrayOffset+16]=y;
        array[arrayOffset+17]=z;
        array[arrayOffset+18]=xOff;
        array[arrayOffset+19]=0.0f;
        array[arrayOffset+20]=x3;
        array[arrayOffset+21]=y3;
        array[arrayOffset+22]=z3;
        array[arrayOffset+23]=xOff+offset;
        array[arrayOffset+24]=1.0f;
        array[arrayOffset+25]=x4;
        array[arrayOffset+26]=y4;
        array[arrayOffset+27]=z4;
        array[arrayOffset+28]=xOff;
        array[arrayOffset+29]=1.0f;
    }
    public static void makeRoundVideoCanvas(float[] array,int arrayOffset,int steppSize,float height,float width){
        //4 rectangles
        int count=0;
        float r=(width/2);
        /*
        double distance_x=1-Math.cos(Math.toRadians((double)alpha_steppSize));
        distance_x=(distance_x*distance_x);
        double distance_z=Math.sin(Math.toRadians((double)alpha_steppSize));
        distance_z=(distance_z*distance_z);
        float texture_radius=(float)Math.sqrt(distance_x+distance_z);*/
        float alpha_steppSize=(180/steppSize);
        float texture_radius=1/(180/alpha_steppSize);
        for(int alpha=180;alpha>0;alpha-=alpha_steppSize){
            float x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4;
            x1=(float)(r*Math.cos(Math.toRadians((double)alpha)));
            y1=(height/2);
            z1=-(float)(r*Math.sin(Math.toRadians((double) alpha)));
            x2=(float)(r*Math.cos(Math.toRadians((double)alpha+alpha_steppSize)));
            y2=(height/2);
            z2=-(float)(r*Math.sin(Math.toRadians((double) alpha+alpha_steppSize)));
            x3=(float)(r*Math.cos(Math.toRadians((double)alpha+alpha_steppSize)));
            y3=-(height/2);
            z3=-(float)(r*Math.sin(Math.toRadians((double)alpha+alpha_steppSize)));
            x4=(float)(r*Math.cos(Math.toRadians((double)alpha)));
            y4=-(height/2);
            z4=-(float)(r*Math.sin(Math.toRadians((double) alpha)));
            makeRectangle3(array,arrayOffset+(count*5*6),x1,y1,z1,x2,y2,z2,x3,y3,z3,x4,y4,z4,count*texture_radius,texture_radius);
            count++;
        }
    }

    public static float[] getIcoSphereCoords(){
        float t = (float)((Math.sqrt(5) - 1)/2);
        float[][] icoshedronVertices = new float[][] {
                new float[] { -1,-t,0 },
                new float[] { 0,1,t },
                new float[] { 0,1,-t },
                new float[] { 1,t,0 },
                new float[] { 1,-t,0 },
                new float[] { 0,-1,-t },
                new float[] { 0,-1,t },
                new float[] { t,0,1 },
                new float[] { -t,0,1 },
                new float[] { t,0,-1 },
                new float[] { -t,0,-1 },
                new float[] { -1,t,0 },
        };
        for (float[] v : icoshedronVertices) {
            // Normalize the vertices to have unit length.
            float length = (float)Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
            v[0] /= length;
            v[1] /= length;
            v[2] /= length;
        }
        for(int i1=0;i1<12;i1++){
            for(int i2=0;i2<3;i2++){
                icoshedronVertices[i1][i2]=6*icoshedronVertices[i1][i2];
            }
        }
        int[][] icoshedronFaces = new int[][] {
                { 3, 7, 1 },
                { 4, 7, 3 },
                { 6, 7, 4 },
                { 8, 7, 6 },
                { 7, 8, 1 },
                { 9, 4, 3 },
                { 2, 9, 3 },
                { 2, 3, 1 },
                { 11, 2, 1 },
                { 10, 2, 11 },
                { 10, 9, 2 },
                { 9, 5, 4 },
                { 6, 4, 5 },
                { 0, 6, 5 },
                { 0, 11, 8 },
                { 11, 1, 8 },
                { 10, 0, 5 },
                { 10, 5, 9 },
                { 0, 8, 6 },
                { 0, 10, 11 },
        };
        float[] vertices=new float[20*15];
        for(int i=0;i<20;i++){
            int index=icoshedronFaces[i][0];
            //first Triangle with tex. coords
            vertices[(i*15)+0]=icoshedronVertices[index][0];
            vertices[(i*15)+1]=icoshedronVertices[index][1];
            vertices[(i*15)+2]=icoshedronVertices[index][2];
            vertices[(i*15)+3]=0.0f;
            vertices[(i*15)+4]=1.0f;
            index=icoshedronFaces[i][1];
            vertices[(i*15)+5]=icoshedronVertices[index][0];
            vertices[(i*15)+6]=icoshedronVertices[index][1];
            vertices[(i*15)+7]=icoshedronVertices[index][2];
            vertices[(i*15)+8]=0.0f;
            vertices[(i*15)+9]=0.0f;
            index=icoshedronFaces[i][2];
            vertices[(i*15)+10]=icoshedronVertices[index][0];
            vertices[(i*15)+11]=icoshedronVertices[index][1];
            vertices[(i*15)+12]=icoshedronVertices[index][2];
            vertices[(i*15)+13]=1.0f;
            vertices[(i*15)+14]=0.0f;
        }

        return vertices;
    }


}

