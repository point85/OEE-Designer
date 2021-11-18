package org.point85.app.opc.da;

import org.point85.app.designer.DesignerLocalizer;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class OpcDaTagValueChart {

	public static final int LINEAR = 0;
	public static final int STAIR_STEP = 1;
	private static final int NUM_POINTS = 100;

	private static final String CHART_TITLE = DesignerLocalizer.instance().getLangString("tag.chart.title");
	private static final String YAXIS_LABEL = DesignerLocalizer.instance().getLangString("tag.value");
	private static final String XAXIS_LABEL = DesignerLocalizer.instance().getLangString("sample.number");
	private static final String CHART_ID = "tagValueChart";
	private static final String SERIES_NAME = "tagValueSeries";

	private final XYChart.Series<Number, Number> valueDataSeries = new XYChart.Series<>();
	private final NumberAxis xAxis = new NumberAxis(0, NUM_POINTS, NUM_POINTS / 10.0d);
	private final NumberAxis yAxis = new NumberAxis();
	private final LineChart<Number, Number> tagValueChart = new LineChart<>(xAxis, yAxis);

	private Integer sampleNo = Integer.valueOf(-1);
	private int interpolation = OpcDaTagValueChart.LINEAR;

	public LineChart<Number, Number> createChart() {

		tagValueChart.setId(CHART_ID);
		tagValueChart.setCreateSymbols(true);
		tagValueChart.setAnimated(false);
		tagValueChart.setLegendVisible(false);

		xAxis.setLabel(XAXIS_LABEL);
		xAxis.setForceZeroInRange(false);
		yAxis.setLabel(YAXIS_LABEL);
		yAxis.setAutoRanging(true);

		valueDataSeries.setName(SERIES_NAME);
		tagValueChart.getData().add(valueDataSeries);

		reset(CHART_TITLE);

		return tagValueChart;
	}

	public void setInterpolation(int type) {
		interpolation = type;
	}

	public String getItemId() {
		return tagValueChart.getTitle();
	}

	public void setItemId(String itemId) {
		tagValueChart.setTitle(itemId);
	}

	public void plotValue(Number value) {
		sampleNo++;
		XYChart.Data<Number, Number> nextPoint = new XYChart.Data<>(sampleNo, value);

		if ((interpolation == OpcDaTagValueChart.STAIR_STEP || value instanceof Byte) && sampleNo > 0) {
			Number nextY = nextPoint.getYValue();

			XYChart.Data<Number, Number> stepPoint = new XYChart.Data<>((sampleNo - 1), nextY);
			valueDataSeries.getData().add(stepPoint);
		}

		valueDataSeries.getData().add(nextPoint);

		// after N count, delete old point
		if (sampleNo.intValue() > NUM_POINTS) {
			// drop first point
			valueDataSeries.getData().remove(0);
		}

		if (sampleNo.intValue() > (NUM_POINTS - 1)) {
			// move range
			xAxis.setLowerBound(xAxis.getLowerBound() + 1);
			xAxis.setUpperBound(xAxis.getUpperBound() + 1);
		}
	}

	public void reset(String itemId) {
		valueDataSeries.getData().clear();
		sampleNo = -1;
		tagValueChart.setTitle(itemId);
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(NUM_POINTS);
		yAxis.setLowerBound(0);
		yAxis.setUpperBound(90);
	}

	public void reset() {
		reset(CHART_TITLE);
	}
}
