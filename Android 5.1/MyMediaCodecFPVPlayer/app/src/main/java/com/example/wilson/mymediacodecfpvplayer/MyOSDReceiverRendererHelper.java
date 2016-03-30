package com.example.wilson.mymediacodecfpvplayer;



public class MyOSDReceiverRendererHelper {
    //Holds Vertex data and shader Programms for the OSD
    public static String getVertexShader2(){
        return "uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
                + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
                + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.
                + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
                + "void main()                    \n"		// The entry point for our vertex shader.
                + "{                              \n"
                + "   v_Color = a_Color;          \n"		// Pass the color through to the fragment shader.
                // It will be interpolated across the triangle.
                + "   gl_Position = u_MVPMatrix   \n" 	// gl_Position is a special variable used to store the final position.
                + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
                + "}                              \n";    // normalized screen coordinates.
    }
    public static String getFragmentShader2(){
        return "precision mediump float;       \n"		// Set the default precision to medium. We don't need as high of a
                // precision in the fragment shader.
                + "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the
                // triangle per fragment.
                + "void main()                    \n"		// The entry point for our fragment shader.
                + "{                              \n"
                + "   gl_FragColor = v_Color;     \n"		// Pass the color directly through the pipeline.
                + "}                              \n";
    }

    public static final int COORDS_PER_VERTEX = 3;
    public static float[] getTriangleCoords(){
        float[] kopterAndSideArrows={
                //Kopter(gleichschenkliges Dreieck und 4 weitere)
                0.0f, 0.0f, -0.6f,  // top
                1.0f, 0.0f, 0.0f, 1.0f, //Color red
                0.5196152f, 0.0f, 0.3f,  // bottom right
                0.0f, 0.0f, 1.0f, 1.0f,  //Color blue
                -0.5196152f, 0.0f, 0.3f,  // bottom left
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow
                //1
                -1.1f, 0.0f, -1.0f,
                1.0f, 0.0f, 0.0f, 1.0f, //Color red
                -1.0f, 0.0f, -1.0f,
                1.0f, 0.0f, 0.0f, 1.0f, //Color red
                //-1.05f, 0.0f, -1.1f,
                0.0f,0.0f,0.0f,
                1.0f, 0.0f, 0.0f, 1.0f, //Color red
                //2
                1.1f, 0.0f, -1.0f,
                1.0f, 0.0f, 0.0f, 1.0f, //Color red
                1.0f, 0.0f, -1.0f,
                1.0f, 0.0f, 0.0f, 1.0f, //Color red
                //1.05f, 0.0f, -1.1f,
                0.0f,0.0f,0.0f,
                1.0f, 0.0f, 0.0f, 1.0f, //Color red
                //3
                1.1f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,  //Color blue
                1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,  //Color blue
                //1.05f, 0.0f, 1.1f,
                0.0f,0.0f,0.0f,
                0.0f, 0.0f, 1.0f, 1.0f,  //Color blue
                //4
                -1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow
                -1.1f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow
                //-1.05f, 0.0f, 1.1f,
                0.0f,0.0f,0.0f,
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow
                //Arrow representing Copter's height
                //left side
                -1.75f , 0.05f, 0.0f,
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow
                -1.85f , 0.0f,   0.0f,
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow
                -1.75f ,-0.05f, 0.0f,
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow
                //right side
                1.75f , 0.05f, 0.0f,
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow
                1.85f , 0.00f, 0.0f,
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow
                1.75f ,-0.05f, 0.0f,
                1.0f, 1.0f, 0.0f, 1.0f, //Color yellow

        };
        float[] homeArrowCoords= {
                -0.2f, 1.0f, 1.1f,
                0.0f, 0.1f, 0.0f, 1.0f, //Color dark green
                0.2f, 1.0f, 1.1f,
                0.0f, 0.1f, 0.0f, 1.0f, //Color dark green
                0.0f, 1.0f, 1.6f,
                0.0f, 1.0f, 0.0f, 1.0f, //Color green
        };

        float[] FloatReturn=new float[homeArrowCoords.length+kopterAndSideArrows.length+(18*42)];
        //coords for lines representing height
        float x=-2.1f,y=2.005f,z=0.00f,height=0.01f,width=0.25f;
        OpenGLHelper.makeRectangle(FloatReturn, 0, x, y - 0.0f, z, height, width, 0.0f, 1.0f, 0.0f, 0.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (1 * 42), x + (width / 2), y - 0.5f, z, height, width / 2, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (2 * 42), x, y - 1.0f, z, height, width, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (3 * 42), x + (width / 2), y - 1.5f, z, height, width / 2, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (4 * 42), x, y - 2.0f, z, height, width, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (5 * 42), x + (width / 2), y - 2.5f, z, height, width / 2, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (6 * 42), x, y - 3.0f, z, height, width, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (7 * 42), x + (width / 2), y - 3.5f, z, height, width / 2, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (8 * 42), x, y - 4.0f, z, height, width, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (9 * 42), -x - width, y - 0.0f, z, height, width, 1.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (10 * 42), -x - width, y - 0.5f, z, height, width / 2, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (11 * 42), -x - width, y - 1.0f, z, height, width, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (12 * 42), -x - width, y - 1.5f, z, height, width / 2, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (13 * 42), -x - width, y - 2.0f, z, height, width, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (14 * 42), -x - width, y - 2.5f, z, height, width / 2, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (15 * 42), -x - width, y - 3.0f, z, height, width, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (16 * 42), -x - width, y - 3.5f, z, height, width / 2, 0.0f, 1.0f, 0.0f, 1.0f);
        OpenGLHelper.makeRectangle(FloatReturn, (17 * 42), -x - width, y - 4.0f, z, height, width, 0.0f, 1.0f, 0.0f, 1.0f);
        int height_lines_length=18*42;

        int counter=0;
        for(int i=height_lines_length;i<height_lines_length+kopterAndSideArrows.length;i++){
            FloatReturn[i]=kopterAndSideArrows[counter];
            counter++;
        }
        counter=0;
        for(int i=height_lines_length+kopterAndSideArrows.length;i<height_lines_length+kopterAndSideArrows.length+homeArrowCoords.length;i++){
            FloatReturn[i]=homeArrowCoords[counter];
            counter++;
        }
        return FloatReturn;

    }
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

