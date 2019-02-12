package org.point85.app.charts;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerController;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.EquipmentEventResolver;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.OeeEventType;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class TrendChartController extends DesignerController {
	// chart views
	private static final int INPUT_VALUE_VIEW = 0;
	private static final int OUTPUT_VALUE_VIEW = 1;

	// trend button text
	private static final String START = "Start";
	private static final String STOP = "Stop";

	private static final int DEFAULT_UPDATE_SEC = 5;

	// the data provider
	private DataSubscriber subscriber;

	private final EquipmentEventResolver equipmentResolver = new EquipmentEventResolver();

	// script resolver for input value
	private EventResolver eventResolver;

	// data for table view
	private final ObservableList<OeeEvent> resolvedItems = FXCollections.observableArrayList(new ArrayList<>());

	// how to interpolate the data points
	private final ObservableList<InterpolationType> interpolationTypes = FXCollections
			.observableArrayList(new ArrayList<>());

	// controller for input values
	private SampleChartController inputValueController;

	// controller for output values
	private SampleChartController outputValueController;

	// number of data points to display
	int dataCount = 100;

	// resolution counter
	int resolutionCounter = 0;

	// resolver controls
	@FXML
	private Button btToggleTrend;

	@FXML
	private Button btResetTrend;

	@FXML
	private TableView<OeeEvent> tvResolvedItems;

	@FXML
	private TableColumn<OeeEvent, String> tcItem;

	@FXML
	private TableColumn<OeeEvent, String> tcInputValue;

	@FXML
	private TableColumn<OeeEvent, String> tcTimestamp;

	@FXML
	private TableColumn<OeeEvent, String> tcOutputValue;

	@FXML
	private TableColumn<OeeEvent, Text> tcLossCategory;

	@FXML
	private Spinner<Integer> spUpdatePeriod;

	// chart controls
	@FXML
	private ComboBox<InterpolationType> cbInterpolationTypes;

	@FXML
	private StackPane spCharts;

	@FXML
	private RadioButton rbInputValue;

	@FXML
	private RadioButton rbOutputValue;

	@FXML
	private TextField tfPointCount;

	@FXML
	private ToggleGroup tgWhichChart;

	// date range selection criteria
	@FXML
	private DatePicker dpStartDate;

	@FXML
	private TextField tfStartTime;

	@FXML
	private DatePicker dpEndDate;

	@FXML
	private TextField tfEndTime;

	@FXML
	private Button btRefresh;

	public void initialize(DesignerApplication app) throws Exception {
		setApp(app);

		// button images
		setButtonImages();

		// set up resolver item display
		intializeItemTable();

		// value and state charts
		initializeCharts();

		// spinner value factory for update period
		SpinnerValueFactory<Integer> periodValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1,
				Integer.MAX_VALUE, DEFAULT_UPDATE_SEC);

		spUpdatePeriod.setValueFactory(periodValueFactory);
	}

	private void initializeCharts() throws Exception {
		// load first value chart controller
		FXMLLoader loader = FXMLLoaderFactory.sampleChartLoader();
		AnchorPane pane1 = (AnchorPane) loader.getRoot();
		spCharts.getChildren().add(INPUT_VALUE_VIEW, pane1);
		inputValueController = loader.getController();
		inputValueController.getChart().setTitle("Input Value");
		inputValueController.getChart().getYAxis().setLabel("Value");

		// load second state chart controller
		loader = FXMLLoaderFactory.sampleChartLoader();
		AnchorPane pane2 = (AnchorPane) loader.getRoot();
		spCharts.getChildren().add(OUTPUT_VALUE_VIEW, pane2);
		outputValueController = loader.getController();
		outputValueController.getChart().setTitle("Output Value");
		outputValueController.getChart().getYAxis().setLabel("Value");

		cbInterpolationTypes.setItems(interpolationTypes);
		interpolationTypes.addAll(InterpolationType.values());
		cbInterpolationTypes.getSelectionModel().select(InterpolationType.LINEAR);

		tfPointCount.setText(String.valueOf(dataCount));

		onResetTrending();

		// set point table
		Map<String, Integer> chartStrings = new HashMap<>();

		for (TimeLoss loss : TimeLoss.values()) {
			Integer ordinal = loss.ordinal() + 1;
			chartStrings.put(loss.toString(), ordinal);
		}
		outputValueController.setChartStrings(chartStrings);

		// show value chart
		onSelectInputValueChart();
	}

	private void intializeItemTable() {
		// table view row selection listener
		tvResolvedItems.getSelectionModel().selectedItemProperty()
				.addListener((observableValue, oldValue, newValue) -> {
					try {
					} catch (Exception e) {
						AppUtils.showErrorDialog(e);
					}
				});

		// item id
		tcItem.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getSourceId());
		});

		// input value
		tcInputValue.setCellValueFactory(cellDataFeatures -> {
			Object inputValue = cellDataFeatures.getValue().getInputValue();

			if (inputValue == null) {
				inputValue = "";
			}
			return new SimpleStringProperty(inputValue.toString());
		});

		// timestamp
		tcTimestamp.setCellValueFactory(cellDataFeatures -> {
			OffsetDateTime odt = cellDataFeatures.getValue().getStartTime();
			return new SimpleStringProperty(DomainUtils.offsetDateTimeToString(odt, DomainUtils.OFFSET_DATE_TIME_PATTERN));
		});

		// output value
		tcOutputValue.setCellValueFactory(cellDataFeatures -> {
			Object outputValue = cellDataFeatures.getValue().getOutputValue();

			if (outputValue == null) {
				outputValue = "";
			}
			return new SimpleStringProperty(outputValue.toString());
		});

		// loss category
		tcLossCategory.setCellValueFactory(cellDataFeatures -> {
			SimpleObjectProperty<Text> lossProperty = null;

			Reason reason = cellDataFeatures.getValue().getReason();
			if (reason != null && reason.getLossCategory() != null) {
				Color color = reason.getLossCategory().getColor();
				Text text = new Text(reason.getLossCategory().toString());
				text.setFill(color);
				lossProperty = new SimpleObjectProperty<Text>(text);
			}

			return lossProperty;
		});
	}

	private void toggleTrendButton() throws Exception {
		if (subscriber.isSubscribed()) {
			btToggleTrend.setGraphic(ImageManager.instance().getImageView(Images.STOP));
			btToggleTrend.setText(STOP);
		} else {
			btToggleTrend.setGraphic(ImageManager.instance().getImageView(Images.START));
			btToggleTrend.setText(START);
		}
	}

	// images for buttons
	protected void setButtonImages() throws Exception {
		// trend auto starts
		btToggleTrend.setGraphic(ImageManager.instance().getImageView(Images.STOP));
		btToggleTrend.setContentDisplay(ContentDisplay.LEFT);
		btToggleTrend.setText(STOP);

		// clear trend
		btResetTrend.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));
		btResetTrend.setContentDisplay(ContentDisplay.LEFT);

		// refresh
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
		btRefresh.setContentDisplay(ContentDisplay.LEFT);
	}

	public void setProvider(DataSubscriber provider) {
		this.subscriber = provider;
	}

	public EventResolver getEventResolver() {
		return this.eventResolver;
	}

	public void setEventResolver(EventResolver eventResolver) throws Exception {
		this.eventResolver = eventResolver;
		this.setUpdatePeriodMsec(eventResolver.getUpdatePeriod());
	}

	public OeeEvent invokeResolver(OeeContext context, Object sourceValue, OffsetDateTime dateTime) throws Exception {
		OeeEvent resolvedItem = equipmentResolver.invokeResolver(eventResolver, context, sourceValue, dateTime);

		OeeEventType type = eventResolver.getType();

		switch (type) {
		case AVAILABILITY: {
			// plot loss category
			String lossCategory = null;
			if (resolvedItem.getReason() != null) {
				TimeLoss loss = resolvedItem.getReason().getLossCategory();
				lossCategory = loss.toString();
			}

			plotData(resolvedItem.getInputValue(), lossCategory);
			break;
		}

		case CUSTOM:
		case JOB_CHANGE:
		case MATL_CHANGE: {
			plotData(resolvedItem.getInputValue(), resolvedItem.getOutputValue());
			break;
		}

		case PROD_GOOD:
		case PROD_REJECT:
		case PROD_STARTUP: {
			Object plottedValue = resolvedItem.getInputValue();

			// convert from String
			if (plottedValue instanceof String) {
				plottedValue = Double.valueOf((String) plottedValue);
			}

			plotData(plottedValue, resolvedItem.getAmount());
			break;
		}

		default:
			break;
		}

		// add to table with limit
		addEvent(resolvedItem);

		return resolvedItem;
	}

	private synchronized void addEvent(OeeEvent event) {
		if (resolvedItems.size() > dataCount) {
			resolvedItems.remove(0);
		}
		resolvedItems.add(event);

		tvResolvedItems.setItems(resolvedItems);
		tvResolvedItems.refresh();
	}

	private void plotData(final Object inputValue, final Object plottedValue) throws Exception {
		if (inputValue == null) {
			return;
		}

		Platform.runLater(() -> {
			// plot output value
			if (plottedValue != null) {
				if (plottedValue instanceof String) {
					outputValueController.plotValue((String) plottedValue);
				} else if (plottedValue instanceof Number) {
					outputValueController.plotValue((Number) plottedValue);
				}
			}

			// plot input value
			if (inputValue instanceof Number) {
				inputValueController.plotValue((Number) inputValue);
			} else if (inputValue instanceof Boolean) {
				int intValue = (Boolean) inputValue ? 1 : 0;
				inputValueController.plotValue(intValue);
			} else if (inputValue instanceof String) {
				inputValueController.plotValue((String) inputValue);
			}
		});
	}

	@FXML
	private void onToggleTrending() {
		if (subscriber.isSubscribed()) {
			this.onStopTrending();
		} else {
			this.onStartTrending();
		}
	}

	public void setUpdatePeriodMsec(Integer millis) {
		if (millis != null) {
			spUpdatePeriod.getValueFactory().setValue(millis / 1000);
		}
	}

	public void enableTrending(boolean enable) {
		btToggleTrend.setDisable(!enable);
	}

	public void onStartTrending() {
		try {
			// number of points to display
			dataCount = Integer.valueOf(tfPointCount.getText());

			// update period in msec
			Integer period = spUpdatePeriod.getValue() * 1000;
			eventResolver.setUpdatePeriod(period);

			// start plotting
			onPlot();

			// subscribe for data
			subscriber.subscribeToDataSource();

			// toggle button
			toggleTrendButton();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void onPlot() {
		onSelectInterpolationType();
	}

	public void onStopTrending() {
		try {
			// unsubscribe from data change events
			subscriber.unsubscribeFromDataSource();

			// toggle button
			toggleTrendButton();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onResetTrending() {
		try {
			// number of points to display
			dataCount = Integer.valueOf(tfPointCount.getText());

			resolvedItems.clear();
			tvResolvedItems.refresh();

			inputValueController.reset(10, dataCount, 10, dataCount);
			outputValueController.reset(10, dataCount, 1, TimeLoss.values().length + 1);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSelectInterpolationType() {
		InterpolationType type = this.cbInterpolationTypes.getSelectionModel().getSelectedItem();

		if (rbInputValue.isSelected()) {
			inputValueController.setInterpolation(type);
		} else {
			outputValueController.setInterpolation(type);
		}
	}

	@FXML
	private void onSelectInputValueChart() {
		spCharts.getChildren().get(INPUT_VALUE_VIEW).setVisible(true);
		spCharts.getChildren().get(OUTPUT_VALUE_VIEW).setVisible(false);
	}

	@FXML
	private void onSelectOutputValueChart() {
		spCharts.getChildren().get(INPUT_VALUE_VIEW).setVisible(false);
		spCharts.getChildren().get(OUTPUT_VALUE_VIEW).setVisible(true);
	}

	@FXML
	public void onRefresh() {
		try {
			onResetTrending();
			
			OffsetDateTime odtStart = null;
			OffsetDateTime odtEnd = null;

			// start date and time
			LocalDate startDate = dpStartDate.getValue();

			if (startDate != null) {
				Duration startSeconds = null;
				if (tfStartTime.getText() != null && tfStartTime.getText().trim().length() > 0) {
					startSeconds = AppUtils.durationFromString(tfStartTime.getText().trim());
				} else {
					startSeconds = Duration.ZERO;
				}

				LocalTime startTime = LocalTime.ofSecondOfDay(startSeconds.getSeconds());
				LocalDateTime ldtStart = LocalDateTime.of(startDate, startTime);
				odtStart = DomainUtils.fromLocalDateTime(ldtStart);
			}

			// end date and time
			LocalDate endDate = dpEndDate.getValue();

			if (endDate != null) {
				Duration endSeconds = null;
				if (tfEndTime.getText() != null && tfEndTime.getText().trim().length() > 0) {
					endSeconds = AppUtils.durationFromString(tfEndTime.getText().trim());
				} else {
					endSeconds = Duration.ZERO;
				}

				LocalTime endTime = LocalTime.ofSecondOfDay(endSeconds.getSeconds());
				LocalDateTime ldtEnd = LocalDateTime.of(endDate, endTime);
				odtEnd = DomainUtils.fromLocalDateTime(ldtEnd);
			}

			if (odtStart != null && odtEnd != null && odtEnd.isBefore(odtStart)) {
				throw new Exception("The starting time " + odtStart + " must be before the ending time " + odtEnd);
			}

			List<OeeEvent> events = PersistenceService.instance().fetchEvents(eventResolver.getEquipment(),
					eventResolver.getType(), odtStart, odtEnd);

			for (OeeEvent event : events) {
				// plot values
				Object plottedValue = null;

				switch (eventResolver.getType()) {
				case AVAILABILITY:
					plottedValue = event.getReason().getName();
					break;

				case CUSTOM:
					break;

				case JOB_CHANGE:
					plottedValue = event.getJob();
					break;

				case MATL_CHANGE:
					plottedValue = event.getMaterial().getName();
					break;

				case PROD_GOOD:
				case PROD_REJECT:
				case PROD_STARTUP:
					plottedValue = event.getAmount();
					break;

				default:
					break;
				}
				
				// add event to table
				event.setSourceId(eventResolver.getSourceId());
				event.setOutputValue(plottedValue);
				addEvent(event);

				// plot it
				plotData(event.getInputValue(), plottedValue);
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

}
