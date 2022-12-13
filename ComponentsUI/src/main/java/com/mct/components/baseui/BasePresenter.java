package com.mct.components.baseui;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BasePresenter {

    private static final String TAG = "MCT_B_Presenter";

    private BaseView baseView;
    private Handler mainHandler;
    private ExecutorService executorService;
    private boolean isRelease;

    public BasePresenter(BaseView baseView) {
        this.baseView = baseView;
    }

    private Handler getMainHandler() {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        return mainHandler;
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }

    protected void exec(Runnable r) {
        if (canExec()) getExecutorService().execute(handleRunnable(r));
    }

    protected void postMain(Runnable r) {
        if (canExec()) getMainHandler().post(handleRunnable(r));
    }

    protected void postMain(Runnable r, long delay) {
        if (canExec()) getMainHandler().postDelayed(handleRunnable(r), delay);
    }

    protected BaseView getBaseView() {
        return baseView;
    }

    @NonNull
    private Runnable handleRunnable(Runnable r) {
        return () -> {
            try {
                r.run();
            } catch (Throwable t) {
                getMainHandler().post(() -> {
                    if (baseView != null) baseView.onFalse(t);
                });
            }
        };
    }

    private boolean canExec() {
        if (isRelease) {
            Log.i(TAG, "Presenter is released!");
            return false;
        }
        return true;
    }

    public boolean isRelease() {
        return isRelease;
    }

    public void release() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        mainHandler = null;
        baseView = null;
        isRelease = true;
    }
}
