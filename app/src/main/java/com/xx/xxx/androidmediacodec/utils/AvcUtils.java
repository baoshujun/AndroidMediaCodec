package com.xx.xxx.androidmediacodec.utils;

import java.nio.ByteBuffer;

public class AvcUtils {

    public static final int NAL_TYPE_SPS = 1;
    public static final int START_PREFIX_LENGTH = 1;
    public static final int NAL_UNIT_HEADER_LENGTH = 1;
    public static final int NAL_TYPE_PPS = 1;

    public static void parseSPS(byte[] sps_nal, int[] width, int[] height) {
    }

    public static boolean goToPrefix(ByteBuffer byteb) {
        return true;
    }

    public static int getNalType(ByteBuffer byteb) {
        return 1;
    }
}
