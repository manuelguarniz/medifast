# Diagramas funcionales

> Diagramas en [Mermaid](https://mermaid.js.org/): GitHub y VS Code (extensión *Markdown Preview Mermaid Support*) los renderizan directamente. Para el informe, exportarlos a PNG/SVG con [mermaid.live](https://mermaid.live) o con mermaid-cli (ver [índice](README.md)).
>
> La fuente de verdad de requerimientos y reglas es el [README del proyecto](../../README.md). Si un diagrama lo contradice, se corrige el diagrama.

Describen **qué hace el sistema y para quién**, sin entrar en cómo está construido.

| # | Diagrama |
| --- | --- |
| F-01 | Casos de uso |
| F-02 | Proceso AS-IS (gestión manual actual) |
| F-03 | Proceso TO-BE (con MEDIFAST) |
| F-04 | Estados de la cita |
| F-05 | Estados del pago |
| F-06 | Mapa de navegación |
| F-07 | Cronograma |

> **Nota sobre BPMN:** Mermaid no tiene notación BPMN. F-02 y F-03 usan diagramas de flujo con carriles (`subgraph`) que siguen la lógica BPMN: inicio y fin redondeados, tareas rectangulares, compuertas en rombo. Si la rúbrica exige la notación BPMN formal, se redibujan en Bizagi a partir de estos.

---

## F-01 — Casos de uso

Actores, casos de uso (CU01–CU10) y la tarea automática del sistema (RF16). Detalle y trazabilidad en el README, §8.

```mermaid
flowchart LR
    PAC((Paciente))
    MED((Médico))
    SIS((Sistema))

    subgraph MEDIFAST["Sistema MEDIFAST"]
        direction TB
        CU01(["CU01 Registrarse"])
        CU02(["CU02 Iniciar / cerrar sesión"])
        CU03(["CU03 Consultar disponibilidad"])
        CU04(["CU04 Reservar y adjuntar constancia"])
        CU05(["CU05 Ver historial y descargar PDF"])
        CU06(["CU06 Actualizar mis datos"])
        CU07(["CU07 Consultar agenda"])
        CU08(["CU08 Validar o rechazar pago"])
        CU09(["CU09 Atendida / no asistió / cancelar"])
        CU10(["CU10 Generar y exportar reportes"])
        RF16(["RF16 Cancelar reservas vencidas"])
    end

    PAC --- CU01
    PAC --- CU02
    PAC --- CU03
    PAC --- CU04
    PAC --- CU05
    PAC --- CU06
    MED --- CU02
    MED --- CU07
    MED --- CU08
    MED --- CU09
    MED --- CU10
    SIS --- RF16

    CU04 -. incluye .-> CU03
    CU08 -. extiende .-> CU07
    CU09 -. extiende .-> CU07
```

---

## F-02 — Proceso AS-IS (gestión manual actual)

Según la entrevista con el Dr. Barrantes: la cita se coordina por WhatsApp, el pago se hace por transferencia y la agenda se lleva en un cuaderno.

```mermaid
flowchart LR
    subgraph PAC["Paciente"]
        direction TB
        A0([Inicio]) --> A1["Escribe al médico por WhatsApp pidiendo una cita"]
        A3["Elige un horario"]
        A4["Transfiere el pago a la cuenta bancaria del médico"]
        A5["Envía la captura del voucher por WhatsApp"]
        A8["Asiste a la consulta"]
    end

    subgraph MED["Médico"]
        direction TB
        B1["Revisa el cuaderno de citas"]
        B2["Informa los horarios libres y el costo"]
        B3["Verifica el abono en su cuenta bancaria"]
        B4{"¿Pago correcto?"}
        B5["Anota la cita en el cuaderno"]
        B6["Confirma la cita por WhatsApp"]
        B7["Solicita corregir el pago"]
        B9["Atiende al paciente"]
        B10([Fin])
    end

    A1 --> B1 --> B2 --> A3 --> A4 --> A5 --> B3 --> B4
    B4 -- Sí --> B5 --> B6 --> A8 --> B9 --> B10
    B4 -- No --> B7 --> A4
```

**Problemas que muestra:** el médico interviene en cada paso administrativo, la disponibilidad depende de su respuesta, el cuaderno no impide cruces de horario y la verificación del voucher es manual y sin registro.

---

## F-03 — Proceso TO-BE (con MEDIFAST)

```mermaid
flowchart LR
    subgraph PAC["Paciente"]
        direction TB
        P0([Inicio]) --> P1["Inicia sesión o se registra"]
        P2["Elige un horario disponible"]
        P3["Adjunta la constancia de pago Yape"]
        P4["Ve la confirmación y descarga el PDF"]
        P5["Asiste a la consulta"]
    end

    subgraph SIS["Sistema MEDIFAST"]
        direction TB
        S1["Detecta el tipo de cita (nuevo / control) y muestra horarios y tarifa"]
        S2{"¿Tiene una cita activa?"}
        S3["Muestra su cita activa"]
        S4["Aparta los bloques 15 min (PENDIENTE_PAGO)"]
        S5{"¿Constancia dentro de 15 min?"}
        S6["Cancela la reserva y libera el horario"]
        S7["Registra el pago POR_VALIDAR y la cita PROGRAMADA"]
        S8(["Fin: reserva vencida"])
    end

    subgraph MED["Médico"]
        direction TB
        M1["Revisa la agenda y la constancia"]
        M2{"¿Constancia válida?"}
        M3["Rechaza el pago con observación (cita CANCELADA)"]
        M4["Valida el pago"]
        M5{"¿El paciente asistió?"}
        M6["Atiende y marca ATENDIDA"]
        M7["Marca NO_ASISTIO"]
        M8([Fin])
    end

    P1 --> S1 --> S2
    S2 -- Sí --> S3 --> M8
    S2 -- No --> P2 --> S4 --> P3 --> S5
    S5 -- No --> S6 --> S8
    S5 -- Sí --> S7 --> P4 --> M1 --> M2
    M2 -- No --> M3 --> M8
    M2 -- Sí --> M4 --> P5 --> M5
    M5 -- Sí --> M6 --> M8
    M5 -- No --> M7 --> M8
```

**Mejoras:** el paciente se autogestiona, la BD impide cruces de horario (RN-01) y citas duplicadas (RN-09), el apartado de 15 min evita bloqueos indefinidos (RN-04) y cada validación del médico queda registrada.

---

## F-04 — Estados de la cita

Ciclo de vida de una cita (RN-05). Cita **activa** = `PENDIENTE_PAGO` o `PROGRAMADA`.

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE_PAGO : Paciente elige horario (aparta 15 min)
    PENDIENTE_PAGO --> PROGRAMADA : Adjunta constancia
    PENDIENTE_PAGO --> CANCELADA : Vencen los 15 min (sistema) o médico con observación
    PROGRAMADA --> ATENDIDA : Médico, con pago VALIDADO
    PROGRAMADA --> NO_ASISTIO : Médico
    PROGRAMADA --> CANCELADA : Médico con observación o rechazo del pago
    ATENDIDA --> [*]
    NO_ASISTIO --> [*]
    CANCELADA --> [*]

    note right of PROGRAMADA
        Cita activa junto con PENDIENTE_PAGO
        (máximo una por paciente, RN-09)
    end note
```

---

## F-05 — Estados del pago

El pago se crea al adjuntar la constancia y lo resuelve el médico (RN-07).

```mermaid
stateDiagram-v2
    [*] --> POR_VALIDAR : Paciente adjunta constancia
    POR_VALIDAR --> VALIDADO : Médico valida
    POR_VALIDAR --> RECHAZADO : Médico rechaza (la cita pasa a CANCELADA)
    VALIDADO --> [*]
    RECHAZADO --> [*]
```

---

## F-06 — Mapa de navegación

Las 11 pantallas comprometidas para el APF3 (README, §7) y cómo se llega a cada una según el rol.

```mermaid
flowchart LR
    P0["P0 Login"]
    P1["P1 Registro"]

    subgraph PAC["Paciente"]
        P2["P2 Reservar cita"]
        P3["P3 Pago Yape (15 min)"]
        P4["P4 Confirmación + PDF"]
        P5["P5 Mis citas"]
        P6["P6 Mi perfil"]
    end

    subgraph MED["Médico"]
        P7["P7 Agenda"]
        P8["P8 Detalle de cita"]
        P9["P9 Reporte de citas"]
        P10["P10 Reporte de pagos"]
    end

    P0 <-->|Crear cuenta| P1
    P0 -->|Rol PACIENTE| P2
    P0 -->|Rol MEDICO| P7

    P2 -->|Elige horario| P3
    P3 -->|Adjunta constancia| P4
    P4 --> P5
    P5 -->|Continuar pago| P3
    P5 -->|Descargar PDF| P4
    P2 -. menú .- P5
    P5 -. menú .- P6
    P6 -. menú .- P2

    P7 -->|Ver cita| P8
    P8 -->|Volver| P7
    P7 -. menú .- P9
    P9 -. menú .- P10
    P10 -. menú .- P7
```

---

## F-07 — Cronograma

Plan hasta el APF3 (24-oct) y la sustentación final (24-nov).

```mermaid
gantt
    title MEDIFAST — APF3 y entrega final
    dateFormat YYYY-MM-DD
    axisFormat %d/%m

    section APF3
    BD y repositorio                        :a1, 2026-10-20, 1d
    Arquitectura y cuentas (P0 P1 P6)       :a2, 2026-10-21, 1d
    Flujo del paciente (P2 a P5)            :a3, 2026-10-22, 1d
    Flujo del médico y reportes (P7 a P10)  :a4, 2026-10-23, 1d
    Informe y entrega                       :a5, 2026-10-24, 1d
    Entrega APF3                            :milestone, m1, 2026-10-24, 0d

    section Entrega final
    Correcciones del APF3 y pulido          :b1, 2026-10-26, 7d
    Pruebas, cobertura y seguridad          :b2, 2026-11-02, 7d
    Usabilidad y validación con el doctor   :b3, 2026-11-09, 7d
    Manual, Javadoc e informe final         :b4, 2026-11-16, 7d
    Ensayo                                  :b5, 2026-11-23, 1d
    Sustentación                            :milestone, m2, 2026-11-24, 0d
```
