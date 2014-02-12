package com.example.dragdropgrid.lib;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;

public class DragDropGrid extends ViewGroup implements View.OnLongClickListener, OnTouchListener, OnDragListener,
    View.OnClickListener {
    
    private static final String LOG_TAG = "DragDropGrid";

    /**
     * アニメーションの実行時間
     */
    private static final int ANIMATION_DURATION = 300;

    /**
     * 各ChildViewに通知するinterface
     */
    public interface DragAndDropElement {
        void onDragStartPreceding(View v);
        void onDragEnded(View v);
        int getIndex();
        void setIndex(int index);
    }

    /**
     * 自身のおやに対して通知するinterface
     */
    public interface DragSource {
        void onDragStartPreced();
        void onDragEnded();
        void onClickDragAndDropElement(DragAndDropElement v);
    }

    /**
     * 親
     */
    private DragSource mDragSource;
    /**
     * アダプター
     */
    private DragDropGridAdapter mAdapter;
    /**
     * ChildViewの幅
     */
    private int columnWidthSize;
    /**
     * ChildViewの高さ
     */
    private int rowHeightSize;
    /**
     * 行
     */
    private int computedRowCount;
    /**
     * 列
     */
    private int computedColumnCount;
    /**
     * タッチポイントX
     */
    private int mLastTouchX;
    /**
     * タッチポイントY
     */
    private int mLastTouchY;
    /**
     * 初期位置X
     */
    private int initialX;
    /**
     * 初期位置Y
     */
    private int initialY;

    /**
     * ドラック中のView
     */
    private View mDraggingView;
    /**
     * ドラッグ中のViewの位置
     */
    private int mLastOverlapIndex = -1;
    /**
     * タッチのギャップ
     */
    private Point mLastTouchPoint;
    /**
     * Header領域
     */
    private View mHeader;
    /**
     * Footer領域
     */
    private View mFooter;

    public DragDropGrid(Context context) {
        super(context);
        init();
    }

    public DragDropGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        if (mAdapter == null) {
            this.useDummyAdapter();
        }

        setOnDragListener(this);
        setOnTouchListener(this);
        setOnLongClickListener(this);
        setOnClickListener(this);

    }

    public void setDragSource(DragSource s) {
        this.mDragSource = s;
    }

    private void useDummyAdapter() {
        mAdapter = new DragDropGridAdapter() {
            @Override
            public int rowCount() {
                return 1;
            }

            @Override
            public int itemCount() {
                return AUTOMATIC;
            }

            @Override
            public View getView(int index) {
                return null;
            }

            @Override
            public int columnCount() {
                return 1;
            }

            @Override
            public int getChildViewWidth() {
                return 400;
            }

            @Override
            public int getChildViewHeight() {
                return getChildViewWidth();
            }

            @Override
            public int getViewHeight() {
                return 0;
            }

            @Override
            public int getHeaderViewHeight() {
                return 0;
            }

            @Override
            public int getFooterViewHeight() {
                return 0;
            }

            @Override
            public View getHeader() {
                return null;
            }

            @Override
            public View getFooter() {
                return null;
            }
        };
    }

    public void setAdapter(DragDropGridAdapter adapter) {
        this.removeAllViews();
        
        this.mAdapter = adapter;
        this.addChildView();
        
        mHeader = mAdapter.getHeader();
        if (mHeader != null)
            addView(mHeader);
        
        mFooter = mAdapter.getFooter();
        if (mFooter != null)
            addView(mFooter);
    }

    private void addChildView() {
        int count = mAdapter.itemCount();
        for (int index = 0; index < count; index++) {
            View childView = mAdapter.getView(index);
            addView(childView);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mDraggingView == null) {
            for (int index = 0; index < getChildCount(); index++)
                layoutAChild(index);
        }
        
        if (mHeader != null)
            mHeader.layout(0, 0, getWidth(), mAdapter.getHeaderViewHeight());
        
        if (mFooter != null) {
            int footerY = mAdapter.rowCount() * mAdapter.getChildViewHeight() + mAdapter.getHeaderViewHeight();
            mFooter.layout(0, footerY, getWidth(), footerY+mAdapter.getFooterViewHeight());
        }
            
    }

    private void layoutAChild(int index) {
        int position = index;
        View child = getChildAt(position);
        if (child instanceof DragAndDropElement) {
            DragAndDropElement element = (DragAndDropElement) child;
            Point p = pointWithIndex(element.getIndex(), child);

            int left = p.x;
            int top  = p.y;
            
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }
    }

    private Point pointWithIndex(int index, View childView) {
        int marginTop = mAdapter.getHeaderViewHeight();
        int col = index % mAdapter.columnCount();
        int row = index / mAdapter.columnCount();
        int x = (col * columnWidthSize);
        int y = (row * rowHeightSize) + marginTop;
        return new Point(x, y);
    }

    private int indexWithPoint(int x, int y) {
        int childViewCount = getDragAndDropElementCount();
        for (int i = 0; i < childViewCount; i++) {
            int marginTop = mAdapter.getHeaderViewHeight();
            int col = i % mAdapter.columnCount();
            int row = i / mAdapter.columnCount();
            int left = (col * columnWidthSize);
            int right = left + columnWidthSize;
            int top = (row * rowHeightSize) + marginTop;
            int bottom = top + rowHeightSize;
            Rect r = new Rect(left, top, right, bottom);
            if (r.contains(x, y)) {
                return i;
            }
        }
        return -1;
    }
    
    public int getDragAndDropElementCount() {
        int count = getChildCount();
        if (mFooter != null) count--;
        if (mHeader != null) count--;
        return count;
    }

    private View viewWithIndex(int index) {
        for (View v : getChildViews()) {
            if (v instanceof DragAndDropElement && index == ((DragAndDropElement) v).getIndex())
                return v;
        }
        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        // int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        widthSize = acknowledgeWidthSize(widthMode, widthSize, display);
        // heightSize = acknowledgeHeightSize(heightMode, heightSize, display);
        heightSize = mAdapter.getChildViewHeight() * mAdapter.rowCount();

        adaptChildrenMeasuresToViewSize(widthSize, heightSize);
        // searchBiggestChildMeasures();
        computeGridMatrixSize(widthSize, heightSize);
        computeColumnsAndRowsSizes(widthSize, heightSize);

        if (heightSize < mAdapter.getViewHeight())
            heightSize = mAdapter.getViewHeight();
        
        setMeasuredDimension(widthSize, heightSize);
    }

    private void computeColumnsAndRowsSizes(int widthSize, int heightSize) {
        columnWidthSize = widthSize / computedColumnCount;
        rowHeightSize = heightSize / computedRowCount;
    }

    private int getItemViewCount() {
        return getChildCount();
    }

    private void computeGridMatrixSize(int widthSize, int heightSize) {
        if (mAdapter.columnCount() != -1 && mAdapter.rowCount() != -1) {
            computedColumnCount = mAdapter.columnCount();
            computedRowCount = mAdapter.rowCount();
        } else {
            // if (biggestChildWidth > 0 && biggestChildHeight > 0) {
            // computedColumnCount = widthSize / biggestChildWidth;
            // computedRowCount = heightSize / biggestChildHeight;
            // }
        }

        if (computedColumnCount == 0) {
            computedColumnCount = 1;
        }

        if (computedRowCount == 0) {
            computedRowCount = 1;
        }
    }

    @SuppressWarnings("deprecation")
    private int acknowledgeWidthSize(int widthMode, int widthSize, Display display) {
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            widthSize = display.getWidth();
        }
        return widthSize;
    }

    private void adaptChildrenMeasuresToViewSize(int widthSize, int heightSize) {
        measureChildren(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action) {
        case MotionEvent.ACTION_MOVE:
            break;
        case MotionEvent.ACTION_DOWN:
            touchDown(ev);
            break;
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        mLastTouchX = (int) event.getX();
        mLastTouchY = (int) event.getY();

        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            touchDown(event);
            break;
        case MotionEvent.ACTION_MOVE:
            break;
        case MotionEvent.ACTION_UP:
            break;
        }
        return false;
    }

    private void touchDown(MotionEvent event) {
        initialX = (int) event.getRawX();
        initialY = (int) event.getRawY();
    }

    private int positionForView(View v) {
        for (int index = 0; index < getItemViewCount(); index++) {
            View child = getChildView(index);
            if (isPointInsideView(initialX, initialY, child)) {
                return index;
            }
        }
        return -1;
    }

    private View getChildView(int index) {
        return getChildAt(index);
    }

    private boolean isPointInsideView(float x, float y, View view) {
        int location[] = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];

        if (pointIsInsideViewBounds(x, y, view, viewX, viewY)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean pointIsInsideViewBounds(float x, float y, View view, int viewX, int viewY) {
        return (x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()));
    }

    @Override
    public boolean onLongClick(View v) {
        int index = positionForView(v);
        View view = getChildAt(index);
        if (view != null && view instanceof DragAndDropElement) {
            startDrag(view);
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        int index = positionForView(v);
        View view = getChildAt(index);
        if (mDragSource != null && view instanceof DragAndDropElement) {
            mDragSource.onClickDragAndDropElement((DragAndDropElement) view);
        }
    }

    private void startDrag(View v) {
        mDraggingView = v;
        mDraggingView.bringToFront();

        for (View cv : getChildViews()) {
            if (cv instanceof DragAndDropElement) {
                ((DragAndDropElement) cv).onDragStartPreceding(mDraggingView);
            }
        }

        if (mDragSource != null) {
            mDragSource.onDragStartPreced();
        }

        ClipData data = ClipData.newPlainText("text", "text : " + v.toString());
        mDraggingView.startDrag(data, new TabDragShadowBuilder(mDraggingView), (Object) mDraggingView, 0);
    }

    public List<View> getChildViews() {
        List<View> views = new ArrayList<View>();
        int count = getChildCount();
        for (int i = 0; i < count; i++)
            views.add(getChildAt(i));
        return views;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        final int action = event.getAction();
        switch (action) {

        case DragEvent.ACTION_DRAG_STARTED:
            Log.d(LOG_TAG, ">>> START DRAG <<<");
            break;

        case DragEvent.ACTION_DRAG_ENTERED:
            Log.d(LOG_TAG, ">>> ACTION_DRAG_ENTERED <<<");
            break;

        case DragEvent.ACTION_DRAG_LOCATION:
            if (mDraggingView != null) {
                
                final int x = (int) event.getX();
                final int y = (int) event.getY();

                int index = indexWithPoint(x, y);
                
                if (index != -1) {
                    if (mLastOverlapIndex == index) {
                        onDragOver(index);
                    } else {
                        if (mLastOverlapIndex != -1) {
                            onDragExit(mLastOverlapIndex);
                        }
                        onDragEnter(index);
                    }
                } else {
                    if (mLastOverlapIndex != -1) {
                        onDragEnter(mLastOverlapIndex);
                    }
                }

                moveDraggedView(x, y);

                mLastOverlapIndex = index;
            }
            break;

        case DragEvent.ACTION_DRAG_EXITED:
            Log.d(LOG_TAG," >>> ACTION_DRAG_EXITED <<< ");
            break;

        case DragEvent.ACTION_DROP:
            Log.d(LOG_TAG, " >>> ACTION_DROP <<< ");
            break;

        case DragEvent.ACTION_DRAG_ENDED:
            Log.d(LOG_TAG, " >>> ACTION_DRAG_ENDED <<< ");
            for (View cv : getChildViews()) {
                if (cv instanceof DragAndDropElement)
                    ((DragAndDropElement) cv).onDragEnded(mDraggingView);
            }

            returnDraggingViewPosition(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator ani) {
                }

                @Override
                public void onAnimationRepeat(Animator ani) {
                }

                @Override
                public void onAnimationEnd(Animator ani) {
                    if (mDragSource != null) {
                        mDragSource.onDragEnded();
                    }
                    mDraggingView = null;
                }

                @Override
                public void onAnimationCancel(Animator ani) {
                }
            });
            break;
        }
        return true;
    }

    private void moveDraggedView(int x, int y) {
        View childAt = mDraggingView;

        int width = childAt.getWidth();
        int height = childAt.getHeight();

        int l = x - (1 * width / 2);
        int t = y - (1 * height / 2);

        if (mLastTouchPoint != null) {
            l = l - mLastTouchPoint.x + width / 2;
            t = t - mLastTouchPoint.y + height / 2;
        }

        childAt.layout(l, t, l + width, t + height);
    }

    private void transferAnimation(final View v, final float fromX, final float toX, final float fromY, final float toY) {
        // 既存アニメーションをキャンセルしておく
        animationCancel(v);

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimator.setDuration(ANIMATION_DURATION);
        valueAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator anima) {
                // LogUtil.d(" >> onAnimationStart << "+ tabView.getIndex());
            }

            @Override
            public void onAnimationRepeat(Animator anima) {
            }

            @Override
            public void onAnimationEnd(Animator anima) {
                // LogUtil.d(" >> onAnimationEnd << "+ tabView.getIndex() +
                // "  >> left =" + v.getLeft());
            }

            @Override
            public void onAnimationCancel(Animator anima) {
                // LogUtil.d(" >> onAnimationCancel << "+ tabView.getIndex() +
                // "   >> left =" + v.getLeft() );
            }
        });
        valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
            // アニメーションフレーム毎に呼ばれる
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float r = ((Float) animation.getAnimatedValue()).floatValue();
                int l = (int) (r * toX + (1 - r) * fromX);
                int t = (int) (r * toY + (1 - r) * fromY);
                v.layout(l, t, l + v.getWidth(), t + v.getHeight());
            }
        });

        // アニメーションの開始
        valueAnimator.start();
        v.setTag(valueAnimator);
    }

    private void animationCancel(View v) {
        ValueAnimator a = (ValueAnimator) v.getTag();
        if (a != null)
            a.cancel();
        v.setTag(null);
    }

    private void onDragEnter(int index) {
        transferChildView(index);
    }

    private void onDragExit(int index) {

    }

    private void onDragOver(int index) {

    }

    public View getDraggingView() {
        return mDraggingView;
    }

    public View getDraggingView(String str) {
        if (mDraggingView == null) mDraggingView = new View(getContext());
        mDraggingView.setTag(str);
        return mDraggingView;
    }

    private void returnDraggingViewPosition(AnimatorListener animatorListener) {
        if (mDraggingView != null) {
            final View tb = mDraggingView;
            final int index = ((DragAndDropElement) tb).getIndex();

            // from
            final float fromX = tb.getX();
            final float fromY = tb.getY();

            // to
            final Point p = pointWithIndex(index, mDraggingView);
            final float toX = p.x;
            final float toY = p.y;

            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            valueAnimator.setDuration((long) (ANIMATION_DURATION * 1.3));
            valueAnimator.addListener(animatorListener);
            valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    int l = (int) (r * toX + (1 - r) * fromX);
                    int t = (int) (r * toY + (1 - r) * fromY);
                    tb.layout(l, t, l + tb.getWidth(), t + tb.getHeight());
                }
            });
            valueAnimator.start();
        }
    }

    private void transferChildView(int baseIndex) {
        final int toIndex = baseIndex;
        final int fromIndex = ((DragAndDropElement) mDraggingView).getIndex();
        if (toIndex > fromIndex) {
            for (int i = fromIndex + 1; i < toIndex + 1; i++) {

                int moveIndex = i - 1;
                final View tb = viewWithIndex(i);
                if (tb == null)
                    continue;
                
                ((DragAndDropElement) tb).setIndex(moveIndex);

                // From
                final float fromX = tb.getX();
                final float fromY = tb.getY();

                // To
                final Point p = pointWithIndex(moveIndex, mDraggingView);
                final float toX = p.x;
                final float toY = p.y;

                transferAnimation((View) tb, fromX, toX, fromY, toY);
            }
        } else {
            for (int i = fromIndex - 1; i > toIndex - 1; i--) {
                int moveIndex = i + 1;
                final View v = viewWithIndex(i);
                if (v == null)
                    continue;
                
                ((DragAndDropElement) v).setIndex(moveIndex);
                // From
                final float fromX = v.getX();
                final float fromY = v.getY();

                // To
                final Point p = pointWithIndex(moveIndex, mDraggingView);
                final float toX = p.x;
                final float toY = p.y;

                transferAnimation((View) v, fromX, toX, fromY, toY);
            }
        }

        final Point p = pointWithIndex(toIndex, mDraggingView);
        final View dragTabView = mDraggingView;
        ((DragAndDropElement) dragTabView).setIndex(toIndex);
        dragTabView.layout(p.x, p.y, p.x + dragTabView.getWidth(), p.y + dragTabView.getHeight());
    }

    private class TabDragShadowBuilder extends DragShadowBuilder {

        private int width, height;
        private View mView;

        public TabDragShadowBuilder(View v) {
            super(v);
            mView = v;
        }

        @Override
        public void onProvideShadowMetrics(Point size, Point touch) {

            // Sets the width of the shadow to half the width of the original
            // View
            width = getView().getWidth();

            // Sets the height of the shadow to half the height of the original
            // View
            height = getView().getHeight();

            // Sets the size parameter's width and height values. These get back
            // to the system
            // through the size parameter.
            size.set(width, height);

            // dragview get x and y
            Rect r = new Rect();
            offsetDescendantRectToMyCoords(mView, r);

            int x = mLastTouchX - r.left;
            if (x < 0) x = 0;
            int y = mLastTouchY - r.top;
            if (y < 0) y = 0;

            // Sets the touch point's position to be in the touch point
            touch.set(x, y);

            mLastTouchPoint = touch;
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            // ダミーのため描写はしない
            //super.onDrawShadow(canvas);
        }

    }

}
