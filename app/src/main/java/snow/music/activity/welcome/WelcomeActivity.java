package snow.music.activity.welcome;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.blankj.utilcode.util.CrashUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import snow.music.R;
import snow.music.activity.navigation.NavigationActivity;

public class WelcomeActivity extends AppCompatActivity {
    private Disposable mTimerDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        CrashUtils.init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startTimer();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(
                android.R.anim.fade_in,
                android.R.anim.fade_out);
    }

    public void startTimer() {
        if (mTimerDisposable != null && !mTimerDisposable.isDisposed()) {
            mTimerDisposable.isDisposed();
        }

        mTimerDisposable = Observable.timer(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::startNavigationActivity);
    }

    public void startNavigationActivity(Long along) {
        Intent intent = new Intent(this, NavigationActivity.class);
        startActivity(intent);
        finish();
    }
}
