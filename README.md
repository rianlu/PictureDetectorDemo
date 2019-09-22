# PictureDetectorDemo
从TensorflowLiteDemo中分离出来的功能模块
官方Demo：<a href="https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection/android">对象检测</a><br>
官方Demo识别数据来自摄像头，该项目把识别模块单独分离出来，用于识别本地的图片以及视频
视频的实现是把视频分解成图片去进行识别，然后再合成新的视频，所以比较慢
