package org.point85.app.monitor;

import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.NotificationSeverity;

/**
 * Notification of a data collector event
 *
 */
public class CollectorNotification extends AbstractNotification {
	private final NotificationSeverity severity;
	private final String text;

	public CollectorNotification(CollectorNotificationMessage message) {
		super(message);
		this.severity = message.getSeverity();
		this.text = message.getText();
	}

	public NotificationSeverity getSeverity() {
		return severity;
	}

	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return super.toString() + ", severity: " + severity + ", message: " + text;
	}
}
