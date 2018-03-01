package org.point85.app.charts;

import javafx.scene.chart.XYChart;

public interface CategoryClickListener {
	void onClickCategory(XYChart.Data<String, Number> item);
}
