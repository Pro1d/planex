package com.pijodev.insatpe;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/** Represents an entry in the schedule **/
public class Entry {
	public String mClassName = "";
	public String mRoomName = "";
	
	/** Start time of the class, in minutes, since 8 o'clock **/
	public int mStartTime;
	/** End time of the class, in minutes, since 8 o'clock**/
	public int mEndTime;
	
	/** The background color of the view (0xAARRGGBB) **/
	public int mColor = 0xFFB6B6B6;// default color is grey
	
	/** Popup builder **/
	static AlertDialog dialog = null;
	static View popupLayout = null;
	/** Date for popup view **/
	private int day = 0, date = 0, month = 0, year = 0;
	
	/** Creates a new entry **/
	public Entry()
	{
		
	}
	
	/** Compare the content of the given Entry with this **/
	public boolean equal(Entry e) {
		return mClassName.compareTo(e.mClassName) == 0
				&& mRoomName.compareTo(e.mRoomName) == 0
				&& mStartTime == e.mStartTime
				&& mEndTime == e.mEndTime
				&& mColor == e.mColor;
	}
	
	/** Returns the created entry view **/
	public LinearLayout createView(Main context) {
		// Create and set the linear layout container
    	LinearLayout ll = new LinearLayout(context);
    	RelativeLayout.LayoutParams ll_params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    	ll_params.topMargin = mStartTime * ScheduleView.HourHeightPx / 60;
    	ll_params.height = (mEndTime-mStartTime) * ScheduleView.HourHeightPx / 60;
    	ll.setLayoutParams(ll_params);
    	ll.setGravity(Gravity.CENTER);
    	ll.setOrientation(LinearLayout.VERTICAL);
    	ll.setBackgroundColor(mColor == 0xFFFFFFFF ? 0xFFEEEEEE : mColor);//getBackground()
    	
    	// Create and set three textviews
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    	
    	if(mClassName.length() > 0) {
    		TextView tv_class = new TextView(context);
	    	tv_class.setText(mClassName);
	    	tv_class.setLayoutParams(params);
	    	tv_class.setTextColor(0xFF000000);
	    	tv_class.setTextSize(TypedValue.COMPLEX_UNIT_DIP, ScheduleView.EntryTextSizeDpi);
	    	tv_class.setGravity(Gravity.CENTER);
	    	tv_class.setTypeface(null, Typeface.BOLD);
	    	
	    	tv_class.setSingleLine(true);
	    	tv_class.setEllipsize(TruncateAt.END);
	    	
	    	ll.addView(tv_class);
    	}

    	if(mRoomName.length() > 0) {
	    	TextView tv_room = new TextView(context);
	    	tv_room.setText(mRoomName);
	    	tv_room.setLayoutParams(params);
	    	tv_room.setTextColor(0xFF000000);
	    	tv_room.setTextSize(TypedValue.COMPLEX_UNIT_DIP, ScheduleView.EntryTextSizeDpi);
	    	tv_room.setGravity(Gravity.CENTER);
	    	
	    	tv_room.setSingleLine(true);
	    	tv_room.setEllipsize(TruncateAt.END);
	    	
	    	ll.addView(tv_room);
    	}

    	TextView tv_hour = new TextView(context);
    	tv_hour.setText(HourToString());
    	tv_hour.setLayoutParams(params);
    	tv_hour.setTextColor(0xFF000000);
    	tv_hour.setTextSize(TypedValue.COMPLEX_UNIT_DIP, ScheduleView.EntryTextSizeDpi);
    	tv_hour.setGravity(Gravity.CENTER);
    	ll.addView(tv_hour);
    	
    	setOnClicListener(ll);
    	
    	return ll;
	}
	
	/** Returns the created entry view and the date **/
	public LinearLayout createView(Main context, int day, int date, int month, int year) {
		this.day = day;
		this.date = date;
		this.month = month;
		this.year = year;
		
		return createView(context);
	}
	
	private void setOnClicListener(LinearLayout view) {
		view.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Entry.this.showPopup();
			}
		});
	}
	
	/** Returns a formated string containing the hours looking like "8H00 - 10H00" **/
	@SuppressLint("DefaultLocale")
	public String HourToString() {
		int hourStart = mStartTime / 60 + 8;
		int minStart = mStartTime % 60;
		int hourEnd = mEndTime / 60 + 8;
		int minEnd = mEndTime % 60;
		
		return String.format("%dH%02d - %dH%02d", hourStart, minStart, hourEnd, minEnd);//"" + hourStart + "H" + (minStart < 10? "0":"") + minStart + " - " + hourEnd + "H" + (minEnd < 10? "0":"") + minEnd;
	}
	
	/** Returns a formated string containing the duration looking like "8H00" **/
	@SuppressLint("DefaultLocale")
	private String durationToString() {
		int hour = (mEndTime - mStartTime) / 60;
		int min = (mEndTime - mStartTime) % 60;
		
		return String.format("%dH%02d", hour, min);
	}
	
	static final String[] sday = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi"};
	static final String[] smonth = {"Janvier", "F�vrier", "Mars", "Avril", "Mai", "Juin", "Juillet", "Ao�t", "Septembre", "Octobre", "Novembre", "D�cembre"};
	/** Returns a formated string containing the date looking like "Vendredi 21 D�cembre 2012" **/
	private String dateToString() {
		if(date == 0)
			return "";
		return sday[day] + " " + date + " " + smonth[month] + " " + year;
	}
	
	// Build the static dialog
	static public void initPopup(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		popupLayout = LayoutInflater.from(context).inflate(R.layout.popup, null);
		
		builder.setView(popupLayout);
		builder.setNeutralButton("Fermer", null);
			
		dialog = builder.create();
	}
	
	void showPopup() {
		// modify the content of the static dialog view
		dialog.setTitle(mClassName);
		
		//popupLayout.findViewById(R.id.tv_popup_class).setText(mClassName);
		((TextView) popupLayout.findViewById(R.id.tv_popup_date)).setText("Date : " + dateToString());
		((TextView) popupLayout.findViewById(R.id.tv_popup_hour)).setText("Dur�e : " + durationToString() + " (" + HourToString() + ")");
		((TextView) popupLayout.findViewById(R.id.tv_popup_room)).setText("Salle : " + mRoomName);
		
		// Show the static dialog
		dialog.show();
	}
	
}
