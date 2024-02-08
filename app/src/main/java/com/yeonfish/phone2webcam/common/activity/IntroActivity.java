package com.yeonfish.phone2webcam.common.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yeonfish.phone2webcam.R;
import com.yeonfish.phone2webcam.common.util.RouterUtil;

public class IntroActivity extends BaseActivity {

    private static final String TAG = "IntroActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;

    private View imgSplash;
    private String notificationUrl;

    private static final int FULL_SCREEN_SETTING = View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        getWindow().getDecorView().setSystemUiVisibility(FULL_SCREEN_SETTING);

        imgSplash = findViewById(R.id.imgSplash);
        imgSplash.setVisibility(View.INVISIBLE);

//        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
//            @Override
//            public void onComplete(@NonNull Task<String> task) {
//                if (!task.isSuccessful()) {
//                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
//                    return;
//                }
//
//                String token = task.getResult();
//                Log.d("PushService", "FCM_token: " + token);
//
//                sendRegistrationInfo(token);
//            }
//        });
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
//            resume();
//        } else {
//            requestNotificationPermission();
//        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            resume();
        }
    }


//    private void sendRegistrationInfo(String token) {
//        String url = "http://lyj.kr:17001/register";
//        JSONObject json = new JSONObject();
//        try {
//            json.put("token", token);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));
//        Request request = new Request.Builder()
//                .url(url)
//                .post(requestBody)
//                .build();
//
//        OkHttpClient client = new OkHttpClient();
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                try (Response res = response) {
//                    if (!response.isSuccessful()) {
//                        throw new IOException("Unexpected code " + response);
//                    }
//
//                    String responseBody = res.body().string();
//                    if (responseBody != null) {
//                        Log.i("RES", responseBody);
//                    }
//                }
//            }
//        });
//    }

    private void resume() {
        imgSplash.setAlpha(0f);
        imgSplash.setVisibility(View.VISIBLE);
        imgSplash.animate()
                .setDuration(2000)
                .alpha(1f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        imgSplash.animate().setListener(null);
                        if (notificationUrl != null) {
                            RouterUtil.gotoHome(IntroActivity.this);
                        } else {
                            RouterUtil.gotoHome(IntroActivity.this);
                        }
                    }
                })
                .start();
    }

//    private void requestNotificationPermission() {
//        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
//            Toast.makeText(this, "정상적인 서비스 이용을 위하여 알림을 허용해주세요", Toast.LENGTH_SHORT).show();
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
//        } else {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
//        }
//    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(this, "정상적인 서비스 이용을 위하여 카메라 권한을 허용해주세요", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "알림이 설정되었습니다.", Toast.LENGTH_SHORT).show();
                    resume();
                } else {
                    Toast.makeText(this, "알림권한이 거부된 상태입니다. 설정에 들어가서 별도로 권한을 허용 후, 다시 앱을 시작해주세요.", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }
}