package com.example.wyopengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class WonderfulGLSurfaceView extends GLSurfaceView {

    public WonderfulGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        //设置版本
        setEGLContextClientVersion(2);
        //设置自定义渲染器
        setRenderer(new WonderfulRender(this));
        //设置渲染模式-按需渲染
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
}
