package snow.music.service;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.blankj.utilcode.util.CacheDiskStaticUtils;
import com.blankj.utilcode.util.CacheDiskUtils;
import com.blankj.utilcode.util.CacheDoubleStaticUtils;
import com.blankj.utilcode.util.UriUtils;
import com.blankj.utilcode.util.Utils;
import com.tencent.mmkv.MMKV;
import com.zj.jplayercore.controller.JEqparam;
import com.zj.jplayercore.controller.JPlayer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.common.base.Preconditions;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import snow.music.util.GetFilePathFromUri;
import snow.music.util.NightModeUtil;
import snow.player.audio.AbstractMusicPlayer;
import snow.player.audio.ErrorCode;

/**
 * 封装了一个 Jplayer。
 */
public class JMusicPlayer extends AbstractMusicPlayer {
    private static final String TAG = "JMusicPlayer";

    private final Context mContext;
    private final Uri mUri;
    private final String mPath;
    private final Map<String, String> mHeaders;
    private List<HttpCookie> mCookies;
    private static List<JEqparam> jEqparams = new ArrayList<>();

    private JPlayer jplayer;

    @Nullable
    private OnErrorListener mErrorListener;
    @Nullable
    private OnStalledListener mStalledListener;
    @Nullable
    private OnRepeatListener mRepeatListener;
    @Nullable
    private OnCompletionListener mCompletionListener;

    private boolean mStalled;
    private boolean mInvalid;
    private boolean mLooping;

    /**
     * 创建一个 {@link JMusicPlayer} 对象。
     *
     * @param context Context 对象，不能为 null
     * @param uri     要播放的歌曲的 URI，不能为 null
     */
    public JMusicPlayer(@NonNull Context context, @NonNull Uri uri,@NonNull String path) {
        this(context, uri,path, null);
    }

    /**
     * 创建一个 {@link JMusicPlayer} 对象。
     *
     * @param context Context 对象，不能为 null
     * @param uri     要播放的歌曲的 URI，不能为 null
     * @param headers HTTP 首部
     */
    public JMusicPlayer(@NonNull Context context, @NonNull Uri uri,@NonNull String path, @Nullable Map<String, String> headers) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(uri);

        mContext = context;
        mUri = uri;
        mPath = path;
        mHeaders = headers;

        jplayer = new JPlayer();
        mInvalid = false;

//        loadEqparam();
//        setEnabledEffect();
//        setEnabledStereoWidth();
//        setEnabledChafen();
//        setChafenDelay();
//        setStereoWidth();
        setEnabledStereoWidth(isEnabledStereoWidth(mContext));
        setStereoWidth(getStereoWidth(mContext));
        setSampleRate(getSampleRate(mContext));
        setEnabledEffect(isEnabledEffect(mContext));
        setEnabledChafen(isEnabledChafen(mContext));
        setChafenDelay(getChafenDelay(mContext));
        loadEqparam();

        jplayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);

        jplayer.setOnErrorListener(new JPlayer.OnErrorListener() {
            @Override
            public boolean onError(JPlayer mp, int what, int extra) {
                Log.e(TAG, "Jplayer Error[what: " + what + ", extra: " + extra + "]");

                setInvalid();

                if (mErrorListener != null) {
                    mErrorListener.onError(JMusicPlayer.this, toErrorCode(what, extra));
                }
                return true;
            }
        });

        jplayer.setOnInfoListener(new JPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(JPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        //setStalled(true);
                        return true;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        //setStalled(false);
                        return true;
                }

                return false;
            }
        });

        jplayer.setOnCompletionListener(new JPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(JPlayer mp) {
                if (mLooping) {
                    mp.reset();
                    mp.prepare();
                    mp.start();
                    notifyOnRepeat();
                    return;
                }

                notifyOnComplete();
            }
        });
    }

    private void notifyOnRepeat() {
        if (mRepeatListener != null) {
            mRepeatListener.onRepeat(this);
        }
    }

    private void notifyOnComplete() {
        if (mCompletionListener != null) {
            mCompletionListener.onCompletion(this);
        }
    }

    /**
     * 创建一个 {@link JMusicPlayer} 对象。
     *
     * @param context Context 对象，不能为 null
     * @param uri     要播放的歌曲的 URI，不能为 null
     * @param headers HTTP 首部
     * @param cookies HTTP cookies
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public JMusicPlayer(@NonNull Context context, @NonNull Uri uri,@NonNull String path, @Nullable Map<String, String> headers, @Nullable List<HttpCookie> cookies) {
        this(context, uri,path, headers);
        mCookies = cookies;
    }

    private int toErrorCode(int what, int extra) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    return ErrorCode.UNKNOWN_ERROR;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    return ErrorCode.PLAYER_ERROR;
            }
        }

        switch (extra) {
            case MediaPlayer.MEDIA_ERROR_IO:            // 注意！case 穿透！
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                return ErrorCode.DATA_LOAD_FAILED;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:     // 注意！case 穿透！
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
            case -2147483648/*低级系统错误*/:
                return ErrorCode.PLAYER_ERROR;
            default:
                return ErrorCode.UNKNOWN_ERROR;
        }
    }

    private void setStalled(boolean stalled) {
        mStalled = stalled;
        if (mStalledListener != null) {
            mStalledListener.onStalled(mStalled);
        }
    }

    @Override
    public void prepare() throws Exception {
        if (isInvalid()) {
            return;
        }
        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                //jplayer.setDataSource(GetFilePathFromUri.getFileAbsolutePath(mContext,mUri));
////                jplayer.setDataSource(UriUtils.uri2File(mUri).getAbsolutePath());
//                jplayer.setDataSource(mPath);
//            } else {
//                jplayer.setDataSource(mPath);
//            }
            File file = UriUtils.uri2File(mUri);
            if(null == file || !file.exists()){
                new RuntimeException("读取文件有误");
            }

            jplayer.setDataSource(file.getAbsolutePath());
            jplayer.prepare();
            startEx();
        } catch (Exception e) {
            setInvalid();
            throw e;
        }
    }

    @Override
    public void setLooping(boolean looping) {
        mLooping = looping;
    }

    @Override
    public boolean isLooping() {
        return false;
    }

    @Override
    public boolean isStalled() {
        return mStalled;
    }

    @Override
    public boolean isPlaying() {
        return jplayer.isPlaying();
    }

    @Override
    public int getDuration() {
        return jplayer.getDuration();
    }

    @Override
    public int getProgress() {
        return jplayer.getCurrentPosition();
    }

    @Override
    public void startEx() {
        jplayer.start();
    }

    @Override
    public void pauseEx() {
        jplayer.pause();
    }

    @Override
    public void stopEx() {
        jplayer.stop();
    }

    @Override
    public void seekTo(int pos) { jplayer.seekTo(pos);}

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        jplayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void setSpeed(float speed) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        PlaybackParams playbackParams = jplayer.audioTrack.getPlaybackParams();
        playbackParams.setSpeed(speed);
        jplayer.audioTrack.setPlaybackParams(playbackParams);
    }

    @Override
    public void releaseEx() {
        setInvalid();
        if (jplayer != null) {
            jplayer.release();
            jplayer = null;
        }
    }

    @Override
    public synchronized boolean isInvalid() {
        return mInvalid;
    }

    private synchronized void setInvalid() {
        mInvalid = true;
    }

    @Override
    public int getAudioSessionId() {
        return jplayer.getAudioSessionId();
    }

    @Override
    public void setOnPreparedListener(@Nullable final OnPreparedListener listener) {
        if (listener == null) {
            jplayer.setOnPreparedListener(null);
            return;
        }

        jplayer.setOnPreparedListener(new JPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(JPlayer mp) {
                listener.onPrepared(JMusicPlayer.this);
            }
        });
    }

    @Override
    public void setOnCompletionListener(@Nullable OnCompletionListener listener) {
        mCompletionListener = listener;
    }

    @Override
    public void setOnRepeatListener(@Nullable OnRepeatListener listener) {
        mRepeatListener = listener;
    }

    @Override
    public void setOnSeekCompleteListener(@Nullable final OnSeekCompleteListener listener) {
        if (listener == null) {
            jplayer.setOnSeekCompleteListener(null);
            return;
        }

        jplayer.setOnSeekCompleteListener(new JPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(JPlayer mp) {
                listener.onSeekComplete(JMusicPlayer.this);
            }
        });
    }

    @Override
    public void setOnStalledListener(@Nullable final OnStalledListener listener) {
        mStalledListener = listener;
    }

    @Override
    public void setOnBufferingUpdateListener(@Nullable final OnBufferingUpdateListener listener) {
        if (listener == null) {
            jplayer.setOnBufferingUpdateListener(null);
            return;
        }

        jplayer.setOnBufferingUpdateListener(new JPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(JPlayer jPlayer, int i, boolean b) {
                listener.onBufferingUpdate(JMusicPlayer.this, i, b);
            }
        });
    }

    @Override
    public void setOnErrorListener(@Nullable OnErrorListener listener) {
        mErrorListener = listener;
    }

    @Override
    public void setEnabledStereoWidth(boolean enabledStereoWidth){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(mContext));
        mmkv.encode("EnabledStereoWidth", enabledStereoWidth);

        JPlayer.jConfig.setEnabledStereoWidth(enabledStereoWidth);
        Log.d(TAG, "setEnabledStereoWidth: "+enabledStereoWidth);
    }

    @Override
    public void setEnabledEffect(boolean enabledEffect){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(mContext));
        mmkv.encode("EnabledEffect", enabledEffect);

        JPlayer.jConfig.setEnabledEffect(enabledEffect);
        Log.d(TAG, "setEnabledEffect: "+enabledEffect);
    }

    @Override
    public void setSampleRate(int sampleRate){
        if(sampleRate!=44100 && sampleRate != 48000  && sampleRate != 96000 ){
            return;
        }
        jplayer.changeSampleRate(sampleRate);
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(mContext));
        mmkv.encode("SampleRate", sampleRate);
        Log.d(TAG, "setSampleRate: "+sampleRate);
    }

    @Override
    public void setStereoWidth(float stereoWidth){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(mContext));
        mmkv.encode("SetStereoWidth", stereoWidth);

        JPlayer.jConfig.setStereoWidth(stereoWidth);
        Log.d(TAG, "setStereoWidth: "+stereoWidth);
    }

    @Override
    public void setEnabledChafen(boolean enabledChafen){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(mContext));
        mmkv.encode("EnabledChafen", enabledChafen);

        JPlayer.jConfig.setEnabledChafen(enabledChafen);
        Log.d(TAG, "setEnabledChafen: "+enabledChafen);
    }

    @Override
    public void setChafenDelay(int chafenDelay){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(mContext));
        mmkv.encode("SetChafenDelay", chafenDelay);

        JPlayer.jConfig.setChafenDelay(chafenDelay);
        Log.d(TAG, "setChafenDelay: "+chafenDelay);
    }

    @Override
    public void setEnabledCompressor(boolean enabledCompressor){
        Log.d(TAG, "setEnabledCompressor: ");
    }

    @Override
    public void setThreshold(float threshold){
        Log.d(TAG, "setThreshold: ");
    }

    @Override
    public void setRatio(double ratio){
        Log.d(TAG, "setRatio: ");
    }

    @Override
    public void setAttack(double attack){
        Log.d(TAG, "setAttack: ");
    }

    @Override
    public void setReleaseTime(double releaseTime){
        Log.d(TAG, "setReleaseTime: ");
    }

    @Override
    public void setGain(double gain){
        Log.d(TAG, "setGain: ");
    }

    @Override
    public void setAutoGain(boolean autoGain){
        Log.d(TAG, "setAutoGain: ");
    }

    @Override
    public void setDetectionType(String detectionType){
        Log.d(TAG, "setDetectionType: ");
    }

    @Override
    public void setThresholdWidth(int thresholdWidth){
        Log.d(TAG, "setThresholdWidth: ");
    }

    @Override
    public void setEqparam(List<String> eqparam){
        MMKV mmkvFilter = MMKV.mmkvWithID(getMMapId(mContext));
        mmkvFilter.encode("eqparamNmae", eqparam.get(0));
        jEqparams.clear();
        String preCut = "0";
        String[] precut = eqparam.get(eqparam.size()-1).split(" ");
        if(precut[0].equals("precut")){
            preCut=precut[1];
        }
        mmkvFilter.encode("precut", preCut);
        for (String a:eqparam
        ) {
            String[] filter = a.split(" ");
            if(filter.length==5 && filter[0].equals("filter")){
                JEqparam eq1 = new JEqparam();
                eq1.setFreq(Double.parseDouble(filter[2]));
                eq1.setPeak(Double.parseDouble(filter[4]));
                eq1.setQ(Double.parseDouble(filter[3]));
                eq1.setPrecut(Double.parseDouble(preCut));
                jEqparams.add(eq1);
                mmkvFilter.encode(filter[0]+filter[1], a);
            }
        }
        if(jEqparams!=null && jEqparams.size()>0){
            jplayer.loadEQ(jEqparams);
        }
        Log.d(TAG, "setEqparam: "+eqparam);
    }

