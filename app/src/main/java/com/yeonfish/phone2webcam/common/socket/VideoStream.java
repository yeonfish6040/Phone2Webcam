package com.yeonfish.phone2webcam.common.socket;

import android.view.TextureView;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class VideoStream {

    // status
    private boolean isStreaming = false;

    // video
    private TextureView textureView;
    private Thread worker;

    // socket
    private CustomSocket socket;

    public VideoStream(TextureView textureView) {
        this.textureView = textureView;
    }

    public boolean isStreaming() {
        if (worker == null)
            return false;

        return worker.isAlive();
    }

    public boolean isSocketSet() {
        return socket != null;
    }

    public void setSocket(CustomSocket socket) {
        this.socket = socket;
    }

    public void start() {
        worker = new Thread(() -> {
            while (true) {
                if (socket.socket.isClosed()) break;
                if (!socket.socket.isConnected()) continue;
                if (socket.socket.isOutputShutdown()) break;

                ObjectOutputStream out = socket.objectOutputStream;
                ObjectInputStream in = socket.objectInputStream;

                textureView.getBitmap();
            }
        });
    }

}
