package com.example.wilson.mymediacodecfpvplayer;


//Holds Vertices data and shader program's for the video rendering
public class MyGLRendererHelper {
    public static String getVertexShader(){
        return "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "  gl_Position = uMVPMatrix*aPosition;\n" +
                //"  gl_Position = aPosition;\n" +
                "  vTextureCoord = (aTextureCoord).xy;\n" +
                //"  vTextureCoord = aTextureCoord;\n" +
                //"  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                "}\n";
    }
    public static String getFragmentShader(){
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                //"  gl_FragColor =vec4(0.5,0,0,1);" +
                "}\n";
    }

    public static float[] getTriangleVerticesDataByFormat(float format,float distance){
        //F.e format=4:3
        //The x and z values stay,only the y values change for different video format's
        float x0=-5.0f , y0=(1.0f/format)*5.0f     , z0=-distance;
        float[] TriangleVerticesData=new float[5*6*30];
        OpenGLHelper.makeRectangle3(TriangleVerticesData, 0,
                x0, y0, z0,
                x0 + 10, y0, z0,
                x0 + 10, y0 - (10.0f * (1.0f / format)), z0,
                x0, y0 - (10.0f * (1.0f / format)), z0,
                0.0f, 1.0f
        );
        //makeRoundVideoCanvas(TriangleVerticesData,0,30,7.7f,10.0f);
        return TriangleVerticesData;
        //return getIcoSphereCoords();
    }
    public static int triangleVerticesDataNumberOfTraingles=3*20;
}
