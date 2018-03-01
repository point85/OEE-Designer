package org.point85.app.opc.da;

import org.point85.app.designer.DataSourceConnectionController;
import org.point85.domain.opc.da.OpcDaSource;

public abstract class OpcDaController extends DataSourceConnectionController {
	// current source
	private OpcDaSource source;

	public OpcDaSource getSource() {
		if (source == null) {
			source = new OpcDaSource();
		}
		return this.source;
	}

	protected void setSource(OpcDaSource source) {
		this.source = source;
	}

	@Override
	protected void terminateConnectionService() throws Exception {
		// disconnect
		getApp().getOpcDaClient().disconnect();
		super.terminateConnectionService();
	}

	@Override
	protected void connectToDataSource() throws Exception {
		if (source == null) {
			throw new Exception("The OPC DA source is not defined.");
		}
		
		// connect to OPC server
		getApp().getOpcDaClient().connect(source);
	}
}
