package org.point85.app.modbus;

import org.point85.app.designer.DataSourceConnectionController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.modbus.ModbusMaster;
import org.point85.domain.modbus.ModbusSource;

public abstract class ModbusController extends DataSourceConnectionController {
	// current slave source
	private ModbusSource modbusSource;

	public ModbusSource getSource() throws Exception {
		if (modbusSource == null) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.modbus.source"));
		}
		return modbusSource;
	}

	protected void setSource(ModbusSource source) {
		this.modbusSource = source;
	}

	@Override
	protected void connectToDataSource() throws Exception {
		ModbusMaster modbusMaster = getApp().getModbusMaster();

		if (modbusMaster == null) {
			modbusMaster = getApp().createModbusMaster(modbusSource);
		} else {
			modbusMaster.setDataSource(modbusSource);
		}

		if (modbusMaster.getDataSource() == null) {
			if (modbusSource == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.modbus.source"));
			}
		}

		// connect to Modbus master
		modbusMaster.connect();
	}

	protected void disconnectFromDataSource() {
		ModbusMaster modbusMaster = getApp().getModbusMaster();

		if (modbusMaster == null) {
			return;
		}

		// connect to Modbus master
		modbusMaster.disconnect();
	}
}
