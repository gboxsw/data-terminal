package com.gboxsw.dataterm.samples.simpleapp;

import com.gboxsw.dataterm.BaseItem;

/**
 * Simple data item for storing a value.
 */
class SimpleItem extends BaseItem {

	/**
	 * The label.
	 */
	private final String label;

	/**
	 * Indicates whether data item is read-only.
	 */
	private final boolean readOnly;

	/**
	 * The value.
	 */
	private String value;

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void setValue(String newValue) {
		value = newValue;
	}

	/**
	 * Constructs data item.
	 * 
	 * @param label
	 *            the label of item.
	 * @param readOnly
	 *            true, if the data item is read-only, false otherwise.
	 */
	public SimpleItem(String label, boolean readOnly) {
		this.label = label;
		this.readOnly = readOnly;
	}
}
