# Visualizador de Partidos y Cuotas LaLiga en Tiempo Real

## 1. Descripción del Proyecto y Propuesta de Valor

Este proyecto es una aplicación web desarrollada en **Java 21** que permite a los usuarios visualizar información detallada y actualizada de los partidos de fútbol de LaLiga. La plataforma ofrece horarios, estado de los partidos (próximos, en directo, históricos), cuotas de apuestas en tiempo real (victoria local, empate, victoria visitante), información contextual como el estadio y el árbitro del partido.

**Propuesta de Valor Principal:**
El objetivo fundamental es ofrecer una experiencia de usuario fluida y sencilla para consultar todos los partidos de LaLiga, con un énfasis especial en la **evolución de las cuotas a lo largo del tiempo**, presentada mediante un gráfico interactivo. Esto permite a los aficionados y a los interesados en las apuestas deportivas seguir las tendencias y fluctuaciones de las cuotas de manera clara y directa, todo actualizado en tiempo real sin necesidad de recargar la página.

## 2. Justificación de la Elección de Fuentes de Datos y Estructura del DataMart

### Fuentes de Datos (Datalake)

El sistema se nutre de información almacenada en un "datalake" local estructurado en archivos, simulando la ingesta de datos de diferentes fuentes o APIs:

* **`Match_Topic` (Fuente de Cuotas y Estado del Partido):**
    * **Ubicación:** `datalake/eventstore/Match_Topic/default/YYYYMMDD.events`
    * **Contenido:** Archivos diarios con arrays JSON. Cada objeto JSON representa un evento de partido e incluye `timeStamp`, `dateTime` (que puede ser la hora de inicio o el minuto de juego, ej., "23′"), `oddsDraw`, `teamAway`, `oddsAway`, `teamHome`, `source` (ej. "Betfair"), `oddsHome`.
    * **Justificación:** Proporciona los datos dinámicos esenciales para el seguimiento de las cuotas y el estado en vivo de los partidos, lo cual es central para la propuesta de valor.
* **`MatchApi_Topic` (Fuente de Detalles Adicionales del Partido):**
    * **Ubicación:** `datalake/eventstore/MatchApi_Topic/default/YYYYMMDD.events`
    * **Contenido:** Archivos diarios con arrays JSON. Cada objeto JSON incluye `round`, `stadium`, `homeTeam` y `referee`.
    * **Justificación:** Enriquece la información de cada partido con datos contextuales (estadio, árbitro), mejorando la experiencia del usuario al ofrecer una vista más completa.

### Estructura del DataMart

El concepto de "DataMart" en este proyecto se materializa de dos formas:

1.  **Caché en Memoria (`MatchDataService.matchEventsCache`):**
    * Una lista de objetos `MatchEvent.java` dentro del módulo `business-unit`.
    * **Estructura:** Cada `MatchEvent` es un objeto Java que consolida la información de `Match_Topic` (cuotas, estado, `liveTimeDisplay`, logos) y `MatchApi_Topic` (estadio, árbitro). La fusión se realiza usando el nombre del equipo local (`homeTeam`) normalizado y la fecha como clave principal.
    * **Justificación:** Esta caché permite un acceso muy rápido a la información completa y actualizada de los partidos, necesaria para servir las vistas web y las actualizaciones vía Server-Sent Events (SSE). Se mantiene actualizada mediante un polling programado de los archivos del datalake.
2.  **Archivo Físico del DataMart (`output_datamart/default/YYYYMMDD.datamart.json`):**
    * **Contenido:** Un archivo JSON generado diariamente que contiene un snapshot de la `matchEventsCache`, es decir, la lista de objetos `MatchEvent` fusionados.
    * **Justificación:**
        * **Persistencia del Estado Combinado:** Guarda una copia de los datos procesados y fusionados del día.
        * **Análisis y Auditoría:** Permite el análisis offline de la evolución de los datos sin impactar la aplicación en caliente.
        * **Potencial Recuperación Rápida:** Podría usarse en el futuro para acelerar el reinicio de la aplicación (aunque no está implementado actualmente, la carga inicial reprocesa los archivos fuente).

