package com.yeonfish.phone2webcam.common.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
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
import com.yeonfish.phone2webcam.common.streaming.StreamEvent;
import com.yeonfish.phone2webcam.common.streaming.ClientManager;
import com.yeonfish.phone2webcam.common.streaming.UDPServer;
import com.yeonfish.phone2webcam.databinding.ActivityHomeBinding;

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
    private UDPServer udpServer;
    public List<String> clients;
    private ImageReader reader;


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
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                } finally {
                    isChanging = false;
                }
            }
        });
        binding.seekBar.setMin(0);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {
                try {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                    float zoomRatio = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) / seekBar.getMax() * (float) progress;

                    float percentage = 1 / characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
                    float current = percentage * zoomRatio;
                    binding.textView.setText(String.valueOf((int) (current * 100)) + "%");
                    zoom.setZoom(captureRequestBuilder, zoomRatio);
                    updatePreview();
                } catch (CameraAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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

        // start server
        try {
            clientManager = new ClientManager(port, imageDimensions.getHeight()*imageDimensions.getWidth(), clients);
            clientManager.startClientRegistering();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
        reader = ImageReader.newInstance(imageDimensions.getWidth(), imageDimensions.getHeight(), ImageFormat.JPEG, 5);

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

                udpServer = new UDPServer(reader, clients);


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
        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                super.onCaptureStarted(session, request, timestamp, frameNumber);
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);

                ((StreamEvent)udpServer).OnNewCapture(clientManager);
            }
        }, null);

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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.hold, R.anim.activity_slide_out_bottom);
    }
}