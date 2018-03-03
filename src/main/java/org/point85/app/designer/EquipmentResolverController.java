package org.point85.app.designer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.point85.app.AppUtils;
import org.point85.app.ImageEnum;
import org.point85.app.ImageManager;
import org.point85.app.opc.ua.OpcUaTreeNode;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.http.HttpSource;
import org.point85.domain.messaging.MessagingSource;
import org.point85.domain.opc.da.OpcDaBrowserLeaf;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.da.OpcDaVariant;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.persistence.PersistencyService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.script.ResolverFunction;
import org.point85.domain.script.ScriptResolver;
import org.point85.domain.script.ScriptResolverType;
import org.point85.domain.web.WebSource;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class EquipmentResolverController extends DesignerController {
	// availability reason resolvers
	private ObservableList<ScriptResolver> scriptResolvers = FXCollections.observableArrayList(new ArrayList<>());

	// reason resolver being edited
	private ScriptResolver selectedScriptResolver;

	@FXML
	private ComboBox<ScriptResolverType> cbResolverTypes;

	@FXML
	private ComboBox<DataSourceType> cbDataSources;

	@FXML
	private ComboBox<DataCollector> cbCollectors;

	@FXML
	private Button btBrowseSource;

	@FXML
	private TextField tfSourceId;

	@FXML
	private TextField tfServerId;

	@FXML
	private TextField tfUpdatePeriod;

	@FXML
	private Button btEditCollector;

	@FXML
	private Button btEditScript;

	@FXML
	private Label lbScript;

	@FXML
	private Label lbDataType;

	@FXML
	private Button btNewResolver;

	@FXML
	private Button btAddResolver;

	@FXML
	private Button btRemoveResolver;

	@FXML
	private TableView<ScriptResolver> tvResolvers;

	@FXML
	private TableColumn<ScriptResolver, String> tcCollector;

	@FXML
	private TableColumn<ScriptResolver, String> tcResolverType;

	@FXML
	private TableColumn<ScriptResolver, String> tcDataSourceType;

	@FXML
	private TableColumn<ScriptResolver, String> tcServer;

	@FXML
	private TableColumn<ScriptResolver, String> tcSourceId;

	@FXML
	private TableColumn<ScriptResolver, String> tcScript;

	@FXML
	private TableColumn<ScriptResolver, Integer> tcUpdatePeriod;

	@FXML
	private TableColumn<ScriptResolver, String> tcDataType;

	@FXML
	private Button btRun;

	public ScriptResolver getSelectedResolver() {
		if (selectedScriptResolver == null) {
			// new resolver
			selectedScriptResolver = new ScriptResolver();
		}
		return selectedScriptResolver;
	}

	private void setDataCollectors() {
		List<DataCollector> collectors = PersistencyService.instance().fetchAllDataCollectors();
		cbCollectors.getItems().clear();
		cbCollectors.getItems().addAll(collectors);
	}

	void initialize(DesignerApplication app) throws Exception {
		setApp(app);

		// data collectors
		setDataCollectors();

		// data sources
		cbDataSources.getItems().addAll(DataSourceType.values());

		// resolver types
		cbResolverTypes.getItems().addAll(ScriptResolverType.values());

		// images
		setImages();

		// class reasons
		try {
			initializeResolverTable();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeResolverTable() throws Exception {
		// bind to list of reason resolvers
		tvResolvers.setItems(scriptResolvers);

		// add the table view listener for reason resolver selection
		tvResolvers.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectScriptResolver(newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		// collector column
		tcCollector.setCellValueFactory(cellDataFeatures -> {
			DataCollector collector = cellDataFeatures.getValue().getCollector();
			SimpleStringProperty property = null;

			if (collector != null) {
				property = new SimpleStringProperty(collector.getName());
			}
			return property;
		});

		// resolver type column
		tcResolverType.setCellValueFactory(cellDataFeatures -> {
			ScriptResolverType type = cellDataFeatures.getValue().getType();
			SimpleStringProperty property = null;

			if (type != null) {
				property = new SimpleStringProperty(type.toString());
			}

			return property;
		});

		// data source type column
		tcDataSourceType.setCellValueFactory(cellDataFeatures -> {
			DataSource dataSource = cellDataFeatures.getValue().getDataSource();
			SimpleStringProperty property = null;

			if (dataSource != null && dataSource.getDataSourceType() != null) {
				property = new SimpleStringProperty(dataSource.getDataSourceType().toString());
			}

			return property;
		});

		// server column
		tcServer.setCellValueFactory(cellDataFeatures -> {
			DataSource dataSource = cellDataFeatures.getValue().getDataSource();
			SimpleStringProperty property = null;

			if (dataSource != null) {
				property = new SimpleStringProperty(dataSource.getId());
			}

			return property;
		});

		// data source id column
		tcSourceId.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getSourceId());
		});

		// data type column
		tcDataType.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getDataType());
		});

		// script column
		tcScript.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			String functionScript = cellDataFeatures.getValue().getScript();
			if (functionScript != null) {
				ResolverFunction resolver;
				try {
					resolver = new ResolverFunction(functionScript);
					property = new SimpleStringProperty(resolver.getDisplayString());
				} catch (Exception e) {
					// ignore
				}
			}
			return property;
		});

		// update column
		tcUpdatePeriod.setCellValueFactory(cellDataFeatures -> {
			Integer updatePeriod = cellDataFeatures.getValue().getUpdatePeriod();
			return new SimpleObjectProperty<Integer>(updatePeriod);
		});
	}

	private void onSelectScriptResolver(ScriptResolver scriptResolver) {
		if (scriptResolver == null) {
			return;
		}
		selectedScriptResolver = scriptResolver;

		// collector
		cbCollectors.getSelectionModel().select(scriptResolver.getCollector());

		// resolver type
		cbResolverTypes.getSelectionModel().select(scriptResolver.getType());

		DataSource source = scriptResolver.getDataSource();

		// data source type
		if (source != null) {
			tfServerId.setText(source.getId());
			this.cbDataSources.getSelectionModel().select(source.getDataSourceType());
		}

		// source id
		this.tfSourceId.setText(scriptResolver.getSourceId());

		// data type
		this.lbDataType.setText(scriptResolver.getDataType());

		// script
		String functionScript = scriptResolver.getScript();
		lbScript.setText(null);
		if (functionScript != null) {
			try {
				ResolverFunction resolver = new ResolverFunction(functionScript);
				lbScript.setText(resolver.getDisplayString());
			} catch (Exception e) {
				// ignore
			}
		}

		// update period
		this.tfUpdatePeriod.setText(String.valueOf(scriptResolver.getUpdatePeriod()));

		btAddResolver.setText("Update");
	}

	void showResolvers(Equipment equipment) throws Exception {
		if (equipment == null) {
			return;
		}

		clearEditor();

		scriptResolvers.clear();
		for (ScriptResolver resolver : equipment.getScriptResolvers()) {
			scriptResolvers.add(resolver);
		}
		tvResolvers.refresh();
	}

	void setResolvers(Equipment equipment) {
		Set<ScriptResolver> resolvers = new HashSet<>();
		resolvers.clear();
		resolvers.addAll(scriptResolvers);
		equipment.setScriptResolvers(resolvers);
	}

	// Remove reason
	@FXML
	private void onRemoveResolver() {
		try {
			if (selectedScriptResolver == null) {
				AppUtils.showErrorDialog("No reason resolver has been selected for deletion.");
				return;
			}

			// remove from entity
			PlantEntity selectedEntity = getApp().getPhysicalModelController().getSelectedEntity();
			((Equipment) selectedEntity).removeScriptResolver(selectedScriptResolver);

			// remove from list
			scriptResolvers.remove(selectedScriptResolver);
			selectedScriptResolver = null;
			tvResolvers.refresh();
			tvResolvers.getSelectionModel().clearSelection();

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	protected void setImages() throws Exception {
		// new resolver
		btNewResolver.setGraphic(ImageManager.instance().getImageView(ImageEnum.NEW));
		btNewResolver.setContentDisplay(ContentDisplay.RIGHT);

		// add resolver
		btAddResolver.setGraphic(ImageManager.instance().getImageView(ImageEnum.ADD));
		btAddResolver.setContentDisplay(ContentDisplay.RIGHT);

		// remove resolver
		btRemoveResolver.setGraphic(ImageManager.instance().getImageView(ImageEnum.REMOVE));
		btRemoveResolver.setContentDisplay(ContentDisplay.RIGHT);

		// script execution
		btRun.setGraphic(ImageManager.instance().getImageView(ImageEnum.EXECUTE));
		btRun.setContentDisplay(ContentDisplay.RIGHT);

		// browse to data source
		btBrowseSource.setGraphic(ImageManager.instance().getImageView(ImageEnum.SOURCE));
		btBrowseSource.setContentDisplay(ContentDisplay.CENTER);

		// script editor
		btEditScript.setGraphic(ImageManager.instance().getImageView(ImageEnum.SCRIPT));
		btEditScript.setContentDisplay(ContentDisplay.CENTER);

		// collector editor
		btEditCollector.setGraphic(ImageManager.instance().getImageView(ImageEnum.COLLECTOR));
		btEditCollector.setContentDisplay(ContentDisplay.CENTER);
	}

	@FXML
	private void onSelectResolverType() {
		if (getSelectedResolver() != null) {
			ScriptResolverType type = this.cbResolverTypes.getSelectionModel().getSelectedItem();

			if (type != null) {
				getSelectedResolver().setType(type);
			}
		}
	}

	@FXML
	private void onSelectDataSource() throws Exception {
		DataSourceType sourceType = this.cbDataSources.getSelectionModel().getSelectedItem();

		ImageView buttonImage = null;
		switch (sourceType) {
		case HTTP:
			buttonImage = ImageManager.instance().getImageView(ImageEnum.HTTP);
			break;
		case MESSAGING:
			buttonImage = ImageManager.instance().getImageView(ImageEnum.RMQ);
			break;
		case OPC_DA:
			buttonImage = ImageManager.instance().getImageView(ImageEnum.OPC_DA);
			break;
		case OPC_UA:
			buttonImage = ImageManager.instance().getImageView(ImageEnum.OPC_UA);
			break;
		case WEB:
			buttonImage = ImageManager.instance().getImageView(ImageEnum.WEB);
			break;
		default:
			break;
		}
		btBrowseSource.setGraphic(buttonImage);
	}

	// browse to data source
	@FXML
	private void onBrowseSource() {
		try {
			Equipment equipment = (Equipment) getApp().getPhysicalModelController().getSelectedEntity();
			if (equipment == null) {
				throw new Exception("Equipment must be selected before browsing the source.");
			}

			DataSourceType sourceType = cbDataSources.getSelectionModel().getSelectedItem();
			if (sourceType == null) {
				throw new Exception("A data source must be selected");
			}

			// OPC DA
			if (sourceType.equals(DataSourceType.OPC_DA)) {

				OpcDaBrowserLeaf sourceTag = getApp().showOpcDaDataSourceBrowser();

				if (sourceTag == null) {
					return;
				}

				// leaf tag
				tfSourceId.setText(sourceTag.getPathName());

				// its data source
				OpcDaSource dataSource = getApp().getOpcDaBrowserController().getSource();
				getSelectedResolver().setDataSource(dataSource);
				tfServerId.setText(dataSource.getId());

				// data type
				OpcDaVariant nodeDataType = sourceTag.getDataType();
				Class<?> javaType = nodeDataType.getDataType().getJavaClass();
				lbDataType.setText(javaType.getSimpleName());

				// OPC UA
			} else if (sourceType.equals(DataSourceType.OPC_UA)) {
				OpcUaTreeNode node = getApp().showOpcUaDataSourceBrowser();

				if (node == null) {
					return;
				}

				// variable node
				tfSourceId.setText(node.getNodeId().toParseableString());

				// its data source
				OpcUaSource dataSource = getApp().getOpcUaBrowserController().getSource();
				getSelectedResolver().setDataSource(dataSource);
				tfServerId.setText(dataSource.getId());

				// data type
				NodeId nodeDataType = node.getNodeDataType();
				Class<?> javaType = BuiltinDataType.getBackingClass(nodeDataType);
				lbDataType.setText(javaType.getSimpleName());

				// HTTP
			} else if (sourceType.equals(DataSourceType.HTTP)) {
				// show HTTP server editor
				HttpSource dataSource = getApp().showHttpServerEditor();
				tfServerId.setText(dataSource.getId());

				getSelectedResolver().setDataSource(dataSource);

				lbDataType.setText(String.class.getSimpleName());

				setDefaultSourceId();

				// Messaging
			} else if (sourceType.equals(DataSourceType.MESSAGING)) {
				// show RabbitMQ broker editor
				MessagingSource dataSource = getApp().showRmqBrokerEditor();
				tfServerId.setText(dataSource.getId());

				getSelectedResolver().setDataSource(dataSource);

				lbDataType.setText(String.class.getSimpleName());

				setDefaultSourceId();
			}
			// Web server
			else if (sourceType.equals(DataSourceType.WEB)) {
				// show server editor
				WebSource dataSource = getApp().showWebServerEditor();

				if (dataSource == null) {
					return;
				}
				tfServerId.setText(dataSource.getId());

				getSelectedResolver().setDataSource(dataSource);

				lbDataType.setText(String.class.getSimpleName());

				setDefaultSourceId();
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void setDefaultSourceId() {
		PlantEntity entity = getApp().getPhysicalModelController().getSelectedEntity();

		if (entity != null) {
			ScriptResolverType resolverType = cbResolverTypes.getSelectionModel().getSelectedItem();

			if (resolverType != null) {
				DataSourceType sourceType = cbDataSources.getSelectionModel().getSelectedItem();

				String sourceId = entity.getName() + "." + sourceType + "." + resolverType;
				tfSourceId.setText(sourceId);
			}
		}
	}

	// show collector editor
	@FXML
	private void onEditCollector() {
		try {
			getApp().showCollectorEditor();
			setDataCollectors();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// show script editor
	@FXML
	private void onEditScript() {
		try {
			PlantEntity entity = getApp().getPhysicalModelController().getSelectedEntity();

			if (entity == null || !(entity instanceof Equipment)) {
				throw new Exception("Equipment must be selected before editing a script.");
			}

			// resolver type
			ScriptResolverType resolverType = cbResolverTypes.getSelectionModel().getSelectedItem();

			if (resolverType == null) {
				throw new Exception("The script resolver type must be selected.");
			}

			ScriptResolver scriptResolver = getSelectedResolver();

			scriptResolver.setType(resolverType);
			scriptResolver.setDataType(lbDataType.getText());

			if (scriptResolver.getScript() == null || scriptResolver.getScript().length() == 0) {
				if (resolverType.isAvailability()) {
					scriptResolver.setScript(ScriptResolver.getPassthroughScript());
				} else if (resolverType.isProduction()) {
					scriptResolver.setScript(ScriptResolver.getDefaultProductionScript());
				} else if (resolverType.isMaterial()) {
					scriptResolver.setScript(ScriptResolver.getDefaultMaterialScript());
				} else if (resolverType.isJob()) {
					scriptResolver.setScript(ScriptResolver.getDefaultJobScript());
				}
			}

			// display the editor
			String scriptFunction = getApp().showScriptEditor(scriptResolver);

			if (scriptFunction == null) {
				return;
			}

			scriptResolver.setScript(scriptFunction);

			ResolverFunction resolverFunction = new ResolverFunction(scriptFunction);

			// display up to first newline
			this.lbScript.setText(resolverFunction.getDisplayString());

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void clearEditor() {
		this.cbCollectors.getSelectionModel().clearSelection();
		this.cbCollectors.getSelectionModel().select(null);

		this.cbResolverTypes.getSelectionModel().clearSelection();
		this.cbResolverTypes.getSelectionModel().select(null);

		this.cbDataSources.getSelectionModel().clearSelection();
		this.cbDataSources.getSelectionModel().select(null);

		this.tfSourceId.setText(null);
		this.tfServerId.setText(null);
		this.lbDataType.setText(null);
		this.lbScript.setText(null);

		this.tfUpdatePeriod.setText(null);
	}

	@FXML
	private void onNewResolver() {
		try {
			clearEditor();

			selectedScriptResolver = null;

			this.btAddResolver.setText("Add");

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void addScriptResolver() throws Exception {
		if (selectedScriptResolver.getKey() != null) {
			// already added
			return;
		}

		// add to entity
		PlantEntity selectedEntity = getApp().getPhysicalModelController().getSelectedEntity();

		if (selectedEntity == null || !(selectedEntity instanceof Equipment)) {
			throw new Exception("An equipment entity must be selected before adding resolvers to it.");
		}

		Equipment equipment = ((Equipment) selectedEntity);

		// new resolver for equipment
		if (!equipment.hasResolver(selectedScriptResolver)) {
			scriptResolvers.add(selectedScriptResolver);

			equipment.addScriptResolver(selectedScriptResolver);
		}
	}

	// add a new resolver or update an existing one
	@FXML
	private void onAddResolver() {
		try {
			// collector
			selectedScriptResolver.setCollector(cbCollectors.getSelectionModel().getSelectedItem());

			// resolver type
			selectedScriptResolver.setType(cbResolverTypes.getSelectionModel().getSelectedItem());

			// source id
			selectedScriptResolver.setSourceId(tfSourceId.getText());

			// data type
			selectedScriptResolver.setDataType(lbDataType.getText());

			// update period
			String updateText = tfUpdatePeriod.getText();

			if (updateText != null && updateText.trim().length() > 0) {
				selectedScriptResolver.setUpdatePeriod(Integer.valueOf(updateText));
			}

			// add to equipment if necessary
			addScriptResolver();

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

			tvResolvers.refresh();

			selectedScriptResolver = null;

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRun() {
		try {
			if (selectedScriptResolver == null) {
				throw new Exception("A resolver must be selected");
			}

			DataSourceType type = selectedScriptResolver.getDataSource().getDataSourceType();

			if (type == null) {
				throw new Exception("A data source type must be specified.");
			}

			if (type.equals(DataSourceType.OPC_DA)) {
				getApp().showOpcDaTrendDialog(selectedScriptResolver);
			} else if (type.equals(DataSourceType.OPC_UA)) {
				getApp().showOpcUaTrendDialog(selectedScriptResolver);
			} else if (type.equals(DataSourceType.HTTP)) {
				getApp().showHttpTrendDialog(selectedScriptResolver);
			} else if (type.equals(DataSourceType.MESSAGING)) {
				getApp().showMessagingTrendDialog(selectedScriptResolver);
			} else if (type.equals(DataSourceType.WEB)) {
				getApp().showWebTrendDialog(selectedScriptResolver);
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
