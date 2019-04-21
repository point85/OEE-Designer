package org.point85.app.designer;

public enum ConnectionState {
	DISCONNECTED, CONNECTING, CONNECTED;

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
