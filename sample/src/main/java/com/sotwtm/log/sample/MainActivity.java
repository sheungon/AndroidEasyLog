package com.sotwtm.log.sample;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.FileObserver;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.sotwtm.log.sample.databinding.ActivityMainBinding;
import com.sotwtm.util.ECLogcatUtil;
import com.sotwtm.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;
    private MyFileObserver mLogObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String logPath = ApplicationClass.getInstance()
                .getLogFile()
                .getAbsolutePath();

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setActivity(this);
        mBinding.setLogPath(logPath);

        mLogObserver = new MyFileObserver(this,
                logPath,
                FileObserver.MODIFY | FileObserver.DELETE | FileObserver.CLOSE_WRITE | FileObserver.CREATE);
        mLogObserver.startWatching();

        updateLogView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mLogObserver != null) {
            mLogObserver.stopWatching();
            mLogObserver = null;
        }

        if (mBinding != null) {
            mBinding.unbind();
            mBinding = null;
        }
    }

    public void onClickStart() {
        ECLogcatUtil.getInstance(this).startLogcat();
        mLogObserver.stopWatching();
        mLogObserver.startWatching();
    }

    public void onClickStop() {
        ECLogcatUtil.getInstance(this).stopLogcat();
    }

    public void onClickClear() {
        ECLogcatUtil.getInstance(this).clearLogcat();
        mLogObserver.stopWatching();
        mLogObserver.startWatching();
    }

    public void onClickReset() {
        ECLogcatUtil.getInstance(this).resetLogcat();
        mLogObserver.stopWatching();
        mLogObserver.startWatching();
    }

    public void onClickFilter() {

        final ActivityMainBinding binding = mBinding;
        if (binding == null) {
            return;
        }

        ECLogcatUtil logcatUtil = ECLogcatUtil.getInstance(this);
        logcatUtil.setFilterLogTag(binding.filterText.getText().toString());
        logcatUtil.stopLogcat();
        logcatUtil.startLogcat();
        mLogObserver.stopWatching();
        mLogObserver.startWatching();
    }

    public void onClickLog() {
        Log.d("Clicked at : " + new Date());
    }

    private void updateLogView() {

        final ActivityMainBinding binding = mBinding;
        if (binding == null) {
            return;
        }

        File logFile = ApplicationClass.getInstance().getLogFile();

        if (logFile == null ||
                !logFile.isFile() ||
                !logFile.canRead()) {
            if (logFile != null &&
                    !logFile.isFile()) {
                // Log file not yet created
                binding.setLog("");
            } else {
                binding.setLog(getString(R.string.error_read_log));
            }
            return;
        }

        StringBuilder logContentBuilder = new StringBuilder();
        FileUtil.readTextFile(logFile, logContentBuilder);
        binding.setLog(logContentBuilder.toString());

        // Force log scroll view to the bottom
        binding.scrollViewLog.postDelayed(new Runnable() {
            @Override
            public void run() {
                binding.scrollViewLog.fullScroll(View.FOCUS_DOWN);
            }
        }, 200);
    }


    ///////////////////////////
    // Class and interface
    ///////////////////////////
    private static class MyFileObserver extends FileObserver {

        private final WeakReference<MainActivity> mActivityRef;
        private final Runnable mUpdateLogTask = new Runnable() {
            @Override
            public void run() {
                MainActivity activity = mActivityRef.get();
                if (activity == null) {
                    return;
                }
                activity.updateLogView();
            }
        };

        public MyFileObserver(@NonNull MainActivity activity,
                              @NonNull String path,
                              int mask) {
            super(path, mask);

            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onEvent(int event, String path) {

            MainActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }

            switch (event) {
                case FileObserver.MODIFY:
                case FileObserver.CLOSE_WRITE:
                case FileObserver.DELETE:
                case FileObserver.DELETE_SELF:
                    activity.runOnUiThread(mUpdateLogTask);
                    break;
            }
        }
    }
}
