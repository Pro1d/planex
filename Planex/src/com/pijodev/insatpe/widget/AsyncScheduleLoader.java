package com.pijodev.insatpe.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.pijodev.insatpe.Entry;
import com.pijodev.insatpe.Main;
import com.pijodev.insatpe.WeekEntriesCache;

public class AsyncScheduleLoader extends AsyncTask < String, Integer, Integer > {
	private static final int RESULT_SUCCESS = 0x00;
	private static final int RESULT_FAILED = 0x01;
	private static final int RESULT_CANCELLED = 0x02;
	
	
	private Handler handler;
	private Context context;
	private Runnable runnableOnSuccess;
	private Runnable runnableOnFail;
	
	ArrayList<Entry> schedule = null;

	public AsyncScheduleLoader(Context c, Handler h, Runnable runnableOnSuccess, Runnable runnableOnFail)
	{
		this.context = c;
		this.handler = h;
		this.runnableOnSuccess = runnableOnSuccess;
		this.runnableOnFail = runnableOnFail;
	}

	@Override
	protected Integer doInBackground(String... params)
	{
		schedule = null;
		if (params.length != 1)
			return RESULT_FAILED;
		
		schedule = load(context, params[0]);
		
		if (isCancelled())
			return RESULT_CANCELLED;
		else if(schedule == null)
			return RESULT_FAILED;
		else
			return RESULT_SUCCESS;
	}

	@Override
	protected void onPostExecute(Integer result)
	{
		switch (result)
		{
		case RESULT_SUCCESS:
			this.handler.postDelayed(runnableOnSuccess, 100);
			break;
		case RESULT_FAILED:
		case RESULT_CANCELLED:
			this.handler.postDelayed(runnableOnFail, 100);
			break;
		}
	}
	
	public ArrayList<Entry> load(Context c, String strGroupId) {
		int groupId = Integer.valueOf(strGroupId);

		// Current year and week of year
		Date date = Main.getMondayOfCurrentWeek();
		GregorianCalendar calendar = new GregorianCalendar(date.getYear()+1900, date.getMonth(), date.getDate());
		calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
	
		int year = calendar.get(GregorianCalendar.YEAR);
		int weekOfYear = calendar.get(GregorianCalendar.WEEK_OF_YEAR);

		// Load data from cache or from ADE (Can be long)
		WeekEntriesCache.WeekEntriesRequestResult result = WeekEntriesCache.Cached(c, year, weekOfYear, groupId, true);

		if(!result.requestFound)
			return null;
		
		// Day of the week
		int day = new Date().getDay() - 1;
		if(day < 0 || day > 4) /*return null;/*/ day = 0;
		
		// Returns the schedule of today
		return result.we.mDays[day];
	}
}
