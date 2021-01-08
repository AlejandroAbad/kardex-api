package es.hefame.kardexapi.httpcontroller;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import es.hefame.hcore.JsonEncodable;
import es.hefame.hcore.HException;
import es.hefame.hcore.http.HttpController;
import es.hefame.hcore.http.exchange.HttpConnection;
import es.hefame.kardexapi.datastructure.Stock;
import es.hefame.kardexapi.datastructure.StockLine;

public class StockHandler extends HttpController
{
	private static Logger L = LogManager.getLogger();

	@Override
	public void get(HttpConnection exchange) throws IOException, HException
	{
		L.info("Requested Kardex Stock from address [{}]", () -> exchange.request.getIP());

		String article_id = exchange.request.getURIField(1);

		JsonEncodable response_body = null;

		if (article_id != null && article_id.length() > 0)
		{
			L.debug("Requested article code [{}]", article_id);
			response_body = new StockLine(article_id);
		}
		else
		{
			L.debug("No article code found. Retrieving full stock list.");
			response_body = new Stock();
		}

		exchange.response.addHeader("Access-Control-Allow-Origin", "*");
		exchange.response.addHeader("Access-Control-Allow-Headers", "*");
		exchange.response.addHeader("Access-Control-Allow-Methods", "GET");
		
		exchange.response.send(response_body, 200);

	}

}
