package es.hefame.kardex.httpcontroller.prtg;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.hefame.hcore.http.HttpController;
import es.hefame.hcore.http.exchange.HttpConnection;
import es.hefame.hcore.oracle.DBConnection;
import es.hefame.hcore.prtg.PrtgChannelResult;
import es.hefame.hcore.prtg.PrtgSensor;
import es.hefame.hcore.prtg.PrtgThresshold;

public class PrtgOracleHandler extends HttpController
{
	private static Logger L = LogManager.getLogger();

	@Override
	public void get(HttpConnection exchange) throws IOException
	{
		L.traceEntry();
		L.info("Requested a Self Monitoring data from address [{}]", () -> exchange.request.getIP());

		PrtgSensor sensor = new PrtgSensor();
		PrtgChannelResult channel = new PrtgChannelResult("Oracle Connected", (DBConnection.isConnectionAlive() ? 1 : 0), "Count", new PrtgThresshold(0.5, null, null, null));
		sensor.addChannel(channel);

		exchange.response.send(sensor, 200);
	}
}

//
