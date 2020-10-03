//着色器：着色器(Shader)是运行在GPU上的小程序
//顶点着色器：处理顶点、法线等数据的小程序。
//顶点坐标,给其值赋确定要画的形状，我的理解是这里要画相机预览数据是一个矩形，所有顶点坐标有四个，用vector4
attribute vec4 vertexCoordinate;
//纹理坐标，纹理坐标代表什么，为什么是vec4类型
attribute vec4 textureCoordinate;
//变化矩阵
//疑问：mat4是4*4的矩阵，自定义render中float[16]应该是对应的，但是为什么是16
//根据老师的解释，这只是一个变换矩阵，是对图像进行运算的矩阵，并不是像素
uniform mat4 transformMatrix;
//坐标，传给片元着色器
//疑问：这个坐标对应的是一个像素吗
varying vec2 coordinate;

void main(){
    //gl_Position（内置变量）：vec4类型，表示顶点着色器中顶点位置，赋值后openGL就知道要画的形状了
    gl_Position = vertexCoordinate;
    //矩阵运算，位置不能交换
    //疑问：这里代表什么意思不清楚
    coordinate = (transformMatrix * textureCoordinate).xy;
}