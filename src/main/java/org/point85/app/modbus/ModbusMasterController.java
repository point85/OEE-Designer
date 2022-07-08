package org.point85.app.modbus;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.ConnectionState;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.modbus.ModbusDataType;
import org.point85.domain.modbus.ModbusEndpoint;
import org.point85.domain.modbus.ModbusRegisterType;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.modbus.ModbusTransport;
import org.point85.domain.modbus.ModbusVariant;
import org.point85.domain.persistence.PersistenceService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller for a Modbus master
 *
 */
public class ModbusMasterController extends ModbusController {
	// list of Modbus sources
	private final ObservableList<ModbusSource> dataSources = FXCollections.observableArrayList(new ArrayList<>());

	// list of transports
	private final ObservableList<ModbusTransport> transports = FXCollections.observableArrayList(new ArrayList<>());

	// list of data types
	private final ObservableList<ModbusDataType> dataTypes = FXCollections.observableArrayList(new ArrayList<>());

	// list of register types
	private final ObservableList<ModbusRegisterType> registerTypes = FXCollections
			.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfConnectionName;

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfPort;

	@FXML
	private TextField tfDescription;

	@FXML
	private TextField tfUnitId;

	@FXML
	private TextField tfRegisterAddress;

	@FXML
	private TextField tfValueCount;

	@FXML
	private TextArea taValues;

	@FXML
	private ComboBox<ModbusSource> cbDataSources;

	@FXML
	private ComboBox<ModbusTransport> cbTransports;

	@FXML
	private ComboBox<ModbusRegisterType> cbRegisterTypes;

	@FXML
	private ComboBox<ModbusDataType> cbDataTypes;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btConnect;

	@FXML
	private Button btDisconnect;

	@FXML
	private Button btCancelConnect;

	@FXML
	private Button btRead;

	@FXML
	private Button btWrite;

	@FXML
	private Button btBackup;

	@FXML
	private Label lbState;

	@FXML
	private CheckBox ckReverseEndianess;

	@FXML
	private ProgressIndicator piConnection;

	@FXML
	private Label lbValueCount;

	public void initialize(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

		// button images
		setImages();

		// button states
		btRead.setDisable(true);
		btWrite.setDisable(true);

		btConnect.setDisable(true);
		btDisconnect.setDisable(true);
		btCancelConnect.setDisable(true);

		// retrieve the defined data sources
		populateDataSources();

		// set the transports
		populateTransports();

		// set the data types
		populateDataTypes();

		// set the register types
		populateRegisterTypes();

		setDefaults();

		// indicator for connection progress
		piConnection.setVisible(false);
	}

	private void setDefaults() {
		// default port
		tfPort.setText(String.valueOf(ModbusSource.DEFAULT_PORT));

		// default unit Id
		tfUnitId.setText(String.valueOf(ModbusSource.DEFAULT_UNIT_ID));

		// default data type
		cbDataTypes.getSelectionModel().select(ModbusDataType.INT16);

		// default register type
		cbRegisterTypes.getSelectionModel().select(ModbusRegisterType.HOLDING_REGISTER);

		// starting word
		tfRegisterAddress.setText("0");

		// number of values
		tfValueCount.setText("1");

		// LE by default
		ckReverseEndianess.setSelected(true);
	}

