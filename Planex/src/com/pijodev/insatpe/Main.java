package com.pijodev.insatpe;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.System;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pijodev.insatpe.WeekEntriesCache.WeekEntriesRequestResult;



public class Main extends Activity {
	
	/* -------------------------------------------------------------------
	 * Variables
	 * -----------------------------------------------------------------*/
	WeekEntries mCurrentEntries;
	static Handler mHandler;
	/** Parameters (year, weekOfMonth) used in LoadDataAsync and which are set when we change
	 * the current week in setDesiredWeekIdParams*/
	int [] desiredWeekIdParams = new int[2];
	/** Parameter indicating the next desired week */
	int mDesiredWeek = 0;
	/** Parameter indicating the next desired group and its year */
	int mDesiredGroupNumero = 0, mDesiredYearNumero = 0;
	int mDesiredGroupId = 0;
	
	/** Layouts created outside of the main thread, that must be added 
	 *  in the main thread. */
	LinearLayout[][] mLayoutAddQueue;
	String[] mDateAddQueue = new String[5];
	/** Views **/
	ImageButton imgButtonPrev; // action button for getting the previous week
	ImageButton imgButtonCurrent;// action button for getting the current week
	ScheduleView mScheduleView;
	
	SubMenu mSubMenuListGroup; // Menu of the groupe selector
	
	/** Constantes **/
	static final int ADD_ENTRY = 0, CLEAR_VIEW = 1, MSG_ERROR = 4, LOADING = 5, MSG_ALERT = 6;
	private float dpToPx;
	
	/** Provide the dialog box containing the list of the groups, and the group name and id **/
	ExpandableListGroups mExpListGroups;
	/** Numero -> name */
	//String[] mListGroups[];//mListGroups;
	/** Numero -> Id */
	//int[] mListGroupId[];
	
	/* -------------------------------------------------------------------
	 * Android callbacks
	 * -----------------------------------------------------------------*/
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        /** Checks for new cache version, clears the cache if necessary **/
        checkCacheVersion(Integer.parseInt(this.getResources().getString(R.string.cache_version)));
        
        /** Initialize data objects **/
        dpToPx = getResources().getDisplayMetrics().densityDpi / 160f;
        mScheduleView = new ScheduleView(this);
        
        setupExpandableListGroups();// must be call in first
        
        setupGroupButtonListener();
        setupWeekButtonListener();
        setupGroupIdTextListener();
        setupHandler();
        
        // Set new params for the desired week
     	setDesiredWeekIdParams(0);
     	
