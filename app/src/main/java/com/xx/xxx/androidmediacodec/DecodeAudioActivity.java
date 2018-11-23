package com.xx.xxx.androidmediacodec;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
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

public class DecodeAudioActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/1080.mp4";
    private PlayerThread mPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);
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
        private  int audioInputBufferSize;
        private AudioTrack audioTrack;

        public PlayerThread(Surface surface) {
            this.surface = surface;
        }

        @Override
        public void run() {
            extractor = new MediaExtractor();
            try {
                extractor.setDataSource(SAMPLE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int i = 0;i< extractor.getTrackCount();i++){
                MediaFormat mediaFormat = extractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")){
                    extractor.selectTrack(i);
                    int audioChannel = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int audioSample = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int minBufferSize = AudioTrack.getMinBufferSize(audioSample,audioChannel == 1? AudioFormat.CHANNEL_OUT_MONO:AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);

                    int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    audioInputBufferSize = minBufferSize > 0?minBufferSize * 4:maxInputSize;
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,audioSample,(audioChannel == 1 ?
                            AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO), AudioFormat.ENCODING_PCM_16BIT,audioInputBufferSize,AudioTrack.MODE_STREAM);

                    audioTrack.play();




                    try {
                        decoder = MediaCodec.createDecoderByType(mime);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (decoder != null){
                        decoder.configure(mediaFormat,null,null,0);  //因为是音轨，所以第二参数为null
                    }
                    break;
                }
            }

            if (decoder == null){
                Log.e("DecodeActivity", "Can't find video info!");
                return;
            }

            decoder.start();

            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isEOS = false;
            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()){
                if (!isEOS){
                    int inIndex = decoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0){
                        ByteBuffer byteBuffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(byteBuffer,0);
                        if (sampleSize >0){
                            decoder.queueInputBuffer(inIndex,0,sampleSize,extractor.getSampleTime(),0);
                            extractor.advance();
                        } else {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to decoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            decoder.queueInputBuffer(inIndex,0,0,0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info,10000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        //注意:当dequeueOutputBuffer返回的id为-3时，说明缓存区发生了改变，必须重新通过getOutputBuffers来获得新的outputBuffer对象，不能再使用之前创建的了。
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                     case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                         Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                         break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                        default:
                            //1. 视频可以直接显示在Surface上，音频需要获取pcm所在的ByteBuffer
                            ByteBuffer byteBuffer = outputBuffers[outIndex];

                            byte[] tempBuffer = new byte[byteBuffer.limit()];
                            byteBuffer.position(0);
                            byteBuffer.get(tempBuffer, 0, byteBuffer.limit());      //2.将保存在ByteBuffer的数据，转到临时的tempBuffer字节数组中去
                            byteBuffer.clear();

                            if (audioTrack != null)
                                audioTrack.write(tempBuffer, 0, tempBuffer.length);      //3.最后写到AudioTrack中


                            Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + byteBuffer);
                            // We use a very simple clock to keep the video FPS, or the video
                            // playback will be too fast
                            while (info.presentationTimeUs/1000>System.currentTimeMillis() -startMs){
                                SystemClock.sleep(10);

                            }

                            decoder.releaseOutputBuffer(outIndex,false);//4. 释放该id的ByteBuffer
                            break;
                }
                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }


            }

            decoder.stop();
            decoder.release();
            extractor.release();


        }

    }
}
