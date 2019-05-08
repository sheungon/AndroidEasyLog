package com.sotwtm.log.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.FileObserver
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.sotwtm.log.sample.databinding.ActivityMainBinding
import com.sotwtm.util.ECLogcatUtil
import com.sotwtm.util.Log
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUIRED_PERMISSIONS = Manifest.permission.WRITE_EXTERNAL_STORAGE
        private const val REQUEST_PERMISSION = 1000
    }

    private var mBinding: ActivityMainBinding? = null
    private var mLogObserver: MyFileObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logPath = ApplicationClass.instance
            .logFile
            .absolutePath

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding?.activity = this
        mBinding?.logPath = logPath

        mLogObserver = MyFileObserver(
            this,
            logPath,
            FileObserver.MODIFY or FileObserver.DELETE or FileObserver.CLOSE_WRITE or FileObserver.CREATE
        )
        mLogObserver?.startWatching()

        updateLogView()
    }

    override fun onPostResume() {
        super.onPostResume()

        val permissionCheck = ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSIONS
        )
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_CONTACTS
                )
            ) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, R.string.msg_needed_external_access, Toast.LENGTH_LONG)
                    .show()

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(REQUIRED_PERMISSIONS),
                    REQUEST_PERMISSION
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mLogObserver?.stopWatching()
        mLogObserver = null

        mBinding?.unbind()
        mBinding = null
    }

    fun onClickStart() {
        ECLogcatUtil.getInstance(application).startLogcat()
        mLogObserver?.stopWatching()
        mLogObserver?.startWatching()
    }

    fun onClickStop() {
        ECLogcatUtil.getInstance(application).stopLogcat()
    }

    fun onClickClear() {
        ECLogcatUtil.getInstance(application).clearLogcat()
        mLogObserver?.stopWatching()
        mLogObserver?.startWatching()
    }

    fun onClickReset() {
        ECLogcatUtil.getInstance(application).resetLogcat()
        mLogObserver?.stopWatching()
        mLogObserver?.startWatching()
    }

    fun onClickFilter() {

        val binding = mBinding ?: return

        val logcatUtil = ECLogcatUtil.getInstance(application)
        logcatUtil.setFilterLogTag(binding.filterText.text.toString())
        logcatUtil.stopLogcat()
        logcatUtil.startLogcat()
        mLogObserver?.stopWatching()
        mLogObserver?.startWatching()
    }

    fun onClickLog() {
        Log.d("Clicked at : " + Date())
        Test().log()
    }

    private fun updateLogView() {

        val binding = mBinding ?: return

        val logFile = ApplicationClass.instance.logFile

        if (!logFile.isFile ||
            !logFile.canRead()
        ) {
            if (!logFile.isFile) {
                // Log file not yet created
                binding.setLog("")
            } else {
                binding.setLog(getString(R.string.error_read_log))
            }
            return
        }

        val logContentBuilder = StringBuilder()
        logFile.readTextFile(logContentBuilder)
        binding.setLog(logContentBuilder.toString())

        // Force log scroll view to the bottom
        binding.scrollViewLog.postDelayed({ binding.scrollViewLog.fullScroll(View.FOCUS_DOWN) }, 200)
    }


    ///////////////////////////
    // Class and interface
    ///////////////////////////
    private class MyFileObserver(
        activity: MainActivity,
        path: String,
        mask: Int
    ) : FileObserver(path, mask) {

        private val mActivityRef: WeakReference<MainActivity> = WeakReference(activity)
        private val mUpdateLogTask = Runnable {
            mActivityRef.get()?.updateLogView()
        }

        override fun onEvent(event: Int, path: String?) {

            val activity = mActivityRef.get() ?: return

            when (event) {
                FileObserver.MODIFY, FileObserver.CLOSE_WRITE, FileObserver.DELETE, FileObserver.DELETE_SELF -> activity.runOnUiThread(
                    mUpdateLogTask
                )
            }
        }
    }

    private class Test {
        fun log() {
            Log.d("Clicked at : " + Date())
        }
    }
}
