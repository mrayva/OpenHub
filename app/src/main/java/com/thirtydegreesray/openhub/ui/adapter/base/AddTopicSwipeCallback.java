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

/**
 * Swipe-right-to-add-to-My-Topics for RepoTopicsFragment's topic chips.
 * Unlike IgnoreSwipeCallback (which removes/reinserts rows, since it lists
 * the repos themselves), this never touches the underlying data - the row
 * being swiped is a topic name, not the thing being added/removed, so it
 * always stays visible; the caller just snaps it back via
 * notifyItemChanged() after handling the swipe.
 */
public class AddTopicSwipeCallback extends ItemTouchHelper.SimpleCallback {

    public interface Listener {
        void onSwipeToAdd(int position);
    }

    private static final int ADD_COLOR = Color.parseColor("#388E3C");

    private final Listener listener;
    private final Drawable addIcon;
    private final Paint backgroundPaint = new Paint();

    public AddTopicSwipeCallback(@NonNull Context context, @NonNull Listener listener) {
        super(0, ItemTouchHelper.RIGHT);
        this.listener = listener;
        this.addIcon = VectorDrawableCompat.create(context.getResources(), R.drawable.ic_add, context.getTheme());
        if (addIcon != null) addIcon.setTint(Color.WHITE);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                           @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    // Same reasoning as IgnoreSwipeCallback - default thresholds read as
    // sluggish next to a normal swipe-to-dismiss gesture.
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
        listener.onSwipeToAdd(position);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                             @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                             int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE || dX <= 0) return;

        View itemView = viewHolder.itemView;
        int top = itemView.getTop();
        int bottom = itemView.getBottom();
        int height = bottom - top;
        int iconSize = height / 3;
        int iconTop = top + (height - iconSize) / 2;
        int iconMargin = (height - iconSize) / 2;

        backgroundPaint.setColor(ADD_COLOR);
        c.drawRect(itemView.getLeft(), top, itemView.getLeft() + dX, bottom, backgroundPaint);
        if (addIcon != null) {
            int left = itemView.getLeft() + iconMargin;
            addIcon.setBounds(left, iconTop, left + iconSize, iconTop + iconSize);
            addIcon.draw(c);
        }
    }

}
