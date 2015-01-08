package com.pijodev.insatpe;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/** Represents a set of entries corresponding to the days in a week **/
public class WeekEntries 
{
	/* ----------------------------------------------------------------------------------
	 * Variables
	 * --------------------------------------------------------------------------------*/
	public ArrayList<Entry>[] mDays;
	private int mYear;
	private int mWeekOfYear;
	private int mGroupId;
	private Date weekDate; // corresponding to monday's date ; can be null if the weekentries is empty
	
	private long cacheDate; // Date of cache update, in ms since 1970; by default, initialized at the current date
	
	/* ----------------------------------------------------------------------------------
	 * Accessors
	 * --------------------------------------------------------------------------------*/
	public int getYear()
	{
		return mYear;
	}
	/** Gets the week of year value of the first day of this week. */
	public int getWeekOfYear()
	{
		return mWeekOfYear;
	}
	public int getGroupId() {
		return mGroupId;
	}
	public Date getWeekDate() {
		// TODO : returns the date with the mYear and the mWeekOfYear
		return weekDate;
	}
	public long getCacheDate() {
		return cacheDate;
	}
	/** Returns the difference between the current date and the date of cache **/
	public String getDateCacheToString() {
		long diff = (new Date().getTime() - cacheDate) / (3600*1000); // diff time in hours

		if(diff == 0)
			return "moins d'une heure";
		else if(diff < 24)
			return "" + diff + " heure" + (diff==1?"":"s");
		else if((diff/=24) < 7)
			return "" + diff + " jour" + (diff==1?"":"s");
		else
			return "" + (diff/7) + " semaine" + ((diff/7)==1?"":"s") + (diff%7==0?"" : " et "+ (diff%7) + " jour" + ((diff%7)==1?"":"s") + "\nAttention données potentiellement obsolètes !");
		
	}
	
	/** Gets the year id of the first day of this week. */
	public void setYear(int year) {
		mYear = year;
	}
	public void setWeekOfYear(int weekOfYear) {
		mWeekOfYear= weekOfYear;
	}
	public void setGroupId(int groupId) {
		mGroupId = groupId;
	}
	public void setWeekDate(Date date) {
		weekDate = date;
	}
	public void setCacheDate(long date) {
		cacheDate = date;
	}
	
	/** Gets the WeekId corresponding to this Week.r */
	public long getWeekId()
	{
		return WeekEntriesCache.getWeekId(mYear, mWeekOfYear, mGroupId);
	}
	/* ----------------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------------*/
	/** Creates a new WeekEntries instance.
	 * @param year
	 * @param weekId
	 */
	@SuppressWarnings("unchecked")
	public WeekEntries()
	{
		cacheDate = new Date().getTime();
		mDays = new ArrayList[5];
		for(int i = 0; i < 5; i++)
		{
			mDays[i] = new ArrayList<Entry>();
		}
	}
	/* ----------------------------------------------------------------------------------
	 * Save / Load
	 * --------------------------------------------------------------------------------*/
	/** Loads the entries in the given Stream */
	public static WeekEntries Load(DataInputStream dis) throws IOException
	{
		WeekEntries entries = new WeekEntries();
		
		entries.cacheDate = dis.readLong();
		entries.mYear = dis.readInt();
		entries.mWeekOfYear = dis.readInt();
		entries.mGroupId = dis.readInt();
		if(dis.readBoolean()) {
			entries.weekDate = new Date(dis.readInt(), dis.readInt(), dis.readInt());
		}
		int lengh = dis.readInt();
		for(int day = 0; day < lengh; day++)
		{
			int size = dis.readInt();
			for(int i = 0; i < size; i++)
			{
				Entry e = new Entry();
				e.mStartTime = dis.readInt();
				e.mEndTime = dis.readInt();
				e.mClassName = dis.readUTF();
				e.mRoomName = dis.readUTF();
				e.mColor = dis.readInt();
				entries.mDays[day].add(e);
			}
		}
		return entries;
	}
	/** Saves the current WeekEntries to the given output stream */
	public void Save(DataOutputStream dos) throws FileNotFoundException, IOException
	{
		// Writes the basic information about the entry.
		dos.writeLong(cacheDate);
		dos.writeInt(mYear);
		dos.writeInt(mWeekOfYear);
		dos.writeInt(mGroupId);
		dos.writeBoolean(weekDate != null);
		if(weekDate != null) {
			dos.writeInt(weekDate.getYear());
			dos.writeInt(weekDate.getMonth());
			dos.writeInt(weekDate.getDate());
		}
		// Writes the size of each day, and then the entries data :
		int lengh = mDays.length;
		dos.writeInt(lengh);
		for(int day = 0; day < lengh; day++)
		{
			int size = mDays[day].size();
			// Writes the size of the following array.
			dos.writeInt(size);
			for(int i = 0; i < size; i++)
			{
				Entry e = mDays[day].get(i);
				dos.writeInt(e.mStartTime);
				dos.writeInt(e.mEndTime);
				dos.writeUTF(e.mClassName);
				dos.writeUTF(e.mRoomName);
				dos.writeInt(e.mColor);
			}
		}
	}

	/* ----------------------------------------------------------------------------------
	 * Content Comparator
	 * --------------------------------------------------------------------------------*/
	public boolean equal(WeekEntries we) {
		for(int i = 0; i < 5; i++)
		{
			ArrayList<Entry> day = mDays[i];
			ArrayList<Entry> weday = we.mDays[i];
			
			if(day.size() != weday.size())
				return false;
			
			for(int j = day.size()-1; j >= 0; j--)
				if(!day.get(j).equal(weday.get(j)))
					return false;
		}
		return true;
	}
	
}
