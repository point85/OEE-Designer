package org.point85.app.charts;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.LoaderFactory;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerController;
import org.point85.domain.DomainUtils;
import org.point85.domain.performance.TimeLoss;
import org.point85.domain.plant.EquipmentEventResolver;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.ResolvedEvent;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.EventResolverType;

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

	// the data provider
	private DataSubscriber subscriber;

	private EquipmentEventResolver equipmentResolver = new EquipmentEventResolver();

	// script resolver for input value
	private EventResolver eventResolver;

	// data for table view
	private ObservableList<ResolvedEvent> resolvedItems = FXCollections.observableArrayList(new ArrayList<>());

	// how to interpolate the data points
	private ObservableList<InterpolationType> interpolationTypes = FXCollections.observableArrayList(new ArrayList<>());

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
	private TableView<ResolvedEvent> tvResolvedItems;

	@FXML
	private TableColumn<ResolvedEvent, String> tcItem;

	@FXML
	private TableColumn<ResolvedEvent, String> tcInputValue;

	@FXML
	private TableColumn<ResolvedEvent, String> tcTimestamp;

	@FXML
	private TableColumn<ResolvedEvent, String> tcOutputValue;

	@FXML
	private TableColumn<ResolvedEvent, Text> tcLossCategory;

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

	public void initialize(DesignerApplication app) throws Exception {
		setApp(app);

		// button images
		setButtonImages();

		// set up resolver item display
		intializeItemTable();

		// value and state charts
		initializeCharts();

		// 5 sec default update period
		// spinner value factory
		SpinnerValueFactory<Integer> periodValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1,
				Integer.MAX_VALUE, 5);

		spUpdatePeriod.setValueFactory(periodValueFactory);
	}

	private void initializeCharts() throws Exception {
		// load first value chart controller
		FXMLLoader loader = LoaderFactory.sampleChartLoader();
		AnchorPane pane1 = (AnchorPane) loader.getRoot();
		spCharts.getChildren().add(INPUT_VALUE_VIEW, pane1);
		inputValueController = loader.getController();
		inputValueController.getChart().setTitle("Input Value");
		inputValueController.getChart().getYAxis().setLabel("Value");

		// load second state chart controller
		loader = LoaderFactory.sampleChartLoader();
		AnchorPane pane2 = (AnchorPane) loader.getRoot();
		spCharts.getChildren().add(OUTPUT_VALUE_VIEW, pane2);
		outputValueController = loader.getController();
		outputValueController.setInterpolation(InterpolationType.STAIR_STEP);
		outputValueController.getChart().setTitle("Output Value");
		outputValueController.getChart().getYAxis().setLabel("Value");

		cbInterpolationTypes.setItems(interpolationTypes);
		interpolationTypes.addAll(InterpolationType.values());

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
						// TBD
						System.out.print("Resolution selected");
					} catch (Exception e) {
						AppUtils.showErrorDialog(e);
					}
				});

		// item id
		tcItem.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getItemId());
		});

		// input value
		tcInputValue.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getInputValue().toString());
		});

		// timestamp
		tcTimestamp.setCellValueFactory(cellDataFeatures -> {
			OffsetDateTime odt = cellDataFeatures.getValue().getTimestamp();
			return new SimpleStringProperty(DomainUtils.offsetDateTimeToString(odt));
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
			Reason reason = cellDataFeatures.getValue().getReason();
			SimpleObjectProperty<Text> lossProperty = null;

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
		btToggleTrend.setContentDisplay(ContentDisplay.RIGHT);
		btToggleTrend.setText(STOP);

		// clear trend
		btResetTrend.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));
		btResetTrend.setContentDisplay(ContentDisplay.RIGHT);
	}

	@FXML
	protected void onOK() {

	}

	public void setProvider(DataSubscriber provider) {
		this.subscriber = provider;
	}

	public EventResolver getEventResolver() {
		return this.eventResolver;
	}

	public void setScriptResolver(EventResolver scriptResolver) throws Exception {
		this.eventResolver = scriptResolver;
	}

	public ResolvedEvent invokeResolver(OeeContext context, Object sourceValue, OffsetDateTime dateTime) throws Exception {
		ResolvedEvent resolvedItem = this.equipmentResolver.invokeResolver(eventResolver, context, sourceValue,
				dateTime);

		EventResolverType type = eventResolver.getType();

		switch (type) {
		case AVAILABILITY:
			// plot loss category
			TimeLoss loss = resolvedItem.getReason().getLossCategory();

			if (loss != null) {
				plotData(resolvedItem.getInputValue(), loss.toString());
			}
			break;
		case JOB:
			plotData(resolvedItem.getInputValue(), resolvedItem.getOutputValue());
			break;
		case MATERIAL:
			plotData(resolvedItem.getInputValue(), resolvedItem.getOutputValue());
			break;
		case OTHER:
			plotData(resolvedItem.getInputValue(), resolvedItem.getOutputValue());
			break;
		case PROD_GOOD:
		case PROD_REJECT:
		case PROD_STARTUP:
			Object plottedValue = resolvedItem.getInputValue();

			// convert from String
			if (plottedValue instanceof String) {
				plottedValue = Double.valueOf((String) plottedValue);
			}

			plotData(plottedValue, resolvedItem.getQuantity().getAmount());
			break;
		default:
			break;
		}

		// add to table with limit
		tvResolvedItems.setItems(null);
		if (resolvedItems.size() > dataCount) {
			resolvedItems.remove(0);
		}
		resolvedItems.add(resolvedItem);
		tvResolvedItems.setItems(resolvedItems);
		tvResolvedItems.refresh();

		return resolvedItem;
	}

	private void plotData(final Object inputValue, final Object plottedValue) throws Exception {
		if (inputValue == null) {
			return;
		}

		Platform.runLater(() -> {
			// plot output value
			if (plottedValue instanceof String) {
				outputValueController.plotValue((String) plottedValue);
			} else if (plottedValue instanceof Number) {
				outputValueController.plotValue((Number) plottedValue);
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
		InterpolationType interpolation = this.cbInterpolationTypes.getSelectionModel().getSelectedItem();

		// state is always stair-step
		if (interpolation != null) {
			inputValueController.setInterpolation(interpolation);
		}
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
		this.resolvedItems.clear();
		this.tvResolvedItems.refresh();

		inputValueController.reset(10, dataCount, 10, dataCount);
		outputValueController.reset(10, dataCount, 1, TimeLoss.values().length + 1);
	}

	@FXML
	private void onSelectInterpolationType() {
		InterpolationType type = this.cbInterpolationTypes.getSelectionModel().getSelectedItem();
		inputValueController.setInterpolation(type);
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

}
