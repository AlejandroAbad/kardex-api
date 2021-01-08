# Introducción
El propósito de este documento es especificar y definir los distintos recursos disponibles a través del API de acceso a la información del Kardex. Este protocolo está basado en los principios de una API REST, por lo que también se realizará una explicación de estos principios.

## ¿Qué es una API Rest?
REST, REpresentational State Transfer, es un tipo de arquitectura de desarrollo software que se apoya totalmente en el estándar Cliente-Servidor HTTP. Es importante indicar que la arquitectura REST no es un estándar ni un protocolo, sino una serie de principios de arquitectura. REST nos permite crear servicios y aplicaciones que pueden ser usadas por cualquier dispositivo o cliente que entienda HTTP, por lo que es increíblemente más simple y convencional que otras alternativas que se han usado en los últimos diez años como SOAP y XML-RPC. Por lo tanto REST es el tipo de arquitectura más natural y estándar para crear APIs para servicios orientados a Internet.


# Funcionamiento básico de la API

El servidor de acceso a la API se encuentra publicado en la siguiente URL por defecto:
```
https://orackardex-scan.hefame.es
```

El API es de tipo REST y va protegido por TLS, es decir, para realizar consultas hay que hacer peticiones HTTPS.
Todos los mensajes intercambiados con el servidor estarán condificados en formato JSON.


##  Obtener stock de artículos en Kardex
### Todo el stock
`GET /stock`

Obtiene una lista (Array JSON) con todos los artículos existentes en el momento de la consulta, donde cada artículo es un Objeto JSON con los siguientes campos:

- `id` Código del artículo
- `ean` Código EAN (Opcional. De no especificarse se interpretará como NULL)
- `name` Nombre del artículo (Opcional. De no especificarse se interpretará como NULL)
- `stock` Cantidad en stock

Un ejemplo de respuesta sería:
```json
[
	{
		"id":"007483",
		"name":"BOVILIS BVD 50ML 25 DOSIS",
		"stock":0
	},
	{
		"id":"751024",
		"ean":"0983420398423",
		"name":"CHIROFLU  VACUNA  ANTIGRIPAL",
		"stock":15
	},
	(...)
]
```

### El stock de un artícluo en concreto

`GET /stock/<cod_articulo>`

Devuelve la información del artículo solicitado en un Objeto JSON con los siguientes campos:

- `id` Código del artículo
- `ean` Código EAN (Opcional. De no especificarse se interpretará como NULL)
- `name` Nombre del artículo (Opcional. De no especificarse se interpretará como NULL)
- `stock` Cantidad en stock

Por ejemplo, para la petición  `GET /stock/751024`, la respuesta sería:
```json
{
	"id":"751024",
	"ean":"0983420398423",
	"name":"CHIROFLU  VACUNA  ANTIGRIPAL",
	"stock":15
}
```

