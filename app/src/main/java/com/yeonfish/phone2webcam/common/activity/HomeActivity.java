package com.yeonfish.phone2webcam.common.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.core.app.ActivityCompat;

import com.yeonfish.phone2webcam.R;
import com.yeonfish.phone2webcam.common.cameraUtil.Zoom;
import com.yeonfish.phone2webcam.common.image.ImageUtil;
import com.yeonfish.phone2webcam.common.streaming.ClientManager;
import com.yeonfish.phone2webcam.common.streaming.StreamEvent;
import com.yeonfish.phone2webcam.databinding.ActivityHomeBinding;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class HomeActivity extends BaseActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;

    private static final int FULL_SCREEN_SETTING = View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};


    private ActivityHomeBinding binding;

    private int cLens = CameraSelector.LENS_FACING_BACK;

    // Camera2 variables
    private String cameraId;
    private CameraManager manager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimensions;
    private TextureView textureView;
    private Zoom zoom;
    private boolean isChanging = false; // Is camera changing in progress between front/back
    private Context context;

    // streaming variables
    private int port = 19001;
    private ClientManager clientManager;
    public List<String> clients;
    private ImageReader reader;
    private Bitmap bitmapImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().getDecorView().setSystemUiVisibility(FULL_SCREEN_SETTING);
        context = this;

        // camera2
        textureView = binding.cameraView;
        startCamera();

        // streaming
        clients = new ArrayList<>();

        // start server
        try {
            clientManager = new ClientManager(port, clients);
            clientManager.startClientRegistering();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        binding.imageButton8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isChanging) return;
                clientManager.close();
                try {
                    isChanging = true;
                    cameraDevice.close();
                    cLens = cLens == CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
                    openCamera(cLens==CameraSelector.LENS_FACING_BACK?0:1);
                    try {
                        clientManager = new ClientManager(port, clients);
                        clientManager.startClientRegistering();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                } finally {
                    isChanging = false;
                }
            }
        });
        binding.seekBar.setMin(0);
        binding.seekBar.setMax(100);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {
                binding.textView.setText(String.valueOf(binding.seekBar.getProgress())+"%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        binding.seekBar.setProgress(100);
    }



    private void startCamera() {
        textureView.setSurfaceTextureListener(textureListener);
    }

    private void openCamera(int camera) throws CameraAccessException {
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        cameraId = manager.getCameraIdList()[camera];
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        zoom = new Zoom(characteristics);

        imageDimensions = map.getOutputSizes(SurfaceTexture.class)[2];

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            manager.openCamera(cameraId, stateCallback, null);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void createCameraPreview() throws CameraAccessException {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(imageDimensions.getWidth(), imageDimensions.getHeight());
        Surface surface = new Surface(texture);
        reader = ImageReader.newInstance(imageDimensions.getWidth(), imageDimensions.getHeight(), ImageFormat.YUV_420_888, 5);
        reader.setOnImageAvailableListener(imageListener, null);

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG);
        captureRequestBuilder.addTarget(reader.getSurface());
        captureRequestBuilder.addTarget(surface);


        cameraDevice.createCaptureSession(Arrays.asList(surface, reader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if (cameraDevice == null) {
                    return;
                }

                cameraCaptureSession = session;

                try {
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Toast.makeText(getApplicationContext(), "Configuration Changed", Toast.LENGTH_LONG).show();
            }
        }, null);
    }

    private void updatePreview() throws CameraAccessException {
        if (cameraDevice == null) {
            return;
        }

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        if (!cameraCaptureSession.isReprocessable()) {
            cameraCaptureSession.stopRepeating();
        }

        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);

    }

    // 리스너 콜백 함수
    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            try {
                openCamera(cLens==CameraSelector.LENS_FACING_BACK?0:1);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            try {
                createCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private final ImageReader.OnImageAvailableListener imageListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image img = reader.acquireNextImage();
            if (img == null) { return; }
            if (clients.size() == 0 || img.getPlanes()[0] == null) { img.close(); return; }

            byte[] jpeg = ImageUtil.YUV_420_888toJPEG(img);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            bitmapImage = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, binding.seekBar.getProgress(), byteStream);

            byte[] byteArray = byteStream.toByteArray();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (clients.size() != 0) {
                        clients.forEach(host -> {
                            ((StreamEvent)clientManager).SendImg(host, byteArray);
                        });
                        clients.clear();
                    }
                }
            }).start();
        }
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.hold, R.anim.activity_slide_out_bottom);
    }
}