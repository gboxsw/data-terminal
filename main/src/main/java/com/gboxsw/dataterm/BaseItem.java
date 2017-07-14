package com.gboxsw.dataterm;

/**
 * Base class for items of a data terminal.
 */
public abstract class BaseItem {

	/**
	 * Data terminal.
	 */
	private DataTerminal dataTerminal;

	/**
	 * Sets data terminal which is receiving change notification.
	 * 
	 * @param dataTerminal
	 *            the data terminal.
	 */
	void setDataTerminal(DataTerminal dataTerminal) {
		this.dataTerminal = dataTerminal;
	}

	/**
	 * Fires change of value. The notification is received by the currently set
	 * data terminal.
	 */
	protected void fireValueChanged() {
		this.dataTerminal.updateOnItemChange(this);
	}

	/**
	 * Returns label (name) of the data item.
	 * 
	 * @return the label identifying or describing the data item.
	 */
	public abstract String getLabel();

	/**
	 * Indicates whether the data item is read-only.
	 * 
	 * @return true, if the data item is read-only, false otherwise.
	 */
	public abstract boolean isReadOnly();

	/**
	 * Returns the current value of the data item.
	 * 
	 * @return the current value of the data item transformed to a string. The
	 *         value null indicates that the value of data item is not
	 *         available.
	 */
	public abstract String getValue();

	/**
	 * Set new value of data item. Data terminal invokes this method in other
	 * thread.
	 * 
	 * @param newValue
	 *            the desired value.
	 */
	public abstract void setValue(String newValue);
}