//    private void setSampleRate(){
//        int sampleRate = getSampleRate(mContext);
//        if(sampleRate!=44100 && sampleRate != 48000  && sampleRate != 96000 ){
//            return;
//        }
//        jplayer.changeSampleRate(sampleRate);
//        Log.d(TAG, "changeSampleRate: "+sampleRate);
//    }

    public static int getSampleRate(@NonNull Context context){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
        return mmkv.decodeInt("SampleRate", 44100);
    }

//    public static void changeSampleRate(@NonNull Context context,int sampleRate){
//        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
//        mmkv.encode("SampleRate", sampleRate);
//    }

    private static String getMMapId(Context context) {
        return context.getPackageName() + ".Effect";
    }



    private void setEnabledEffect(){
        boolean a = isEnabledEffect(mContext);
        JPlayer.jConfig.setEnabledEffect(a);
    }

//    public static void enabledEffect(@NonNull Context context,boolean ef){
//        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
//        mmkv.encode("EnabledEffect", ef);
//
//        JPlayer.jConfig.setEnabledEffect(ef);
//        Log.d(TAG, "enabledEffect: "+ef);
//    }

    public static boolean isEnabledEffect(@NonNull Context context){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
        return mmkv.decodeBool("EnabledEffect", false);
    }

    private void setEnabledStereoWidth(){
        boolean a = isEnabledStereoWidth(mContext);
        JPlayer.jConfig.setEnabledStereoWidth(a);
    }

