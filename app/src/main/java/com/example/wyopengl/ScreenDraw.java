package com.example.wyopengl;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 通过操作openGL绘制图像
 */
public class ScreenDraw {

    private int vertexCoordinate; //顶点坐标索引
    private int textureCoordinate;//纹理坐标索引
    private int transformMatrix;  //变换矩阵索引
    private int texture;          //采样器索引

    private FloatBuffer vertexCoordinateBuffer; //顶点坐标
    private FloatBuffer textureCoordinateBuffer;//纹理坐标

    private int width;
    private int height;

    private int programId;//程序id

    public ScreenDraw(Context context){
        //顶点着色器源码
        String vertexSource = OpenGLCodeReader.sourceReader(context,R.raw.vertex_shader);
        //片元着色器源码
        String fragmentSource = OpenGLCodeReader.sourceReader(context,R.raw.fragment_shader);

        /**
         * 1.配置顶点着色器
         */
        //1.1创建顶点着色器
        int vertexShaderId = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        //1.2绑定着色器源码
        GLES20.glShaderSource(vertexShaderId,vertexSource);
        //1.3编译源码
        GLES20.glCompileShader(vertexShaderId);
        //1.4判断是否编译成功
        int[] state = new int[1];
        GLES20.glGetShaderiv(vertexShaderId,GLES20.GL_COMPILE_STATUS,state,0);
        if(state[0] != GLES20.GL_TRUE){
            throw new IllegalStateException("顶点着色器配置失败！");
        }

        /**
         * 2.配置片元着色器
         */
        //2.1创建片元着色器
        int fragmentShaderId = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        //2.2绑定源码
        GLES20.glShaderSource(fragmentShaderId,fragmentSource);
        //2.3编译源码
        GLES20.glCompileShader(fragmentShaderId);
        //2.4判断编译是否成功
        GLES20.glGetShaderiv(fragmentShaderId,GLES20.GL_COMPILE_STATUS,state,0);
        if(state[0] != GLES20.GL_TRUE){
            throw new IllegalStateException("片元着色器配置失败！");
        }

        /**
         * 3.配置着色器程序
         */
        //3.1创建着色器程序
        programId = GLES20.glCreateProgram();
        //3.2将顶点和片元着色器绑定到程序上
        GLES20.glAttachShader(programId,vertexShaderId);
        GLES20.glAttachShader(programId,fragmentShaderId);
        //3.3链接着色器
        GLES20.glLinkProgram(programId);
        //判断链接是否成功
        GLES20.glGetProgramiv(programId,GLES20.GL_LINK_STATUS,state,0);
        if(state[0] != GLES20.GL_TRUE){
            throw new IllegalStateException("着色器程序配置失败！");
        }

        /**
         * 4.释放，删除着色器
         */
        GLES20.glDeleteShader(vertexShaderId);
        GLES20.glDeleteShader(fragmentShaderId);

        /**
         * 5.给顶点、片元着色器变量赋值
         */
        //5.1获取变量索引
        vertexCoordinate = GLES20.glGetAttribLocation(programId,"vertexCoordinate");  //顶点坐标
        textureCoordinate = GLES20.glGetAttribLocation(programId,"textureCoordinate");//纹理坐标
        transformMatrix = GLES20.glGetUniformLocation(programId,"transformMatrix");   //变换矩阵
        texture = GLES20.glGetUniformLocation(programId,"texture");                   //采样器
        //5.2顶点坐标缓冲区分配内存
        vertexCoordinateBuffer = ByteBuffer
                .allocateDirect(4*2*4)          //坐标数 * 左边维度（2D-XY）* 所占字节数（float为4字节）
                .order(ByteOrder.nativeOrder()) //使用本地字节序
                .asFloatBuffer();               //转换
        //5.3顶点坐标缓冲区赋值
        //使用世界坐标，坐标顺序很有讲究，连续的三个点组成一个三角形，现在需要画矩形，因此顺序不能按照某个方向依次排列
        //具体看直播内容和https://www.jianshu.com/p/c4dda6884655
        float[] vertexCoordinate = {
            -1.0f,-1.0f,
            1.0f, -1.0f,
            -1.0f,1.0f,
            1.0f, 1.0f
        };
        vertexCoordinateBuffer.clear();
        vertexCoordinateBuffer.put(vertexCoordinate);
        //5.4纹理坐标缓冲区内存分配
        textureCoordinateBuffer = ByteBuffer
                .allocateDirect(4*2*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        //5.5纹理缓冲区赋值
        //使用android屏幕坐标系，这里的坐标顺序如果和顶点坐标顺序一样，图像是倒的，还需反转镜像
        float[] textureCoordinate = {
//                //和顶点坐标顺序一样
//                0.0f,1.0f,
//                1.0f,1.0f,
//                0.0f,0.0f,
//                1.0f,0.0f

                //逆时针旋转并镜像翻转
                0.0f,0.0f,
                1.0f,0.0f,
                0.0f,1.0f,
                1.0f,1.0f
        };
        textureCoordinateBuffer.clear();
        textureCoordinateBuffer.put(textureCoordinate);

    }

    public void onReady(int width,int height){
        this.width = width;
        this.height = height;
    }

    /**
     * 绘制
     * @param textureId openGL创建的画布
     * @param matrix 相机数据
     */
    public void onDrawFrame(int textureId,float[] matrix){
        /**1.设置视窗大小*/
        GLES20.glViewport(0,0,width,height);
        /**2.使用着色器程序*/
        GLES20.glUseProgram(programId);
        /**3.顶点坐标赋值*/
        //因为要是从第0个开始取，所有设置position为零，相当于一个偏移
        vertexCoordinateBuffer.position(0);
        //顶点坐标赋值（attribute类型，这与源码中是对应的）
        //p1：顶点坐标索引，p2：坐标维度（2D,相对与每次取2个数据为一个坐标），p3：数据类型
        //p4：指定在访问定点数据值时是应将其标准化（GL_TRUE）还是直接转换为定点值（GL_FALSE）。
        //p5：指定连续通用顶点属性之间的字节偏移量。 如果stride为0，则通用顶点属性被理解为紧密打包在数组中的。 初始值为0
        //p6：指定指向数组中第一个通用顶点指针
        GLES20.glVertexAttribPointer(vertexCoordinate,2,GLES20.GL_FLOAT,false,0,vertexCoordinateBuffer);
        //激活
        GLES20.glEnableVertexAttribArray(vertexCoordinate);
        /**4.纹理坐标赋值（attribute类型，这与源码中是对应的），原理参数同上*/
        textureCoordinateBuffer.position(0);
        GLES20.glVertexAttribPointer(textureCoordinate,2,GLES20.GL_FLOAT,false,0,textureCoordinateBuffer);
        //激活
        GLES20.glEnableVertexAttribArray(textureCoordinate);
        /**5.变化矩阵赋值（uniform类型，这与源码中是对应的）*/
        //p1：矩阵索引，p2：被赋值的Uniform数量（可以是数组所以可以有多个）
        //p3：uniform变量赋值时该矩阵是否需要转置。因为我们使用的是glm定义的矩阵，因此不要进行转置
        //p4：数据指针
        //p5：偏移
        GLES20.glUniformMatrix4fv(transformMatrix,1,false,matrix,0);
        /**6.激活图层*/
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        /**7.绑定纹理*/
        //不能使用GL_TEXTURE_2D
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId);
        //采样器赋值，这个有点晕，0好像是默认的位置
        GLES20.glUniform1i(texture,0);
        /**8.通知openGL绘制*/
        //以连续三顶点绘制三角形的方式绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
    }
}
