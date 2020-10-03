//着色器：着色器(Shader)是运行在GPU上的小程序
//片元着色器：处理光、阴影、遮挡、环境等等对物体表面的影响，最终生成一副图像的小程序
//安卓中不能直接使 Sampler2D（注意：这种情况是在将摄像头数据经openGL直接渲染到屏幕上，而没有经过FBO）,
//而是 samplerExternalOES，由于其不是openGL内部默认支持的，所以需要打开外部扩展
//https://www.jianshu.com/p/f1a86ac46b4d
#extension GL_OES_EGL_image_external : require
//设置float为中等精度
precision mediump float;
//从顶点着色器传过来的参数（这里是坐标），vec2：包含两个float的向量（因为是坐标所以使用vec2这就很好理解了）
varying vec2 coordinate;
//采样器
uniform samplerExternalOES texture;

void main(){
    //采集指定坐标位置的纹理（2D）
    //gl_FragColor（内置变量）：vec4类型，表示片元着色器中颜色
    //疑问：这里采集的纹理是一个像素吗
    gl_FragColor = texture2D(texture,coordinate);
}