La elección de un sistema de archivos como "datalake" y una caché en memoria como "DataMart" simplifica la infraestructura para este proyecto, permitiendo enfocarse en la lógica de procesamiento y visualización de datos en tiempo real.

## 3. Instrucciones para Compilar y Ejecutar

**Prerrequisitos:**
* JDK 21 (o superior compatible).
* Apache Maven 3.6+ (o superior).
* Apache ActiveMQ (o un broker JMS compatible) instalado y en ejecución.
* Una API Key válida de [Api-Football](https://www.api-football.com/) para el módulo `feeders`.
* Estructura de directorios del "datalake" creada (`datalake/eventstore/Match_Topic/default/` y `datalake/eventstore/MatchApi_Topic/default/`). Los archivos `.events` serán generados por los `feeders` y `eventReceivers`.

**Pasos para la Ejecución:**

1.  **Iniciar Apache ActiveMQ:**
    * Navega hasta el directorio `bin` de tu instalación de ActiveMQ.
    * Ejemplo en Windows (ajusta la ruta según tu instalación):
        ```bash
        cd C:\DACD\apache-activemq-6.1.0\bin\win64
        start activemq.bat console
        ```
        (O `activemq start` en otros sistemas si está configurado como servicio).

2.  **Ejecutar los Módulos del Proyecto (en orden):**
    Asumiendo que tienes una estructura de proyecto con módulos como `feeders`, `event-store`, y `business-unit`. Deberás compilar cada módulo si es necesario (`mvn clean package`).

    * **a. Módulo `feeders`:**
        * Este módulo es responsable de obtener datos de fuentes externas (como Api-Football) y enviarlos a los tópicos JMS.
        * Ejecuta la clase principal (Main) de este módulo.
        * Se te solicitará ingresar tu API Key de Api-Football.
        * Este módulo comenzará a enviar datos a los tópicos JMS (`Match_Topic`, `MatchApi_Topic`).

    * **b. Módulo `event-store` (EventReceivers):**
        * Este módulo contiene los listeners JMS (`EventReceiver` para `Match_Topic` y `EventReceiverApi` para `MatchApi_Topic`) que escuchan los mensajes de los `feeders` y los persisten en los archivos `.events` del datalake.
        * Ejecuta la clase principal (Main) de este módulo (o las clases de los listeners si se ejecutan por separado).

    * **c. Módulo `business-unit` (Aplicación Principal):**
        * Este es el módulo que hemos estado desarrollando, contiene la lógica de negocio, el DataMart en memoria y la interfaz web.

3.  **Acceder a la Aplicación Web:**
    * Abre tu navegador web.
    * Ve a la dirección: `http://localhost:8080/laliga/matches`
    * Deberías ver la lista de partidos de LaLiga. La información se actualizará automáticamente si hay partidos en curso o si nueva información llega a los archivos del datalake.

## 4. Ejemplos de Uso

* **Vista Principal de Partidos:**
    * URL: `http://localhost:8080/laliga/matches`
    * Muestra partidos "EN DIRECTO" y "PRÓXIMOS PARTIDOS" en tarjetas interactivas.
    * Se actualiza automáticamente vía Server-Sent Events (SSE).

* **Detalle del Partido:**
    * Haz clic en cualquier partido desde la vista principal.
    * URL: `http://localhost:8080/laliga/match/{matchId}`
    * Muestra información detallada: equipos, hora/estado, cuotas actuales, fuente de cuotas (Betfair con logo), estadio, árbitro.
    * Presenta un **gráfico de la evolución de las cuotas** (`oddsHome`, `oddsDraw`, `oddsAway`) a lo largo del tiempo, usando el `timeStamp` del evento para datos pre-partido y el minuto de juego (`dateTime`) para datos durante el partido.
    * También se actualiza vía SSE.

* **Stream SSE (para depuración):**
    * URL: `http://localhost:8080/laliga/matches/stream`
    * Permite ver los eventos `match-update` en crudo con el payload JSON de la lista de `MatchEvent` fusionados.

* **Archivo del DataMart Físico:**
    * Ubicación: `output_datamart/default/YYYYMMDD.datamart.json`
    * Contiene el snapshot JSON de los datos de partido fusionados, generado por la aplicación.

## 5. Arquitectura

### 5.1. Diagrama de Arquitectura del Sistema


### 5.2 Descripción del Diagrama de Sistema:

   * Api-Football: Fuente externa de datos de partidos.
   * Módulo Feeders: Consume datos de Api-Football y los publica en tópicos de ActiveMQ.
   * ActiveMQ: Broker de mensajería que desacopla los feeders de los event receivers.
   * Módulo Event-Store (EventReceivers): Se suscribe a los tópicos de ActiveMQ y escribe los datos crudos en archivos .events en el Datalake (separados para cuotas y para API de estadio/árbitro).
   * Datalake (Sistema de Archivos): Almacena los archivos .events diarios.
   * Aplicación Business Unit (Spring Boot):
   * MatchDataService: Lee periódicamente los archivos .events del Datalake, normaliza nombres de equipo, fusiona la información de ambas fuentes en objetos MatchEvent, mantiene esta lista fusionada como una caché en memoria (el DataMart lógico) y escribe un snapshot a un archivo físico (.datamart.json).
   * MatchController: Maneja las peticiones HTTP, obtiene datos del MatchDataService, y usa Thymeleaf para renderizar las vistas.
   * MatchSseService: Permite a los clientes (navegadores) suscribirse a actualizaciones. Cuando MatchDataService actualiza su caché, notifica a este servicio para enviar los nuevos datos a los clientes.
   * Output DataMart (Sistema de Archivos): Almacena el archivo .datamart.json generado.
   * Navegador del Usuario: Interactúa con la aplicación, recibe el HTML inicial y luego actualizaciones en tiempo real vía SSE.

5.3. Diagrama de Arquitectura de la Aplicación (Módulo business-unit)

## 6. Descripción del Diagrama de Aplicación:

Se muestra la interacción entre los componentes principales dentro del módulo business-unit:

   *El cliente interactúa con los REST_Endpoints (definidos en MatchController).
   *El MatchController utiliza MatchDataService para la lógica de negocio y datos, y ThymeleafEngine para la representación.
   *MatchDataService es central, manejando la caché (DataMart lógico), el polling, la fusión, y la interacción con MatchSseService para las actualizaciones en tiempo real.
   *El JavaScript del cliente maneja los eventos SSE recibidos de MatchSseService.
   
## 7. Principios y Patrones de Diseño Aplicados
   *Arquitectura en Capas (implícita): Presentación (Controlador, Thymeleaf, JS), Servicio (MatchDataService, MatchSseService), Dominio (Modelos Java), Acceso a Datos (adaptado a archivos).
   *Inyección de Dependencias (Spring): Usado para gestionar y conectar componentes (@Service, @Controller, @Autowired).
   *Single Responsibility Principle (SRP):
   *MatchController: Manejo de peticiones web.
   *MatchDataService: Lógica de datos, fusión, caché, polling y escritura del DataMart.
   *MatchSseService: Comunicación SSE.
   *EventReceivers (en event-store): Responsabilidad única de persistir datos de un tópico JMS al datalake.
   *Feeders (en módulo feeders): Responsabilidad de obtener datos externos y publicarlos.
   *Patrón Observador (SSE): El frontend se suscribe a eventos del backend.
   *Patrón DTO: MatchEventDTO y MatchApiDataDTO para la transferencia de datos desde las fuentes JSON.
   *Programación Orientada a Eventos (parcial): Con SSE y la respuesta a cambios en el datalake.
   *Tareas Programadas (@Scheduled): Para el polling periódico y desacoplado del datalake.
   *Normalización de Datos: Aplicada en MatchDataService para la fusión consistente de nombres de equipos.
   *Caché en Memoria: Usada en MatchDataService para un acceso rápido a los datos fusionados.
   *Desacoplamiento (con ActiveMQ): El broker de mensajería desacopla los productores de datos (feeders) de los consumidores iniciales (event-store).
   *DataMart: La caché matchEventsCache y el archivo YYYYMMDD.datamart.json actúan como un DataMart, proporcionando una vista consolidada y procesada de los datos para la aplicación.
