package org.point85.app.designer;

import javafx.scene.paint.Color;

public enum ConnectionState {
	DISCONNECTED, CONNECTING, CONNECTED;
	
	// connection state colors
	public static final Color CONNECTED_COLOR = Color.GREEN;
	public static final Color CONNECTING_COLOR = Color.BLUE;
	public static final Color DISCONNECTED_COLOR = Color.BLACK;

	@Override
	public String toString() {
		String key = null;

		switch (this) {
		case CONNECTED:
			key = "connected.state";
			break;
		case CONNECTING:
			key = "connecting.state";
			break;
		case DISCONNECTED:
			key = "disconnected.state";
			break;
		default:
			break;
		}
		return DesignerLocalizer.instance().getLangString(key);
	}
}
