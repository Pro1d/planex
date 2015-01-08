package com.pijodev.insatpe.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.pijodev.insatpe.ExpandableListGroups;
import com.pijodev.insatpe.R;

public class WidgetConfigure extends Activity {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID; 
	private ExpandableListGroups mExpListGroups = null;
	private EditText mEditTextWidgetName;
	private EditText mEditTextGroupId;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.configure_widget);

    	// Get the app widget id
		Bundle extras = getIntent().getExtras();
		if (extras != null)
		    mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    	if(mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
			finish();
		
    	final Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_CANCELED, resultValue);
		
		// Gets EditText views
		mEditTextGroupId = (EditText) findViewById(R.id.configure_widget_id);
		mEditTextWidgetName = (EditText) findViewById(R.id.configure_widget_name);
		
		mExpListGroups = new ExpandableListGroups(this, new ExpandableListGroups.OnGroupSelectedListener() {
			@Override
			public void onGroupSelected(int groupPosition, int childPosition, long id) {
				mEditTextWidgetName.setText("" + (groupPosition+1) + " " + mExpListGroups.getGroupName(groupPosition, childPosition));
				mEditTextGroupId.setText(""+mExpListGroups.getGroupId(groupPosition, childPosition));
			}
		});
		
		// Image button which shows the expandable listview of the groups
		((ImageButton)findViewById(R.id.configure_widget_showlist)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mExpListGroups.show(-1);
			}
		});
		
		// Validation button 
    	((Button)findViewById(R.id.configure_widget_ok)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Context context = WidgetConfigure.this;
				
				/// Check input data
				if(mEditTextWidgetName.getText().length() == 0 || mEditTextGroupId.getText().length() == 0) {
					Toast.makeText(context, "Choisissez un groupe et un nom", Toast.LENGTH_SHORT).show();
					return;
				}
				
				// Save the preferences
				saveWidgetPrefs(context, mEditTextWidgetName.getText().toString(), Integer.parseInt(mEditTextGroupId.getText().toString()));
				
				// Set results and finish the this configure activity
				setResult(RESULT_OK, resultValue);
				finish();
				
				Log.i("###", "Create widget, call onUpdate");
				// Call the onUpdate method of the new widget
		        new WidgetProvider().onUpdate(context, AppWidgetManager.getInstance(context), new int[] {mAppWidgetId});
				/*
				// When the configuration is complete, get an instance of the AppWidgetManager by calling getInstance(Context):
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetConfigure.this);
				
				// Update the App Widget with a RemoteViews layout by calling updateAppWidget(int, RemoteViews):
				RemoteViews views = WidgetProvider.updateRemoteView(WidgetConfigure.this, appWidgetManager, data, mAppWidgetId);
									//new RemoteViews(WidgetConfigure.this.getPackageName(), R.layout.compatible_widget);//TODO compatible vs listview
				appWidgetManager.updateAppWidget(mAppWidgetId, views);
				
				// Finally, create the return Intent, set it with the Activity result, and finish the Activity:
				setResult(RESULT_OK, resultValue);
				
				finish();*/
			}
		});
    }
    
    private void saveWidgetPrefs(Context context, String title, int groupId) {
    	// Get the shared preferences editor
		SharedPreferences prefs = context.getSharedPreferences("widgets_cfg", Context.MODE_PRIVATE);
		Editor prefEditor = prefs.edit();
		
		// And save the params
		prefEditor.putString("title"+mAppWidgetId, title);
		prefEditor.putInt("id"+mAppWidgetId, groupId);
		prefEditor.commit();
    }
}
