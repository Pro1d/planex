package com.pijodev.insatpe;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Set;

import android.content.Context;
import android.content.res.Resources.NotFoundException;

public class WeekEntriesCache 
{
	/* ----------------------------------------------------------------------------------
	 * Variables
	 * --------------------------------------------------------------------------------*/
	/** Cache containing the Day of Week (Monday etc...) in function of the Date */ 
	static HashMap<Long, Integer> sDayOfWeekMappingCache = new HashMap<Long, Integer>();
	
	/** HashMap containing the WeekEntries in function of their Id (see WeekEntries.getId()) */
	static HashMap<Long, WeekEntries> sWeekEntriesById = new HashMap<Long, WeekEntries>();
	
	static boolean hasBeenLoaded = false;
	/* ----------------------------------------------------------------------------------
	 * Utils / cached utils
	 * --------------------------------------------------------------------------------*/
	/** Gets the day of week (Monday etc...) of the given date */
	public static int getDayOfWeek(int year, int month, int day)
	{
		// Checks the cache for an existing version of the data.
		long dateLong = Date.toLong(year, month, day);
		if(sDayOfWeekMappingCache.containsKey(dateLong))
		{
			return sDayOfWeekMappingCache.get(dateLong);
		}
		
		GregorianCalendar calendar = new GregorianCalendar(year, month, day);

		calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
		int dayOfWeek = calendar.get(GregorianCalendar.DAY_OF_WEEK);
		
		// Sunday ? Saturday ? We'll never go to school this days !!! (But changed to Monday to avoid bugs)
		if(dayOfWeek == GregorianCalendar.SUNDAY || dayOfWeek == GregorianCalendar.SATURDAY)
			dayOfWeek = GregorianCalendar.MONDAY;

		int trueDayOfWeek = dayOfWeek - GregorianCalendar.MONDAY; // The obtained day of week starts at index 0 (=Monday)
		
		sDayOfWeekMappingCache.put(dateLong, trueDayOfWeek);
		return trueDayOfWeek;
	}
	
	/** Gets the WeekId corresponding to this Week. */
	static long getWeekId(int year, int weekOfYear, int groupeId)
	{
		return (short)groupeId | (weekOfYear << 16) | ((long)year << 24);
	}
	/** Initializes the cache by loading it **/
	private static void initialize(Context context) {
		try {
			LoadCache(context);
		} catch (IOException e) {
			return;
		}
		
		// Get the current week
		java.util.Date date = Main.getMondayOfCurrentWeek();
        GregorianCalendar calendar = new GregorianCalendar(date.getYear()+1900, date.getMonth(), date.getDate());
		calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
		long weekId = WeekEntriesCache.getWeekId(calendar.get(GregorianCalendar.YEAR),
				calendar.get(GregorianCalendar.WEEK_OF_YEAR), 0);
		
		Update(weekId, context);
	}
	
