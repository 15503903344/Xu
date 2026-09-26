package com.surexu.sesame.hook;

import android.content.Context;
import android.os.Handler;

import com.surexu.sesame.model.normal.base.BaseModel;
import com.surexu.sesame.util.Log;

public class Toast {
    private static final String TAG = Toast.class.getSimpleName();

    public static void show(CharSequence cs) {
        show(cs, false);
    }

    public static void show(CharSequence cs, boolean force) {
        Context context = ApplicationHook.getContext();
        if (context != null && (force || com.surexu.sesame.data.AppConfig.INSTANCE.getShowToast())) {
            show(context, ApplicationHook.getMainHandler(), cs);
        }
    }

    public static void show(Context context, Handler handler, CharSequence cs) {
        try {
            handler.post(() -> {
                try {
                    android.widget.Toast toast = android.widget.Toast.makeText(context, cs, android.widget.Toast.LENGTH_SHORT);
                    toast.setGravity(toast.getGravity(), toast.getXOffset(), com.surexu.sesame.data.AppConfig.INSTANCE.getToastOffsetY());
                    toast.show();
                } catch (Throwable t) {
                    Log.err(TAG, "show.run err:", t);
                }
            });
        } catch (Throwable t) {
            Log.err(TAG, "show err:", t);
        }
    }
}
