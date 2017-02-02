# AndroidEasyLog [ ![Download](https://api.bintray.com/packages/sheungon/maven/android-ec-log/images/download.svg) ](https://bintray.com/sheungon/maven/android-ec-log/_latestVersion)  [![](https://raw.githubusercontent.com/novoda/novoda/master/assets/btn_apache_lisence.png)](LICENSE)

Easily enable/disable logs in your app without changing much in your current codes.<br />
It also shows code line number in the log.<br />
```
01-27 12:18:35.086 15686-15686/com.sotwtm.log.sample D/Log: <15686>[(MainActivity.java:96)#onClickLog] Clicked at : Fri Jan 27 12:18:35 GMT+08:00 2017
```

## Adding to project
####Gradle
``` gradle
compile 'com.sotwtm.util:ec-log:0.1.0'
```

## Usage
### Enable/Disable Log
``` java
// Show all logs
Log.setLogLevel(Log.VERBOSE);
// Hide all logs
Log.setLogLevel(Log.NONE);
```

### Enable log for debug version only
Set the following in your application class onCreate method before any log.
``` java
...
import com.sotwtm.util.Log;
...
public class YourApplication extends Application {
    @Override
    public void onCreate() {
        ...
        Log.setLogLevel(BuildConfig.DEBUG ? Log.VERBOSE : Log.NONE);
        ...
    }
    ...
}
```

### How to apply it to my existing project?
Simplely replace with the following
``` java
import android.util.Log;
```
with
```java
import com.sotwtm.util.Log;
```

### More settings
#### Set default log TAG for all logs
We have all log methods taking one parameter (the log) only.<br />
This can ease your pain on logging things.<br />
To let multiple log TAG available, the android.util.Log 's two parameters methods is still there.
``` java
Log.setDefaultLogTag("MyLogTag");
// Then, the following will be equivalent
Log.d("This is log");
android.util.Log("MyLogTag", "This is log");
```
#### Set action on logging wtf (What a Terrible Failure)
``` java
// This is the aciton will be taken on release build
Log.setActionOnWtf(new Log.OnWtfListener() {...});
// This is the action will be taken on debug build
Log.setActionOnWtfDebug(new Log.OnWtfListener() {...});
```

### Notes
Disable log doesn't mean the log method won't be called.<br />
So, for some logs that could take time. It is still recommanded to skip the call to log.
``` java
if (Log.isDebuggable()) {
   // Do only if the app is debuggable (loggable)
}
```


## LICENSE
[Apache License, version 2.0](LICENSE)
