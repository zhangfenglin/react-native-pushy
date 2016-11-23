package cn.reactnative.modules.update;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by maliang on 16/11/18.
 * NetListener for okhttp
 * return response to the main thread
 */

public class NetCallback implements Callback {
    private static final int SUCCESS = 0x01;
    private static final int FAILED = 0x02;

    private Handler handler = new UIHandler(this);

    @Override
    public void onFailure(Call call, IOException e) {
        Message message = Message.obtain();
        message.what = FAILED;
        message.obj = e;
        handler.sendMessage(message);
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        Message message = Message.obtain();
        if (response.isSuccessful()) {
            message.what = SUCCESS;
            message.obj = response.body().string();
        } else {
            message.what = FAILED;
        }

        handler.sendMessage(message);
    }

    //主线程回调
    static class UIHandler extends Handler {
        private WeakReference mWeakReference;

        public UIHandler(NetCallback netCallback) {
            super(Looper.getMainLooper());
            mWeakReference = new WeakReference(netCallback);
        }

        @Override
        public void handleMessage(Message msg) {
            NetCallback netCallback = (NetCallback) mWeakReference.get();
            if (msg.what == SUCCESS) {
                if (netCallback != null)
                    netCallback.onResponse(msg.obj.toString());
            }

            if (msg.what == FAILED) {
                IOException ioException = (IOException) msg.obj;
                if (netCallback != null)
                    netCallback.onFailure(ioException);
            }
        }
    }

    public void onResponse(String response) {

    }

    public void onFailure(IOException e) {

    }
}
