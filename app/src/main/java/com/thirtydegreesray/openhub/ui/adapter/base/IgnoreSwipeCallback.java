package com.thirtydegreesray.openhub.ui.adapter.base;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.ui.adapter.RepositoriesAdapter;
import com.thirtydegreesray.openhub.util.IgnoredRepoHelper;

/**
 * Swipe-left-to-ignore / swipe-right-to-unignore for Trending/Created rows.
 * Unlike ItemTouchHelperCallback (shared by LanguagesEditorFragment/
 * TopicsEditorFragment/BookmarksFragment/TraceFragment, all direction-
 * agnostic "swipe either way to remove"), this needs a different allowed
 * direction per row depending on whether it's already ignored, so it
 * overrides getSwipeDirs() per-ViewHolder instead of taking one fixed
 * swipeDirs value for the whole list. Also the first swipe visual (colored
 * background + icon) in the codebase - no existing onChildDraw override to
 * extend.
 */
public class IgnoreSwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface Listener {
        void onSwipeToIgnore(int position);
        void onSwipeToUnignore(int position);
    }

    private static final int IGNORE_COLOR = Color.parseColor("#D32F2F");
    private static final int UNIGNORE_COLOR = Color.parseColor("#388E3C");

    private final RepositoriesAdapter adapter;
    private final Listener listener;
    private final Drawable ignoreIcon;
    private final Drawable unignoreIcon;
    private final Paint backgroundPaint = new Paint();

    public IgnoreSwipeCallback(@NonNull Context context, @NonNull RepositoriesAdapter adapter,
                                @NonNull Listener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.listener = listener;
        this.ignoreIcon = VectorDrawableCompat.create(context.getResources(), R.drawable.ic_block, context.getTheme());
        this.unignoreIcon = VectorDrawableCompat.create(context.getResources(), R.drawable.ic_done_title, context.getTheme());
        if (ignoreIcon != null) ignoreIcon.setTint(Color.WHITE);
        if (unignoreIcon != null) unignoreIcon.setTint(Color.WHITE);
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION || position >= adapter.getData().size()) return 0;
        Repository repository = adapter.getData().get(position);
        return IgnoredRepoHelper.isIgnored(repository.getFullName()) ? ItemTouchHelper.RIGHT : ItemTouchHelper.LEFT;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                           @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    /**
     * Default SimpleCallback values (0.5 width threshold, 1x escape velocity)
     * need a swipe past half the row's width to commit - on a real device
     * that reads as sluggish/unresponsive next to how short a swipe-to-
     * dismiss gesture normally is. Lowering both lets a shorter drag, or a
     * quick flick over a short distance, commit the swipe instead of
     * snapping back.
     */
    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.3f;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 0.5f;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;
        if (direction == ItemTouchHelper.LEFT) {
            listener.onSwipeToIgnore(position);
        } else {
            listener.onSwipeToUnignore(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                             @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                             int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) return;

        View itemView = viewHolder.itemView;
        int top = itemView.getTop();
        int bottom = itemView.getBottom();
        int height = bottom - top;
        int iconSize = height / 3;
        int iconTop = top + (height - iconSize) / 2;
        int iconMargin = (height - iconSize) / 2;

        if (dX > 0) {
            backgroundPaint.setColor(UNIGNORE_COLOR);
            c.drawRect(itemView.getLeft(), top, itemView.getLeft() + dX, bottom, backgroundPaint);
            if (unignoreIcon != null) {
                int left = itemView.getLeft() + iconMargin;
                unignoreIcon.setBounds(left, iconTop, left + iconSize, iconTop + iconSize);
                unignoreIcon.draw(c);
            }
        } else if (dX < 0) {
            backgroundPaint.setColor(IGNORE_COLOR);
            c.drawRect(itemView.getRight() + dX, top, itemView.getRight(), bottom, backgroundPaint);
            if (ignoreIcon != null) {
                int right = itemView.getRight() - iconMargin;
                ignoreIcon.setBounds(right - iconSize, iconTop, right, iconTop + iconSize);
                ignoreIcon.draw(c);
            }
        }
    }

}