     	// Initialize the popup view
     	Entry.initPopup(this);
    }
    @Override
    protected void onStart() {
    	super.onStart();
    	
        // Updates the desired group and the interface (text on the button)
     	mDesiredGroupId = getIdGroup();
     	if(mDesiredGroupId == -1) {
     		mDesiredGroupId = mExpListGroups.getGroupId(0,0);
     		mDesiredYearNumero = 0;
     		mDesiredGroupNumero = 0;
     	} else {
     		int grp[] = mExpListGroups.getGroupIndex(mDesiredGroupId);
     		mDesiredYearNumero = grp[0];
     		mDesiredGroupNumero = grp[1];
     	}
        setGroupTexts();
        
        setupWeekEntriesCache();
    }
    @Override
    protected void onStop() {
    	super.onStop();
    	try {
			WeekEntriesCache.SaveCache(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	saveSelectedGroupId(mDesiredGroupId);
    }
    private boolean scrollInitialized = false;
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	// Initialize the scroll of the schedule view only the first time
    	if(hasFocus && !scrollInitialized) {
        	mScheduleView.init();
        	scrollInitialized = true;
    	}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	
    	mScheduleView.changeOrientation(newConfig.orientation);
    	mScheduleView.resetScrollViews();//initScroll();
    }
    
    /* ----------------------------------------------------------------------------
     * Setup
     * --------------------------------------------------------------------------*/
    /** Initializes the WeekEntriesCache : tries to load the current week's planning */
    private void setupWeekEntriesCache()
    {
        // Prepares the calendar for further actions :
        // The calendar contains the date of this week's monday
		Date date = getMondayOfCurrentWeek();
        GregorianCalendar calendar = new GregorianCalendar(date.getYear()+1900, date.getMonth(), date.getDate());
		calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
		long weekId = WeekEntriesCache.getWeekId(calendar.get(GregorianCalendar.YEAR),
				calendar.get(GregorianCalendar.WEEK_OF_YEAR), mDesiredGroupId);
    	
		// Loads the entries first only if it exists in the cache.
    	// Otherwise it will be first loaded async (with a latency delay).
    	if(WeekEntriesCache.cacheContains(weekId, this))
    	{
    		LoadSchedule(WeekEntriesCache.Cached(this, calendar.get(GregorianCalendar.YEAR),
    				calendar.get(GregorianCalendar.WEEK_OF_YEAR), mDesiredGroupId, false).we, false);
    		
            // Asynchroneous loading of data.
            SeekDataAsync(false, true, false, false);
    	}
    	else
    		SeekDataAsync(true, false, false, false);
    }
    private void setupGroupButtonListener() {
    	((Button)findViewById(R.id.b_group)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mExpListGroups.show(mDesiredYearNumero);
		}});
    }
    private void setupWeekButtonListener() {
    	imgButtonPrev = ((ImageButton)findViewById(R.id.ib_prev));
    	imgButtonPrev.setEnabled(false);
    	imgButtonPrev.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				gotoPrevWeek();
		}});
    	
    	imgButtonCurrent = ((ImageButton)findViewById(R.id.ib_current));
    	imgButtonCurrent.setEnabled(false);
    	imgButtonCurrent.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			gotoCurrentWeek();
		}});
    	
    	((ImageButton)findViewById(R.id.ib_next)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
	    		gotoNextWeek();
		}});
    }
    private void setupGroupIdTextListener() {
    	EditText edit = (EditText) findViewById(R.id.et_group_id);
    	edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_GO) {
					mDesiredGroupId = Integer.parseInt("0"+v.getText());
		     		int grp[] = mExpListGroups.getGroupIndex(mDesiredGroupId);
		         	mDesiredYearNumero = grp[0];
		         	mDesiredGroupNumero = grp[1];
					SeekDataAsync(true, false, false, false);
	    			setGroupTexts();
				}
				return false;
		}});
    }
    /** Initialize the image views : timing and day */
    
    volatile Mutex mInterfaceMutex = new Mutex();
    /**    
     * Create a handler to be able to modify the content view from another thread
     **/
    private void setupHandler()
    {
    	mHandler = new Handler() {
    		@Override
    		public void handleMessage(Message msg) {
    			mInterfaceMutex.takeMutex();
    			// ---- Trade safe zone ----
    			switch(msg.what) {
    			case CLEAR_VIEW:
    				mScheduleView.clearEntries();
    				break;
    			case ADD_ENTRY:
    				mScheduleView.addWeekEntriesView(mLayoutAddQueue, mDateAddQueue);
    				break;
    			case MSG_ERROR:
    				break;
    			case MSG_ALERT:
    				Toast.makeText(Main.this, (String)msg.obj, Toast.LENGTH_SHORT).show(); 
    				break;
    			case LOADING:
    				mScheduleView.addLoadingView();
    				break;
    			}
    			
    			// ---- End of Thread safe zone ----
    			mInterfaceMutex.releaseMutex();
    		}
    	};
    	
    }
    private void setupExpandableListGroups() {
    	mExpListGroups = new ExpandableListGroups(this, new ExpandableListGroups.OnGroupSelectedListener() {
			@Override
			public void onGroupSelected(int groupPosition, int childPosition, long id) {
				mDesiredYearNumero = groupPosition;
				mDesiredGroupNumero = childPosition;
				mDesiredGroupId = mExpListGroups.getGroupId(groupPosition, childPosition);
				
    			SeekDataAsync(true, false, false, false);
    			setGroupTexts();
			}
		});
    }
    private void setGroupTexts() {
    	if(mDesiredGroupNumero == -1 || mDesiredYearNumero == -1) {
			((Button) Main.this.findViewById(R.id.b_group)).setText("Autre");
			((EditText) Main.this.findViewById(R.id.et_group_id)).setText(""+mDesiredGroupId);
    	} else {
    		((Button) Main.this.findViewById(R.id.b_group))
    				.setText(""+(mDesiredYearNumero+1) + " " + mExpListGroups.getGroupName(mDesiredYearNumero, mDesiredGroupNumero));
			((EditText) Main.this.findViewById(R.id.et_group_id))
					.setText(""+mExpListGroups.getGroupId(mDesiredYearNumero, mDesiredGroupNumero));
    	}
    }
    
    volatile int threadsCount = 0;
    /* --------------------------------------------------------------------------------
     * Data loading and refreshing
     * ------------------------------------------------------------------------------*/
    /** Called to seek the schedule from ADE
     * @param showOnlyChange : update the view only if the data have been loaded from ADE and are different to the cached data */
    void SeekDataAsync(final boolean displayLoadingView, final boolean showOnlyChange, final boolean gettingNextWeek, final boolean gettingPrevWeek)
    {
    	Thread thread = new Thread() {
    		public void run() {
    			// Wait for the thread to be released. 
    			threadsCount++;
    			if(threadsCount != 1)
    			{
    				// Memorizes the count of threads at this moment.
    				int thisCount = threadsCount;
    				while(threadsCount != 0) // while the main thread has not exited.
    				{
    					// Delete this thread if another one has been requested after this one.
    					if(threadsCount > thisCount)
    						return;
    					
    					try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
    				}
    			}
    			threadsCount = 1;
    			if(displayLoadingView) {
    				if(gettingNextWeek || gettingPrevWeek)
    					mHandler.sendEmptyMessage(CLEAR_VIEW);
    				else
    					mHandler.sendEmptyMessage(CLEAR_VIEW);
	    			mHandler.sendEmptyMessage(LOADING);
    			}
    			
    			// load the week from ADE (could be long) or from the cache
    			WeekEntriesRequestResult result = WeekEntriesCache.Cached(Main.this,
    					desiredWeekIdParams[0], desiredWeekIdParams[1], mDesiredGroupId, true);
    			
    			// Show an empty schedule 'cause nothing has been found
    			if(!result.requestFound || result.we == null) {
					LoadSchedule(new WeekEntries(), true);
					//Toast.makeText(Main.this, result.requestFound?"":"", Toast.LENGTH_SHORT).show();
    			}
				// Update the view if needed
    			else if(!showOnlyChange || result.changeFromADE) {
					LoadSchedule(result.we, true);
    			}
    			
    			// Show an alert message if the data comes from the cache
				if(result.loadedFromCache) {
					Message msg = new Message();
					msg.obj = "Récupération des données mises en cache il y a " + result.we.getDateCacheToString();
					msg.what = MSG_ALERT;
					mHandler.sendMessage(msg);
				}
    			
    			threadsCount = 0;
    		};
    	};
    	
    	thread.start();
    }
    /** Loads a schedule whose data has been retrieved by SeekDataAsync(). **/
    @SuppressLint("HandlerLeak")
    /** Loads the schedule views
     * useHandler must be true if LoadSchedule is not called by the Main thread.
     * @param entries
     * @param useHandler
     */
	void LoadSchedule(WeekEntries entries, boolean useHandler)
    {
		mInterfaceMutex.takeMutex();
		
		// ---- Trade safe zone ----
    	mCurrentEntries = entries;
    	mLayoutAddQueue = new LinearLayout[5][];

		Date date = (Date) (entries.getWeekDate()!=null ? entries.getWeekDate().clone() : null);
		
    	// Create entry views
    	for(int day = 0; day < 5; day++) {
    		int size = entries.mDays[day].size();
			mLayoutAddQueue[day] = new LinearLayout[size];
			// Create view
    		for(int i = 0; i < size; i++)
    			if(date != null)
    				mLayoutAddQueue[day][i] = entries.mDays[day].get(i).createView(this, day, date.getDate(), date.getMonth(), date.getYear()+1900);
    			else
    				mLayoutAddQueue[day][i] = entries.mDays[day].get(i).createView(this);
    		// Set formated string
    		if(date != null) {
    			mDateAddQueue[day] = String.format("%02d/%02d/%d", date.getDate(), date.getMonth()+1, date.getYear()+1900);
    			date.setDate(date.getDate()+1);
    		} else {
    			mDateAddQueue[day] = "";
    		}
    	}

    	// ---- End of trade safe zone ----
    	mInterfaceMutex.releaseMutex();
    	
    	// If using handler, the entries adding will not be performed in the current thread.
    	if(useHandler) {
	    	// Destroy the current loading views (entries) in the main view
	    	mHandler.sendEmptyMessage(CLEAR_VIEW);
	    	// Creates the entries in the main view
	    	mHandler.sendEmptyMessage(ADD_ENTRY);
    	}
    	else {
    		mScheduleView.clearEntries();
    		mScheduleView.addWeekEntriesView(mLayoutAddQueue, mDateAddQueue);
    	}
    }
    
    /** Gets the group id from the preferences **/
    int getIdGroup() {
    	return this.getSharedPreferences("groupe", MODE_PRIVATE).getInt("idGroupe", -1);
    }
    /** Saves the group id in the preferences **/
    void saveSelectedGroupId(int id) {
    	SharedPreferences prefs = this.getSharedPreferences("groupe", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt("idGroupe", id);
		editor.commit();
    }
   
    /** Returns the date corresponding to Monday of the current week **/
    public static Date getMondayOfCurrentWeek() {
    	Date date = new Date();
    	switch(date.getDay()) {
    	case 0: // dimanche : semaine planning = semaine suivante
    		date.setDate(date.getDate()+1);
    		break;
    	case 6: // samedi : semaine planning = semaine suivante
    		date.setDate(date.getDate()+2);
    		break;
    	default:
    		date.setDate(date.getDate() - (date.getDay()-1));
    		break;
    	}
    	
    	return date;
    }
    /**
     * Sets the current week to the current one + nbWeek.
     * An initialization at 0 is necessary to get the function work
     * before any further use.
     * @param nbWeek
     */
    void setDesiredWeekIdParams(int nbWeek) {
    	mDesiredWeek += nbWeek;
    	Date date = getMondayOfCurrentWeek();
    	date.setDate(date.getDate() + mDesiredWeek*7);
		// new GrCal([since 0 J-C], [0~11], [1~31])
        GregorianCalendar calendar = new GregorianCalendar(date.getYear()+1900, date.getMonth(), date.getDate());
		calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
		desiredWeekIdParams[0] = calendar.get(GregorianCalendar.YEAR);
		desiredWeekIdParams[1] = calendar.get(GregorianCalendar.WEEK_OF_YEAR);
		((TextView)findViewById(R.id.tv_week)).setText(String.format("Semaine %02d", desiredWeekIdParams[1]));
    }
    
    /** Sets views and starts loading for the next week  **/
    void gotoNextWeek() {
    	setDesiredWeekIdParams(+1);
		SeekDataAsync(true, false, true, false);
    	if(!imgButtonPrev.isEnabled())
    		imgButtonPrev.setEnabled(true);
		if(!imgButtonCurrent.isEnabled())
			imgButtonCurrent.setEnabled(true);
    }
    /** Sets the views and starts loading for the previous week **/
    void gotoPrevWeek() {
    	if(mDesiredWeek <= 0)
    		return;
    	
		setDesiredWeekIdParams(-1);
		SeekDataAsync(true, false, false, true);
		if(imgButtonPrev.isEnabled() != (mDesiredWeek != 0))
			imgButtonPrev.setEnabled(mDesiredWeek != 0);
		if(imgButtonCurrent.isEnabled() != (mDesiredWeek != 0))
			imgButtonCurrent.setEnabled(mDesiredWeek != 0);
    }
    /** Sets the views and starts loading for the current week **/
    void gotoCurrentWeek() {
    	if(mDesiredWeek <= 0)
    		return;
    	
    	mDesiredWeek = 0;
		setDesiredWeekIdParams(0);
		SeekDataAsync(true, false, false, true);
		if(imgButtonPrev.isEnabled())
			imgButtonPrev.setEnabled(false);
		if(imgButtonCurrent.isEnabled())
			imgButtonCurrent.setEnabled(false);
    }
    

	int convertDpToPx(float dp) {
		return (int)(dpToPx*dp + 0.5f);
	}
	class Mutex
	{
		boolean isHandled;
		public Mutex()
		{
			isHandled = false;
		}
		/*synchronized*/ public void takeMutex()
		{
			if(isHandled)
			{
				/*try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				/**/
				while(isHandled)
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				//*/
			}
			isHandled = true;
		}
		/*synchronized*/ public void releaseMutex()
		{
			isHandled = false;
			/*this.notify();*/
		}
	}
	
	/** Version manager (for the cache) **/
	void checkCacheVersion(int currentVersion) {
    	int savedVersion = this.getSharedPreferences("cache_version", MODE_PRIVATE).getInt("version", -1);

		if(savedVersion == -1) {
			AlertDialog.Builder adb = new Builder(this);
			adb.setTitle("Bienvenu(e) dans Planex !");
			adb.setMessage(
					"Les nouveautés :\n" +
						"\t-Cliquez pour afficher les détails\n" +
						"\t-Esthétisme++;\n" +
						"\t-Affichage dynamique des barres latérales\n" +
						"\t-Centrage automatique sur le jour actuel au lancement\n" +
						"\t-Passez en mode paysage pour la vue globale (bêta)\n" +
						"\t-Mise à jour de la liste des groupes 2013-2014\n" +
						"\t-Improves performance and stability\n" +
						"\t-Minor bug fixes...\n"+
					"\n\n" +
					"Remarques :\n\n" +
					"\tNous déclinons toutes responsabilités face aux éventuelles" +
					" mésaventures liées à une utilisation abusive du (performant)" +
					" système de cache.\n" +
					"\tAvant de poursuivre, assurez-vous d'avoir mis" +
					" 5 étoiles et un gentil commentaire sur le Play Store." +
					"\n\nEnjoy ! - Le Club Info de l'INSA de Toulouse");
			adb.setNeutralButton("Let's go !", null);
			adb.create().show();
		}
    	if(savedVersion != currentVersion) {
    		this.deleteFile("we-cache.dat");
    		updateCacheVersion(currentVersion);
    	}
	}
	void updateCacheVersion(int currentVersion) {
		SharedPreferences prefs = this.getSharedPreferences("cache_version", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt("version", currentVersion);
		editor.commit();
	}
}