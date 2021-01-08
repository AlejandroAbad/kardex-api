package es.hefame.kardex.httpcontroller;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.hefame.hcore.JsonEncodable;
import es.hefame.hcore.HException;
import es.hefame.hcore.http.HttpController;
import es.hefame.hcore.http.exchange.HttpConnection;
import es.hefame.kardex.datastructure.ListaUbicaciones;

public class UbicacionesHandler extends HttpController {
	private static Logger L = LogManager.getLogger();

	@Override
	public void get(HttpConnection exchange) throws IOException, HException {
		L.info("Solicitada lista de ubicaciones desde la IP {}", () -> exchange.request.getIP());

		JsonEncodable response_body = null;

		
		response_body = new ListaUbicaciones();
		

		exchange.response.addHeader("Access-Control-Allow-Origin", "*");
		exchange.response.addHeader("Access-Control-Allow-Headers", "*");
		exchange.response.addHeader("Access-Control-Allow-Methods", "GET");

		exchange.response.send(response_body, 200);

	}

}
