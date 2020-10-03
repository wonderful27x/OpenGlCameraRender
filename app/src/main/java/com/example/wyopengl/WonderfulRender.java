package com.example.wyopengl;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 自定义着色器
 */
//https://www.cnblogs.com/wytiger/p/5693569.html //surfaceTexture
//初次接触SurfaceView、TextureView、GLSurfaceView、SurfaceTexture一些迷惑，但他们并不是同一个东西，
//SurfaceTexture并不是View，它可以将相机等的数据转换成GL纹理，然后就可以对纹理进行各种处理，
//而TextureView和GLSurfaceView都可将纹理显示出来
public class WonderfulRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    //openGL提供的surface
    private GLSurfaceView surfaceView;
    //相机
    private CameraHelper cameraHelper;
    //纹理id
    private int[] textureId;
    //画布
    private SurfaceTexture surfaceTexture;
    //openGL绘画操作类
    private ScreenDraw screenDraw;
    //变换矩阵
    float[] matrix = new float[16];

    public WonderfulRender(GLSurfaceView surfaceView){
        this.surfaceView = surfaceView;
    }

    /**
     * 画布创建时调用
     * @param gl 1.0 api预留参数
     * @param config
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        textureId = new int[1];
        //创建纹理，textureId.length :指定创建纹理数量 textureId纹理id保存，
        GLES20.glGenTextures(textureId.length,textureId,0);

        //创建SurfaceTexture并将openGL生成的纹理绑定上
        surfaceTexture = new SurfaceTexture(textureId[0]);
        //设置回调,当画布有有效数据时回调
        surfaceTexture.setOnFrameAvailableListener(this);

        //创建相机
        cameraHelper = new CameraHelper((Activity) surfaceView.getContext(), Camera.CameraInfo.CAMERA_FACING_FRONT,640,480);
        cameraHelper.setPreviewTexture(surfaceTexture);

        //创建绘画操作类
        screenDraw = new ScreenDraw(surfaceView.getContext());
    }

    /**
     * 画布发生改变时回调
     * @param gl 1.0 api预留参数
     * @param width
     * @param height
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        cameraHelper.startPreview();
        screenDraw.onReady(width,height);
    }

    /**
     * 会画一帧图像时回调
     * 该方法必须进行绘画操作（返回后会交换渲染缓冲区，不绘制会导致闪屏）
     * @param gl
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        //设置清屏颜色
        GLES20.glClearColor(255,0,0,0);
        //清理颜色缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //绘制相机图像数据
        //一般在onDrawFrame中调用updateTexImage()将数据绑定给OpenGLES对应的纹理对象。
        //注意，必须显示的调用updateTexImage()将数据更新到GL_OES_EGL_image_external类型的OpenGL ES纹理对象中后，
        //SurfaceTexture才有空间来获取下一帧的数据。否则下一帧数据永远不会交给SurfaceTexture。
        //版权声明：本文为CSDN博主「lyzirving」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
        //原文链接：https://blog.csdn.net/lyzirving/java/article/details/79051437
        surfaceTexture.updateTexImage();
        //当从OpenGL ES的纹理对象取样时，首先应该调用getTransformMatrix()来转换纹理坐标。
        //每次updateTexImage()被调用时，纹理矩阵都可能发生变化。所以，每次texture image被更新时，getTransformMatrix ()也应该被调用。
        //版权声明：本文为CSDN博主「lyzirving」的原创文章，遵循 CC 4.0 BY-SA 版权协议，转载请附上原文出处链接及本声明。
        //原文链接：https://blog.csdn.net/lyzirving/java/article/details/79051437
        //根据老师的说法，matrix只是一个变换矩阵，不是像素
        surfaceTexture.getTransformMatrix(matrix);
        screenDraw.onDrawFrame(textureId[0],matrix);
    }

    /**========================================*/

    //当画布有有效数据时回调
    //当画布SurfaceTexture有有效数据时回调，告诉GLSurfaceView可以显示了
    //SurfaceTexture.OnFrameAvailableListener
    //在前面的代码中，此接口设置给了SurfaceTexture，
    //而SurfaceTexture又设置给了相机 -> cameraHelper.setPreviewTexture(surfaceTexture);
    //而SurfaceTexture可以将相机等的数据转换成GL纹理，于是我们猜测当相机获取到数据时他就绘制到surfaceTexture上，
    //或者里理解为相机把数据交给了surfaceTexture，这样surfaceTexture获取到这帧数据后就回掉onFrameAvailable这个接口，
    //然后我们在通知GLSurface进行渲染（而GLSurface的渲染应该指的是GPU的操作，可能是滤镜的处理可能是绘制到屏幕上）
    //经过上面的分析于是我们得出利用openGL渲染相机数据的流程：
    //1.摄像头获取一帧数据
    //2.交给SurfaceTexture
    //3.SurfaceTexture回调onFrameAvailable
    //4.按需渲染-调用GLSurface.requestRender()
    //5.GLSurface回调onDrawFrame(GL10 gl)方法
    //6.从SurfaceTexture中取出纹理并交给openGl
    //7.openGl对纹理进行采用处理（一般是绘制的FBO上）
    //8.渲染到屏幕上
    //实际上从6-8的理解还是有点模糊的，这里面涉及到EGL环境，而EGL如何和纹理联系起来的至今仍没有搞明白
    //或许可以这样理解，我们知道GLSurface有一套自己的EGL环境，因此对纹理的操作实际上是渲染到了GLSurface内部创建的surface缓存去上了
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        surfaceView.requestRender();
    }
}
