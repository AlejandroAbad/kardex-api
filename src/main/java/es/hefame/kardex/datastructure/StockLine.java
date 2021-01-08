package es.hefame.kardex.datastructure;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import es.hefame.hcore.JsonEncodable;
import es.hefame.hcore.HException;
import es.hefame.hcore.http.HttpException;
import es.hefame.hcore.oracle.DBConnection;
import es.hefame.hcore.oracle.OracleException;

public class StockLine implements JsonEncodable
{
	private static Logger	L	= LogManager.getLogger();
	private String			cn;
	private String			ean;
	private String			name;
	private long			stock;
	private String			expireDate;
	private String			lot;
	
	private String			modeloCaja;
	private int				modeloCajaX;
	private int				modeloCajaY;
	
	private int				idUbicacion;
	private String			estanteria;
	private String			bandeja;
	private String			posicion;

	public StockLine(String cn, String ean, String name, long stock, String expireDate, String lot, 
			String modeloCaja, int modeloCajaX, int modeloCajaY, int idUbicacion, String estanteria, String bandeja, String posicion)
	{
		this.cn = cn;
		this.ean = ean;
		this.name = name;
		this.stock = stock;
		this.expireDate = expireDate;
		this.lot = lot;
		
		this.modeloCaja = modeloCaja;
		this.modeloCajaX = modeloCajaX;
		this.modeloCajaY = modeloCajaY;
		
		this.idUbicacion = idUbicacion;
		this.estanteria = estanteria;
		this.bandeja = bandeja;
		this.posicion = posicion;
		
		
	}

	public StockLine(String cn) throws HException
	{
		L.debug("Creating StockLine for article with CN [{}]", cn);

		PreparedStatement preparedStatement = null;
		ResultSet rs = null;

		try
		{
			Connection dbConnection = DBConnection.get();

			String selectSQL = "SELECT L.MENGE AS CANTIDAD, L.ARTIKEL AS CN, L.CHARGENNR AS LOTE, L.HALTBARDATUM AS CADUCIDAD, A.EAN AS EAN, A.BESCHREIBUNG1 AS DESCRIPCION, (SELECT PLATZGR FROM KARDEX_HEFAME.PLATZGR WHERE PLATZGR_ID = L.PLATZGR_ID) AS MODELO_CAJA, (SELECT X FROM KARDEX_HEFAME.PLATZGR WHERE PLATZGR_ID = L.PLATZGR_ID) AS MODELO_CAJA_X, (SELECT Y FROM KARDEX_HEFAME.PLATZGR WHERE PLATZGR_ID = L.PLATZGR_ID) AS MODELO_CAJA_Y, (SELECT REGAL FROM KARDEX_HEFAME.LAGERORT WHERE LAGERORT_ID = L.LAGERORT_ID) AS ESTANTERIA, (SELECT FACHBODEN FROM KARDEX_HEFAME.LAGERORT WHERE LAGERORT_ID = L.LAGERORT_ID) AS BANDEJA, (SELECT POS_NAME FROM KARDEX_HEFAME.LAGERORT WHERE LAGERORT_ID = L.LAGERORT_ID) AS POSICION, L.LAGERORT_ID AS UBICACION FROM KARDEX_HEFAME.LO_ZU_ART L, KARDEX_HEFAME.ARTIKEL A WHERE A.ARTIKEL_ID = L.ARTIKEL_ID AND L.ARTIKEL = ?";

			L.debug("Preparing query [{}]", selectSQL);
			preparedStatement = dbConnection.prepareStatement(selectSQL);

			L.debug("Setting query param {} to {}", 1, cn);
			preparedStatement.setString(1, cn);

			rs = preparedStatement.executeQuery();

			L.debug("Parsing results");
			if (rs.next())
			{
				this.cn = rs.getString("CN"); // L.ARTIKEL
				this.ean = rs.getString("EAN"); // A.EAN
				this.name = rs.getString("DESCRIPCION"); // L.BESCHREIBUNG1
				this.stock = Math.round(rs.getFloat("CANTIDAD")); // L.MENGE
				Date expireDateTmp = rs.getDate("CADUCIDAD");
				this.expireDate = (expireDateTmp == null ? "" : expireDateTmp.toString()); // L.HALTBARDATUM
				this.lot = rs.getString("LOTE"); // L.CHARGENNR
				
				this.modeloCaja = rs.getString("MODELO_CAJA");
				this.modeloCajaX = rs.getInt("MODELO_CAJA_X");
				this.modeloCajaY = rs.getInt("MODELO_CAJA_Y");
				
				this.idUbicacion = rs.getInt("UBICACION");
				this.estanteria = rs.getString("ESTANTERIA");
				this.bandeja = rs.getString("BANDEJA");
				this.posicion = rs.getString("POSICION");

				L.info("Retrieved Stock Line [{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}]", cn, ean, name, stock, expireDate, lot, modeloCaja, modeloCajaX, modeloCajaY, idUbicacion, estanteria, bandeja, posicion);
			}
			else
			{
				throw L.throwing(new HttpException(404, "Article with CN = " + cn + " not found."));
			}

		}
		catch (SQLException e)
		{
			throw L.throwing(new OracleException(e));
		}
		finally
		{
			DBConnection.clearResources(preparedStatement, rs);
		}
	}

	public String getCN()
	{
		return cn;
	}

	public String getEan()
	{
		return ean;
	}

	public String getName()
	{
		return name;
	}

	public float getStock()
	{
		return stock;
	}
	
	
	public String getExpireDate() {
		return expireDate;
	}

	public String getLot() {
		return lot;
	}
	
	
	

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject jsonEncode()
	{
		JSONObject root = new JSONObject();

		root.put("id", this.cn);
		if (this.ean != null && this.ean.length() > 0) root.put("ean", this.ean);
		root.put("name", this.name);
		root.put("stock", this.stock);
		if (this.expireDate != null && this.expireDate.length() > 0) root.put("expireDate", this.expireDate);
		if (this.lot != null && this.lot.length() > 0) root.put("lot", this.lot);
		
		JSONObject caja = new JSONObject();
		caja.put("modelo", this.modeloCaja);
		caja.put("ancho", this.modeloCajaX);
		caja.put("profundidad", this.modeloCajaY);
		root.put("caja", caja);
		
		JSONObject ubicacion = new JSONObject();
		ubicacion.put("id", this.idUbicacion);
		ubicacion.put("estanteria", this.estanteria);
		ubicacion.put("bandeja", this.bandeja);
		ubicacion.put("posicion", this.posicion);
		root.put("ubicacion", ubicacion);
		
		

		return root;
	}

}
