package es.hefame.kardexapi.datastructure;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;

import es.hefame.hcore.JsonEncodable;
import es.hefame.hcore.HException;
import es.hefame.hcore.oracle.DBConnection;
import es.hefame.hcore.oracle.OracleException;

public class Stock implements JsonEncodable {
	private static Logger L = LogManager.getLogger();
	public static int fetch_size = 50;
	private List<StockLine> lines;

	public Stock() throws HException {
		L.debug("Creating Full Stock");
		lines = new LinkedList<StockLine>();

		PreparedStatement preparedStatement = null;
		ResultSet rs = null;

		try {
			Connection dbConnection = DBConnection.get();

			// String selectSQL = "SELECT L.MENGE AS CANTIDAD, L.ARTIKEL AS CN, L.CHARGENNR
			// AS LOTE, L.HALTBARDATUM AS CADUCIDAD, A.EAN AS EAN, A.BESCHREIBUNG1 AS
			// DESCRIPCION FROM KARDEX_HEFAME.LO_ZU_ART L, KARDEX_HEFAME.ARTIKEL A WHERE
			// A.ARTIKEL_ID = L.ARTIKEL_ID";
			String selectSQL = "SELECT L.MENGE AS CANTIDAD, L.ARTIKEL AS CN, L.CHARGENNR AS LOTE, L.HALTBARDATUM AS CADUCIDAD, A.EAN AS EAN, A.BESCHREIBUNG1 AS DESCRIPCION, (SELECT PLATZGR FROM KARDEX_HEFAME.PLATZGR WHERE PLATZGR_ID = L.PLATZGR_ID) AS MODELO_CAJA, (SELECT X FROM KARDEX_HEFAME.PLATZGR WHERE PLATZGR_ID = L.PLATZGR_ID) AS MODELO_CAJA_X, (SELECT Y FROM KARDEX_HEFAME.PLATZGR WHERE PLATZGR_ID = L.PLATZGR_ID) AS MODELO_CAJA_Y, (SELECT REGAL FROM KARDEX_HEFAME.LAGERORT WHERE LAGERORT_ID = L.LAGERORT_ID) AS ESTANTERIA, (SELECT FACHBODEN FROM KARDEX_HEFAME.LAGERORT WHERE LAGERORT_ID = L.LAGERORT_ID) AS BANDEJA, (SELECT POS_NAME FROM KARDEX_HEFAME.LAGERORT WHERE LAGERORT_ID = L.LAGERORT_ID) AS POSICION, L.LAGERORT_ID AS UBICACION FROM KARDEX_HEFAME.LO_ZU_ART L, KARDEX_HEFAME.ARTIKEL A WHERE A.ARTIKEL_ID = L.ARTIKEL_ID";

			L.debug("Preparing query [{}]", selectSQL);
			preparedStatement = dbConnection.prepareStatement(selectSQL);

			rs = preparedStatement.executeQuery();
			rs.setFetchSize(Stock.fetch_size);

			L.debug("Parsing results");
			while (rs.next()) {
				try {
					String cn = rs.getString("CN"); // L.ARTIKEL
					String ean = rs.getString("EAN"); // A.EAN
					String name = rs.getString("DESCRIPCION"); // L.BESCHREIBUNG1
					long stock = Math.round(rs.getFloat("CANTIDAD")); // L.MENGE
					Date expireDateTmp = rs.getDate("CADUCIDAD");
					String expireDate = (expireDateTmp == null ? "" : expireDateTmp.toString()); // L.HALTBARDATUM
					String lot = rs.getString("LOTE"); // L.CHARGENNR

					String modeloCaja = rs.getString("MODELO_CAJA");
					int modeloCajaX = rs.getInt("MODELO_CAJA_X");
					int modeloCajaY = rs.getInt("MODELO_CAJA_Y");

					int idUbicacion = rs.getInt("UBICACION");
					String estanteria = rs.getString("ESTANTERIA");
					String bandeja = rs.getString("BANDEJA");
					String posicion = rs.getString("POSICION");

					this.lines.add(new StockLine(cn, ean, name, stock, expireDate, lot, modeloCaja, modeloCajaX,
							modeloCajaY, idUbicacion, estanteria, bandeja, posicion));
				} catch (SQLException e) {
					L.catching(e);
				}
			}
			L.info("Retrieved [{}] Stock Line", () -> lines.size());

			Stock.fetch_size = Math.min(2000, lines.size());

		} catch (SQLException e) {
			throw L.throwing(new OracleException(e));
		} finally {
			DBConnection.clearResources(preparedStatement, rs);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONArray jsonEncode() {
		JSONArray array = new JSONArray();

		for (StockLine line : this.lines) {
			array.add(line.jsonEncode());
		}

		return array;
	}
}
