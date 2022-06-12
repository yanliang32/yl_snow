package snow.music.activity.browser.folder;

import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import snow.music.R;
import snow.music.activity.ListActivity;
import snow.music.activity.detail.folder.FolderDetailActivity;
import snow.music.service.AppPlayerService;
import snow.music.util.PlayerUtil;
import snow.player.lifecycle.PlayerViewModel;

public class FolderBrowserActivity extends ListActivity {
    private RecyclerView rvFolderBrowser;
    private FolderBrowserViewModel mViewModel;
    private PlayerViewModel mPlayerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_browser);

        ViewModelProvider provider = new ViewModelProvider(this);
        mViewModel = provider.get(FolderBrowserViewModel.class);
        mPlayerViewModel = provider.get(PlayerViewModel.class);

        PlayerUtil.initPlayerViewModel(this, mPlayerViewModel, AppPlayerService.class);

        rvFolderBrowser = findViewById(R.id.rvFolderBrowser);
        initRecyclerView();
    }

    private void initRecyclerView() {
        rvFolderBrowser.setLayoutManager(new LinearLayoutManager(this));

        List<String> allFolder = mViewModel.getAllFolder().getValue();
        assert allFolder != null;

        FolderBrowserAdapter adapter = new FolderBrowserAdapter(allFolder);
        rvFolderBrowser.setAdapter(adapter);

        mViewModel.getAllFolder()
                .observe(this, adapter::setAllFolder);

        mPlayerViewModel.getPlayingMusicItem()
                .observe(this, musicItem -> {
                    if (musicItem == null) {
                        adapter.clearMark();
                        return;
                    }

                    List<String> folderList = mViewModel.getAllFolder().getValue();
                    adapter.setMarkPosition(folderList.indexOf(musicItem.getFolder()));
                });

        adapter.setOnItemClickListener((position, viewId, view, holder) ->
                navigateToFolderDetail(mViewModel.getFolder(position))
        );
    }

    public void finishSelf(View view) {
        finish();
    }

    public void navigateToFolderDetail(String folderName) {
        FolderDetailActivity.start(this, folderName);
    }
}