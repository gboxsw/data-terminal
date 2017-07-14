package com.gboxsw.dataterm;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;

/**
 * Base class defining action panels for data items.
 */
public abstract class BaseActionPanel {

	/**
	 * Data item that related to the action.
	 */
	protected final BaseItem dataItem;

	/**
	 * Data terminal in which the panel will be displayed.
	 */
	protected final DataTerminal dataTerminal;

	/**
	 * Constructs action panel for data item.
	 * 
	 * @param dataTerminal
	 *            the data terminal.
	 * @param dataItem
	 *            the underlying data item.
	 */
	public BaseActionPanel(DataTerminal dataTerminal, BaseItem dataItem) {
		this.dataTerminal = dataTerminal;
		this.dataItem = dataItem;
	}

	/**
	 * Returns height of panel - the number of rows.
	 * 
	 * @return the height of the panel in rows.
	 */
	public abstract int getPanelHeight();

	/**
	 * Refreshes the view of panel on the screen.
	 * 
	 * @param screen
	 *            the screen
	 * @param startRow
	 *            the row where the view of panel starts.
	 */
	public abstract void refresh(Screen screen, int startRow);

	/**
	 * Handles keyStroke.
	 * 
	 * @param keyStroke
	 *            the keystroke.
	 */
	public abstract void handleKeyStroke(KeyStroke keyStroke);

	/**
	 * Returns whether the action of panel is completed.
	 * 
	 * @return true, if the action is completed and the panel should disappear,
	 *         false otherwise.
	 */
	public abstract boolean actionCompleted();

	/**
	 * Returns the current status text.
	 * 
	 * @return the status text.
	 */
	public abstract String getStatusText();
}
