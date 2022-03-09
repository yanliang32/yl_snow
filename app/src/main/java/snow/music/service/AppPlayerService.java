package snow.music.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import snow.music.R;
import snow.music.activity.player.PlayerActivity;
import snow.music.store.MusicStore;
import snow.music.util.FavoriteObserver;
import snow.music.util.MusicUtil;
import snow.player.HistoryRecorder;
import snow.player.PlayMode;
import snow.player.PlayerService;
import snow.player.annotation.PersistenceId;
import snow.player.effect.AudioEffectManager;
import snow.player.ui.equalizer.AndroidAudioEffectManager;

@PersistenceId("AppPlayerService")
public class AppPlayerService extends PlayerService {
    private MusicStore mMusicStore;

    @Override
    public void onCreate() {
        super.onCreate();

        setMaxIDLETime(5);
        mMusicStore = MusicStore.getInstance();
    }

    @Nullable
    @Override
    protected NotificationView onCreateNotificationView() {
        return new AppNotificationView();
    }

    @Nullable
    @Override
    protected HistoryRecorder onCreateHistoryRecorder() {
        return musicItem -> Single.create(emitter -> mMusicStore.addHistory(MusicUtil.asMusic(musicItem)))
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    @Nullable
    @Override
    protected AudioEffectManager onCreateAudioEffectManager() {
        return new AndroidAudioEffectManager();
    }

    private static class AppNotificationView extends MediaNotificationView {
        private static final String ACTION_TOGGLE_FAVORITE = "toggle_favorite";
        private static final String ACTION_SWITCH_PLAY_MODE = "switch_play_mode";

        private FavoriteObserver mFavoriteObserver;
        private PendingIntent mToggleFavorite;
        private PendingIntent mSwitchPlayMode;

        private PendingIntent mContentIntent;

        @Override
        protected void onInit(Context context) {
            super.onInit(context);

            mFavoriteObserver = new FavoriteObserver(favorite -> invalidate());
            mFavoriteObserver.subscribe();

            mToggleFavorite = buildCustomAction(ACTION_TOGGLE_FAVORITE, (player, extras) ->
                    MusicStore.getInstance().toggleFavorite(MusicUtil.asMusic(getPlayingMusicItem())));

            mSwitchPlayMode = buildCustomAction(ACTION_SWITCH_PLAY_MODE, (player, extras) -> {
                switch (getPlayMode()) {
                    case PLAYLIST_LOOP:
                        player.setPlayMode(PlayMode.LOOP);
                        break;
                    case LOOP:
                        player.setPlayMode(PlayMode.SHUFFLE);
                        break;
                    case SHUFFLE:
                        player.setPlayMode(PlayMode.PLAYLIST_LOOP);
                        break;
                }
            });

            setDefaultIcon(loadDefaultIcon());

            Intent intent = new Intent(getContext(), PlayerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(PlayerActivity.KEY_START_BY_PENDING_INTENT, true);

            int flags;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
            } else {
                flags = PendingIntent.FLAG_UPDATE_CURRENT;
            }

            mContentIntent = PendingIntent.getActivity(context, 0, intent, flags);
        }

        @Override
        protected void onPlayModeChanged(@NonNull PlayMode playMode) {
            invalidate();
        }

        @Override
        protected void onRelease() {
            super.onRelease();
            mFavoriteObserver.unsubscribe();
        }

        @Override
        public int getSmallIconId() {
            return R.mipmap.ic_notif_small_icon;
        }

        @Override
        protected void onBuildMediaStyle(androidx.media.app.NotificationCompat.MediaStyle mediaStyle) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaStyle.setShowActionsInCompactView(1, 2, 3);
                return;
            }
            mediaStyle.setShowActionsInCompactView(2, 3);
        }

        @Override
        protected void onBuildNotification(NotificationCompat.Builder builder) {
            addToggleFavorite(builder);
            addSkipToPrevious(builder);
            addPlayPause(builder);
            addSkipToNext(builder);
            addSwitchPlayMode(builder);

            builder.setContentIntent(mContentIntent);
        }

        private Bitmap loadDefaultIcon() {
            Context context = getContext();
            BitmapDrawable drawable = (BitmapDrawable) ResourcesCompat.getDrawable(
                    context.getResources(),
                    R.mipmap.ic_notif_default_icon,
                    context.getTheme());

            if (drawable == null) {
                throw new NullPointerException();
            }

            return drawable.getBitmap();
        }

        private void addToggleFavorite(NotificationCompat.Builder builder) {
            if (isExpire()) {
                mFavoriteObserver.setMusicItem(getPlayingMusicItem());
            }

            if (mFavoriteObserver.isFavorite()) {
                builder.addAction(R.mipmap.ic_notif_favorite_true, "favorite", mToggleFavorite);
            } else {
                builder.addAction(R.mipmap.ic_notif_favorite_false, "don't favorite", mToggleFavorite);
            }
        }

        private void addSkipToPrevious(NotificationCompat.Builder builder) {
            builder.addAction(R.mipmap.ic_notif_skip_to_previous, "skip to previous", doSkipToPrevious());
        }

        private void addPlayPause(NotificationCompat.Builder builder) {
            int iconId = isPlayingState() ? R.mipmap.ic_notif_pause : R.mipmap.ic_notif_play;
            builder.addAction(iconId, "play pause", doPlayPause());
        }

        private void addSkipToNext(NotificationCompat.Builder builder) {
            builder.addAction(R.mipmap.ic_notif_skip_to_next, "skip to next", doSkipToNext());
        }

        private void addSwitchPlayMode(NotificationCompat.Builder builder) {
            switch (getPlayMode()) {
                case PLAYLIST_LOOP:
                    builder.addAction(R.mipmap.ic_notif_play_mode_playlist_loop, "sequential", mSwitchPlayMode);
                    break;
                case LOOP:
                    builder.addAction(R.mipmap.ic_notif_play_mode_loop, "sequential", mSwitchPlayMode);
                    break;
                case SHUFFLE:
                    builder.addAction(R.mipmap.ic_notif_play_mode_shuffle, "sequential", mSwitchPlayMode);
                    break;
            }
        }
    }
}
