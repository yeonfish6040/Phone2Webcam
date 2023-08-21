package com.yeonfish.phone2webcam.common.socket;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class CustomSocket {
    public Socket socket;
    public String id;
    public ObjectOutputStream objectOutputStream;
    public ObjectInputStream objectInputStream;

    CustomSocket(String id, Socket socket, ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
        this.id = id;
        this.socket = socket;
        this.objectOutputStream = objectOutputStream;
        this.objectInputStream = objectInputStream;
    }
}
