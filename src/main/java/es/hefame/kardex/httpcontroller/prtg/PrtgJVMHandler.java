package es.hefame.kardex.httpcontroller.prtg;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.hefame.hcore.http.HttpController;
import es.hefame.hcore.http.exchange.HttpConnection;
import es.hefame.hcore.prtg.sensors.JVMPrtgSensor;

public class PrtgJVMHandler extends HttpController
{
	private static Logger L = LogManager.getLogger();

	@Override
	public void get(HttpConnection exchange) throws IOException
	{
		L.traceEntry();
		L.info("Requested a Self Monitoring data from address [{}]", () -> exchange.request.getIP());
		exchange.response.send(new JVMPrtgSensor(), 200);
	}

}
