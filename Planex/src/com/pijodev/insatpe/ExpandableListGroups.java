package com.pijodev.insatpe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.SimpleExpandableListAdapter;

public class ExpandableListGroups  {
	
	/** Numero -> name */
	private String[] mListGroups[] = null;
	/** Numero -> Id */
	private int[] mListGroupId[] = null;
	/** **/
	private AlertDialog mDialog = null;
	private ExpandableListView mExpListView = null;
	
	/** Shows the expadanble list view of the group in a dialog box.
	 * It will call the listener given in the class builder **/ 
	public void show(int yearFocus) {
		focusYear(yearFocus);
		
		mDialog.show();
	}

	/** Accessor to the list of the groups **/
	public String getGroupName(int year, int index) {
		return mListGroups[year][index];
	}
	/** Accessor to the list of the groups **/
	public int getGroupId(int year, int index) {
		return mListGroupId[year][index];
	}

    /** Returns the index of the group and the year in function of the id or {-1, -1} in case of unknown id **/
    public int[] getGroupIndex(int id) {
    	int grp[] = {-1,-1};
    	boolean found = false;
    	
    	for(int a = 0; a < 5 && !found; a++)
    	for(int i = 0; i < mListGroupId[a].length && !found; i++)
    		if(mListGroupId[a][i] == id) {
    			grp[0] = a;
    			grp[1] = i;
    			found = true;
    	}

    	return grp;
    }
	
	/** Expands the given group and collapse all the others **/
	private void focusYear(int yearFocus) {
		for(int i = mListGroups.length-1; i >= 0; i--)
			if(i != yearFocus && mExpListView.isGroupExpanded(i))
				mExpListView.collapseGroup(i);
		
	    if(yearFocus != -1 && !mExpListView.isGroupExpanded(yearFocus))
	    	mExpListView.expandGroup(yearFocus);
	}
	
	public ExpandableListGroups(Context context, final OnGroupSelectedListener listener) {
		// Create the diaog box
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle(context.getResources().getString(R.string.menu_groupe));
		ad.setCancelable(true);
        mDialog = ad.create();
		
        // Create the lists
		final String NAME = "NAME", IS_EVEN = "IS_EVEN";
		List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
        List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
        
        // Load the list if it still not done
        if(mListGroups == null)
        	loadListGroups(context);
        
        // Fill the lists
        for (int i = 0; i < 5; i++) {
            Map<String, String> curGroupMap = new HashMap<String, String>();
            groupData.add(curGroupMap);
            curGroupMap.put(NAME, "" + (1+i) + (i==0 ? "ère" : "ème") + " année");
            curGroupMap.put(IS_EVEN, "Année/PO");
            
            List<Map<String, String>> children = new ArrayList<Map<String, String>>();
	        for (int j = 0; j < mListGroups[i].length; j++) {
                Map<String, String> curChildMap = new HashMap<String, String>();
                children.add(curChildMap);
                curChildMap.put(NAME, "" + (i+1) + " " + mListGroups[i][j]);
                curChildMap.put(IS_EVEN, "" + mListGroupId[i][j]);
            }
            childData.add(children);
        }
        
        // Set up our adapter
        ExpandableListAdapter adapter = new SimpleExpandableListAdapter(context,
                groupData, android.R.layout.simple_expandable_list_item_1,
                new String[] {NAME, IS_EVEN}, new int[] { android.R.id.text1, android.R.id.text2 },
                childData, android.R.layout.simple_expandable_list_item_2,
                new String[] {NAME, IS_EVEN}, new int[] { android.R.id.text1, android.R.id.text2 });
        
        // Create the list view
        mExpListView = new ExpandableListView(context);
        mExpListView.setAdapter(adapter);

        mExpListView.setOnChildClickListener(new OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				// Call the given on group selected listener
				listener.onGroupSelected(groupPosition, childPosition, id);
				// Close the dialog box
    			mDialog.cancel();
    			return true;
		}});
        mExpListView.setOnGroupExpandListener(new OnGroupExpandListener() {
			public void onGroupExpand(int groupPosition) {
				focusYear(groupPosition);
			}
		});
        
        mDialog.setView(mExpListView);
	}
	/** Loads the list of the groups and id from the xml files **/
	private void loadListGroups(Context context) {
    	int arrayIdGroupeName[] = {R.array.group_name_1, R.array.group_name_2, R.array.group_name_3, R.array.group_name_4, R.array.group_name_5};
    	int arrayIdGroupeId[] = {R.array.group_id_1, R.array.group_id_2, R.array.group_id_3, R.array.group_id_4, R.array.group_id_5};
    	mListGroups = new String[5][];
    	mListGroupId = new int[5][];
    	
    	Resources res = context.getResources();
    	
    	for(int year = 0; year < 5; year++) {
    		mListGroups[year] = res.getStringArray(arrayIdGroupeName[year]);
    		mListGroupId[year] = res.getIntArray(arrayIdGroupeId[year]);
    	}
    }
	/**
	 * TODO loadListGroups
	 * 	-> www.etud.insa-toulouse.fr/planex/api.php?PIPIKK=Pr0ut!
	 * @author Proïd
	 *
	 */
	public static abstract class OnGroupSelectedListener {
		public abstract void onGroupSelected(int groupPosition, int childPosition, long id);
	}
}
