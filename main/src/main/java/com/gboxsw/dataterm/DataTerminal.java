package com.gboxsw.dataterm;

import java.io.IOException;
import java.util.*;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;

/**
 * The data terminal that can be configured and then executed as an application.
 */
public class DataTerminal {

	/**
	 * Period (in milliseconds) of key stroke checking.
	 */
	private static final long KEY_CHECK_PERIOD_MS = 20;

	/**
	 * Internal data about one row (a data item).
	 */
	private static class DataRow {
		/**
		 * Data item displayed in the data row.
		 */
		final BaseItem dataItem;

		/**
		 * Label displayed in data row.
		 */
		String label;

		/**
		 * Value displayed in data row.
		 */
		String value;

		/**
		 * Nano time when the value has been displayed for the first time.
		 */
		long firstDisplayed;

		/**
		 * Indicates that the new value of data item has not been displayed.
		 */
		boolean invalidated;

		/**
		 * Indicates that the data item is read-only.
		 */
		boolean readOnly;

		/**
		 * Construct new data row for a given data item.
		 * 
		 * @param dataItem
		 *            the data item.
		 */
		DataRow(BaseItem dataItem) {
			this.dataItem = dataItem;
			this.invalidated = true;
		}
	}

	/**
	 * Displayed data rows.
	 */
	private final List<DataRow> dataRows = new ArrayList<>();

	/**
	 * Mapping of items to rows.
	 */
	private final Map<BaseItem, DataRow> itemToRowMap = new HashMap<BaseItem, DataTerminal.DataRow>();

	/**
	 * Active action panel.
	 */
	private BaseActionPanel actionPanel;

	/**
	 * Indicates that the terminal is running.
	 */
	private boolean running;

	/**
	 * Index of the selected data row.
	 */
	private int selectedRowIndex;

	/**
	 * Index of the first displayed data row.
	 */
	private int topRowIndex;

	/**
	 * Number of rows in one page.
	 */
	private int pageHeight;

	/**
	 * Text displayed in status bar.
	 */
	private String statusText = "";

	/**
	 * Screen of terminal.
	 */
	private Screen screen;

	/**
	 * Time in milliseconds specifying time between two screen refreshes.
	 */
	private long screenRefreshPeriod = 300;

	/**
	 * Time in milliseconds to emphasize changed data item.
	 */
	private long notifyChangeDuration = 10000;

	/**
	 * Main lock handling thread-safe access to methods.
	 */
	private final Object lock = new Object();

	/**
	 * Constructs new data terminal.
	 */
	public DataTerminal() {
		statusText = "[ENTER] Edit";
	}

	/**
	 * Adds new data item to data terminal to be displayed.
	 * 
	 * @param dataItem
	 *            the data item to be added.
	 */
	public void addDataItem(BaseItem dataItem) {
		if (dataItem == null) {
			throw new NullPointerException("Data item cannot be null.");
		}

		synchronized (lock) {
			if (running) {
				throw new IllegalStateException("Data items cannot be added to running data terminal.");
			}

			DataRow dataRow = itemToRowMap.get(dataItem);
			if (dataRow != null) {
				return;
			}

			dataRow = new DataRow(dataItem);
			dataRow.label = dataItem.getLabel();
			dataRow.value = dataItem.getValue();
			dataRow.invalidated = true;
			dataRow.readOnly = dataItem.isReadOnly();

			itemToRowMap.put(dataItem, dataRow);
			dataRows.add(dataRow);
			dataItem.setDataTerminal(this);
		}
	}

	/**
	 * Returns screen refresh period in milliseconds.
	 * 
	 * @return the refresh period in milliseconds.
	 */
	public long getScreenRefreshPeriod() {
		synchronized (lock) {
			return screenRefreshPeriod;
		}
	}

	/**
	 * Sets screen refresh period in milliseconds.
	 * 
	 * @param screenRefreshPeriod
	 *            the refresh period in milliseconds.
	 */
	public void setScreenRefreshPeriod(long screenRefreshPeriod) {
		synchronized (lock) {
			if (running) {
				throw new IllegalStateException("Running data terminal cannot be configured.");
			}

			this.screenRefreshPeriod = Math.max(100, screenRefreshPeriod);
		}
	}

	/**
	 * Returns duration in milliseconds of emphasized change of data item.
	 * 
	 * @return the duration in milliseconds.
	 */
	public long getNotifyChangeDuration() {
		synchronized (lock) {
			return notifyChangeDuration;
		}
	}

	/**
	 * Sets duration in milliseconds of emphasized change of data item.
	 * 
	 * @param notifyChangeDuration
	 *            the duration in milliseconds.
	 */
	public void setNotifyChangeDuration(long notifyChangeDuration) {
		synchronized (lock) {
			if (running) {
				throw new IllegalStateException("Running data terminal cannot be configured.");
			}

			this.notifyChangeDuration = Math.max(100, notifyChangeDuration);
		}
	}

	/**
	 * Returns the status text displayed when no action panel is active.
	 * 
	 * @return the status text.
	 */
	public String getStatusText() {
		synchronized (lock) {
			return statusText;
		}
	}

