package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.R2;
import com.thirtydegreesray.openhub.mvp.contract.base.IBaseContract;
import com.thirtydegreesray.openhub.ui.activity.base.SingleFragmentActivity;
import com.thirtydegreesray.openhub.ui.fragment.TopicsEditorFragment;
import com.thirtydegreesray.openhub.ui.widget.ZoomAbleFloatingActionButton;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Manage the "My Topics" list used by MyTopicsActivity: add a topic by name,
 * swipe to remove one, tap to include/exclude it from the multi-topic search.
 */
public class TopicsEditorActivity extends
        SingleFragmentActivity<IBaseContract.Presenter, TopicsEditorFragment> {

    public static void show(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(activity, TopicsEditorActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @BindView(R2.id.float_action_bn) ZoomAbleFloatingActionButton floatingActionButton;

    @Override
    protected TopicsEditorFragment createFragment() {
        return TopicsEditorFragment.create();
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarTitle(getString(R.string.my_topics));
        setToolbarScrollAble(true);
        floatingActionButton.setVisibility(View.VISIBLE);
        floatingActionButton.setImageResource(R.drawable.ic_add);
        // Every change (add/remove/toggle) is persisted immediately, so the
        // caller should always reload on return - regardless of whether the
        // user exits via the hardware back button, the toolbar's up arrow
        // (which bypasses onBackPressed(), going through finishActivity()
        // instead), or a system back gesture.
        setResult(Activity.RESULT_OK);
    }

    @OnClick(R2.id.float_action_bn)
    public void onAddClick() {
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setHint(getString(R.string.add_topic_hint));
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_topic)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String slug = editText.getText().toString();
                    if (!getFragment().addTopic(slug)) {
                        showWarningToast(getString(R.string.topic_already_added));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
