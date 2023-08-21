package com.yeonfish.phone2webcam.common.socket;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SocketServer {

    private int port;
    private OnSocketEvent socketEvent;
    private Context context;



    public SocketServer(Context context, int port, OnSocketEvent socketEvent) {
        this.context = context;
        this.port = port;
        this.socketEvent = socketEvent;

        run();
    }

    private void run() {
        try {
            int portNumber = 17101;

            ServerSocket server = new ServerSocket(portNumber);
            Log.d("Started Server Socket", "서버 시작함: "+portNumber);

            Handler mainHandler = new Handler(Looper.getMainLooper());
            while (true) {
                Socket socket = server.accept();
                InetAddress clientHost = socket.getLocalAddress();
                int clientPort = socket.getPort();
                Log.d("Socket Event", "클라이언트 연결됨: "+clientHost+" : "+clientPort);


                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                String s = inputStream.readObject().toString();
                Log.d("Socket Event", "데이터 받음: "+s);

                if (!s.toString().startsWith("Phone2Webcam_ClientConnectRequest_")) {
                    socket.close();
                    continue;
                }

                outputStream.writeObject("OK");
                outputStream.flush();
                Log.d("Socket Event", "Client Connected");

                socketEvent.OnClientConnect(new CustomSocket(s.toString().replace("Phone2Webcam_ClientConnectRequest_", ""), socket, outputStream, inputStream));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Bitmap imageToBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        image.close();

        return bitmapImage;
    }
}
