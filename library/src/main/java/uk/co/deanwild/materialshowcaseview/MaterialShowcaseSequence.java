package uk.co.deanwild.materialshowcaseview;

import android.app.Activity;
import android.util.Pair;
import android.view.View;

import java.util.LinkedList;
import java.util.Queue;


public class MaterialShowcaseSequence implements IDetachedListener {

    private static final ShowcaseViewPredicate DEFAULT_PREDICATE = new ShowcaseViewPredicate() {
        @Override
        public boolean apply(MaterialShowcaseView view) {
            return true;
        }
    };

    PrefsManager mPrefsManager;
    Queue<Pair<MaterialShowcaseView, ShowcaseViewPredicate>> mShowcaseQueue;
    private boolean mSingleUse = false;
    Activity mActivity;
    private ShowcaseConfig mConfig;
    private int mSequencePosition = 0;

    private OnSequenceItemShownListener mOnItemShownListener = null;
    private OnSequenceItemDismissedListener mOnItemDismissedListener = null;
    private OnSequenceFinishedListener mOnSequenceFinishedListener;

    private boolean skipSequence;

    public MaterialShowcaseSequence(Activity activity) {
        mActivity = activity;
        mShowcaseQueue = new LinkedList<>();
    }

    public MaterialShowcaseSequence(Activity activity, String sequenceID) {
        this(activity);
        this.singleUse(sequenceID);
    }

    public MaterialShowcaseSequence addSequenceItem(View targetView, String content, String dismissText) {
        addSequenceItem(targetView, "", content, dismissText);
        return this;
    }

    public MaterialShowcaseSequence addSequenceItem(View targetView, String title, String content, String dismissText) {

        MaterialShowcaseView sequenceItem = new MaterialShowcaseView.Builder(mActivity)
                .setTarget(targetView)
                .setTitleText(title)
                .setDismissText(dismissText)
                .setContentText(content)
                .build();

        if (mConfig != null) {
            sequenceItem.setConfig(mConfig);
        }

        return addSequenceItem(sequenceItem, DEFAULT_PREDICATE);
    }

    public MaterialShowcaseSequence addSequenceItem(MaterialShowcaseView sequenceItem) {
        return addSequenceItem(sequenceItem, DEFAULT_PREDICATE);
    }

    public MaterialShowcaseSequence addSequenceItem(MaterialShowcaseView sequenceItem, ShowcaseViewPredicate predicate) {
        if (predicate == null) {
            predicate = DEFAULT_PREDICATE;
        }

        mShowcaseQueue.add(Pair.create(sequenceItem, predicate));
        return this;
    }

    public MaterialShowcaseSequence singleUse(String sequenceID) {
        mSingleUse = true;
        mPrefsManager = new PrefsManager(mActivity, sequenceID);
        return this;
    }

    public void setOnItemShownListener(OnSequenceItemShownListener listener) {
        this.mOnItemShownListener = listener;
    }

    public void setOnItemDismissedListener(OnSequenceItemDismissedListener listener) {
        this.mOnItemDismissedListener = listener;
    }

    public void setOnSequenceFinishedListener(OnSequenceFinishedListener onSequenceFinishedListener) {
        this.mOnSequenceFinishedListener = onSequenceFinishedListener;
    }

    public boolean hasFired() {

        if (mPrefsManager.getSequenceStatus() == PrefsManager.SEQUENCE_FINISHED) {
            return true;
        }

        return false;
    }

    public void start() {

        /**
         * Check if we've already shot our bolt and bail out if so         *
         */
        if (mSingleUse) {
            if (hasFired()) {
                return;
            }

            /**
             * See if we have started this sequence before, if so then skip to the point we reached before
             * instead of showing the user everything from the start
             */
            mSequencePosition = mPrefsManager.getSequenceStatus();

            if (mSequencePosition > 0) {
                for (int i = 0; i < mSequencePosition; i++) {
                    mShowcaseQueue.poll();
                }
            }
        }


        // do start
        if (mShowcaseQueue.size() > 0)
            showNextItem();
    }

    private void showNextItem() {
        if (mShowcaseQueue.size() > 0 && !mActivity.isFinishing()) {
            Pair<MaterialShowcaseView, ShowcaseViewPredicate> pair = mShowcaseQueue.remove();
            MaterialShowcaseView sequenceItem = pair.first;
            if (skipSequence) {
                sequenceItem.setFired();
                showNextItem();
            } else {
                sequenceItem.setDetachedListener(this);
                if (pair.second.apply(sequenceItem) && sequenceItem.show(mActivity)) {
                    if (mOnItemShownListener != null) {
                        mOnItemShownListener.onShow(sequenceItem, mSequencePosition);
                    }
                } else {
                    sequenceItem.setDetachedListener(null);
                    showNextItem();
                }
            }
        } else {
            /**
             * We've reached the end of the sequence, save the fired state
             */
            if (mSingleUse) {
                mPrefsManager.setFired();
            }

            if (mOnSequenceFinishedListener != null) {
                mOnSequenceFinishedListener.onSequenceFinished();
            }
        }
    }


    @Override
    public void onShowcaseDetached(MaterialShowcaseView showcaseView, boolean wasDismissed) {

        showcaseView.setDetachedListener(null);

        /**
         * We're only interested if the showcase was purposefully dismissed
         */
        if (wasDismissed) {

            if (mOnItemDismissedListener != null) {
                mOnItemDismissedListener.onDismiss(showcaseView, mSequencePosition);
            }

            /**
             * If so, update the prefsManager so we can potentially resume this sequence in the future
             */
            if (mPrefsManager != null) {
                mSequencePosition++;
                mPrefsManager.setSequenceStatus(mSequencePosition);
            }

            skipSequence = showcaseView.isSkipped();
            showNextItem();
        }
    }

    public void setConfig(ShowcaseConfig config) {
        this.mConfig = config;
    }

    public interface OnSequenceItemShownListener {
        void onShow(MaterialShowcaseView itemView, int position);
    }

    public interface OnSequenceItemDismissedListener {
        void onDismiss(MaterialShowcaseView itemView, int position);
    }

    public interface ShowcaseViewPredicate {
        boolean apply(MaterialShowcaseView view);
    }

    public interface OnSequenceFinishedListener {
        void onSequenceFinished();
    }
}
