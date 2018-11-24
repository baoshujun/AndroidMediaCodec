package com.xx.xxx.androidmediacodec;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.xx.xxx.androidmediacodec.utils.MediaCodecThread;
import com.xx.xxx.androidmediacodec.utils.MediaCodecUtil;

/**
 *
 *  经过测试，无法播放，蓝屏
 *
 *
 */
public class DecodeMP4Activity3 extends AppCompatActivity  {

    SurfaceView testSurfaceView;

    private SurfaceHolder holder;
    //解码器
    private MediaCodecUtil codecUtil;
    //读取文件解码线程
    private MediaCodecThread thread;
    //文件路径
    private String path = "/mnt/sdcard/720pq.h264";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode_mp4);
        testSurfaceView = findViewById(R.id.surfaceView);
        initSurface();
    }

    //初始化播放相关
    private void initSurface() {
        holder = testSurfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (codecUtil == null) {
                    codecUtil = new MediaCodecUtil(holder);
                    codecUtil.startCodec();
                }
                if (thread == null) {
                    //解码线程第一次初始化
                    thread = new MediaCodecThread(codecUtil, path);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (codecUtil != null) {
                    codecUtil.stopCodec();
                    codecUtil = null;
                }
                if (thread != null && thread.isAlive()) {
                    thread.stopThread();
                    thread = null;
                }
            }
        });
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startPlayer:
                if (thread != null) {
                    thread.start();
                }
                break;
        }
    }
}


