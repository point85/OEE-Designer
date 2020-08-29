package org.point85.app.charts;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.oee.ParetoItem;

import javafx.geometry.Side;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class ParetoChartController {
	// top N items to chart
	private static final int TOP_N = 10;

	// bar chart data series, X = reason and Y = % in sample
	private XYChart.Series<String, Number> barChartSeries;

	// line chart data series, X = reason and Y = cumulative % in sample
	private XYChart.Series<String, Number> lineChartSeries;

	// total data count
	private Number totalCount;

	// list of reasons to chart
	private List<ParetoItem> paretoItems;

	// title of chart
	private String chartTitle;

	// listener for mouse clicks on an X-axis category
	private CategoryClickListener clickListener;

	public void createParetoChart(String title, StackPane spPareto, List<ParetoItem> items, Number totalCount,
			String categoryLabel) {
		barChartSeries = new XYChart.Series<>();
		lineChartSeries = new XYChart.Series<>();

		this.chartTitle = title;
		this.totalCount = totalCount;

		// sort the items
		this.paretoItems = items;
		Collections.sort(paretoItems, Collections.reverseOrder());

		barChartSeries.setName(DesignerLocalizer.instance().getLangString("categories"));
		lineChartSeries.setName(DesignerLocalizer.instance().getLangString("cumulative"));

		// create the stacked charts
		layerCharts(spPareto, createBarChart(categoryLabel), createLineChart(categoryLabel));
	}

	public void createParetoChart(String title, StackPane spPareto, List<ParetoItem> items, String categoryLabel) {
		Number countTotal = null;

		for (int i = 0; i < items.size(); i++) {
			Number value = items.get(i).getValue();

			if (i == 0) {
				countTotal = value;
			} else {
				if (value instanceof BigDecimal) {
					countTotal = ((BigDecimal) countTotal).add((BigDecimal) value);
				} else if (value instanceof Integer) {
					countTotal = (Integer) countTotal + (Integer) value;
				} else if (value instanceof Float) {
					countTotal = (Float) countTotal + (Float) value;
				} else if (value instanceof Double) {
					countTotal = (Double) countTotal + (Double) value;
				}
			}
		}
		createParetoChart(title, spPareto, items, countTotal, categoryLabel);
	}

	public void clearData() {
		barChartSeries.getData().clear();
		lineChartSeries.getData().clear();
	}

	private BarChart<String, Number> createBarChart(String categoryLabel) {
		// X-Axis category
		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel(categoryLabel);

		// Y-Axis (%)
		NumberAxis yAxis = new NumberAxis(0, 100, 10);
		yAxis.setLabel(DesignerLocalizer.instance().getLangString("percent"));
		yAxis.setAutoRanging(false);
		yAxis.setUpperBound(100.0d);
		yAxis.setLowerBound(0.0d);

		// create bar chart
		BarChart<String, Number> chBarChart = new BarChart<>(xAxis, yAxis);
		chBarChart.setTitle(chartTitle);
		chBarChart.setLegendVisible(false);
		chBarChart.setAnimated(false);
		chBarChart.getData().add(barChartSeries);

		// add the points
		double total = totalCount.doubleValue();

		if (total > 0.0d) {
			int count = 0;
			for (ParetoItem paretoItem : paretoItems) {
				if (count > TOP_N) {
					break;
				}
				count++;

				Float percentage = new Float(paretoItem.getValue().floatValue() / total * 100.0f);
				XYChart.Data<String, Number> point = new XYChart.Data<>(paretoItem.getCategory(), percentage);
				barChartSeries.getData().add(point);
			}
		}

		// add listener for mouse click on bar
		for (Series<String, Number> series : chBarChart.getData()) {
			for (XYChart.Data<String, Number> item : series.getData()) {

				item.getNode().setOnMouseClicked((MouseEvent event) -> onBarChartNodeSelected(item));
			}
		}

		return chBarChart;
	}

	private void onBarChartNodeSelected(XYChart.Data<String, Number> item) {
		if (clickListener != null) {
			clickListener.onClickCategory(item);
		}
	}

	private LineChart<String, Number> createLineChart(String categoryLabel) {
		// X-Axis category (not shown)
		CategoryAxis xAxis = new CategoryAxis();
		xAxis.setLabel(categoryLabel);
		xAxis.setOpacity(0);

		// Y-Axis (%)
		NumberAxis yAxis = new NumberAxis(0, 100, 10);
		yAxis.setLabel(DesignerLocalizer.instance().getLangString("cum.percent"));
		yAxis.setSide(Side.RIGHT);
		yAxis.setAutoRanging(false);
		yAxis.setUpperBound(100.0d);
		yAxis.setLowerBound(0.0d);

		// create the line chart
		LineChart<String, Number> chLineChart = new LineChart<>(xAxis, yAxis);
		chLineChart.setTitle(chartTitle);
		chLineChart.setLegendVisible(false);
		chLineChart.setAnimated(false);
		chLineChart.setCreateSymbols(true);
		chLineChart.getData().add(lineChartSeries);

		// plot the points
		double total = totalCount.doubleValue();
		Float cumulative = 0.0f;

		for (ParetoItem paretoItem : this.paretoItems) {
			cumulative += new Float(paretoItem.getValue().floatValue() / total * 100.0f);
			XYChart.Data<String, Number> point = new XYChart.Data<>(paretoItem.getCategory(), cumulative);
			lineChartSeries.getData().add(point);
		}

		return chLineChart;
	}

	private void hideChart(XYChart<String, Number> chart) {
		// set background transparent and colors
		chart.getStylesheets().addAll(getClass().getResource("/org/point85/css/pareto_chart.css").toExternalForm());

		chart.setAlternativeRowFillVisible(false);
		chart.setAlternativeColumnFillVisible(false);
		chart.setHorizontalGridLinesVisible(false);
		chart.setVerticalGridLinesVisible(false);
	}

	private void layerCharts(StackPane spPareto, BarChart<String, Number> barChart,
			LineChart<String, Number> lineChart) {

		hideChart(lineChart);
		spPareto.getChildren().addAll(barChart, lineChart);

	}

	public Number getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(Number totalCount) {
		this.totalCount = totalCount;
	}

	public String getTitle() {
		return this.chartTitle;
	}

	public void setChartTitle(String title) {
		this.chartTitle = title;
	}

	public void setCategoryClickListener(CategoryClickListener listener) {
		this.clickListener = listener;
	}

}
