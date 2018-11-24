[Android分离合成音视频(用MediaExtractor和MediaMuxer)](https://blog.csdn.net/k_bb_666/article/details/79175510)

[MediaExtractor](https://developer.android.google.cn/reference/android/media/MediaExtractor)

1. MediaExtractor和MediaCodec的初认知：

   MediaExtractor：a. 将音视频文件解析出音轨和视轨数据； b.可以获取音/视轨的参数信息(如getTrackFormat()获得mediaFormat后，从mediaFormat中可得到视频的width/height/duration等数据)

   MediaCodec：将音视频文件解码成可以用Surface显示和用AudioTrack播放类型的数据。

private final static String MIME_TYPE = "video/avc"; // H.264 video


2.相机实时预览及播放

ByteBuffer的用法
