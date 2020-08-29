package org.point85.app.designer;

import org.point85.app.AppUtils;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public abstract class DataSourceConnectionController extends DesignerDialogController {
	protected static final String NO_ERROR = "NO_ERROR";

	// state of our connection
	protected ConnectionState connectionState = ConnectionState.DISCONNECTED;

	// maximum time to allow for an OPC Server connection
	protected static final long MAX_WAIT_SECONDS = 30;

	// Connection service
	private ConnectionService service;

	protected abstract void onCancelConnect() throws Exception;

	protected abstract void onConnectionSucceeded() throws Exception;

	protected DataSourceConnectionController() {
		super();
	}

	protected ConnectionService getConnectionService() {
		return service;
	}

	protected boolean isConnected() {
		return service != null;
	}

	protected void startConnectionService() {
		// create connection service
		service = new ConnectionService();

		service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				String value = (String) event.getSource().getValue();

				if (value != null && value.equals(NO_ERROR)) {
					try {
						// successful connect
						onConnectionSucceeded();

					} catch (Exception e) {
						AppUtils.showErrorDialog(e);
					}
				} else {
					// connection failed
					AppUtils.showErrorDialog(value);
				}
			}
		});

		service.setOnCancelled(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				String value = (String) event.getSource().getValue();
				if (value != null) {
					service = null;
					AppUtils.showErrorDialog(value);
				}
			}
		});

		service.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				String value = (String) event.getSource().getValue();
				if (value != null) {
					service = null;
					AppUtils.showErrorDialog(value);
				}
			}
		});

		service.start();
	}

	protected void terminateConnectionService() throws Exception {
		service = null;
	}

	protected void cancelConnectionService() {
		if (service == null) {
			return;
		}
		Platform.runLater(() -> service.cancel());
	}

	protected abstract void connectToDataSource() throws Exception;

	// class to connect to the OPC DA server
	private class ConnectionService extends Service<String> {

		private ConnectionService() {
		}

		@Override
		protected Task<String> createTask() {
			return new Task<String>() {

				@Override
				protected String call() throws Exception {
					String errorMessage = NO_ERROR;

					try {
						// connect to a specific data source
						connectToDataSource();
					} catch (Exception e) {
						errorMessage = e.getMessage();
					}
					return errorMessage;
				}
			};
		}
	}
}
