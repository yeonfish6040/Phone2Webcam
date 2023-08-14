package com.yeonfish.phone2webcam.common.socket;

import java.io.IOException;
import java.net.SocketException;

public interface OnSocketEvent {
    void OnClientConnect(CustomSocket socket) throws SocketException;
//    void OnClientMessage(String s, CustomSocket socket) throws IOException;
}
