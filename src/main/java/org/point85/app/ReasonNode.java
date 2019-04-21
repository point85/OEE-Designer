package org.point85.app;

import org.point85.domain.plant.Reason;

public class ReasonNode {
	private Reason reason;

	public ReasonNode(Reason reason) {
		setReason(reason);
	}

	public Reason getReason() {
		return reason;
	}

	public void setReason(Reason reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return reason.getName() + " (" + reason.getDescription() + ")";
	}
}
