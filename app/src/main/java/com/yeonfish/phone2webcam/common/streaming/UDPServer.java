package com.yeonfish.phone2webcam.common.streaming;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class UDPServer implements StreamEvent {

    private List<String> clients;
    private ImageReader reader;

    public UDPServer(ImageReader reader, List<String> clients) {
        this.clients = clients;
        this.reader = reader;
    }

    @Override
    public void OnNewCapture(StreamEvent event) {
        Image img = reader.acquireLatestImage();
        if (img == null || img.getPlanes()[0] == null) {
            return;
        }
        byte[] byteArray = imageToByteArray(img); img.close();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (clients.size() != 0) {
                    clients.forEach(host -> {
                        event.SendImg(host, byteArray);
                    });
                    clients.clear();
                }
            }
        }).start();
    }

    @Override
    public void SendImg(String host, byte[] byteArray) {

    }

    private byte[] imageToByteArray(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        buffer.clear();

        return bytes;
    }
}