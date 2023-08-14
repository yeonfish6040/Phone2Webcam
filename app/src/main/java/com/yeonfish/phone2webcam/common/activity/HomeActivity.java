package com.yeonfish.phone2webcam.common.activity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalZeroShutterLag;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.yeonfish.phone2webcam.R;
import com.yeonfish.phone2webcam.common.socket.SocketAdapter;
import com.yeonfish.phone2webcam.common.socket.SocketServer;
import com.yeonfish.phone2webcam.databinding.ActivityHomeBinding;

import java.util.concurrent.ExecutionException;


public class HomeActivity extends BaseActivity {

    private static final int FULL_SCREEN_SETTING = View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE;

    private ActivityHomeBinding binding;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView cameraView;
    private ImageCapture imageCapture;
    private int cLens = CameraSelector.LENS_FACING_BACK;
    private Camera camera = null;
    private SocketAdapter socketAdapter;



    @SuppressLint("RestrictedApi")
    @Override
    @OptIn(markerClass = ExperimentalZeroShutterLag.class)
    protected void  onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // camera2
//        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
//        try {
//            cameraManager.openCamera(cameraManager.getCameraIdList());
//        } catch (CameraAccessException e) {
//            throw new RuntimeException(e);
//        }


        // camerax
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraView = binding.cameraView;

        imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG).build();
        try {
            camera = bindPreview(cameraProviderFuture.get(), cLens);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        new Thread(() -> {
            SocketServer socketServer = new SocketServer(17101, binding.cameraView, imageCapture);
        }).start();

//        binding.socketRecyclerView

        binding.imageButton8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    showLoading();
                    if (cLens == CameraSelector.LENS_FACING_BACK)
                        camera = bindPreview(cameraProviderFuture.get(), CameraSelector.LENS_FACING_FRONT);
                    else camera = bindPreview(cameraProviderFuture.get(), CameraSelector.LENS_FACING_BACK);

                    cLens = cLens==CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_FRONT:CameraSelector.LENS_FACING_BACK;
                    hideLoading();
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        binding.seekBar.setMin(0);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {
                if (camera != null) {
                    CameraControl cameraControl = camera.getCameraControl();

                    float maxZoomRatio = ((CameraInfo) camera.getCameraInfo()).getZoomState().getValue().getMaxZoomRatio();
                    float zoomRatio = maxZoomRatio / seekBar.getMax() * (float) progress;

                    float percentage = 1/maxZoomRatio;
                    float current = percentage*zoomRatio;
                    binding.textView.setText(String.valueOf((int)(current*100))+"%");
                    Log.d("MaxZoom", String.valueOf(maxZoomRatio));
                    cameraControl.setLinearZoom(current);
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

    Camera bindPreview(@NonNull ProcessCameraProvider cameraProvider, int lens) {

        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
            .requireLensFacing(lens)
            .build();


        preview.setSurfaceProvider(cameraView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageCapture, preview);
        return camera;
    }

    private void showLoading() {
        binding.loadingView.setVisibility(View.VISIBLE);
        ((AnimationDrawable) binding.loadingView.getBackground()).start();
    }

    private void hideLoading() {
        binding.loadingView.setVisibility(View.INVISIBLE);
        ((AnimationDrawable) binding.loadingView.getBackground()).stop();
    }

    private boolean appInstalledOrNot(String uri) {
        try {
            getPackageManager().getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.hold, R.anim.activity_slide_out_bottom);
    }
}