	/**
	 * Sets the status text displayed when no action panel is active.
	 * 
	 * @param statusText
	 *            the new status text.
	 */
	public void setStatusText(String statusText) {
		synchronized (lock) {
			this.statusText = statusText;
		}
	}

	/**
	 * Receives external notification that a data item has been changed. This
	 * method is thread safe and can be called from other threads to notify the
	 * terminal about required update of the displayed content.
	 * 
	 * @param dataItem
	 *            the changed data item.
	 */
	void updateOnItemChange(BaseItem dataItem) {
		if (dataItem == null) {
			return;
		}

		String currentLabel = dataItem.getLabel();
		String currentValue = dataItem.getValue();
		boolean currentReadOnly = dataItem.isReadOnly();

		synchronized (lock) {
			DataRow dataRow = itemToRowMap.get(dataItem);
			if (dataRow == null) {
				return;
			}

			dataRow.label = currentLabel;
			dataRow.value = currentValue;
			dataRow.readOnly = currentReadOnly;
			dataRow.invalidated = true;
		}
	}

	/**
	 * Creates action panel for selected data item for given activation key
	 * stroke.
	 * 
	 * @param dataItem
	 *            the selected data item.
	 * @param keyStroke
	 *            the key stroke.
	 * @return action panel or null, if no action is associated with the key
	 *         stroke.
	 */
	protected BaseActionPanel onCreateActionPanel(BaseItem dataItem, KeyStroke keyStroke) {
		if (keyStroke.getKeyType() == KeyType.Enter) {
			return new EditActionPanel(this, dataItem);
		}

		return null;
	}

