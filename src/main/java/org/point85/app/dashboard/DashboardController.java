package org.point85.app.dashboard;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.charts.CategoryClickListener;
import org.point85.app.charts.ParetoChartController;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.performance.EquipmentLoss;
import org.point85.domain.performance.ParetoItem;
import org.point85.domain.performance.TimeCategory;
import org.point85.domain.performance.TimeLoss;
import org.point85.domain.script.ScriptResolverType;
import org.point85.domain.uom.Unit;
import org.point85.tilesfx.Tile;
import org.point85.tilesfx.TileBuilder;
import org.point85.tilesfx.Tile.SkinType;
import org.point85.tilesfx.skins.BarChartItem;
import org.point85.tilesfx.skins.LeaderBoardItem;
import org.point85.tilesfx.tools.FlowGridPane;

import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class DashboardController extends DialogController implements CategoryClickListener {
	private static final String LOSS_CHART_TITLE = "Equipment Times";
	private static final String TIME_CATEGORY_LABEL = "Time Categories";
	private static final String NET_TIME_SERIES = "Time in Category";

	private static final float SEC_PER_DAY = 86400.0f;
	private static final float SEC_PER_HOUR = 3600.0f;
	private static final float SEC_PER_MIN = 60.0f;

	private static final long DAYS_AMOUNT = 30 * 3600;
	private static final long HOURS_AMOUNT = 24 * 3600;
	private static final long MINS_AMOUNT = 3600;

	// TODO remove simulated data
	private static final Random RND = new Random();
	private long lastTimerCall;
	private AnimationTimer timer;

	private static final double TILE_WIDTH = 300;
	private static final double TILE_HEIGHT = TILE_WIDTH;
	private static final double TILE_VGAP = 20.0;
	private static final double TILE_HGAP = 20.0;

	private static final String OEE_FORMAT = "%.1f %%";
	private static final String PROD_FORMAT = "%.1f ";

	// cumulative production
	private boolean showCumulative = true;
	private double cumulativeTotal = 0.0d;
	private double cumulativeGood = 0.0d;
	private double cumulativeReject = 0.0d;

	// container for dashboard tiles
	@FXML
	private AnchorPane apTileLayout;

	// production tile
	private Tile tiProduction;
	private LeaderBoardItem lbiGoodProduction;
	private LeaderBoardItem lbiRejectProduction;

	// OEE tile
	private Tile tiOee;
	private BarChartItem bciOee;
	private BarChartItem bciAvailability;
	private BarChartItem bciPerformance;
	private BarChartItem bciQuality;

	// availability tile
	private Tile tiAvailability;

	// job and material tile
	private Tile tiJobMaterial;

	// net times
	private ObservableList<Data<Number, String>> netTimeList = FXCollections.observableArrayList(new ArrayList<>());

	// no demand loss
	private ObservableList<Data<Number, String>> noDemandList = FXCollections.observableArrayList(new ArrayList<>());

	// special events loss
	private ObservableList<Data<Number, String>> specialEventList = FXCollections
			.observableArrayList(new ArrayList<>());

	// planned downtime
	private ObservableList<Data<Number, String>> plannedDowntimeList = FXCollections
			.observableArrayList(new ArrayList<>());

	// setup
	private ObservableList<Data<Number, String>> setupList = FXCollections.observableArrayList(new ArrayList<>());

	// unplanned downtime
	private ObservableList<Data<Number, String>> unplannedDowntimeList = FXCollections
			.observableArrayList(new ArrayList<>());

	// minor stoppages loss
	private ObservableList<Data<Number, String>> minorStoppageList = FXCollections
			.observableArrayList(new ArrayList<>());

	// reduced speed loss
	private ObservableList<Data<Number, String>> reducedSpeedList = FXCollections
			.observableArrayList(new ArrayList<>());

	// rejects and rework loss
	private ObservableList<Data<Number, String>> rejectList = FXCollections.observableArrayList(new ArrayList<>());

	// yield loss
	private ObservableList<Data<Number, String>> yieldList = FXCollections.observableArrayList(new ArrayList<>());

	// times series net of the loss category
	private XYChart.Series<Number, String> netTimeSeries = new XYChart.Series<>();

	// no demand series
	private XYChart.Series<Number, String> noDemandSeries = new XYChart.Series<>();

	// special events series
	private XYChart.Series<Number, String> specialEventSeries = new XYChart.Series<>();

	// planned downtime series
	private XYChart.Series<Number, String> plannedDowntimeSeries = new XYChart.Series<>();

	// setup series
	private XYChart.Series<Number, String> setupSeries = new XYChart.Series<>();

	// unplanned downtime series
	private XYChart.Series<Number, String> unplannedDowntimeSeries = new XYChart.Series<>();

	// minor stoppages series
	private XYChart.Series<Number, String> minorStoppageSeries = new XYChart.Series<>();

	// reduced speed series
	private XYChart.Series<Number, String> reducedSpeedSeries = new XYChart.Series<>();

	// rejects series
	private XYChart.Series<Number, String> rejectSeries = new XYChart.Series<>();

	// yield series
	private XYChart.Series<Number, String> yieldSeries = new XYChart.Series<>();

	// title of chart
	private String chartTitle = LOSS_CHART_TITLE;

	// x-axis time unit
	private Unit timeUnit;

	private float divisor = 1.0f;

	// the loss data
	private EquipmentLoss equipmentLoss;

	// loss chart
	@FXML
	private StackedBarChart<Number, String> bcLosses;

	// tab pane
	@FXML
	private TabPane tpParetoCharts;

	@FXML
	private Tab tbTimeLosses;

	@FXML
	private Tab tbLevel1Pareto;

	@FXML
	private Tab tbYieldPareto;

	@FXML
	private Tab tbRejectsPareto;

	@FXML
	private Tab tbReducedSpeedPareto;

	@FXML
	private Tab tbMinorStoppagesPareto;

	@FXML
	private Tab tbUnplannedDowntimePareto;

	@FXML
	private Tab tbSetupPareto;

	@FXML
	private Tab tbPlannedDowntimePareto;

	// level 1 Pareto chart
	@FXML
	private StackPane spLevel1ParetoChart;

	@FXML
	private StackPane spYieldParetoChart;

	@FXML
	private StackPane spRejectsParetoChart;

	@FXML
	private StackPane spSpeedParetoChart;

	@FXML
	private StackPane spMinorStoppagesParetoChart;

	@FXML
	private StackPane spUnplannedDowntimeParetoChart;

	@FXML
	private StackPane spSetupParetoChart;

	@FXML
	private StackPane spPlannedDowntimeParetoChart;

	public void setEquipmentLoss(EquipmentLoss equipmentLoss) {
		this.equipmentLoss = equipmentLoss;
	}

	private float determineTimeUnits(Duration duration) {
		float seconds = duration.getSeconds();
		timeUnit = Unit.SECOND;

		if (seconds > DAYS_AMOUNT) {
			divisor = SEC_PER_DAY;
			timeUnit = Unit.DAY;
		} else if (seconds > HOURS_AMOUNT) {
			divisor = SEC_PER_HOUR;
			timeUnit = Unit.HOUR;
		} else if (seconds > MINS_AMOUNT) {
			divisor = SEC_PER_MIN;
			timeUnit = Unit.MINUTE;
		}
		return divisor;
	}

	private Float convertDuration(Duration duration) {
		float time = ((float) duration.getSeconds()) / divisor;
		return new Float(time);
	}

	private List<ParetoItem> fetchParetoData() throws Exception {
		// TODO: query database for the records of interest from equipment
		// loss start time to end time
		// select REASON, START_TIME, END_TIME from EVENT_HISTORY where
		// LOSS_CATEGORY = 'MINOR_STOPPAGE' and
		// START_TIME > '2017-01-01 00:00:00' and END_TIME < '2017-12-31
		// 00:00:00'

		List<ParetoItem> items = new ArrayList<>();

		items.add(new ParetoItem("Jams", equipmentLoss.convertSeconds(960, timeUnit)));
		items.add(new ParetoItem("No part", equipmentLoss.convertSeconds(96, timeUnit)));
		items.add(new ParetoItem("No operator", equipmentLoss.convertSeconds(528, timeUnit)));
		items.add(new ParetoItem("Sensor", equipmentLoss.convertSeconds(240, timeUnit)));
		items.add(new ParetoItem("Mis-align", equipmentLoss.convertSeconds(384, timeUnit)));
		items.add(new ParetoItem("Other", equipmentLoss.convertSeconds(192, timeUnit)));
		return items;
	}

	private void onSelectTimeLosses() {
		if (bcLosses.getData() == null || bcLosses.getData().size() == 0) {
			createLossChart();
		}
	}

	private void onSelectMinorStoppagesPareto() throws Exception {
		if (spMinorStoppagesParetoChart.getChildren().size() == 0) {
			List<ParetoItem> items = fetchParetoData();

			ParetoChartController controller = new ParetoChartController();
			Duration availableTime = equipmentLoss.getAvailableTime();
			Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

			controller.createParetoChart("Minor Stoppages Pareto", spMinorStoppagesParetoChart, items, divisor,
					"Percent Time by Reason");
		}
	}

	private void onSelectRejectsPareto() throws Exception {
		if (spRejectsParetoChart.getChildren().size() == 0) {
			List<ParetoItem> items = fetchParetoData();

			ParetoChartController controller = new ParetoChartController();
			Duration availableTime = equipmentLoss.getAvailableTime();
			Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

			controller.createParetoChart("Rejects and Rework Pareto", spRejectsParetoChart, items, divisor,
					"Percent Time by Reason");
		}
	}

	private void onSelectReducedSpeedPareto() throws Exception {
		if (spSpeedParetoChart.getChildren().size() == 0) {
			List<ParetoItem> items = fetchParetoData();

			ParetoChartController controller = new ParetoChartController();
			Duration availableTime = equipmentLoss.getAvailableTime();
			Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

			controller.createParetoChart("Reduced Speed Pareto", spSpeedParetoChart, items, divisor,
					"Percent Time by Reason");
		}
	}

	private void onSelectYieldPareto() throws Exception {
		if (spYieldParetoChart.getChildren().size() == 0) {
			List<ParetoItem> items = fetchParetoData();

			ParetoChartController controller = new ParetoChartController();
			Duration availableTime = equipmentLoss.getAvailableTime();
			Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

			controller.createParetoChart("Startup And Yield Pareto", spYieldParetoChart, items, divisor,
					"Percent Time by Reason");
		}
	}

	private void onSelectUnplannedDowntimePareto() throws Exception {
		if (spUnplannedDowntimeParetoChart.getChildren().size() == 0) {
			List<ParetoItem> items = fetchParetoData();

			ParetoChartController controller = new ParetoChartController();
			Duration availableTime = equipmentLoss.getAvailableTime();
			Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

			controller.createParetoChart("Unplanned Downtime Pareto", spUnplannedDowntimeParetoChart, items, divisor,
					"Percent Time by Reason");
		}
	}

	private void onSelectSetupPareto() throws Exception {
		if (spSetupParetoChart.getChildren().size() == 0) {
			List<ParetoItem> items = fetchParetoData();

			ParetoChartController controller = new ParetoChartController();
			Duration availableTime = equipmentLoss.getAvailableTime();
			Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

			controller.createParetoChart("Setup and Yield Pareto", spSetupParetoChart, items, divisor,
					"Percent Time by Reason");
		}
	}

	private void onSelectPlannedDowntimePareto() throws Exception {
		if (spPlannedDowntimeParetoChart.getChildren().size() == 0) {
			List<ParetoItem> items = fetchParetoData();

			ParetoChartController controller = new ParetoChartController();
			Duration availableTime = equipmentLoss.getAvailableTime();
			Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

			controller.createParetoChart("Planned Downtime Pareto", spPlannedDowntimeParetoChart, items, divisor,
					"Percent Time by Reason");
		}
	}

	public void displayLosses() throws Exception {
		// loss chart
		createLossChart();

		// show stats
		showStatistics();

		// pareto of losses
		showFirstLevelPareto();
	}

	private void createLossChart() {
		// build data series
		netTimeSeries.setData(netTimeList);
		noDemandSeries.setData(noDemandList);
		specialEventSeries.setData(specialEventList);
		plannedDowntimeSeries.setData(plannedDowntimeList);
		setupSeries.setData(setupList);
		unplannedDowntimeSeries.setData(unplannedDowntimeList);
		minorStoppageSeries.setData(minorStoppageList);
		reducedSpeedSeries.setData(reducedSpeedList);
		rejectSeries.setData(rejectList);
		yieldSeries.setData(yieldList);

		List<XYChart.Data<Number, String>> netTimePoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> noDemandPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> specialEventPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> plannedDowntimePoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> setupPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> unplannedDowntimePoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> minorStoppagePoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> reducedSpeedPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> rejectPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> yieldPoints = new ArrayList<>();

		// x-axis time units
		determineTimeUnits(equipmentLoss.getTotalTime());

		// value adding
		String category = TimeCategory.VALUE_ADDING.toString();
		Number netTime = convertDuration(equipmentLoss.getValueAddingTime());
		Number yield = convertDuration(equipmentLoss.getLoss(TimeLoss.STARTUP_YIELD));

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		yieldPoints.add(new XYChart.Data<Number, String>(yield, category));

		// effective net production time
		category = TimeCategory.EFFECTIVE_NET_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getEffectiveNetProductionTime());
		Number rejects = convertDuration(equipmentLoss.getLoss(TimeLoss.REJECT_REWORK));

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		rejectPoints.add(new XYChart.Data<Number, String>(rejects, category));

		// efficient net production time
		category = TimeCategory.EFFICIENT_NET_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getEfficientNetProductionTime());
		Number reducedSpeed = convertDuration(equipmentLoss.getLoss(TimeLoss.REDUCED_SPEED));

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		reducedSpeedPoints.add(new XYChart.Data<Number, String>(reducedSpeed, category));

		// net production time
		category = TimeCategory.NET_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getNetProductionTime());
		Number minorStoppagesLoss = convertDuration(equipmentLoss.getLoss(TimeLoss.MINOR_STOPPAGES));

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));

		XYChart.Data<Number, String> minorStoppagesData = new XYChart.Data<Number, String>(minorStoppagesLoss,
				category);
		minorStoppagesData.nodeProperty().addListener(new ChangeListener<Node>() {
			@Override
			public void changed(ObservableValue<? extends Node> ov, Node oldNode, final Node node) {
				if (node != null) {
					displayLabelForData(minorStoppagesData);
				}
			}
		});

		minorStoppagePoints.add(minorStoppagesData);

		// reported production time
		category = TimeCategory.REPORTED_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getReportedProductionTime());
		Number unplannedDowntime = convertDuration(equipmentLoss.getLoss(TimeLoss.UNPLANNED_DOWNTIME));

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		unplannedDowntimePoints.add(new XYChart.Data<Number, String>(unplannedDowntime, category));

		// production time
		category = TimeCategory.PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getProductionTime());
		Number setup = convertDuration(equipmentLoss.getLoss(TimeLoss.SETUP));

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		setupPoints.add(new XYChart.Data<Number, String>(setup, category));

		// scheduled time
		category = TimeCategory.SCHEDULED_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getScheduledProductionTime());
		Number plannedDowntime = convertDuration(equipmentLoss.getLoss(TimeLoss.PLANNED_DOWNTIME));

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		plannedDowntimePoints.add(new XYChart.Data<Number, String>(plannedDowntime, category));

		// available time
		category = TimeCategory.AVAILABLE.toString();
		netTime = convertDuration(equipmentLoss.getAvailableTime());
		Number specialEventsLosses = convertDuration(equipmentLoss.getLoss(TimeLoss.UNSCHEDULED));

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		specialEventPoints.add(new XYChart.Data<Number, String>(specialEventsLosses, category));

		// operations time
		category = TimeCategory.REQUIRED_OPERATIONS.toString();
		netTime = convertDuration(equipmentLoss.getRequiredOperationsTime());
		Number noDemand = convertDuration(equipmentLoss.getLoss(TimeLoss.NOT_SCHEDULED));

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		noDemandPoints.add(new XYChart.Data<Number, String>(noDemand, category));

		// add the data to each series
		netTimeList.addAll(netTimePoints);
		noDemandList.addAll(noDemandPoints);
		specialEventList.addAll(specialEventPoints);
		plannedDowntimeList.addAll(plannedDowntimePoints);
		setupList.addAll(setupPoints);
		unplannedDowntimeList.addAll(unplannedDowntimePoints);
		minorStoppageList.addAll(minorStoppagePoints);
		reducedSpeedList.addAll(reducedSpeedPoints);
		rejectList.addAll(rejectPoints);
		yieldList.addAll(yieldPoints);

		// plot time buckets
		CategoryAxis categoryAxis = new CategoryAxis();
		categoryAxis.setLabel(TIME_CATEGORY_LABEL);

		NumberAxis timeAxis = new NumberAxis();
		timeAxis.setLabel("Time (" + timeUnit + ")");
		timeAxis.setAutoRanging(true);
		timeAxis.setSide(Side.TOP);

		bcLosses.setTitle(chartTitle);
		bcLosses.setAnimated(false);

		// net times
		netTimeSeries.setName(NET_TIME_SERIES);
		bcLosses.getData().add(netTimeSeries);

		// no demand
		noDemandSeries.setName(TimeLoss.NOT_SCHEDULED.toString());
		bcLosses.getData().add(noDemandSeries);

		// special events
		specialEventSeries.setName(TimeLoss.UNSCHEDULED.toString());
		bcLosses.getData().add(specialEventSeries);

		// planned downtime
		plannedDowntimeSeries.setName(TimeLoss.PLANNED_DOWNTIME.toString());
		bcLosses.getData().add(plannedDowntimeSeries);

		// setup
		setupSeries.setName(TimeLoss.SETUP.toString());
		bcLosses.getData().add(setupSeries);

		// unplanned downtime
		unplannedDowntimeSeries.setName(TimeLoss.UNPLANNED_DOWNTIME.toString());
		bcLosses.getData().add(unplannedDowntimeSeries);

		// short stops
		minorStoppageSeries.setName(TimeLoss.MINOR_STOPPAGES.toString());
		bcLosses.getData().add(minorStoppageSeries);

		// reduced speed
		reducedSpeedSeries.setName(TimeLoss.REDUCED_SPEED.toString());
		bcLosses.getData().add(reducedSpeedSeries);

		// rejects
		rejectSeries.setName(TimeLoss.REJECT_REWORK.toString());
		bcLosses.getData().add(rejectSeries);

		// yield
		yieldSeries.setName(TimeLoss.STARTUP_YIELD.toString());
		bcLosses.getData().add(yieldSeries);

		// style the chart
		bcLosses.getStylesheets().addAll(getClass().getResource("/org/point85/css/dashboard.css").toExternalForm());

		// add listener for mouse click on bar
		for (Series<Number, String> series : bcLosses.getData()) {
			for (XYChart.Data<Number, String> item : series.getData()) {

				item.getNode().setOnMouseClicked((MouseEvent event) -> {
					onClickLossCategory(series, item);
				});
			}
		}
	}

	private void showStatistics() throws Exception {
		float oee = equipmentLoss.calculateOEEPercentage();
		bciOee.setValue(oee);

		float availability = equipmentLoss.calculateAvailabilityPercentage();
		bciAvailability.setValue(availability);

		float performance = equipmentLoss.calculatePerformancePercentage();
		bciPerformance.setValue(performance);

		float quality = equipmentLoss.calculateQualityPercentage();
		bciQuality.setValue(quality);
	}

	private void showFirstLevelPareto() throws Exception {
		Duration availableTime = equipmentLoss.getAvailableTime();
		Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

		List<ParetoItem> paretoItems = equipmentLoss.getLossItems(timeUnit);

		ParetoChartController level1Controller = new ParetoChartController();
		level1Controller.setCategoryClickListener(this);
		level1Controller.createParetoChart("First-Level Pareto", spLevel1ParetoChart, paretoItems, divisor,
				"Loss Category");
	}

	private void onClickLossCategory(Series<Number, String> series, XYChart.Data<Number, String> lossCategory) {
		showParetoTab(series.getName());
	}

	private void showParetoTab(String lossCategory) {
		Tab tab = null;

		if (lossCategory.equals(TimeLoss.STARTUP_YIELD.toString())) {
			tab = this.tbYieldPareto;
		} else if (lossCategory.equals(TimeLoss.REJECT_REWORK.toString())) {
			tab = this.tbRejectsPareto;
		} else if (lossCategory.equals(TimeLoss.REDUCED_SPEED.toString())) {
			tab = this.tbReducedSpeedPareto;
		} else if (lossCategory.equals(TimeLoss.MINOR_STOPPAGES.toString())) {
			tab = this.tbMinorStoppagesPareto;
		} else if (lossCategory.equals(TimeLoss.UNPLANNED_DOWNTIME.toString())) {
			tab = this.tbUnplannedDowntimePareto;
		} else if (lossCategory.equals(TimeLoss.SETUP.toString())) {
			tab = this.tbSetupPareto;
		} else if (lossCategory.equals(TimeLoss.PLANNED_DOWNTIME.toString())) {
			tab = this.tbPlannedDowntimePareto;
		}

		this.tpParetoCharts.getSelectionModel().select(tab);
	}

	@Override
	public void onClickCategory(XYChart.Data<String, Number> item) {
		String lossCategory = item.getXValue();
		showParetoTab(lossCategory);
	}

	/**
	 * places a text label with a bar's value above a bar node for a given
	 * XYChart.Data
	 */
	private void displayLabelForData(XYChart.Data<Number, String> data) {
		final Node node = data.getNode();
		final Text dataText = new Text(data.getXValue() + "");

		node.parentProperty().addListener((observable, oldParent, parent) -> {
			Group parentGroup = (Group) parent;
			parentGroup.getChildren().add(dataText);
		});

		node.boundsInParentProperty().addListener((observable, oldBounds, bounds) -> {
			dataText.setLayoutX(Math.round(bounds.getMinX() + bounds.getWidth() / 2 - dataText.prefWidth(-1) / 2));
			dataText.setLayoutY(Math.round(bounds.getMinY() - dataText.prefHeight(-1) * 0.5));
		});
	}

	@FXML
	public void initialize() {
		tpParetoCharts.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {

			if (newValue == null || equipmentLoss == null) {
				return;
			}

			String id = newValue.getId();
			try {
				if (id.equals(tbTimeLosses.getId())) {
					onSelectTimeLosses();
				} else if (id.equals(tbLevel1Pareto.getId())) {
					showFirstLevelPareto();
				} else if (id.equals(tbYieldPareto.getId())) {
					onSelectYieldPareto();
				} else if (id.equals(tbRejectsPareto.getId())) {
					onSelectRejectsPareto();
				} else if (id.equals(tbReducedSpeedPareto.getId())) {
					onSelectReducedSpeedPareto();
				} else if (id.equals(tbMinorStoppagesPareto.getId())) {
					onSelectMinorStoppagesPareto();
				} else if (id.equals(tbUnplannedDowntimePareto.getId())) {
					onSelectUnplannedDowntimePareto();
				} else if (id.equals(tbSetupPareto.getId())) {
					onSelectSetupPareto();
				} else if (id.equals(tbPlannedDowntimePareto.getId())) {
					onSelectPlannedDowntimePareto();
				}

			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		buildDashboardTiles();
	}

	private double incrementTotalProduction(double delta) {
		cumulativeTotal += delta;
		return cumulativeTotal;
	}

	public void buildDashboardTiles() {
		// reset cumulative production
		cumulativeTotal = 0.0d;
		cumulativeGood = 0.0d;
		cumulativeReject = 0.0d;

		// OEE tile
		bciOee = new BarChartItem("OEE", 0, Tile.BLUE);
		bciOee.setFormatString(OEE_FORMAT);

		bciPerformance = new BarChartItem("Performance", 0, Tile.GREEN);
		bciPerformance.setFormatString(OEE_FORMAT);

		bciAvailability = new BarChartItem("Availability", 0, Tile.RED);
		bciAvailability.setFormatString(OEE_FORMAT);

		bciQuality = new BarChartItem("Quality", 0, Tile.ORANGE);
		bciQuality.setFormatString(OEE_FORMAT);

		tiOee = TileBuilder.create().skinType(SkinType.BAR_CHART).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title("Overall Equipment Effectiveness").text("Current OEE")
				.barChartItems(bciOee, bciAvailability, bciPerformance, bciQuality).decimals(0).sortedData(false)
				.build();

		// production tile
		lbiGoodProduction = new LeaderBoardItem("Good", 0);
		lbiRejectProduction = new LeaderBoardItem("Reject", 0);

		String productionText = "Cumulative Quantity";

		if (!showCumulative) {
			productionText = "Change in Quantity";
		}

		tiProduction = TileBuilder.create().skinType(SkinType.LEADER_BOARD).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title("Current Production").text(productionText)
				.leaderBoardItems(lbiGoodProduction, lbiRejectProduction).sortedData(true).build();

		// availability tile
		tiAvailability = TileBuilder.create().skinType(SkinType.TEXT).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title("Availability").textVisible(true).descriptionAlignment(Pos.CENTER).build();

		// material and job
		tiJobMaterial = TileBuilder.create().skinType(SkinType.TEXT).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title("Material and Job").textVisible(true).descriptionAlignment(Pos.CENTER).build();

		FlowGridPane pane = new FlowGridPane(4, 1, tiOee, tiProduction, tiAvailability, tiJobMaterial);
		pane.setHgap(TILE_VGAP);
		pane.setVgap(TILE_HGAP);
		pane.setAlignment(Pos.CENTER);
		pane.setCenterShape(true);
		pane.setPadding(new Insets(5));
		pane.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		apTileLayout.getChildren().add(pane);

		AnchorPane.setTopAnchor(pane, 0.0);
		AnchorPane.setBottomAnchor(pane, 0.0);
		AnchorPane.setLeftAnchor(pane, 0.0);
		AnchorPane.setRightAnchor(pane, 0.0);

		// TODO remove simulation
		lastTimerCall = System.nanoTime();
		timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				long delta = 3_500_000_000L;
				if (now > lastTimerCall + delta) {
					// OEE
					bciOee.setValue(RND.nextDouble() * 100);
					bciAvailability.setValue(RND.nextDouble() * 100);
					bciPerformance.setValue(RND.nextDouble() * 100);
					bciQuality.setValue(RND.nextDouble() * 100);

					// production
					boolean sim1 = false;

					if (sim1) {
						double totalGood = RND.nextDouble() * 100;
						lbiGoodProduction.setFormatString("%.1f kg");
						lbiGoodProduction.setValue(showCumulative ? incrementTotalProduction(totalGood) : totalGood);

						double totalReject = RND.nextDouble() * 100;
						lbiRejectProduction.setFormatString("%.1f kg");
						lbiRejectProduction
								.setValue(showCumulative ? incrementTotalProduction(totalReject) : totalReject);
					} else {

						tiProduction.getLeaderBoardItems().get(RND.nextInt(2)).setValue(RND.nextDouble() * 80);
					}

					// material
					tiJobMaterial.setDescription(
							"M123456789 (and description could get long)" + (long) (RND.nextDouble() * 10000));

					// job
					tiJobMaterial.setText("Job " + (long) (RND.nextDouble() * 10000));

					// availability
					TimeLoss loss = null;

					if (RND.nextDouble() > 0.5) {
						loss = TimeLoss.UNPLANNED_DOWNTIME;
					} else {
						loss = TimeLoss.AVAILABLE;
					}
					tiAvailability.setTextColor(loss.getColor());
					tiAvailability.setDescription(loss.getDisplayString());

					tiAvailability
							.setText("Reason" + (long) (RND.nextDouble() * 1000) + " (description of the reason)");

					lastTimerCall = now;
				}
			}
		};
		
		// TODO
		//timer.start();
	}

	public void update(CollectorResolvedEventMessage message) {
		ScriptResolverType resolverType = message.getResolverType();

		switch (resolverType) {
		case AVAILABILITY: {
			// availability reason
			tiAvailability.setText(message.getReasonName() + "(" + message.getReasonDescription() + ")");

			// loss category
			TimeLoss loss = message.getLoss();
			tiAvailability.setDescription(loss.toString());
			tiAvailability.setTextColor(loss.getColor());
			break;
		}

		case JOB: {
			// job
			tiJobMaterial.setText(message.getJob());
			break;
		}

		case MATERIAL: {
			// material
			String displayString = message.getMaterialName() + " (" + message.getMaterialDescription() + ")";
			tiJobMaterial.setDescription(displayString);
			break;
		}

		case PROD_GOOD: {
			// good production
			lbiGoodProduction.setFormatString(PROD_FORMAT + " " + message.getUom());
			lbiGoodProduction.setValue(message.getAmount());
			break;
		}

		case PROD_REJECT: {
			// reject and rework
			lbiRejectProduction.setFormatString(PROD_FORMAT + " " + message.getUom());
			lbiRejectProduction.setValue(message.getAmount());
			break;
		}

		default:
			break;
		}
	}
}
