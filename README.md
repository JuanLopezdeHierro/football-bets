# ⚽ Football Bets Real-Time Odds Project

## 📌 1. Descripción del Proyecto y Propuesta de Valor

**Football Bets Real-Time Odds** es una aplicación web que permite visualizar en tiempo real información detallada sobre partidos de LaLiga: horarios, estado del partido, cuotas de apuestas, estadio, árbitro y más.

### 🎯 Propuesta de Valor

Ofrecer a aficionados y apostadores una plataforma clara, actualizada automáticamente, para tomar decisiones informadas o disfrutar del seguimiento en directo. Utiliza Server-Sent Events (SSE) para actualizaciones sin recargar la página.

---

## 🧠 2. Justificación: APIs y DataMart

### 🧩 Fuentes de Datos

- **Match_Topic**
  - 📁 `datalake/eventstore/Match_Topic/default/YYYYMMDD.events`
  - Incluye: cuotas, estado del partido, equipos.
  - ✅ Datos en tiempo real y cruciales para la visualización.
  
- **MatchApi_Topic**
  - 📁 `datalake/eventstore/MatchApi_Topic/default/YYYYMMDD.events`
  - Incluye: estadio, árbitro, ronda.
  - ✅ Datos estáticos que enriquecen la experiencia.

### 🧱 Estructura del DataMart

- **Caché en memoria** (`MatchDataService`)
  - Lista `List<MatchEvent>` fusionada de ambas fuentes.
  - Alta velocidad de acceso.
- **Archivo persistente**
  - 📁 `output_datamart/default/YYYYMMDD.datamart.json`
  - Permite análisis offline, auditoría o reinicio de caché.

🔑 **Clave de unión**: `homeTeam` normalizado + fecha.

---

## ⚙️ 3. Instrucciones de Compilación y Ejecución

### ✅ Requisitos

- JDK 21+
- Apache Maven 3.6+
- Estructura esperada del datalake

### ▶️ Compilación

```bash
cd business-unit
mvn clean package

graph LR
    subgraph Datalake
        A1[Match_Topic.events]
        A2[MatchApi_Topic.events]
    end

    subgraph SpringBootApp
        B1[MatchController]
        B2[MatchDataService]
        B3[MatchSseService]
        B4[MatchTopicListener (opcional)]
        B5[output_datamart.json]
    end

    User[Usuario] --> Browser
    Browser -->|HTTP| B1
    Browser -->|SSE| B3
    B1 --> B2
    B2 --> A1
    B2 --> A2
    B2 --> B5
    B2 --> B3
    B4 --> B2