	/**
	 * Launches the terminal screen.
	 */
	public void launch() {
		// ensure thread-safe start.
		synchronized (lock) {
			// check that data terminal is not running.
			if (running) {
				throw new IllegalStateException("Data terminal is already running.");
			}

			// Check whether there are data items to display
			if (dataRows.isEmpty()) {
				throw new IllegalStateException("No data items to display.");
			}

			running = true;
		}

		// create and start terminal screen
		screen = null;
		try {
			Terminal terminal = new DefaultTerminalFactory().createTerminal();
			screen = new TerminalScreen(terminal);
			screen.startScreen();
			screen.clear();
		} catch (Exception e) {
			synchronized (lock) {
				running = false;
			}
			throw new RuntimeException("Initialization of terminal screen failed.");
		}

		try {
			selectedRowIndex = 0;
			topRowIndex = 0;

			// main loop for handling key strokes
			while (true) {
				synchronized (lock) {
					refreshScreen();
				}

				KeyStroke keyStroke = readInput(screenRefreshPeriod);
				if (keyStroke == null) {
					continue;
				}

				synchronized (lock) {
					// Exit main loop in case of EOF
					if (keyStroke.getKeyType() == KeyType.EOF) {
						break;
					}

					// If action panel is active, forward key stroke to panel
					if (actionPanel != null) {
						actionPanel.handleKeyStroke(keyStroke);
						if (actionPanel.actionCompleted()) {
							actionPanel = null;
						}
					} else {
						// Basic navigation
						switch (keyStroke.getKeyType()) {
						case ArrowDown:
							if (selectedRowIndex < dataRows.size() - 1) {
								selectedRowIndex++;
							}
							break;
						case ArrowUp:
							if (selectedRowIndex >= 1) {
								selectedRowIndex--;
							}
							break;
						case PageUp:
							topRowIndex -= pageHeight;
							topRowIndex = Math.max(topRowIndex, 0);
							selectedRowIndex -= pageHeight;
							selectedRowIndex = Math.max(selectedRowIndex, 0);
							break;
						case PageDown:
							topRowIndex += pageHeight;
							selectedRowIndex += pageHeight;
							selectedRowIndex = Math.min(selectedRowIndex, dataRows.size() - 1);
							break;
						default:
							break;
						}

						// create action panel if the current key stroke
						// activates an action panel.
						actionPanel = onCreateActionPanel(dataRows.get(selectedRowIndex).dataItem, keyStroke);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Data terminal failed.", e);
		} finally {
			// stop terminal screen
			try {
				screen.stopScreen();
			} catch (Exception ignored) {

			}
			screen = null;

			// mark that data terminal is not running.
			synchronized (lock) {
				running = false;
			}
		}
	}

	/**
	 * Updates the terminal screen.
	 * 
	 * @throws IOException
	 */
	private void refreshScreen() throws IOException {
		// remove action panel if necessary
		if ((actionPanel != null) && (actionPanel.actionCompleted())) {
			actionPanel = null;
		}

		// prepare screen
		boolean completeRefresh = false;
		if (screen.doResizeIfNecessary() != null) {
			completeRefresh = true;
		}
		screen.clear();

		TerminalSize ts = screen.getTerminalSize();
		int usedRows = 0;
		TerminalSize singleLineSize = new TerminalSize(ts.getColumns(), 1);

		// basic UI skeleton
		screen.setCursorPosition(null);
		TextGraphics basic = screen.newTextGraphics();
		basic.setBackgroundColor(TextColor.ANSI.BLACK);
		basic.setForegroundColor(TextColor.ANSI.WHITE);
		// Heading
		basic.fillRectangle(new TerminalPosition(0, 0), singleLineSize, '═');
		basic.fillRectangle(new TerminalPosition(0, 2), singleLineSize, '═');
		String headingLeftInfo = "Data terminal";
		basic.putString(0, 1, headingLeftInfo);
		String headingRightInfo = " " + (selectedRowIndex + 1) + "/" + dataRows.size() + " ";
		basic.putString(ts.getColumns() - headingRightInfo.length(), 1, headingRightInfo, SGR.BOLD);
		usedRows += 3;
		// Status bar
		basic.fillRectangle(new TerminalPosition(0, ts.getRows() - 2), singleLineSize, '═');
		String sbText = (actionPanel != null) ? actionPanel.getStatusText() : statusText;
		if (sbText != null) {
			basic.putString(0, ts.getRows() - 1, sbText);
		}
		usedRows += 2;

		// border of action panel
		if (actionPanel != null) {
			int actionPanelHeight = actionPanel.getPanelHeight();
			basic.fillRectangle(new TerminalPosition(0, ts.getRows() - 3 - actionPanelHeight), singleLineSize, '═');
			usedRows += actionPanelHeight + 1;
		}

		// compute page height
		pageHeight = ts.getRows() - usedRows;

		// ensure that selected row is visible
		if (topRowIndex > selectedRowIndex) {
			topRowIndex = selectedRowIndex;
		}
		if (selectedRowIndex >= topRowIndex + pageHeight) {
			topRowIndex = selectedRowIndex - pageHeight + 1;
		}
		topRowIndex = Math.min(topRowIndex, dataRows.size() - pageHeight);
		topRowIndex = Math.max(topRowIndex, 0);

		// display data items
		TextGraphics diGraphics = screen.newTextGraphics();

		int lastVisibleIdx = Math.min(topRowIndex + pageHeight, dataRows.size()) - 1;
		int screenRow = 3;
		final long now = System.nanoTime();
		final long notifyNanoDuration = notifyChangeDuration * 1_000_000;
		for (int rowIdx = topRowIndex; rowIdx <= lastVisibleIdx; rowIdx++) {
			DataRow dataRow = dataRows.get(rowIdx);

			// mark that the value of data item is displayed.
			if (dataRow.invalidated) {
				dataRow.invalidated = false;
				dataRow.firstDisplayed = now;
			}

			// style for selected row
			if (rowIdx == selectedRowIndex) {
				diGraphics.setBackgroundColor(TextColor.ANSI.WHITE);
				diGraphics.setForegroundColor(TextColor.ANSI.BLACK);
				diGraphics.fillRectangle(new TerminalPosition(0, screenRow), singleLineSize, ' ');
			} else {
				diGraphics.setBackgroundColor(TextColor.ANSI.BLACK);
				diGraphics.setForegroundColor(TextColor.ANSI.WHITE);
			}

			// style for item with changed value
			if (now - dataRow.firstDisplayed < notifyNanoDuration) {
				if (rowIdx == selectedRowIndex) {
					diGraphics.setForegroundColor(TextColor.ANSI.MAGENTA);
				} else {
					diGraphics.setForegroundColor(TextColor.ANSI.YELLOW);
				}
			}

			if (dataRow.readOnly) {
				diGraphics.putString(0, screenRow, "*");
			}
			diGraphics.putString(1, screenRow, dataRow.label + ":");

			// style for value
			diGraphics.setForegroundColor(TextColor.ANSI.WHITE);
			if (dataRow.value != null) {
				if (rowIdx == selectedRowIndex) {
					diGraphics.setForegroundColor(TextColor.ANSI.BLACK);
				}
				diGraphics.putString(dataRow.label.length() + 3, screenRow, dataRow.value, SGR.BOLD);
			} else {
				diGraphics.setForegroundColor(TextColor.ANSI.RED);
				diGraphics.putString(dataRow.label.length() + 3, screenRow, "N/A");
			}

			screenRow++;
		}

		if (actionPanel != null) {
			actionPanel.refresh(screen, ts.getRows() - actionPanel.getPanelHeight() - 2);
		}

		screen.refresh(completeRefresh ? Screen.RefreshType.COMPLETE : Screen.RefreshType.DELTA);
	}

	/**
	 * Waits for key stroke limited amount of time.
	 * 
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @return the key stroke or null, if time elapsed.
	 */
	private KeyStroke readInput(long timeout) {
		try {
			final long start = System.nanoTime();
			while (System.nanoTime() - start <= timeout * 1_000_000) {
				KeyStroke keyStroke = screen.pollInput();
				if (keyStroke != null) {
					return keyStroke;
				}

				Thread.sleep(KEY_CHECK_PERIOD_MS);
			}
		} catch (Exception ignore) {

		}

		return null;
	}
}
