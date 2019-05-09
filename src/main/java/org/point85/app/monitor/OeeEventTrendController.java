package org.point85.app.monitor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.EquipmentMaterial;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.uom.Quantity;
import org.point85.domain.uom.UnitOfMeasure;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.RadioButton;

public class OeeEventTrendController extends DesignerDialogController {
	// OEE event records
	private List<OeeEvent> oeeRecords;

	// loss category series, X = delta time and Y = loss category
	private XYChart.Series<Number, String> lossSeries = new XYChart.Series<>();

	// chart data series, X = delta time and Y = good production value
	private final XYChart.Series<Number, Number> goodSeries = new XYChart.Series<>();

	// chart data series, X = delta time and Y = reject production value
	private final XYChart.Series<Number, Number> rejectSeries = new XYChart.Series<>();

	// chart data series, X = delta time and Y = setup production value
	private final XYChart.Series<Number, Number> startupSeries = new XYChart.Series<>();

	// first event time
	private OffsetDateTime firstEventTime;

	// last event time
	private OffsetDateTime lastEventTime;

	// previous event
	private OeeEvent previousEvent;

	// line chart of produced material
	@FXML
	private LineChart<Number, Number> chProduction;

	// line chart of availability
	@FXML
	private LineChart<Number, String> chLosses;

	@FXML
	private RadioButton rbProduction;

	@FXML
	private RadioButton rbLosses;

	// initialize the production and availability charts and plot the points
	public void buildTrend(List<OeeEvent> records, OffsetDateTime odtStart, OffsetDateTime odtEnd) throws Exception {
		if (records == null || records.size() == 0) {
			return;
		}

		oeeRecords = records;
		firstEventTime = odtStart;
		lastEventTime = odtEnd;

		initializeProductionChart();
		initializeLossChart();

		plotValues();

		rbProduction.setSelected(true);
	}

	private void initializeProductionChart() {
		// add the production series (name is the legend)
		goodSeries.setName(DesignerLocalizer.instance().getLangString("good.production"));
		rejectSeries.setName(DesignerLocalizer.instance().getLangString("reject.production"));
		startupSeries.setName(DesignerLocalizer.instance().getLangString("startup.production"));

		// add to get the desired line colors
		chProduction.getData().add(rejectSeries);
		chProduction.getData().add(startupSeries);
		chProduction.getData().add(goodSeries);
	}

	private void initializeLossChart() {
		chLosses.setLegendVisible(false);

		// fixed categories
		List<String> categories = new ArrayList<>();

		for (TimeLoss loss : TimeLoss.values()) {
			categories.add(loss.toString());
		}

		CategoryAxis yAxis = (CategoryAxis) chLosses.getYAxis();
		yAxis.setCategories(FXCollections.<String>observableArrayList(categories));
		chLosses.getData().add(lossSeries);
	}

	private Duration computeDeltaTime(OeeEvent event) {
		return Duration.between(firstEventTime, event.getStartTime());
	}

	private void plotValues() throws Exception {
		if (oeeRecords.size() == 0) {
			return;
		}

		// create an availability event for the starting time
		Equipment equipment = oeeRecords.get(0).getEquipment();
		OeeEvent startingAvailability = PersistenceService.instance().fetchLastBoundEvent(equipment,
				OeeEventType.AVAILABILITY, firstEventTime);
		startingAvailability.setStartTime(firstEventTime);

		plotOeeValue(startingAvailability);

		// plot all the data
		for (OeeEvent event : oeeRecords) {
			plotOeeValue(event);
		}

		// create an availability point for the ending time
		OeeEvent endingAvailability = PersistenceService.instance().fetchLastBoundEvent(equipment,
				OeeEventType.AVAILABILITY, lastEventTime);
		endingAvailability.setStartTime(lastEventTime);

		plotOeeValue(endingAvailability);
	}

	private XYChart.Data<Number, String> createAvailabilityPoint(OeeEvent event) {
		Duration delta = computeDeltaTime(event);
		Long deltaMinutes = delta.toMinutes();

		TimeLoss loss = event.getReason().getLossCategory();

		return new XYChart.Data<>(deltaMinutes.intValue(), loss.toString());
	}

	private XYChart.Data<Number, Number> createProductionPoint(OeeEvent event) throws Exception {
		Duration delta = computeDeltaTime(event);
		Long deltaMinutes = delta.toMinutes();

		Quantity produced = event.getQuantity();

		// convert to good UOM if necessary
		Equipment equipment = event.getEquipment();
		Material material = event.getMaterial();

		EquipmentMaterial eqm = equipment.getEquipmentMaterial(material);
		UnitOfMeasure uom = eqm.getRunRateUOM().getDividend();

		if (!produced.getUOM().equals(uom)) {
			produced = produced.convert(uom);
		}

		Double value = new Double(produced.getAmount());
		return new XYChart.Data<>(deltaMinutes.intValue(), value);
	}