	// images for buttons
	@Override
	protected void setImages() {
		super.setImages();

		// new
		btNew.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNew.setContentDisplay(ContentDisplay.LEFT);

		// save
		btSave.setGraphic(ImageManager.instance().getImageView(Images.SAVE));
		btSave.setContentDisplay(ContentDisplay.LEFT);

		// delete
		btDelete.setGraphic(ImageManager.instance().getImageView(Images.DELETE));
		btDelete.setContentDisplay(ContentDisplay.LEFT);

		// read
		btRead.setGraphic(ImageManager.instance().getImageView(Images.READ));
		btRead.setContentDisplay(ContentDisplay.LEFT);

		// write
		btWrite.setGraphic(ImageManager.instance().getImageView(Images.WRITE));
		btWrite.setContentDisplay(ContentDisplay.LEFT);

		// connect
		btConnect.setGraphic(ImageManager.instance().getImageView(Images.CONNECT));
		btConnect.setContentDisplay(ContentDisplay.LEFT);

		// disconnect
		btDisconnect.setGraphic(ImageManager.instance().getImageView(Images.DISCONNECT));
		btDisconnect.setContentDisplay(ContentDisplay.LEFT);

		// cancel connect
		btCancelConnect.setGraphic(ImageManager.instance().getImageView(Images.CANCEL));
		btCancelConnect.setContentDisplay(ContentDisplay.LEFT);
		
		// backup
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.LEFT); 
	}

	Integer getUnitId() {
		return Integer.valueOf(tfUnitId.getText());
	}

	Integer getPort() {
		return Integer.valueOf(tfPort.getText());
	}

	@FXML
	private void onSelectDataType() {
		ModbusDataType type = cbDataTypes.getSelectionModel().getSelectedItem();

		String caption = DesignerLocalizer.instance().getLangString("value.count");

		if (type.equals(ModbusDataType.STRING)) {
			caption = DesignerLocalizer.instance().getLangString("string.chars");
		}
		lbValueCount.setText(caption);
	}

	@FXML
	private void onSelectRegisterType() {
		ModbusRegisterType type = cbRegisterTypes.getSelectionModel().getSelectedItem();

		if (type.equals(ModbusRegisterType.COIL) || type.equals(ModbusRegisterType.DISCRETE)) {
			cbDataTypes.getSelectionModel().select(ModbusDataType.DISCRETE);
		}
	}

	@FXML
	private void onSelectDataSource() {
		try {
			ModbusSource source = cbDataSources.getSelectionModel().getSelectedItem();

			if (source == null) {
				return;
			}
			setSource(source);

			btConnect.setDisable(false);
			btDisconnect.setDisable(false);
			btCancelConnect.setDisable(false);

			tfConnectionName.setText(source.getName());
			tfDescription.setText(source.getDescription());
			tfHost.setText(source.getHost());
			tfPort.setText(String.valueOf(source.getPort()));
			cbTransports.getSelectionModel().select(source.getTransport());

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onDeleteDataSource() {
		try {
			// delete
			ModbusSource source = getSource();

			if (source != null) {
				// confirm
				ButtonType type = AppUtils.showConfirmationDialog(
						DesignerLocalizer.instance().getLangString("object.delete", source.toString()));

				if (type.equals(ButtonType.CANCEL)) {
					return;
				}

				PersistenceService.instance().delete(source);
				dataSources.remove(source);
				cbDataSources.setItems(dataSources);

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewDataSource() {
		try {
			tfConnectionName.clear();
			tfHost.clear();
			tfDescription.clear();
			cbDataSources.getSelectionModel().clearSelection();
			tfRegisterAddress.setText("0");
			tfUnitId.clear();
			tfValueCount.setText("1");
			ckReverseEndianess.setSelected(true);

			lbState.setText(null);
			taValues.clear();

			setSource(new ModbusSource());

			setDefaults();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void updateSource() throws Exception {
		ModbusSource eventSource = getSource();

		eventSource.setName(getName());
		eventSource.setDescription(getDescription());
		eventSource.setTransport(cbTransports.getSelectionModel().getSelectedItem());
		eventSource.setHost(tfHost.getText());
		eventSource.setPort(Integer.valueOf(tfPort.getText()));
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			updateSource();

			ModbusSource eventSource = getSource();

			// save data source
			ModbusSource saved = (ModbusSource) PersistenceService.instance().save(eventSource);
			setSource(saved);

			populateDataSources();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	public ModbusEndpoint setRegisterData() throws Exception {
		ModbusEndpoint endpoint = new ModbusEndpoint();
		endpoint.setUnitId(Integer.valueOf(tfUnitId.getText()));
		endpoint.setRegisterType(cbRegisterTypes.getSelectionModel().getSelectedItem());
		endpoint.setRegisterAddress(Integer.valueOf(tfRegisterAddress.getText()));
		endpoint.setValueCount(Integer.valueOf(tfValueCount.getText()));
		endpoint.setReverseEndianess(ckReverseEndianess.isSelected());
		endpoint.setModbusDataType(cbDataTypes.getSelectionModel().getSelectedItem());

		if (getSource() != null) {
			getSource().setEndpoint(endpoint);
		}
		return endpoint;
	}

	@FXML
	private void onRead() {
		try {
			ModbusEndpoint endpoint = setRegisterData();
			endpoint.setValueCount(Integer.valueOf(tfValueCount.getText()));

			List<ModbusVariant> values = getApp().getModbusMaster().readDataSource(endpoint);

			taValues.clear();
			String stringValues = "";

			for (int i = 0; i < values.size(); i++) {
				if (i > 0) {
					stringValues += "\n";
				}
				stringValues += values.get(i);
			}

			taValues.setText(stringValues);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onWrite() {
		try {
			// values
			String valueList = taValues.getText();

			if (valueList == null || valueList.trim().length() == 0) {
				return;
			}
			String[] values = valueList.split("\n");

			ModbusEndpoint endpoint = setRegisterData();
			endpoint.setValueCount(values.length);

			List<ModbusVariant> variants = new ArrayList<>(values.length);

			ModbusDataType dataType = endpoint.getDataType();

			switch (dataType) {
			case BYTE_HIGH: {
				for (String value : values) {
					variants.add(new ModbusVariant(dataType, Byte.valueOf(value.trim())));
				}
				break;
			}
			case BYTE_LOW: {
				for (String value : values) {
					variants.add(new ModbusVariant(dataType, Byte.valueOf(value.trim())));
				}
				break;
			}

			case DISCRETE: {
				for (String value : values) {
					variants.add(new ModbusVariant(Boolean.valueOf(value.trim())));
				}
				break;
			}

			case DOUBLE: {
				for (String value : values) {
					variants.add(new ModbusVariant(dataType, Double.valueOf(value.trim())));
				}
				break;
			}

			case INT16: {
				for (String value : values) {
					variants.add(new ModbusVariant(dataType, Short.valueOf(value.trim())));
				}
				break;
			}

			case INT32: {
				for (String value : values) {
					variants.add(new ModbusVariant(dataType, Integer.valueOf(value.trim())));
				}
				break;
			}

			case INT64: {
				for (String value : values) {
					variants.add(new ModbusVariant(dataType, Long.valueOf(value.trim())));
				}
				break;
			}

			case SINGLE: {
				for (String value : values) {
					variants.add(new ModbusVariant(dataType, Float.valueOf(value.trim())));
				}
				break;
			}

			case STRING: {
				for (String value : values) {
					variants.add(new ModbusVariant(value.trim()));
				}
				break;
			}

			case UINT16: {
				for (String value : values) {
					variants.add(new ModbusVariant(dataType, Integer.valueOf(value.trim())));
				}
				break;
			}

			case UINT32: {
				for (String value : values) {
					variants.add(new ModbusVariant(dataType, Long.valueOf(value.trim())));
				}
				break;
			}

			default:
				return;
			}

			// write to data source
			getApp().getModbusMaster().writeDataSource(endpoint, variants);

			taValues.clear();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateRegisterTypes() {
		registerTypes.clear();
		registerTypes.addAll(ModbusRegisterType.values());
		cbRegisterTypes.setItems(registerTypes);
	}

	private void populateDataTypes() {
		dataTypes.clear();
		dataTypes.addAll(ModbusDataType.values());
		cbDataTypes.setItems(dataTypes);
	}

	private void populateTransports() {
		transports.clear();
		transports.addAll(ModbusTransport.values());
		cbTransports.setItems(transports);
		cbTransports.getSelectionModel().select(ModbusTransport.TCP);
	}

	private void populateDataSources() throws Exception {
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.MODBUS);

		dataSources.clear();
		for (CollectorDataSource source : sources) {
			dataSources.add((ModbusSource) source);
		}
		cbDataSources.setItems(dataSources);

		if (dataSources.size() == 1) {
			cbDataSources.getSelectionModel().select(0);
			onSelectDataSource();
		}
	}

	String getHost() {
		return tfHost.getText();
	}

	String getName() {
		return tfConnectionName.getText();
	}

	String getDescription() {
		return tfDescription.getText();
	}

	private void updateConnectionStatus(ConnectionState state) {
		connectionState = state;

		switch (state) {
		case CONNECTED:
			piConnection.setVisible(false);
			lbState.setText(ConnectionState.CONNECTED.toString());
			lbState.setTextFill(ConnectionState.CONNECTED_COLOR);
			break;

		case CONNECTING:
			piConnection.setVisible(true);
			lbState.setText(ConnectionState.CONNECTING.toString());
			lbState.setTextFill(ConnectionState.CONNECTING_COLOR);
			break;

		case DISCONNECTED:
			Platform.runLater(() -> {
				piConnection.setVisible(false);
				lbState.setText(ConnectionState.DISCONNECTED.toString());
				lbState.setTextFill(ConnectionState.DISCONNECTED_COLOR);
			});
			break;

		default:
			break;
		}
	}

	@FXML
	private void onConnect() {
		try {
			if (connectionState.equals(ConnectionState.CONNECTED)) {
				// disconnect first
				onDisconnect();
			}

			// connect
			updateConnectionStatus(ConnectionState.CONNECTING);

			updateSource();

			startConnectionService();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onDisconnect() {
		try {
			// disconnect
			disconnectFromDataSource();

			terminateConnectionService();
			updateConnectionStatus(ConnectionState.DISCONNECTED);

			btRead.setDisable(true);
			btWrite.setDisable(true);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	@Override
	protected void onCancelConnect() throws Exception {
		try {
			cancelConnectionService();
			updateConnectionStatus(ConnectionState.DISCONNECTED);

			btRead.setDisable(true);
			btWrite.setDisable(true);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	protected void onConnectionSucceeded() throws Exception {
		updateConnectionStatus(ConnectionState.CONNECTED);

		btRead.setDisable(false);
		btWrite.setDisable(false);
	}

	@FXML
	private void onBackup() {
		backupToFile(ModbusSource.class);
	}
}
