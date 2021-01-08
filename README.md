# Introducci�n
El prop�sito de este documento es especificar y definir los distintos recursos disponibles a trav�s del API de acceso a la informaci�n del Kardex. Este protocolo est� basado en los principios de una API REST, por lo que tambi�n se realizar� una explicaci�n de estos principios.

## �Qu� es una API Rest?
REST, REpresentational State Transfer, es un tipo de arquitectura de desarrollo software que se apoya totalmente en el est�ndar Cliente-Servidor HTTP. Es importante indicar que la arquitectura REST no es un est�ndar ni un protocolo, sino una serie de principios de arquitectura. REST nos permite crear servicios y aplicaciones que pueden ser usadas por cualquier dispositivo o cliente que entienda HTTP, por lo que es incre�blemente m�s simple y convencional que otras alternativas que se han usado en los �ltimos diez a�os como SOAP y XML-RPC. Por lo tanto REST es el tipo de arquitectura m�s natural y est�ndar para crear APIs para servicios orientados a Internet.


# Funcionamiento b�sico de la API

El servidor de acceso a la API se encuentra publicado en la siguiente URL por defecto:
```
https://orackardex-scan.hefame.es
```

El API es de tipo REST y va protegido por TLS, es decir, para realizar consultas hay que hacer peticiones HTTPS.
Todos los mensajes intercambiados con el servidor estar�n condificados en formato JSON.


##  Obtener stock de art�culos en Kardex
### Todo el stock
`GET /stock`

Obtiene una lista (Array JSON) con todos los art�culos existentes en el momento de la consulta, donde cada art�culo es un Objeto JSON con los siguientes campos:

- `id` C�digo del art�culo
- `ean` C�digo EAN (Opcional. De no especificarse se interpretar� como NULL)
- `name` Nombre del art�culo (Opcional. De no especificarse se interpretar� como NULL)
- `stock` Cantidad en stock

Un ejemplo de respuesta ser�a:
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

### El stock de un art�cluo en concreto

`GET /stock/<cod_articulo>`

Devuelve la informaci�n del art�culo solicitado en un Objeto JSON con los siguientes campos:

- `id` C�digo del art�culo
- `ean` C�digo EAN (Opcional. De no especificarse se interpretar� como NULL)
- `name` Nombre del art�culo (Opcional. De no especificarse se interpretar� como NULL)
- `stock` Cantidad en stock

Por ejemplo, para la petici�n  `GET /stock/751024`, la respuesta ser�a:
```json
{
	"id":"751024",
	"ean":"0983420398423",
	"name":"CHIROFLU  VACUNA  ANTIGRIPAL",
	"stock":15
}
```

### Ejemplo de llamada desde ABAP
A continuaci�n veremos un peque�o programa ABAP de ejemplo (cortes�a del compa�ero Dani), donde llamaremos al servicio y obtendremos una tabla con los resultados del stock:
```abap
* *************************************
* Paso 1. Llamada al servicio web
* *************************************

DATA: L_HTTP_URL TYPE STRING , * La URL del web service que vamos a llamar
      L_HTTP_CLIENT TYPE REF TO IF_HTTP_CLIENT,
      CONTENT TYPE STRING. * Un String donde se almacenar� la respuesta obtenida

* Construimos la URL a la que vamos a consultar, en funci�n de que datos
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


* Indicamos que el m�todo de llamada es "GET".
L_HTTP_CLIENT->REQUEST->SET_HEADER_FIELD( NAME  = '~REQUEST_METHOD'
                                          VALUE = 'GET' ).

* Enviamos la petici�n !!
L_HTTP_CLIENT->SEND( ).
CALL METHOD L_HTTP_CLIENT->RECEIVE
  EXCEPTIONS
    HTTP_COMMUNICATION_FAILURE = 1
    HTTP_INVALID_STATE         = 2
    HTTP_PROCESSING_FAILED     = 3
    OTHERS                     = 4.


* Obtenemos la respuesta de la petici�n.
* Esta respuesta contiene la cadena JSON con toda la informaci�n solicitada.
CONTENT = L_HTTP_CLIENT->RESPONSE->GET_CDATA( ).

* Cerramos la conexi�n (esto ahorra recursos)
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
* Tambi�n es interesante saber que podemos quedarnos con los campos que
* queramos, por ejemplo, descartar� el campo "ean" para
* este ejemplo.
TYPES: BEGIN OF ity_str,
       id TYPE string,
       name TYPE string,
       stock TYPE i
      END OF ity_str.

DATA: it_str TYPE STANDARD TABLE OF ity_str.

* Y dejamos que SAP haga su magia
* (Nota: la clase ZCL_JSON es una copia de CL_JSON donde se arregla un bug
* de la versi�n SAP que tenemos. En versiones posteriores de SAP el bug ya
* est� solucionado)
ZCL_JSON=>DESERIALIZE( EXPORTING JSON = CONTENT
                       CHANGING DATA = it_str).

* Et voil� :
DATA: wa_str TYPE ity_str .

LOOP AT it_str INTO wa_str.
  WRITE: wa_str-id.
  WRITE: wa_str-name.
  WRITE: wa_str-stock.
  WRITE:/.
ENDLOOP.
```