	private String getBaseData(OeeEvent event) {
		// time
		String timeText = DesignerLocalizer.instance().getLangString("event.time") + ": "
				+ DomainUtils.offsetDateTimeToString(event.getStartTime(), DomainUtils.OFFSET_DATE_TIME_PATTERN);

		// material
		String materialText = DesignerLocalizer.instance().getLangString("material") + ": "
				+ event.getMaterial().getDisplayString();

		// job
		String jobDisplay = event.getJob() != null ? event.getJob() : "";
		String jobText = DesignerLocalizer.instance().getLangString("job") + ": " + jobDisplay;

		return timeText + "\n" + materialText + "\n" + jobText;
	}

	private String getProductionData(OeeEvent event) {
		// quantity
		Quantity q = event.getQuantity();
		String qtyText = DesignerLocalizer.instance().getLangString("production") + ": " + q.getAmount() + " "
				+ q.getUOM().getSymbol();

		return getBaseData(event) + "\n" + qtyText;
	}

	private String getLossData(OeeEvent event) {
		// reason
		Reason reason = event.getReason();

		String reasonText = DesignerLocalizer.instance().getLangString("reason") + ": " + reason.getName() + " ("
				+ reason.getLossCategory() + ")";

		return getBaseData(event) + "\n" + reasonText;
	}

	private void showAvailabilityInfo(OeeEvent event) {
		String title = DesignerLocalizer.instance().getLangString("app.info");
		String header = DesignerLocalizer.instance().getLangString("availability.info");
		AppUtils.showAlert(AlertType.INFORMATION, title, header, getLossData(event));
	}

	private void showProductionInfo(OeeEvent event) {
		String title = DesignerLocalizer.instance().getLangString("app.info");
		String header = DesignerLocalizer.instance().getLangString("production.info");
		AppUtils.showAlert(AlertType.INFORMATION, title, header, getProductionData(event));
	}

	private void plotOeeValue(OeeEvent event) throws Exception {
		switch (event.getEventType()) {
		case AVAILABILITY: {
			// create a point for the previous availability step
			if (previousEvent != null) {
				OeeEvent createdEvent = new OeeEvent();
				createdEvent.setReason(previousEvent.getReason());
				createdEvent.setStartTime(event.getStartTime());
				createdEvent.setMaterial(event.getMaterial());
				createdEvent.setJob(event.getJob());
				XYChart.Data<Number, String> newPoint = createAvailabilityPoint(createdEvent);

				lossSeries.getData().add(newPoint);

				newPoint.getNode().setOnMouseClicked(e -> showAvailabilityInfo(createdEvent));
			}

			// plot the new loss category
			XYChart.Data<Number, String> nextPoint = createAvailabilityPoint(event);
			lossSeries.getData().add(nextPoint);

			nextPoint.getNode().setOnMouseClicked(e -> showAvailabilityInfo(event));

			previousEvent = event;
			break;
		}

		case CUSTOM:
			break;

		case JOB_CHANGE:
			break;

		case MATL_CHANGE:
			break;

		case PROD_GOOD: {
			// plot good production point
			XYChart.Data<Number, Number> nextPoint = createProductionPoint(event);
			goodSeries.getData().add(nextPoint);
			nextPoint.getNode().setOnMouseClicked(e -> showProductionInfo(event));
			break;
		}
		
		case PROD_REJECT: {
			// plot reject production point
			XYChart.Data<Number, Number> nextPoint = createProductionPoint(event);
			rejectSeries.getData().add(nextPoint);
			nextPoint.getNode().setOnMouseClicked(e -> showProductionInfo(event));
			break;
		}
		
		case PROD_STARTUP: {
			// plot good production point
			XYChart.Data<Number, Number> nextPoint = createProductionPoint(event);
			startupSeries.getData().add(nextPoint);
			nextPoint.getNode().setOnMouseClicked(e -> showProductionInfo(event));
			break;
		}

		default:
			break;
		}
	}

	@FXML
	private void onSelectProductionChart() {
		chProduction.setVisible(true);
		chLosses.setVisible(false);
	}

	@FXML
	private void onSelectLossChart() {
		chProduction.setVisible(false);
		chLosses.setVisible(true);
	}
}
