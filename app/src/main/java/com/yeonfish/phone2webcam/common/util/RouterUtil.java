package com.yeonfish.phone2webcam.common.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.yeonfish.phone2webcam.R;
import com.yeonfish.phone2webcam.common.activity.HomeActivity;

public class RouterUtil {

    public static void gotoHome(Context context) {
        gotoWebViewActivity(context);
    }

    public static void gotoWebViewActivity(Context context) {
        if (context != null) {
            Intent intent = new Intent(context, HomeActivity.class);
            context.startActivity(intent);
            applyActivityTransition(context);
        }
    }

    /**
     * GNB 클릭시 트랜지션 처리
     *
     * @param context
     */
    private static void applyActivityTransition(Context context) {
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(R.anim.hold, R.anim.hold);
        }
    }

    private static void applySubActivityTransition(Context context) {
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_scale_down);
        }
    }

    private static void applyPopupActivityTransition(Context context) {
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(R.anim.activity_slide_in_top, R.anim.hold);
        }
    }
}