package com.gboxsw.dataterm;

import java.util.concurrent.*;
import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.*;
import com.googlecode.lanterna.input.*;
import com.googlecode.lanterna.screen.*;

/**
 * Action panel that allows to change value of a data item. The change of value
 * is executed asynchronously.
 */
public class EditActionPanel extends BaseActionPanel {

	/**
	 * State of action.
	 */
	private enum State {
		Editing, Changing, ChangeDone, ChangeFailed
	}

	/**
	 * Executor service used to asynchronously change values.
	 */
	private static final ExecutorService CHANGE_EXECUTOR_SERVICE = Executors.newCachedThreadPool();

	/**
	 * Indicates whether action has been completed.
	 */
	private boolean completed = false;

	/**
	 * Edited value.
	 */
	private final StringBuilder content = new StringBuilder();

	/**
	 * Current position of cursor.
	 */
	private int cursorPosition = 0;

	/**
	 * Current state of action.
	 */
	private volatile State state;

	/**
	 * Constructs action panel for editing the value of data item.
	 * 
	 * @param dataTerminal
	 *            the data terminal.
	 * @param dataItem
	 *            the data item whose value will be edited.
	 */
	public EditActionPanel(DataTerminal dataTerminal, BaseItem dataItem) {
		super(dataTerminal, dataItem);

		if ((dataItem == null) || dataItem.isReadOnly()) {
			completed = true;
		}

		// Initialize
		String value = dataItem.getValue();
		if (value != null) {
			content.append(dataItem.getValue());
		}

		cursorPosition = 0;
		state = State.Editing;
	}

	@Override
	public int getPanelHeight() {
		return 1;
	}

	@Override
	public void refresh(Screen screen, int startRow) {
		TextGraphics textStyle = screen.newTextGraphics();
		textStyle.setForegroundColor(TextColor.ANSI.WHITE);

		switch (state) {
		case Editing:
			textStyle.putString(0, startRow, ">", SGR.BOLD);
			textStyle.putString(2, startRow, content.toString());
			screen.setCursorPosition(new TerminalPosition(2 + cursorPosition, startRow));
			break;
		case Changing:
			textStyle.putString(0, startRow, "Changing value ...", SGR.BLINK);
			break;
		case ChangeDone:
			textStyle.setForegroundColor(TextColor.ANSI.GREEN);
			textStyle.putString(0, startRow, "Value has been changed.");
			break;
		case ChangeFailed:
			textStyle.setForegroundColor(TextColor.ANSI.RED);
			textStyle.putString(0, startRow, "Change of value failed.");
			break;
		}
	}

	@Override
	public void handleKeyStroke(KeyStroke keyStroke) {
		if (state == State.Changing) {
			return;
		}

		if (keyStroke.getKeyType() == KeyType.Escape) {
			completed = true;
			return;
		}

		if (state != State.Editing) {
			return;
		}

		switch (keyStroke.getKeyType()) {
		case ArrowLeft:
			if (cursorPosition > 0) {
				cursorPosition--;
			}
			break;
		case ArrowRight:
			if (cursorPosition < content.length()) {
				cursorPosition++;
			}
			break;
		case End:
			cursorPosition = content.length();
			break;
		case Home:
			cursorPosition = 0;
			break;
		case Character:
			content.insert(cursorPosition, keyStroke.getCharacter());
			cursorPosition++;
			break;
		case Delete:
			if (cursorPosition < content.length()) {
				content.delete(cursorPosition, cursorPosition + 1);
			}
			break;
		case Backspace:
			if (cursorPosition > 0) {
				content.delete(cursorPosition - 1, cursorPosition);
				cursorPosition--;
			}
			break;
		case Enter:
			changeValueAsync();
			break;
		default:
			break;
		}
	}

	@Override
	public boolean actionCompleted() {
		return completed;
	}

	@Override
	public String getStatusText() {
		switch (state) {
		case Editing:
			return "[ESC] Cancel [ENTER] Change";
		case Changing:
			return "Please wait ...";
		case ChangeDone:
		case ChangeFailed:
			return "[ESC] Close panel";
		}

		return "";
	}

	/**
	 * Changes the value asynchronously.
	 */
	private void changeValueAsync() {
		final String newValue = content.toString().trim();
		state = State.Changing;
		CHANGE_EXECUTOR_SERVICE.submit(new Runnable() {
			@Override
			public void run() {
				try {
					dataItem.setValue(newValue);
					state = State.ChangeDone;
					dataTerminal.updateOnItemChange(dataItem);
				} catch (Exception e) {
					state = State.ChangeFailed;
				}
			}
		});
	}
}
