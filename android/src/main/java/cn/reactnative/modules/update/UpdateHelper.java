package cn.reactnative.modules.update;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by zhangfenglin on 16/11/22.
 */

public class UpdateHelper {
    private static final String HOST = "http://update.reactnative.cn/api";
    private static final String TAG = "NativeUpdate";

    private static final int START_UPDATE = 0;
    private static final int SET_CONTENT_LENGTH = 1;
    private static final int UPDATE_PROGRESS = 2;
    private static final int UPDATE_FINISH = 3;

    public interface NativeUpdateListener {
        void finish();
    }

    public static void checkUpdate(String appKey, final UpdateContext updateContext, final NativeUpdateListener listener, final boolean expired){
        if (TextUtils.isEmpty(appKey))
            throw new RuntimeException("AppKey must not be empty.");

        clearCache(updateContext.getContext());

        String url = String.format("%s/checkUpdate/%s", HOST, appKey);
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).connectTimeout(60, TimeUnit.SECONDS).build();

        String currentVersion = updateContext.getCurrentVersion();

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("packageVersion", updateContext.getPackageVersion());
        builder.add("hash", currentVersion != null ? currentVersion : "");

        Headers headers = new Headers.Builder()
                .add("Accept", "application/json")
                .add("Content-Type", "application/json")
                .build();

        Request request = new Request.Builder().url(url)
                .post(builder.build())
                .headers(headers)
                .build();

        final Handler handler = getHandler(updateContext.getContext());

        client.newCall(request).enqueue(new NetCallback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (listener != null) {
                    listener.finish();
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    download(response.body().string(), updateContext, listener, handler, expired);
                } catch (JSONException e) {
                    if (listener != null) {
                        listener.finish();
                    }
                }
            }
        });
    }

    private static Handler getHandler(final Context context) {
        return new Handler(){

            DownloadProgressDialog progressDialog = new DownloadProgressDialog(context);
            int max = 0;
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == START_UPDATE) {
                    progressDialog.show();
                    progressDialog.setProgress(0);
                }else if (msg.what == SET_CONTENT_LENGTH) {
                    progressDialog.setProgressMax(msg.arg1);
                    max = msg.arg1;
                }else if (msg.what == UPDATE_PROGRESS) {
                    progressDialog.setProgressNum(msg.arg1, String.format ("当前下载进度 %.2f%%",msg.arg1 * 100.0 / max));
                }else if (msg.what == UPDATE_FINISH) {
                    progressDialog.dismiss();
                }
            }
        };
    }

    private static void download(String response, final UpdateContext updateContext, final NativeUpdateListener listener, final Handler handler, boolean expired) throws JSONException {
        if (UpdateContext.DEBUG)
            Log.i(TAG, response != null ? response : "");
        final JSONObject json = new JSONObject(response);
        if (!json.optBoolean("update") && !json.optBoolean("expired")) {
            if (listener != null) {
                listener.finish();
            }
            return;
        }

        UpdateContext.DownloadFileListener downloadFileListener = new UpdateContext.DownloadFileListener() {
            @Override
            public void onDownloadCompleted() {
                if (TextUtils.isEmpty(json.optString("downloadUrl"))) {
                    Log.i(TAG, "switchVersion " + json.optString("hash"));
                    updateContext.switchVersion(json.optString("hash"));
                    updateContext.markSuccess();
                }

                handler.sendMessage(handler.obtainMessage(UPDATE_FINISH, 0, 0));
                if (listener != null) {
                    listener.finish();
                }
            }

            @Override
            public void onDownloadFailed(Throwable error) {
                if (listener != null) {
                    listener.finish();
                }
            }

            @Override
            public void onUpdateProgress(long totalRead) {
                handler.sendMessage(handler.obtainMessage(UPDATE_PROGRESS, (int) totalRead, 0));
            }

            @Override
            public void onContentLength(long contentLength) {
                handler.sendMessage(handler.obtainMessage(SET_CONTENT_LENGTH, (int)contentLength, 0));
                handler.sendMessage(handler.obtainMessage(START_UPDATE, 0, 0));
            }


            @Override
            public void onUpdateUnzipProgress(String name) {
                Log.i(TAG, name);
            }
        };

        if (TextUtils.isEmpty(json.optString("diffUrl")) == false) {
            updateContext.downloadPatchFromPpk(json.optString("diffUrl"), json.optString("hash"), updateContext.getCurrentVersion(), downloadFileListener);
        } else if (TextUtils.isEmpty(json.optString("pdiffUrl")) == false) {
            updateContext.downloadPatchFromApk(json.optString("pdiffUrl"), json.optString("hash"), downloadFileListener);
        } else if (expired && json.optBoolean("expired")) {
            updateContext.downloadFromApk(json.optString("downloadUrl"), String.valueOf(System.currentTimeMillis()), downloadFileListener);
        } else if (TextUtils.isEmpty(json.optString("updateUrl")) == false) {
            updateContext.downloadFile(json.optString("updateUrl"), json.optString("hash"), downloadFileListener);
        } else {
            if (listener != null) {
                listener.finish();
            }
        }

    }

    public static void clearCache(Context context) {
        UpdateFileUtil.clearCache(context);
    }
}
