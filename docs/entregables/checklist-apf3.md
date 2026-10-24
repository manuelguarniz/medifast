# MEDIFAST — Checklist del APF3

Seguimiento de la [rúbrica del APF3](rubrica_apf3.txt): qué evidencia respalda cada criterio y qué falta. La definición del sistema (RF, RN, RNF, pantallas y arquitectura) está en el [README](../../README.md).

**Estados:**

- ✅ **Completo:** existe, está actualizado, funciona y puede demostrarse.
- 🟡 **En progreso:** existe en parte o le falta implementación, evidencia o revisión.
- ❌ **No incluido:** no forma parte del alcance vigente.

Un elemento pasa a ✅ solo cuando hay una implementación, archivo, prueba, captura o demo que lo demuestre; no basta con que esté descrito en el README.

---

## 1. Cobertura de la rúbrica

| Criterio | Evidencia esperada | Estado |
| --- | --- | --- |
| Arquitectura con MVC, TDD, DAO, SOLID y seguridad | `controller/`, `service/` + `impl/`, `dao/` (`JpaRepository`), `model/`, `templates/`, `security/`, pruebas en `src/test/java/`, diagramas T-01 y T-02 | 🟡 En progreso |
| Google Guava, Apache Commons, Apache POI y Logback | `pom.xml` + uso en clases concretas (README §11.3), archivos `.xlsx` generados, logs | 🟡 En progreso |
| Git + GitHub con el 100 % de los avances | Historial de commits pequeños y descriptivos con la evolución real del proyecto | 🟡 En progreso |
| Interfaces que cubren el 100 % del alcance | P0–P10 en Thymeleaf, conectadas con la lógica, capturas en `docs/capturas/` y demo | 🟡 En progreso |
| Producto completo, coherente, con buenas prácticas y autoría | README, código, SQL, diagramas e informe alineados; el equipo explica el código | 🟡 En progreso |
| Sustentación organizada y con dominio técnico | Guion: problema → alcance → arquitectura → implementación → pruebas → GitHub → demo → próximos pasos | 🟡 Pendiente |

---

## 2. Entregables

| Entregable | APF3 | Final | Estado |
| --- | :---: | :---: | --- |
| README con definición, alcance, RF, RN, RNF, pantallas y arquitectura | ✔ | ✔ | ✅ Completo |
| Scripts SQL: esquema, datos de prueba y consultas | ✔ | ✔ | ✅ Completo |
| Modelos conceptual, lógico y físico; restricciones e índices | ✔ | ✔ | ✅ Completo |
| Diccionario de datos | ✔ | ✔ | 🟡 Revisar versión final |
| Diagramas funcionales F-01 a F-06 | ✔ | ✔ | 🟡 Borrador, revisar |
| Diagramas técnicos T-01 a T-05 | ✔ | ✔ | 🟡 Borrador, revisar |
| Prototipo Figma P0–P10 (README §7.2) | Deseable | ✔ | 🟡 Enlace agregado; faltan los ajustes listados en el README |
| Aplicación Spring Boot: 11 pantallas y 16 RF | ✔ | ✔ | 🟡 En progreso (login y seguridad) |
| Spring Security + bcrypt + roles + CSRF | ✔ | ✔ | 🟡 En progreso |
| Uso real de Guava, Commons Lang3/IO, POI y Logback | ✔ | ✔ | 🟡 En progreso |
| Confirmación PDF con OpenPDF | ✔ | ✔ | 🟡 Pendiente |
| Reportes de citas y pagos exportables a Excel | ✔ | ✔ | 🟡 Pendiente |
| Pruebas TDD de RN-01 a RN-14 | ✔ | ✔ | 🟡 En progreso |
| Cobertura ≥ 60 % en `service` (JaCoCo) | | ✔ | 🟡 Pendiente |
| Capturas P0–P10 en `docs/capturas/` | ✔ | ✔ | 🟡 Pendiente |
| Correcciones a APF1, APF2 y Project Charter (MySQL, Consultorio Medifast, alcance) | ✔ | | 🟡 Pendiente |
| Informe APF3 | ✔ | | 🟡 En progreso |
| Javadoc de los servicios | ✔ | ✔ | 🟡 Pendiente |
| Referencias bibliográficas | ✔ | ✔ | 🟡 Pendiente de revisión |
| Manual de usuario | | ✔ | 🟡 Pendiente |
| Prueba de usabilidad con usuarios | | ✔ | 🟡 Pendiente |
| Validación del Dr. Barrantes | | ✔ | 🟡 Pendiente |
| Informe final y sustentación | | ✔ | 🟡 Pendiente |

---

## 3. Verificación antes de entregar

### 3.1 Base de datos y ejecución

- [ ] El proyecto compila y arranca desde una instalación limpia (README §14).
- [ ] `01_schema.sql` y `02_datos_prueba.sql` se ejecutan sin errores en MySQL 8.4.
- [ ] Las consultas de `03_consultas.sql` funcionan y las INSERT de la sección 6 fallan como se espera (RN-01, RN-09).

### 3.2 Arquitectura y librerías

