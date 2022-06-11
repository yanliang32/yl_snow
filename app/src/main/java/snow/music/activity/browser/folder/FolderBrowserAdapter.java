package snow.music.activity.browser.folder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

import recyclerview.helper.ItemClickHelper;
import recyclerview.helper.SelectableHelper;
import snow.music.R;

public class FolderBrowserAdapter extends RecyclerView.Adapter<FolderBrowserAdapter.ViewHolder> {
    private static final int TYPE_EMPTY = 1;
    private static final int TYPE_ITEM = 2;

    private List<String> mAllAlbum;
    private final ItemClickHelper mItemClickHelper;
    private final SelectableHelper mSelectableHelper;

    public FolderBrowserAdapter(@NonNull List<String> allAlbum) {
        Preconditions.checkNotNull(allAlbum);
        mAllAlbum = new ArrayList<>(allAlbum);
        mItemClickHelper = new ItemClickHelper();
        mSelectableHelper = new SelectableHelper(this);
    }

    public void setAllAlbum(List<String> allAlbum) {
        mAllAlbum = new ArrayList<>(allAlbum);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(ItemClickHelper.OnItemClickListener listener) {
        mItemClickHelper.setOnItemClickListener(listener);
    }

    public void setMarkPosition(int position) {
        if (position < 0) {
            mSelectableHelper.clearSelected();
            return;
        }

        mSelectableHelper.setSelect(position, true);
    }

    public void clearMark() {
        mSelectableHelper.clearSelected();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mItemClickHelper.attachToRecyclerView(recyclerView);
        mSelectableHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mItemClickHelper.detach();
        mSelectableHelper.detach();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = R.layout.item_album_browser;
        boolean empty = viewType == TYPE_EMPTY;

        if (empty) {
            layoutId = R.layout.empty_album_browser;
        }

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        return new ViewHolder(itemView, empty);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (holder.empty) {
            return;
        }

        String album = mAllAlbum.get(position);
        holder.tvAlbum.setText(album.substring(album.lastIndexOf("/")+1));

        mItemClickHelper.bindClickListener(holder.itemView);
        mSelectableHelper.updateSelectState(holder, position);
    }

    @Override
    public int getItemCount() {
        if (mAllAlbum.isEmpty()) {
            return 1;
        }

        return mAllAlbum.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mAllAlbum.isEmpty()) {
            return TYPE_EMPTY;
        }

        return TYPE_ITEM;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements SelectableHelper.Selectable {
        public final boolean empty;
        public TextView tvAlbum;
        public View mark;

        public ViewHolder(@NonNull View itemView, boolean empty) {
            super(itemView);

            this.empty = empty;
            if (empty) {
                return;
            }

            tvAlbum = itemView.findViewById(R.id.tvAlbum);
            mark = itemView.findViewById(R.id.mark);
        }

        @Override
        public void onSelected() {
            mark.setVisibility(View.VISIBLE);
        }

        @Override
        public void onUnselected() {
            mark.setVisibility(View.GONE);
        }
    }
}
