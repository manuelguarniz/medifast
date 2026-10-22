# MEDIFAST — Sistema web de reserva de citas y simulación de pagos con Yape

> Curso Integrador I: Sistemas Software — UTP, 2026-02
> Docente: Segundo Edwin Cieza Mostacero
> Cliente / patrocinador: Dr. Moisés Barrantes Cabrera (Medicina Interna y Reumatología, Trujillo) — **Consultorio Medifast**

## Equipo

Los integrantes figuran solo en la carátula de los informes (N-11). El desarrollo completo lo realiza una persona (E-06).

| Integrante | Código |
| --- | --- |
| Guarniz Cruz, Manuel Elmer Esteban | U24314208 |
| Linares Barrantes, Jorge Moisés | U23262883 |
| Hurtado Razuri, Jack Max Malek | U24228666 |
| Angulo Leiva, Jennifer Elizabeth | U24203850 |

Este documento es la **fuente única de verdad** del proyecto. Consolida los APF, el Project Charter, los scripts SQL, la rúbrica del APF3 (`rubrica_af3.txt`) y las rondas de preguntas (§3). Si un informe contradice este README, se corrige el informe.

Leyenda: ✅ Confirmado · 🟡 Supuesto de diseño (vigente salvo que se indique lo contrario) · ❌ Fuera de alcance

**Última actualización:** 20-oct-2026, después de la ronda 4 y de auditar la coherencia de RF, RN y RNF.

| Hito | Fecha | Alcance |
| --- | --- | --- |
| **APF3** | **sábado 24-oct-2026** | **100 % de las pantallas y RF** (N-24), arquitectura, librerías, TDD, GitHub |
| **Entrega final / sustentación** | **martes 24-nov-2026** | Calidad, pruebas, validación con el doctor, documentación y sustentación |

---

## Índice

