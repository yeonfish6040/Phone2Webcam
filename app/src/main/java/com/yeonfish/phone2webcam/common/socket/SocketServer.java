package com.yeonfish.phone2webcam.common.socket;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.view.PreviewView;

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
    Context context;
    private ImageReader imageReader;
    CameraCaptureSession cameraCaptureSession;
    CaptureRequest.Builder captureRequstBuilder;
    private volatile byte[] byteArray;



    public SocketServer(Context context,int port, CaptureRequest.Builder captureRequstBuilder, CameraCaptureSession cameraCaptureSession, ImageReader imageReader) {
        this.context = context;
        this.port = port;
        this.captureRequstBuilder = captureRequstBuilder;
        this.cameraCaptureSession = cameraCaptureSession;
        this.imageReader = imageReader;

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

                HandlerThread handlerThread = new HandlerThread("CaptureCallbackThread");
                handlerThread.start();
                Handler captureCallbackHandler = new Handler(handlerThread.getLooper());

                captureRequstBuilder.addTarget(imageReader.getSurface());
                cameraCaptureSession.setRepeatingRequest(captureRequstBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);

                        if (socket.isClosed()) {
                            captureRequstBuilder.removeTarget(imageReader.getSurface());
                            try {
                                cameraCaptureSession.setRepeatingRequest(captureRequstBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                throw new RuntimeException(e);
                            }
                            return;
                        }
                        Image image = imageReader.acquireLatestImage();
                        Bitmap bitmap = imageToBitmap(image);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
                        byteArray = stream.toByteArray();
                        try {
                            outputStream.writeObject(byteArray);
                            outputStream.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, captureCallbackHandler);
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
