package com.example.wilson.mymediacodecfpvplayer;


import android.opengl.GLES20;
import android.util.Log;

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


    public static String getVertexShader(){
        return "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uSTMatrix;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "void main() {\n" +
                        "  gl_Position = aPosition;\n" +
                        "  vTextureCoord = aTextureCoord.xy;\n" +
                        "}\n";
    }
    public static String getFragmentShader(){
        return "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                        "}\n";
    }

    public static float[] getTriangleVerticesDataByFormat(float format){
        //F.e format=4:3
        // (1/desired format) multiplied by the display factor (in this case: a 16:9 Display)
        float y=(1.0f/format)*(8.0f/9.0f);
        if(y<=0 || y>1){y=0.75f;}
        float[] TriangleVerticesData = {
                // X, Y, Z, U, V
                /*
                -1.0f, y, 0, 0.f, 0.f,
                 0.0f, y, 0, 1.f, 0.f,
                -1.0f,-y, 0, 0.f, 1.f,
                 0.0f,-y, 0, 1.f, 1.f,

                 0.0f, y, 0, 0.f, 0.f,
                 1.0f, y, 0, 1.f, 0.f,
                 0.0f,-y, 0, 0.f, 1.f,
                 1.0f,-y, 0, 1.f, 1.f*/
                //left Side
                -1.0f, y, 0, 0.f, 0.f,
                0.0f, y, 0, 1.f, 0.f,
                -1.0f,-y, 0, 0.f, 1.f,
                0.0f,-y, 0, 1.f, 1.f,
                0.0f, y, 0, 1.f, 0.f,
                -1.0f,-y, 0, 0.f, 1.f,
                //right side
                0.0f, y, 0, 0.f, 0.f,
                1.0f, y, 0, 1.f, 0.f,
                0.0f,-y, 0, 0.f, 1.f,
                0.0f,-y, 0, 0.f, 1.f,
                1.0f, y, 0, 1.f, 0.f,
                1.0f,-y, 0, 1.f, 1.f
        };
        return TriangleVerticesData;
    }






    //FOR OSD -----------------------------------------------------------------------------------------
    public static String getFragmentShader2(){
        return "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}";
    }
    public static String getVertexShader2(){
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        return "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                // the matrix must be included as a modifier of gl_Position
                // Note that the uMVPMatrix factor *must be first* in order
                // for the matrix multiplication product to be correct.
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}";
    }
    public static final int COORDS_PER_VERTEX = 3;
    /*static float triangleCoords[] = {   // in counterclockwise order:
            0.0f, 0.5f, 0.0f, // top
            -0.5f, 0.0f, 0.0f, // bottom left
            0.5f, 0.0f, 0.0f  // bottom right
    };*/
    /*
    Vertices Data for Horizon (3 Triangles)
     */
    public static float triangleCoords[] = {   // in counterclockwise order:
            /*
            0.0f, 0.0f, 0.5f, // top
            -0.5f, 0.0f,-0.5f, // bottom left
            0.5f, 0.0f,-0.5f,  // bottom right*/
            //Gleichschenkliges Dreieck
             0.0f      ,0.0f,-0.6f,  // top
             0.5196152f,0.0f, 0.3f,  // bottom right
            -0.5196152f,0.0f, 0.3f,  // bottom left
            //1
            -1.0f,0.0f,1.0f,
            -1.1f,0.0f,1.0f,
            -1.05f,0.0f,1.1f,
            //2
            1.0f,0.0f,1.0f,
            1.1f,0.0f,1.0f,
            1.05f,0.0f,1.1f,
            //3
            -1.0f,0.0f,-1.0f,
            -1.1f,0.0f,-1.0f,
            -1.05f,0.0f,-1.1f,
            //4
            1.0f,0.0f,-1.0f,
            1.1f,0.0f,-1.0f,
            1.05f,0.0f,-1.1f,


    };
    public static float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };

    public static int vertexCount = triangleCoords.length / COORDS_PER_VERTEX;
    public static int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex


    //Program for drawing overlay
    public static String getVertexShader3(){
        return  "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec2 a_texCoord;" +
                "varying vec2 v_texCoord;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "  v_texCoord = a_texCoord;" +
                "}";
    }
    public static String getFragmentShader3(){
        return "precision mediump float;" +
                "varying vec2 v_texCoord;" +
                "uniform sampler2D s_texture;" +
                "void main() {" +
                "  gl_FragColor = texture2D( s_texture, v_texCoord );" +
                //"  gl_FragColor =vec4(0.5,0,0,1);" +
                "}";
    }

    public static float[] getOverdrawCoord(){
        float offset=(float)((double)1/14.0000000000);
        float x0=-2.5f , y0= 3     , z0=0;
        float x1=-2.5f , y1= 2.75f , z1=0;
        float x2=-0.5f , y2= 3.0f  , z2=0;
        float x3=-0.5f , y3= 2.75f , z3=0;
        float x4= 1.5f , y4= 3     , z4=0;
        float x5= 1.5f , y5= 2.75f , z5=0;
        float x6=-2.5f , y6= 0.125f, z6=0;
        float x7= 1.5f , y7= 0.125f, z7=0;
        float x8=-2.5f , y8=-2.25f , z8=0;
        float x9=-2.5f , y9=-2.5f  , z9=0;
        float x10=-0.5f,y10=-2.25f  , z10=0;
        float x11=-0.5f,y11=-2.5f  , z11=0;
        float x12= 1.5f,y12=-2.25f  , z12=0;
        float x13= 1.5f,y13=-2.5f  , z13=0;

        float[] TriangleVerticesData = {
                // X, Y, Z, U, V
                //1.Rectangle (left up)
                x0   , y0       , z0  ,     0.0f , 0.0f,
                x0+1 , y0       , z0  ,   offset , 0.0f,
                x0   , y0-0.25f , z0  ,     0.0f , 1.0f,
                x0   , y0-0.25f , z0  ,     0.0f , 1.0f,
                x0+1 , y0       , z0  ,   offset , 0.0f,
                x0+1 , y0-0.25f , z0  ,   offset , 1.0f,
                //2.Rectangle (left up)
                x1   , y1       , z1  ,   offset , 0.0f,
                x1+1 , y1       , z1  , 2*offset , 0.0f,
                x1   , y1-0.25f , z1  ,   offset , 1.0f,
                x1   , y1-0.25f , z1  ,   offset , 1.0f,
                x1+1 , y1       , z1  , 2*offset , 0.0f,
                x1+1 , y1-0.25f , z1  , 2*offset , 1.0f,
                //3.Rectangle (middle up)
                x2   , y2       , z2  , 2*offset , 0.0f,
                x2+1 , y2       , z2  , 3*offset , 0.0f,
                x2   , y2-0.25f , z2  , 2*offset , 1.0f,
                x2   , y2-0.25f , z2  , 2*offset , 1.0f,
                x2+1 , y2       , z2  , 3*offset , 0.0f,
                x2+1 , y2-0.25f , z2  , 3*offset , 1.0f,
                //4.Rectangle (middle up)
                x3   , y3       , z3  , 3*offset , 0.0f,
                x3+1 , y3       , z3  , 4*offset , 0.0f,
                x3   , y3-0.25f , z3  , 3*offset , 1.0f,
                x3   , y3-0.25f , z3  , 3*offset , 1.0f,
                x3+1 , y3       , z3  , 4*offset , 0.0f,
                x3+1 , y3-0.25f , z3  , 4*offset , 1.0f,
                //5.Rectangle (right up)
                x4   , y4       , z4  , 4*offset , 0.0f,
                x4+1 , y4       , z4  , 5*offset , 0.0f,
                x4   , y4-0.25f , z4  , 4*offset , 1.0f,
                x4   , y4-0.25f , z4  , 4*offset , 1.0f,
                x4+1 , y4       , z4  , 5*offset , 0.0f,
                x4+1 , y4-0.25f , z4  , 5*offset , 1.0f,
                //6.Rectangle  (right up)
                x5   , y5       , z5  , 5*offset , 0.0f,
                x5+1 , y5       , z5  , 6*offset , 0.0f,
                x5   , y5-0.25f , z5  , 5*offset , 1.0f,
                x5   , y5-0.25f , z5  , 5*offset , 1.0f,
                x5+1 , y5       , z5  , 6*offset , 0.0f,
                x5+1 , y5-0.25f , z5  , 6*offset , 1.0f,
                //7.Rectangle  (left middle)
                x6   , y6       , z6  , 6*offset , 0.0f,
                x6+1 , y6       , z6  , 7*offset , 0.0f,
                x6   , y6-0.25f , z6  , 6*offset , 1.0f,
                x6   , y6-0.25f , z6  , 6*offset , 1.0f,
                x6+1 , y6       , z6  , 7*offset , 0.0f,
                x6+1 , y6-0.25f , z6  , 7*offset , 1.0f,
                //8.Rectangle  (right middle)
                x7   , y7       , z7  , 7*offset , 0.0f,
                x7+1 , y7       , z7  , 8*offset , 0.0f,
                x7   , y7-0.25f , z7  , 7*offset , 1.0f,
                x7   , y7-0.25f , z7  , 7*offset , 1.0f,
                x7+1 , y7       , z7  , 8*offset , 0.0f,
                x7+1 , y7-0.25f , z7  , 8*offset , 1.0f,
                //9.Rectangle  (right down)
                x8   , y8       , z8  , 8*offset , 0.0f,
                x8+1 , y8       , z8  , 9*offset , 0.0f,
                x8   , y8-0.25f , z8  , 8*offset , 1.0f,
                x8   , y8-0.25f , z8  , 8*offset , 1.0f,
                x8+1 , y8       , z8  , 9*offset , 0.0f,
                x8+1 , y8-0.25f , z8  , 9*offset , 1.0f,
                //10.Rectangle  (right down)
                x9   , y9       , z9  , 9*offset , 0.0f,
                x9+1 , y9       , z9  ,10*offset , 0.0f,
                x9   , y9-0.25f , z9  , 9*offset , 1.0f,
                x9   , y9-0.25f , z9  , 9*offset , 1.0f,
                x9+1 , y9       , z9  ,10*offset , 0.0f,
                x9+1 , y9-0.25f , z9  ,10*offset , 1.0f,
                //11.Rectangle  (middle down)
                x10  , y10      , z10 ,10*offset , 0.0f,
                x10+1, y10      , z10 ,11*offset , 0.0f,
                x10  , y10-0.25f, z10 ,10*offset , 1.0f,
                x10  , y10-0.25f, z10 ,10*offset , 1.0f,
                x10+1, y10      , z10 ,11*offset , 0.0f,
                x10+1, y10-0.25f, z10 ,11*offset , 1.0f,
                //12.Rectangle  (middle down)
                x11  , y11      , z11 ,11*offset , 0.0f,
                x11+1, y11      , z11 ,12*offset , 0.0f,
                x11  , y11-0.25f, z11 ,11*offset , 1.0f,
                x11  , y11-0.25f, z11 ,11*offset , 1.0f,
                x11+1, y11      , z11 ,12*offset , 0.0f,
                x11+1, y11-0.25f, z11 ,12*offset , 1.0f,
                //13.Rectangle  (middle down)
                x12  , y12      , z12 ,12*offset , 0.0f,
                x12+1, y12      , z12 ,13*offset , 0.0f,
                x12  , y12-0.25f, z12 ,12*offset , 1.0f,
                x12  , y12-0.25f, z12 ,12*offset , 1.0f,
                x12+1, y12      , z12 ,13*offset , 0.0f,
                x12+1, y12-0.25f, z12 ,13*offset , 1.0f,
                //14.Rectangle  (middle down)
                x13  , y13      , z13 ,13*offset , 0.0f,
                x13+1, y13      , z13 ,14*offset , 0.0f,
                x13  , y13-0.25f, z13 ,13*offset , 1.0f,
                x13  , y13-0.25f, z13 ,13*offset , 1.0f,
                x13+1, y13      , z13 ,14*offset , 0.0f,
                x13+1, y13-0.25f, z13 ,14*offset , 1.0f

        };
        return TriangleVerticesData;
    }
}

