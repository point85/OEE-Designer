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
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.charts.CategoryClickListener;
import org.point85.app.charts.ParetoChartController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.app.monitor.OeeEventTrendController;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.oee.EquipmentLoss;
import org.point85.domain.oee.EquipmentLossManager;
import org.point85.domain.oee.ParetoItem;
import org.point85.domain.oee.TimeCategory;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.tilesfx.Tile;
import org.point85.tilesfx.Tile.SkinType;
import org.point85.tilesfx.Tile.TextSize;
import org.point85.tilesfx.TileBuilder;
import org.point85.tilesfx.chart.ChartData;
import org.point85.tilesfx.skins.BarChartItem;
import org.point85.tilesfx.skins.LeaderBoardItem;
import org.point85.tilesfx.tools.FlowGridPane;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DashboardController extends DialogController implements CategoryClickListener {
	private static final float SEC_PER_DAY = 86400.0f;
	private static final float SEC_PER_HOUR = 3600.0f;
	private static final float SEC_PER_MIN = 60.0f;

	// 1 hour = 3600 sec
	private static final long MINS_AMOUNT = 3600;

	// 24 hours
	private static final long HOURS_AMOUNT = 24 * MINS_AMOUNT;

	// 1 30-day month
	private static final long DAYS_AMOUNT = 30 * HOURS_AMOUNT;

	private static final double TILE_WIDTH = 300.0;
	private static final double TILE_HEIGHT = TILE_WIDTH;
	private static final double TILE_WIDE = 400.0;
	private static final double TILE_VGAP = 20.0;
	private static final double TILE_HGAP = 20.0;

	private static final String OEE_FORMAT = "%.1f %%";
	private static final String PROD_FORMAT = "%.1f ";

	// time between auto refresh
	private static final long REFRESH_SEC = 60;

	// manual event id
	private static final String EDITOR_SOURCE_ID = "EDITOR";

	// auto refresh timer
	private Timer refreshTimer;

	// refresh task
	private RefreshTask refreshTask;

	// the loss data for the current equipment
	private EquipmentLoss equipmentLoss;

	// map of loss by equipment
	private final ConcurrentMap<String, EquipmentLoss> lossMap = new ConcurrentHashMap<>();

	// selection criteria
	@FXML
	private DatePicker dpStartDate;

	@FXML
	private TextField tfStartTime;

	@FXML
	private DatePicker dpEndDate;

	@FXML
	private TextField tfEndTime;

	@FXML
	private ComboBox<String> cbMaterials;

	@FXML
	private Button btRefresh;

	@FXML
	private CheckBox cbAutoRefresh;

	@FXML
	private TextField tfRefreshPeriod;

	@FXML
	private Label lblNotification;

	// event editors
	@FXML
	private Button btNewAvailability;

	@FXML
	private Button btNewProduction;

	@FXML
	private Button btNewSetup;

	@FXML
	private Button btUpdateEvent;

	@FXML
	private Button btDeleteEvent;

	@FXML
	private Button btOeeTrend;

	// availability controller
	private AvailabilityEditorController availabilityEditorController;

	// setup controller
	private SetupEditorController setupEditorController;

	// production controller
	private ProductionEditorController productionEditorController;

	// container for dashboard tiles
	@FXML
	private AnchorPane apTileLayout;

	// production tile
	private LeaderBoardItem lbiGoodProduction;
	private LeaderBoardItem lbiRejectProduction;
	private LeaderBoardItem lbiStartupProduction;

	// OEE tile
	private BarChartItem bciOee;
	private BarChartItem bciAvailability;
	private BarChartItem bciPerformance;
	private BarChartItem bciQuality;

	// OEE tile
	private Tile tiOee;

	// availability tile
	private Tile tiAvailability;

	// production tile
	private Tile tiProduction;

	// job and material tile
	private Tile tiJobMaterial;

	// time loss tile
	private Tile tiLoss;

	// net times
	private final ObservableList<Data<Number, String>> netTimeList = FXCollections
			.observableArrayList(new ArrayList<>());

	// no demand loss
	private final ObservableList<Data<Number, String>> notScheduledList = FXCollections
			.observableArrayList(new ArrayList<>());

	// special events loss
	private final ObservableList<Data<Number, String>> unscheduledList = FXCollections
			.observableArrayList(new ArrayList<>());

	// planned downtime
	private final ObservableList<Data<Number, String>> plannedDowntimeList = FXCollections
			.observableArrayList(new ArrayList<>());

	// setup
	private final ObservableList<Data<Number, String>> setupList = FXCollections.observableArrayList(new ArrayList<>());

	// unplanned downtime
	private final ObservableList<Data<Number, String>> unplannedDowntimeList = FXCollections
			.observableArrayList(new ArrayList<>());

	// minor stoppages loss
	private final ObservableList<Data<Number, String>> minorStoppageList = FXCollections
			.observableArrayList(new ArrayList<>());

	// reduced speed loss
	private final ObservableList<Data<Number, String>> reducedSpeedList = FXCollections
			.observableArrayList(new ArrayList<>());

	// rejects and rework loss
	private final ObservableList<Data<Number, String>> rejectList = FXCollections
			.observableArrayList(new ArrayList<>());

	// yield loss
	private final ObservableList<Data<Number, String>> yieldList = FXCollections.observableArrayList(new ArrayList<>());

	// times series net of the loss category
	private final XYChart.Series<Number, String> netTimeSeries = new XYChart.Series<>();

	// no demand series
	private final XYChart.Series<Number, String> notScheduledSeries = new XYChart.Series<>();

	// special events series
	private final XYChart.Series<Number, String> unscheduledSeries = new XYChart.Series<>();

	// planned downtime series
	private final XYChart.Series<Number, String> plannedDowntimeSeries = new XYChart.Series<>();

	// setup series
	private final XYChart.Series<Number, String> setupSeries = new XYChart.Series<>();

	// unplanned downtime series
	private final XYChart.Series<Number, String> unplannedDowntimeSeries = new XYChart.Series<>();

	// minor stoppages series
	private final XYChart.Series<Number, String> minorStoppageSeries = new XYChart.Series<>();

	// reduced speed series
	private final XYChart.Series<Number, String> reducedSpeedSeries = new XYChart.Series<>();

	// rejects series
	private final XYChart.Series<Number, String> rejectSeries = new XYChart.Series<>();

	// yield series
	private final XYChart.Series<Number, String> yieldSeries = new XYChart.Series<>();

	// x-axis time unit
	private Unit timeUnit = Unit.MINUTE;

	// list of events
	private final ObservableList<OeeEvent> resolvedEvents = FXCollections.observableArrayList(new ArrayList<>());

	// last availability event
	private OeeEvent lastAvailability;

	@FXML
	private TableView<OeeEvent> tvResolvedEvents;

	@FXML
	private TableColumn<OeeEvent, Reason> tcAvailability;

	@FXML
	private TableColumn<OeeEvent, String> tcStartTime;

	@FXML
	private TableColumn<OeeEvent, String> tcEndTime;

	@FXML
	private TableColumn<OeeEvent, String> tcDuration;

	@FXML
	private TableColumn<OeeEvent, String> tcShift;

	@FXML
	private TableColumn<OeeEvent, String> tcTeam;

	@FXML
	private TableColumn<OeeEvent, String> tcReason;

	@FXML
	private TableColumn<OeeEvent, Text> tcLossCategory;

	@FXML
	private TableColumn<OeeEvent, String> tcLostTime;

	@FXML
	private TableColumn<OeeEvent, String> tcProdType;

	@FXML
	private TableColumn<OeeEvent, String> tcProdAmount;

	@FXML
	private TableColumn<OeeEvent, String> tcProdUnit;

	@FXML
	private TableColumn<OeeEvent, String> tcMaterial;

	@FXML
	private TableColumn<OeeEvent, String> tcJob;

	@FXML
	private TableColumn<OeeEvent, String> tcSourceId;

	@FXML
	private TableColumn<OeeEvent, String> tcCollector;

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

	@FXML
	private AnchorPane apYieldPareto;

	@FXML
	private AnchorPane apRejectsPareto;

	@FXML
	private AnchorPane apSpeedPareto;

	@FXML
	private AnchorPane apMinorStoppagesPareto;

	@FXML
	private AnchorPane apUnplannedDowntimePareto;

	@FXML
	private AnchorPane apSetupPareto;

	@FXML
	private AnchorPane apPlannedDowntimePareto;

	private void postNotification(String message) {
		lblNotification.setText(message);
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
		return (duration.getSeconds()) / divisor;
	}

	private void onSelectTimeLosses() {
		if (bcLosses.getData() == null || bcLosses.getData().isEmpty()) {
			createLossChart();
		}
	}

	private void onSelectHistory() {
		List<OeeEvent> records = equipmentLoss.getEventRecords();

		resolvedEvents.clear();

		for (OeeEvent event : records) {
			resolvedEvents.add(event);
		}

		tvResolvedEvents.refresh();
	}

	private void onSelectMinorStoppagesPareto() {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.MINOR_STOPPAGES);

		Number divisor = equipmentLoss.getLoss(TimeLoss.MINOR_STOPPAGES).getSeconds();

		StackPane spMinorStoppagesPareto = new StackPane();

		AnchorPane.setBottomAnchor(spMinorStoppagesPareto, 0.0);
		AnchorPane.setLeftAnchor(spMinorStoppagesPareto, 0.0);
		AnchorPane.setRightAnchor(spMinorStoppagesPareto, 0.0);
		AnchorPane.setTopAnchor(spMinorStoppagesPareto, 0.0);

		apMinorStoppagesPareto.getChildren().clear();
		apMinorStoppagesPareto.getChildren().add(0, spMinorStoppagesPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart(DesignerLocalizer.instance().getLangString("stoppages.pareto"),
				spMinorStoppagesPareto, items, divisor, DesignerLocalizer.instance().getLangString("time.by.reason"));
	}

	private void onSelectRejectsPareto() {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.REJECT_REWORK);

		Number divisor = equipmentLoss.getLoss(TimeLoss.REJECT_REWORK).getSeconds();

		StackPane spRejectsPareto = new StackPane();

		AnchorPane.setBottomAnchor(spRejectsPareto, 0.0);
		AnchorPane.setLeftAnchor(spRejectsPareto, 0.0);
		AnchorPane.setRightAnchor(spRejectsPareto, 0.0);
		AnchorPane.setTopAnchor(spRejectsPareto, 0.0);

		apRejectsPareto.getChildren().clear();
		apRejectsPareto.getChildren().add(0, spRejectsPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart(DesignerLocalizer.instance().getLangString("rejects.pareto"), spRejectsPareto,
				items, divisor, DesignerLocalizer.instance().getLangString("time.by.reason"));
	}

	private void onSelectReducedSpeedPareto() {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.REDUCED_SPEED);

		Number divisor = equipmentLoss.getLoss(TimeLoss.REDUCED_SPEED).getSeconds();

		StackPane spSpeedPareto = new StackPane();

		AnchorPane.setBottomAnchor(spSpeedPareto, 0.0);
		AnchorPane.setLeftAnchor(spSpeedPareto, 0.0);
		AnchorPane.setRightAnchor(spSpeedPareto, 0.0);
		AnchorPane.setTopAnchor(spSpeedPareto, 0.0);

		apSpeedPareto.getChildren().clear();
		apSpeedPareto.getChildren().add(0, spSpeedPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart(DesignerLocalizer.instance().getLangString("speed.pareto"), spSpeedPareto, items,
				divisor, DesignerLocalizer.instance().getLangString("time.by.reason"));
	}

	private void onSelectStartupAndYieldPareto() {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.STARTUP_YIELD);

		Number divisor = equipmentLoss.getLoss(TimeLoss.STARTUP_YIELD).getSeconds();

		StackPane spYieldPareto = new StackPane();

		AnchorPane.setBottomAnchor(spYieldPareto, 0.0);
		AnchorPane.setLeftAnchor(spYieldPareto, 0.0);
		AnchorPane.setRightAnchor(spYieldPareto, 0.0);
		AnchorPane.setTopAnchor(spYieldPareto, 0.0);

		apYieldPareto.getChildren().clear();
		apYieldPareto.getChildren().add(0, spYieldPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart(DesignerLocalizer.instance().getLangString("startup.pareto"), spYieldPareto, items,
				divisor, DesignerLocalizer.instance().getLangString("time.by.reason"));
	}

	private void onSelectUnplannedDowntimePareto() {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.UNPLANNED_DOWNTIME);

		Number divisor = equipmentLoss.getLoss(TimeLoss.UNPLANNED_DOWNTIME).getSeconds();

		StackPane spUnplannedDowntimePareto = new StackPane();

		AnchorPane.setBottomAnchor(spUnplannedDowntimePareto, 0.0);
		AnchorPane.setLeftAnchor(spUnplannedDowntimePareto, 0.0);
		AnchorPane.setRightAnchor(spUnplannedDowntimePareto, 0.0);
		AnchorPane.setTopAnchor(spUnplannedDowntimePareto, 0.0);

		apUnplannedDowntimePareto.getChildren().clear();
		apUnplannedDowntimePareto.getChildren().add(0, spUnplannedDowntimePareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart(DesignerLocalizer.instance().getLangString("downtime.pareto"),
				spUnplannedDowntimePareto, items, divisor,
				DesignerLocalizer.instance().getLangString("time.by.reason"));
	}

	private void onSelectSetupPareto() {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.SETUP);

		Number divisor = equipmentLoss.getLoss(TimeLoss.SETUP).getSeconds();

		StackPane spSetupPareto = new StackPane();

		AnchorPane.setBottomAnchor(spSetupPareto, 0.0);
		AnchorPane.setLeftAnchor(spSetupPareto, 0.0);
		AnchorPane.setRightAnchor(spSetupPareto, 0.0);
		AnchorPane.setTopAnchor(spSetupPareto, 0.0);

		apSetupPareto.getChildren().clear();
		apSetupPareto.getChildren().add(0, spSetupPareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart(DesignerLocalizer.instance().getLangString("setup.pareto"), spSetupPareto, items,
				divisor, DesignerLocalizer.instance().getLangString("time.by.reason"));
	}

	private void onSelectPlannedDowntimePareto() {
		List<ParetoItem> items = EquipmentLossManager.getParetoData(equipmentLoss, TimeLoss.PLANNED_DOWNTIME);

		Number divisor = equipmentLoss.getLoss(TimeLoss.PLANNED_DOWNTIME).getSeconds();

		StackPane spPlannedDowntimePareto = new StackPane();

		AnchorPane.setBottomAnchor(spPlannedDowntimePareto, 0.0);
		AnchorPane.setLeftAnchor(spPlannedDowntimePareto, 0.0);
		AnchorPane.setRightAnchor(spPlannedDowntimePareto, 0.0);
		AnchorPane.setTopAnchor(spPlannedDowntimePareto, 0.0);

		apPlannedDowntimePareto.getChildren().clear();
		apPlannedDowntimePareto.getChildren().add(0, spPlannedDowntimePareto);

		ParetoChartController controller = new ParetoChartController();
		controller.createParetoChart(DesignerLocalizer.instance().getLangString("planned.downtime.pareto"),
				spPlannedDowntimePareto, items, divisor, DesignerLocalizer.instance().getLangString("time.by.reason"));
	}

	private void clearLossData() {
		if (bcLosses.getData() != null) {
			bcLosses.getData().clear();
		}
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
		Number startupYield = convertDuration(equipmentLoss.getLoss(TimeLoss.STARTUP_YIELD), divisor);

		netTimePoints.add(new XYChart.Data<>(netTime, category));
		yieldPoints.add(new XYChart.Data<>(startupYield, category));

		// effective net production time
		category = TimeCategory.EFFECTIVE_NET_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getEffectiveNetProductionTime(), divisor);
		Number rejects = convertDuration(equipmentLoss.getLoss(TimeLoss.REJECT_REWORK), divisor);

		netTimePoints.add(new XYChart.Data<>(netTime, category));
		rejectPoints.add(new XYChart.Data<>(rejects, category));

		// efficient net production time
		category = TimeCategory.EFFICIENT_NET_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getEfficientNetProductionTime(), divisor);
		Number reducedSpeed = convertDuration(equipmentLoss.getLoss(TimeLoss.REDUCED_SPEED), divisor);

		netTimePoints.add(new XYChart.Data<>(netTime, category));
		reducedSpeedPoints.add(new XYChart.Data<>(reducedSpeed, category));

		// net production time
		category = TimeCategory.NET_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getNetProductionTime(), divisor);
		Number minorStoppagesLoss = convertDuration(equipmentLoss.getLoss(TimeLoss.MINOR_STOPPAGES), divisor);

		netTimePoints.add(new XYChart.Data<>(netTime, category));

		XYChart.Data<Number, String> minorStoppagesData = new XYChart.Data<>(minorStoppagesLoss, category);

		minorStoppagePoints.add(minorStoppagesData);

		// reported production time
		category = TimeCategory.REPORTED_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getReportedProductionTime(), divisor);
		Number unplannedDowntime = convertDuration(equipmentLoss.getLoss(TimeLoss.UNPLANNED_DOWNTIME), divisor);

		netTimePoints.add(new XYChart.Data<>(netTime, category));
		unplannedDowntimePoints.add(new XYChart.Data<>(unplannedDowntime, category));

		// production time
		category = TimeCategory.PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getProductionTime(), divisor);
		Number setup = convertDuration(equipmentLoss.getLoss(TimeLoss.SETUP), divisor);

		netTimePoints.add(new XYChart.Data<>(netTime, category));
		setupPoints.add(new XYChart.Data<>(setup, category));

		// scheduled time
		category = TimeCategory.SCHEDULED_PRODUCTION.toString();
		netTime = convertDuration(equipmentLoss.getScheduledProductionTime(), divisor);
		Number plannedDowntime = convertDuration(equipmentLoss.getLoss(TimeLoss.PLANNED_DOWNTIME), divisor);

		netTimePoints.add(new XYChart.Data<>(netTime, category));
		plannedDowntimePoints.add(new XYChart.Data<>(plannedDowntime, category));

		// available time
		category = TimeCategory.AVAILABLE.toString();
		netTime = convertDuration(equipmentLoss.getAvailableTime(), divisor);
		Number specialEventsLosses = convertDuration(equipmentLoss.getLoss(TimeLoss.UNSCHEDULED), divisor);

		netTimePoints.add(new XYChart.Data<>(netTime, category));
		unscheduledPoints.add(new XYChart.Data<>(specialEventsLosses, category));

		// operations time
		category = TimeCategory.REQUIRED_OPERATIONS.toString();
		netTime = convertDuration(equipmentLoss.getRequiredOperationsTime(), divisor);
		Number noDemand = convertDuration(equipmentLoss.getLoss(TimeLoss.NOT_SCHEDULED), divisor);

		netTimePoints.add(new XYChart.Data<>(netTime, category));
		notScheduledPoints.add(new XYChart.Data<>(noDemand, category));

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
		categoryAxis.setLabel(DesignerLocalizer.instance().getLangString("time.cats"));

		NumberAxis timeAxis = new NumberAxis();
		timeAxis.setLabel(DesignerLocalizer.instance().getLangString("time.axis", timeUnit));
		timeAxis.setAutoRanging(true);
		timeAxis.setSide(Side.TOP);

		// title of chart
		String chartTitle = DesignerLocalizer.instance().getLangString("equipment.times");
		bcLosses.setTitle(DesignerLocalizer.instance().getLangString("losses.title", chartTitle, timeUnit));
		bcLosses.setAnimated(false);

		clearLossData();

		// net times
		netTimeSeries.setName(DesignerLocalizer.instance().getLangString("time.in.cat"));
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
		bcLosses.getStylesheets().addAll(getClass().getResource("/css/dashboard.css").toExternalForm());

		// add listener for mouse click on bar and tooltip
		for (Series<Number, String> series : bcLosses.getData()) {
			for (XYChart.Data<Number, String> item : series.getData()) {
				item.getNode().setOnMouseClicked((MouseEvent event) -> onClickLossCategory(series, item));

				Tooltip tooltip = new Tooltip(String.format("%.2f", item.getXValue()));
				Tooltip.install(item.getNode(), tooltip);
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

		// target OEE
		EquipmentMaterial eqm = equipmentLoss.getEquipmentMaterial();

		if (eqm != null) {
			String targetOee = String.format(Locale.getDefault(), OEE_FORMAT, eqm.getOeeTarget());
			tiOee.setText(DesignerLocalizer.instance().getLangString("target.oee", targetOee));

			Quantity actualSpeed = equipmentLoss.calculateActualSpeed(eqm.getRunRate());

			if (actualSpeed != null) {
				String speed = String.format(Locale.getDefault(), PROD_FORMAT, actualSpeed.getAmount());
				tiProduction.setText(DesignerLocalizer.instance().getLangString("actual.speed", speed,
						actualSpeed.getUOM().getSymbol()));
			} else {
				tiProduction.setText(null);
			}
		}
	}

	private void onSelectFirstLevelPareto() throws Exception {
		Duration availableTime = equipmentLoss.getAvailableTime();
		Number divisor = equipmentLoss.convertSeconds(availableTime.getSeconds(), timeUnit);

		StackPane spLevel1Pareto = new StackPane();

		AnchorPane.setBottomAnchor(spLevel1Pareto, 0.0);
		AnchorPane.setLeftAnchor(spLevel1Pareto, 0.0);
		AnchorPane.setRightAnchor(spLevel1Pareto, 0.0);
		AnchorPane.setTopAnchor(spLevel1Pareto, 0.0);

		apLevel1Pareto.getChildren().clear();
		apLevel1Pareto.getChildren().add(0, spLevel1Pareto);

		List<ParetoItem> paretoItems = equipmentLoss.getLossItems(timeUnit);

		ParetoChartController level1Controller = new ParetoChartController();
		level1Controller.setCategoryClickListener(this);
		level1Controller.createParetoChart(DesignerLocalizer.instance().getLangString("first.level.pareto"),
				spLevel1Pareto, paretoItems, divisor, DesignerLocalizer.instance().getLangString("loss.category"));
	}

	private void onClickLossCategory(Series<Number, String> series, XYChart.Data<Number, String> dataItem) {
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
	protected void setImages() {
		// refresh
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));

		// new availability
		btNewAvailability.setGraphic(ImageManager.instance().getImageView(Images.ADD));

		// new production
		btNewProduction.setGraphic(ImageManager.instance().getImageView(Images.NEW));

		// new setup
		btNewSetup.setGraphic(ImageManager.instance().getImageView(Images.IMPORT));

		// update event
		btUpdateEvent.setGraphic(ImageManager.instance().getImageView(Images.UPDATE));

		// delete event
		btDeleteEvent.setGraphic(ImageManager.instance().getImageView(Images.DELETE));

		// OEE trend
		btOeeTrend.setGraphic(ImageManager.instance().getImageView(Images.CHARTXY));
	}

	@FXML
	public void initialize() {
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

		// set default period
		tfRefreshPeriod.setText(String.valueOf(REFRESH_SEC));

		buildDashboardTiles();

		initializeEventTable();

		initializeDateRange();

		initializeRefreshTimer();

		// select the time loss chart
		tpParetoCharts.getSelectionModel().select(tbTimeLosses);
	}

	private void initializeDateRange() {
		final String hhSS = "00:00";

		LocalDate today = LocalDate.now();
		LocalDate tomorrow = today.plusDays(1);

		dpStartDate.setValue(today);
		tfStartTime.setText(hhSS);

		dpEndDate.setValue(tomorrow);
		tfEndTime.setText(hhSS);
	}

	private void initializeRefreshTimer() {
		// create timer and task
		refreshTimer = new Timer();
		refreshTask = new RefreshTask();
	}

	private void startRefreshTimer() {
		if (refreshTimer == null) {
			initializeRefreshTimer();
		}

		int refreshSec = Integer.parseInt(tfRefreshPeriod.getText());
		refreshTimer.schedule(refreshTask, 1000l, refreshSec * 1000l);
	}

	private void stopRefreshTimer() {
		refreshTimer.cancel();
		refreshTimer = null;
	}

	@FXML
	private void onToggleRefresh() {
		if (cbAutoRefresh.isSelected()) {
			startRefreshTimer();
		} else {
			stopRefreshTimer();
		}
	}

	private void initializeEventTable() {
		tvResolvedEvents.setItems(resolvedEvents);

		// loss category
		tcAvailability.setCellFactory(column -> new TableCell<OeeEvent, Reason>() {
			@Override
			protected void updateItem(Reason reason, boolean empty) {
				super.updateItem(reason, empty);

				if (reason != null && reason.getLossCategory() != null) {
					Color color = Color.web(reason.getLossCategory().getColor());

					// remove 0x
					setStyle("-fx-background-color: #" + color.toString().substring(2));
				}
			}
		});

		tcAvailability.setCellValueFactory(cellDataFeatures -> {
			// set the reason
			OeeEvent event = cellDataFeatures.getValue();
			Reason reason = null;

			if (event.isAvailability()) {
				// switch to current reason
				reason = event.getReason();
				lastAvailability = event;
			} else {
				// use last reason
				if (lastAvailability != null) {
					reason = lastAvailability.getReason();
				}
			}
			return new SimpleObjectProperty<Reason>(reason);

		});

		// start time
		tcStartTime.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(DomainUtils.offsetDateTimeToString(
				cellDataFeatures.getValue().getStartTime(), DomainUtils.OFFSET_DATE_TIME_PATTERN)));

		// end time
		tcEndTime.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(DomainUtils.offsetDateTimeToString(
				cellDataFeatures.getValue().getEndTime(), DomainUtils.OFFSET_DATE_TIME_PATTERN)));

		// duration
		tcDuration.setCellValueFactory(cellDataFeatures -> {
			OeeEvent event = cellDataFeatures.getValue();
			SimpleStringProperty property = null;

			Duration duration = event.getDuration();
			if (duration != null) {
				property = new SimpleStringProperty(DomainUtils.formatDuration(duration));
			}

			return property;
		});

		// shift
		tcShift.setCellValueFactory(cellDataFeatures -> {
			OeeEvent event = cellDataFeatures.getValue();
			SimpleStringProperty property = null;

			if (event.getShift() != null) {
				property = new SimpleStringProperty(event.getShift().getName());
			}
			return property;
		});

		// team
		tcTeam.setCellValueFactory(cellDataFeatures -> {
			OeeEvent event = cellDataFeatures.getValue();
			SimpleStringProperty property = null;

			if (event.getTeam() != null) {
				property = new SimpleStringProperty(event.getTeam().getName());
			}
			return property;
		});

		// reason
		tcReason.setCellValueFactory(cellDataFeatures -> {
			OeeEvent event = cellDataFeatures.getValue();
			SimpleStringProperty property = null;

			if (event.getReason() != null) {
				property = new SimpleStringProperty(event.getReason().getDisplayString());
			}
			return property;
		});

		// loss category
		tcLossCategory.setCellValueFactory(cellDataFeatures -> {
			OeeEvent event = cellDataFeatures.getValue();
			SimpleObjectProperty<Text> lossProperty = null;
			Reason reason = event.getReason();

			if (reason != null) {
				Color color = Color.web(reason.getLossCategory().getColor());
				Text text = new Text(reason.getLossCategory().toString());
				text.setFill(color);
				lossProperty = new SimpleObjectProperty<>(text);
			}
			return lossProperty;
		});

		// lost production time
		tcLostTime.setCellValueFactory(cellDataFeatures -> {
			OeeEvent event = cellDataFeatures.getValue();
			return new SimpleStringProperty(DomainUtils.formatDuration(event.getLostTime()));
		});

		// production type
		tcProdType.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			OeeEvent event = cellDataFeatures.getValue();

			if (event.getEventType() != null) {
				property = new SimpleStringProperty(event.getEventType().toString());
			}
			return property;
		});

		// production amount
		tcProdAmount.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			OeeEvent event = cellDataFeatures.getValue();

			if (event.isProduction()) {
				double amount = event.getAmount();
				property = new SimpleStringProperty(AppUtils.formatDouble(amount));
			}
			return property;
		});

		// production UOM
		tcProdUnit.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			OeeEvent event = cellDataFeatures.getValue();

			if (event.isProduction()) {
				UnitOfMeasure uom = event.getUOM();
				if (uom != null) {
					property = new SimpleStringProperty(uom.getSymbol());
				}
			}
			return property;
		});

		// material
		tcMaterial.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			OeeEvent event = cellDataFeatures.getValue();
			Material material = event.getMaterial();

			if (material != null) {
				property = new SimpleStringProperty(material.getName());
			}
			return property;
		});

		// job
		tcJob.setCellValueFactory(cellDataFeatures -> {
			OeeEvent event = cellDataFeatures.getValue();
			String job = event.getJob();

			return new SimpleStringProperty(job);
		});

		// source
		tcSourceId.setCellValueFactory(cellDataFeatures -> {
			OeeEvent event = cellDataFeatures.getValue();
			String sourceId = event.getSourceId();
			return new SimpleStringProperty(sourceId);
		});

		// data collector
		tcCollector.setCellValueFactory(cellDataFeatures -> {
			OeeEvent event = cellDataFeatures.getValue();
			String collector = event.getCollector();
			return new SimpleStringProperty(collector);
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
		bciOee = new BarChartItem(DesignerLocalizer.instance().getLangString("oee"), 0, Tile.BLUE);
		bciOee.setFormatString(OEE_FORMAT);

		bciPerformance = new BarChartItem(DesignerLocalizer.instance().getLangString("performance"), 0, Tile.GREEN);
		bciPerformance.setFormatString(OEE_FORMAT);

		bciAvailability = new BarChartItem(DesignerLocalizer.instance().getLangString("availability"), 0, Tile.RED);
		bciAvailability.setFormatString(OEE_FORMAT);

		bciQuality = new BarChartItem(DesignerLocalizer.instance().getLangString("quality"), 0, Tile.ORANGE);
		bciQuality.setFormatString(OEE_FORMAT);

		tiOee = TileBuilder.create().skinType(SkinType.BAR_CHART).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title(DesignerLocalizer.instance().getLangString("oee.title"))
				.barChartItems(bciOee, bciAvailability, bciPerformance, bciQuality).decimals(0).sortedData(false)
				.animated(false).build();

		// production tile
		lbiGoodProduction = new LeaderBoardItem(DesignerLocalizer.instance().getLangString("good"), 0);
		lbiRejectProduction = new LeaderBoardItem(DesignerLocalizer.instance().getLangString("reject"), 0);
		lbiStartupProduction = new LeaderBoardItem(DesignerLocalizer.instance().getLangString("startup"), 0);

		String productionText = DesignerLocalizer.instance().getLangString("quantity.change");

		tiProduction = TileBuilder.create().skinType(SkinType.LEADER_BOARD).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title(DesignerLocalizer.instance().getLangString("current.production")).text(productionText)
				.leaderBoardItems(lbiGoodProduction, lbiRejectProduction, lbiStartupProduction).sortedData(false)
				.animated(false).build();

		// availability tile
		tiAvailability = TileBuilder.create().skinType(SkinType.TEXT).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title(DesignerLocalizer.instance().getLangString("availability")).textVisible(true)
				.descriptionAlignment(Pos.CENTER).build();
		tiAvailability.setDescriptionTextSize(TextSize.BIGGER);

		// material and job
		tiJobMaterial = TileBuilder.create().skinType(SkinType.TEXT).prefSize(TILE_WIDTH, TILE_HEIGHT)
				.title(DesignerLocalizer.instance().getLangString("material.title")).textVisible(true)
				.descriptionAlignment(Pos.CENTER).build();
		tiJobMaterial.setDescriptionTextSize(TextSize.NORMAL);
		tiJobMaterial.setDescriptionColor(Color.WHITE);

		// time losses
		tiLoss = TileBuilder.create().skinType(SkinType.DONUT_CHART).prefSize(TILE_WIDE, TILE_HEIGHT)
				.title(DesignerLocalizer.instance().getLangString("loss.time")).textVisible(true).animated(true)
				.sortedData(false).build();

		FlowGridPane pane = new FlowGridPane(5, 1, tiOee, tiProduction, tiAvailability, tiJobMaterial, tiLoss);
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
		// check if this message is for the selected equipment
		if (equipmentLoss != null && !equipmentLoss.getEquipment().getName().equals(message.getEquipmentName())) {
			return;
		}

		// update the UI
		OeeEventType resolverType = message.getResolverType();

		switch (resolverType) {
		case AVAILABILITY: {
			// availability reason
			tiAvailability.setText(message.getReasonName() + " (" + message.getReasonDescription() + ")");

			// loss category
			TimeLoss loss = message.getLoss();
			if (loss == null) {
				loss = TimeLoss.NO_LOSS;
			}
			tiAvailability.setDescription(loss.toString());
			tiAvailability.setTextColor(Color.web(loss.getColor()));
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
			UnitOfMeasure uom = MeasurementSystem.instance().getUomBySymbol(message.getUomSymbol());
			Quantity delta = new Quantity(message.getAmount(), uom);
			Quantity good = equipmentLoss.incrementGoodQuantity(delta);
			lbiGoodProduction.setValue(good.getAmount(), false);
			break;
		}

		case PROD_REJECT: {
			// reject and rework
			UnitOfMeasure uom = MeasurementSystem.instance().getUomBySymbol(message.getUomSymbol());
			Quantity delta = new Quantity(message.getAmount(), uom);
			Quantity reject = equipmentLoss.incrementRejectQuantity(delta);
			lbiRejectProduction.setValue(reject.getAmount(), true);
			break;
		}

		case PROD_STARTUP: {
			// startup and yield
			UnitOfMeasure uom = MeasurementSystem.instance().getUomBySymbol(message.getUomSymbol());
			Quantity delta = new Quantity(message.getAmount(), uom);
			Quantity startup = equipmentLoss.incrementStartupQuantity(delta);
			lbiStartupProduction.setValue(startup.getAmount(), true);
			break;
		}

		default:
			break;
		}

		// display the database record
		Long key = message.getOeeEventKey();

		if (key != null) {
			OeeEvent event = PersistenceService.instance().fetchEventByKey(key);
			resolvedEvents.add(event);
			tvResolvedEvents.refresh();
		}
	}

	@FXML
	private void clearMaterials() {
		// material filtering
		cbMaterials.getItems().clear();
		cbMaterials.getItems().add(DesignerLocalizer.instance().getLangString("all.materials"));
	}

	private OffsetDateTime getStartTime() throws Exception {
		LocalDate startDate = dpStartDate.getValue();

		if (startDate == null) {
			startDate = LocalDate.now();
		}

		Duration startSeconds = null;
		if (tfStartTime.getText() != null && tfStartTime.getText().trim().length() > 0) {
			startSeconds = AppUtils.durationFromString(tfStartTime.getText().trim());
		} else {
			startSeconds = Duration.ZERO;
		}
		LocalTime startTime = LocalTime.ofSecondOfDay(startSeconds.getSeconds());
		LocalDateTime ldtStart = LocalDateTime.of(startDate, startTime);
		return DomainUtils.fromLocalDateTime(ldtStart);
	}

	private OffsetDateTime getEndTime() throws Exception {
		LocalDate endDate = dpEndDate.getValue();

		if (endDate == null) {
			endDate = LocalDate.now().plusDays(1);
		}

		Duration endSeconds = null;
		if (tfEndTime.getText() != null && tfEndTime.getText().trim().length() > 0) {
			endSeconds = AppUtils.durationFromString(tfEndTime.getText().trim());
		} else {
			endSeconds = Duration.ZERO;
		}
		LocalTime endTime = LocalTime.ofSecondOfDay(endSeconds.getSeconds());
		LocalDateTime ldtEnd = LocalDateTime.of(endDate, endTime);

		return DomainUtils.fromLocalDateTime(ldtEnd);
	}

	@FXML
	public void onRefresh() {
		try {
			// clear previous calculation
			if (equipmentLoss == null) {
				return;
			}
			equipmentLoss.reset();

			tiJobMaterial.setText(null);
			tiJobMaterial.setDescription("");

			tiAvailability.setText(null);
			tiAvailability.setDescription("");

			tiProduction.setText(null);

			lbiGoodProduction.setFormatString(PROD_FORMAT + " ");
			lbiGoodProduction.setValue(0.0d, false);
			lbiRejectProduction.setFormatString(PROD_FORMAT + " ");
			lbiRejectProduction.setValue(0.0d, false);
			lbiStartupProduction.setFormatString(PROD_FORMAT + " ");
			lbiStartupProduction.setValue(0.0d, false);

			// start date and time
			OffsetDateTime odtStart = getStartTime();

			// end date and time
			OffsetDateTime odtEnd = getEndTime();

			if (odtEnd.isBefore(odtStart)) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("start.before.end", odtStart, odtEnd));
			}

			// equipment
			Equipment equipment = equipmentLoss.getEquipment();

			if (equipment == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.equipment"));
			}

			String selectedMaterialId = cbMaterials.getSelectionModel().getSelectedItem();
			String materialId = null;

			if (selectedMaterialId != null
					&& !selectedMaterialId.equals(DesignerLocalizer.instance().getLangString("all.materials"))) {
				materialId = selectedMaterialId;
			}

			// generate all of the loss data for this equipment
			EquipmentLossManager.buildLoss(equipmentLoss, materialId, odtStart, odtEnd);

			// find the setups
			OeeEvent lastSetup = null;
			for (OeeEvent event : equipmentLoss.getEventRecords()) {

				if (event.getEventType().equals(OeeEventType.MATL_CHANGE)) {
					lastSetup = event;
					cbMaterials.getItems().add(event.getMaterial().getName());
				}
			}
			Collections.sort(cbMaterials.getItems());

			if (lastSetup != null && lastSetup.getMaterial() != null) {
				tiJobMaterial.setDescription(lastSetup.getMaterial().getDisplayString());
				tiJobMaterial.setText(lastSetup.getJob());
			}

			// show the production
			String symbol = null;
			double amount;

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

			// show last availability and setup records (job changes not shown)
			List<OeeEvent> historyRecords = equipmentLoss.getEventRecords();

			// sort by start time
			Collections.sort(historyRecords, new Comparator<OeeEvent>() {
				public int compare(OeeEvent record1, OeeEvent record2) {
					return record1.getStartTime().compareTo(record2.getStartTime());
				}
			});

			OeeEvent lastAvailabilityEvent = null;

			int i = historyRecords.size() - 1;

			while (i > -1) {
				if (historyRecords.get(i).isAvailability()) {
					lastAvailabilityEvent = historyRecords.get(i);
					break;
				}
				i--;
			}

			// last availability
			if (lastAvailabilityEvent != null) {
				// availability reason
				Reason reason = lastAvailabilityEvent.getReason();

				if (reason != null) {
					tiAvailability.setText(reason.getName() + " (" + reason.getDescription() + ")");

					// loss category
					TimeLoss loss = reason.getLossCategory();
					if (loss != null) {
						tiAvailability.setDescription(loss.toString());
						tiAvailability.setDescriptionColor(Color.web(loss.getColor()));
					}
				}
			}

			// time losses
			ChartData[] slices = new ChartData[9];

			slices[0] = new ChartData(DesignerLocalizer.instance().getLangString("loss.not.scheduled"),
					toDouble(equipmentLoss.getLoss(TimeLoss.NOT_SCHEDULED)), Color.VIOLET);
			slices[1] = new ChartData(DesignerLocalizer.instance().getLangString("loss.unscheduled"),
					toDouble(equipmentLoss.getLoss(TimeLoss.UNSCHEDULED)), Color.DARKBLUE);
			slices[2] = new ChartData(DesignerLocalizer.instance().getLangString("loss.planned.downtime"),
					toDouble(equipmentLoss.getLoss(TimeLoss.PLANNED_DOWNTIME)), Color.DEEPSKYBLUE);
			slices[3] = new ChartData(DesignerLocalizer.instance().getLangString("loss.setup"),
					toDouble(equipmentLoss.getLoss(TimeLoss.SETUP)), Color.AQUAMARINE);
			slices[4] = new ChartData(DesignerLocalizer.instance().getLangString("loss.unplanned.downtime"),
					toDouble(equipmentLoss.getLoss(TimeLoss.UNPLANNED_DOWNTIME)), Color.GREENYELLOW);
			slices[5] = new ChartData(DesignerLocalizer.instance().getLangString("loss.minor"),
					toDouble(equipmentLoss.getLoss(TimeLoss.MINOR_STOPPAGES)), Color.YELLOW);
			slices[6] = new ChartData(DesignerLocalizer.instance().getLangString("loss.speed"),
					toDouble(equipmentLoss.getLoss(TimeLoss.REDUCED_SPEED)), Color.GOLD);
			slices[7] = new ChartData(DesignerLocalizer.instance().getLangString("loss.reject"),
					toDouble(equipmentLoss.getLoss(TimeLoss.REJECT_REWORK)), Color.DARKORANGE);
			slices[8] = new ChartData(DesignerLocalizer.instance().getLangString("loss.yield"),
					toDouble(equipmentLoss.getLoss(TimeLoss.STARTUP_YIELD)), Color.RED);

			tiLoss.setChartData(slices);

			tiLoss.setTitle(DesignerLocalizer.instance().getLangString("loss.time") + " (" + timeUnit.toString() + ")");

			// overall duration
			Duration duration = this.equipmentLoss.getDuration();
			long timeDuration = 0l;

			if (timeUnit.equals(Unit.SECOND)) {
				timeDuration = duration.getSeconds();
			} else if (timeUnit.equals(Unit.MINUTE)) {
				timeDuration = duration.toMinutes();
			} else if (timeUnit.equals(Unit.HOUR)) {
				timeDuration = duration.toHours();
			} else if (timeUnit.equals(Unit.DAY)) {
				timeDuration = duration.toDays();
			}
			tiLoss.setText(DesignerLocalizer.instance().getLangString("loss.duration") + ": " + timeDuration);

			// show stats in tiles
			showStatistics();

			// display the selected tab
			clearLossData();
			refreshCharts(tpParetoCharts.getSelectionModel().getSelectedItem());

			String timestamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(), "yyyy-MM-dd HH:mm:ss ZZZZZ");
			postNotification(DesignerLocalizer.instance().getLangString("data.refreshed", timestamp));

		} catch (Exception e) {
			String timestamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(), "yyyy-MM-dd HH:mm:ss ZZZZZ");
			postNotification(timestamp + ": " + e.getMessage());
		}
	}

	private double toDouble(Duration duration) {
		long value = 0;
		if (timeUnit.equals(Unit.SECOND)) {
			value = duration.getSeconds();
		} else if (timeUnit.equals(Unit.MINUTE)) {
			value = duration.toMinutes();
		} else if (timeUnit.equals(Unit.HOUR)) {
			value = duration.toHours();
		} else if (timeUnit.equals(Unit.DAY)) {
			value = duration.toDays();
		}
		return value;
	}

	private AvailabilityEditorController getAvailabilityController() throws Exception {
		if (availabilityEditorController == null) {
			FXMLLoader loader = FXMLLoaderFactory.availabilityEditorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("availability.editor"));
			dialogStage.initModality(Modality.APPLICATION_MODAL);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			availabilityEditorController = loader.getController();
			availabilityEditorController.setDialogStage(dialogStage);
		}
		return availabilityEditorController;
	}

	private ProductionEditorController getProductionController() throws Exception {
		if (productionEditorController == null) {
			FXMLLoader loader = FXMLLoaderFactory.productionEditorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("production.editor"));
			dialogStage.initModality(Modality.APPLICATION_MODAL);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			productionEditorController = loader.getController();
			productionEditorController.setDialogStage(dialogStage);
		}
		return productionEditorController;
	}

	private SetupEditorController getSetupController() throws Exception {
		if (setupEditorController == null) {
			FXMLLoader loader = FXMLLoaderFactory.setupEditorLoader();
			AnchorPane page = (AnchorPane) loader.getRoot();

			Stage dialogStage = new Stage(StageStyle.DECORATED);
			dialogStage.setTitle(DesignerLocalizer.instance().getLangString("setup.editor"));
			dialogStage.initModality(Modality.APPLICATION_MODAL);
			Scene scene = new Scene(page);
			dialogStage.setScene(scene);

			// get the controller
			setupEditorController = loader.getController();
			setupEditorController.setDialogStage(dialogStage);
		}
		return setupEditorController;
	}

	private OeeEventTrendController getOeeEventTrendController() throws Exception {
		FXMLLoader loader = FXMLLoaderFactory.oeeEventTrendLoader();
		AnchorPane page = (AnchorPane) loader.getRoot();

		Stage dialogStage = new Stage(StageStyle.DECORATED);
		dialogStage.setTitle(DesignerLocalizer.instance().getLangString("oee.event.trend"));
		dialogStage.initModality(Modality.NONE);
		Scene scene = new Scene(page);
		dialogStage.setScene(scene);

		// get the controller
		OeeEventTrendController oeeEventTrendController = loader.getController();
		oeeEventTrendController.setDialogStage(dialogStage);

		return oeeEventTrendController;
	}

	@FXML
	private void onNewAvailability() {
		try {
			OeeEvent event = new OeeEvent(equipmentLoss.getEquipment());
			event.setSourceId(EDITOR_SOURCE_ID);
			event.setEventType(OeeEventType.AVAILABILITY);
			getAvailabilityController().initializeEditor(event);
			getAvailabilityController().getDialogStage().showAndWait();

			onRefresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewProduction() {
		try {
			OeeEvent event = new OeeEvent(equipmentLoss.getEquipment());
			event.setSourceId(EDITOR_SOURCE_ID);
			getProductionController().initializeEditor(event);
			getProductionController().getDialogStage().showAndWait();

			onRefresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewSetup() {
		try {
			OeeEvent event = new OeeEvent(equipmentLoss.getEquipment());
			event.setSourceId(EDITOR_SOURCE_ID);
			getSetupController().initializeEditor(event);
			getSetupController().getDialogStage().showAndWait();

			onRefresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onUpdateEvent() {
		try {
			OeeEvent event = tvResolvedEvents.getSelectionModel().getSelectedItem();

			if (event == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.event"));
			}

			if (event.isAvailability()) {
				getAvailabilityController().initializeEditor(event);
				getAvailabilityController().getDialogStage().showAndWait();
			} else if (event.isProduction()) {
				getProductionController().initializeEditor(event);
				getProductionController().getDialogStage().showAndWait();
			} else if (event.isSetup()) {
				getSetupController().initializeEditor(event);
				getSetupController().getDialogStage().showAndWait();
			}

			onRefresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onDeleteEvent() {
		try {
			OeeEvent event = tvResolvedEvents.getSelectionModel().getSelectedItem();

			if (event == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.event"));
			}

			// confirm
			String msg = DesignerLocalizer.instance().getLangString("confirm.deletion", event.getEquipment().getName());
			ButtonType type = AppUtils.showConfirmationDialog(msg);

			if (type.equals(ButtonType.CANCEL)) {
				return;
			}

			PersistenceService.instance().delete(event);

			onRefresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onOeeEventTrend() {
		try {
			OeeEventTrendController controller = getOeeEventTrendController();
			controller.buildTrend(resolvedEvents, getStartTime(), getEndTime());
			controller.getDialogStage().showAndWait();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	public void enableRefresh(boolean value) {
		this.btRefresh.setDisable(!value);
		this.cbAutoRefresh.setDisable(!value);
		this.tpParetoCharts.setDisable(!value);
	}

	public void setupEquipmentLoss(Equipment equipment) {
		if (equipment == null) {
			equipmentLoss = null;
		} else {
			equipmentLoss = lossMap.get(equipment.getName());

			if (equipmentLoss == null) {
				equipmentLoss = new EquipmentLoss(equipment);
				lossMap.put(equipment.getName(), equipmentLoss);
			}
		}
	}

	private class RefreshTask extends TimerTask {
		@Override
		public void run() {
			Platform.runLater(() -> onRefresh());
		}
	}
}