## Tratamiento de errores
En cualquier tipo de API es necesario que la aplicaci�n cliente detecte si las peticiones u operaciones que ha realizado han finalizado de forma correcta o por el contrario se ha producido alg�n tipo de error. El protocolo HTTP proporciona una lista de c�digos de estado para especificar el resultado de la operaci�n. Los c�digos utilizados son los siguientes:

- `200 OK` Indica que la operaci�n result� en �xito.
- `400 Bad Request` - *Petici�n err�nea*. La petici�n est� malformada, por ejemplo, el campo de art�culo est� mal construido.
- `404 Not Found` - *No encontrado*. Cuando un recurso no existente es solicitado.
- `500 Internal Server Error` - *Error interno del servidor*. Ha ocurrido un error inesperado en el servidor, por ejemplo, una excepci�n en c�digo no controlada, o no hay conexi�n a la base de datos.

En caso de error, el servidor devolver� un mensaje en formato JSON con al menos los siguientes campos:

- `message` - Un mensaje descriptivo del error
- `code` - Un c�digo de referencia del error, para poder depurarlo en caso de ser necesario.

A continuaci�n se muestran algunos ejemplos de errores comunes:

**Ejemplo 1.** Se busca un art�culo que no existe
URL: `https://orackardex-scan.hefame.es:8123/stock/88888`
C�digo respuesta HTTP: `404 Not Found`
Mensaje JSON:
```json
{
  "code": "436865636B696E48616E646C6572-147-1",
  "message": "No se encuentra el articulo",
  "httpcode": 404
}
```
**Ejemplo 2.** El art�culo a buscar est� vac�o
URL: `https://orackardex-scan.hefame.es:8123/stock//`
C�digo respuesta HTTP: `400 Bad Request`
Mensaje JSON:
```json
{
  "code": "48747470436F6E74726F6C6C6572-30-3",
  "message": "Debe especificarse el articulo",
  "httpcode": 400
}
```

**Ejemplo 3. Error de la base de datos**
C�digo respuesta HTTP: `500 Internal Server Error`
Mensaje JSON:
```json
{
  "code": "436865636B696E536574-76-1",
  "oracle_code": 12541,
  "message": "ORA-12541 TNS:No Listener Error",
  "httpcode": 500
}
```



## Administraci�n del servidor
El servidor de API corre en los dos servidores que albergan la base de datos de Kardex, que corren en un cl�ster Oracle RAC. Puesto que para acceder a este servicio, accedemos a la direcci�n `orackardex-scan.hefame.es`, que en realidad es una direcci�n flotante, tenemos alta disponibilidad de la misma gracias al propio cl�ster de la base de datos.

### Ficheros
Por defecto, todos los ficheros necesarios se encuentran en el directorio `/usr/local/bin/apikardex` de ambas m�quinas del RAC de Kardex. Dentro de este directorio encontramos los siguientes ficheros:

| Fichero | Permisos | Descripci�n |
| -- | -- | -- |
| log4j2.xml | `-rw-r--r--` | Configuraci�n de los logs |
| hefame.jks | `-rw-------` | Keystore con certificados para HTTPS |
| apikardex.conf | `-rw-------` | Fichero de configuraci�n |
| apikardex.jar | `-rwx------` | Binario de la aplicaci�n |
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

#### Arranque autom�tico al reiniciar el SO

Para que la API se ponga a funcionar tan pronto la m�quina sea arrancada, crearemos un link al ejecutable `/usr/local/bin/apikardex/apikardex` en el directorio `/etc/init.d`:

```shell
# ln -s /usr/local/bin/apikardex/apikardex /etc/init.d/apikardex
```

## Configuraci�n del API

### Configuraci�n de la aplicaci�n
El script de arranque/parada tiene algunas variables que pueden ser modificadas (aunque generalmente no tendremos que modificar nada aqu�) para especificar los siguientes par�metros:

| Variable | Valor | Descripci�n |
| -- | -- | -- |
| `JAVA_BIN` | `/usr/bin/java` | Ejecutable de java (podr�amos forzar una JVM espec�fica, p.e. `/usr/lib/jvm/jre-1.7.0/bin/java` para JAVA7) |
| `BASE_DIR` | `/usr/local/bin/apikardex` | Directorio base de la aplicaci�n |
| `LOG_DIR` | `/var/log/apikardex` | Directorio donde se almacenan los logs del API |
| `CONFIG_FILE` | `$BASE_DIR/apikardex.conf` | Fichero de configuraci�n del API |
| `JAR_FILE` | `$BASE_DIR/apikardex.jar` | Ruta al JAR de la aplicaci�n |

