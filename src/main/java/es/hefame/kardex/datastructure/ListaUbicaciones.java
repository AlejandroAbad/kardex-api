package es.hefame.kardex.datastructure;

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

public class ListaUbicaciones implements JsonEncodable {
	private static Logger L = LogManager.getLogger();
	public static int fetch_size = 50;
	private List<Ubicacion> ubicaciones;

	public ListaUbicaciones() throws HException {
		L.debug("Consultando ubicaciones");
		ubicaciones = new LinkedList<Ubicacion>();

		PreparedStatement preparedStatement = null;
		ResultSet rs = null;

		try {
			Connection dbConnection = DBConnection.get();

			//
			String selectSQL = "SELECT L.LAGERORT_ID AS ID_UBICACION, L.REGAL AS ESTANTERIA, L.FACHBODEN AS BANDEJA, L.POS_NAME AS POSICION, L.XOFFSET AS OFFSET_UBICACION, L.PLATZGR AS MODELO_UBICACION, L.XSIZE AS ANCHO_UBICACION, L.YSIZE AS ALTO_UBICACION, L.ZONEN AS TIPO_UBICACION, (SELECT ARTIKEL FROM KARDEX_HEFAME.LO_ZU_ART WHERE LAGERORT_ID = L.LAGERORT_ID AND ARTIKEL_ID = L.ARTIKEL_ID ) AS CODIGO_NACIONAL, (SELECT BESCHREIBUNG1 FROM KARDEX_HEFAME.ARTIKEL WHERE ARTIKEL_ID = L.ARTIKEL_ID) AS DESCRIPCION, (SELECT CHARGENNR FROM KARDEX_HEFAME.LO_ZU_ART WHERE LAGERORT_ID = L.LAGERORT_ID AND ARTIKEL_ID = L.ARTIKEL_ID) AS LOTE, (SELECT HALTBARDATUM FROM KARDEX_HEFAME.LO_ZU_ART WHERE LAGERORT_ID = L.LAGERORT_ID AND ARTIKEL_ID = L.ARTIKEL_ID) AS FECHA_CADUCIDAD, (SELECT MENGE FROM KARDEX_HEFAME.LO_ZU_ART WHERE LAGERORT_ID = L.LAGERORT_ID AND ARTIKEL_ID = L.ARTIKEL_ID) AS CANTIDAD, (SELECT PLATZGR FROM KARDEX_HEFAME.PLATZGR WHERE PLATZGR_ID =  (SELECT PLATZGR_ID FROM KARDEX_HEFAME.ARTIKEL WHERE ARTIKEL_ID = L.ARTIKEL_ID) ) AS MODELO_CAJA, (SELECT X FROM KARDEX_HEFAME.PLATZGR WHERE PLATZGR_ID =  (SELECT PLATZGR_ID FROM KARDEX_HEFAME.ARTIKEL WHERE ARTIKEL_ID = L.ARTIKEL_ID) ) AS ANCHO_CAJA, (SELECT Y FROM KARDEX_HEFAME.PLATZGR WHERE PLATZGR_ID =  (SELECT PLATZGR_ID FROM KARDEX_HEFAME.ARTIKEL WHERE ARTIKEL_ID = L.ARTIKEL_ID) ) AS ALTO_CAJA FROM KARDEX_HEFAME.LAGERORT L ORDER BY ESTANTERIA, BANDEJA, POSICION";

			L.debug("Preparando consulta [{}]", selectSQL);
			preparedStatement = dbConnection.prepareStatement(selectSQL);

			rs = preparedStatement.executeQuery();
			rs.setFetchSize(ListaUbicaciones.fetch_size);

			L.debug("Analizando resultados ...");
			while (rs.next()) {
				try {
					int id = Math.round(rs.getFloat("ID_UBICACION"));
					String estante = rs.getString("ESTANTERIA");
					String bandeja = rs.getString("BANDEJA");
					String posicion = rs.getString("POSICION");
					int offset = Math.round(rs.getFloat("OFFSET_UBICACION"));
					String modeloUbicacion = rs.getString("MODELO_UBICACION");
					int modeloUbicacionX = Math.round(rs.getFloat("ANCHO_UBICACION"));
					int modeloUbicacionY = Math.round(rs.getFloat("ALTO_UBICACION"));
					String tipoUbicacion = rs.getString("TIPO_UBICACION");
					String artiCn = rs.getString("CODIGO_NACIONAL");
					String artiDescripcion = rs.getString("DESCRIPCION");
					String artiLote = rs.getString("LOTE");
					Date artiCaducidadTmp = rs.getDate("FECHA_CADUCIDAD");
					String artiCaducidad = (artiCaducidadTmp == null ? "" : artiCaducidadTmp.toString());
					int artiCantidad = Math.round(rs.getFloat("CANTIDAD"));
					String artiModeloCaja = rs.getString("MODELO_CAJA");
					int artiModeloCajaX = Math.round(rs.getFloat("ANCHO_CAJA"));
					int artiModeloCajaY = Math.round(rs.getFloat("ALTO_CAJA"));

					this.ubicaciones.add(new Ubicacion(id, estante, bandeja, posicion, offset, modeloUbicacion,
							modeloUbicacionX, modeloUbicacionY, tipoUbicacion, artiCn, artiDescripcion, artiLote,
							artiCaducidad, artiCantidad, artiModeloCaja, artiModeloCajaX, artiModeloCajaY));
					
				} catch (SQLException e) {
					L.catching(e);
				}
			}
			L.info("Recuperadas {} ubicaciones", () -> ubicaciones.size());

			ListaUbicaciones.fetch_size = Math.min(2000, ubicaciones.size());

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

		for (Ubicacion line : this.ubicaciones) {
			array.add(line.jsonEncode());
		}

		return array;
	}
}
