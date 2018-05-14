package com.hct.monitorcamera;

/**
 * Created by jiaqing on 2018/1/4.
 */

import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class WorkThreadManager {
    private static Handler mManinHandler;
    private static Object mMainHandlerLock = new Object();
    public static final Executor NETWORK_EXECUTOR = initNetworkExecutor();

    private static Handler SUB_THREAD_HANDLER;
    private static HandlerThread SUB_THREAD;

    //timeout handler
    private static Handler mTimeoutHandler;
    private static HandlerThread mTimeoutHandlerThread;

    //message dispatcher
    public static final Executor MESSAGE_DISPATCH_EXECUTOR = initMessageDispatchExecutor();

    //config handler
    public static final Executor CONFIG_EXECUTOR = initMessageDispatchExecutor();


    private static Executor initNetworkExecutor() {
        Executor result = null;
        if (Build.VERSION.SDK_INT >= 11) {
            result = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        } else {
            Executor tmp = null;
            try {
                Field field = AsyncTask.class.getDeclaredField("sExecutor");
                field.setAccessible(true);
                tmp = (Executor) field.get(null);
            } catch (Exception e) {
                tmp = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue());
            }
            result = tmp;
        }
        if ((result instanceof ThreadPoolExecutor)) {
            ((ThreadPoolExecutor) result).setCorePoolSize(3);
        }
        return result;
    }

    public static void init() {
    }

    public static Handler getMainHandler() {
        if (mManinHandler == null) {
            synchronized (mMainHandlerLock) {
                if (mManinHandler == null) {
                    mManinHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return mManinHandler;
    }

    public static void executeOnNetWorkThread(Runnable run) {
        try {
            NETWORK_EXECUTOR.execute(run);
        } catch (RejectedExecutionException localRejectedExecutionException) {
        }
    }

    public static void executeOnConfigThread(Runnable run) {
        try {
            CONFIG_EXECUTOR.execute(run);
        } catch (RejectedExecutionException localRejectedExecutionException) {
        }
    }

    public static Thread getSubThread() {
        if (SUB_THREAD == null) {
            getSubThreadHandler();
        }
        return SUB_THREAD;
    }

    public static Handler getSubThreadHandler() {
        if (SUB_THREAD_HANDLER == null) {
            synchronized (WorkThreadManager.class) {
                SUB_THREAD = new HandlerThread("QQ_SUB");
                SUB_THREAD.start();
                SUB_THREAD_HANDLER = new Handler(SUB_THREAD.getLooper());
            }
        }
        return SUB_THREAD_HANDLER;
    }

    public static Looper getSubThreadLooper() {
        return getSubThreadHandler().getLooper();
    }

    public static void executeOnSubThread(Runnable run) {
        getSubThreadHandler().post(run);
    }

    //connect task
    private static Executor initConnectExecutor() {
        Executor result = null;
        if (Build.VERSION.SDK_INT >= 11) {
            result = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        } else {
            Executor tmp = null;
            try {
                Field field = AsyncTask.class.getDeclaredField("sExecutor");
                field.setAccessible(true);
                tmp = (Executor) field.get(null);
            } catch (Exception e) {
                tmp = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue());
            }
            result = tmp;
        }
        if ((result instanceof ThreadPoolExecutor)) {
            ((ThreadPoolExecutor) result).setCorePoolSize(3);
        }
        return result;
    }

    //connect task
    private static Executor initMessageDispatchExecutor() {
        Executor result = null;
        if (Build.VERSION.SDK_INT >= 11) {
            result = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        } else {
            Executor tmp = null;
            try {
                Field field = AsyncTask.class.getDeclaredField("sExecutor");
                field.setAccessible(true);
                tmp = (Executor) field.get(null);
            } catch (Exception e) {
                tmp = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue());
            }
            result = tmp;
        }
        if ((result instanceof ThreadPoolExecutor)) {
            ((ThreadPoolExecutor) result).setCorePoolSize(3);
        }
        return result;
    }

    //timeout handler
    public static Handler getTimeoutHandler() {
        if (mTimeoutHandler == null) {
            synchronized (WorkThreadManager.class) {
                mTimeoutHandlerThread = new HandlerThread("TIMEOUT_HANDLER");
                mTimeoutHandlerThread.start();
                mTimeoutHandler = new Handler(mTimeoutHandlerThread.getLooper());
            }
        }
        return mTimeoutHandler;
    }

    public static void executeOnDispatchThread(Runnable run) {
        try {
            MESSAGE_DISPATCH_EXECUTOR.execute(run);
        } catch (RejectedExecutionException localRejectedExecutionException) {
        }
    }
}

