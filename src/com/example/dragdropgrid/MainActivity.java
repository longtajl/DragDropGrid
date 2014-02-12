package com.example.dragdropgrid;

import java.util.Random;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.dragdropgrid.lib.DragDropGrid;
import com.example.dragdropgrid.lib.DragDropGrid.DragAndDropElement;
import com.example.dragdropgrid.lib.DragDropGrid.DragSource;
import com.example.dragdropgrid.lib.DragDropGridAdapter;

@SuppressLint("Registered")
public class MainActivity extends Activity implements DragSource {
    
    DragDropGrid mGrid;
    DragDropGridAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mGrid = (DragDropGrid) findViewById(R.id.drag_grid);
        mGrid.setDragSource(this);
        
        mAdapter = new SmapleAdapter();
        mGrid.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onDragStartPreced() {
        
    }

    @Override
    public void onDragEnded() {
        
    }

    @Override
    public void onClickDragAndDropElement(DragAndDropElement v) {
        
    }
    
    public int getWindowWidth() {
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }
    
    public class SmapleAdapter implements DragDropGridAdapter {

        @Override
        public int itemCount() {
            return rowCount() * columnCount();
        }

        @Override
        public View getView(int index) {
            
            Random rnd = new Random(); 
            int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            
            DragView r = new DragView(getApplicationContext());
            r.setBackgroundColor(color);
            r.setIndex(index);
            r.setLayoutParams(new ViewGroup.LayoutParams(getChildViewWidth(), getChildViewHeight()));
            
            return r;
        }

        @Override
        public int rowCount() {
            return 30;
        }

        @Override
        public int columnCount() {
            return 3;
        }

        @Override
        public int getChildViewWidth() {
            return getWindowWidth() / columnCount();
        }

        @Override
        public int getChildViewHeight() {
            return (int) (getChildViewWidth() * 1.1);
        }

        @Override
        public int getViewHeight() {
            return getChildViewHeight() * rowCount();
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
        
    }
    
    private class DragView extends RelativeLayout implements DragAndDropElement {

        private int mIndex;
        private TextView mTextView;
        
        public DragView(Context context) {
            super(context);
            
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            mTextView = new TextView(getApplicationContext());
            mTextView.setTextColor(Color.WHITE);
            mTextView.setTextSize(17.f);
            this.addView(mTextView, params);
        }

        @Override
        public void onDragStartPreceding(View v) {
            if (v == this) {
                v.setAlpha(0.8f);
            }
        }

        @Override
        public void onDragEnded(View v) {
            if (v == this) {
                v.setAlpha(1);
            }
        }

        @Override
        public int getIndex() {
            return mIndex;
        }

        @Override
        public void setIndex(int index) {
            this.mIndex = index;
            this.mTextView.setText(String.valueOf(mIndex));
        }
        
    }

}
