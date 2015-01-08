package com.pijodev.insatpe;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;;

public class XYScrollView extends FrameLayout {
	/** Constants **/
	static final int MIN_MOTION_TO_SCROLL = 10;
    static final int INVALID_POINTER_ID = -1;

	/** Touch event variables **/
	private boolean mIsBeingDragged = false;
    private int mActivePointerId = INVALID_POINTER_ID;
	private float mDownX, mDownY;
	private float mStartX, mStartY;
	private float mLastX, mLastY;
	private int mStartScrollX, mStartScrollY;
	private int mStartBarPosX, mStartBarPosY;
	private View DayBarInParent, TimingBarInParent;
	
	public XYScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public XYScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public XYScrollView(Context context) {
		super(context);
	}
	
	void setBarReference(View day, View Timing) {
		DayBarInParent = day;
		TimingBarInParent = Timing;
	}
	
	/** Intercepts the touch event, returns true if this view will catch the event **/
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
    	final int action = event.getAction();
		
		switch (action & MotionEvent.ACTION_MASK) {
	        case MotionEvent.ACTION_DOWN:
				// Starts a new event
	        	setStart(event.getX(), event.getY());
				setActivePointerId(event.getPointerId(0));
				setStartScroll();
				setStartBarPos();
				mIsBeingDragged = false;
				break;
	        case MotionEvent.ACTION_MOVE: { // this view intercept the touch event if it will scroll
	        	int pointerIndex = event.findPointerIndex(mActivePointerId);
	        	float x = event.getX(pointerIndex), y = event.getY(pointerIndex);
				return mIsBeingDragged ||
						Math.hypot(x-mStartX, y-mStartY) > XYScrollView.MIN_MOTION_TO_SCROLL;//mIsBeingDragged;
	        }
	        case MotionEvent.ACTION_POINTER_UP: {
	            final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	            final int pointerId = event.getPointerId(pointerIndex);
	            if (pointerId == getActivePointerId()) {
	                // This was our active pointer going up. Choose a new
	                // active pointer and adjust accordingly.
	                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;

		        	setStart(event.getX(newPointerIndex), event.getY(newPointerIndex));
		        	setActivePointerId(event.getPointerId(newPointerIndex));
		        	setStartScroll();//mLayoutDays.getScrollX(), mImgViewTiming.getScrollY());
					setStartBarPos();
	            }
	            break;
	        }
	        case MotionEvent.ACTION_UP:
	        case MotionEvent.ACTION_CANCEL: {
				mIsBeingDragged = false;
				setActivePointerId(INVALID_POINTER_ID);
				break;
			}
		}

		return false;
	}
	
	public boolean isBeingDragged() {
		return mIsBeingDragged;
	}
	public void setBeingDragged(boolean mIsBeingDragged) {
		this.mIsBeingDragged = mIsBeingDragged;
	}

	public int scrollX() {
		return this.getChildAt(0).getScrollX();
	}
	public int scrollY() {
		return this.getChildAt(0).getScrollY();
	}
	
	public void setStartScroll() {
		this.mStartScrollX = scrollX();
		this.mStartScrollY = scrollY();
	}
	public void setStartScroll(int startScrollX, int startScrollY) {
		this.mStartScrollX = startScrollX;
		this.mStartScrollY = startScrollY;
	}
	public void setStartScrollX(int startScrollX) {
		this.mStartScrollX = startScrollX;
	}
	public void setStartScrollY(int startScrollY) {
		this.mStartScrollY = startScrollY;
	}
	public int getStartScrollY() {
		return mStartScrollY;
	}
	public int getStartScrollX() {
		return mStartScrollX;
	}
	

	public void setStartBarPos() {
		this.mStartBarPosX = ((RelativeLayout.LayoutParams) TimingBarInParent.getLayoutParams()).leftMargin;
		this.mStartBarPosY = ((RelativeLayout.LayoutParams) DayBarInParent.getLayoutParams()).topMargin;
	}
	public int getStartBarPosX() {
		return mStartBarPosX;
	}
	public int getStartBarPosY() {
		return mStartBarPosY;
	}
	public void setStartBarPosX(int startX) {
		mStartBarPosX = startX;
	}
	public void setStartBarPosY(int startY) {
		mStartBarPosY = startY;
	}
	
	public void setLast(float mLastX, float mLastY) {
		this.mLastY = mLastY;
		this.mLastX = mLastX;
	}
	public float getLastX() {
		return mLastX;
	}
	public float getLastY() {
		return mLastY;
	}
	
	public float getMotionX(float currentX) {
		return currentX-mDownX;
	}
	public float getMotionY(float currentY) {
		return currentY-mDownY;
	}
	
	public void setStart(float mStartX, float mStartY) {
		this.mStartX = mStartX;
		this.mLastX = mStartX;
		this.mStartY = mStartY;
		this.mLastY = mStartY;
		
		this.mDownX = mStartX;
		this.mDownY = mStartY;
	}
	public void setStartX(float mStartX) {
		this.mStartX = mStartX;
		this.mLastX = mStartX;
	}
	public void setStartY(float mStartY) {
		this.mStartY = mStartY;
		this.mLastY = mStartY;
	}
	public float getStartX() {
		return mStartX;
	}
	public float getStartY() {
		return mStartY;
	}
	
	public void setDownX(float x) {
		mDownX = x;
	}
	public void setDownY(float y) {
		mDownY = y;
	}
	
	public int getActivePointerId() {
		return mActivePointerId;
	}
	public void setActivePointerId(int mActivePointerId) {
		this.mActivePointerId = mActivePointerId;
	}
}
