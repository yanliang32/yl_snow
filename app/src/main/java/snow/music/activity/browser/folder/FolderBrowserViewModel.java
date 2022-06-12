package snow.music.activity.browser.folder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pinyin.util.PinyinComparator;
import snow.music.store.MusicStore;

public class FolderBrowserViewModel extends ViewModel {
    private final MutableLiveData<List<String>> mAllFolder;
    private Disposable mLoadAllFolderDisposable;

    public FolderBrowserViewModel() {
        mAllFolder = new MutableLiveData<>(Collections.emptyList());
        loadAllFolder();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        if (mLoadAllFolderDisposable != null && !mLoadAllFolderDisposable.isDisposed()) {
            mLoadAllFolderDisposable.dispose();
        }
    }

    public LiveData<List<String>> getAllFolder() {
        return mAllFolder;
    }

    public String getFolder(int position) {
        return Objects.requireNonNull(mAllFolder.getValue()).get(position);
    }

    private void loadAllFolder() {
        mLoadAllFolderDisposable = Single.create((SingleOnSubscribe<List<String>>) emitter -> {
            List<String> allFolder = MusicStore.getInstance()
                    .getAllFolder();

            Collections.sort(allFolder, new PinyinComparator());

            if (emitter.isDisposed()) {
                return;
            }

            emitter.onSuccess(allFolder);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAllFolder::setValue);
    }
}