### Configuraci�n del servicio
El resto de par�metros se especifica en el fichero de configuraci�n `CONFIG_FILE`, que est� en formato JSON, como por ejemplo:
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

| Par�metro | Valor por defecto  | Descripci�n |
| -- | -- | -- |
| `http_port` | 443 |  Puerto donde el servidor espera peticiones de la API. |
| `http_max_connections` | 10 | N�mero m�ximo de conexiones simult�neas que el servidor puede aceptar. Las excedentes ser�n rechazadas con `TCP_RESET`. |
| `ssl_enable` | false | Indica si se utilizar� un protocolo seguro para establecer las conexiones HTTP. |
| `ssl_keystore` | hefame.jks | El servidor espera encontrar un almac�n de claves JKS en el fichero indicado por este par�metro. M�s adelante, en la secci�n 'Tratando con JKS' hablaremos de como administrar este almac�n de claves. |
| `ssl_passphrase` | `<cadena vac�a>` | La clave de acceso al almac�n de claves, en caso de que sea necesario. |
| `oracle_tns`  | (DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=KARDEX))) | Cadena de conexi�n Oracle TNS para realizar conexiones a la base de datos de Kardex. |
| `oracle_user` | apikardex | Usuario con el que conectar a la base de dato Kardex. |
| `oracle_pass` | `<cadena vac�a>` | Clave del usuario de la base de datos |

### Configuraci�n de logs

Los logs que la aplicaci�n deja se configuran en funci�n del fichero `log4j2.xml`, que reside en el directorio base de la aplicaci�n. La aplicaci�n utiliza la librer�a `log4j2` para dejar log, por lo que este fichero se debe configurar en base a la documentaci�n de dicha librer�a.

Con el siguiente ejemplo, todos los logs de nivel `info` o superior que genere la aplicaci�n se dejan en el directorio `/var/log/apikardex` y la propia librar�a `log4j2` se encarga de rotarlos diariamente, comprimirlos y eliminar los logs anteriores a 60 d�as.

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

Para activar el servicio de API en modo seguro, es preciso tener un fichero con el almac�n de claves en formato JKS. En nuestro caso, vamos a mostrar como se generar�a este fichero para el certificado `*.hefame.es`, partiendo de los ficheros en formato PEM (como los que usa apache2, por lo general con extensiones .crt y .key)

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
***Nota**: Este comando nos pedir� la clave para abrir `hefame.key` y luego nos pedir� una nueva clave (llamemosla `K1`) para cifrar el fichero de salida.*

Utilizando el fichero `hefame.p12` que acabamos de generar, creamos el almac�n de claves JKS:

```shell
# keytool -importkeystore -destkeystore hefame.jks -srckeystore hefame.p12  -srcstoretype PKCS12 -destalias hefame -alias hefame
```
***Nota**: Este comando nos pedir� la clave `K1` para abrir `hefame.p12` y luego nos pedir� una nueva clave (que llamaremos `K2`)para cifrar el almac�n de claves.*

Ya tenemos el almac�n de claves con el certificado `*.hefame.es` y su clave privada. A continuaci�n, a�adiremos la cadena de certificados de Thawte:
```shell
# keytool -import -keystore hefame.jks -file root.crt -alias root
# keytool -import -keystore hefame.jks -file intermediate.crt -alias intermediate
```
(Nota: Este comando nos pedir� la clave `K2` para abrir el almac�n `hefame.jks`)

En este punto, el fichero `hefame.jks` ya tiene todo lo necesario para que el servidor API pueda arrancar en modo seguro. Cuando el servidor arranca en modo seguro, este escribe informaci�n de depuraci�n en el log acerca de los certificados y las claves que ha encontrado, por si fuera necesario depurar.
Tambien podemos utilizar la herramienta keytool para listar los certificados (con la opci�n `-v` imprime todos los detalles de la mundial):
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

### Configuraci�n de la base de datos
Para obtener esta informaci�n en tiempo real, el servidor de la API realiza consultas directamente sobre la base de datos del Kardex, en concreto, sobre la tabla `KARDEX_HEFAME.ARTIKEL`.
La consulta b�sica que realiza es:
```sql
SELECT ARTIKEL, EAN, BESCHREIBUNG1, BESTAND FROM KARDEX_HEFAME.ARTIKEL;
```

Los campos de la tabla se corresponden de la siguiente manera:

- `ARTIKEL`. C�digo del art�culo
- `EAN`. C�digo EAN
- `BESCHREIBUNG1`. Nombre del art�culo
- `BESTAND`. Cantidad en stock

Para acceder a la BBDD, se ha creado el usuario `APIKARDEX` de la siguiente manera:
```sql
CREATE USER APIKARDEX IDENTIFIED BY "KeepMyS3cret"
  DEFAULT TABLESPACE USERS
  TEMPORARY TABLESPACE TEMP;

GRANT CREATE SESSION TO APIKARDEX;
GRANT SELECT ON "KARDEX_HEFAME"."ARTIKEL" TO APIKARDEX;
```
