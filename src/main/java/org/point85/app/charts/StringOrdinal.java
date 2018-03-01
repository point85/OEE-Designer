package org.point85.app.charts;

import java.util.Objects;

public class StringOrdinal {
	private Integer axisKey;

	private String axisValue;

	public StringOrdinal(Integer axisKey, String axisValue) {
		this.axisKey = axisKey;
		this.axisValue = axisValue;
	}

	public Integer getAxisKey() {
		return axisKey;
	}

	public void setAxisKey(Integer axisKey) {
		this.axisKey = axisKey;
	}

	public String getAxisValue() {
		return axisValue;
	}

	public void setAxisValue(String axisValue) {
		this.axisValue = axisValue;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StringOrdinal) {
			StringOrdinal other = (StringOrdinal) obj;
			if (getAxisKey().equals(other.getAxisKey())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(axisValue);
	}

}
