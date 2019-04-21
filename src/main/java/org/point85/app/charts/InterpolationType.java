package org.point85.app.charts;

import org.point85.app.designer.DesignerLocalizer;

public enum InterpolationType {
	LINEAR, STAIR_STEP;
	
	@Override
	public String toString() {
		String key = null;
		
		switch (this) {
		case LINEAR:
			key = "linear.type";
			break;
		case STAIR_STEP:
			key = "stair.step.type";
			break;
		default:
			break;
		}
		return DesignerLocalizer.instance().getLangString(key);
	}
}
