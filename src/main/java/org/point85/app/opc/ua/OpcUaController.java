package org.point85.app.opc.ua;

import org.point85.app.designer.DataSourceConnectionController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.opc.ua.OpcUaSource;

public abstract class OpcUaController extends DataSourceConnectionController {
	// current source
	private OpcUaSource dataSource;

	public OpcUaSource getSource() {
		if (dataSource == null) {
			dataSource = new OpcUaSource();
		}
		return dataSource;
	}

	protected void setSource(OpcUaSource source) {
		this.dataSource = source;
	}

	@Override
	protected void terminateConnectionService() throws Exception {

		// disconnect
		if (getApp().getOpcUaClient() != null) {
			getApp().getOpcUaClient().disconnect();
		}
		super.terminateConnectionService();
	}

	@Override
	protected void connectToDataSource() throws Exception {
		if (dataSource == null) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.ua.source"));
		}

		// connect to OPC server
		getApp().getOpcUaClient().connect(dataSource);
	}
}
