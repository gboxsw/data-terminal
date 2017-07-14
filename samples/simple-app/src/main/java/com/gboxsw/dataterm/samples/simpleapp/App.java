package com.gboxsw.dataterm.samples.simpleapp;

import java.util.*;
import com.gboxsw.dataterm.*;

/**
 * Simple application that demonstrates usage of the DataTerm framework.
 */
public class App {
	public static void main(String[] args) {
		// generate 40 data items
		List<SimpleItem> dataItems = new ArrayList<>();
		for (int i = 0; i < 40; i++) {
			dataItems.add(new SimpleItem("Item " + i, i % 3 == 0));
		}

		// create and configure terminal
		DataTerminal dataTerminal = new DataTerminal();
		for (SimpleItem tdi : dataItems) {
			dataTerminal.addDataItem(tdi);
		}

		dataTerminal.setStatusText("[ENTER] Edit");
		dataTerminal.setScreenRefreshPeriod(300);

		// launch the data terminal as an application
		dataTerminal.launch();
	}
}
