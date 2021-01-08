package es.hefame.kardex;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.hefame.hcore.http.HttpController;
import es.hefame.hcore.http.server.HttpService;
import es.hefame.hcore.http.server.HttpsService;
import es.hefame.kardex.configuration.Configuration;
import es.hefame.kardex.httpcontroller.StockHandler;
import es.hefame.kardex.httpcontroller.UbicacionesHandler;
import es.hefame.kardex.httpcontroller.prtg.PrtgJVMHandler;
import es.hefame.kardex.httpcontroller.prtg.PrtgOracleHandler;
import es.hefame.hcore.oracle.DBConnection;

public class ApiKardex
{
	private static Logger L = LogManager.getLogger();

	public static void main(String... args)
	{
		L.traceEntry("{}", (Object[]) args);
		L.info("Inciando servidor API de Kardex");

		Configuration.load();

		try
		{

			String ora_tns = Configuration.instance.get_oracle_tns();
			String ora_user = Configuration.instance.get_oracle_user();
			String ora_pass = Configuration.instance.get_oracle_pass();
			DBConnection.setConnectionParameters(ora_tns, ora_user, ora_pass);

			int port = Configuration.instance.get_http_port();
			int max_connections = Configuration.instance.get_http_max_connections();
			String keystore = Configuration.instance.get_ssl_keystore();
			char[] password = Configuration.instance.get_ssl_passphrase();

			Map<String, HttpController> routes = new HashMap<String, HttpController>();
			routes.put("/stock", new StockHandler());
			routes.put("/ubicaciones", new UbicacionesHandler());
			routes.put("/prtg/jvm", new PrtgJVMHandler());
			routes.put("/prtg/oracle", new PrtgOracleHandler());

			HttpService server = new HttpsService(port, max_connections, keystore, password, routes);

			ShutdownHook shutdown_hook = new ShutdownHook(server);
			L.trace("Setting up ShutdownHook [{}]", shutdown_hook.getClass().getName());
			Runtime.getRuntime().addShutdownHook(new ShutdownHook(server));

			server.start();
		}
		catch (Exception e)
		{
			L.catching(e);
			L.fatal("Aborting execution with exit code {}", 2);
			System.exit(2);
		}

	}

}
