package snow.music.activity.setting;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import snow.music.R;
import snow.music.dialog.MessageDialog;
import snow.music.service.AppPlayerService;
import snow.music.service.JMusicPlayer;
import snow.music.util.CheckGroup;
import snow.music.util.NightModeUtil;
import snow.music.util.PlayerUtil;
import snow.player.lifecycle.PlayerViewModel;

public class SettingActivity extends AppCompatActivity {
    private static final int DARK_MODE_ID_FOLLOW_SYSTEM = 1;
    private static final int DARK_MODE_ID_OFF = 2;
    private static final int DARK_MODE_ID_ON = 3;

    final int[] flag = {0};

    private SettingViewModel mSettingViewModel;

    private View itemFollowSystem;
    private View itemDarkModeOff;
    private View itemDarkModeOn;
    //private View itemSampleRate44100;
    //private View itemSampleRate48000;
    private Spinner sampleRate;

    private View itemSoundEffects;
    private SwitchCompat swSoundEffects;
    private TextView soundEffectFile;
    private SwitchCompat swSurroundSound;
    private SwitchCompat swsoundField;
    private TextView soundFieldCount;
    private SeekBar sbSoundField;
    private SwitchCompat swDifference;
    private TextView differenceCount;
    private SeekBar sbDifference;

    private View itemPlayWithOtherApp;
    private SwitchCompat swPlayWithOtherApp;

    private CheckGroup mCheckGroup;
    //private CheckGroup srCheckGroup;

    private List<String> effectList;

    ActivityResultLauncher<Intent> intentActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        initViewModel();
        findViews();
        initViews();
        addClickListener();

        intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                //此处是跳转的result回调方法
                if (result.getData() != null && result.getResultCode() == Activity.RESULT_OK) {
                    String eqparam = result.getData().getDataString();
                    Uri uri = Uri.parse(eqparam);
                    String a = getFileRealNameFromUri(getApplication(),uri);
                    mSettingViewModel.setEqparam(eqparam,a);
                    soundEffectFile.setText("《"+a.substring(a.lastIndexOf("/")+1,a.lastIndexOf("."))+"》");
                } else {
                    Toast.makeText(getApplicationContext(), "未选择音效文件", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static String getFileRealNameFromUri(Context context, Uri fileUri) {
        if (context == null || fileUri == null) return null;
        DocumentFile documentFile = DocumentFile.fromSingleUri(context, fileUri);
        if (documentFile == null) return null;
        return documentFile.getName();
    }

    private void initViewModel() {
        ViewModelProvider provider = new ViewModelProvider(this);

        PlayerViewModel playerViewModel = provider.get(PlayerViewModel.class);
        PlayerUtil.initPlayerViewModel(this, playerViewModel, AppPlayerService.class);

        mSettingViewModel = provider.get(SettingViewModel.class);
        mSettingViewModel.init(playerViewModel);
    }

    private void findViews() {
        itemFollowSystem = findViewById(R.id.itemFollowSystem);
        itemDarkModeOff = findViewById(R.id.itemDarkModeOff);
        itemDarkModeOn = findViewById(R.id.itemDarkModeOn);
        //itemSampleRate44100 = findViewById(R.id.itemSampleRate44100);
        //itemSampleRate48000 = findViewById(R.id.itemSampleRate48000);
        sampleRate = findViewById(R.id.sampleRate);

        itemSoundEffects = findViewById(R.id.itemSoundEffects);
        swSoundEffects = findViewById(R.id.swSoundEffects);
        soundEffectFile = findViewById(R.id.soundEffectFile);
        //swSurroundSound = findViewById(R.id.swSurroundSound);
        swsoundField = findViewById(R.id.swsoundField);
        soundFieldCount = findViewById(R.id.soundFieldCount);
        sbSoundField = findViewById(R.id.seekBarSoundField);
        swDifference = findViewById(R.id.swdifference);
        differenceCount = findViewById(R.id.differenceCount);
        sbDifference = findViewById(R.id.seekBarDifference);

        itemPlayWithOtherApp = findViewById(R.id.itemPlayWithOtherApp);
        swPlayWithOtherApp = findViewById(R.id.swPlayWithOtherApp);
    }

    private void initViews() {
        mCheckGroup = new CheckGroup();
        //srCheckGroup = new CheckGroup();

        DarkModeItem followSystem = new DarkModeItem(DARK_MODE_ID_FOLLOW_SYSTEM, itemFollowSystem);
        DarkModeItem darkModeOff = new DarkModeItem(DARK_MODE_ID_OFF, itemDarkModeOff);
        DarkModeItem darkModeOn = new DarkModeItem(DARK_MODE_ID_ON, itemDarkModeOn);

        //OutputSampleRate sampleRate44100 = new OutputSampleRate(44100,itemSampleRate44100);
        //OutputSampleRate sampleRate48000 = new OutputSampleRate(48000,itemSampleRate48000);

        mCheckGroup.addItem(followSystem);
        mCheckGroup.addItem(darkModeOff);
        mCheckGroup.addItem(darkModeOn);

        //srCheckGroup.addItem(sampleRate44100);
        //srCheckGroup.addItem(sampleRate48000);

        mSettingViewModel.getNightMode()
                .observe(this, mode -> {
                    switch (mode) {
                        case NIGHT_FOLLOW_SYSTEM:
                            mCheckGroup.setChecked(DARK_MODE_ID_FOLLOW_SYSTEM);
                            break;
                        case NIGHT_NO:
                            mCheckGroup.setChecked(DARK_MODE_ID_OFF);
                            break;
                        case NIGHT_YES:
                            mCheckGroup.setChecked(DARK_MODE_ID_ON);
                            break;
                    }
                });

        mSettingViewModel.getSampleRate()
                .observe(this,rate ->{
                    switch (rate){
                        case 44100:
                            //srCheckGroup.setChecked(44100);
                            sampleRate.setSelection(0);
                            break;
                        case 48000:
                            //srCheckGroup.setChecked(48000);
                            sampleRate.setSelection(1);
                            break;
                        case 96000:
                            sampleRate.setSelection(2);
                            break;
                    }
                });

        mSettingViewModel.getEqparam().observe(this,eqp ->{
            soundEffectFile.setText("《"+eqp+"》");
        });

        mSettingViewModel.getSoundEffects().observe(this,se ->{
            swSoundEffects.setChecked(se);
        });

        mSettingViewModel.getEnabledStereoWidth().observe(this,esw ->{
            swsoundField.setChecked(esw);
        });

        mSettingViewModel.getStereoWidth().observe(this,sw ->{
            sbSoundField.setProgress(Math.round(sw));
        });

        mSettingViewModel.getChafenDelay().observe(this,cd ->{
            sbDifference.setProgress(cd);
        });

        mSettingViewModel.getEnabledChafen().observe(this,ec ->{
            swDifference.setChecked(ec);
        });

        Boolean value = mSettingViewModel.getPlayWithOtherApp().getValue();
        swPlayWithOtherApp.setChecked(Objects.requireNonNull(value));
    }

    private void addClickListener() {
        itemFollowSystem.setOnClickListener(v -> mCheckGroup.setChecked(DARK_MODE_ID_FOLLOW_SYSTEM));
        itemDarkModeOff.setOnClickListener(v -> mCheckGroup.setChecked(DARK_MODE_ID_OFF));
        itemDarkModeOn.setOnClickListener(v -> mCheckGroup.setChecked(DARK_MODE_ID_ON));

        //itemSampleRate44100.setOnClickListener(v -> srCheckGroup.setChecked(44100));
        //itemSampleRate48000.setOnClickListener(v -> srCheckGroup.setChecked(48000));


        mCheckGroup.setOnCheckedItemChangeListener(checkedItemId -> {
            switch (checkedItemId) {
                case DARK_MODE_ID_FOLLOW_SYSTEM:
                    mSettingViewModel.setNightMode(NightModeUtil.Mode.NIGHT_FOLLOW_SYSTEM);
                    break;
                case DARK_MODE_ID_OFF:
                    mSettingViewModel.setNightMode(NightModeUtil.Mode.NIGHT_NO);
                    break;
                case DARK_MODE_ID_ON:
                    mSettingViewModel.setNightMode(NightModeUtil.Mode.NIGHT_YES);
                    break;
                default:
                    break;
            }
        });

//        srCheckGroup.setOnCheckedItemChangeListener(checkedItemId -> {
//            switch (checkedItemId) {
//                case 44100:
//                    mSettingViewModel.setSampleRate(44100);
//                    break;
//                case 48000:
//                    mSettingViewModel.setSampleRate(48000);
//                    break;
//                default:
//                    break;
//            }
//        });

        sampleRate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (flag[0] == 0){
                    flag[0] = flag[0] + 1;
                    return;
                }
                switch (i) {
                    case 0:
                        mSettingViewModel.setSampleRate(44100);
                        break;
                    case 1:
                        mSettingViewModel.setSampleRate(48000);
                        break;
                    case 2:
                        mSettingViewModel.setSampleRate(96000);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        itemSoundEffects.setOnClickListener(v -> {
            swSoundEffects.toggle();
            //swSurroundSound.toggle();
            swsoundField.toggle();
        });
        swSoundEffects.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mSettingViewModel.setSoundEffects(true);
            }
            else {
                mSettingViewModel.setSoundEffects(false);
            }
        });
//        swSurroundSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//
//            }
//            else {
//
//            }
//        });
        swsoundField.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mSettingViewModel.setEnabledStereoWidth(true);

            }
            else {
                mSettingViewModel.setEnabledStereoWidth(false);

            }
        });
        sbSoundField.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //主要是用于监听进度值的改变
                soundFieldCount.setText(progress+"");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //监听用户开始拖动进度条的时候
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //监听用户结束拖动进度条的时候
                mSettingViewModel.setStereoWidth((float) seekBar.getProgress());
                //Toast.makeText(getApplicationContext(), "设置声场："+seekBar.getProgress(), Toast.LENGTH_SHORT).show();
            }
        });
        swDifference.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mSettingViewModel.setEnabledChafen(true);
            }
            else {
                mSettingViewModel.setEnabledChafen(false);
            }
        });
        sbDifference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //主要是用于监听进度值的改变
                differenceCount.setText(progress+"");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //监听用户开始拖动进度条的时候
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //监听用户结束拖动进度条的时候
                mSettingViewModel.setChafenDelay(seekBar.getProgress());
                //Toast.makeText(getApplicationContext(), "设置差分："+seekBar.getProgress(), Toast.LENGTH_SHORT).show();
            }
        });

        soundEffectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");//筛选器
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intentActivityResultLauncher.launch(intent);
            return;
        });

        itemPlayWithOtherApp.setOnClickListener(v -> swPlayWithOtherApp.toggle());
        swPlayWithOtherApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showPlayWithOtherAppTipsDialog();
                return;
            }

            mSettingViewModel.setPlayWithOtherApp(false);
        });
    }

    private void showPlayWithOtherAppTipsDialog() {
        MessageDialog dialog = new MessageDialog.Builder(this)
                .setMessage(R.string.description_play_with_other_app)
                .setNegativeButtonClickListener((dialog1, which) -> swPlayWithOtherApp.setChecked(false))
                .setPositiveButtonClickListener((dialog1, which) -> mSettingViewModel.setPlayWithOtherApp(true))
                .build();

        dialog.setCancelable(false);

        dialog.show(getSupportFragmentManager(), "PlayWithOtherAppTips");
    }

    public void finishSelf(View view) {
        finish();
    }

    private static class DarkModeItem extends CheckGroup.CheckItem {
        private ImageView ivChecked;

        public DarkModeItem(int id, View itemView) {
            super(id);
            ivChecked = itemView.findViewById(R.id.ivChecked);
        }

        @Override
        public void onChecked() {
            ivChecked.setVisibility(View.VISIBLE);
        }

        @Override
        public void onUnchecked() {
            ivChecked.setVisibility(View.GONE);
        }
    }

//    private static class OutputSampleRate extends CheckGroup.CheckItem {
//        private ImageView srChecked;
//
//        public OutputSampleRate(int id, View itemView) {
//            super(id);
//            srChecked = itemView.findViewById(R.id.srChecked);
//        }
//
//        @Override
//        public void onChecked() {
//            srChecked.setVisibility(View.VISIBLE);
//        }
//
//        @Override
//        public void onUnchecked() {
//            srChecked.setVisibility(View.GONE);
//        }
//    }

}