package org.springframework.cloud.stream.app.twitter.common;

/**
 * @author Christian Tzolov
 */
public class Cursor {
	private long cursor = -1;

	public long getCursor() {
		return cursor;
	}

	public void updateCursor(long newCursor) {
		this.cursor = (newCursor > 0) ? newCursor : -1;
	}

	@Override
	public String toString() {
		return "Cursor{" +
				"cursor=" + cursor +
				'}';
	}
}
