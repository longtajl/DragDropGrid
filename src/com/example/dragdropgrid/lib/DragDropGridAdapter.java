package com.example.dragdropgrid.lib;

import android.view.View;

public interface DragDropGridAdapter {

    // Automatic child distribution
    public final static int AUTOMATIC = -1; 
    
    /**
     * Returns the count of item
     * 
     * @param page index
     * @return item count for page
     */
    public int itemCount();
    
    /**
     * Returns the view for the item in the page
     * 
     * @param page index
     * @param item index
     * @return the view 
     */
    public View getView(int index);
    
    /**
     * 
     * @return
     */
    public int rowCount();
    
    /**
     * 
     * @return
     */
    public int columnCount();
    
    /**
     * 子Viewの幅
     * @return
     */
    public int getChildViewWidth();
    
    /**
     * 子Viewの高さ
     * @return
     */
    public int getChildViewHeight();
    
    /**
     * View高さ
     * @return
     */
    public int getViewHeight();
    
    /**
     * HeaderViewの高さ
     */
    public int getHeaderViewHeight();
    
    /**
     * FooterViewの高さ
     */
    public int getFooterViewHeight();
    
    /**
     * Header
     */
    public View getHeader();
    
    /**
     * Footer
     */
    public View getFooter();
    
}
