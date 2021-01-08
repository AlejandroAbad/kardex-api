package es.hefame.kardexapi.configuration;

import java.io.FileReader;
import java.io.Reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Configuration
{
	private static Logger		L			= LogManager.getLogger();

	public static Configuration	instance;
	private static String		config_file_path;
	private JSONObject			json_root	= null;

	public static void load()
	{
		L.traceEntry();

		if (System.getProperty("config") != null)
		{
			Configuration.config_file_path = System.getProperty("config");
			L.debug("Configuration file [{}] obtained from System Properties", Configuration.config_file_path);
		}
		else
		{
			Configuration.config_file_path = "apikardex.conf";
			L.debug("Using default configuration file [{}]", Configuration.config_file_path);
		}

		L.info("Loading configuration from file [{}]", Configuration.config_file_path);
		Configuration.instance = new Configuration();
		L.info("Finished reading configuration");

		L.debug("Loaded configuration: {}", instance);
		L.traceExit();
	}

	private Configuration()
	{
		L.traceEntry();

		this.json_root = new JSONObject();
		try
		{
			Reader fileReader = new FileReader(config_file_path);
			this.json_root = (JSONObject) JSONValue.parseWithException(fileReader);
		}
		catch (Exception e)
		{
			L.catching(e);
		}

		if (this.json_root == null)
		{
			this.json_root = new JSONObject();
			L.warn("Configuration not imported due to error. Using delfault configuration parameters.");
		}

		L.traceExit();
	}

	public int get_http_port()
	{
		L.traceEntry();
		Object o = this.json_root.get("http_port");
		if (o == null)
		{
			L.info("Unable to find parameter 'http_port'");
		}
		else
		{
			try
			{
				return L.traceExit(Integer.parseInt(o.toString()));
			}
			catch (NumberFormatException nfe)
			{
				L.error("Cannot convert 'http_port' [{}] to integer: {}", (Supplier<String>) () -> o.toString(), nfe.getMessage());
			}
		}

		L.debug("Returning default value [ 8123 ]");
		return L.traceExit("Default value [{}]", 8123);

	}

	public int get_http_max_connections()
	{
		L.traceEntry();

		Object o = this.json_root.get("http_max_connections");
		if (o == null)
		{
			L.info("Unable to find parameter 'http_max_connections'");
		}
		else
		{
			try
			{
				return L.traceExit(Integer.parseInt(o.toString()));
			}
			catch (NumberFormatException nfe)
			{
				L.info("Cannot convert 'http_max_connections' to integer: {}", nfe.getMessage());
			}
		}

		return L.traceExit("Default value [{}]", 3);
	}

	public String get_oracle_tns()
	{
		L.traceEntry();

		Object o = this.json_root.get("oracle_tns");
		if (o == null)
		{
			L.info("Unable to find parameter 'oracle_tns'");
		}
		else
		{
			return L.traceExit(o.toString());
		}

		String default_tns = "(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=KARDEX)))";
		return L.traceExit("Default value [{}]", default_tns);
	}

	public String get_oracle_user()
	{
		L.traceEntry();

		Object o = this.json_root.get("oracle_user");
		if (o == null)
		{
			L.info("Unable to find parameter 'oracle_user'");
		}
		else
		{
			return L.traceExit(o.toString());
		}

		L.info("Returning default value [ apikardex ]");
		return L.traceExit("Default value [{}]", "apikardex");
	}

	public String get_oracle_pass()
	{
		L.traceEntry();

		Object o = this.json_root.get("oracle_pass");
		if (o == null)
		{
			L.info("Unable to find parameter 'oracle_pass'");
		}
		else
		{
			return L.traceExit("<hidden>", o.toString());
		}

		return L.traceExit("Returning default value [{}]", "");
	}

	public String get_ssl_keystore()
	{
		L.traceEntry();

		Object o = this.json_root.get("ssl_keystore");
		if (o == null)
		{
			L.info("Unable to find parameter 'ssl_keystore'");
		}
		else
		{
			return L.traceExit(o.toString());
		}

		return L.traceExit("Returning default value [{}]", "apikardex.jks");
	}

	public char[] get_ssl_passphrase()
	{
		L.traceEntry();

		Object o = this.json_root.get("ssl_passphrase");
		if (o == null)
		{
			L.info("Unable to find parameter 'ssl_passphrase'");
		}
		else
		{
			return L.traceExit(o.toString().toCharArray());
		}

		return L.traceExit("Returning default value [{}]", new char[0]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public String toString()
	{
		JSONObject root = new JSONObject();
		root.put("http_port", this.get_http_port());
		root.put("http_max_connections", this.get_http_max_connections());

		root.put("ssl_keystore", this.get_ssl_keystore());
		root.put("ssl_pass", "<hidden>");

		root.put("oracle_tns", this.get_oracle_tns());
		root.put("oracle_user", this.get_oracle_user());
		root.put("oracle_pass", "<hidden>");

		return root.toJSONString();
	}

}
