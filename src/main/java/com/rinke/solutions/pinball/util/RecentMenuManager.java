package com.rinke.solutions.pinball.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;

public class RecentMenuManager {
	
	private int nrecent = 8;

	private Menu menu;

	private Listener listener;

	private String prefix;
	
	Config config;
	
	public RecentMenuManager(String prefix, int nrecent, Menu menu, Listener l, Config c) {
		super();
		this.prefix = prefix;
		this.nrecent = nrecent;
		this.menu = menu;
		this.listener = l;
		this.config = c;
	}

	List<String> recentMenuItems = new ArrayList<>();
	
	public void loadRecent() {
		for( int i = 0; i < nrecent; i++) {
			String project = config.get(prefix+i);
			if( project != null ) {
				recentMenuItems.add(project);
			}
		}
		recreateMenu(recentMenuItems);
	}
	
	private void saveRecent() {
		for( int i = 0; i < Math.min(nrecent, recentMenuItems.size()); i++) {
			config.put(prefix+i, recentMenuItems.get(i));
		}
	}

	public void populateRecent(String file) {
		// repopulate remove oldest
		Iterator<String> i = recentMenuItems.iterator();
		while( i.hasNext() ) {
			String recentItem = i.next();
			if( recentItem.equals(file)) {
				i.remove();
				break;
			}
		}
		// add and remove oldest
		recentMenuItems.add(0, file);
		if( recentMenuItems.size() > nrecent ) recentMenuItems.remove(nrecent);
		saveRecent();
		// reorder also in Menu
		recreateMenu(recentMenuItems);
	}

	private void recreateMenu(List<String> files) {
		for (MenuItem item : menu.getItems()) {
			item.dispose();
		}
		for(String recent: files) {
			MenuItem item = new MenuItem(menu, SWT.NONE);
			item.setData(recent);
			item.setText(recent);
			item.setEnabled(true);
			item.addListener(SWT.Selection, listener);
		}
	}

	public void setConfig(Config config) {
		this.config = config;
	}


}
