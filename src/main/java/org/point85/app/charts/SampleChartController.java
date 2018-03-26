package org.point85.app.charts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class SampleChartController {
	// max number of samples to show on the chart
	private static final int MAX_SAMPLES = 100;

	// current number of samples being shown
	private int numberSamplesToDisplay = MAX_SAMPLES;

	// chart data series, X = sample # and Y = value
	private XYChart.Series<Integer, Number> dataSeries = new XYChart.Series<>();

	// legend for non-numerical points
	private ObservableList<StringOrdinal> chartOrdinals = FXCollections.observableArrayList(new ArrayList<>());

	// sample number
	private Integer sampleNumber = new Integer(0);

	// linear or stair-step interpolation between points
	private InterpolationType interpolationType = InterpolationType.LINEAR;

	// map of strings (non-numerical) data plotted
	Map<String, Integer> chartStrings = new HashMap<>();

	// line chart
	@FXML
	private LineChart<Integer, Number> chSamples;

	// axes
	@FXML
	private NumberAxis xAxis;

	@FXML
	private NumberAxis yAxis;

	// table of non-numerical point plotted
	@FXML
	private TableView<StringOrdinal> tvChartOrdinals;

	@FXML
	private TableColumn<StringOrdinal, Integer> tcOrdinalY;

	@FXML
	private TableColumn<StringOrdinal, String> tcStringY;

	@FXML
	public void initialize() {
		initializeChart();
		initializeStringValueTable();
	}

	private void initializeChart() {
		dataSeries.setName("Sample Data");
		chSamples.getData().add(dataSeries);
	}

	private void initializeStringValueTable() {
		// bind to list of points
		tvChartOrdinals.setItems(chartOrdinals);

		tcOrdinalY.setCellValueFactory(cellDataFeatures -> {
			return new SimpleObjectProperty<Integer>(cellDataFeatures.getValue().getAxisKey());
		});

		tcStringY.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getAxisValue());
		});
	}

	public LineChart<Integer, Number> getChart() {
		return this.chSamples;
	}

	public void setInterpolation(InterpolationType type) {
		this.interpolationType = type;
	}

	public void setChartStrings(Map<String, Integer> chartStrings) {
		this.chartStrings = chartStrings;
	}

	private Integer convertToOrdinal(String value) {
		Integer ordinal = null;

		ordinal = this.chartStrings.get(value);

		if (ordinal == null) {
			ordinal = chartStrings.size() + 1;
			this.chartStrings.put(value, ordinal);
		}

		StringOrdinal point = new StringOrdinal(ordinal, value);

		if (!chartOrdinals.contains(point)) {
			chartOrdinals.add(point);
		}

		return ordinal;
	}

	public void plotValue(Number value) {
		if (numberSamplesToDisplay < 1) {
			return;
		}

		sampleNumber++;
		XYChart.Data<Integer, Number> nextPoint = new XYChart.Data<>(sampleNumber, value);

		if (interpolationType.equals(InterpolationType.STAIR_STEP) && sampleNumber > 0) {
			Number nextY = nextPoint.getYValue();

			XYChart.Data<Integer, Number> stepPoint = new XYChart.Data<>(sampleNumber - 1, nextY);
			dataSeries.getData().add(stepPoint);
		}

		dataSeries.getData().add(nextPoint);

		// after N count, delete old sample(s)
		if (sampleNumber.intValue() > numberSamplesToDisplay) {

			int delta = 0;
			// drop oldest samples
			if (interpolationType.equals(InterpolationType.LINEAR)) {
				delta = dataSeries.getData().size() - numberSamplesToDisplay;
			} else {
				// stair step
				delta = dataSeries.getData().size() - 2 * numberSamplesToDisplay;
			}

			if (delta > 0) {
				dataSeries.getData().remove(0, delta);
			}
		}

		if (sampleNumber.intValue() > (numberSamplesToDisplay - 1)) {
			// move range
			xAxis.setLowerBound(xAxis.getLowerBound() + 1);
			xAxis.setUpperBound(xAxis.getUpperBound() + 1);
		}
	}

	public void plotValue(String value) {
		plotValue(convertToOrdinal(value));
	}

	public void setXAxis(int tickUnit, int upperBound) {
		this.xAxis.setLowerBound(0);
		this.xAxis.setTickUnit(tickUnit);
		this.xAxis.setUpperBound(upperBound);

		this.numberSamplesToDisplay = upperBound;
	}
	
	public void setYAxis(int tickUnit, int upperBound) {
		this.yAxis.setLowerBound(0);
		this.yAxis.setUpperBound(upperBound);
		this.yAxis.setTickUnit(tickUnit);
	}

	public void reset(int xTickUnit, int xUpperBound, int yTickUnit, int yUpperBound) {
		// value view
		dataSeries.getData().clear();
		sampleNumber = 0;

		setXAxis(xTickUnit, xUpperBound);
		setXAxis(yTickUnit, yUpperBound);

		chartOrdinals.clear();
	}
}
