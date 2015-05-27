package com.pijodev.insatpe;

import java.util.Date;

import org.apache.http.util.LangUtils;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ScheduleView {
	/** Views **/ 
	/*private*/ XYScrollView mXYScrollLayout; // main scrollview
	private LinearLayout mLayoutDays; // Day bar (top of the main scrollview)
	private ImageView mImgViewTiming; // Hours bar (left of the main scrollview)
	private ToggleButton mButtonSettings; // show/hide setting button (top-left of the main scrollview)
	private BarAnimation barAnim = new BarAnimation();
	
	/** Constantes **/
	static final float HourHeightDpi = 50;// TODO : load from xml files
	static final float HourWidthDpi = 60;// TODO : load from xml files
	static final float ColumnWidthDpi = 200;// TODO : load from xml files
	static final float ColumnHeightDpi = 750;// TODO : load from xml files
	static final float SeparatorWidthDpi = 1;// TODO : load from xml files
	static final float DayHeightDpi = 50;// TODO : load from xml files
	static final float LoadingViewYPosDpi = 100;// TODO : load from xml files
	static final float HourTextSizeDpi = 14;// TODO : load from xml files
	static final float EntryTextSizeDpi = 12;// TODO : load from xml files
	/** Constant dimension values in pixel **/
	static int HourHeightPx, HourWidthPx, ColumnWidthPx,
		ColumnHeightPx, SeparatorWidthPx, DayHeightPx,
		LoadingViewYPosPx, HourTextSizePx, EntryTextSizePx,
		FullScheduleWidthPx;
	
	//final float HourHeightPx, HourWidthPx, ColumnWidthPx, DayHeightPx, LoadingViewYPosPx, HourTextSizePx;// TODO : load from xml files
	
	Main mContext;
	
	/** Lanscape mode enabled <=> view fill in screen width, lock the scroll **/
	private boolean mLandscapeMode = false;
	
	/** There aren't comments for this function !! (That's a troll ! lol) */
	public ScheduleView(Main context) {
		mContext = context;
		HourHeightPx = mContext.convertDpToPx(HourHeightDpi);
		HourWidthPx = mContext.convertDpToPx(HourWidthDpi);
		ColumnWidthPx = mContext.convertDpToPx(ColumnWidthDpi);
		ColumnHeightPx = mContext.convertDpToPx(ColumnHeightDpi);
		SeparatorWidthPx = mContext.convertDpToPx(SeparatorWidthDpi);
		DayHeightPx = mContext.convertDpToPx(DayHeightDpi);
		LoadingViewYPosPx = mContext.convertDpToPx(LoadingViewYPosDpi);
		HourTextSizePx = mContext.convertDpToPx(HourTextSizeDpi);
		EntryTextSizePx = mContext.convertDpToPx(EntryTextSizeDpi);
		FullScheduleWidthPx = (int)(ColumnWidthPx*5 + SeparatorWidthPx*4);
		
		setupImageViews();
    	setupSettingsButtonListener();
    	
    	mXYScrollLayout = (XYScrollView) mContext.findViewById(R.id.fl_week);
    	mXYScrollLayout.setOnTouchListener(getOnTouchListener());
    	mXYScrollLayout.setBarReference(mLayoutDays, mImgViewTiming); // Note : By default, the bars are visible (cf main.xml)
    }
	
	/** Initialize the scroll of the view and the bars and focus on the current
	 * day column, Set the landscape mode; this function should be called once only... 
	 **/
	public void init() {
		mLandscapeMode = (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
		if(mLandscapeMode)
			changeOrientation(Configuration.ORIENTATION_LANDSCAPE);

		// Compute the scrollX with the current day
		int day = new Date().getDay() - 1;
		if(day < 0 || day > 4) day = 0;
		
    	int scrollDayX = (int)(day*(ColumnWidthPx+SeparatorWidthPx) - (mContext.findViewById(R.id.rl_main).getWidth()+HourWidthPx-ColumnWidthPx)/2);
    	int scrollDayXMin = -HourWidthPx;
    	int scrollDayXMax = FullScheduleWidthPx - mContext.findViewById(R.id.rl_main).getWidth();
    	
    	// Here, the scroll will automatically be adpated if the landscape mode is enabled (see scrollViewsTo)
    	scrollViewsTo(Math.min(scrollDayXMax, Math.max(scrollDayX, scrollDayXMin)), -HourHeightPx);
	}
	
	/** OnTouch listener **/
	private OnTouchListener getOnTouchListener() {
		OnTouchListener listener = new OnTouchListener() {
	    	
			public boolean onTouch(View v, MotionEvent event) {
				final int action = event.getAction();
				boolean handled = true;
		        
				switch (action & MotionEvent.ACTION_MASK) {
			        case MotionEvent.ACTION_DOWN:
						// Starts a new event
			        	mXYScrollLayout.setStart(event.getX(), event.getY());
			        	mXYScrollLayout.setActivePointerId(event.getPointerId(0));
						mXYScrollLayout.setStartScroll();
						mXYScrollLayout.setStartBarPos();
						mXYScrollLayout.setBeingDragged(false);
						
						break;
			        case MotionEvent.ACTION_MOVE: {
			        	barAnim.stopAnimation();
			        	int pointerIndex = event.findPointerIndex(mXYScrollLayout.getActivePointerId());
						float x = event.getX(pointerIndex);
						float y = event.getY(pointerIndex);
						float startX = mXYScrollLayout.getStartX();
						float startY = mXYScrollLayout.getStartY();
						float startScrollX = mXYScrollLayout.getStartScrollX();
						float startScrollY = mXYScrollLayout.getStartScrollY();
						
						if(mXYScrollLayout.isBeingDragged() || x-startX != 0 || y-startY != 0) {
							// Saves that a motion has been done
							if(!mXYScrollLayout.isBeingDragged() && Math.hypot(x-startX, y-startY) > XYScrollView.MIN_MOTION_TO_SCROLL)
								mXYScrollLayout.setBeingDragged(true);
							
							float dx = x - mXYScrollLayout.getLastX();
							float dy = y - mXYScrollLayout.getLastY();
							if(mXYScrollLayout.isBeingDragged() && (dx != 0 || dy != 0)) {
								// Calculs the scrolling values
								float scrollX = (int)(-(x-startX)+ startScrollX);
								float scrollY = (int)(-(y-startY)+ startScrollY);
								// Sets the max scrolling values
								int maxX = Math.max(0, FullScheduleWidthPx - mContext.findViewById(R.id.rl_main).getWidth());
								int maxY = Math.max(0, ColumnHeightPx - mContext.findViewById(R.id.rl_main).getHeight());
								int minX = -HourWidthPx;
								int minY = -DayHeightPx;
								// Corrects the scrolling-X values 
								if(scrollX < minX) {
									scrollX = minX;
									mXYScrollLayout.setStartScrollX(minX);
									mXYScrollLayout.setStartX(x);
								} else if(scrollX > maxX) {
									scrollX = maxX;
									mXYScrollLayout.setStartScrollX(maxX);
									mXYScrollLayout.setStartX(x);
								}
								// Corrects the scrolling-Y values 
								if(scrollY < minY) {
									scrollY = minY;
									mXYScrollLayout.setStartScrollY(minY);
									mXYScrollLayout.setStartY(y);
								} else if(scrollY > maxY) {
									scrollY = maxY;
									mXYScrollLayout.setStartScrollY(maxY);
									mXYScrollLayout.setStartY(y);
								}
								
								// Applies the scrolling and the position
								int posX = mXYScrollLayout.getStartBarPosX() + (int) mXYScrollLayout.getMotionX(x);
								int posY = mXYScrollLayout.getStartBarPosY() + (int) mXYScrollLayout.getMotionY(y);
								if(-HourWidthPx > posX || posX > 0) {
									posX = Math.min(0, Math.max(-HourWidthPx, posX));
									mXYScrollLayout.setStartBarPosX(posX);
									mXYScrollLayout.setDownX(x);
								}
								if(-DayHeightPx > posY || posY > 0) {
									posY = Math.min(0, Math.max(-DayHeightPx, posY));
									mXYScrollLayout.setStartBarPosY(posY);
									mXYScrollLayout.setDownY(y);
								}
								setBarPosition(posX, posY);
								
								// Here, the scroll will automatically be adpated if the landscape mode is enabled (see scrollViewsTo)
						    	scrollViewsTo((int)scrollX, (int)scrollY);
							}
						}

						mXYScrollLayout.setLast(x, y);
						
						break;
					}
			        case MotionEvent.ACTION_POINTER_UP: {
			            final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK)  >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			            final int pointerId = event.getPointerId(pointerIndex);
			            if (pointerId == mXYScrollLayout.getActivePointerId()) {
			                // This was our active pointer going up. Choose a new
			                // active pointer and adjust accordingly.
			                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;

				        	mXYScrollLayout.setStart(event.getX(newPointerIndex), event.getY(newPointerIndex));
				        	mXYScrollLayout.setActivePointerId(event.getPointerId(newPointerIndex));
				        	mXYScrollLayout.setStartScroll();//mLayoutDays.getScrollX(), mImgViewTiming.getScrollY());
							mXYScrollLayout.setStartBarPos();
			            }
			            break;
			        }
			        case MotionEvent.ACTION_UP:
			        	// TODO : Velocity ?
			        	if(mXYScrollLayout.isBeingDragged())
			        		barAnim.startAnimationIfNecessary();
			        case MotionEvent.ACTION_CANCEL: {
			        	/*
			        	// Case of a clic
			        	int pointerIndex = event.findPointerIndex(mXYScrollLayout.getActivePointerId());
						if(!mXYScrollLayout.isBeingDragged())
							onClick((int)event.getX(pointerIndex), (int)event.getY(pointerIndex), null);//	toggleBarsVisibility();
						 */
						mXYScrollLayout.setBeingDragged(false);
						mXYScrollLayout.setActivePointerId(XYScrollView.INVALID_POINTER_ID);
						break;
					}
				}

				return handled;
		}};
			
		return listener;
	}

	/** Scrolls the views **/
	public void scrollViews(int scrollX, int scrollY, int initBarPosX, int initBarPosY, int dx, int dy) {
		scrollViewsTo(scrollX, scrollY);
    	
    	int x = initBarPosX - dx;
    	int y = initBarPosY - dy;
    	setBarPosition(Math.max(-HourWidthPx, Math.min(x, 0)), Math.max(-DayHeightPx, Math.min(y, 0)));
    }
	
	/** Scrolls the XYScrollView and the bars in absolute, it does not change the bar's position 
	 * if the landscape mode is enabled, the scrollX is set to 0 **/
    public void scrollViewsTo(int scrollX, int scrollY) {
    	if(mLandscapeMode)
    		scrollX = 0;
    	
    	// Scroll the XYScrollView
    	mXYScrollLayout.getChildAt(0).scrollTo(scrollX, scrollY);
    	
    	RelativeLayout.LayoutParams lp;
    	
    	/// Scroll the day bar
    	//mLayoutDays.scrollTo(scrollX, 0);
    	lp = (RelativeLayout.LayoutParams) mLayoutDays.getLayoutParams();
    	lp.leftMargin = -scrollX;
    	mLayoutDays.setLayoutParams(lp);
    	
    	/// Scroll the timing bar
    	//mImgViewTiming.scrollTo(0, scrollY);
    	lp = (RelativeLayout.LayoutParams) mImgViewTiming.getLayoutParams();
    	lp.topMargin = -scrollY;
    	mImgViewTiming.setLayoutParams(lp);
    }
    public void setBarPosition(int posX, int posY) {
		RelativeLayout.LayoutParams lp;
    	
    	/// Move the day bar
    	lp = (RelativeLayout.LayoutParams) mLayoutDays.getLayoutParams();
    	lp.topMargin = posY;
    	mLayoutDays.setLayoutParams(lp);
    	
    	/// Move the timing bar
    	lp = (RelativeLayout.LayoutParams) mImgViewTiming.getLayoutParams();
    	lp.leftMargin = posX;
    	mImgViewTiming.setLayoutParams(lp);
    	
    	/// Move the settings button
    	lp = (RelativeLayout.LayoutParams) mButtonSettings.getLayoutParams();
    	lp.leftMargin = posX;
    	lp.topMargin = posY;
    	mButtonSettings.setLayoutParams(lp);
    }
    /** Scrolls the XYScrollView to the origine (aligned with the bars) and set the visibility of the bars **/
	public void resetScrollViews(boolean showBars) {
		barAnim.stopAnimation();
		if(showBars) {
			setBarPosition(0, 0);
			
			// Here, the scroll will automatically be adpated if the landscape mode is enabled (see scrollViewsTo)
	    	scrollViewsTo(-HourWidthPx, -DayHeightPx);
		}
    	else {
			setBarPosition(-HourWidthPx, -DayHeightPx);
			scrollViewsTo(0, 0);
    	}
	}
    /** Scrolls the XYScrollView to the origine (aligned with the bars) **/
	public void resetScrollViews() {
		barAnim.stopAnimation();
    	int offsetX = ((RelativeLayout.LayoutParams) mImgViewTiming.getLayoutParams()).leftMargin;
    	int offsetY = ((RelativeLayout.LayoutParams) mLayoutDays.getLayoutParams()).topMargin;
    	
    	scrollViewsTo(-HourWidthPx - offsetX, -DayHeightPx - offsetY);
	}
    
	/** Clears all the entries view in the schedule **/
	public void clearEntries() {
		int daysID[] = {R.id.rl_monday, R.id.rl_tuesday, R.id.rl_wednesday, R.id.rl_thursday, R.id.rl_friday};
    	
    	for(int i = 0; i < 5; i++) {
    		RelativeLayout rl = ((RelativeLayout) mXYScrollLayout.findViewById(daysID[i]));
    		rl.clearAnimation();
    		rl.removeAllViews();
    	}
    	
    	setDaysView(mLayoutDays, null);
	}
	/** Adds the Entries view created with createEntryView
     * This method shall not be called outside of the main thread.
     */
    void addWeekEntriesView(LinearLayout[][] entriesView, String[] entriesDate) {
    	int daysID[] = {R.id.rl_monday, R.id.rl_tuesday, R.id.rl_wednesday, R.id.rl_thursday, R.id.rl_friday};
    	
    	AlphaAnimation anim = new AlphaAnimation(0, 1);
    	anim.setDuration(400);
    	anim.setFillBefore(true);
    	
    	for(int day = 0; day < 5; day++) {
    		RelativeLayout rl = (RelativeLayout) mXYScrollLayout.findViewById(daysID[day]);
    		LinearLayout ll[] = entriesView[day];
    		
    		for(int i = 0; i < ll.length; i++)
    			rl.addView(ll[i]);
    		
	    	// Add a beautiful animation
	    	LayoutAnimationController layoutAnim = new LayoutAnimationController(anim);
	    	layoutAnim.setOrder(LayoutAnimationController.ORDER_RANDOM);
	    	layoutAnim.setDelay(0.25f);
	    	
        	rl.setLayoutAnimation(layoutAnim);
        	rl.startLayoutAnimation();
    	}
    	
    	setDaysView(mLayoutDays, entriesDate);
    }
    /** Show progressBar for waiting for loading */
    void addLoadingView() {
    	int daysID[] = {R.id.rl_monday, R.id.rl_tuesday, R.id.rl_wednesday, R.id.rl_thursday, R.id.rl_friday};
    	RelativeLayout.LayoutParams params =
    			new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	params.topMargin = LoadingViewYPosPx;

		AlphaAnimation anim = new AlphaAnimation(0, 1);
    	anim.setDuration(200);
    	anim.setFillBefore(true);

    	for(int i = 0; i < 5; i++) {
    		RelativeLayout rl = (RelativeLayout) mContext.findViewById(daysID[i]);
    		rl.setGravity(Gravity.CENTER_HORIZONTAL);
    		ProgressBar loader = new ProgressBar(mContext);
    		loader.setIndeterminateDrawable(mContext.getResources().getDrawable(R.drawable.loader_anim));
    		rl.addView(loader, params);
    		
        	rl.setLayoutAnimation(new LayoutAnimationController(anim));
        	rl.startLayoutAnimation();
    	}
	}

    /** Draws the timing view
     * One hour is represented by 60 Dpi (cf constant HourHeightDpi)
     **/
    private void drawTimingView(ImageView src) {
    	Bitmap bmp = Bitmap.createBitmap(HourWidthPx, mContext.convertDpToPx(HourHeightDpi*15), Config.ARGB_8888);
    	Canvas c = new Canvas(bmp);

    	/** Setting paint **/
    	Paint paint = new Paint();
        paint.setColor(0xff000000);
        paint.setTextSize(HourTextSizePx);
        float y;
    	
    	/** Draw background **/
    	c.drawColor(mContext.getResources().getColor(R.color.bar_background));
    	//c.drawLine(HourWidthPx-1, 0, HourWidthPx-1, bmp.getHeight(), paint);
    	
    	/** Draw Texts ... **/
        paint.setAntiAlias(true);
        for(int h = 0; h < 16; h++) {
        	y = h*HourHeightPx;
            c.drawText(""+(h+8)+"H00", mContext.convertDpToPx(10), HourTextSizePx+y, paint);
        }
        /** ... and (fun) lines **/
        paint.setAntiAlias(false);
        for(int h = 0; h < 16; h++) {
        	y = h*HourHeightPx;
        	c.drawLine(HourWidthPx*0.2f, y, HourWidthPx, y, paint);
        }

    	src.setImageBitmap(bmp);
    }
    /** sets the day bar view **/
    private void setDaysView(LinearLayout layout, String[] date) {
        String dayName[] = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi"};
        int dayId[] = {R.id.ll_monday, R.id.ll_tuesday, R.id.ll_wednesday, R.id.ll_thursday, R.id.ll_friday};
        
        for(int d = 0; d < 5; d++) {
        	LinearLayout dayLayout = (LinearLayout) layout.findViewById(dayId[d]);
        	((TextView) dayLayout.findViewById(R.id.tv_day)).setText(dayName[d]);;
        	((TextView) dayLayout.findViewById(R.id.tv_date)).setText(date != null ? date[d] : "");
        }
    }
    
    /** Sets up the setting button **/
    private void setupSettingsButtonListener() // TODO : faire de cette fantastique merde un beau truc
    {
    	final RelativeLayout mainViewLayout = (RelativeLayout) mContext.findViewById(R.id.rl_main_view);
    	final RelativeLayout rlMain = (RelativeLayout) mContext.findViewById(R.id.rl_main);
		final LinearLayout ll = (LinearLayout) mContext.findViewById(R.id.ll_settings);
		ll.setVisibility(LinearLayout.GONE);
    	(mButtonSettings = (ToggleButton) mContext.findViewById(R.id.ib_settings)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(ll.getVisibility() == LinearLayout.GONE) {
					ll.setVisibility(LinearLayout.VISIBLE);
					ll.measure(rlMain.getWidth(), rlMain.getHeight());
					RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mainViewLayout.getLayoutParams();
					params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
					params.addRule(RelativeLayout.ABOVE, 0);
					mainViewLayout.setLayoutParams(params);

					TranslateAnimation anim = new TranslateAnimation(0,0, 0, ll.getMeasuredHeight());
					anim.setDuration(200);
					
					anim.setAnimationListener(new AnimationListener() {
						public void onAnimationStart(Animation animation) {}
						public void onAnimationRepeat(Animation animation) {}
						public void onAnimationEnd(Animation animation) {
							mainViewLayout.clearAnimation();
							RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mainViewLayout.getLayoutParams();
							params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
							params.addRule(RelativeLayout.ABOVE, R.id.ll_settings);
							mainViewLayout.setLayoutParams(params);
						}});

					mainViewLayout.startAnimation(anim);
				}
				else {
					TranslateAnimation anim = new TranslateAnimation(0,0, ll.getHeight(), 0);
					anim.setDuration(300);
					
					TranslateAnimation fakeAnim = new TranslateAnimation(0,0,0,0);
					fakeAnim.setDuration(300);
					ll.setVisibility(LinearLayout.GONE);

					mainViewLayout.startAnimation(anim);
					ll.startAnimation(fakeAnim);
				}
		}});
    }
    /** Initialize the image views **/
    private void setupImageViews()
    {
        mImgViewTiming = (ImageView) mContext.findViewById(R.id.iv_timing);
        drawTimingView(mImgViewTiming);
        mImgViewTiming.setScaleType(ScaleType.MATRIX);
        mLayoutDays = (LinearLayout) mContext.findViewById(R.id.ll_day);
        setDaysView(mLayoutDays, null);
    }
    
    /** Reduced dimensions in pixels **/
	static float ReducedHourHeightPx = 50;// TODO : load from xml files
	static float ReducedHourWidthPx = 60;// TODO : load from xml files
	static float ReducedColumnWidthPx = 200;// TODO : load from xml files
	static float ReducedColumnHeightPx = 750;// TODO : load from xml files
	static float ReducedSeparatorWidthPx = 1;// TODO : load from xml files
	static float ReducedDayHeightPx = 50;// TODO : load from xml files
	static float ReducedLoadingViewYPosPx = 100;// TODO : load from xml files
	static float ReducedHourTextSizePx = 14;// TODO : load from xml files
	static float ReducedEntryTextSizePx = 12;// TODO : load from xml files
    
	/** Zoom state ** /
	static boolean zoomNormal = true;
	/** Toggle the zomm state (normal or out) ** /
	public void toggleZoom(int x, int y) {
		if(zoomNormal = !zoomNormal)
			zoomIn(x, y);
		else
			zoomOut();
	}
    /** Zoom out : the schedule will be reduced to fill the screen  ** /
    public void zoomOut() {
    	ReducedColumnWidthPx = (mContext.findViewById(R.id.rl_main).getWidth() - 4)/5;
    	float factor = ReducedColumnWidthPx / ColumnWidthDpi;
    	ReducedColumnHeightPx = mContext.convertDpToPx(ColumnHeightDpi * factor);
    	ReducedHourHeightPx = mContext.convertDpToPx(HourHeightDpi * factor); 
    	ReducedLoadingViewYPosPx = mContext.convertDpToPx(LoadingViewYPosDpi * factor);
    	ReducedHourTextSizePx = mContext.convertDpToPx(HourTextSizeDpi * factor);
    	ReducedEntryTextSizePx = mContext.convertDpToPx(EntryTextSizeDpi * factor);
    	LinearLayout ll_week = (LinearLayout) mXYScrollLayout.findViewById(R.id.ll_week);
    	for(int i = 0; i < 5; i++) {
    		ll_week.getChildAt(i).getLayoutParams().width = (int) ReducedColumnWidthPx;
    		ll_week.getChildAt(i).setLayoutParams(ll_week.getChildAt(i).getLayoutParams());
    		mLayoutDays.getChildAt(i).getLayoutParams().width = (int) ReducedColumnWidthPx;
    		mLayoutDays.getChildAt(i).setLayoutParams(mLayoutDays.getChildAt(i).getLayoutParams());
    	}
    	/*mLayoutDays.invalidate();
    	mXYScrollLayout.invalidate();* /
    	
    	//	for each rl_day
    	//		reduce dimensions of column
    	//		for each entry
    	//			reduce dimensions
    	//			hide room
    	//	reduce bar dim
    	//	hide date
    }
    /** Zoom in and scroll the view to show the x,y point (relative to parent) in the middle of the view if possible ** /
    public void zoomIn(float x, float y) {
    	// show all
    	// reset dim
    	LinearLayout ll_week = (LinearLayout) mXYScrollLayout.findViewById(R.id.ll_week);
    	for(int i = 0; i < 5; i++) {
    		ll_week.getChildAt(i).getLayoutParams().width = (int) mContext.convertDpToPx(ColumnWidthDpi);
    		ll_week.getChildAt(i).setLayoutParams(ll_week.getChildAt(i).getLayoutParams());
    		mLayoutDays.getChildAt(i).getLayoutParams().width = (int) mContext.convertDpToPx(ColumnWidthDpi);
    		mLayoutDays.getChildAt(i).setLayoutParams(mLayoutDays.getChildAt(i).getLayoutParams());
    	}
    }*/
    
    /** Updates the orientation of the view, changes the width of the columns **/
    public void changeOrientation(int orientation) {
		View v;
		LinearLayout.LayoutParams lp;
    	LinearLayout ll_week = (LinearLayout) mXYScrollLayout.findViewById(R.id.ll_week);

    	mLandscapeMode = (orientation == Configuration.ORIENTATION_LANDSCAPE);

    	float weight = mLandscapeMode ? 0.2f : 0.0f;
    	int width = mLandscapeMode ? LinearLayout.LayoutParams.FILL_PARENT : (int) mContext.convertDpToPx(ColumnWidthDpi);
    	
    	for(int i = 0; i < 5; i++) {
    		// Change the width of the column
    		v = ll_week.getChildAt(i);
    		lp = (LayoutParams) v.getLayoutParams();
    		lp.width = width;//(int) mContext.convertDpToPx(ColumnWidthDpi);
    		lp.weight = weight;
    		v.setLayoutParams(lp);
    		
    		// Change the width of the day bar
    		v = mLayoutDays.getChildAt(i);
    		lp = (LayoutParams) v.getLayoutParams();
    		lp.width = width;//(int) mContext.convertDpToPx(ColumnWidthDpi);
    		lp.weight = weight;
    		v.setLayoutParams(lp);
    	}
    }
    
    class BarAnimation {
    	// The animation frames change each 30ms
    	private static final long delayMillis = 30;
    	private static final float speed = 0.65f;
		private final Handler handler = new Handler();
    	private final Runnable motion = new Runnable() {
			public void run() {
				int x = ((RelativeLayout.LayoutParams) mImgViewTiming.getLayoutParams()).leftMargin;
				int y = ((RelativeLayout.LayoutParams) mLayoutDays.getLayoutParams()).topMargin;
				/* show the bars if one of them is show at 50% at least or if the scroll is negative
				 * hide the bars else
				 */
				if(x > -HourWidthPx/2 || mXYScrollLayout.getChildAt(0).getScrollX() < 0)
					x = (int) (x*speed);// -> 0
				else
					x = (int) ((x+HourWidthPx)*speed) - HourWidthPx;// -> HourWidthDpi
				
				if(y > -DayHeightPx/2 || mXYScrollLayout.scrollY() < 0)
					y = (int) (y*speed);// -> 0
				else
					y = (int) ((y+DayHeightPx)*speed) - DayHeightPx;// -> DayHeightDpi
				
				setBarPosition(x, y);
				
				if(x == 0 && y == 0)
					return; // the both bars are already totaly shown
				else if(x == -HourWidthPx && y == -DayHeightPx)
					return; // the both bars are totaly hiden
				else
					handler.postDelayed(motion, delayMillis);
			}
		};
		
		public void startAnimationIfNecessary() {
			RelativeLayout.LayoutParams lpt = (RelativeLayout.LayoutParams) mImgViewTiming.getLayoutParams();
			RelativeLayout.LayoutParams lpd = (RelativeLayout.LayoutParams) mLayoutDays.getLayoutParams();
			if(lpt.leftMargin == 0 && lpd.topMargin == 0)
				return; // the both bars are already totaly shown
			else if(lpt.leftMargin == -HourWidthPx && lpd.topMargin == -DayHeightPx)
				return; // the both bars are totaly hide
			else
				handler.post(motion);
		}
		
		public void stopAnimation() {
			handler.removeCallbacks(motion);
		}
    }
}
