package org.point85.app.dashboard;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.charts.CategoryClickListener;
import org.point85.app.charts.ParetoChartController;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.AvailabilityRecord;
import org.point85.domain.collector.BaseRecord;
import org.point85.domain.collector.SetupRecord;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.oee.EquipmentLoss;
import org.point85.domain.oee.EquipmentLossManager;
import org.point85.domain.oee.ParetoItem;
import org.point85.domain.oee.TimeCategory;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.EventResolverType;
import org.point85.domain.script.ResolvedEvent;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.tilesfx.Tile;
import org.point85.tilesfx.Tile.SkinType;
import org.point85.tilesfx.Tile.TextSize;
import org.point85.tilesfx.TileBuilder;
import org.point85.tilesfx.skins.BarChartItem;
import org.point85.tilesfx.skins.LeaderBoardItem;
import org.point85.tilesfx.tools.FlowGridPane;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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

	private static final String TIME_BY_REASON = "Percent Time by Reason";

	private static final float SEC_PER_DAY = 86400.0f;
	private static final float SEC_PER_HOUR = 3600.0f;
	private static final float SEC_PER_MIN = 60.0f;

	// 1 hour = 3600 sec
	private static final long MINS_AMOUNT = 3600;

	// 24 hours
	private static final long HOURS_AMOUNT = 24 * MINS_AMOUNT;

	// 1 30-day month
	private static final long DAYS_AMOUNT = 30 * HOURS_AMOUNT;

	private static final double TILE_WIDTH = 300;
	private static final double TILE_HEIGHT = TILE_WIDTH;
	private static final double TILE_VGAP = 20.0;
	private static final double TILE_HGAP = 20.0;

	private static final String OEE_FORMAT = "%.1f %%";
	private static final String PROD_FORMAT = "%.1f ";

	// cumulative production
	private boolean showCumulative = true;

	// the loss data
	private EquipmentLoss equipmentLoss;

	// selection criteria
	@FXML
	private DatePicker dpFromDate;

	@FXML
	private DatePicker dpToDate;

	@FXML
	private Button btRefresh;

	// container for dashboard tiles
	@FXML
	private AnchorPane apTileLayout;

	// production tile
	private Tile tiProduction;
	private LeaderBoardItem lbiGoodProduction;
	private LeaderBoardItem lbiRejectProduction;
	private LeaderBoardItem lbiStartupProduction;

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
	private ObservableList<Data<Number, String>> notScheduledList = FXCollections
			.observableArrayList(new ArrayList<>());

	// special events loss
	private ObservableList<Data<Number, String>> unscheduledList = FXCollections.observableArrayList(new ArrayList<>());

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
	private XYChart.Series<Number, String> notScheduledSeries = new XYChart.Series<>();

	// special events series
	private XYChart.Series<Number, String> unscheduledSeries = new XYChart.Series<>();

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
	private Unit timeUnit = Unit.MINUTE;

	// list of events
	private ObservableList<ResolvedEvent> resolvedEvents = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TableView<ResolvedEvent> tvResolvedEvents;

	@FXML
	private TableColumn<ResolvedEvent, Reason> tcAvailability;

	@FXML
	private TableColumn<ResolvedEvent, String> tcTimestamp;

	@FXML
	private TableColumn<ResolvedEvent, String> tcDuration;

	@FXML
	private TableColumn<ResolvedEvent, String> tcShift;

	@FXML
	private TableColumn<ResolvedEvent, String> tcReason;

	@FXML
	private TableColumn<ResolvedEvent, Text> tcLossCategory;

	@FXML
	private TableColumn<ResolvedEvent, String> tcLostTime;

	@FXML
	private TableColumn<ResolvedEvent, String> tcProdType;

	@FXML
	private TableColumn<ResolvedEvent, String> tcProdAmount;

	@FXML
	private TableColumn<ResolvedEvent, String> tcProdUnit;

	@FXML
	private TableColumn<ResolvedEvent, String> tcMaterial;

	@FXML
	private TableColumn<ResolvedEvent, String> tcJob;

	// loss chart
	@FXML
	private StackedBarChart<Number, String> bcLosses;

	// tab pane
	@FXML
	private TabPane tpParetoCharts;

	@FXML
	private Tab tbTimeLosses;

	@FXML
	private Tab tbEvents;

	@FXML
	private Tab tbFirstLevelPareto;

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

	@FXML
	private AnchorPane apLevel1Pareto;
	private StackPane spLevel1Pareto;

	@FXML
	private AnchorPane apYieldPareto;
	private StackPane spYieldPareto;

	@FXML
	private AnchorPane apRejectsPareto;
	private StackPane spRejectsPareto;

	@FXML
	private AnchorPane apSpeedPareto;
	private StackPane spSpeedPareto;

	@FXML
	private AnchorPane apMinorStoppagesPareto;
	private StackPane spMinorStoppagesPareto;

	@FXML
	private AnchorPane apUnplannedDowntimePareto;
	private StackPane spUnplannedDowntimePareto;

	@FXML
	private AnchorPane apSetupPareto;
	private StackPane spSetupPareto;

	@FXML
	private AnchorPane apPlannedDowntimePareto;
	private StackPane spPlannedDowntimePareto;

	public void setEquipmentLoss(EquipmentLoss equipmentLoss) {
		this.equipmentLoss = equipmentLoss;
	}

	private float determineTimeUnits(Duration duration) {
		float divisor = 1.0f;
		float seconds = duration.getSeconds();
		timeUnit = Unit.SECOND;

		// if more than 30 days, use days
		if (seconds > DAYS_AMOUNT) {
			divisor = SEC_PER_DAY;
			timeUnit = Unit.DAY;
		} else if (seconds > HOURS_AMOUNT) {
			// if more than 24 hours, use hours
			divisor = SEC_PER_HOUR;
			timeUnit = Unit.HOUR;
		} else if (seconds > MINS_AMOUNT) {
			// if more than 1 hour, use minutes
			divisor = SEC_PER_MIN;
			timeUnit = Unit.MINUTE;
		}
		return divisor;
	}

	private Float convertDuration(Duration duration, float divisor) {
		float time = ((float) duration.getSeconds()) / divisor;
		return new Float(time);
	}

	private void onSelectTimeLosses() {
		if (bcLosses.getData() == null || bcLosses.getData().size() == 0) {
			createLossChart();
		}
	}

	private void onSelectHistory() {
		List<BaseRecord> records = equipmentLoss.getEventRecords();

		resolvedEvents.clear();

		AvailabilityRecord lastAvailability = null;
		SetupRecord lastSetup = null;

		for (BaseRecord record : records) {
			ResolvedEvent event = new ResolvedEvent(record);

			if (event.getReason() == null && lastAvailability != null) {
				// set to previous event's reason
				event.setReason(lastAvailability.getReason());
			}

			if (event.getMaterial() == null && lastSetup != null) {
				// set to previous event's material
				event.setMaterial(lastSetup.getMaterial());
			}

			if (event.getJob() == null && lastSetup != null) {
				// set to previous event's job
				event.setJob(lastSetup.getJob());
			}

			resolvedEvents.add(event);

			if (record instanceof AvailabilityRecord) {
				lastAvailability = (AvailabilityRecord) record;
			}

			if (record instanceof SetupRecord) {
				lastSetup = (SetupRecord) record;
			}

		}

		tvResolvedEvents.refresh();
	}

	private void onSelectMinorStoppagesPareto() throws Exception {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.MINOR_STOPPAGES);

		Number divisor = equipmentLoss.getLoss(TimeLoss.MINOR_STOPPAGES).getSeconds();

		spMinorStoppagesPareto = new StackPane();

		AnchorPane.setBottomAnchor(spMinorStoppagesPareto, 0.0);
		AnchorPane.setLeftAnchor(spMinorStoppagesPareto, 0.0);
		AnchorPane.setRightAnchor(spMinorStoppagesPareto, 0.0);
		AnchorPane.setTopAnchor(spMinorStoppagesPareto, 0.0);

		apMinorStoppagesPareto.getChildren().clear();
		apMinorStoppagesPareto.getChildren().add(0, spMinorStoppagesPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart("Minor Stoppages Pareto", spMinorStoppagesPareto, items, divisor, TIME_BY_REASON);
	}

	private void onSelectRejectsPareto() throws Exception {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.REJECT_REWORK);

		Number divisor = equipmentLoss.getLoss(TimeLoss.REJECT_REWORK).getSeconds();

		spRejectsPareto = new StackPane();

		AnchorPane.setBottomAnchor(spRejectsPareto, 0.0);
		AnchorPane.setLeftAnchor(spRejectsPareto, 0.0);
		AnchorPane.setRightAnchor(spRejectsPareto, 0.0);
		AnchorPane.setTopAnchor(spRejectsPareto, 0.0);

		apRejectsPareto.getChildren().clear();
		apRejectsPareto.getChildren().add(0, spRejectsPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart("Rejects and Rework Pareto", spRejectsPareto, items, divisor, TIME_BY_REASON);
	}

	private void onSelectReducedSpeedPareto() throws Exception {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.REDUCED_SPEED);

		Number divisor = equipmentLoss.getLoss(TimeLoss.REDUCED_SPEED).getSeconds();

		spSpeedPareto = new StackPane();

		AnchorPane.setBottomAnchor(spSpeedPareto, 0.0);
		AnchorPane.setLeftAnchor(spSpeedPareto, 0.0);
		AnchorPane.setRightAnchor(spSpeedPareto, 0.0);
		AnchorPane.setTopAnchor(spSpeedPareto, 0.0);

		apSpeedPareto.getChildren().clear();
		apSpeedPareto.getChildren().add(0, spSpeedPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart("Reduced Speed Pareto", spSpeedPareto, items, divisor, TIME_BY_REASON);
	}

	private void onSelectStartupAndYieldPareto() throws Exception {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.STARTUP_YIELD);

		Number divisor = equipmentLoss.getLoss(TimeLoss.STARTUP_YIELD).getSeconds();

		spYieldPareto = new StackPane();

		AnchorPane.setBottomAnchor(spYieldPareto, 0.0);
		AnchorPane.setLeftAnchor(spYieldPareto, 0.0);
		AnchorPane.setRightAnchor(spYieldPareto, 0.0);
		AnchorPane.setTopAnchor(spYieldPareto, 0.0);

		apYieldPareto.getChildren().clear();
		apYieldPareto.getChildren().add(0, spYieldPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart("Startup And Yield Pareto", spYieldPareto, items, divisor, TIME_BY_REASON);
	}

	private void onSelectUnplannedDowntimePareto() throws Exception {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.UNPLANNED_DOWNTIME);

		Number divisor = equipmentLoss.getLoss(TimeLoss.UNPLANNED_DOWNTIME).getSeconds();

		spUnplannedDowntimePareto = new StackPane();

		AnchorPane.setBottomAnchor(spUnplannedDowntimePareto, 0.0);
		AnchorPane.setLeftAnchor(spUnplannedDowntimePareto, 0.0);
		AnchorPane.setRightAnchor(spUnplannedDowntimePareto, 0.0);
		AnchorPane.setTopAnchor(spUnplannedDowntimePareto, 0.0);

		apUnplannedDowntimePareto.getChildren().clear();
		apUnplannedDowntimePareto.getChildren().add(0, spUnplannedDowntimePareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart("Unplanned Downtime Pareto", spUnplannedDowntimePareto, items, divisor,
				TIME_BY_REASON);
	}

	private void onSelectSetupPareto() throws Exception {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.SETUP);

		Number divisor = equipmentLoss.getLoss(TimeLoss.SETUP).getSeconds();

		spSetupPareto = new StackPane();

		AnchorPane.setBottomAnchor(spSetupPareto, 0.0);
		AnchorPane.setLeftAnchor(spSetupPareto, 0.0);
		AnchorPane.setRightAnchor(spSetupPareto, 0.0);
		AnchorPane.setTopAnchor(spSetupPareto, 0.0);

		apSetupPareto.getChildren().clear();
		apSetupPareto.getChildren().add(0, spSetupPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart("Setup and Yield Pareto", spSetupPareto, items, divisor, TIME_BY_REASON);
	}

	private void onSelectPlannedDowntimePareto() throws Exception {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.PLANNED_DOWNTIME);

		Number divisor = equipmentLoss.getLoss(TimeLoss.PLANNED_DOWNTIME).getSeconds();

		spPlannedDowntimePareto = new StackPane();

		AnchorPane.setBottomAnchor(spPlannedDowntimePareto, 0.0);
		AnchorPane.setLeftAnchor(spPlannedDowntimePareto, 0.0);
		AnchorPane.setRightAnchor(spPlannedDowntimePareto, 0.0);
		AnchorPane.setTopAnchor(spPlannedDowntimePareto, 0.0);

		apPlannedDowntimePareto.getChildren().clear();
		apPlannedDowntimePareto.getChildren().add(0, spPlannedDowntimePareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart("Planned Downtime Pareto", spPlannedDowntimePareto, items, divisor,
				TIME_BY_REASON);
	}

	private void createLossChart() {
		// build data series
		netTimeList.clear();
		notScheduledList.clear();
		unscheduledList.clear();
		plannedDowntimeList.clear();
		setupList.clear();
		unplannedDowntimeList.clear();
		minorStoppageList.clear();
		reducedSpeedList.clear();
		rejectList.clear();
		yieldList.clear();

		netTimeSeries.setData(netTimeList);
		notScheduledSeries.setData(notScheduledList);
		unscheduledSeries.setData(unscheduledList);
		plannedDowntimeSeries.setData(plannedDowntimeList);
		setupSeries.setData(setupList);
		unplannedDowntimeSeries.setData(unplannedDowntimeList);
		minorStoppageSeries.setData(minorStoppageList);
		reducedSpeedSeries.setData(reducedSpeedList);
		rejectSeries.setData(rejectList);
		yieldSeries.setData(yieldList);

		List<XYChart.Data<Number, String>> netTimePoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> notScheduledPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> unscheduledPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> plannedDowntimePoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> setupPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> unplannedDowntimePoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> minorStoppagePoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> reducedSpeedPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> rejectPoints = new ArrayList<>();
		List<XYChart.Data<Number, String>> yieldPoints = new ArrayList<>();

		// x-axis time units
		float divisor = determineTimeUnits(equipmentLoss.getDuration());

		// value adding
		String category = TimeCategory.VALUE_ADDING.toString();
		Number netTime = convertDuration(equipmentLoss.getValueAddingTime(), divisor);
		Number yield = convertDuration(equipmentLoss.getLoss(TimeLoss.STARTUP_YIELD), divisor);

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		yieldPoints.add(new XYChart.Data<Number, String>(yield, category));

		// effective net production time
		category = TimeCategory.EFFECTIVE_NET_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getEffectiveNetProductionTime(), divisor);
		Number rejects = convertDuration(equipmentLoss.getLoss(TimeLoss.REJECT_REWORK), divisor);

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		rejectPoints.add(new XYChart.Data<Number, String>(rejects, category));

		// efficient net production time
		category = TimeCategory.EFFICIENT_NET_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getEfficientNetProductionTime(), divisor);
		Number reducedSpeed = convertDuration(equipmentLoss.getLoss(TimeLoss.REDUCED_SPEED), divisor);

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		reducedSpeedPoints.add(new XYChart.Data<Number, String>(reducedSpeed, category));

		// net production time
		category = TimeCategory.NET_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getNetProductionTime(), divisor);
		Number minorStoppagesLoss = convertDuration(equipmentLoss.getLoss(TimeLoss.MINOR_STOPPAGES), divisor);

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));

		XYChart.Data<Number, String> minorStoppagesData = new XYChart.Data<Number, String>(minorStoppagesLoss,
				category);

		minorStoppagePoints.add(minorStoppagesData);

		// reported production time
		category = TimeCategory.REPORTED_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getReportedProductionTime(), divisor);
		Number unplannedDowntime = convertDuration(equipmentLoss.getLoss(TimeLoss.UNPLANNED_DOWNTIME), divisor);

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		unplannedDowntimePoints.add(new XYChart.Data<Number, String>(unplannedDowntime, category));

		// production time
		category = TimeCategory.PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getProductionTime(), divisor);
		Number setup = convertDuration(equipmentLoss.getLoss(TimeLoss.SETUP), divisor);

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		setupPoints.add(new XYChart.Data<Number, String>(setup, category));

		// scheduled time
		category = TimeCategory.SCHEDULED_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getScheduledProductionTime(), divisor);
		Number plannedDowntime = convertDuration(equipmentLoss.getLoss(TimeLoss.PLANNED_DOWNTIME), divisor);

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		plannedDowntimePoints.add(new XYChart.Data<Number, String>(plannedDowntime, category));

		// available time
		category = TimeCategory.AVAILABLE.toString();
		netTime = convertDuration(equipmentLoss.getAvailableTime(), divisor);
		Number specialEventsLosses = convertDuration(equipmentLoss.getLoss(TimeLoss.UNSCHEDULED), divisor);

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		unscheduledPoints.add(new XYChart.Data<Number, String>(specialEventsLosses, category));

		// operations time
		category = TimeCategory.REQUIRED_OPERATIONS.toString();
		netTime = convertDuration(equipmentLoss.getRequiredOperationsTime(), divisor);
		Number noDemand = convertDuration(equipmentLoss.getLoss(TimeLoss.NOT_SCHEDULED), divisor);

		netTimePoints.add(new XYChart.Data<Number, String>(netTime, category));
		notScheduledPoints.add(new XYChart.Data<Number, String>(noDemand, category));

		// add the data to each series
		netTimeList.addAll(netTimePoints);
		notScheduledList.addAll(notScheduledPoints);
		unscheduledList.addAll(unscheduledPoints);
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

		bcLosses.setTitle(chartTitle + " (" + timeUnit + ")");
		bcLosses.setAnimated(false);

		if (bcLosses.getData() != null) {
			bcLosses.getData().clear();
		}

		// net times
		netTimeSeries.setName(NET_TIME_SERIES);
		bcLosses.getData().add(netTimeSeries);

		// no demand
		notScheduledSeries.setName(TimeLoss.NOT_SCHEDULED.toString());
		bcLosses.getData().add(notScheduledSeries);

		// special events
		unscheduledSeries.setName(TimeLoss.UNSCHEDULED.toString());
		bcLosses.getData().add(unscheduledSeries);

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
		float quality = equipmentLoss.calculateQualityPercentage();
		bciQuality.setValue(quality);

		float performance = equipmentLoss.calculatePerformancePercentage();
		bciPerformance.setValue(performance);

		float availability = equipmentLoss.calculateAvailabilityPercentage();
		bciAvailability.setValue(availability);

		float oee = equipmentLoss.calculateOeePercentage();
		bciOee.setValue(oee);
	}

	private void onSelectFirstLevelPareto() throws Exception {
		Duration availableTime = equipmentLoss.getAvailableTime();
		Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

		spLevel1Pareto = new StackPane();

		AnchorPane.setBottomAnchor(spLevel1Pareto, 0.0);
		AnchorPane.setLeftAnchor(spLevel1Pareto, 0.0);
		AnchorPane.setRightAnchor(spLevel1Pareto, 0.0);
		AnchorPane.setTopAnchor(spLevel1Pareto, 0.0);

		apLevel1Pareto.getChildren().clear();
		apLevel1Pareto.getChildren().add(0, spLevel1Pareto);

		List<ParetoItem> paretoItems = equipmentLoss.getLossItems(timeUnit);

		ParetoChartController level1Controller = new ParetoChartController();
		level1Controller.setCategoryClickListener(this);
		level1Controller.createParetoChart("First-Level Pareto", spLevel1Pareto, paretoItems, divisor, "Loss Category");
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

	@Override
	protected void setImages() throws Exception {
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
		btRefresh.setContentDisplay(ContentDisplay.LEFT);
	}

	@FXML
	public void initialize() throws Exception {
		setImages();

		// listen to tab selections
		tpParetoCharts.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {

			if (newValue == null || equipmentLoss == null) {
				return;
			}

			try {
				refreshCharts(newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		buildDashboardTiles();

		initializeEventTable();
	}

	private void initializeEventTable() {
		// server status table
		tvResolvedEvents.setItems(resolvedEvents);

		// loss category
		tcAvailability.setCellFactory(column -> {
			return new TableCell<ResolvedEvent, Reason>() {
				@Override
				protected void updateItem(Reason reason, boolean empty) {
					super.updateItem(reason, empty);

					if (reason != null && reason.getLossCategory() != null) {
						Color color = reason.getLossCategory().getColor();

						// remove 0x
						setStyle("-fx-background-color: #" + color.toString().substring(2));
					}
				}
			};
		});

		tcAvailability.setCellValueFactory(cellDataFeatures -> {
			// set the reason
			ResolvedEvent event = cellDataFeatures.getValue();
			return new SimpleObjectProperty<Reason>(event.getReason());

		});

		// time
		tcTimestamp.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(AppUtils.formatOffsetDateTime(cellDataFeatures.getValue().getStartTime()));
		});

		// duration
		tcDuration.setCellValueFactory(cellDataFeatures -> {
			ResolvedEvent event = cellDataFeatures.getValue();

			SimpleStringProperty property = null;

			if (event.getDuration() != null) {
				property = new SimpleStringProperty(DomainUtils.formatDuration(event.getDuration()));
			}
			return property;
		});

		// shift
		tcShift.setCellValueFactory(cellDataFeatures -> {
			ResolvedEvent event = cellDataFeatures.getValue();

			SimpleStringProperty property = null;

			if (event.getShift() != null) {
				property = new SimpleStringProperty(event.getShift().getName());
			}
			return property;
		});

		// reason
		tcReason.setCellValueFactory(cellDataFeatures -> {
			ResolvedEvent event = cellDataFeatures.getValue();
			SimpleStringProperty property = null;

			if (event.getResolverType().equals(EventResolverType.AVAILABILITY)) {
				property = new SimpleStringProperty(event.getReason().getName());
			}
			return property;
		});

		// loss category
		tcLossCategory.setCellValueFactory(cellDataFeatures -> {
			ResolvedEvent event = cellDataFeatures.getValue();
			SimpleObjectProperty<Text> lossProperty = null;
			Reason reason = event.getReason();

			if (event.getResolverType().equals(EventResolverType.AVAILABILITY)) {
				Color color = reason.getLossCategory().getColor();
				Text text = new Text(reason.getLossCategory().toString());
				text.setFill(color);
				lossProperty = new SimpleObjectProperty<Text>(text);
			}
			return lossProperty;
		});

		// lost production time
		tcLostTime.setCellValueFactory(cellDataFeatures -> {
			ResolvedEvent event = cellDataFeatures.getValue();
			return new SimpleStringProperty(DomainUtils.formatDuration(event.getLostTime()));
		});

		// production type
		tcProdType.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			ResolvedEvent event = cellDataFeatures.getValue();

			if (event.getResolverType() != null) {
				property = new SimpleStringProperty(event.getResolverType().toString());
			}
			return property;
		});

		// production amount
		tcProdAmount.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			ResolvedEvent event = cellDataFeatures.getValue();

			if (event.getQuantity() != null) {
				property = new SimpleStringProperty(AppUtils.formatDouble(event.getQuantity().getAmount()));
			}
			return property;
		});

		// production UOM
		tcProdUnit.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			ResolvedEvent event = cellDataFeatures.getValue();

			if (event.getQuantity() != null) {
				property = new SimpleStringProperty(event.getQuantity().getUOM().getSymbol());
			}
			return property;
		});

		// material
		tcMaterial.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			ResolvedEvent event = cellDataFeatures.getValue();

			if (event.getMaterial() != null) {
				property = new SimpleStringProperty(event.getMaterial().getName());
			}
			return property;
		});

		// job
		tcJob.setCellValueFactory(cellDataFeatures -> {
			ResolvedEvent event = cellDataFeatures.getValue();
			return new SimpleStringProperty(event.getJob());
		});

	}

	private void refreshCharts(Tab newValue) throws Exception {
		String id = newValue.getId();

		if (id.equals(tbTimeLosses.getId())) {
			onSelectTimeLosses();
		} else if (id.equals(tbFirstLevelPareto.getId())) {
			onSelectFirstLevelPareto();
		} else if (id.equals(tbYieldPareto.getId())) {
			onSelectStartupAndYieldPareto();
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
		} else if (id.equals(tbEvents.getId())) {
			onSelectHistory();
		}
	}

	public void buildDashboardTiles() {
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
				.animated(false).build();

		// production tile
		lbiGoodProduction = new LeaderBoardItem("Good", 0);
		lbiRejectProduction = new LeaderBoardItem("Reject", 0);
		lbiStartupProduction = new LeaderBoardItem("Startup", 0);

		String productionText = "Cumulative Quantity";

		if (!showCumulative) {
			productionText = "Change in Quantity";
		}

		tiProduction = TileBuilder.create().skinType(SkinType.LEADER_BOARD).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title("Current Production").text(productionText)
				.leaderBoardItems(lbiGoodProduction, lbiRejectProduction, lbiStartupProduction).sortedData(false)
				.animated(false).build();

		// availability tile
		tiAvailability = TileBuilder.create().skinType(SkinType.TEXT).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title("Availability").textVisible(true).descriptionAlignment(Pos.CENTER).build();
		tiAvailability.setDescriptionTextSize(TextSize.BIGGER);

		// material and job
		tiJobMaterial = TileBuilder.create().skinType(SkinType.TEXT).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title("Material and Job").textVisible(true).descriptionAlignment(Pos.CENTER).build();
		tiJobMaterial.setDescriptionTextSize(TextSize.NORMAL);
		tiJobMaterial.setDescriptionColor(Color.WHITE);

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
	}

	public void update(CollectorResolvedEventMessage message) throws Exception {
		EventResolverType resolverType = message.getResolverType();

		switch (resolverType) {
		case AVAILABILITY: {
			// availability reason
			tiAvailability.setText(message.getReasonName() + " (" + message.getReasonDescription() + ")");

			// loss category
			TimeLoss loss = message.getLoss();
			tiAvailability.setDescription(loss.toString());
			tiAvailability.setTextColor(loss.getColor());
			break;
		}

		case JOB_CHANGE: {
			// job
			tiJobMaterial.setText(message.getJob());
			break;
		}

		case MATL_CHANGE: {
			// material
			String displayString = message.getMaterialName() + " (" + message.getMaterialDescription() + ")";
			tiJobMaterial.setDescription(displayString);
			break;
		}

		case PROD_GOOD: {
			// good production
			UnitOfMeasure uom = MeasurementSystem.instance().getUOM(message.getUom());
			Quantity delta = new Quantity(message.getAmount(), uom);
			Quantity good = equipmentLoss.incrementGoodQuantity(delta);
			lbiGoodProduction.setValue(good.getAmount(), false);
			break;
		}

		case PROD_REJECT: {
			// reject and rework
			UnitOfMeasure uom = MeasurementSystem.instance().getUOM(message.getUom());
			Quantity delta = new Quantity(message.getAmount(), uom);
			Quantity reject = equipmentLoss.incrementRejectQuantity(delta);
			lbiRejectProduction.setValue(reject.getAmount(), true);
			break;
		}

		case PROD_STARTUP: {
			// startup and yield
			UnitOfMeasure uom = MeasurementSystem.instance().getUOM(message.getUom());
			Quantity delta = new Quantity(message.getAmount(), uom);
			Quantity startup = equipmentLoss.incrementStartupQuantity(delta);
			lbiStartupProduction.setValue(startup.getAmount(), true);
			break;
		}

		default:
			break;
		}
	}

	@FXML
	private void onRefresh() {
		try {
			// clear previous calculation
			equipmentLoss.reset();

			// time period
			LocalDate from = dpFromDate.getValue();

			if (from == null) {
				from = LocalDate.now();
			}
			LocalDateTime ldtFrom = LocalDateTime.of(from, LocalTime.MIN);

			LocalDate to = dpToDate.getValue();
			if (to == null) {
				to = LocalDate.now();
			}
			LocalDateTime ldtTo = LocalDateTime.of(to, LocalTime.MAX);

			if (ldtTo.isBefore(ldtFrom)) {
				throw new Exception("The starting time " + ldtFrom + " must be before the ending time " + ldtTo);
			}

			OffsetDateTime odtFrom = DomainUtils.fromLocalDateTime(ldtFrom);
			OffsetDateTime odtTo = DomainUtils.fromLocalDateTime(ldtTo);

			// equipment
			Equipment equipment = equipmentLoss.getEquipment();

			if (equipment == null) {
				throw new Exception("Equipment must be selected.");
			}

			// material and job during this period from setups
			List<SetupRecord> setups = PersistenceService.instance().fetchSetupsForPeriod(equipment, odtFrom, odtTo);

			if (setups.size() == 0) {
				throw new Exception(
						"The material being produced must be specified for equipment " + equipment.getName());
			}
			
			// add setup events
			equipmentLoss.getEventRecords().addAll(setups);

			// step through each setup period since materials could have changed
			for (SetupRecord setup : setups) {
				equipmentLoss.setMaterial(setup.getMaterial());

				// calculate the time losses over the setup period
				OffsetDateTime periodStart = setup.getStartTime();

				if (periodStart.compareTo(odtFrom) < 0) {
					periodStart = odtFrom;
				}

				OffsetDateTime periodEnd = setup.getEndTime();

				if (periodEnd == null || (periodEnd.compareTo(odtTo) > 0)) {
					periodEnd = odtTo;
				}

				EquipmentLossManager.calculateEquipmentLoss(equipmentLoss, periodStart, periodEnd);
			}

			// last setup
			SetupRecord lastSetup = setups.get(setups.size() - 1);

			tiJobMaterial.setDescription(lastSetup.getMaterial().getDisplayString());
			tiJobMaterial.setText(lastSetup.getJob());

			// show the production
			String symbol = null;
			double amount = 0.0d;

			Quantity goodQty = equipmentLoss.getGoodQuantity();

			if (goodQty != null) {
				symbol = goodQty.getUOM().getSymbol();
				amount = goodQty.getAmount();
			} else {
				symbol = "";
				amount = 0.0d;
			}
			lbiGoodProduction.setFormatString(PROD_FORMAT + " " + symbol);
			lbiGoodProduction.setValue(amount, false);

			Quantity rejectQty = equipmentLoss.getRejectQuantity();

			if (rejectQty != null) {
				symbol = rejectQty.getUOM().getSymbol();
				amount = rejectQty.getAmount();
			} else {
				symbol = "";
				amount = 0.0d;
			}
			lbiRejectProduction.setFormatString(PROD_FORMAT + " " + symbol);
			lbiRejectProduction.setValue(amount, true);

			Quantity startupQty = equipmentLoss.getStartupQuantity();

			if (startupQty != null) {
				symbol = startupQty.getUOM().getSymbol();
				amount = startupQty.getAmount();
			} else {
				symbol = "";
				amount = 0.0d;
			}
			lbiStartupProduction.setFormatString(PROD_FORMAT + " " + symbol);
			lbiStartupProduction.setValue(amount, true);

			// show last availability and setup records
			List<BaseRecord> historyRecords = equipmentLoss.getEventRecords();

			// sort by start time
			Collections.sort(historyRecords, new Comparator<BaseRecord>() {
				public int compare(BaseRecord record1, BaseRecord record2) {

					return record1.getStartTime().compareTo(record2.getStartTime());
				}
			});

			AvailabilityRecord lastAvailability = null;

			for (int i = historyRecords.size() - 1; i == 0; i--) {
				if (historyRecords.get(i) instanceof AvailabilityRecord) {
					lastAvailability = (AvailabilityRecord) historyRecords.get(i);
					break;
				}
			}

			// last availability
			if (lastAvailability != null) {
				// availability reason
				Reason reason = lastAvailability.getReason();

				tiAvailability.setText(reason.getName() + " (" + reason.getDescription() + ")");

				// loss category
				TimeLoss loss = reason.getLossCategory();
				if (loss != null) {
					tiAvailability.setDescription(loss.toString());
					tiAvailability.setDescriptionColor(loss.getColor());
				}
			}

			// show stats in tiles
			showStatistics();

			// display the selected tab
			refreshCharts(tpParetoCharts.getSelectionModel().getSelectedItem());

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
