package org.point85.app.email;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.email.EmailClient;
import org.point85.domain.email.EmailProtocol;
import org.point85.domain.email.EmailSecurityPolicy;
import org.point85.domain.email.EmailSource;
import org.point85.domain.persistence.PersistenceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

public class EmailServerController extends DesignerDialogController {
	// default ports
	private static final int SMTP_DEFAULT_PORT = 465;
	private static final int IMAP_DEFAULT_PORT = 993;
	private static final int POP3_DEFAULT_PORT = 995;

	// current source
	private EmailSource dataSource;

	// list of servers and ports
	private final ObservableList<EmailSource> servers = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfReceiveHost;

	@FXML
	private TextField tfReceivePort;

	@FXML
	private TextField tfSendHost;

	@FXML
	private TextField tfSendPort;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfUserPassword;

	@FXML
	private CheckBox ckReceiveSSL;

	@FXML
	private CheckBox ckSendSSL;

	@FXML
	private RadioButton rbIMAP;

	@FXML
	private RadioButton rbPOP3;

	@FXML
	private ComboBox<EmailSource> cbDataSources;

	@FXML
	private TextField tfDescription;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btTest;

	public void initialize(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

		// button images
		setImages();

		// retrieve the defined data sources
		populateDataSources();
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

		// test
		btTest.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btTest.setContentDisplay(ContentDisplay.LEFT);
	}

	public EmailSource getSource() {
		// set source
		if (dataSource == null) {
			dataSource = new EmailSource();
		}
		return dataSource;
	}

	protected void setSource(EmailSource source) {
		this.dataSource = source;
	}

	@FXML
	private void onTest() {
		EmailClient emailClient = null;

		try {
			emailClient = new EmailClient(dataSource);

			emailClient.sendMail(dataSource.getUserName(),
					DesignerLocalizer.instance().getLangString("email.test.subject"),
					DesignerLocalizer.instance().getLangString("email.test.content"));

			AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("connection.successful"));

		} catch (Exception e) {
			AppUtils.showErrorDialog(
					DesignerLocalizer.instance().getErrorString("connection.failed", DomainUtils.formatException(e)));
		} finally {
			if (emailClient != null) {
				try {
					emailClient.stopPolling();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	@FXML
	private void onSelectDataSource() {
		try {
			dataSource = cbDataSources.getSelectionModel().getSelectedItem();

			if (dataSource == null) {
				return;
			}

			// general
			this.tfUserName.setText(dataSource.getUserName());
			this.pfUserPassword.setText(dataSource.getUserPassword());
			this.tfDescription.setText(dataSource.getDescription());

			// receive
			this.tfReceiveHost.setText(dataSource.getReceiveHost());
			this.tfReceivePort.setText(String.valueOf(dataSource.getReceivePort()));
			this.rbIMAP.setSelected(dataSource.getProtocol().equals(EmailProtocol.IMAP));
			this.rbPOP3.setSelected(dataSource.getProtocol().equals(EmailProtocol.POP3));
			this.ckReceiveSSL.setSelected(dataSource.getReceiveSecurityPolicy().equals(EmailSecurityPolicy.SSL)); 

			// send
			this.tfSendHost.setText(dataSource.getSendHost());
			this.tfSendPort.setText(String.valueOf(dataSource.getSendPort()));
			this.ckSendSSL.setSelected(dataSource.getSendSecurityPolicy().equals(EmailSecurityPolicy.SSL));

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

				PersistenceService.instance().delete(dataSource);
				servers.remove(dataSource);

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void clearEditor() {
		// general
		this.tfDescription.clear();
		this.tfUserName.clear();
		this.pfUserPassword.clear();

		// receive
		this.tfReceiveHost.clear();
		this.tfReceivePort.clear();
		this.ckReceiveSSL.setSelected(true);

		// send
		this.tfSendHost.clear();
		this.tfSendPort.clear();
		this.ckSendSSL.setSelected(true);
		this.rbIMAP.setSelected(true);

		this.cbDataSources.getSelectionModel().clearSelection();
	}

	@FXML
	private void onNewDataSource() {
		try {
			clearEditor();
			tfReceivePort.setText(String.valueOf(IMAP_DEFAULT_PORT));
			tfSendPort.setText(String.valueOf(SMTP_DEFAULT_PORT));
			setSource(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private EmailSecurityPolicy getReceiveSecurityPolicy() {
		return ckReceiveSSL.isSelected() ? EmailSecurityPolicy.SSL : EmailSecurityPolicy.NONE;
	}

	private EmailSecurityPolicy getSendSecurityPolicy() {
		return ckSendSSL.isSelected() ? EmailSecurityPolicy.SSL : EmailSecurityPolicy.NONE;
	}

	private EmailProtocol getReceiveProtocol() {
		return rbIMAP.isSelected() ? EmailProtocol.IMAP : EmailProtocol.POP3;
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			EmailSource eventSource = getSource();

			// general
			eventSource.setUserName(getUserName());
			eventSource.setPassword(getPassword());
			eventSource.setDescription(getDescription());

			// receive
			eventSource.setReceiveHost(getReceiveHost());
			eventSource.setReceivePort(getReceivePort());
			eventSource.setReceiveSecurityPolicy(getReceiveSecurityPolicy());

			// send
			eventSource.setSendHost(getSendHost());
			eventSource.setSendPort(getSendPort());
			eventSource.setProtocol(getReceiveProtocol());
			eventSource.setSendSecurityPolicy(getSendSecurityPolicy());

			// name host and port
			eventSource.setName(getReceiveHost() + ":" + getReceivePort());

			// save data source
			EmailSource saved = (EmailSource) PersistenceService.instance().save(eventSource);
			setSource(saved);

			// update list
			if (eventSource.getKey() != null) {
				// updated
				cbDataSources.getItems().remove(eventSource);
			}
			cbDataSources.getItems().add(saved);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateDataSources() throws Exception {
		// fetch the server ids
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.EMAIL);

		servers.clear();
		for (CollectorDataSource source : sources) {
			servers.add((EmailSource) source);
		}
		cbDataSources.setItems(servers);

		if (servers.size() == 1) {
			this.cbDataSources.getSelectionModel().select(0);
			onSelectDataSource();
		}
	}

	String getReceiveHost() {
		return this.tfReceiveHost.getText();
	}

	Integer getReceivePort() {
		return Integer.valueOf(tfReceivePort.getText());
	}

	String getSendHost() {
		return this.tfSendHost.getText();
	}

	Integer getSendPort() {
		return Integer.valueOf(tfSendPort.getText());
	}

	String getUserName() {
		return this.tfUserName.getText();
	}

	String getPassword() {
		return this.pfUserPassword.getText();
	}

	String getDescription() {
		return this.tfDescription.getText();
	}

	@FXML
	private void onProtocolSelection() {
		if (rbIMAP.isSelected()) {
			tfReceivePort.setText(String.valueOf(IMAP_DEFAULT_PORT));
		} else {
			tfReceivePort.setText(String.valueOf(POP3_DEFAULT_PORT));
		}
	}
}
