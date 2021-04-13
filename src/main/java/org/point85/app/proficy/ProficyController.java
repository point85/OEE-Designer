 package org.point85.app.proficy;

import org.point85.app.designer.DataSourceConnectionController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.proficy.ProficySource;

public abstract class ProficyController extends DataSourceConnectionController {
	// current source
	private ProficySource source;

	public ProficySource getSource() {
		if (source == null) {
			source = new ProficySource();
		}
		return this.source;
	}

	protected void setSource(ProficySource source) {
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
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.proficy.source"));
		}
	}
}
