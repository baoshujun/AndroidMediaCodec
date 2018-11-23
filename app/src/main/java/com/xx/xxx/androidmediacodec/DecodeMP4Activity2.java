package com.xx.xxx.androidmediacodec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * MediaExtractor和MediaCodec的初认知：
 * <p>
 * MediaExtractor：a. 将音视频文件解析出音轨和视轨数据； b.可以获取音/视轨的参数信息(如getTrackFormat()
 * 获得mediaFormat后，从mediaFormat中可得到视频的width/height/duration等数据)
 * <p>
 * MediaCodec：将音视频文件解码成可以用Surface显示和用AudioTrack播放类型的数据。
 * <p>
 * MediaExtractor/MediaCodec基本使用方法
 */
public class DecodeMP4Activity2 extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String path = Environment.getExternalStorageDirectory() + "/1080.mp4";
    private PlayerThread mPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mPlayer != null){
            mPlayer.interrupt();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mPlayer == null) {
            mPlayer = new PlayerThread(holder.getSurface());
            mPlayer.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.interrupt();
        }
    }

    private class PlayerThread extends Thread {
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface surface;

        public PlayerThread(Surface surface) {
            this.surface = surface;
        }

        @Override
        public void run() {
            extractor = new MediaExtractor(); //创建对象

            //设置需播放的视频文件路径
            try {
                extractor.setDataSource(path);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //接下来需要做的是，从videoExtractor中找到视轨的id，方法如下：
            int videoTrackIndex = -1;                                //定义trackIndex为视轨的id
            for (int i = 0; i < extractor.getTrackCount(); i++) {  //在videoExtractor的所以Track中遍历，找到视轨的id
                MediaFormat mediaFormat = extractor.getTrackFormat(i);  //获得第i个Track对应的MediaForamt
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);  //再获取该Track对应的KEY_MIME字段
                if (mime.startsWith("video/")) {       //视轨的KEY_MIME是以"video/"开头的，音轨是"audio/"
                    videoTrackIndex = i;
                    break;
                }
            }

            //选择视轨所在的轨道子集(这样在之后调用readSampleData()/getSampleTrackIndex()方法时候，返回的就只是视轨的数据了，其他轨的数据不会被返回)
            extractor.selectTrack(videoTrackIndex);

            //根据视轨id获得对应的MediaForamt
            MediaFormat mediaFormat = extractor.getTrackFormat(videoTrackIndex);

            //从得到的MediaFormat中，可以获取视频的相关信息，视频的长/宽/时长等
            int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            long duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);

            Log.e("DecodeActivity", "width:" + width + " height:" + height + "  duration:" + duration);

            //   在通过MediaExtractor获得需要解码的音轨的id后，就可以创建对应的MediaCodec来解析数据了,参数为MediaFormat类中的MIMETYPE
            try {
                decoder = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
            } catch (IOException e) {
                e.printStackTrace();
            }

            //第一个参数是待解码的数据格式(也可用于编码操作);第二个参数是设置surface，用来在其上绘制解码器解码出的数据；
            // 第三个参数于数据加密有关；第四个参数上1表示编码器，0是否表示解码器呢？？
            if (decoder != null) {
                decoder.configure(mediaFormat, surface, null, 0);
            } else {
                return;
            }

            //当configure好后，就可以调用start()方法来请求向MediaCodec的inputBuffer中写入数据了
            decoder.start();

//            知识点：MediaCodec类中有三个方法与数据读写有关：queueInputBuffer()/dequeueInputBuffer()/dequeueOutputBuffer()
//            MediaCodec中有维护这两个缓冲区，分别存放的是向MediaCodec中写入的数据，和经MediaCodec解码后写出的数据
//            dequeueInputBuffer(): Returns the index of an input buffer to be filled with valid data
//            dequeueOutputBuffer():Returns the index of an output buffer that has been successfully decoded
//            queueInputBuffer(): After filling a range of the input buffer at the specified index submit it to the component

            //   接下来要做到就是，向MediaCodec的inputBuffer中写入数据，而数据就是来自上面MediaExtractor中解析出的Track，代码如下：
            //1.获取MediaCodec中等待数据写入的ByteBuffer的集合,大概有10个ByteBuffer
            ByteBuffer[] byteBuffers = decoder.getInputBuffers();

            boolean isMediaEOS = false;

            while (!Thread.interrupted()) {

                if (!isMediaEOS) {
                    //            上面这个方法获取的是整个待写入数据的ByteBuffer的集合，在MediaExtractor向MediaCodec中写入数据的过程中，需要判断哪些ByteBuffer
//            是可用的，这就可以通过dequeueInputBuffer得到。
                    int inIndex = decoder.dequeueInputBuffer(10000);

                    if (inIndex >= 0) {  //返回的inputBufferIndex为-1，说明暂无可用的ByteBuffer
                        //有可以就从inputBuffers中拿出那个可用的ByteBuffer的对象
                        ByteBuffer byteBuffer = byteBuffers[inIndex];
                        //把MediaExtractor中的数据写入到这个可用的ByteBuffer对象中去，返回值为-1表示MediaExtractor中数据已全部读完
                        int sampleSize = extractor.readSampleData(byteBuffer, 0);
                        if (sampleSize >= 0) {
                            //将已写入数据的id为inputBufferIndex的ByteBuffer提交给MediaCodec进行解码
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            //在MediaExtractor执行完一次readSampleData方法后，需要调用advance()去跳到下一个sample，然后再次读取数据
                            extractor.advance();
                        } else {
                            isMediaEOS = true;
                            decoder.queueInputBuffer(inIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }

                    }
                }


             //   最后一步的作用容易被忽视掉，但是如果没有的话，也会导致视频无法播放出来，代码如下：

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                int outIndex = decoder.dequeueOutputBuffer(bufferInfo,10000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        break;

                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        break;
                        default:

                            //将该ByteBuffer释放掉，以供缓冲区的循环使用。如果没有这一步的话，会导致上面返回的inputBufferIndex一直为-1，使数据读写操作无法进行下去。
                            // 如果在configure中配置了surface，则首先将缓冲区中数据发送给surface，surface一旦不再使用，就将缓冲区释放给MediaCodec
                            //注意：本段的数据读写操作，应该是循环的。它的中断/结束条件是：a.停止播放视频    b.视频播放结束(结束的标志可从sampleSize是否为-1来判断)

                            decoder.releaseOutputBuffer(outIndex,true);
                }


                // All decoded frames have been rendered, we can stop playing now
                if ((bufferInfo.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }

            }

            if (decoder != null){
                decoder.stop();
                decoder.release();
                extractor.release();
            }

        }

    }
}