    //koords for the OSD Overdraw
    public static float[] getOverdrawCoordByFormat(int numberOfTextureUnits,float videoFormat,float videoDistance){
        //Texture atlas offset
        float offset=(float)(1.0f/numberOfTextureUnits);
        float width=2,height=0.5f;
        float x=-5.0f,y=(((1.0f/videoFormat)*5.0f)+(2*height))+0.0001f,z=-videoDistance;
        float x0= x         , y0= y       , z0=z;
        float x1= x         , y1= y-height, z1=z;
        float x2=-(width/2) , y2= y       , z2=z;
        float x3=-(width/2) , y3= y-height, z3=z;
        float x4=-x-width   , y4= y       , z4=z;
        float x5=-x-width   , y5= y-height, z5=z;
        float x6= x      , y6= y-(2*height) , z6=z;
        float x7=-x-width, y7= y-(2*height) , z7=z;
        float x8= x         , y8=-y+(height*2), z8=z;
        float x9= x         , y9=-y+height     , z9=z;
        float x10=-(width/2),y10=-y+(height*2),z10=z;
        float x11=-(width/2),y11=-y+height    ,z11=z;
        float x12=-x-width  ,y12=-y+(height*2),z12=z;
        float x13=-x-width  ,y13=-y+height    ,z13=z;
        float[] TriangleVerticesData=new float[(14+5+5)*6*5];
        OpenGLHelper.makeRectangle2(TriangleVerticesData,0 *30,x0 ,y0 ,z0 ,height,width,offset*0 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,1 *30,x1 ,y1 ,z1 ,height,width,offset*1 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,2 *30,x2 ,y2 ,z2 ,height,width,offset*2 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,3 *30,x3 ,y3 ,z3 ,height,width,offset*3 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,4 *30,x4 ,y4 ,z4 ,height,width,offset*4 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,5 *30,x5 ,y5 ,z5 ,height,width,offset*5 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,6 *30,x6 ,y6 ,z6 ,height,width,offset*6 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,7 *30,x7 ,y7 ,z7 ,height,width,offset*7 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,8 *30,x8 ,y8 ,z8 ,height,width,offset*8 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,9 *30,x9 ,y9 ,z9 ,height,width,offset*9 ,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,10*30,x10,y10,z10,height,width,offset*10,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,11*30,x11,y11,z11,height,width,offset*11,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,12*30,x12,y12,z12,height,width,offset*12,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,13*30,x13,y13,z13,height,width,offset*13,offset);
        float x14=-1.8f-0.5f,y14= 2.0625f  , z14=0;
        float x15=-1.8f-0.5f,y15= 1.0625f  , z15=0;
        float x16=-1.8f-0.5f,y16= 0.0625f  , z16=0;
        float x17=-1.8f-0.5f,y17=-0.9375f  , z17=0;
        float x18=-1.8f-0.5f,y18=-1.9375f  , z18=0;
        float x19= 2.1f,y19= 2.0625f  , z19=0;
        float x20= 2.1f,y20= 1.0625f  , z20=0;
        float x21= 2.1f,y21= 0.0625f  , z21=0;
        float x22= 2.1f,y22=-0.9375f  , z22=0;
        float x23= 2.1f,y23=-1.9375f  , z23=0;
        OpenGLHelper.makeRectangle2(TriangleVerticesData,14*30,x14,y14,z14,0.125f,0.5f,offset*14,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,15*30,x15,y15,z15,0.125f,0.5f,offset*15,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,16*30,x16,y16,z16,0.125f,0.5f,offset*16,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,17*30,x17,y17,z17,0.125f,0.5f,offset*17,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,18*30,x18,y18,z18,0.125f,0.5f,offset*18,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,19*30,x19,y19,z19,0.125f,0.5f,offset*14,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,20*30,x20,y20,z20,0.125f,0.5f,offset*15,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,21*30,x21,y21,z21,0.125f,0.5f,offset*16,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,22*30,x22,y22,z22,0.125f,0.5f,offset*17,offset);
        OpenGLHelper.makeRectangle2(TriangleVerticesData,23*30,x23,y23,z23,0.125f,0.5f,offset*18,offset);
        return TriangleVerticesData;
    }
}
