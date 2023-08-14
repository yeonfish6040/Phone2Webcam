package com.yeonfish.phone2webcam.common.socket;

import java.net.Socket;

public class CustomSocket {
    public Socket socket;
    public String id;

    CustomSocket(String id, Socket socket) {
        this.id = id;
        this.socket = socket;
    }
}
