package org.point85.app.proficy;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.exim.Exporter;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.proficy.ProficyClient;
import org.point85.domain.proficy.ProficySource;
import org.point85.domain.proficy.TagData;
import org.point85.domain.proficy.TagDataType;
import org.point85.domain.proficy.TagDetail;
import org.point85.domain.proficy.TagSample;
import org.point85.domain.proficy.TagValues;
import org.point85.domain.proficy.TagsListRequestParameters;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ProficyBrowserController extends DesignerDialogController {
	// default ports
	private static final int HTTP_DEFAULT_PORT = 8070;
	private static final int HTTPS_DEFAULT_PORT = 443;
	private static final int UAA_DEFAULT_PORT = 9480;
	private static final int DEFAULT_TAG_COUNT = 100;

	// current source
	private ProficySource dataSource;

	// selected tag
	private TagDetail selectedTag;

	// list of servers and ports
	private final ObservableList<ProficySource> historians = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private ComboBox<ProficySource> cbDataSources;

	@FXML
	private TextField tfConnectionName;

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfHttpPort;

	@FXML
	private TextField tfHttpsPort;

	@FXML
	private TextField tfUaaPort;

	@FXML
	private TextField tfUaaUserName;

	@FXML
	private PasswordField pfUaaUserPassword;

	@FXML
	private TextField tfDescription;

	@FXML
	private CheckBox ckValidateCertificate;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btBackup;
	
	@FXML
	private Button btRefresh;

	@FXML
	private Button btFilter;

	@FXML
	private Button btRead;

	@FXML
	private Button btWrite;

	@FXML
	private Button btClearAuthentication;

	@FXML
	private Label lbTagQuality;

	@FXML
	private Label lbTagTimestamp;

	@FXML
	private Label lbTagDataType;

	@FXML
	private TextArea taTagValue;

	@FXML
	private TextField tfMask;

	@FXML
	private TextField tfTagCount;

	@FXML
	private ListView<String> lvFilteredTags;

	// list of tags
	private final ObservableList<String> filteredTags = FXCollections.observableArrayList();

	public void initialize(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

		// button images
		setImages();

		// retrieve the defined data sources
		populateDataSources();

		if (cbDataSources.getItems().size() == 1) {
			cbDataSources.getSelectionModel().select(0);
		}

		lvFilteredTags.setItems(filteredTags);

		// add the listener for tag selection
		lvFilteredTags.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectTag(newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		tfTagCount.setText(String.valueOf(DEFAULT_TAG_COUNT));
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

		// filter
		btFilter.setGraphic(ImageManager.instance().getImageView(Images.FILTER));
		btFilter.setContentDisplay(ContentDisplay.LEFT);

		// clear
		btClearAuthentication.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));

		// read
		btRead.setGraphic(ImageManager.instance().getImageView(Images.READ));
		btRead.setContentDisplay(ContentDisplay.LEFT);

		// write
		btWrite.setGraphic(ImageManager.instance().getImageView(Images.WRITE));
		btWrite.setContentDisplay(ContentDisplay.LEFT);
		
		// backup
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.LEFT); 
		
		// refresh
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
	}

	public ProficySource getSource() {
		return dataSource;
	}

	protected void setSource(ProficySource source) {
		this.dataSource = source;
	}

	public TagDetail getSelectedTag() {
		return selectedTag;
	}

	private void populateDataSources() throws Exception {
		// fetch the historian ids
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.PROFICY);

		historians.clear();
		for (CollectorDataSource source : sources) {
			historians.add((ProficySource) source);
		}
		ProficySource one = historians.size() == 1 ? historians.get(0) : null;
		historians.add(null);
		cbDataSources.setItems(historians);

		if (one != null) {
			cbDataSources.getSelectionModel().select(one);
			onSelectDataSource();
		}
	}

	@FXML
	private void onSelectDataSource() {
		try {
			dataSource = cbDataSources.getSelectionModel().getSelectedItem();

			if (dataSource == null) {
				onNewDataSource();
				return;
			}

			// host
			this.tfHost.setText(dataSource.getHost());
			this.tfDescription.setText(dataSource.getDescription());

			// connection
			Integer httpPort = dataSource.getPort();
			this.tfHttpPort.setText(httpPort != null ? String.valueOf(httpPort) : "");
			Integer httpsPort = dataSource.getHttpsPort();
			this.tfHttpsPort.setText(httpsPort != null ? String.valueOf(httpsPort) : "");
			this.ckValidateCertificate.setSelected(dataSource.getValidateCertificate());

			// UAA
			Integer uaaPort = dataSource.getUaaHttpPort();
			this.tfUaaPort.setText(uaaPort != null ? String.valueOf(uaaPort) : "");
			this.tfUaaUserName.setText(dataSource.getUserName());
			this.pfUaaUserPassword.setText(dataSource.getUserPassword());
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void clearEditor() {
		// general
		this.cbDataSources.getSelectionModel().clearSelection();

		this.tfHost.clear();
		this.tfDescription.clear();

		// connection
		this.tfHttpPort.setText(String.valueOf(HTTP_DEFAULT_PORT));
		this.tfHttpsPort.setText(String.valueOf(HTTPS_DEFAULT_PORT));
		this.ckValidateCertificate.setSelected(true);

		// UAA
		this.tfUaaPort.setText(String.valueOf(UAA_DEFAULT_PORT));
		this.tfUaaUserName.clear();
		this.pfUaaUserPassword.clear();

		// tags
		this.tfMask.clear();
		this.tfTagCount.setText(String.valueOf(DEFAULT_TAG_COUNT));
		this.filteredTags.clear();

		// tag data
		this.lbTagQuality.setText(null);
		this.lbTagDataType.setText(null);
		this.lbTagTimestamp.setText(null);
		this.taTagValue.clear();
	}

	@FXML
	private void onNewDataSource() {
		try {
			clearEditor();

			setSource(new ProficySource());
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	String getHost() {
		return this.tfHost.getText();
	}

	String getDescription() {
		return this.tfDescription.getText();
	}

	String getUaaUserName() {
		return this.tfUaaUserName.getText();
	}

	String getUaaUserPassword() {
		return this.pfUaaUserPassword.getText();
	}

	Integer getUaaHttpPort() {
		String port = tfUaaPort.getText().trim();
		return (!port.isEmpty()) ? Integer.parseInt(port) : null;
	}

	Integer getHttpPort() {
		String port = tfHttpPort.getText().trim();
		return (!port.isEmpty()) ? Integer.parseInt(port) : null;
	}

	Integer getHttpsPort() {
		String port = tfHttpsPort.getText().trim();
		return (!port.isEmpty()) ? Integer.parseInt(port) : null;
	}

	@FXML
	private void onSaveDataSource() {
		try {
			ProficySource eventSource = getSource();

			if (eventSource == null) {
				return;
			}

			// general
			eventSource.setHost(getHost());
			eventSource.setDescription(getDescription());

			// UAA
			eventSource.setUaaHttpPort(getUaaHttpPort());
			eventSource.setUserName(getUaaUserName());
			eventSource.setPassword(getUaaUserPassword());

			// connection
			eventSource.setHttpPort(getHttpPort());
			eventSource.setHttpsPort(getHttpsPort());
			eventSource.setValidateCertificate(ckValidateCertificate.isSelected());

			// name host and port
			Integer port = getHttpsPort() != null ? getHttpsPort() : getHttpPort();
			eventSource.setName(getHost() + ":" + port);

			// save data source
			ProficySource saved = (ProficySource) PersistenceService.instance().save(eventSource);
			setSource(saved);

			// update list
			if (eventSource.getKey() != null) {
				// updated
				cbDataSources.getItems().remove(eventSource);
			}
			cbDataSources.getItems().add(saved);

			if (cbDataSources.getItems().size() == 1) {
				cbDataSources.getSelectionModel().select(0);
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onDeleteDataSource() {
		try {
			// delete
			if (dataSource != null) {
				// confirm
				ButtonType type = AppUtils.showConfirmationDialog(
						DesignerLocalizer.instance().getLangString("object.delete", dataSource.toString()));

				if (type.equals(ButtonType.CANCEL)) {
					return;
				}

				// delete from database
				PersistenceService.instance().delete(dataSource);

				clearEditor();
				this.cbDataSources.getItems().remove(dataSource);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onFilterTags() {
		if (dataSource == null) {
			return;
		}

		try {
			String mask = tfMask.getText();
			Integer count = Integer.parseInt(tfTagCount.getText());

			// tag names
			ProficyClient proficyClient = new ProficyClient(dataSource);
			List<String> tagNames = proficyClient.readTagNames(mask, count);
			Collections.sort(tagNames);

			filteredTags.clear();

			for (String name : tagNames) {
				filteredTags.add(name);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onClearAuthentication() {
		// UAA
		this.tfUaaPort.setText(String.valueOf(UAA_DEFAULT_PORT));
		this.tfUaaUserName.clear();
		this.pfUaaUserPassword.clear();
	}

	private void showCurrentTagValue(ProficyClient proficyClient) throws Exception {
		if (selectedTag == null) {
			return;
		}
		// current value
		List<String> tagNames = new ArrayList<>(1);
		tagNames.add(selectedTag.getTagName());
		TagValues currentValue = proficyClient.readCurrentValue(tagNames);
		TagData tagData = currentValue.getTagData().get(0);
		TagSample tagSample = tagData.getSamples().get(0);
		lbTagQuality.setText(tagSample.getEnumeratedQuality().toString());

		String value = null;
		if (!tagSample.isArray()) {
			value = tagSample.getTypedValue(selectedTag.getEnumeratedType()).toString();
		} else {
			value = toDisplayString(tagSample.getTypedList(selectedTag.getEnumeratedType()));
		}
		taTagValue.setText(value);
	}

	private void onSelectTag(String tagName) throws Exception {
		if (tagName == null) {
			return;
		}

		// details
		TagsListRequestParameters params = new TagsListRequestParameters();
		params.addRequestParameter(TagsListRequestParameters.TAG_NAME, tagName,
				TagsListRequestParameters.MATCH_MULTI_CHAR);

		ProficyClient proficyClient = new ProficyClient(dataSource);
		List<TagDetail> tagDetails = proficyClient.readTagsList(params).getTagDetails();

		if (tagDetails.isEmpty()) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("cannot.read.tag.details", tagName));
		}

		// detail info
		TagDetail detail = tagDetails.get(0);
		selectedTag = detail;

		TagDataType dataType = detail.getEnumeratedType();
		lbTagDataType.setText(dataType.toString());
		Instant lastModifiedTime = detail.getLastModifiedTime();
		lbTagTimestamp.setText(lastModifiedTime.toString());

		showCurrentTagValue(proficyClient);
	}

	private String toDisplayString(List<Object> dataValues) {
		StringBuilder sb = new StringBuilder();

		sb.append('[');

		for (int i = 0; i < dataValues.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(dataValues.get(i).toString());
		}
		sb.append(']');

		return sb.toString();
	}

	@FXML
	private void onRead() {
		try {
			ProficyClient proficyClient = new ProficyClient(dataSource);

			showCurrentTagValue(proficyClient);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onWrite() {
		if (selectedTag == null) {
			return;
		}

		try {
			ProficyClient proficyClient = new ProficyClient(dataSource);

			proficyClient.writeTag(selectedTag.getTagName(), taTagValue.getText());

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRefresh() throws Exception {
		populateDataSources();
	}
	
	private void backupSources(List<CollectorDataSource> sources) {
		try {
			// show file chooser
			File file = getBackupFile();

			if (file != null) {
				// backup
				Exporter.instance().backupProficySources(sources, file);

				AppUtils.showInfoDialog(
						DesignerLocalizer.instance().getLangString("backup.successful", file.getCanonicalPath()));
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
	
	@FXML
	private void onBackup() {
		if (getSource() == null || getSource().getName() == null) {
			// confirm all
			ButtonType type = AppUtils.showConfirmationDialog(
					DesignerLocalizer.instance().getLangString("all.export", ProficySource.class.getSimpleName()));

			if (type.equals(ButtonType.CANCEL)) {
				return;
			}

			backupToFile(ProficySource.class);
		} else {
			// one source
			ProficySource source = getSource();

			List<CollectorDataSource> sources = new ArrayList<>();
			sources.add(source);

			// confirm
			ButtonType type = AppUtils.showConfirmationDialog(
					DesignerLocalizer.instance().getLangString("object.export", source.getName()));

			if (type.equals(ButtonType.CANCEL)) {
				return;
			}

			backupSources(sources);
		}
	}
}
