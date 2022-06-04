package snow.music.service;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.common.base.Preconditions;
import com.zj.jplayercore.controller.Jplayer;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;

import snow.player.audio.AbstractMusicPlayer;
import snow.player.audio.ErrorCode;

/**
 * 封装了一个 Jplayer。
 */
public class JMusicPlayer extends AbstractMusicPlayer {
    private static final String TAG = "JMusicPlayer";

    private final Context mContext;
    private final Uri mUri;
    private final Map<String, String> mHeaders;
    private List<HttpCookie> mCookies;

    private Jplayer jplayer;

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
    public JMusicPlayer(@NonNull Context context, @NonNull Uri uri) {
        this(context, uri, null);
    }

    /**
     * 创建一个 {@link JMusicPlayer} 对象。
     *
     * @param context Context 对象，不能为 null
     * @param uri     要播放的歌曲的 URI，不能为 null
     * @param headers HTTP 首部
     */
    public JMusicPlayer(@NonNull Context context, @NonNull Uri uri, @Nullable Map<String, String> headers) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(uri);

        mContext = context;
        mUri = uri;
        mHeaders = headers;

        jplayer = new Jplayer();
        mInvalid = false;

        jplayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);

        jplayer.setOnErrorListener(new Jplayer.OnErrorListener() {
            @Override
            public boolean onError(Jplayer mp, int what, int extra) {
                Log.e(TAG, "Jplayer Error[what: " + what + ", extra: " + extra + "]");

                setInvalid();

                if (mErrorListener != null) {
                    mErrorListener.onError(JMusicPlayer.this, toErrorCode(what, extra));
                }
                return true;
            }
        });

//        jplayer.setOnInfoListener(new Jplayer.OnInfoListener() {
//            @Override
//            public boolean onInfo(Jplayer mp, int what, int extra) {
//                switch (what) {
//                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
//                        setStalled(true);
//                        return true;
//                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
//                        setStalled(false);
//                        return true;
//                }
//
//                return false;
//            }
//        });

        jplayer.setOnCompletionListener(new Jplayer.OnCompletionListener() {
            @Override
            public void onCompletion(Jplayer mp) {
                if (mLooping) {
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
    public JMusicPlayer(@NonNull Context context, @NonNull Uri uri, @Nullable Map<String, String> headers, @Nullable List<HttpCookie> cookies) {
        this(context, uri, headers);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                jplayer.setDataSource(getFileFromContentUri(mContext,mUri));
            } else {
                jplayer.setDataSource(getFileFromContentUri(mContext,mUri));
            }
            jplayer.prepare();
            startEx();
        } catch (Exception e) {
            setInvalid();
            throw e;
        }
    }

    //因为目前播放器不支持使用uri异步播放，需要将uri转为文件路径
    @SuppressLint("Range")
    public static String getFileFromContentUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String filePath;
        String[] filePathColumn = {MediaStore.DownloadColumns.DATA, MediaStore.DownloadColumns.DISPLAY_NAME};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, filePathColumn, null,
                null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            try {
                filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]));
                return filePath;
            } catch (Exception e) {
            } finally {
                cursor.close();
            }
        }
        return "";
    }

    @Override
    public void setLooping(boolean looping) {
        mLooping = looping;
    }

    @Override
    public boolean isLooping() {
        return jplayer.isLooping();
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
    public void startEx() { jplayer.start(); }

    @Override
    public void pauseEx() {
        jplayer.pause();
    }

    @Override
    public void stopEx() {
        jplayer.stop();
    }

    @Override
    public void seekTo(int pos) {
        jplayer.seekTo(pos);
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        jplayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void setSpeed(float speed) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

//        PlaybackParams playbackParams = jplayer.getPlaybackParams();
//        playbackParams.setSpeed(speed);
//        jplayer.setPlaybackParams(playbackParams);
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

        jplayer.setOnPreparedListener(new Jplayer.OnPreparedListener() {
            @Override
            public void onPrepared(Jplayer mp) {
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
//        if (listener == null) {
//            jplayer.setOnSeekCompleteListener(null);
//            return;
//        }
//
//        jplayer.setOnSeekCompleteListener(new Jplayer.OnSeekCompleteListener() {
//            @Override
//            public void onSeekComplete(Jplayer mp) {
//                listener.onSeekComplete(JMusicPlayer.this);
//            }
//        });
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

        jplayer.setOnBufferingUpdateListener(new Jplayer.OnBufferingUpdateListener() {
            @Override
            public void onCompletion() {

            }

//            @Override
//            public void onBufferingUpdate(Jplayer mp, int percent) {
//                listener.onBufferingUpdate(JMusicPlayer.this, percent, true);
//            }
        });
    }

    @Override
    public void setOnErrorListener(@Nullable OnErrorListener listener) {
        mErrorListener = listener;
    }
}
