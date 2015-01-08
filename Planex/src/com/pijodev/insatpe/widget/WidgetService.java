package com.pijodev.insatpe.widget;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

import com.pijodev.insatpe.Entry;
import com.pijodev.insatpe.R;

public class WidgetService extends Service {
	AsyncScheduleLoader loader;
	protected boolean isLoaded = false;
	AppWidgetManager appWidgetManager;
	
	final static String EXTRA_REQUEST_INDEX = "scrollIndex", ACTION_SCROLL = "actionScroll";
 
	protected Handler handler = new Handler();
 
	SharedPreferences prefs;
	/** Array of list of item views for each widget (by widget id) **/ // TODO with CursorAdaptor android support library
	private SparseArray<ListItems> mWidgetListOfViews = new SparseArray<ListItems>();
 
	@Override
	public void onStart(Intent intent, int startId)
	{
		this.prefs = this.getSharedPreferences("widgets_cfg", Context.MODE_PRIVATE);
 
		this.appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());
 
		int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
		for(int widgetId : allWidgetIds)
		{
			if(widgetId == 0)
				continue;
			
			// Gets the index
			int curIndex = 0;//intent.getIntExtra(EXTRA_REQUEST_INDEX, 0);
			
			// CREATE THE VIEW HERE
			RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.compatible_widget);
			
			// Set to visible the text of "emptiness"
			remoteViews.setViewVisibility(R.id.widget_emptyview, View.VISIBLE);
			// Update the title
			remoteViews.setTextViewText(R.id.widget_title_tv, prefs.getString("title"+widgetId, "???"));

			// Update the widget without schedule
			this.appWidgetManager.updateAppWidget(widgetId, remoteViews);

			// Set the onclicklistener of the button next and previous
			Intent clickIntentScrollDown = new Intent(this.getApplicationContext(), WidgetProvider.class);
			clickIntentScrollDown.setData(Uri.withAppendedPath(Uri.parse("imgwidget://widget/id/"), String.valueOf(widgetId)));
			clickIntentScrollDown.setAction(ACTION_SCROLL);
			clickIntentScrollDown.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { widgetId });
			clickIntentScrollDown.putExtra(EXTRA_REQUEST_INDEX, curIndex + 1);
			
			PendingIntent pendingIntentScrollDown = PendingIntent.getBroadcast(getApplicationContext(), 0, clickIntentScrollDown, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.widget_button_next, pendingIntentScrollDown);

			// Set the onclicklistener of the button next and previous
			Intent clickIntentScrollUp = new Intent(this.getApplicationContext(), WidgetProvider.class);
			clickIntentScrollUp.setData(Uri.withAppendedPath(Uri.parse("imgwidget://widget/id/"), String.valueOf(widgetId)));
			clickIntentScrollUp.setAction(ACTION_SCROLL);
			clickIntentScrollUp.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { widgetId });
			clickIntentScrollUp.putExtra(EXTRA_REQUEST_INDEX, curIndex - 1);
			
			PendingIntent pendingIntentScrollUp = PendingIntent.getBroadcast(getApplicationContext(), 0, clickIntentScrollUp, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.widget_button_prev, pendingIntentScrollUp);
			
			// Click on the refresh button : update widget
			Intent clickIntentRefresh = new Intent(this.getApplicationContext(), WidgetProvider.class);
			clickIntentRefresh.setData(Uri.withAppendedPath(Uri.parse("imgwidget://widget/id/"), String.valueOf(widgetId)));
			clickIntentRefresh.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			clickIntentRefresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { widgetId });
			
			PendingIntent pendingIntentRefresh = PendingIntent.getBroadcast(getApplicationContext(), 0, clickIntentRefresh, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.widget_button_reload, pendingIntentRefresh);
 
			// Load the schedule
			loadSchedule(remoteViews, widgetId, String.valueOf(prefs.getInt("id"+widgetId, 0)));
		}
 
		stopSelf();
 
		super.onStart(intent, startId);
	}
 
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
 
	private void loadSchedule(final RemoteViews remoteViews, final int widgetId, String strGroupId)
	{
		this.loader = new AsyncScheduleLoader(this, this.handler,
			new Runnable() { // OnSuccess
				public void run() {
					updateAppWidgetEntry(widgetId, remoteViews, WidgetService.this.loader.schedule);
					//WidgetService.this.loader = null; 
				}
			},
			new Runnable() { // OnFail
				public void run() {
					updateAppWidgetEntry(widgetId, remoteViews, null);
					//WidgetService.this.loader = null;
				}
			});

		this.loader.execute(strGroupId);
		
		// Update the view : remove the list and show the progressBar
		remoteViews.removeAllViews(R.id.widget_listlayout);
		remoteViews.setViewVisibility(R.id.widget_emptyview, View.GONE);
		remoteViews.setViewVisibility(R.id.widget_progressbar, View.VISIBLE);
		
		this.appWidgetManager.updateAppWidget(widgetId, remoteViews);
	}
	
	public void updateAppWidgetEntry(int widgetId, RemoteViews remoteViews, ArrayList<Entry> schedule) {
		// Nothing has been loaded : show the emptyview
		if(schedule == null || schedule.size() == 0) {
			remoteViews.removeAllViews(R.id.widget_listlayout);
			remoteViews.setViewVisibility(R.id.widget_emptyview, View.VISIBLE);
			remoteViews.setViewVisibility(R.id.widget_progressbar, View.GONE);
		}
		// Make the list view with the new schedule
		else {
			remoteViews.setViewVisibility(R.id.widget_emptyview, View.GONE);
			remoteViews.setViewVisibility(R.id.widget_progressbar, View.GONE);
			for(int i = 0; i < schedule.size(); i++) {
				Entry e = schedule.get(i);
				
				RemoteViews rv = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.item_widget);
				//rv.setInt(R.id.item_widget_ll, "setId", i);
				rv.setTextViewText(R.id.item_widget_summary, e.mClassName);
				rv.setTextViewText(R.id.item_widget_details, e.HourToString() + ((e.mRoomName.length() != 0) ? " ; " + e.mRoomName : ""));
				rv.setInt(R.id.item_widget_ll, "setBackgroundColor", e.mColor);
				
				remoteViews.addView(R.id.widget_listlayout, rv);
			}
		}
		// Update the widget view
		this.appWidgetManager.updateAppWidget(widgetId, remoteViews);
	}
	
	static class ListItems {
		RemoteViews views[];
		int posScroll;
	}
}
