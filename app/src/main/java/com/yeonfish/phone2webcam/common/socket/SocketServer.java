package com.yeonfish.phone2webcam.common.socket;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.yeonfish.phone2webcam.databinding.ActivityHomeBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SocketServer {
    private int port;
    private PreviewView view;
    private ServerSocket serverSocket;
    private ActivityHomeBinding binding;
    private ImageCapture imageCapture;
    private volatile byte[] byteArray;
    private volatile boolean isFirst = true;



    public SocketServer(int port, PreviewView view, ImageCapture imageCapture) {
        this.port = port;
        this.view = view;
        this.imageCapture = imageCapture;

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
                Log.d("Socket Event", "데이터 보냄");

                while (true) {
                    new Thread(() -> {
                        imageCapture.takePicture(ContextCompat.getMainExecutor(view.getContext()), new ImageCapture.OnImageCapturedCallback() {
                            @Override
                            @OptIn(markerClass = ExperimentalGetImage.class)
                            public void onCaptureSuccess(@NonNull ImageProxy image) {
                                super.onCaptureSuccess(image);
                                // image to bitmap
                                Bitmap bitmap = imageToBitmap(image.getImage());
                                image.close();
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                byteArray = stream.toByteArray();
                                new Thread(() -> {
                                    try {
                                        outputStream.writeObject(byteArray);
                                        outputStream.flush();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Log.d("Socket Event", "데이터 보냄");
                                }).start();
                            }

                            @Override
                            public void onError(@NonNull final ImageCaptureException exception) {
                                super.onError(exception);
                                exception.printStackTrace();
                            }
                        });
                    }).start();
                    Log.d("Socket Event", "Loop");
                    Thread.sleep(100);
                    if (socket.isClosed()) {
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Bitmap imageToBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        image.close();

        return bitmapImage;
    }
}