- [ ] Los controladores solo delegan en interfaces de servicio.
- [ ] Los servicios acceden a la BD únicamente a través de los `*Dao`.
- [ ] Los nombres de controladores, servicios y DAO coinciden con T-02.
- [ ] Guava, Commons, POI, OpenPDF y Logback se usan en clases concretas.
- [ ] Los logs registran los eventos de auditoría sin contraseñas ni documentos completos.

### 3.3 Pruebas

- [ ] `./mvnw test` (o `.\mvnw.cmd test`) pasa en verde.
- [ ] Cada regla RN-01 a RN-14 implementada tiene su prueba, con Mockito donde corresponde.
- [ ] El reporte de JaCoCo se genera en `medifast-app/target/site/jacoco/index.html`. El porcentaje de cobertura solo se informa a partir de ese reporte.

### 3.4 Interfaces (P0–P10)

| Pantalla | Debe comprobarse |
| --- | --- |
| P0 Login | Paciente y médico ingresan con DNI o CE; el rol decide la pantalla de destino; credenciales incorrectas muestran error; el bloqueo tras 5 intentos funciona |
| P1 Registro | Campos obligatorios (incluida la fecha de nacimiento); documento y correo no se duplican; contraseña ≥ 8 caracteres e igual a su confirmación; mensajes de error claros |
| P2 Reservar | Solo horarios libres de mañana a +30 días; tipo NUEVO/CONTROL, duración y monto correctos; no permite una segunda cita activa ni un bloque ocupado |
| P3 Pago Yape | Monto correcto; leyenda de simulación; contador de 15 min; constancia obligatoria (JPG/PNG/PDF ≤ 5 MB); se puede retomar mientras la reserva siga vigente |
| P4 Confirmación | Médico, fecha, hora, tipo, monto y estado del pago; el PDF se descarga; no menciona envío por correo |
| P5 Mis citas | Orden de la más reciente a la más antigua; estados de cita y pago; "Continuar pago" y "Descargar PDF" solo cuando corresponden |
| P6 Mi perfil | Edita los datos permitidos; el documento es solo lectura; el cambio de contraseña pide la actual y la confirmación |
| P7 Agenda | Orden por fecha y hora; filtros por día, semana y estado |
| P8 Detalle de cita | Ver constancia; validar; rechazar con observación (cancela la cita); cancelar con observación; atendida / no asistió solo cuando corresponde; no permite atendida sin pago validado |
| P9 Reporte de citas | Filtros por día, semana y estado; indicadores; tabla correcta; `.xlsx` generado con POI |
| P10 Reporte de pagos | Filtro por período; solo pagos validados; cantidad, total y ticket promedio; `.xlsx` generado con POI |

Además:

- [ ] Las 11 pantallas son responsive desde 360 px.
- [ ] Las validaciones también se hacen en el servidor.
- [ ] El acceso se restringe por rol (`/paciente/**`, `/medico/**`).
- [ ] Las pantallas aplican los ajustes del prototipo listados en el README §7.2.

### 3.5 Coherencia de la documentación

- [ ] README, código, SQL, diagramas, prototipo e informe usan las mismas decisiones: Consultorio Medifast, MySQL 8.4, Java 21, Spring Boot + Thymeleaf, P0–P10, RF01–RF16, RN-01–RN-14, Yape simulado.
- [ ] Los informes anteriores ya no mencionan PostgreSQL, tablas clínicas ni requerimientos descartados.

---

## 4. Evidencias

### 4.1 Historial de GitHub

Commits pequeños y descriptivos por funcionalidad; nunca un único commit final. Los `test:` de una regla van antes de su `feat:`. Ejemplos:

```text
test: agregar pruebas de RN-12 para el registro
feat: implementar registro y login de paciente
security: configurar roles y bloqueo de intentos
test: validar RN-01 y RN-09 en el servicio de reservas
feat: implementar disponibilidad y reserva de citas
feat: implementar pago simulado y carga de constancia
feat: implementar agenda y gestión de citas del médico
feat: agregar reportes Excel con Apache POI
docs: actualizar diagramas y documentación del APF3
```

### 4.2 Capturas

Una captura por pantalla de la **aplicación en ejecución** (no del prototipo):

```text
docs/capturas/
├── P0-login.png
├── P1-registro.png
├── P2-reservar.png
├── P3-pago-yape.png
├── P4-confirmacion.png
├── P5-mis-citas.png
├── P6-mi-perfil.png
├── P7-agenda.png
├── P8-detalle-cita.png
├── P9-reporte-citas.png
└── P10-reporte-pagos.png
```

### 4.3 Demo para la sustentación

Desde `http://localhost:8080`, con las cuentas de prueba del README §14:

```text
PACIENTE: Registro / Login → Reservar → Pago simulado → Confirmación (PDF) → Mis citas → Mi perfil
MÉDICO:   Login → Agenda → Detalle de cita → Validar / rechazar / atender → Reportes (Excel)
```

La demo en vivo vale más como evidencia que una secuencia de capturas; ensayarla desde una instalación limpia.
