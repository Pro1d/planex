package com.pijodev.insatpe.widget;

import java.util.Calendar;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;

import com.pijodev.insatpe.R;

public class WidgetProvider extends AppWidgetProvider {
	
	// Service which update the widget
	private PendingIntent service = null; 
	
	/**
	* this method is called every XX mins as specified on widgetinfo.xml
	* this method is called on every phone reboot
	**/
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		
		// Création de l'alarme pour mettre à jour le widget
		final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		
		// On récupère les préférences
		SharedPreferences prefs = context.getSharedPreferences("widgets_cfg", Context.MODE_PRIVATE);
		
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		
		/*
		 * int[] appWidgetIds holds ids of multiple instance of your widget
		 * meaning you are placing more than one widgets on your homescreen
		 */
		for (int wid : appWidgetIds) {
			
			// Do not update the widget if it is still not configured 
			if(!prefs.contains("title"+wid) || !prefs.contains("id"+wid))
				continue;
			
			//if (currentapiVersion < 11/*android.os.Build.VERSION_CODES.HONEYCOMB*/)
				updateWidgetCompatibleView(context, wid, m);
			//else
			//	updateWidgetListView(context, wid, m); // TODO
		}
	}
	
	/** Returns the pre-configured view corresponding to the local android version ** /
	static public RemoteViews updateRemoteView(Context context, AppWidgetManager appWidgetManager, DataProvider data, int wid) {
		RemoteViews remoteViews;
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		
		
		/** Widget compatible for android before 3.0 Honeycomb (API level 11) ** /
        if (currentapiVersion < 11/*android.os.Build.VERSION_CODES.HONEYCOMB* /){
			remoteViews = updateWidgetCompatibleView(context, wid);
        }
        /** Widget with list view avaible since API level 11 ** /
        else  {
			remoteViews = updateWidgetListView(context, wid);
			appWidgetManager.notifyAppWidgetViewDataChanged(wid, remoteViews.getLayoutId());
			
			/** Widget on lockscreen available since android 4.2 Jelly bean (API level 17) ** /
        	if(currentapiVersion >= 17/*android.os.Build.VERSION_CODES.JELLY_BEAN_MR1* /) {
	        	Bundle myOptions = appWidgetManager.getAppWidgetOptions(wid);
	            // Get the value of OPTION_APPWIDGET_HOST_CATEGORY
	            int category = myOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
	            // If the value is WIDGET_CATEGORY_KEYGUARD, it's a lockscreen widget
	            boolean isKeyguard = (category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD);
	        }
        }
        
        remoteViews.setTextViewText(R.id.widget_title_tv, data.title);
        
		return remoteViews;
	}*/
	
	// Load the view and its content
	private void updateWidgetCompatibleView(Context context, int appWidgetId, AlarmManager alarm) {
		// Création de l'intent du service
		final Intent i = new Intent(context, WidgetService.class);
		// On passe l'id du widget
		i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
 
		/*
		 * Cette ligne de code permet de corriger un problème sur android
		 * qui met à jour uniquement le dernier widget lorsque vous en ajoutez
		 * plusieurs sur votre bureau. Ne le supprimez pas ;)
		 */
		i.setData(Uri.withAppendedPath(Uri.parse("imgwidget://widget/id/"), String.valueOf(appWidgetId)));
 
		service = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		
		final Calendar TIME = Calendar.getInstance();
		TIME.set(Calendar.MINUTE, 0); TIME.set(Calendar.SECOND, 0); TIME.set(Calendar.MILLISECOND, 0);
		
		// ceate the alarm, start the first time now (TIME.getTime().getTime() <=> now)
		alarm.setRepeating(AlarmManager.RTC, TIME.getTime().getTime(), 4 * 60*60 * 1000, service);
	}
	
	// Load the view and set the service adapter
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	static private RemoteViews updateWidgetListView(Context context, int appWidgetId, AlarmManager alarm) {
		// which layout to show on widget
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.listview_widget);

		// RemoteViews Service needed to provide adapter for ListView
		Intent svcIntent = new Intent(context, WidgetRemoteViewService.class);
		// passing app widget id to that RemoteViews Service
		svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		// setting a unique Uri to the intent; don't know its purpose to me right now
		svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
		// setting adapter to listview of the widget
		remoteViews.setRemoteAdapter(appWidgetId, R.id.widget_listview, svcIntent);
		// setting an empty view in case of no data
		remoteViews.setEmptyView(R.id.widget_listview, R.id.widget_emptyview);
		
		return remoteViews;
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		/*
		if(intent.getAction().equals("ScrollDown")) {
			int allWid[] = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			for(int wid : allWid)
				Log.i("###", "onReceive ScrollDown on "+wid);
		}
		else if(intent.getAction().equals("ScrollUp")) {
			int allWid[] = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			for(int wid : allWid)
				Log.i("###", "onReceive ScrollUp on "+wid);
		}
		*/
	}
    /*public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;
    	Log.i("###", "onUpdate"+N);

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            boolean isKeyguard;
            if (currentapiVersion < 17/*android.os.Build.VERSION_CODES.JELLY_BEAN* /){
                isKeyguard = false;
            } else{
            	Bundle myOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
                // Get the value of OPTION_APPWIDGET_HOST_CATEGORY
                int category = myOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
                // If the value is WIDGET_CATEGORY_KEYGUARD, it's a lockscreen widget
                isKeyguard = (category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD);
            }
            
           
            // Create an Intent to launch ExampleActivity
           // Intent intent = new Intent(context, ExampleActivity.class);
           // PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

           // views.setOnClickPendingIntent(R.id.button, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }*/
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("widgets_cfg", Context.MODE_PRIVATE);
		Editor prefEditor = prefs.edit();

		final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		for(int wid : appWidgetIds) {
			prefEditor.remove("title"+wid);
			prefEditor.remove("id"+wid);

			// On récupère l'intent à supprimer
			final Intent i = new Intent(context, WidgetService.class);
			i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { wid });
			i.setData(Uri.withAppendedPath( Uri.parse("imgwidget://widget/id/"), String.valueOf(wid)));
 
			service = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
 
			// On supprime l'alarme
			m.cancel(service);
		}
    	
		prefEditor.commit();
        super.onDeleted(context, appWidgetIds);
    }
    
}
