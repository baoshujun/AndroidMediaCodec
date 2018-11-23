package com.xx.xxx.androidmediacodec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecodeMP4Activity extends AppCompatActivity implements SurfaceHolder.Callback {
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
                if (mime.startsWith("video/")){
                    extractor.selectTrack(i);
                    try {
                        decoder = MediaCodec.createDecoderByType(mime);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (decoder != null){
                        decoder.configure(mediaFormat,surface,null,0);
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
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                     case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                         Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                         break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                        default:
                            ByteBuffer byteBuffer = outputBuffers[outIndex];
                            Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + byteBuffer);
                            // We use a very simple clock to keep the video FPS, or the video
                            // playback will be too fast
                            while (info.presentationTimeUs/1000>System.currentTimeMillis() -startMs){
                                SystemClock.sleep(10);

                            }

                            decoder.releaseOutputBuffer(outIndex,true);
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
