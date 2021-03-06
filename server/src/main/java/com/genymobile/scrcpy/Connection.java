package com.genymobile.scrcpy;

import com.genymobile.scrcpy.wrappers.ContentProvider;

import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Build;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class Connection implements Device.RotationListener {
    protected final ControlMessageReader reader = new ControlMessageReader();
    protected static final int DEVICE_NAME_FIELD_LENGTH = 64;
    protected StreamInvalidateListener streamInvalidateListener;
    protected Device device;
    protected VideoSettings videoSettings;
    protected Options options;
    protected Controller controller;
    protected ScreenEncoder screenEncoder;

    abstract void send(ByteBuffer data) throws IOException;

    abstract void sendDeviceMessage(DeviceMessage msg) throws IOException;

    abstract void close() throws Exception;

    abstract boolean hasConnections();

    public Connection(Options options, VideoSettings videoSettings) throws IOException {
        Ln.i("Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")");
        this.videoSettings = videoSettings;
        this.options = options;
        device = new Device(options, videoSettings);
        device.setRotationListener(this);
        controller = new Controller(device, this);
        startDeviceMessageSender(controller.getSender());
        device.setClipboardListener(new Device.ClipboardListener() {
            @Override
            public void onClipboardTextChanged(String text) {
                controller.getSender().pushClipboardText(text);
            }
        });

        boolean mustDisableShowTouchesOnCleanUp = false;
        int restoreStayOn = -1;

        if (options.getShowTouches() || options.getStayAwake()) {
            try (ContentProvider settings = device.createSettingsProvider()) {
                if (options.getShowTouches()) {
                    String oldValue = settings.getAndPutValue(ContentProvider.TABLE_SYSTEM, "show_touches", "1");
                    // If "show touches" was disabled, it must be disabled back on clean up
                    mustDisableShowTouchesOnCleanUp = !"1".equals(oldValue);
                }

                if (options.getStayAwake()) {
                    int stayOn = BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB | BatteryManager.BATTERY_PLUGGED_WIRELESS;
                    String oldValue = settings.getAndPutValue(ContentProvider.TABLE_GLOBAL, "stay_on_while_plugged_in", String.valueOf(stayOn));
                    try {
                        restoreStayOn = Integer.parseInt(oldValue);
                        if (restoreStayOn == stayOn) {
                            // No need to restore
                            restoreStayOn = -1;
                        }
                    } catch (NumberFormatException e) {
                        restoreStayOn = 0;
                    }
                }
            }
        }
        CleanUp.configure(mustDisableShowTouchesOnCleanUp, restoreStayOn, true);
    }

    public void setVideoSettings(byte[] bytes) {
        ByteBuffer data = ByteBuffer.wrap(bytes);
        int bitRate = data.getInt();
        int maxFps = data.getInt();
        byte iFrameInterval = data.get();
        int width = data.getShort();
        int height = data.getShort();
        int left = data.getShort();
        int top = data.getShort();
        int right = data.getShort();
        int bottom = data.getShort();
        boolean sendMetaFrame = data.get() != 0;
        int lockedVideoOrientation = data.get();
        int displayId = data.getInt();
        if (data.remaining() > 0) {
            int codecOptionsLength = data.getInt();
            if (codecOptionsLength > 0) {
                byte[] textBuffer = new byte[codecOptionsLength];
                data.get(textBuffer, 0, codecOptionsLength);
                String codecOptions = new String(textBuffer, 0, codecOptionsLength, StandardCharsets.UTF_8);
                if (!codecOptions.isEmpty()) {
                    videoSettings.setCodecOptions(codecOptions);
                }
            }
        }
        if (data.remaining() > 0) {
            int encoderNameLength = data.getInt();
            if (encoderNameLength > 0) {
                byte[] textBuffer = new byte[encoderNameLength];
                data.get(textBuffer, 0, encoderNameLength);
                String encoderName = new String(textBuffer, 0, encoderNameLength, StandardCharsets.UTF_8);
                if (!encoderName.isEmpty()) {
                    videoSettings.setEncoderName(encoderName);
                }
            }
        }
        videoSettings.setBitRate(bitRate);
        videoSettings.setMaxFps(maxFps);
        videoSettings.setIFrameInterval(iFrameInterval);
        videoSettings.setBounds(width, height);
        if (left == 0 && right == 0 && top == 0 && bottom == 0) {
            videoSettings.setCrop(null);
        } else {
            videoSettings.setCrop(new Rect(left, top, right, bottom));
        }
        videoSettings.setSendFrameMeta(sendMetaFrame);
        videoSettings.setLockedVideoOrientation(lockedVideoOrientation);
        if (displayId > 0) {
            videoSettings.setDisplayId(displayId);
        }
        device.applyNewVideoSetting(videoSettings);
        if (this.streamInvalidateListener != null) {
            streamInvalidateListener.onStreamInvalidate();
        }
    }

    public void setStreamInvalidateListener(StreamInvalidateListener listener) {
        this.streamInvalidateListener = listener;
    }

    public interface StreamInvalidateListener {
        void onStreamInvalidate();
    }

    public void onRotationChanged(int rotation) {
        if (this.streamInvalidateListener != null) {
            streamInvalidateListener.onStreamInvalidate();
        }
    }


    private static void startDeviceMessageSender(final DeviceMessageSender sender) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sender.loop();
                } catch (IOException | InterruptedException e) {
                    // this is expected on close
                    Ln.d("Device message sender stopped");
                }
            }
        }).start();
    }
}