1. [Inventario de archivos](#1-inventario-de-archivos)
2. [Inconsistencias](#2-inconsistencias)
3. [Preguntas](#3-preguntas)
4. [Definición del proyecto](#4-definición-del-proyecto)
5. [Alcance](#5-alcance)
6. [Actores y roles](#6-actores-y-roles)
7. [Requerimientos funcionales](#7-requerimientos-funcionales)
8. [Reglas de negocio](#8-reglas-de-negocio)
9. [Requerimientos no funcionales](#9-requerimientos-no-funcionales)
10. [Pantallas, casos de uso y trazabilidad](#10-pantallas-casos-de-uso-y-trazabilidad)
11. [Modelo de datos](#11-modelo-de-datos)
12. [Arquitectura, principios y stack](#12-arquitectura-principios-y-stack)
13. [Plan de trabajo](#13-plan-de-trabajo)
14. [Entregables y rúbrica APF3](#14-entregables-y-rúbrica-apf3)
15. [Estructura del repositorio](#15-estructura-del-repositorio)

---

## 1. Inventario de archivos

| Archivo | Contenido | Estado |
| --- | --- | --- |
| [`docs/entregables/APF1_Medifast.pdf`](docs/entregables/APF1_Medifast.pdf) (fuente: `proyecto_integrador.docx`, fuera del repo) | APF1: contexto, alternativas, Charter, Gantt, EDT, RF, CU, entrevista | Corregir: PostgreSQL → MySQL; "Consultorio privado Barrantes" → "Consultorio Medifast"; aclarar lo de multi-médico |
| [`docs/entregables/APF2_Medifast_Grupo2.pdf`](docs/entregables/APF2_Medifast_Grupo2.pdf) (fuente: `MEDIFAST_APF2.docx`, fuera del repo) | APF2: BPMN, BD, clases, UX/UI, reportes, documentación | Completar `[INSERTAR…]` / `Figura X`; actualizar RF, RN y BD según este README |
| [`docs/entregables/Project_Charter_Medifast.pdf`](docs/entregables/Project_Charter_Medifast.pdf) | Acta de constitución | Corregir PostgreSQL → MySQL (sin justificación, N-12) |
| [`docs/entregables/rubrica_apf3.txt`](docs/entregables/rubrica_apf3.txt) | Criterios de evaluación del APF3 | Base de §14 |
| `modelado.drawio`, `modelado_logico.png`, `modelado_fisico.png` | Modelos de BD anteriores (fuera del repo) | **Reemplazados** por [docs/diagramas/base-de-datos.md](docs/diagramas/base-de-datos.md) |
| `docs/diagramas/` | Diagramas en Mermaid: [funcionales](docs/diagramas/funcionales.md), [técnicos](docs/diagramas/tecnicos.md) y [base de datos](docs/diagramas/base-de-datos.md), con [índice](docs/diagramas/README.md) | ✅ Vigente |
| `script.sql`, `script_datos.sql`, `script_querys.sql`, `script_querys_bkp.sql` | DDL, datos y consultas en PostgreSQL con tablas clínicas (fuera del repo) | **Obsoletos**: reemplazados por [`docs/sql/`](docs/sql/) |
| [`docs/sql/`](docs/sql/) | `01_schema.sql`, `02_datos_prueba.sql`, `03_consultas.sql` para MySQL 8.4 | ✅ Vigente; probados en MySQL 8.4.11 |
| [`medifast-app/`](medifast-app/) | Aplicación Spring Boot + Thymeleaf | 🟡 En construcción: arquitectura, seguridad y login (P0) |

---

## 2. Inconsistencias

### 2.1 Resueltas (rondas 1 a 4)

| # | Tema | Resolución vigente |
| --- | --- | --- |
| I-01, I-17 | Motor de BD | **MySQL 8.4**; se corrigen el Charter y los APF (E-01, N-12) |
| I-02 | Frontend | Thymeleaf + HTML/CSS/JS |
| I-03, I-04 | Tablas clínicas y 100 médicos en los scripts | Se eliminan; el seed tiene 1 médico |
| I-05, I-21, I-34 | Datos y login del paciente | **DNI o CE**, nombres, apellidos, fecha de nacimiento, celular y correo, todos obligatorios. Login con **n.º de documento + contraseña** (P-01, N-01, N-02, N-20) |
| I-06, I-07, I-29 | Pago | El paciente adjunta la constancia durante la reserva; el **médico** la valida o la rechaza antes de atender. Rechazar = cancelar con observación (P-04, P-06, N-19) |
| I-08, I-24, I-32 | Estados y cancelación | Solo el médico cancela (siempre con observación) y marca no asistió. El paciente no cancela ni reprograma (P-07, N-06, N-07) |
| I-09, I-25, I-35 | Nuevo vs. control y citas simultáneas | **Control** si el paciente tiene alguna cita `ATENDIDA` previa con ese médico; si no, **nuevo**. **Solo una cita activa a la vez** por paciente (N-21, reemplaza a N-05) |
| I-10, I-18, I-33 | Horario | Se configura **solo en la BD**, sin pantalla. Seed: lunes a sábado de 09:00 a 13:00 (P-02, N-04, N-17) |
| I-11 | Acceso | Login con rol para paciente y médico |
| I-12 | Prioridad de RNF01 (Java) | Must |
| I-13 | Nombre | **Consultorio Medifast** (P-12) |
| I-14, I-20 | Reportes | Reporte 1: citas por día, semana o estado. Reporte 2: pagos por período. Ambos se exportan a Excel (P-09, N-08) |
| I-15 | Persona vs. paciente | `usuario` (credenciales y documento) + `paciente` / `medico` |
| I-16, I-22, I-27 | Metodología y equipo | XP adaptada a un solo desarrollador; los integrantes solo figuran en la carátula |
| I-19, I-38 | Reutilizable / multi-médico | **BD multi-médico y multi-consultorio; interfaz de 1 médico**. Los médicos se cargan por script (N-03). En los informes: "la BD soporta varios médicos; esta versión opera con uno" |
| I-23 | RN-01 en MySQL | Columna generada única en `cita_horario` (§11) |
| I-28 | Anticipación | Desde mañana hasta 30 días después (N-09) |
| I-30 | Ventana de 15 min | El horario queda apartado 15 min; si el paciente no adjunta la constancia, se libera (N-18) |
| I-31 | Precios | Nuevo S/ 150 (60 min), control S/ 80 (30 min); tabla `tarifa` por médico |
| I-36 | PDF | Confirmación descargable en PDF (RF15) |
| I-37 | Calendario | APF3 el 24-oct; final el 24-nov (§13) |
| I-39 | Alcance del APF3 | **100 % de las pantallas** en el APF3: RF11 (perfil) y RF15 (PDF) se adelantan al 24-oct (N-24) |
| I-40, I-41, I-42 | Rúbrica: librerías, TDD, GitHub | Guava, Commons, POI y Logback desde el día 1; TDD en los servicios; repositorio desde hoy (§12, §13) |
| I-43, I-44 | Coherencia y autoría | Informes alineados con este README antes del APF3; decisiones documentadas en `docs/decisiones/` |
| I-45 | Patrón DAO | Se acepta Spring Data JPA como DAO (N-25): interfaces `*Dao` que extienden `JpaRepository` |

### 2.2 Auditoría de coherencia de RF, RN y RNF (ronda 4)

Revisión de todos los requerimientos contra las respuestas de las rondas 1 a 4 (pedido en N-24). Cambios aplicados en §7, §8 y §9:

| # | Hallazgo | Corrección |
| --- | --- | --- |
| A-01 | RF11 (perfil) y RF15 (PDF) eran Should y quedaban para la entrega final, pero N-24 exige el 100 % de las pantallas en el APF3 | Pasan a **Must / APF3**. OpenPDF se agrega al stack del APF3 |
| A-02 | RF13 tenía "rechazar" como una acción aislada, pero N-19 dice que el médico cancela con observación | **Rechazar el pago = cancelar la cita con observación**, en una sola acción (el pago queda `RECHAZADO` y la cita `CANCELADA`) |
| A-03 | Ningún RF cubría al paciente que sale de la pantalla de pago antes de que venzan los 15 min | RF03: el paciente puede **retomar el pago** desde "Mis citas" mientras no venza el plazo |
| A-04 | RF11 no definía qué datos se pueden editar; el documento es el login | Se editan nombres, apellidos, fecha de nacimiento, celular, correo y contraseña. **Tipo y n.º de documento no se editan** |
| A-05 | RF08 restringía marcar atendida / no asistió a después de la hora de inicio; N-07 dice "a criterio del médico" | Se mantiene la restricción **solo para no marcar citas futuras**; dentro de eso, decide el médico (🟡) |
| A-06 | RF07 incluía "bloqueo tras 5 intentos" como si lo hubiera pedido el cliente | Se mueve a RNF03 (seguridad, uso de Guava) |
| A-07 | RNF11 exigía una cobertura ≥ 60 % en el APF3, que no es realista en 4 días | APF3: pruebas de **todas las RN** (TDD). Cobertura ≥ 60 %: entrega final |
| A-08 | La rúbrica habla de pantallas, pero no existía un inventario cerrado de pantallas | Nuevo inventario P0–P10 (§10.1) que se usa en Figma, en el código y en el informe |

---

## 3. Preguntas

Todas las preguntas de las rondas 1 a 4 están respondidas. **No hay preguntas abiertas.** Los supuestos de diseño marcados con 🟡 siguen vigentes salvo que se indique lo contrario.

### 3.1 Ronda 1 — Dr. Barrantes

| # | Pregunta | Respuesta |
| --- | --- | --- |
| P-01 | ¿Qué datos del paciente son obligatorios? | Tipo y número de documento, teléfono, correo, nombres, apellidos, fecha de nacimiento |
| P-02 | ¿Horario de atención? ¿Excepciones? | Los horarios deben ser libres y configurables desde la base de datos |
| P-03 | ¿Cómo se sabe si es paciente nuevo o de control? | Es control cuando el paciente tuvo alguna cita a la que asistió previamente |
| P-04 | ¿Pago antes de confirmar? ¿Tiempo de espera? | La ventana de reserva de cita tiene un tiempo de 15 minutos; posterior a ello, se cancela |
| P-05 | ¿Costo siempre S/ 150? | Según una encuesta: control S/ 80 y cita (nuevo) S/ 150 |
| P-06 | ¿Quién verifica el pago? | El médico, antes de atenderlo |
| P-07 | ¿Cancelar o reprogramar? | No está dentro del alcance (ver N-06: el médico sí cancela) |
| P-08 | ¿Si no asiste? | Se registra como "no asistió" |
| P-09 | ¿Reportes? | Un reporte con filtros por día, semana o estado; exportable a Excel |
| P-10 | ¿Anticipación máxima? | Desde un día antes hasta un mes antes |
| P-11 | ¿Confirmación al paciente? | En pantalla, con opción a descargar en PDF |
| P-12 | ¿Nombre del consultorio? | Medifast |

### 3.2 Ronda 1 — Equipo

| # | Pregunta | Decisión |
| --- | --- | --- |
| E-01 | ¿PostgreSQL o MySQL? | MySQL |
| E-02 | ¿El paciente tiene cuenta? | Sí: se registra y desde ahí reserva, ve su historial de citas y actualiza sus datos |
| E-03 | ¿El paciente consulta sus citas? | Sí, en el historial (de la más reciente a la más antigua) |
| E-04 | ¿Qué incluye la simulación de Yape? | Adjuntar la constancia de pago |
| E-05 | ¿Despliegue? | Localhost |
| E-06 | ¿Responsables? | Un solo desarrollador |
| E-07 | ¿Herramienta de prototipo? | Figma (falta el enlace) |
| E-08 | ¿Repositorio? | Aún no existe |
| E-09 | ¿Fechas? | Avanzar la planificación y la base de datos |

### 3.3 Ronda 2

| # | Pregunta | Respuesta |
| --- | --- | --- |
| N-01 | ¿Con qué inicia sesión el paciente? | DNI + contraseña |
| N-02 | ¿Celular obligatorio? | Sí |
| N-03 | ¿"Reutilizable" = parametrizable o multi-médico? | Multi-médico → **BD lista, interfaz de 1 médico** |
| N-04 | ¿Horario correcto? ¿Refrigerio? | Se gestiona en la BD; datos por defecto de 9:00 a 13:00 |
| N-05 | ¿Más de una cita futura a la vez? | Sí → **reemplazada por N-21** (una cita a la vez) |
| N-06 | ¿El médico cancela citas? | Sí, con una observación que detalle el motivo |
| N-07 | ¿Quién marca "no asistió"? | El médico, con un botón (como el de cancelar), a su criterio |
| N-08 | ¿Segundo reporte de pagos? | Sí |
| N-09 | ¿Reserva el mismo día? | No se puede reservar para el mismo día |
| N-10 | ¿Ver la constancia desde el panel? | Sí |
| N-11 | ¿Roles del equipo? | No; los integrantes solo van en la carátula |
| N-12 | ¿Justificar el cambio a MySQL? | No; solo se cambia a MySQL |
| N-13 | ¿Fecha de entrega? | 24 de octubre → **APF3** (próximo avance) |
| N-14 | ¿El médico registra citas por terceros? | No |
| N-15 | ¿Recuperar contraseña? | Fuera de alcance |
| N-16 | ¿Constancia JPG/PNG/PDF ≤ 5 MB, n.º de operación opcional? | Sí |

### 3.4 Ronda 3

| # | Pregunta | Respuesta |
| --- | --- | --- |
| N-17 | ¿Pantalla para editar el horario o solo BD? | Base de datos |
| N-18 | ¿Los 15 min apartan el horario y luego se libera? | Sí |
| N-19 | Si se rechaza la constancia, ¿quién cancela? | El médico, con observación |
| N-20 | ¿Qué documentos acepta el registro? | DNI y CE |
| N-21 | ¿Un paciente nuevo con 2 citas futuras? | **Definitiva:** para simplificar, solo una cita a la vez |
| N-22 | ¿Qué exige la rúbrica del APF3? | Ver `rubrica_af3.txt` (§14) |
| N-23 | ¿Fecha de la entrega final? | Dentro de 5 semanas → **24-nov-2026** |

### 3.5 Ronda 4

| # | Pregunta | Respuesta |
| --- | --- | --- |
| N-24 | ¿Qué parte del alcance se compromete para el APF3? | Hay que cubrir el **100 % de las pantallas**; revisar que RF, RNF y RN estén acordes a las respuestas (→ §2.2) |
| N-25 | ¿Spring Data JPA vale como patrón DAO? | Sí |

---

## 4. Definición del proyecto

### 4.1 Problema

✅ El Dr. Barrantes gestiona sus citas de forma manual: las anota en un cuaderno, coordina la disponibilidad directamente con cada paciente y verifica los pagos revisando las capturas de transferencia que recibe por WhatsApp. Esto le quita tiempo de atención, dificulta saber rápido qué horarios quedan libres y abre la posibilidad de cruces de horario.

### 4.2 Objetivo general

Desarrollar un sistema web para el Consultorio Medifast en el que los pacientes se registren, consulten los horarios disponibles y reserven una cita adjuntando la constancia de pago (Yape simulado), y en el que el médico valide los pagos, gestione su agenda y obtenga reportes. La base de datos queda preparada para varios médicos.

### 4.3 Objetivos específicos

1. Permitir el registro de pacientes (DNI/CE), la actualización de sus datos y el inicio de sesión por número de documento.
2. Mostrar la disponibilidad a partir del horario configurado en la BD.
3. Reservar citas de 60 min (nuevo, S/ 150) o 30 min (control, S/ 80), con el tipo detectado automáticamente y una sola cita activa por paciente.
4. Apartar el horario 15 min mientras el paciente adjunta la constancia, y entregar la confirmación en pantalla y en PDF.
5. Permitir al médico validar o rechazar pagos, cancelar con observación y registrar atendida / no asistió.
6. Generar dos reportes filtrables y exportables a Excel.
7. Construir la solución con Java, arquitectura MVC, DAO, SOLID y TDD, con control de versiones en GitHub.

### 4.4 Indicadores de éxito

- 100 % de las pantallas (P0–P10) y de los RF implementados en el APF3.
- 0 bloques asignados a más de una cita activa y 0 pacientes con más de una cita activa (constraints en BD).
- 0 bloques apartados más de 15 min sin constancia.
- Pruebas unitarias verdes para todas las reglas de negocio; cobertura ≥ 60 % en `service` para la entrega final.
- 100 % de los avances con evidencia en GitHub.
- Validación del Dr. Barrantes (acta o evidencia) antes del 24-nov.

---

## 5. Alcance

Todo el alcance funcional se entrega en el **APF3 (24-oct)**. Entre el APF3 y la entrega final solo se mejoran la calidad, las pruebas, la validación y la documentación.

### 5.1 Incluye

| Módulo | Detalle |
| --- | --- |
| Cuentas | Registro de paciente (DNI/CE); login por n.º de documento (paciente y médico); actualización de los datos del paciente |
| Horarios | Plantilla semanal por médico **en BD** (seed: lunes a sábado, 09:00–13:00); bloques de 30 min; bloqueos en BD |
| Citas | Reserva con apartado de 15 min; detección nuevo/control; una cita activa por paciente; confirmación en pantalla y en PDF; historial |
| Pago simulado | Pantalla tipo Yape con el monto según el tipo; constancia adjunta; posibilidad de retomar el pago dentro de los 15 min |
| Gestión médica | Agenda, ver constancia, validar el pago, rechazar (= cancelar con observación), cancelar con observación, atendida / no asistió |
| Reportes | (1) Citas por día, semana o estado; (2) pagos por período. Exportación a Excel (Apache POI) |
| Plataforma | Web responsive, MySQL, localhost. BD multi-médico y multi-consultorio |

### 5.2 No incluye ❌

Pantalla para editar horarios (se hace en la BD, N-17), cancelación o reprogramación por el paciente, varias citas activas por paciente, reprogramación por el médico, reembolsos, módulo de administración de médicos o consultorios, pasaporte u otros documentos, cambio de documento, recuperación de contraseña, notificaciones por correo/SMS/WhatsApp, registro de citas por terceros, historia clínica, diagnósticos, recetas, telemedicina, facturación electrónica, pagos reales o integración con Yape/bancos, IA, app móvil nativa, despliegue en la nube.

### 5.3 Supuestos y restricciones

- La interfaz opera con un médico y un consultorio; la BD admite varios.
- Un solo desarrollador. APF3 el 24-oct-2026; final el 24-nov-2026.
- Java ≥ 50 % del stack; uso obligatorio de Guava, Apache Commons, Apache POI y Logback (rúbrica).
- El pago Yape es **solo simulación**: no se piden credenciales bancarias.
- Ejecución en localhost; costo adicional S/ 0.00.

---

## 6. Actores y roles

| Actor | Descripción | Autenticación |
| --- | --- | --- |
| **Paciente** | Se registra, actualiza sus datos, consulta horarios, reserva (una cita activa), adjunta la constancia, descarga la confirmación en PDF y ve su historial | N.º de documento + contraseña |
| **Médico** | Ve su agenda, revisa constancias, valida o rechaza pagos, cancela con observación, marca atendida / no asistió y genera reportes | N.º de documento + contraseña (creado por script) |
| **Sistema** | Genera bloques, detecta nuevo/control, aplica la ventana de 1–30 días y cancela las reservas sin constancia a los 15 min | — |

---

## 7. Requerimientos funcionales

Todos los RF son **Must** y se entregan en el **APF3** (N-24).

| ID | Requerimiento | Actor | Pantalla | Origen |
| --- | --- | --- | --- | --- |
| RF01 | Reservar una cita en un horario disponible, con apartado de 15 min | Paciente | P2 | Entrevista, P-04 |
| RF02 | Registrar la cuenta: tipo (DNI/CE) y n.º de documento, nombres, apellidos, fecha de nacimiento, celular, correo y contraseña | Paciente | P1 | P-01, N-02, N-20 |
| RF03 | Simular el pago con Yape: mostrar el monto según el tipo, adjuntar la constancia y retomar el pago dentro de los 15 min | Paciente | P3 | P-04, P-05, E-04, A-03 |
| RF04 | Consultar la agenda con filtros por día, semana y estado | Médico | P7 | Entrevista |
| RF05 | Mostrar los horarios disponibles según la duración del tipo de cita | Paciente | P2 | P-03 |
| RF06 | Horario de atención configurable por médico **en BD** | Sistema | — (BD) | P-02, N-17 |
| RF07 | Iniciar y cerrar sesión por n.º de documento, con rol | Ambos | P0 | N-01 |
| RF08 | Marcar una cita como atendida o no asistió | Médico | P8 | P-08, N-07 |
| RF09 | Reportes: citas (día/semana/estado) y pagos (período), con exportación a Excel | Médico | P9, P10 | P-09, N-08 |
| RF10 | Historial de citas propias, de la más reciente a la más antigua | Paciente | P5 | E-02, E-03 |
| RF11 | Actualizar los datos personales y la contraseña | Paciente | P6 | E-02, N-24 |
| RF12 | Ver la constancia de pago de una cita | Médico | P8 | N-10 |
| RF13 | Validar el pago o rechazarlo (rechazo = cancelación con observación) | Médico | P8 | P-06, N-19 |
| RF14 | Cancelar una cita con observación obligatoria | Médico | P8 | N-06 |
| RF15 | Mostrar la confirmación de la cita en pantalla y descargarla en PDF | Paciente | P4, P5 | P-11, N-24 |
| RF16 | Cancelar automáticamente las reservas sin constancia después de 15 min y liberar el horario | Sistema | — (tarea) | P-04, N-18 |
| ~~RF17~~ | Pantalla para editar el horario | — | — | ❌ N-17 |

### 7.1 Criterios de aceptación

- **RF01** — Si el paciente ya tiene una cita activa (`PENDIENTE_PAGO` o `PROGRAMADA`), no puede reservar y se le muestra cuál es. Si no la tiene, al elegir un horario libre se crea, en una transacción, la cita `PENDIENTE_PAGO` con `expira_en = ahora + 15 min` y se apartan sus bloques (1 o 2). Si otro paciente tomó el horario antes: "Horario no disponible, elija otro".
- **RF02** — DNI de 8 dígitos; CE alfanumérico de 9 a 12 caracteres; n.º de documento único; fecha de nacimiento en el pasado; celular de 9 dígitos que empieza en 9; correo válido y único; contraseña de al menos 8 caracteres.
- **RF03** — Muestra un QR o número de Yape de ejemplo, el monto de la tarifa (S/ 150 / S/ 80), un contador del tiempo restante y la constancia como campo obligatorio (JPG/PNG/PDF ≤ 5 MB); n.º de operación opcional. Leyenda: "Simulación — no se realizó ningún cobro". Al adjuntarla, la cita pasa a `PROGRAMADA` y el pago queda `POR_VALIDAR`. Si el paciente sale de la pantalla, puede volver desde "Mis citas" mientras no venza el plazo; si vence, ve la cita como cancelada.
- **RF04** — Lista ordenada por fecha y hora con paciente, tipo, estado de la cita y estado del pago; filtros por día, semana y estado.
- **RF05** — Bloques desde mañana hasta +30 días, no bloqueados y sin cita activa. Para un paciente nuevo, solo inicios con 2 bloques consecutivos libres dentro del horario.
- **RF06** — Los bloques se generan a partir de `horario_semanal` para la ventana de reserva, sin duplicados.
- **RF07** — `/paciente/**` exige el rol PACIENTE y `/medico/**` el rol MEDICO. Al iniciar sesión, cada rol llega a su pantalla principal (P2 o P7).
- **RF08** — Solo sobre citas `PROGRAMADA` cuya hora de inicio ya llegó (no se marcan citas futuras 🟡). `ATENDIDA` exige el pago `VALIDADO`; `NO_ASISTIO`, no.
- **RF09** — Cada reporte tiene filtros, una consulta documentada y exportación .xlsx con Apache POI. Reporte 2: solo pagos `VALIDADO`, con cantidad y total en S/.
- **RF10** — Citas del paciente con fecha, hora, tipo, monto, estado de la cita y del pago, de la más reciente a la más antigua. Acciones: "Continuar pago" (si está `PENDIENTE_PAGO`) y "Descargar PDF" (si está `PROGRAMADA` o `ATENDIDA`).
- **RF11** — Se pueden editar nombres, apellidos, fecha de nacimiento, celular, correo y contraseña (pidiendo la actual). El tipo y el n.º de documento no se editan. Se aplican las mismas validaciones de RF02.
- **RF12** — La constancia solo la ven el paciente dueño y el médico de la cita.
- **RF13** — Solo sobre pagos `POR_VALIDAR`. "Validar" → pago `VALIDADO` con fecha y hora. "Rechazar" → pide una observación y, en una sola transacción, deja el pago `RECHAZADO` y la cita `CANCELADA`, y libera sus bloques.
- **RF14** — Solo sobre citas `PENDIENTE_PAGO` o `PROGRAMADA`; observación obligatoria; libera los bloques.
- **RF15** — La confirmación muestra el n.º de cita, el médico, la fecha, la hora, el tipo, el monto y el estado del pago, con un botón "Descargar PDF" (OpenPDF) disponible en P4 y P5.
- **RF16** — Una tarea programada (cada minuto) cancela las citas `PENDIENTE_PAGO` vencidas con la observación "Tiempo de reserva vencido" y libera sus bloques.

---

## 8. Reglas de negocio

| ID | Regla | Origen |
| --- | --- | --- |
| RN-01 | Un bloque de horario pertenece a lo sumo a **una cita activa**. Se garantiza en BD (§11) | Entrevista |
| RN-02 | El tipo se detecta al reservar: **CONTROL** si el paciente tiene alguna cita `ATENDIDA` con ese médico; si no, **NUEVO** | P-03, N-03 |
| RN-03 | Tarifas por defecto: NUEVO = 60 min / S/ 150; CONTROL = 30 min / S/ 80 (tabla `tarifa`, configurable en BD) | P-05 |
| RN-04 | Una reserva sin constancia se cancela automáticamente a los **15 min** y libera el horario | P-04, N-18 |
| RN-05 | Estados de la cita y transiciones (diagrama) | P-08, N-06, N-07 |
| RN-06 | Solo se reserva desde **mañana** hasta **30 días** después (parámetros del consultorio) | P-10, N-09 |
| RN-07 | El médico valida el pago antes de atender: no se puede marcar `ATENDIDA` sin el pago `VALIDADO`. Rechazar el pago cancela la cita con observación | P-06, N-19 |
| RN-08 | El horario se configura por médico **en la BD**; seed: lunes a sábado, 09:00–13:00, en bloques de 30 min. Una cita debe terminar dentro del horario | P-02, N-04, N-17 |
| RN-09 | Un paciente solo puede tener **una cita activa** (`PENDIENTE_PAGO` o `PROGRAMADA`) a la vez. Se garantiza en BD (§11) | N-21 |
| RN-10 | El sistema no almacena información clínica | Alcance |
| RN-11 | Constancia: JPG, PNG o PDF de hasta 5 MB; n.º de operación opcional | N-16 |
| RN-12 | El n.º de documento identifica a un único usuario y no se puede modificar; el correo es único. Documentos aceptados: DNI y CE | N-01, N-20 |
| RN-13 | Solo el médico cancela, y siempre con observación. El paciente no cancela ni reprograma | P-07, N-06 |
| RN-14 | Solo se marcan atendida / no asistió citas cuya hora de inicio ya llegó | 🟡 A-05 |

### RN-05 — Estados de la cita y del pago

Diagramas: [F-04 Estados de la cita](docs/diagramas/funcionales.md#f-04--estados-de-la-cita) y [F-05 Estados del pago](docs/diagramas/funcionales.md#f-05--estados-del-pago).

| Cita | Transición | Quién |
| --- | --- | --- |
| → `PENDIENTE_PAGO` | Elige un horario (aparta 15 min) | Paciente |
| `PENDIENTE_PAGO` → `PROGRAMADA` | Adjunta la constancia | Paciente |
| `PENDIENTE_PAGO` → `CANCELADA` | Vencen los 15 min / cancelación con observación | Sistema / Médico |
| `PROGRAMADA` → `ATENDIDA` | Atiende (requiere pago `VALIDADO`) | Médico |
| `PROGRAMADA` → `NO_ASISTIO` | El paciente no asistió | Médico |
| `PROGRAMADA` → `CANCELADA` | Cancelación con observación o rechazo del pago | Médico |

Estados del pago: `POR_VALIDAR` → `VALIDADO` | `RECHAZADO` (médico; rechazar cancela la cita).

Cita **activa** = `PENDIENTE_PAGO` o `PROGRAMADA`. Bloque **libre** = no bloqueado y sin cita activa. Ambos se derivan; no se guardan como estado aparte.

---

## 9. Requerimientos no funcionales

| ID | Categoría | Requerimiento | Cómo se verifica | APF3 | Final |
| --- | --- | --- | --- | --- | --- |
| RNF01 | Restricción | Java como lenguaje principal (≥ 50 % del stack) | Stack y repositorio | ✔ | ✔ |
| RNF02 | Usabilidad | Las 11 pantallas son responsive (desde 360 px de ancho) | Capturas móvil y escritorio | ✔ | ✔ |
| RNF03 | Seguridad | Contraseñas con bcrypt; acceso por rol; sesión con expiración; bloqueo de 15 min tras 5 intentos fallidos de login (Guava) | Prueba de acceso | ✔ | ✔ |
| RNF04 | Seguridad | Consultas parametrizadas (JPA), validación en servidor (Bean Validation), protección CSRF | Revisión de código | ✔ | ✔ |
| RNF05 | Integridad | RN-01 y RN-09 garantizadas por constraints en BD y operaciones transaccionales | Prueba de concurrencia | ✔ | ✔ |
| RNF06 | Seguridad | Archivos: validar tipo real y tamaño, renombrar con UUID, guardar fuera de `static/`, servir solo al dueño y al médico | Prueba con archivo inválido | ✔ | ✔ |
| RNF07 | Privacidad | Solo datos administrativos mínimos (Ley 29733); los logs no registran contraseñas ni documentos completos | Revisión de logs | ✔ | ✔ |
| RNF08 | Rendimiento | Disponibilidad y agenda en < 2 s con 1 000 citas | Datos de carga | | ✔ |
| RNF09 | Usabilidad | Reserva en ≤ 4 pantallas desde el login; contador visible de los 15 min; mensajes de error claros | Prueba de usabilidad | ✔ | ✔ (con usuarios) |
| RNF10 | Mantenibilidad | MVC + DAO + SOLID; Javadoc en servicios; README y `docs/` | Revisión de código | ✔ | ✔ |
| RNF11 | Calidad | TDD: pruebas unitarias de todas las RN escritas antes de su implementación. Cobertura ≥ 60 % en `service` para la entrega final | JUnit + JaCoCo | ✔ (RN) | ✔ (cobertura) |
| RNF12 | Portabilidad | Se ejecuta en localhost con Java 21 y MySQL 8.4 siguiendo este README | Instalación limpia | ✔ | ✔ |
| RNF13 | Auditoría | `creado_en` / `actualizado_en`; log de login, reserva, validación, rechazo y cancelación (Logback) | Modelo físico + logs | ✔ | ✔ |
| RNF14 | Rúbrica | Uso real de Guava, Apache Commons, Apache POI y Logback (§12.3) | `pom.xml` + código | ✔ | ✔ |
| RNF15 | Rúbrica | Todos los avances y entregables versionados en GitHub | Historial de commits | ✔ | ✔ |

---

## 10. Pantallas, casos de uso y trazabilidad

### 10.1 Inventario de pantallas (100 % en el APF3)

| # | Pantalla | Rol | RF | Contenido principal |
| --- | --- | --- | --- | --- |
| P0 | Login | Ambos | RF07 | N.º de documento, contraseña, enlace a registro, mensaje de bloqueo |
| P1 | Registro | Paciente | RF02 | Formulario con validaciones |
| P2 | Reservar cita (horarios) | Paciente | RF01, RF05 | Tipo detectado y tarifa; calendario de 1–30 días; bloques libres; aviso si ya tiene una cita activa |
| P3 | Pago Yape (simulado) | Paciente | RF03 | Monto, QR de ejemplo, contador de 15 min, subida de constancia, n.º de operación |
| P4 | Confirmación | Paciente | RF15 | Datos de la cita + "Descargar PDF" |
| P5 | Mis citas | Paciente | RF10, RF15, RF03 | Historial; "Continuar pago"; "Descargar PDF" |
| P6 | Mi perfil | Paciente | RF11 | Datos editables y cambio de contraseña |
| P7 | Agenda | Médico | RF04 | Lista con filtros por día, semana y estado |
| P8 | Detalle de cita | Médico | RF08, RF12, RF13, RF14 | Datos, constancia, botones Validar / Rechazar / Atendida / No asistió / Cancelar (con modal de observación) |
| P9 | Reporte de citas | Médico | RF09 | Filtros por día, semana y estado; tabla; "Exportar Excel" |
| P10 | Reporte de pagos | Médico | RF09 | Filtro por período; tabla y total; "Exportar Excel" |

Elementos comunes: barra de navegación por rol, cierre de sesión y páginas de error 403/404.

### 10.2 Casos de uso

| CU | Nombre | Actor | RF | RN | Pantallas |
| --- | --- | --- | --- | --- | --- |
| CU01 | Registrarse | Paciente | RF02 | RN-12 | P1 |
| CU02 | Iniciar / cerrar sesión | Ambos | RF07 | — | P0 |
| CU03 | Consultar disponibilidad | Paciente | RF05, RF06 | RN-02, RN-03, RN-06, RN-08 | P2 |
| CU04 | Reservar y adjuntar constancia | Paciente | RF01, RF03, RF16 | RN-01, RN-04, RN-09, RN-11 | P2 → P3 → P4 |
| CU05 | Ver historial y descargar PDF | Paciente | RF10, RF15 | — | P5, P4 |
| CU06 | Actualizar mis datos | Paciente | RF11 | RN-12 | P6 |
| CU07 | Consultar agenda | Médico | RF04 | — | P7 |
| CU08 | Validar o rechazar pago | Médico | RF12, RF13 | RN-07, RN-13 | P8 |
| CU09 | Atendida / no asistió / cancelar | Médico | RF08, RF14 | RN-05, RN-07, RN-13, RN-14 | P8 |
| CU10 | Generar y exportar reportes | Médico | RF09 | — | P9, P10 |

Diagramas: [F-01 Casos de uso](docs/diagramas/funcionales.md#f-01--casos-de-uso) · [F-06 Mapa de navegación](docs/diagramas/funcionales.md#f-06--mapa-de-navegación).

Flujo del paciente: `P0/P1 → P2 → P3 (15 min) → P4 (PDF) → P5 → P6`
Flujo del médico: `P0 → P7 → P8 (constancia → validar/rechazar → atendida / no asistió / cancelar) → P9 / P10`

### 10.3 Prototipo UX/UI

El prototipo de MEDIFAST se elaboró en Figma y representa las pantallas
P0–P10 definidas en el inventario de interfaces.

**Prototipo Figma:** [MEDIFAST — Prototipo P0–P10](PEGAR_AQUI_EL_ENLACE_PUBLICO_DE_FIGMA)

El prototipo mantiene trazabilidad con los requerimientos funcionales y los
casos de uso descritos anteriormente.

Las pantallas contempladas son:

- **P0:** Login.
- **P1:** Registro.
- **P2:** Reservar cita / horarios disponibles.
- **P3:** Pago Yape simulado.
- **P4:** Confirmación de cita.
- **P5:** Mis citas.
- **P6:** Mi perfil.
- **P7:** Agenda del médico.
- **P8:** Detalle de cita.
- **P9:** Reporte de citas.
- **P10:** Reporte de pagos.

Para el APF3, las interfaces implementadas en Thymeleaf deben conservar
coherencia funcional y visual con este prototipo y cubrir el alcance
comprometido del proyecto.

## 11. Modelo de datos

Modelo para **MySQL 8.4** (InnoDB, utf8mb4), normalizado hasta 3FN. Reemplaza `script.sql` y los diagramas `modelado.drawio` / `modelado_*.png`. Admite varios consultorios y médicos.

**Diagramas y diccionario:** [docs/diagramas/base-de-datos.md](docs/diagramas/base-de-datos.md)

| # | Contenido |
| --- | --- |
| [BD-01](docs/diagramas/base-de-datos.md#bd-01--modelo-conceptual) | Modelo conceptual: entidades y relaciones |
| [BD-02](docs/diagramas/base-de-datos.md#bd-02--modelo-lógico) | Modelo lógico: atributos, claves y cardinalidades |
| [BD-03](docs/diagramas/base-de-datos.md#bd-03--modelo-físico-mysql-84) | Modelo físico: tipos MySQL, nulos, defaults, columnas generadas |
| [Restricciones e índices](docs/diagramas/base-de-datos.md#restricciones-e-índices) | UNIQUE, CHECK, índices y FK (incluye RN-01 y RN-09) |
| [Diccionario de datos](docs/diagramas/base-de-datos.md#diccionario-de-datos) | Cada campo de las 10 tablas |

**Tablas (10):** `consultorio`, `usuario`, `paciente`, `medico`, `tarifa`, `horario_semanal`, `horario`, `cita`, `cita_horario`, `pago`.

### Decisiones de diseño

- **RN-01 y RN-09 en MySQL:** MySQL no tiene índices únicos parciales, así que se usan columnas generadas (`horario_activo`, `paciente_activo`) que valen `NULL` cuando la fila deja de estar activa, con `UNIQUE`. MySQL permite varios `NULL`: al cancelar o atender, el horario y el paciente quedan libres automáticamente, y la BD rechaza duplicados aunque haya dos peticiones simultáneas.
- **Cancelar o rechazar** = `cita.estado = 'CANCELADA'` + `cita_horario.activo = FALSE` (+ `pago.estado = 'RECHAZADO'` si se rechaza), en la misma transacción.
- **Fecha y hora de la cita** no se repiten en `cita`: salen de sus bloques mediante la vista `v_cita` (inicio = MIN, fin = MAX).
- **`tipo_cita` y `pago.monto` sí se guardan:** fijan el tipo y el precio vigentes al reservar.
- **`pago` se crea al adjuntar la constancia**; mientras la cita está en `PENDIENTE_PAGO` no existe la fila.
- **El documento vive en `usuario`** porque es el login y no se edita (RN-12).
- **Generación de bloques:** al iniciar la aplicación y una vez al día, a partir de `horario_semanal`. El seed ya trae los bloques.
- **Constancias** en la carpeta local `uploads/comprobantes/`; la BD guarda la ruta y el tipo MIME.

### Datos de prueba (seed)

- Consultorio **Medifast** (1–30 días, bloques de 30 min, apartado de 15 min).
- 1 médico: Dr. Moisés Barrantes Cabrera, Medicina Interna y Reumatología, con su usuario.
- Tarifas: NUEVO 60 min / S/ 150; CONTROL 30 min / S/ 80.
- Horario semanal de lunes a sábado, 09:00–13:00; bloques desde 30 días atrás hasta 30 días adelante.
- ~30 pacientes ficticios (DNI y algunos CE) con usuario y una contraseña de prueba documentada.
- Citas en todos los estados (`ATENDIDA`, `NO_ASISTIO`, `CANCELADA` por sistema, por médico y por pago rechazado, `PROGRAMADA` con pago por validar y validado, 1 `PENDIENTE_PAGO`), con pagos y constancias de ejemplo, respetando una cita activa por paciente.

### Consultas a entregar ([`docs/sql/03_consultas.sql`](docs/sql/03_consultas.sql))

1. Vista `v_cita` (cita + paciente + médico + inicio/fin + estado del pago).
2. SELECT con JOIN de verificación de todas las relaciones.
3. Reporte 1: citas por estado en un rango de fechas (día/semana).
4. Reporte 2: pagos validados por período (cantidad y total en S/).
5. Disponibilidad: bloques libres de un médico en una fecha.
6. Pruebas de las constraints: INSERT que **deben fallar** (bloque duplicado, segunda cita activa).

---

## 12. Arquitectura, principios y stack

### 12.1 Arquitectura

Monolito Spring Boot en capas (MVC) en localhost: **Spring Security → Controladores (Spring MVC) → Servicios (reglas de negocio) → DAO (Spring Data JPA) → MySQL**, con vistas Thymeleaf, tareas programadas y Logback como componentes transversales.

Diagramas: [T-01 Arquitectura MVC por capas](docs/diagramas/tecnicos.md#t-01--arquitectura-mvc-por-capas) · [T-02 Clases por capas](docs/diagramas/tecnicos.md#t-02--clases-por-capas-mvc-servicios-y-dao) · [T-04](docs/diagramas/tecnicos.md#t-04--secuencia-reservar-y-adjuntar-la-constancia) y [T-05](docs/diagramas/tecnicos.md#t-05--secuencia-validar-pago-y-registrar-la-atención) Secuencias.

### 12.2 Principios exigidos por la rúbrica (≥ 3; se aplican los 4)

| Principio | Cómo se aplica | Evidencia |
| --- | --- | --- |
| **MVC** | Modelo = entidades JPA y DTO; Vista = plantillas Thymeleaf (P0–P10); Controlador = `@Controller` delgado que solo delega en los servicios | Paquetes `model/`, `templates/`, `controller/` |
| **DAO** | Interfaces `CitaDao`, `HorarioDao`, `PagoDao`, etc. que extienden `JpaRepository` (aceptado por el docente, N-25), más consultas propias (`@Query`) para disponibilidad y reportes. Los servicios dependen solo de esas interfaces | Paquete `dao/` |
| **SOLID** | **S**: un servicio por responsabilidad (`ReservaService`, `PagoService`, `ReporteService`, `AlmacenamientoService`, `ConfirmacionPdfService`). **O/L**: `ExportadorReporte` con implementación Excel. **I**: interfaces pequeñas. **D**: inyección por constructor de interfaces | Diagrama de clases + código |
| **TDD** | Se escribe la prueba (JUnit 5 + Mockito) **antes** de cada regla: RN-01 a RN-14. Commits "test → impl" visibles en GitHub | `src/test/`, historial de commits, reporte JaCoCo |

**Seguridad:** bcrypt; roles con Spring Security; CSRF; validación en servidor (Bean Validation); consultas parametrizadas; subida de archivos validada por tipo real, tamaño y nombre UUID; bloqueo por intentos fallidos; control de acceso a constancias y PDF; logs sin datos sensibles; usuario de BD con privilegios mínimos.

### 12.3 Librerías exigidas por la rúbrica

| Librería | Uso real en MEDIFAST |
| --- | --- |
| **Google Guava** | `Preconditions` en los servicios; caché con expiración (`CacheBuilder`) para contar intentos de login fallidos y bloquear 15 min; colecciones inmutables para catálogos (tipos de documento, estados) |
| **Apache Commons Lang3** | `StringUtils` para normalizar nombres y documentos; `Validate` en constructores de DTO |
| **Apache Commons IO** | `FilenameUtils` / `FileUtils` para guardar constancias de forma segura (extensión, ruta y tamaño) |
| **Apache POI** | Exportación de los reportes 1 y 2 a .xlsx |
| **Logback** | `logback-spring.xml` con log de consola y archivo rotativo; eventos de auditoría (login, reserva, validación, rechazo, cancelación) |

### 12.4 Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 (LTS) |
| Framework | Spring Boot 4.1 (Web MVC, Thymeleaf, Data JPA, Security, Validation) |
| BD | MySQL 8.4 LTS (`docs/sql/`; `docker-compose.yml` para desarrollo) |
| Librerías de apoyo | Google Guava, Apache Commons Lang3 e IO, Apache POI, Logback |
| PDF | OpenPDF (confirmación de cita, RF15) |
| Build | Maven |
| Pruebas | JUnit 5, Mockito, Spring Boot Test, JaCoCo |
| Versionado | Git + GitHub |
| Prototipo | Figma — prototipo P0–P10 (🟡 pendiente de agregar enlace público) |
| Diagramas | Mermaid en `docs/diagramas/` (funcionales, técnicos y base de datos); exportación a PNG con Mermaid Live o Mermaid CLI; Bizagi solo si se exige notación BPMN formal |

---

## 13. Plan de trabajo

**XP adaptada a un solo desarrollador:** entregas pequeñas y funcionales; TDD en los servicios; commits pequeños y frecuentes en GitHub (evidencia de la rúbrica); historias de usuario en GitHub Issues; demostración al doctor en cada incremento.

### 13.1 Situación (20-oct-2026)

- Faltan **4 días para el APF3** y **5 semanas para la entrega final**; trabaja una sola persona.
- Por N-24, el APF3 debe tener **las 11 pantallas y los 16 RF**. No queda alcance funcional para después: las 5 semanas siguientes son de calidad, validación y documentación.
- Para que entre en 4 días, las pantallas se construyen con plantillas Bootstrap compartidas (layout, formularios, tablas, modales) y los servicios se prueban con TDD solo en las reglas de negocio.

### 13.2 Plan hasta el APF3 (20 al 24 de octubre)

| Día | Objetivo | Tareas | Listo cuando… |
| --- | --- | --- | --- |
| **Mar 20** | BD + repositorio | Crear el **repositorio GitHub** y subir README y documentos · `docs/sql/01_schema.sql`, `02_datos_prueba.sql`, `03_consultas.sql` · ejecutarlos en MySQL 8.4 · capturas · commit | Scripts sin errores en una BD vacía; las INSERT que deben fallar fallan; todo está en GitHub |
| **Mié 21** | Arquitectura + cuentas | Spring Boot con Guava, Commons, POI, OpenPDF y Logback · paquetes MVC/DAO · layout Bootstrap común · Spring Security (login por documento, roles, bloqueo con Guava) · **P0, P1, P6** (RF07, RF02, RF11) · pruebas de RN-12 primero | Registro, login y perfil funcionando; el médico del seed inicia sesión; pruebas verdes |
| **Jue 22** | Flujo del paciente | **P2, P3, P4, P5**: disponibilidad (RF05, RF06), reserva con apartado y una cita activa (RF01), pago con constancia y retomar pago (RF03), vencimiento (RF16), confirmación + PDF (RF15), historial (RF10) · pruebas de RN-01 a RN-04, RN-06, RN-08, RN-09, RN-11 | El paciente reserva, paga, descarga el PDF y ve su historial; la reserva duplicada y la segunda cita activa fallan |
| **Vie 23** | Flujo del médico + reportes | **P7, P8, P9, P10**: agenda con filtros (RF04), constancia (RF12), validar/rechazar (RF13), atendida / no asistió (RF08), cancelar (RF14), reportes con Excel (RF09) · pruebas de RN-05, RN-07, RN-13, RN-14 | Las 11 pantallas funcionan; los .xlsx se descargan |
| **Sáb 24** | Informe + entrega | Revisión responsive de P0–P10 · informe APF3 (arquitectura, principios, librerías, seguridad, capturas de las 11 pantallas, enlace a GitHub) · correcciones de MySQL y Medifast en los APF y el Charter · ensayo de la demo · **entrega** | APF3 entregado; documentación y código alineados |

**Si falta tiempo, se simplifica sin quitar pantallas ni RF:** vista previa de la constancia → solo descarga · PDF con diseño mínimo · filtro "semana" con un selector de fecha simple · estilos básicos. **No se recorta**: ninguna pantalla, ningún RF, las 4 librerías, las pruebas de las RN ni los commits.

**Disciplina diaria:** al menos 1 commit por historia terminada, con mensajes descriptivos (`test: …`, `feat: …`, `docs: …`); `git push` al final de cada día.

### 13.3 Plan después del APF3 (26-oct al 24-nov)

| Semana | Fechas | Contenido |
| --- | --- | --- |
| 1 | 26-oct – 01-nov | Correcciones según la retroalimentación del APF3 · pulir lo simplificado (vista previa, PDF, estilos) · Figma y BPMN TO-BE alineados con las 11 pantallas |
| 2 | 02-nov – 08-nov | Cobertura de pruebas ≥ 60 % en `service` · pruebas de integración · revisión de seguridad · prueba de rendimiento (RNF08) |
| 3 | 09-nov – 15-nov | Prueba de usabilidad con 3 usuarios · validación con el Dr. Barrantes (evidencia) · correcciones |
| 4 | 16-nov – 22-nov | Manual de usuario · Javadoc + `docs/` · diagrama de clases final · informe final |
| 5 | 23-nov – 24-nov | Ensayo desde una instalación limpia · **sustentación el 24-nov** |

### 13.4 Riesgos

| Riesgo | Prob. | Impacto | Respuesta |
| --- | --- | --- | --- |
| **11 pantallas y 16 RF en 4 días con una persona** | Alta | Alto | Layout y componentes Bootstrap reutilizables; plan diario con criterio de "listo"; se simplifica la presentación, nunca se quita un RF |
| **Autoría / dominio del código en la sustentación** | Media | Alto | Código simple y homogéneo; `docs/decisiones/`; repasar cada módulo antes del 24-oct y del 24-nov |
| Librerías agregadas "de adorno" | Media | Medio | Cada una con el uso concreto de §12.3, visible en el código |
| Documentación desalineada con el código | Media | Medio | README como fuente única; actualizarlo en el mismo commit que el código |
| Lógica de bloques (60 min = 2 bloques) y vencimiento de 15 min | Media | Medio | TDD; el seed cubre todos los casos |
| Reservas duplicadas o dos citas activas | Baja | Alto | Constraints `UNIQUE` en columnas generadas + transacción + prueba |

---

## 14. Entregables y rúbrica APF3

### 14.1 Cobertura de la rúbrica

Criterio de la rúbrica	Cómo se aborda en MEDIFAST	Evidencia esperada	Estado
Arquitectura inicial con MVC, TDD, DAO, SOLID y seguridad	El proyecto se organiza mediante Spring MVC, servicios, DAO con Spring Data JPA, interfaces e implementaciones de servicio, pruebas unitarias y Spring Security.	Código en medifast-app/, paquetes controller/, service/, dao/, model/, security/ y src/test/.	🟡 Pendiente de verificación completa
Uso de Google Guava, Apache Commons, Apache POI y Logback	Cada librería tiene un uso concreto dentro del sistema y no se incorpora únicamente como dependencia.	pom.xml y clases donde se utilicen realmente las librerías.	🟡 Pendiente de verificación completa
Git y GitHub con evidencia de los avances	Los avances del proyecto se versionan mediante commits descriptivos en la rama principal del repositorio.	Historial de commits y archivos versionados en GitHub.	🟡 En progreso
Interfaces UI que cubren el 100 % del alcance comprometido	El inventario P0–P10 contempla las pantallas relacionadas con los requerimientos funcionales definidos para el APF3.	Código Thymeleaf, capturas y demostración funcional de P0–P10.	🟡 En desarrollo
Solución completa, coherente, con buenas prácticas y autoría	README, código, scripts SQL, diagramas e informe deben permanecer alineados durante el desarrollo.	Repositorio, informe APF3 y sustentación.	🟡 En progreso
Sustentación oral organizada y coherente	La exposición seguirá la secuencia problema → alcance → arquitectura → implementación → pruebas → control de versiones → demostración → próximos pasos.	Presentación y demostración del sistema.	🟡 Pendiente

Importante: un criterio solo debe cambiarse a ✅ Completo cuando exista evidencia verificable dentro del repositorio o durante la demostración.

### 14.2 Checklist de entregables

| Entregable | APF3 (24-oct) | Final (24-nov) | Estado |
| --- | --- | --- | --- |
| README (planificación consolidada) | ✔ | ✔ | ✅ sin preguntas abiertas |
| Repositorio GitHub con historial de avances | ✔ | ✔ | ⬜ |
| Scripts SQL: esquema, datos, consultas | ✔ | | ⬜ |
| Diagramas de base de datos: conceptual, lógico, físico + restricciones | ✔ | | ✅ (BD-01 a BD-03) |
| Diccionario de datos | ✔ | | ✅ borrador |
| Aplicación: 11 pantallas, 16 RF | ✔ | | ⬜ |
| Pruebas unitarias (TDD) de RN-01 a RN-14 | ✔ | ✔ (cobertura ≥ 60 %) | ⬜ |
| Informe APF3 (arquitectura, principios, librerías, seguridad, capturas) | ✔ | | ⬜ |
| Correcciones a APF1, APF2 y Charter | ✔ | | ⬜ |
| Diagramas funcionales: casos de uso, AS-IS, TO-BE, estados, navegación, cronograma | ✔ | ✔ | ✅ borrador (F-01 a F-07) |
| Diagramas técnicos: arquitectura MVC, clases, secuencia | ✔ | ✔ | ✅ borrador (T-01 a T-05) |
| Prototipo Figma actualizado (P0–P10) | Deseable | ✔ | 🟡 |
| Prueba de usabilidad y validación del doctor | | ✔ | ⬜ |
| `docs/`, Javadoc, manual de usuario | | ✔ | ⬜ |
| Informe final y sustentación | | ✔ | ⬜ |

---

## 15. Estructura del repositorio

Monorepo: documentación, scripts de BD y aplicación en un solo repositorio.

```text
medifast/
├── README.md                    # este documento (fuente única de verdad)
├── docker-compose.yml           # MySQL 8.4 con el esquema y los datos de prueba
├── docs/
│   ├── diagramas/               # Mermaid: README (índice), funcionales, tecnicos, base-de-datos
│   ├── sql/                     # 01_schema.sql, 02_datos_prueba.sql, 03_consultas.sql
│   └── entregables/             # APF1, APF2, Charter, rúbrica (PDF)
└── medifast-app/                # Spring Boot + Thymeleaf
    ├── pom.xml
    └── src/
        ├── main/java/pe/edu/utp/medifast/
        │   ├── controller/      # MVC: controladores
        │   ├── service/         # interfaces de servicio
        │   │   └── impl/        # implementaciones (SOLID)
        │   ├── dao/             # DAO: interfaces que extienden JpaRepository
        │   ├── model/           # entidades JPA y enums
        │   ├── dto/
        │   ├── security/        # UserDetailsService, bloqueo por intentos (Guava)
        │   ├── util/            # exportadores (POI), PDF (OpenPDF), almacenamiento (Commons IO)
        │   └── config/          # SecurityConfig
        ├── main/resources/
        │   ├── templates/       # MVC: vistas Thymeleaf (P0–P10), fragments/, error/
        │   ├── static/
        │   ├── application.yml
        │   └── logback-spring.xml
        └── test/java/...        # TDD: JUnit 5 + Mockito
```

Fuera de Git: `uploads/` (constancias, RNF06), `logs/`, `target/` y los `.docx` fuente de los informes.

### Cómo ejecutar

Requisitos: Java 21 o superior, Docker (o un MySQL 8.4 local). Maven no hace falta: se usa `./mvnw`.

```bash
# 1. Base de datos: crea el esquema y carga los datos de prueba
docker compose up -d

# 2. Aplicación
cd medifast-app
./mvnw spring-boot:run
# http://localhost:8080
```

Sin Docker, ejecuta los scripts en un MySQL 8.4 local:

```bash
mysql -u root -p < docs/sql/01_schema.sql
mysql -u root -p < docs/sql/02_datos_prueba.sql
mysql --force -u root -p medifast < docs/sql/03_consultas.sql   # la sección 6 contiene errores esperados
```

**Cuentas de prueba** (contraseña `Medifast2026`): médico `10000001`; pacientes `70000001` a `70000025` (DNI) y `001000026` a `001000030` (CE).

**Variables de entorno** (opcionales; los valores por defecto apuntan al contenedor): `MEDIFAST_DB_URL`, `MEDIFAST_DB_USER`, `MEDIFAST_DB_PASSWORD`, `MEDIFAST_UPLOADS_DIR`.

**Pruebas y cobertura:** `./mvnw test` → reporte en `medifast-app/target/site/jacoco/index.html`.

Reiniciar la BD desde cero: `docker compose down -v && docker compose up -d`.