### Ejemplo de llamada desde ABAP
A continuación veremos un pequeño programa ABAP de ejemplo (cortesía del compañero Dani), donde llamaremos al servicio y obtendremos una tabla con los resultados del stock:
```abap
* *************************************
* Paso 1. Llamada al servicio web
* *************************************

DATA: L_HTTP_URL TYPE STRING , * La URL del web service que vamos a llamar
      L_HTTP_CLIENT TYPE REF TO IF_HTTP_CLIENT,
      CONTENT TYPE STRING. * Un String donde se almacenará la respuesta obtenida

* Construimos la URL a la que vamos a consultar, en función de que datos
* que queremos obtener, como hemos visto anteriormente:
L_HTTP_URL = 'https://orackardex-scan.hefame.es:8123/stock'.


* Creamos el objeto de la clase CL_HTTP_CLIENT que nos permite 
* hacer la llamada al web service.
CALL METHOD CL_HTTP_CLIENT=>CREATE_BY_URL
  EXPORTING
    URL                = L_HTTP_URL
    SSL_ID             = 'HEFAME'
  IMPORTING
    CLIENT             = L_HTTP_CLIENT
  EXCEPTIONS
    ARGUMENT_NOT_FOUND = 1
    PLUGIN_NOT_ACTIVE  = 2
    INTERNAL_ERROR     = 3
    OTHERS             = 4.


* Indicamos que el método de llamada es "GET".
L_HTTP_CLIENT->REQUEST->SET_HEADER_FIELD( NAME  = '~REQUEST_METHOD'
                                          VALUE = 'GET' ).

* Enviamos la petición !!
L_HTTP_CLIENT->SEND( ).
CALL METHOD L_HTTP_CLIENT->RECEIVE
  EXCEPTIONS
    HTTP_COMMUNICATION_FAILURE = 1
    HTTP_INVALID_STATE         = 2
    HTTP_PROCESSING_FAILED     = 3
    OTHERS                     = 4.


* Obtenemos la respuesta de la petición.
* Esta respuesta contiene la cadena JSON con toda la información solicitada.
CONTENT = L_HTTP_CLIENT->RESPONSE->GET_CDATA( ).

* Cerramos la conexión (esto ahorra recursos)
CALL METHOD L_HTTP_CLIENT->CLOSE
   EXCEPTIONS
     HTTP_INVALID_STATE         = 1.


* *************************************
* Paso 2. Construimos una tabla ABAP con los datos
* *************************************

* Genial, ya tenemos en la variable CONTENT un churro infumable
* que es el JSON que el web service nos ha devuelto.
* Ahora definimos la estructura de la tabla. Donde el orden de los campos 
* podremos utilizar el que mas nos convenga, pues en JSON el orden es 
* irrelevante.
* También es interesante saber que podemos quedarnos con los campos que
* queramos, por ejemplo, descartaré el campo "ean" para
* este ejemplo.
TYPES: BEGIN OF ity_str,
       id TYPE string,
       name TYPE string,
       stock TYPE i
      END OF ity_str.

DATA: it_str TYPE STANDARD TABLE OF ity_str.

* Y dejamos que SAP haga su magia
* (Nota: la clase ZCL_JSON es una copia de CL_JSON donde se arregla un bug
* de la versión SAP que tenemos. En versiones posteriores de SAP el bug ya
* está solucionado)
ZCL_JSON=>DESERIALIZE( EXPORTING JSON = CONTENT
                       CHANGING DATA = it_str).

* Et voilà :
DATA: wa_str TYPE ity_str .

LOOP AT it_str INTO wa_str.
  WRITE: wa_str-id.
  WRITE: wa_str-name.
  WRITE: wa_str-stock.
  WRITE:/.
ENDLOOP.
```

## Tratamiento de errores
En cualquier tipo de API es necesario que la aplicación cliente detecte si las peticiones u operaciones que ha realizado han finalizado de forma correcta o por el contrario se ha producido algún tipo de error. El protocolo HTTP proporciona una lista de códigos de estado para especificar el resultado de la operación. Los códigos utilizados son los siguientes:

- `200 OK` Indica que la operación resultó en éxito.
- `400 Bad Request` - *Petición errónea*. La petición está malformada, por ejemplo, el campo de artículo está mal construido.
- `404 Not Found` - *No encontrado*. Cuando un recurso no existente es solicitado.
- `500 Internal Server Error` - *Error interno del servidor*. Ha ocurrido un error inesperado en el servidor, por ejemplo, una excepción en código no controlada, o no hay conexión a la base de datos.

En caso de error, el servidor devolverá un mensaje en formato JSON con al menos los siguientes campos:

- `message` - Un mensaje descriptivo del error
- `code` - Un código de referencia del error, para poder depurarlo en caso de ser necesario.

A continuación se muestran algunos ejemplos de errores comunes:

