package snow.music.activity.setting;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private SettingViewModel mSettingViewModel;

    private View itemFollowSystem;
    private View itemDarkModeOff;
    private View itemDarkModeOn;
    private View itemSampleRate44100;
    private View itemSampleRate48000;

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
    private CheckGroup srCheckGroup;

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
                    try {
                        //保存读取到的内容
                        StringBuilder eq = new StringBuilder();
                        //获取URI
                        Uri uri = Uri.parse(result.getData().getDataString());
                        //获取输入流
                        InputStream is = getContentResolver().openInputStream(uri);
                        //创建用于字符输入流中读取文本的bufferReader对象
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line;
                        while ((line = br.readLine()) != null) {
                            //将读取到的内容放入结果字符串
                            eq.append(line);
                        }
                        //文件中的内容
                        String content = eq.toString();
                        String a = JMusicPlayer.getFileFromContentUri(getBaseContext(),uri);
                        soundEffectFile.setText("《"+a.substring(a.lastIndexOf("/")+1,a.lastIndexOf("."))+"》");
                        Toast.makeText(getApplicationContext(), content, Toast.LENGTH_SHORT).show();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "未选择音效文件", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
        itemSampleRate44100 = findViewById(R.id.itemSampleRate44100);
        itemSampleRate48000 = findViewById(R.id.itemSampleRate48000);

        itemSoundEffects = findViewById(R.id.itemSoundEffects);
        swSoundEffects = findViewById(R.id.swSoundEffects);
        soundEffectFile = findViewById(R.id.soundEffectFile);
        swSurroundSound = findViewById(R.id.swSurroundSound);
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
        srCheckGroup = new CheckGroup();

        DarkModeItem followSystem = new DarkModeItem(DARK_MODE_ID_FOLLOW_SYSTEM, itemFollowSystem);
        DarkModeItem darkModeOff = new DarkModeItem(DARK_MODE_ID_OFF, itemDarkModeOff);
        DarkModeItem darkModeOn = new DarkModeItem(DARK_MODE_ID_ON, itemDarkModeOn);

        OutputSampleRate sampleRate44100 = new OutputSampleRate(44100,itemSampleRate44100);
        OutputSampleRate sampleRate48000 = new OutputSampleRate(48000,itemSampleRate48000);

        mCheckGroup.addItem(followSystem);
        mCheckGroup.addItem(darkModeOff);
        mCheckGroup.addItem(darkModeOn);

        srCheckGroup.addItem(sampleRate44100);
        srCheckGroup.addItem(sampleRate48000);

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
                        case "44100":
                            srCheckGroup.setChecked(44100);
                            break;
                        case "48000":
                            srCheckGroup.setChecked(48000);
                    }
                });

        Boolean value = mSettingViewModel.getPlayWithOtherApp().getValue();
        swPlayWithOtherApp.setChecked(Objects.requireNonNull(value));
    }

    private void addClickListener() {
        itemFollowSystem.setOnClickListener(v -> mCheckGroup.setChecked(DARK_MODE_ID_FOLLOW_SYSTEM));
        itemDarkModeOff.setOnClickListener(v -> mCheckGroup.setChecked(DARK_MODE_ID_OFF));
        itemDarkModeOn.setOnClickListener(v -> mCheckGroup.setChecked(DARK_MODE_ID_ON));

        itemSampleRate44100.setOnClickListener(v -> srCheckGroup.setChecked(44100));
        itemSampleRate48000.setOnClickListener(v -> srCheckGroup.setChecked(48000));

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

        srCheckGroup.setOnCheckedItemChangeListener(checkedItemId -> {
            switch (checkedItemId) {
                case 44100:
                    mSettingViewModel.setSampleRate("44100");
                    Toast.makeText(getApplicationContext(), "44100", Toast.LENGTH_SHORT).show();
                    break;
                case 48000:
                    mSettingViewModel.setSampleRate("48000");
                    Toast.makeText(getApplicationContext(), "48000", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        });

        itemSoundEffects.setOnClickListener(v -> {
            swSoundEffects.toggle();
            swSurroundSound.toggle();
            swsoundField.toggle();
        });
        swSoundEffects.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(getApplicationContext(), "启用音效", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "关闭音效", Toast.LENGTH_SHORT).show();
            }
        });
        swSurroundSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(getApplicationContext(), "启用王者环绕", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "关闭王者环绕", Toast.LENGTH_SHORT).show();
            }
        });
        swsoundField.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(getApplicationContext(), "启用声场", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "关闭声场", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getApplicationContext(), "设置声场："+seekBar.getProgress(), Toast.LENGTH_SHORT).show();
            }
        });
        swDifference.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(getApplicationContext(), "启用差分", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "关闭差分", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getApplicationContext(), "设置差分："+seekBar.getProgress(), Toast.LENGTH_SHORT).show();
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

    private static class OutputSampleRate extends CheckGroup.CheckItem {
        private ImageView srChecked;

        public OutputSampleRate(int id, View itemView) {
            super(id);
            srChecked = itemView.findViewById(R.id.srChecked);
        }

        @Override
        public void onChecked() {
            srChecked.setVisibility(View.VISIBLE);
        }

        @Override
        public void onUnchecked() {
            srChecked.setVisibility(View.GONE);
        }
    }

}