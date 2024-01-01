package com.yeonfish.phone2webcam.common.streaming;

import java.io.IOException;

public interface StreamEvent {

    public void OnNewCapture(StreamEvent streamEvent);
    public void SendImg(String host, byte[] byteArray);
}
