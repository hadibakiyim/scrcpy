package com.genymobile.scrcpy;

import android.graphics.Rect;

import java.nio.ByteBuffer;
import java.util.List;

public class VideoSettings {
    private static final int DEFAULT_BIT_RATE = 8000000;
    private static final byte DEFAULT_MAX_FPS = 60;
    private static final byte DEFAULT_I_FRAME_INTERVAL = 10; // seconds

    private Size bounds;
    private int bitRate = DEFAULT_BIT_RATE;
    private int maxFps;
    private int lockedVideoOrientation;
    private byte iFrameInterval = DEFAULT_I_FRAME_INTERVAL;
    private Rect crop;
    private boolean sendFrameMeta; // send PTS so that the client may record properly
    private List<CodecOption> codecOptions;
    private String encoderName;

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getIFrameInterval() {
        return iFrameInterval;
    }

    public void setIFrameInterval(byte iFrameInterval) {
        this.iFrameInterval = iFrameInterval;
    };

    public Rect getCrop() {
        return crop;
    }

    public void setCrop(Rect crop) {
        this.crop = crop;
    }

    public boolean getSendFrameMeta() {
        return sendFrameMeta;
    }

    public void setSendFrameMeta(boolean sendFrameMeta) {
        this.sendFrameMeta = sendFrameMeta;
    }

    public int getMaxFps() {
        return maxFps;
    }

    public void setMaxFps(int maxFps) {
        this.maxFps = maxFps;
    }

    public int getLockedVideoOrientation() {
        return lockedVideoOrientation;
    }

    public void setLockedVideoOrientation(int lockedVideoOrientation) {
        this.lockedVideoOrientation = lockedVideoOrientation;
    }

    public Size getBounds() {
        return bounds;
    }

    public void setBounds(Size bounds) {
        this.bounds = bounds;
    }

    public void setBounds(int width, int height) {
        this.bounds = new Size(width  & ~15, height & ~15); // multiple of 16
    }

    public List<CodecOption> getCodecOptions() {
        return codecOptions;
    }

    public void setCodecOptions(List<CodecOption> codecOptions) {
        this.codecOptions = codecOptions;
    }

    public String getEncoderName() {
        return this.encoderName;
    }

    public void setEncoderName(String encoderName) {
        this.encoderName = encoderName;
    }

    public byte[] toByteArray() {
        ByteBuffer temp = ByteBuffer.allocate(23);
        temp.putInt(bitRate);
        temp.putInt(maxFps);
        temp.put(iFrameInterval);
        int width = 0;
        int height = 0;
        if (bounds != null) {
            width = bounds.getWidth();
            height = bounds.getHeight();
        }
        temp.putShort((short) width);
        temp.putShort((short) height);
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        if (crop != null) {
            left = crop.left;
            top = crop.top;
            right = crop.right;
            bottom = crop.bottom;
        }
        temp.putShort((short) left);
        temp.putShort((short) top);
        temp.putShort((short) right);
        temp.putShort((short) bottom);
        temp.put((byte) (sendFrameMeta ? 1 : 0));
        temp.put((byte) lockedVideoOrientation);
        return temp.array();
    }

    @Override
    public String toString() {
        return "VideoSettings{"
                + "bitRate=" + bitRate + ", "
                + "maxFps=" + maxFps + ", "
                + "iFrameInterval=" + iFrameInterval + ", "
                + "bounds=" + bounds + ", "
                + "crop=" + crop + ", "
                + "metaFrame=" + sendFrameMeta + ", "
                + "lockedVideoOrientation=" + lockedVideoOrientation
                + "}";
    }

}