**Ejemplo 1.** Se busca un artículo que no existe
URL: `https://orackardex-scan.hefame.es:8123/stock/88888`
Código respuesta HTTP: `404 Not Found`
Mensaje JSON:
```json
{
  "code": "436865636B696E48616E646C6572-147-1",
  "message": "No se encuentra el articulo",
  "httpcode": 404
}
```
**Ejemplo 2.** El artículo a buscar está vacío
URL: `https://orackardex-scan.hefame.es:8123/stock//`
Código respuesta HTTP: `400 Bad Request`
Mensaje JSON:
```json
{
  "code": "48747470436F6E74726F6C6C6572-30-3",
  "message": "Debe especificarse el articulo",
  "httpcode": 400
}
```

**Ejemplo 3. Error de la base de datos**
Código respuesta HTTP: `500 Internal Server Error`
Mensaje JSON:
```json
{
  "code": "436865636B696E536574-76-1",
  "oracle_code": 12541,
  "message": "ORA-12541 TNS:No Listener Error",
  "httpcode": 500
}
```



## Administración del servidor
El servidor de API corre en los dos servidores que albergan la base de datos de Kardex, que corren en un clúster Oracle RAC. Puesto que para acceder a este servicio, accedemos a la dirección `orackardex-scan.hefame.es`, que en realidad es una dirección flotante, tenemos alta disponibilidad de la misma gracias al propio clúster de la base de datos.

### Ficheros
Por defecto, todos los ficheros necesarios se encuentran en el directorio `/usr/local/bin/apikardex` de ambas máquinas del RAC de Kardex. Dentro de este directorio encontramos los siguientes ficheros:

| Fichero | Permisos | Descripción |
| -- | -- | -- |
| log4j2.xml | `-rw-r--r--` | Configuración de los logs |
| hefame.jks | `-rw-------` | Keystore con certificados para HTTPS |
| apikardex.conf | `-rw-------` | Fichero de configuración |
| apikardex.jar | `-rwx------` | Binario de la aplicación |
| apikardex | `-rwx------` | Script de arranque/parada del servicio |

### Arranque y parada

Existe un script de arranque/parada llamado `apikardex` que puede usarse para arrancar, parar o ver el estado del servicio:
```shell
# apikardex
Usage: apikardex {start|stop|restart|status}

# apikardex start
Starting KARDEX API:                                       [  OK  ]

# apikardex stop
Stopping KARDEX API:                                       [  OK  ]

# apikardex status
KARDEX API is not running

# apikardex restart
Stopping KARDEX API:                                       [ WARN ]
KARDEX API was not running
Starting KARDEX API:                                       [  OK  ]

# apikardex status
KARDEX API is running with PID 20161
```

#### Arranque automático al reiniciar el SO

Para que la API se ponga a funcionar tan pronto la máquina sea arrancada, crearemos un link al ejecutable `/usr/local/bin/apikardex/apikardex` en el directorio `/etc/init.d`:

```shell
# ln -s /usr/local/bin/apikardex/apikardex /etc/init.d/apikardex
```

## Configuración del API

### Configuración de la aplicación
El script de arranque/parada tiene algunas variables que pueden ser modificadas (aunque generalmente no tendremos que modificar nada aquí) para especificar los siguientes parámetros:

| Variable | Valor | Descripción |
| -- | -- | -- |
| `JAVA_BIN` | `/usr/bin/java` | Ejecutable de java (podríamos forzar una JVM específica, p.e. `/usr/lib/jvm/jre-1.7.0/bin/java` para JAVA7) |
| `BASE_DIR` | `/usr/local/bin/apikardex` | Directorio base de la aplicación |
| `LOG_DIR` | `/var/log/apikardex` | Directorio donde se almacenan los logs del API |
| `CONFIG_FILE` | `$BASE_DIR/apikardex.conf` | Fichero de configuración del API |
| `JAR_FILE` | `$BASE_DIR/apikardex.jar` | Ruta al JAR de la aplicación |

