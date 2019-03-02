# AndroidEasyLog [ ![Download](https://api.bintray.com/packages/sheungon/maven/android-ec-log/images/download.svg) ](https://bintray.com/sheungon/maven/android-ec-log/_latestVersion) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/5a7759333fe14d5ba7ce914c3bd08eae)](https://www.codacy.com/app/sheungon/AndroidEasyLog) [![](https://raw.githubusercontent.com/novoda/novoda/master/assets/btn_apache_lisence.png)](LICENSE)

Easily enable/disable logs in your app without changing much in your current codes.<br />
It also shows code line number in the log.<br />
```
01-27 12:18:35.086 15686-15686/com.sotwtm.log.sample D/Log: <15686>[(MainActivity.kt:96)#onClickLog] Clicked at : Fri Jan 27 12:18:35 GMT+08:00 2017
```

## Adding to project
####Gradle
```gradle
implementation 'com.sotwtm.util:ec-log:0.3.0'
```

## Usage
### Enable/Disable Log with log level
```kotlin
// Show all logs
Log.logLevel = Log.VERBOSE
// Hide all logs
Log.logLevel = Log.NONE
```

### Enable log for debug version only
Set the following in your application class onCreate method before any log.
```kotlin
//...
import com.sotwtm.util.Log

class M88Application : Application() {

    override fun onCreate() {
        //...
        Log.logLevel = if (BuildConfig.DEBUG) Log.VERBOSE else Log.NONE
        //...
    }
    //...
}
```

### How to apply it to my existing project?
Simplely replace with the following
```kotlin
import android.util.Log
```
with
```kotlin
import com.sotwtm.util.Log
```

### More settings
#### Set default log TAG for all logs
We have all log methods taking one parameter (the log) only.<br />
This can ease your pain on logging things.<br />
To let multiple log TAG available, the android.util.Log 's two parameters methods is still there.
```kotlin
Log.defaultLogTag = "MyLogTag"
// Then, the following will be equivalent
Log.d("This is log")
android.util.Log("MyLogTag", "This is log")
```
#### Set action on logging wtf (What a Terrible Failure)
```kotlin
// This is the aciton will be taken on release build
Log.actionOnWtf = object : Log.OnWtfListener { ... }
// This is the action will be taken on debug build
Log.actionOnWtfDebug = object : Log.OnWtfListener { ... }
```

### Notes
Disable log does not mean the log method won't be called.<br />
So, for some logs that could take time. It is still recommended to skip the call to log.
```kotlin
if (Log.isDebuggable) {
   // Do only if the app is debuggable (loggable)
}
```
OR by removing them by prograud
```
-assumenosideeffects class com.sotwtm.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}
```
Reference, [Stackoverflow - Android Remove All Logging Calls](http://stackoverflow.com/questions/2446248/remove-all-debug-logging-calls-before-publishing-are-there-tools-to-do-this)

## LICENSE
[Apache License, version 2.0](LICENSE)
