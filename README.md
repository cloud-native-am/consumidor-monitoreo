# Microservicio Consumidor Monitoreo

Microservicio Spring Boot responsable de **persistir** todos los eventos del pipeline en una base de datos Oracle Autonomous Database (Cloud). Actúa como el punto final del flujo, registrando tanto las ubicaciones crudas como los horarios enriquecidos para auditoría y análisis.

---

## Tabla de contenidos

- [Descripción](#descripción)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Base de datos](#base-de-datos)
- [Configuración](#configuración)
  - [Variables de entorno](#variables-de-entorno)
  - [Oracle Wallet](#oracle-wallet)
  - [application.properties](#applicationproperties)
- [Ejecución](#ejecución)
  - [Local](#ejecución-local)
  - [Docker](#ejecución-con-docker)
- [Topics Kafka consumidos](#topics-kafka-consumidos)

---

## Descripción

Este microservicio escucha **dos topics de Kafka simultáneamente** y persiste cada mensaje recibido en su tabla correspondiente de Oracle:

| Topic consumido         | Tabla Oracle              | Descripción                           |
|-------------------------|---------------------------|---------------------------------------|
| `ubicaciones_vehiculos` | `LOG_UBICACION_VEHICULO`  | Registra ubicaciones GPS de vehículos |
| `horarios`              | `LOG_HORARIO_VEHICULO`    | Registra horarios y datos de turno    |

No expone ningún endpoint REST. Su operación es completamente orientada a eventos.

---

## Arquitectura

```
   TOPIC: ubicaciones_vehiculos          TOPIC: horarios
              │                                │
              ▼ (grupo: grupo-monitoreo)        ▼ (grupo: grupo-monitoreo)
┌─────────────────────────────────────────────────────────────────┐
│                    CONSUMIDOR-MONITOREO                         │
│                                                                 │
│  ┌──────────────────────────┐  ┌──────────────────────────────┐ │
│  │ MonitoreoUbicacionService│  │  MonitoreoHorarioService     │ │
│  │ @KafkaListener           │  │  @KafkaListener              │ │
│  │                          │  │                              │ │
│  │ deserializar →           │  │  deserializar →              │ │
│  │ mapear entidad →         │  │  mapear entidad →            │ │
│  │ repository.save()        │  │  repository.save()           │ │
│  └─────────────┬────────────┘  └──────────────┬───────────────┘ │
└────────────────┼───────────────────────────────┼────────────────┘
                 │                               │
                 ▼                               ▼
   ┌─────────────────────────┐   ┌─────────────────────────────┐
   │ LOG_UBICACION_VEHICULO  │   │   LOG_HORARIO_VEHICULO      │
   │  (Oracle Autonomous DB) │   │   (Oracle Autonomous DB)    │
   └─────────────────────────┘   └─────────────────────────────┘
```

---

## Tecnologías

| Tecnología              | Versión | Uso                                    |
|-------------------------|---------|----------------------------------------|
| Java                    | 21      | Lenguaje principal                     |
| Spring Boot             | 4.0.3   | Framework de aplicación                |
| Spring Kafka            | -       | Consumer de topics Kafka               |
| Spring Data JPA         | -       | Acceso a base de datos                 |
| Hibernate               | -       | ORM (dialecto Oracle)                  |
| Oracle JDBC             | 21      | Driver de conexión a Oracle DB         |
| Oracle PKI / OSDT       | -       | Autenticación con Oracle Wallet (mTLS) |
| Lombok                  | -       | Reducción de boilerplate               |
| Apache Kafka            | 7.4.4   | Broker de mensajes                     |
| Maven                   | 3.9     | Gestión de dependencias y build        |
| Docker                  | -       | Contenerización                        |

---

## Estructura del proyecto

```
consumidor-monitoreo/
├── src/
│   └── main/
│       ├── java/com/sumativa/consumidor_monitoreo/
│       │   ├── ConsumidorMonitoreoApplication.java       # Entry point
│       │   ├── config/
│       │   │   └── KafkaConfig.java                      # Consumer group config
│       │   ├── dto/
│       │   │   ├── UbicacionVehiculoDto.java             # Mensaje de ubicaciones
│       │   │   └── HorarioVehiculoDto.java               # Mensaje de horarios
│       │   ├── entity/
│       │   │   ├── LogUbicacionVehiculo.java             # Entidad JPA para ubicaciones
│       │   │   └── LogHorarioVehiculo.java               # Entidad JPA para horarios
│       │   ├── repository/
│       │   │   ├── LogUbicacionVehiculoRepository.java   # JpaRepository ubicaciones
│       │   │   └── LogHorarioVehiculoRepository.java     # JpaRepository horarios
│       │   └── service/
│       │       ├── MonitoreoUbicacionService.java        # Listener topic ubicaciones
│       │       └── MonitoreoHorarioService.java          # Listener topic horarios
│       └── resources/
│           └── application.properties                     # Configuración de la app
├── wallet/                                               # Certificados Oracle Wallet
│   ├── cwallet.sso
│   ├── ewallet.p12
│   ├── ewallet.pem
│   ├── keystore.jks
│   ├── truststore.jks
│   ├── ojdbc.properties
│   ├── sqlnet.ora
│   └── tnsnames.ora
├── .env                                                  # Variables de entorno (local)
├── Dockerfile                                            # Build multi-etapa con wallet
└── pom.xml                                              # Dependencias Maven
```

---

## Base de datos

### Oracle Autonomous Database (Cloud)

- **Proveedor:** Oracle Cloud Infrastructure
- **Región:** sa-saopaulo-1 (Brasil)
- **Autenticación:** Wallet mTLS (certificados incluidos en `/wallet/`)
- **Vencimiento del Wallet:** 2030-10-20
- **Schema:** `CLOUDSUMATIVA`

### Tablas

Las tablas deben crearse con el script disponible en [../script_oracle/schema.sql](../script_oracle/schema.sql).

#### `LOG_UBICACION_VEHICULO`

Almacena el registro crudo de ubicaciones GPS publicadas por `productor-ubicaciones`.

| Columna              | Tipo              | Descripción                          |
|----------------------|-------------------|--------------------------------------|
| `ID`                 | NUMBER (PK, auto) | Identificador único auto-incremental |
| `PATENTE`            | VARCHAR2(10)      | Identificador del vehículo           |
| `LATITUD`            | NUMBER(18, 15)    | Coordenada de latitud GPS            |
| `LONGITUD`           | NUMBER(18, 15)    | Coordenada de longitud GPS           |
| `FECHA_ACTUALIZACION`| TIMESTAMP         | Fecha/hora del evento                |

#### `LOG_HORARIO_VEHICULO`

Almacena los eventos enriquecidos publicados por `consumidor-procesa-senales`.

| Columna                        | Tipo              | Descripción                              |
|--------------------------------|-------------------|------------------------------------------|
| `ID`                           | NUMBER (PK, auto) | Identificador único auto-incremental     |
| `PATENTE`                      | VARCHAR2(10)      | Identificador del vehículo               |
| `LATITUD`                      | NUMBER(18, 15)    | Coordenada de latitud GPS                |
| `LONGITUD`                     | NUMBER(18, 15)    | Coordenada de longitud GPS               |
| `HORA_ACTUALIZACION`           | TIMESTAMP         | Fecha/hora del evento original           |
| `HORA_ESTIMADA_PROXIMA_PARADA` | TIMESTAMP         | ETA de la próxima parada (+15 min)       |
| `TURNO`                        | VARCHAR2(10)      | Turno activo (MAÑANA / TARDE / NOCHE)    |
| `CIUDAD_ORIGEN`                | VARCHAR2(50)      | Ciudad de origen del recorrido           |
| `CIUDAD_DESTINO`               | VARCHAR2(50)      | Ciudad de destino del recorrido          |

---

## Configuración

### Variables de entorno

Crea un archivo `.env` en la raíz del microservicio con el siguiente contenido:

```env
CONNECTION_ALIAS=<alias_tns_de_tu_wallet>
WALLET_PATH=<ruta_absoluta_a_la_carpeta_wallet>
ORACLE_USERNAME=<usuario_oracle>
ORACLE_PASSWORD=<contraseña_oracle>
API_PORT=8080
```

**Ejemplo (Windows local):**

```env
CONNECTION_ALIAS=zbfuyhmceud2olz0_tp
WALLET_PATH=D:/Bani/CLOUD_NATIVE_1/Sumativa_3/consumidor-monitoreo/wallet
ORACLE_USERNAME=CLOUDSUMATIVA
ORACLE_PASSWORD=tu_contraseña_aqui
API_PORT=8080
```

> El archivo `.env` **no debe commitearse** al repositorio ya que contiene credenciales sensibles.

### Oracle Wallet

El wallet contiene los certificados necesarios para la conexión mTLS con Oracle Autonomous Database. Debe ubicarse en la carpeta `wallet/` del microservicio.

**Contenido esperado del wallet:**

```
wallet/
├── cwallet.sso       # Wallet auto-login
├── ewallet.p12       # Wallet con contraseña
├── ewallet.pem       # Certificado en formato PEM
├── keystore.jks      # Keystore Java
├── truststore.jks    # Truststore Java
├── ojdbc.properties  # Propiedades JDBC de Oracle
├── sqlnet.ora        # Configuración de red Oracle
└── tnsnames.ora      # Alias de conexión TNS
```

Descarga el wallet desde la consola de Oracle Cloud:
**OCI Console → Autonomous Database → DB Connection → Download Wallet**

### `application.properties`

```properties
spring.application.name=consumidor-monitoreo
spring.config.import=optional:file:.env[.properties]
server.port=${API_PORT}

# --- Kafka Consumer ---
# Local
spring.kafka.bootstrap-servers=localhost:29092,localhost:39092,localhost:49092
# Docker
# spring.kafka.bootstrap-servers=kafka-1:9092,kafka-2:9092,kafka-3:9092

spring.kafka.consumer.group-id=grupo-monitoreo
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer

spring.jackson.deserialization.fail-on-unknown-properties=false

# --- Oracle Autonomous Database ---
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.url=jdbc:oracle:thin:@${CONNECTION_ALIAS}?TNS_ADMIN=${WALLET_PATH}
spring.datasource.username=${ORACLE_USERNAME}
spring.datasource.password=${ORACLE_PASSWORD}

# --- JPA / Hibernate ---
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect
spring.jpa.hibernate.ddl-auto=update

# --- Connection Pool (HikariCP) ---
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.connection-timeout=30000
```

---

## Ejecución

### Pre-requisitos

- Java 21+
- Maven 3.9+
- Archivo `.env` configurado correctamente
- Carpeta `wallet/` con los certificados Oracle
- Cluster Kafka corriendo (ver [kafka-server/](../kafka-server/))
- Tablas Oracle creadas (ver [script_oracle/schema.sql](../script_oracle/schema.sql))

### Ejecución local

```bash
cd consumidor-monitoreo

# Asegúrate de tener el .env configurado con WALLET_PATH apuntando a la carpeta wallet/ local

mvn spring-boot:run
```

El servicio comenzará a escuchar mensajes de Kafka automáticamente al iniciar.

### Ejecución con Docker

El Dockerfile incluye el wallet de Oracle dentro de la imagen. La variable `WALLET_PATH` en Docker apunta a `/app/wallet`.

```bash
cd consumidor-monitoreo
docker build -t consumidor-monitoreo .

docker run -d \
  --name consumidor-monitoreo \
  --network kafka-net \
  -p 8080:8080 \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-1:9092,kafka-2:9092,kafka-3:9092 \
  -e CONNECTION_ALIAS=zbfuyhmceud2olz0_tp \
  -e WALLET_PATH=/app/wallet \
  -e ORACLE_USERNAME=CLOUDSUMATIVA \
  -e ORACLE_PASSWORD=tu_contraseña_aqui \
  -e API_PORT=8080 \
  consumidor-monitoreo
```

> En Docker, el wallet se incluye en la imagen durante el build multi-etapa. No es necesario montarlo como volumen.

---

## Topics Kafka consumidos

| Topic                   | Grupo de consumidores | Tabla destino            |
|-------------------------|-----------------------|--------------------------|
| `ubicaciones_vehiculos` | `grupo-monitoreo`     | `LOG_UBICACION_VEHICULO` |
| `horarios`              | `grupo-monitoreo`     | `LOG_HORARIO_VEHICULO`   |

- **`auto-offset-reset=earliest`**: Al iniciar, procesa todos los mensajes pendientes desde el inicio del topic.
- **Grupo independiente**: Usa `grupo-monitoreo`, separado del grupo de `consumidor-procesa-senales`, garantizando que ambos consumidores reciban todos los mensajes del topic `ubicaciones_vehiculos`.