//    public static void enabledStereoWidth(@NonNull Context context,boolean ef){
//        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
//        mmkv.encode("EnabledStereoWidth", ef);
//
//        JPlayer.jConfig.setEnabledStereoWidth(ef);
//        Log.d(TAG, "enabledStereoWidth: "+ef);
//    }

    public static boolean isEnabledStereoWidth(@NonNull Context context){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
        return mmkv.decodeBool("EnabledStereoWidth", false);
    }

    private void setStereoWidth(){
        float a = getStereoWidth(mContext);
        JPlayer.jConfig.setStereoWidth(a);
    }

//    public static void changeStereoWidth(@NonNull Context context,float ef){
//        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
//        mmkv.encode("SetStereoWidth", ef);
//
//        JPlayer.jConfig.setStereoWidth(ef);
//        Log.d(TAG, "SetStereoWidth: "+ef);
//    }

    public static float getStereoWidth(@NonNull Context context){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
        return mmkv.decodeFloat("SetStereoWidth", 10);
    }

    private void setEnabledChafen(){
        boolean a = isEnabledChafen(mContext);
        JPlayer.jConfig.setEnabledChafen(a);
    }

//    public static void enabledChafen(@NonNull Context context,boolean ef){
//        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
//        mmkv.encode("EnabledChafen", ef);
//
//        JPlayer.jConfig.setEnabledChafen(ef);
//        Log.d(TAG, "enabledChafen: "+ef);
//    }

    public static boolean isEnabledChafen(@NonNull Context context){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
        return mmkv.decodeBool("EnabledChafen", false);
    }

    private void setChafenDelay(){
        int a = getChafenDelay(mContext);
        JPlayer.jConfig.setChafenDelay(a);
    }

