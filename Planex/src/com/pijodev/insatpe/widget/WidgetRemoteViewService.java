package com.pijodev.insatpe.widget;


import java.util.ArrayList;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.pijodev.insatpe.Entry;
import com.pijodev.insatpe.R;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class WidgetRemoteViewService extends RemoteViewsService {
	/*
	* So pretty simple just defining the Adapter of the listview
	* here Adapter is ListProvider
	* */
	
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		//int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		 
		return (new ListProvider(this.getApplicationContext(), intent));
	}
	/*@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
		 
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());
 
		int[] allWidgetIds = intent
				.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
 
		for(int widgetId : allWidgetIds)
		{
			if(widgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
				continue;
 
			RemoteViews remoteViews = new RemoteViews(this
					.getApplicationContext().getPackageName(),
					R.layout.widget);
 
			// Chargement
			remoteViews.setImageViewResource(R.id.widget_image, android.R.color.transparent);
			this.appWidgetManager.updateAppWidget(widgetId, remoteViews);
 
			// Clique sur l'image
			Intent clickIntentRefresh = new Intent(this.getApplicationContext(),
					ImageWidgetProvider.class);
			clickIntentRefresh.setData(Uri.withAppendedPath(Uri.parse("imgwidget://widget/id/"), String.valueOf(widgetId)));
 
			clickIntentRefresh.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			clickIntentRefresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
					new int[] { widgetId });
 
			PendingIntent pendingIntentRefresh = PendingIntent.getBroadcast(
					getApplicationContext(), 0, clickIntentRefresh,
					PendingIntent.FLAG_UPDATE_CURRENT);
 
			remoteViews.setOnClickPendingIntent(R.id.widget_image, pendingIntentRefresh);
 
			// Notre image aléatoire
			loadImage(remoteViews, widgetId, this.randomImg());
		}
 
		stopSelf();
 
		super.onStart(intent, startId);
	}*/
	
	/**
	* If you are familiar with Adapter of ListView,this is the same as adapter
	* with few changes
	*
	*/
	public class ListProvider implements RemoteViewsFactory { // RemoteViewsFactory require API level 11
		private ArrayList<ListItem> listItemList = new ArrayList<ListItem>();
		private Context context = null;
		private int appWidgetId;
		//private DataProvider data;
		private ArrayList<Entry> mSchedule = null; //data.load(context);
					
		 
		public ListProvider(Context context, Intent intent) {
			this.context = context;
			appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			
			SharedPreferences prefs = context.getSharedPreferences("widgets_cfg", Context.MODE_PRIVATE);
			//data = new DataProvider(prefs.getString("title"+appWidgetId, "???"), prefs.getInt("id"+appWidgetId, 0));
			
		}
		
		private void loadListItem() {
			listItemList.clear();
			if(mSchedule == null)
				return;
			
			for (Entry e : mSchedule) {//int i = 0; i < schedule.size(); i++) {
				ListItem listItem = new ListItem();
				
				listItem.mClass = e.mClassName;
				listItem.mDetails = e.HourToString() + " ; " + e.mRoomName;
				listItem.mColor = e.mColor;
				
				listItemList.add(listItem);
			}
		}
		
		public int getCount() {
			return listItemList.size();
		}
		
		public long getItemId(int position) {
			return position;
		}
		 
		/* Similar to getView of Adapter where instead of View  *
		 * we return RemoteViews 								*/
		public RemoteViews getViewAt(int position) {
			final RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.item_widget);
			ListItem listItem = listItemList.get(position);
			
			remoteView.setTextViewText(R.id.item_widget_summary, listItem.mClass);
			remoteView.setTextViewText(R.id.item_widget_details, listItem.mDetails);
			remoteView.setInt(R.id.item_widget_ll, "setBackgroundColor", listItem.mColor);
			
			return remoteView;
		}
		
		public RemoteViews getLoadingView() {
			return null;
		}
		
		public int getViewTypeCount() {
			return 1;//listItemList.size();
		}
		
		public boolean hasStableIds() {
			return false;
		}
		
		public void onCreate() {

		}

		
	    public void onDataSetChanged() {
	    	//WeekEntriesRequestResult r = WeekEntriesCache.Cached(contetc, year, weekOfYear, groupId, tryToReload)
	    	/*Thread t = new Thread(new Runnable() {
				public void run() {
					// TODO Auto-generated method stub
					
				}
			});
			// Start the loading and wait for its end
	    	t.start();
			
			try {
				t.join();
			} catch (InterruptedException e) { }*/
	    	// TODO
	    	// This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
	        // on the collection view corresponding to this factory. You can do heaving lifting in
	        // here, synchronously. For example, if you need to process an image, fetch something
	        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
	        // in its current state while work is being done here, so you don't need to worry about
	        // locking up the widget.
	    }
		
		public void onDestroy() {
		}
		
		
		class ListItem {
			String mClass;
			String mDetails;
			int mColor;
		}
	}
}