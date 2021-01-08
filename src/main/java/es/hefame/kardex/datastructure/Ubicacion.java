package es.hefame.kardex.datastructure;

import org.json.simple.JSONObject;

import es.hefame.hcore.JsonEncodable;

public class Ubicacion implements JsonEncodable {

	private int id;
	private String estante;
	private String bandeja;
	private String posicion;
	private int offset;

	private String modeloUbicacion;
	private int modeloUbicacionX;
	private int modeloUbicacionY;
	private String tipoUbicacion;

	private String artiCn;
	private String artiDescripcion;
	private String artiLote;
	private String artiCaducidad;
	private int artiCantidad;

	private String artiModeloCaja;
	private int artiModeloCajaX;
	private int artiModeloCajaY;

	public Ubicacion(int id, String estante, String bandeja, String posicion, int offset, String modeloUbicacion,
			int modeloUbicacionX, int modeloUbicacionY, String tipoUbicacion, String artiCn, String artiDescripcion,
			String artiLote, String artiCaducidad, int artiCantidad, String artiModeloCaja, int artiModeloCajaX,
			int artiModeloCajaY) {
		super();
		this.id = id;
		this.estante = estante;
		this.bandeja = bandeja;
		this.posicion = posicion;
		this.offset = offset;
		this.modeloUbicacion = modeloUbicacion;
		this.modeloUbicacionX = modeloUbicacionX;
		this.modeloUbicacionY = modeloUbicacionY;
		this.tipoUbicacion = tipoUbicacion.substring(1, tipoUbicacion.length() - 1);
		this.artiCn = artiCn;
		this.artiDescripcion = artiDescripcion;
		this.artiLote = artiLote;
		this.artiCaducidad = artiCaducidad;
		this.artiCantidad = artiCantidad;
		this.artiModeloCaja = artiModeloCaja;
		this.artiModeloCajaX = artiModeloCajaX;
		this.artiModeloCajaY = artiModeloCajaY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject jsonEncode() {
		JSONObject root = new JSONObject();

		root.put("id", this.id);
		root.put("estante", this.estante);
		root.put("bandeja", this.bandeja);
		root.put("posicion", this.posicion);
		root.put("offset", this.offset);
		root.put("tipo", this.tipoUbicacion);

		JSONObject modeloUbicacion = new JSONObject();
		modeloUbicacion.put("modelo", this.modeloUbicacion);
		modeloUbicacion.put("ancho", this.modeloUbicacionX);
		modeloUbicacion.put("profundidad", this.modeloUbicacionY);
		root.put("formato", modeloUbicacion);

		if (this.artiCn != null  && this.artiCn.length() > 0) {
			JSONObject articulo = new JSONObject();
			articulo.put("cn", this.artiCn);
			articulo.put("name", this.artiDescripcion);
			articulo.put("stock", this.artiCantidad);
			if (this.artiCaducidad != null && this.artiCaducidad.length() > 0)
				articulo.put("caducidad", this.artiCaducidad);
			
			if (this.artiLote != null && this.artiLote.length() > 0)
				articulo.put("lote", this.artiLote);

			JSONObject cajaArticulo = new JSONObject();
			cajaArticulo.put("modelo", this.artiModeloCaja);
			cajaArticulo.put("ancho", this.artiModeloCajaX);
			cajaArticulo.put("profundidad", this.artiModeloCajaY);
			articulo.put("caja", cajaArticulo);

			root.put("articulo", articulo);
		}

		return root;
	}

}
