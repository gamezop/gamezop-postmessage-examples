# The method name is part of Gamezop's JavaScript-to-native contract.
-keep class com.gamezop.postmessageexample.bridge.GameEventBridge { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