### Configuración del servicio
El resto de parámetros se especifica en el fichero de configuración `CONFIG_FILE`, que está en formato JSON, como por ejemplo:
```json
{
        "http_port": 443,
        "http_max_connections": 10,
        "ssl_enable": true,
        "ssl_keystore": "/usr/local/bin/apikardex/hefame.jks",
        "ssl_passphrase": "Plata0Plomo",
        "oracle_tns": "(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=orackardex-scan.hefame.es)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=KARDEX)))",
        "oracle_user": "apikardex",
        "oracle_pass": "G0norrea",
}
```

| Parámetro | Valor por defecto  | Descripción |
| -- | -- | -- |
| `http_port` | 443 |  Puerto donde el servidor espera peticiones de la API. |
| `http_max_connections` | 10 | Número máximo de conexiones simultáneas que el servidor puede aceptar. Las excedentes serán rechazadas con `TCP_RESET`. |
| `ssl_enable` | false | Indica si se utilizará un protocolo seguro para establecer las conexiones HTTP. |
| `ssl_keystore` | hefame.jks | El servidor espera encontrar un almacén de claves JKS en el fichero indicado por este parámetro. Más adelante, en la sección 'Tratando con JKS' hablaremos de como administrar este almacén de claves. |
| `ssl_passphrase` | `<cadena vacía>` | La clave de acceso al almacén de claves, en caso de que sea necesario. |
| `oracle_tns`  | (DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=KARDEX))) | Cadena de conexión Oracle TNS para realizar conexiones a la base de datos de Kardex. |
| `oracle_user` | apikardex | Usuario con el que conectar a la base de dato Kardex. |
| `oracle_pass` | `<cadena vacía>` | Clave del usuario de la base de datos |

### Configuración de logs

Los logs que la aplicación deja se configuran en función del fichero `log4j2.xml`, que reside en el directorio base de la aplicación. La aplicación utiliza la librería `log4j2` para dejar log, por lo que este fichero se debe configurar en base a la documentación de dicha librería.

Con el siguiente ejemplo, todos los logs de nivel `info` o superior que genere la aplicación se dejan en el directorio `/var/log/apikardex` y la propia libraría `log4j2` se encarga de rotarlos diariamente, comprimirlos y eliminar los logs anteriores a 60 días.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="warn" monitorInterval="30">
  <Properties>
    <Property name="baseDir">/var/log/apikardex</Property>
  </Properties>
  <Appenders>
    <RollingRandomAccessFile name="FileLogging" fileName="${baseDir}/apikardex.log" filePattern="${baseDir}/apikardex-%d{MMddyyyy}.%i.log.gz">
      <PatternLayout>
        <Pattern>[%t] %-5level - %msg%n</Pattern>
      </PatternLayout>
      <Policies>
        <TimeBasedTriggeringPolicy />
        <SizeBasedTriggeringPolicy size="10 MB"/>
      </Policies>
      <DefaultRolloverStrategy max="100">
        <Delete basePath="${baseDir}" maxDepth="2">
          <IfFileName glob="apikardex-*.log.gz">
            <IfAny>
              <IfLastModified age="60d" />
              <IfAccumulatedFileSize exceeds="1 GB" />
            </IfAny>
          </IfFileName>
        </Delete>
      </DefaultRolloverStrategy>
    </RollingRandomAccessFile>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="[%t] %-5level - %msg%n"/>
    </Console>
  </Appenders>
  <Loggers>
    <Root level="info" >
      <AppenderRef ref="FileLogging"/>
    </Root>
  </Loggers>
</Configuration>

```

### Tratando con JKS

Para activar el servicio de API en modo seguro, es preciso tener un fichero con el almacén de claves en formato JKS. En nuestro caso, vamos a mostrar como se generaría este fichero para el certificado `*.hefame.es`, partiendo de los ficheros en formato PEM (como los que usa apache2, por lo general con extensiones .crt y .key)

Tenemos los siguientes ficheros:
```
hefame.key               -> Clave privada para el certificado *.hefame.es
root.crt                 -> Thawte Primary Root CA
 |
 +-- intermediate.crt    -> Thawte SSL CA - G2
      |
      +-- hefame.crt     -> Certificado *.hefame.es
