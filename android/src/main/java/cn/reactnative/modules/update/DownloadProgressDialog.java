package cn.reactnative.modules.update;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadProgressDialog extends Dialog {
    private ProgressBar mProgressBar;
    private TextView tv_title;
    private TextView tv_msg;
    private TextView tv_progress;
    private String title;
    private String msg;
    private int max = 0;

    private boolean canceledOnTouchOutside;

    protected DownloadProgressDialog(Context c) {
        super(c);
    }

    protected DownloadProgressDialog(Context c, boolean canceledOnTouchOutside) {
        super(c);
        this.canceledOnTouchOutside = canceledOnTouchOutside;
    }

    public DownloadProgressDialog(Context c, boolean canceledOnTouchOutside, String title, String msg) {
        super(c);
        this.title = title;
        this.msg = msg;
        this.canceledOnTouchOutside = canceledOnTouchOutside;
    }

    public void setProgress(int prgress) {
        mProgressBar.setProgress(prgress);
        if (max > 0 && prgress <= max) {
            tv_progress.setText(String.format ("%.2f%%",prgress * 100.0 / max));
        }else {
            tv_progress.setText(prgress + "");
        }
    }

    public void setProgressMax(int max) {
        if (mProgressBar != null)
            mProgressBar.setMax(max);

        this.max = max;
    }

    public void setProgressNum(int prgress, String numText) {
        mProgressBar.setProgress(prgress);
        tv_progress.setText(numText);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = getLayoutInflater().inflate(initViewId(), null);
        setContentView(rootView);
        setCancelable(canceledOnTouchOutside);
        setCanceledOnTouchOutside(canceledOnTouchOutside);

        initView(rootView);
        initData();
    }

    protected int initViewId() {
        return R.layout.dialog_download_progress;
    }

    protected void initView(View rootView) {
        tv_title = (TextView) rootView.findViewById(R.id.tv_title);
        tv_msg = (TextView) rootView.findViewById(R.id.tv_msg);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.dialog_progress);
        tv_progress = (TextView) rootView.findViewById(R.id.tv_progress);
    }

    protected void initData() {
        tv_title.setText(title);
        tv_msg.setText(msg);
        mProgressBar.setMax(max);
    }
}