	/* ----------------------------------------------------------------------------------
	 * Update
	 * --------------------------------------------------------------------------------*/
	/** Update the cache to fit the current week id, for the cache to adapt this behavior. */
	private static void Update(long weekId, Context context)
	{
		// Delete the entries older than this week.
		Set<Long> keySet = sWeekEntriesById.keySet();
		ArrayList<Long> toDelete = new ArrayList<Long>();
		
		for(int i = 0; i < sWeekEntriesById.size(); i++)
			for(long lg : keySet)
				if(lg < weekId)
					toDelete.add(lg);
		
		// Deletes the older entries.
		for(long lg : toDelete)
			sWeekEntriesById.remove(lg);

		// The cache has been modified; now save it
		try {
			SaveCache(context);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
	/* ----------------------------------------------------------------------------------
	 * Cache Save / Load
	 * --------------------------------------------------------------------------------*/
	/** Saves the cache.
	 * @throws FileNotFoundException **/
	public static void SaveCache(Context c) throws FileNotFoundException, IOException
	{
		FileOutputStream stream = c.openFileOutput("we-cache.dat", Context.MODE_PRIVATE);//MODE_WORLD_WRITEABLE | Context.MODE_WORLD_READABLE);
		DataOutputStream dos = new DataOutputStream(stream);
		
		dos.writeInt(sWeekEntriesById.size());
		for(long key : sWeekEntriesById.keySet())
		{
			dos.writeLong(key);
			sWeekEntriesById.get(key).Save(dos);
		}
		
		stream.close();
	}
	
	/** Loads the cache.
	 * @throws IOException **/
	private static void LoadCache(Context c) throws IOException
	{
		// Creates a new HashMap
		sWeekEntriesById = new HashMap<Long, WeekEntries>();
		
		FileInputStream stream;
		
		// Returns if the file does not exist.
		try {
			stream = c.openFileInput("we-cache.dat");
		} catch (FileNotFoundException e) {
			return;
		}
		
		DataInputStream dis = new DataInputStream(stream);
		int size = dis.readInt();
		for(int i = 0; i < size; i++)
		{
			long key = dis.readLong();
			WeekEntries entries = WeekEntries.Load(dis);
			sWeekEntriesById.put(key, entries);
		}
		
		stream.close();
	}
	
	/* ----------------------------------------------------------------------------------
	 * Entries loading
	 * --------------------------------------------------------------------------------*/
	/** Returns true if the cache contains the given week id */
	public static boolean cacheContains(long weekId, Context c)
	{
		if(!hasBeenLoaded)
			initialize(c);
		
		return sWeekEntriesById.containsKey(weekId);
	}
	/** Adds an item to the cache, removes the item if null, and saves the cache **/
	private static void setWeekEntries(long weekid, WeekEntries entries, Context c) {
		if(entries == null)
			sWeekEntriesById.remove(entries);
		else
			sWeekEntriesById.put(weekid, entries);
		
		try {
			SaveCache(c);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Returns the WeekEntries with the given weekId (in the year : 1-52). 
	 * The entries of the previous weeks are automatically deleted when calling
	 * this method.
	 * @param year
	 * @param weekOfYear
	 * @param reload : set to true to try to load the week from internet
	 * @return
	 */
	public static WeekEntriesRequestResult Cached(Context c, int year, int weekOfYear, int groupId, boolean tryToReload)
	{
		if(!hasBeenLoaded)
			initialize(c);

		long weekId = getWeekId(year, weekOfYear, groupId);
		WeekEntries we = null;
		
		// Try to load the week from ADE if it's asked
		if(tryToReload)
			try {
				we = LoadWeekEntries(c, weekOfYear, groupId);
			} catch (NotFoundException e) { e.printStackTrace();
			} catch (IOException e) { e.printStackTrace(); }
		
		// Loaded from ADE 
		if(we != null) {
			// If the loaded schedule is not empty
			if(we.getWeekId() >= 0) {
				// Update the cache
				if(sWeekEntriesById.containsKey(we.getWeekId()))
				{
					boolean areDifferent = !sWeekEntriesById.get(we.getWeekId()).equal(we);
					
					// Save the cache even there is not change, because the date has been updated
					setWeekEntries(we.getWeekId(), we, c);
					
					// The cache has been modified; now save it
					try {
						SaveCache(c);
					} catch (FileNotFoundException e) {
					} catch (IOException e) {
					}
					
					return new WeekEntriesRequestResult(we, true, false, areDifferent);
				}
				// Or create a new entry
				else {
					setWeekEntries(we.getWeekId(), we, c);
					// The cache has been modified; now save it
					try {
						SaveCache(c);
					} catch (FileNotFoundException e) {
					} catch (IOException e) {
					}
					return new WeekEntriesRequestResult(we, true, false, true);
				}
			}
			// the loaded schedule is empty
			else {
				setWeekEntries(we.getWeekId(), null, c);
				// The cache has been modified; now save it
				try {
					SaveCache(c);
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				}
				return new WeekEntriesRequestResult(null, false, false, true);
			}
		}
		// Load from cache
		else if(sWeekEntriesById.containsKey(weekId)) {
			return new WeekEntriesRequestResult(sWeekEntriesById.get(weekId), true, true, false);
		}
		// Can't find desired week...
		else 
			return new WeekEntriesRequestResult(null, false, false, false);
	}
	
	/** Loads and returns the WeekEntries with the given year and week id
	 * 
	 * @param year
	 * @param weekId
	 * @param context
	 * @return
	 * @throws IOException
	 */
	private static WeekEntries LoadWeekEntries(Context c, int weekOfYear, int groupId) 
			throws IOException
	{
		// Gets the response from Planning express
		URL url = new URL("http://www.etud.insa-toulouse.fr/planning/index.php?gid="+groupId+"&wid="+weekOfYear+"&ics=1&planex=2");
		HttpURLConnection connect = (HttpURLConnection)url.openConnection();

		if(connect.getResponseCode() != HttpURLConnection.HTTP_OK)
			return null;
		
		InputStream in = connect.getInputStream();// the longest execution time 
		
		String resp = "";
		int car;
		
		// Read the whole stream and put it in a String
		do {
			// TODO : Interrupt here if necessary
			car = in.read();
			if(car != -1)
				resp += Character.toString((char)car);
		} while(car != -1);
		
		connect.disconnect();
		//String resp = new java.util.Scanner(in).useDelimiter("\\A").next();
		
		WeekEntries we = ParseEntries(resp, groupId);
		// TODO : set the date of this week here
		/*GregorianCalendar gc = new GregorianCalendar();
		gc.set(GregorianCalendar.WEEK_OF_YEAR, weekOfYear);
		gc.setFirstDayOfWeek(GregorianCalendar.MONDAY);
		while(gc.get(GregorianCalendar.DAY_OF_WEEK) != GregorianCalendar.MONDAY)
			gc.set(GregorianCalendar.DAY_OF_MONTH, gc.get(GregorianCalendar.DAY_OF_MONTH)-1);
		we.setWeekDate(gc.getTime());*/
		return we;
	}
	
	/** Parses the given String to extract the information used to create
	 * a new WeekEntries instance, and returns it.
	 * @param data
	 * @return
	 */
	private static WeekEntries ParseEntries(String data, int groupId)
	{
		String[] entries = data.split("BEGIN:VEVENT\n");
		WeekEntries entriesOut = new WeekEntries();
		int weekYear = -1;
		int weekOfYear = -1;
		java.util.Date weekDate = null;
		// The first entry is useless
		for(int i = 1; i < entries.length; i++)
		{
			Entry entry = new Entry();
			int dayOfWeek = -1;

			// Separates the entry's lines.
			String[] lines = entries[i].split("\n");
			for(int j = 0; j < lines.length; j++)
			{
				String line = lines[j];
				int startIndex = line.indexOf(':');
				// Identifier of the field :
				String id = line.substring(0, startIndex);
				String str = line.substring(startIndex+1);
				
				// Now modify the entry in function of which id we got.
				if(id.compareTo("DTSTART") == 0)
				{
					int year = Short.parseShort(str.substring(0, 4));
					int month = Short.parseShort(str.substring(4, 6)) - 1; // moth starts at index 0 (january)
					int day = Short.parseShort(str.substring(6, 8));
					int hour = Short.parseShort(str.substring(9, 11))+2;
					int minute = Short.parseShort(str.substring(11, 13));

					GregorianCalendar c = new GregorianCalendar(year, month, day);
					
					// Apply the day light time if the date is concerned
					if(!c.getTimeZone().inDaylightTime(new java.util.Date(year-1900, month, day)))
						hour--;
					
					// Sets up information about the beginning of the week.
					if(weekYear == -1)
					{
						weekYear = year;
						c.setFirstDayOfWeek(GregorianCalendar.MONDAY);
						weekOfYear = c.get(GregorianCalendar.WEEK_OF_YEAR);
						
						// Look for the monday's date in this week
						while(c.get(GregorianCalendar.DAY_OF_WEEK) != GregorianCalendar.MONDAY)
							c.set(GregorianCalendar.DAY_OF_MONTH, c.get(GregorianCalendar.DAY_OF_MONTH)-1);
						weekDate = c.getTime();
					}
					
					// Converts to minutes since 8
					entry.mStartTime = minute + (hour - 8) * 60;
					
					// Day of week of this class
					dayOfWeek = getDayOfWeek(year, month, day);
				}
				else if(id.compareTo("DTEND") == 0)
				{
					int year = Short.parseShort(str.substring(0, 4));
					int month = Short.parseShort(str.substring(4, 6)) - 1; // moth starts at index 0 (january)
					int day = Short.parseShort(str.substring(6, 8));
					int hour = Short.parseShort(str.substring(9, 11))+2;
					int minute = Short.parseShort(str.substring(11, 13));

					GregorianCalendar c = new GregorianCalendar(year, month, day);
					
					// Apply the day light time if the date is concerned
					if(!c.getTimeZone().inDaylightTime(new java.util.Date(year-1900, month, day)))
						hour--;
					
					// Converts to minutes since 8
					entry.mEndTime = minute + (hour - 8) * 60;
				}
				else if(id.compareTo("LOCATION") == 0)
				{
					entry.mRoomName = str;
				}
				else if(id.compareTo("SUMMARY") == 0)
				{
					entry.mClassName = str;
				}
				else if(id.compareTo("COLOR") == 0)
				{
					int r = Integer.parseInt(str.substring(0, 3));
					int g = Integer.parseInt(str.substring(3, 6));
					int b = Integer.parseInt(str.substring(6, 9));
					
					entry.mColor = 0xff000000 | (r<<16) | (g<<8) | b;
				}
			}
			// Adds the entry in the good dayOfWeek.
			entriesOut.mDays[dayOfWeek].add(entry);
			
			
		}
		
		// Adds information about the week entries
		entriesOut.setWeekOfYear(weekOfYear); // = -1 si planning vide
		entriesOut.setYear(weekYear); // = -1 si planning vide
		entriesOut.setGroupId(groupId);
		// TODO: do it in LoadWeekEntries(), or not...
		entriesOut.setWeekDate(weekDate);
		
		
		return entriesOut;
	}
	
	
	/** Date object used to make easy comparisons in the HashTable */
	static class Date
	{
		public short Year;
		public byte Month;
		public byte Day;
		public Date(int year, int month, int day)
		{
			Year = (short)year;
			Month = (byte)month;
			Day = (byte)day;
		}
		/** Stores the Date object in a long */
		public long toLong()
		{
			return Day | (Month << 8) | (Year << 16); 
		}
		public static long toLong(int Year, int Month, int Day)
		{
			return Day | (Month << 8) | (Year << 16);
		}
		/** Construct a Date object from a long value */
		public static Date fromLong(long date)
		{
			return new Date(
					(int)(date & 0xff), 
					(int)((date >> 8) & 0xff),
					(int)(date >> 16));

		}
	}
	
	public static class WeekEntriesRequestResult {
		// Result properties
		public final boolean requestFound;
		public final boolean loadedFromCache;
		public final boolean changeFromADE; // must be set to false if loadedFromCache is true
		// Result data
		public final WeekEntries we;
		
		/**
		 * Create a new WeekEntries request result
		 * @param we the schedule data
		 * @param requestFound 
		 * @param loadedFromCache indicates if the schedule was already in the cache
		 * @param changeFromADE indicates if the cache has been changed after an update from ADE
		 */
		public WeekEntriesRequestResult(WeekEntries we, boolean requestFound, boolean loadedFromCache, boolean changeFromADE) {
			this.we = we;
			/*
			this.year = year;
			this.weekOfYear = weekOfYear;
			this.groupId = groupId;
			this.tryToReload = tryToReload;
			*/
			this.requestFound = requestFound;
			this.loadedFromCache = loadedFromCache;
			
			this.changeFromADE = changeFromADE;
		}
	}
}