//    public static void changeChafenDelay(@NonNull Context context,int ef){
//        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
//        mmkv.encode("SetChafenDelay", ef);
//
//        JPlayer.jConfig.setChafenDelay(ef);
//        Log.d(TAG, "SetChafenDelay: "+ef);
//    }

    public static int getChafenDelay(@NonNull Context context){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
        return mmkv.decodeInt("SetChafenDelay", 20);
    }

    private void loadEqparam(){
        jEqparams.clear();
        String precut;
        MMKV mmkvPrecut = MMKV.mmkvWithID(getMMapId(mContext));
        precut = mmkvPrecut.decodeString("precut","0");
        String line;
        for (int i =0;i<10;i++){
            MMKV mmkvFilter = MMKV.mmkvWithID(getMMapId(mContext));
            line = mmkvFilter.decodeString("filter"+i,"");
            String[] filter = line.split(" ");
            if(filter.length==5 && filter[0].equals("filter")){
                JEqparam eq1 = new JEqparam();
                eq1.setFreq(Double.parseDouble(filter[2]));
                eq1.setPeak(Double.parseDouble(filter[4]));
                eq1.setQ(Double.parseDouble(filter[3]));
                eq1.setPrecut(Double.parseDouble(precut));
                jEqparams.add(eq1);
            }
        }
        if(jEqparams!=null && jEqparams.size()>0){
            jplayer.loadEQ(jEqparams);
            Log.d(TAG, "loadEqparam: "+jEqparams.get(0).freq+" "+jEqparams.get(0).peak+" "+jEqparams.get(0).q+" "+jEqparams.get(0).precut);
        }
    }

    public static String getEqparamName(@NonNull Context context){
        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
        return mmkv.decodeString("eqparamNmae", "默认");
    }

//    public static void changeEqparam(@NonNull Context context,String name,List<String> Eqparam){
//        MMKV mmkv = MMKV.mmkvWithID(getMMapId(context));
//        MMKV mmkvFilter = MMKV.mmkvWithID(getMMapId(context));
//        mmkv.encode("Eqparam", name);
//        jEqparams.clear();
//        String preCut = "0";
//        String[] precut = Eqparam.get(Eqparam.size()-1).split(" ");
//        if(precut[0].equals("precut")){
//            preCut=precut[1];
//        }
//        mmkvFilter.encode("precut", preCut);
//        for (String a:Eqparam
//             ) {
//            String[] filter = a.split(" ");
//            if(filter.length==5 && filter[0].equals("filter")){
//                JEqparam eq1 = new JEqparam();
//                eq1.setFreq(Double.parseDouble(filter[2]));
//                eq1.setPeak(Double.parseDouble(filter[4]));
//                eq1.setQ(Double.parseDouble(filter[3]));
//                eq1.setPrecut(Double.parseDouble(preCut));
//                jEqparams.add(eq1);
//                mmkvFilter.encode(filter[0]+filter[1], a);
//            }
//        }
//    }
}
