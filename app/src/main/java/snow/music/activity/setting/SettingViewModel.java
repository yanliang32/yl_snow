package snow.music.activity.setting;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.base.Preconditions;
import com.zj.jplayercore.controller.JEqparam;

import android.app.Application;
import android.net.Uri;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import snow.music.service.JMusicPlayer;
import snow.music.util.NightModeUtil;
import snow.player.lifecycle.PlayerViewModel;

public class SettingViewModel extends AndroidViewModel {
    private MutableLiveData<NightModeUtil.Mode> mNightMode;
    private MutableLiveData<Integer> sampleRate;
    private MutableLiveData<String> eqParam;
    private MutableLiveData<Boolean> mSoundEffects;
    private MutableLiveData<Boolean> mEnabledStereoWidth;
    private MutableLiveData<Float> mStereoWidth;
    private MutableLiveData<Boolean> mEnabledChafen;
    private MutableLiveData<Integer> mChafenDelay;
    private MutableLiveData<Boolean> mPlayWithOtherApp;

    private PlayerViewModel mPlayerViewModel;
    private boolean mInitialized;

    public SettingViewModel(Application application) {
        super(application);

        mEnabledStereoWidth = new MutableLiveData<>(JMusicPlayer.isEnabledStereoWidth(application));
        mStereoWidth = new MutableLiveData<>(JMusicPlayer.getStereoWidth(application));
        mEnabledChafen = new MutableLiveData<>(JMusicPlayer.isEnabledChafen(application));
        mChafenDelay = new MutableLiveData<>(JMusicPlayer.getChafenDelay(application));
        mSoundEffects = new MutableLiveData<>(JMusicPlayer.isEnabledEffect(application));
        eqParam = new MutableLiveData<>(JMusicPlayer.getEqparamName(application));
        sampleRate = new MutableLiveData<>(JMusicPlayer.getSampleRate(application));
        mNightMode = new MutableLiveData<>(NightModeUtil.getNightMode(application));
        mPlayWithOtherApp = new MutableLiveData<>(false);
    }

    public void init(@NonNull PlayerViewModel playerViewModel) {
        Preconditions.checkNotNull(playerViewModel);

        if (mInitialized) {
            return;
        }

        mInitialized = true;
        mPlayerViewModel = playerViewModel;
        mPlayWithOtherApp.setValue(mPlayerViewModel.getPlayerClient().isIgnoreAudioFocus());
    }

    @NonNull
    public LiveData<NightModeUtil.Mode> getNightMode() {
        return mNightMode;
    }

    @NonNull
    public LiveData<Integer> getSampleRate() {
        return sampleRate;
    }

    @NonNull
    public LiveData<Boolean> getSoundEffects() {
        return mSoundEffects;
    }

    @NonNull
    public LiveData<Boolean> getEnabledStereoWidth() {
        return mEnabledStereoWidth;
    }

    @NonNull
    public LiveData<Float> getStereoWidth() {
        return mStereoWidth;
    }

    @NonNull
    public LiveData<Boolean> getEnabledChafen() {
        return mEnabledChafen;
    }

    @NonNull
    public LiveData<Integer> getChafenDelay() {
        return mChafenDelay;
    }

    @NonNull
    public LiveData<String> getEqparam() {
        return eqParam;
    }

    @NonNull
    public LiveData<Boolean> getPlayWithOtherApp() {
        return mPlayWithOtherApp;
    }

    public void setNightMode(@NonNull NightModeUtil.Mode mode) {
        Preconditions.checkNotNull(mode);

        if (mode == mNightMode.getValue()) {
            return;
        }

        mNightMode.setValue(mode);
        NightModeUtil.setDefaultNightMode(getApplication(), mode);
    }

    public void setSoundEffects(@NonNull Boolean i){
        Preconditions.checkNotNull(i);

        if(i == mSoundEffects.getValue()){
            return;
        }
        mSoundEffects.setValue(i);
        mPlayerViewModel.getPlayerClient().setEnabledEffect(i);
        if(i){
            Toast.makeText(getApplication(), "启用音效", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getApplication(), "关闭音效", Toast.LENGTH_SHORT).show();
        }

    }

    public void setEnabledStereoWidth(@NonNull Boolean i){
        Preconditions.checkNotNull(i);

        if(i == mEnabledStereoWidth.getValue()){
            return;
        }
        mEnabledStereoWidth.setValue(i);
        mPlayerViewModel.getPlayerClient().setEnabledStereoWidth(i);
        if(i){
            Toast.makeText(getApplication(), "启用声场", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getApplication(), "关闭声场", Toast.LENGTH_SHORT).show();
        }

    }

    public void setStereoWidth(@NonNull Float i){
        Preconditions.checkNotNull(i);

        if(i == mStereoWidth.getValue()){
            return;
        }
        mStereoWidth.setValue(i);
        mPlayerViewModel.getPlayerClient().setStereoWidth(i);
    }

    public void setEnabledChafen(@NonNull Boolean i){
        Preconditions.checkNotNull(i);

        if(i == mEnabledChafen.getValue()){
            return;
        }
        mEnabledChafen.setValue(i);
        mPlayerViewModel.getPlayerClient().setEnabledChafen(i);
        if(i){
            Toast.makeText(getApplication(), "启用差分", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getApplication(), "关闭差分", Toast.LENGTH_SHORT).show();
        }

    }

    public void setChafenDelay(@NonNull Integer i){
        Preconditions.checkNotNull(i);

        if(i == mChafenDelay.getValue()){
            return;
        }
        mChafenDelay.setValue(i);
        mPlayerViewModel.getPlayerClient().setChafenDelay(i);
    }

    public void setSampleRate(@NonNull Integer rate) {
        Preconditions.checkNotNull(rate);

        int a = sampleRate.getValue();
        if (rate == a) {
            return;
        }
        Toast.makeText(getApplication(), rate+",已重置播放器，请重新播放", Toast.LENGTH_SHORT).show();
        sampleRate.setValue(rate);
        mPlayerViewModel.getPlayerClient().setSampleRate(rate);
        mPlayerViewModel.getPlayerClient().stop();
    }

    public void setEqparam(@NonNull String eqparam,String name){
        Preconditions.checkNotNull(eqparam);

        if (name == eqParam.getValue()) {
            return;
        }

        try {
            List<String> a = new ArrayList<>();
            a.add(name);
            //获取URI
            Uri uri = Uri.parse(eqparam);
            //获取输入流
            InputStream is = getApplication().getContentResolver().openInputStream(uri);
            //创建用于字符输入流中读取文本的bufferReader对象
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                a.add(line);
            }
            mPlayerViewModel.getPlayerClient().setEqparam(a);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        eqParam.setValue(name);

    }

    public void setPlayWithOtherApp(boolean playWithOtherApp) {
        Boolean value = mPlayWithOtherApp.getValue();
        assert value != null;

        if (value == playWithOtherApp) {
            return;
        }

        mPlayWithOtherApp.setValue(playWithOtherApp);
        mPlayerViewModel.getPlayerClient().setIgnoreAudioFocus(playWithOtherApp);
    }
}
