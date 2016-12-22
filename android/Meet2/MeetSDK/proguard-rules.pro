# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\software\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-optimizationpasses 5          # 指定代码的压缩级别
-dontusemixedcaseclassnames   # 是否使用大小写混合
-dontpreverify           # 混淆时是否做预校验
-dontskipnonpubliclibraryclasses #指定不去忽略非公共的库的类
-dontskipnonpubliclibraryclassmembers #指定不去忽略非公共的库的类的成员
-verbose                # 混淆时是否记录日志
-dontshrink             # 不去除没有调用的代码

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*  # 混淆时所采用的算法

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses # fix MediaPlayer$setOnErrorListener problem

-keep public class * extends android.app.Activity      # 保持哪些类不被混淆
-keep public class * extends android.app.Application   # 保持哪些类不被混淆
-keep public class * extends android.app.Service       # 保持哪些类不被混淆
-keep public class * extends android.content.BroadcastReceiver  # 保持哪些类不被混淆
-keep public class * extends android.content.ContentProvider    # 保持哪些类不被混淆
-keep public class * extends android.app.backup.BackupAgentHelper # 保持哪些类不被混淆
-keep public class * extends android.preference.Preference        # 保持哪些类不被混淆
-keep public class com.android.vending.licensing.ILicensingService    # 保持哪些类不被混淆

-keepclasseswithmembernames class * {  # 保持 native 方法不被混淆
    native <methods>;
}
-keepclasseswithmembers class * {   # 保持自定义控件类不被混淆
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {# 保持自定义控件类不被混淆
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends android.app.Activity { # 保持自定义控件类不被混淆   
    public void *(android.view.View);
}
-keepclassmembers enum * {     # 保持枚举 enum 类不被混淆    
    public static **[] values();    
    public static ** valueOf(java.lang.String);
}
-keep class * implements android.os.Parcelable { # 保持 Parcelable 不被混淆  
    public static final android.os.Parcelable$Creator *;
}

-keepclasseswithmembers class * {
    void *(**On*Event);
}

-keepclasseswithmembers class * {
    void *(**On*Listener);
	void *(*Listener);
}

#不混淆变量
-keepclassmembers class com.gotye.meetsdk.player.FFMediaPlayer {
	long mNativeContext;
	long mListenerContext;
	long mSurface;
	java.lang.String libPath;
	private void postEventFromNative(java.lang.Object,int,int,int,java.lang.Object);
}
-keepclassmembers class com.gotye.meetsdk.player.XOMediaPlayer {
	public void postEventFromNative(java.lang.Object,int,int,int,java.lang.Object);
}
-keepclassmembers class com.gotye.meetsdk.player.OMXMediaPlayer {
	private void postEventFromNative(java.lang.Object,int,int,int,java.lang.Object);
}
-keepclassmembers class com.gotye.meetsdk.player.MediaExtractable {
    public abstract boolean advance();
    public abstract boolean hasCachedReachedEndOfStream();
    public abstract int readSampleData(java.nio.ByteBuffer,int);
}
-keepclassmembers class com.gotye.meetsdk.player.FFMediaExtractor {
	private long mNativeContext;
	private long mListenerContext;
	public native boolean advance();
	public native boolean hasCachedReachedEndOfStream();
	public native int readSampleData(java.nio.ByteBuffer,int);
	private void postEventFromNative(java.lang.Object,int,int,int,java.lang.Object);
}
-keepclassmembers class com.gotye.meetsdk.subtitle.SimpleSubTitleParser {
	long mNativeContext;
}
#不混淆所有属性与方法
-keepclasseswithmembers class com.gotye.meetsdk.MeetSDK {
     <fields>;
     <methods>;
}
-keepclassmembers class com.gotye.meetsdk.player.MediaPlayer {
	int PLAYER_IMPL_TYPE_*;
	int MEDIA_*;
	int CAN_*;
	int METADATA_*;
	public android.graphics.Bitmap getSnapShot(int,int,int,int);
}
-keepclasseswithmembers class com.gotye.meetsdk.player.MediaInfo {
    <fields>;
    <methods>;
}
-keepclasseswithmembers class com.gotye.meetsdk.player.MediaPlayerInterface {
    <fields>;
    <methods>;
}
-keep class com.gotye.meetsdk.player.MediaPlayerInterface
-keepclasseswithmembers class com.gotye.meetsdk.subtitle.SubTitleParser {
	<methods>;
}
-keepclasseswithmembers class com.gotye.meetsdk.subtitle.SimpleSubTitleParser {
	<methods>;
}
-keepclasseswithmembers class com.gotye.meetsdk.util.BufferedRandomAccessFile {
	void flush();
}
-keep class com.gotye.meetsdk.** {
	public void set*(***);
	public *** get*();
	public ** is*();
}
-keep class com.gotye.meetsdk.player.MediaPlayer {
	public void prepare();
	public ** getSnapShot();
}

-keep class com.gotye.meetsdk.player.MeetSDK$* {*;}
-keep class com.gotye.meetsdk.player.MediaPlayer$* {*;}
-keep class com.gotye.meetsdk.player.MediaController$* {*;}
-keep class com.gotye.meetsdk.subtitle.SubTitleParser$* {*;}
-keep class com.gotye.meetsdk.subtitle.SimpleSubTitleParser$* {*;}
-keepclasseswithmembers class com.gotye.meetsdk.player.TrackInfo {
    <fields>;
    <methods>;
}
-keepclasseswithmembers class com.gotye.meetsdk.player.MediaController {
    <fields>;
    <methods>;
}
-keepclasseswithmembers class com.gotye.meetsdk.player.MeetVideoView {
    <fields>;
    <methods>;
}
-keepclasseswithmembers class com.gotye.meetsdk.player.MeetGLVideoView {
    <fields>;
    <methods>;
}
-keepclasseswithmembers class com.gotye.meetsdk.player.MeetNativeVideoView {
    <fields>;
    <methods>;
}
#不混淆类名
-keep class com.gotye.meetsdk.util.LogUtils
#不混淆方法
-keepclassmembers class com.gotye.meetsdk.util.LogUtils {
	public static void nativeLog(int,java.lang.String,java.lang.String);
}