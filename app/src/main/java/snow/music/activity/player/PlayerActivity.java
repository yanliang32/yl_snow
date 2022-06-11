package snow.music.activity.player;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import snow.music.GlideApp;
import snow.music.R;
import snow.music.activity.BaseActivity;
import snow.music.activity.navigation.NavigationActivity;
import snow.music.databinding.ActivityPlayerBinding;
import snow.music.dialog.AddToMusicListDialog;
import snow.music.dialog.BottomMenuDialog;
import snow.music.dialog.PlaylistDialog;
import snow.music.dialog.SleepTimerDialog;
import snow.music.service.AppPlayerService;
import snow.music.util.MusicUtil;
import snow.music.util.PlayerUtil;
import snow.player.audio.MusicItem;
import snow.player.lifecycle.PlayerViewModel;

public class PlayerActivity extends BaseActivity {
    public static final String KEY_START_BY_PENDING_INTENT = "START_BY_PENDING_INTENT";

    private PlayerViewModel mPlayerViewModel;
    private PlayerStateViewModel mPlayerStateViewModel;

    private ActivityPlayerBinding mBinding;
    private AlbumIconAnimManager mAlbumIconAnimManager;
    private RequestManager mRequestManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_player);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        ViewModelProvider provider = new ViewModelProvider(this);
        mPlayerViewModel = provider.get(PlayerViewModel.class);
        PlayerUtil.initPlayerViewModel(this, mPlayerViewModel, AppPlayerService.class);
        setPlayerClient(mPlayerViewModel.getPlayerClient());

        mPlayerStateViewModel = provider.get(PlayerStateViewModel.class);
        mPlayerStateViewModel.init(mPlayerViewModel, isStartByPendingIntent());

        mBinding.setPlayerViewModel(mPlayerViewModel);
        mBinding.setPlayerStateViewModel(mPlayerStateViewModel);
        mBinding.setLifecycleOwner(this);

        mAlbumIconAnimManager = new AlbumIconAnimManager(mBinding.ivAlbumIcon, this, mPlayerViewModel);
        mRequestManager = GlideApp.with(this);
        observePlayingMusicItem(mPlayerViewModel);

        mPlayerStateViewModel.setIgnoreKeepScreenOnToast(true);
        mPlayerStateViewModel.getKeepScreenOn()
                .observe(this, keepScreenOn -> {
                    mBinding.btnKeepScreenOn.setKeepScreenOn(keepScreenOn);

                    if (mPlayerStateViewModel.consumeIgnoreKeepScreenOnToast()) {
                        return;
                    }

                    int messageId;

                    if (keepScreenOn) {
                        messageId = R.string.toast_keep_screen_on;
                    } else {
                        messageId = R.string.toast_cancel_keep_screen_on;
                    }

                    Toast.makeText(PlayerActivity.this, messageId, Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isStartByPendingIntent() {
        Intent intent = getIntent();
        return intent.getBooleanExtra(KEY_START_BY_PENDING_INTENT, false);
    }

    private void observePlayingMusicItem(PlayerViewModel playerViewModel) {
        playerViewModel.getPlayingMusicItem()
                .observe(this, musicItem -> {
                    mAlbumIconAnimManager.reset();

                    if (musicItem == null) {
                        mBinding.ivAlbumIcon.setImageResource(R.mipmap.ic_player_album_default_icon_big);
                        return;
                    }

                    mRequestManager.load(musicItem.getIconUri())
                            .error(R.mipmap.ic_player_album_default_icon_big)
                            .placeholder(R.mipmap.ic_player_album_default_icon_big)
                            .transform(new CenterCrop(), new CircleCrop())
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(mBinding.ivAlbumIcon);
                });
    }

    public void finishSelf(View view) {
        finish();
    }

    public void showPlaylist(View view) {
        PlaylistDialog.newInstance()
                .show(getSupportFragmentManager(), "Playlist");
    }

    @Override
    public void finish() {
        if (mPlayerStateViewModel.isStartByPendingIntent()) {
            startActivity(new Intent(this, NavigationActivity.class));
            overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
        }

        super.finish();
    }

    public void showOptionMenu(View view) {
        MusicItem musicItem = mPlayerViewModel.getPlayerClient().getPlayingMusicItem();
        if (musicItem == null) {
            return;
        }

        BottomMenuDialog bottomMenuDialog = new BottomMenuDialog.Builder(this)
                .setTitle(musicItem.getTitle())
                .addMenuItem(R.drawable.ic_menu_item_add, R.string.menu_item_add_to_music_list)
                .addMenuItem(R.drawable.ic_menu_item_rington, R.string.menu_item_set_as_ringtone)
                .setOnMenuItemClickListener((dialog, position) -> {
                    dialog.dismiss();

                    if (position == 0) {
                        addToMusicListDialog(musicItem);
                    } else if (position == 1) {
                        setAsRingtone(musicItem);
                    }
                })
                .build();

        bottomMenuDialog.show(getSupportFragmentManager(), "musicItemOptionMenu");
    }

    private void addToMusicListDialog(MusicItem musicItem) {
        AddToMusicListDialog dialog = AddToMusicListDialog.newInstance(MusicUtil.asMusic(musicItem));
        dialog.show(getSupportFragmentManager(), "addToMusicList");
    }

    private void setAsRingtone(MusicItem musicItem) {
        MusicUtil.setAsRingtone(getSupportFragmentManager(), MusicUtil.asMusic(musicItem));
    }

    public void showSleepTimer(View view) {
        SleepTimerDialog dialog = SleepTimerDialog.newInstance();
        dialog.show(getSupportFragmentManager(), "sleepTimer");
    }
}