```
Primero, tenemos que crear un fichero PKCS12 con el certificado `hefame.crt` y la clave `hefame.key`.
Este fichero `hefame.p12` lo generamos con el siguiente comando:
```shell
# openssl pkcs12 -export -in hefame.crt -inkey hefame.key -out hefame.p12 -name hefame
```
***Nota**: Este comando nos pedirá la clave para abrir `hefame.key` y luego nos pedirá una nueva clave (llamemosla `K1`) para cifrar el fichero de salida.*

Utilizando el fichero `hefame.p12` que acabamos de generar, creamos el almacén de claves JKS:

```shell
# keytool -importkeystore -destkeystore hefame.jks -srckeystore hefame.p12  -srcstoretype PKCS12 -destalias hefame -alias hefame
```
***Nota**: Este comando nos pedirá la clave `K1` para abrir `hefame.p12` y luego nos pedirá una nueva clave (que llamaremos `K2`)para cifrar el almacén de claves.*

Ya tenemos el almacén de claves con el certificado `*.hefame.es` y su clave privada. A continuación, añadiremos la cadena de certificados de Thawte:
```shell
# keytool -import -keystore hefame.jks -file root.crt -alias root
# keytool -import -keystore hefame.jks -file intermediate.crt -alias intermediate
```
(Nota: Este comando nos pedirá la clave `K2` para abrir el almacén `hefame.jks`)

En este punto, el fichero `hefame.jks` ya tiene todo lo necesario para que el servidor API pueda arrancar en modo seguro. Cuando el servidor arranca en modo seguro, este escribe información de depuración en el log acerca de los certificados y las claves que ha encontrado, por si fuera necesario depurar.
Tambien podemos utilizar la herramienta keytool para listar los certificados (con la opción `-v` imprime todos los detalles de la mundial):
```shell
# keytool -list -keystore hefame.jks
Enter keystore password: "clave K2"

Keystore type: JKS
Keystore provider: SUN

Your keystore contains 3 entries

root, Aug 16, 2016, trustedCertEntry,
Certificate fingerprint (SHA1): 91:C6:D6:EE:3E:8A:C8:63:84:E5:48:C2:99:29:5C:75:6C:81:7B:81
hefame, Aug 16, 2016, PrivateKeyEntry,
Certificate fingerprint (SHA1): 4E:B6:84:53:8C:ED:3E:D3:38:99:60:7A:FA:C7:82:7B:73:1E:6D:35
intermediate, Aug 16, 2016, trustedCertEntry,
Certificate fingerprint (SHA1): 2E:A7:1C:36:7D:17:8C:84:3F:D2:1D:B4:FD:B6:30:BA:54:A2:0D:C5
```

### Configuración de la base de datos
Para obtener esta información en tiempo real, el servidor de la API realiza consultas directamente sobre la base de datos del Kardex, en concreto, sobre la tabla `KARDEX_HEFAME.ARTIKEL`.
La consulta básica que realiza es:
```sql
SELECT ARTIKEL, EAN, BESCHREIBUNG1, BESTAND FROM KARDEX_HEFAME.ARTIKEL;
```

Los campos de la tabla se corresponden de la siguiente manera:

- `ARTIKEL`. Código del artículo
- `EAN`. Código EAN
- `BESCHREIBUNG1`. Nombre del artículo
- `BESTAND`. Cantidad en stock

Para acceder a la BBDD, se ha creado el usuario `APIKARDEX` de la siguiente manera:
```sql
CREATE USER APIKARDEX IDENTIFIED BY "KeepMyS3cret"
  DEFAULT TABLESPACE USERS
  TEMPORARY TABLESPACE TEMP;

GRANT CREATE SESSION TO APIKARDEX;
GRANT SELECT ON "KARDEX_HEFAME"."ARTIKEL" TO APIKARDEX;
```
