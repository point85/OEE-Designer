package org.point85.app.designer;

import java.util.Timer;
import java.util.TimerTask;

import org.point85.app.AppUtils;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;

public abstract class DataSourceConnectionController extends DesignerDialogController {
	protected static final String NO_ERROR = "NO_ERROR";

	// connection states
	public enum ConnectionState {
		DISCONNECTED, CONNECTING, CONNECTED
	};

	// state of our connection
	protected ConnectionState connectionState = ConnectionState.DISCONNECTED;

	// TODO maximum time to allow an OPC Server connection
	protected static final long MAX_WAIT_SECONDS = 600;

	// connection state colors
	public static final Color CONNECTED_COLOR = Color.GREEN;
	public static final Color CONNECTING_COLOR = Color.BLUE;
	public static final Color DISCONNECTED_COLOR = Color.BLACK;

	// timer to check for long-running connection
	private Timer connectionTimer;

	// the long-running connection task
	private ConnectionCancellerTask cancellerTask;

	// OPC DA server connection service
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
		return service != null ? true : false;
	}

	protected void startConnectionService() {
		// start the timeout timer
		cancellerTask = new ConnectionCancellerTask();
		connectionTimer = new Timer();
		connectionTimer.schedule(cancellerTask, MAX_WAIT_SECONDS * 1000);

		// create connection service
		service = new ConnectionService();

		service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				String value = (String) event.getSource().getValue();

				if (value != null && value.equals(NO_ERROR)) {
					try {
						// successful connect
						connectionTimer.cancel();

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

	protected void cancelConnectionService() throws Exception {
		if (service == null) {
			return;
		}
		Platform.runLater(() -> {
			service.cancel();  
		});
	}

	protected abstract void connectToDataSource() throws Exception;

	// class to connect to the OPC DA server
	private class ConnectionService extends Service<String> {

		public ConnectionService() {

		}

		@Override
		protected Task<String> createTask() {
			Task<String> connectTask = new Task<String>() {

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
			return connectTask;
		}
	}

	private class ConnectionCancellerTask extends TimerTask {
		@Override
		public void run() {
			try {
				onCancelConnect();
				cancellerTask.cancel();
			} catch (Exception e) {
				// ignore
			}
		}
	}
}
