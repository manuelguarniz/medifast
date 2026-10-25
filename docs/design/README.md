# Diseño de interfaces — MEDIFAST

Bitácora del diseño de las 11 pantallas (P0–P10) con [Stitch](https://stitch.withgoogle.com/). Aquí se guardan los prompts, cada resultado generado y el feedback que llevó a la siguiente versión. Cuando una pantalla queda aprobada, se pasa a Figma.

La referencia funcional es el [README del proyecto](../../README.md) (§3 Alcance, §4 Requerimientos, §5 Reglas de negocio, §7 Pantallas). Si un diseño lo contradice, se corrige el diseño.

## Archivos

| Archivo | Contenido |
| --- | --- |
| [design_system.png](design_system.png) | Sistema de diseño: tokens Bootstrap, tipografía, componentes y estados |
| [00-prompt-base.md](00-prompt-base.md) | Prompt de contexto que se pega primero en cada sesión de Stitch |
| [01-prompts-pantallas.md](01-prompts-pantallas.md) | Prompts iniciales (v1) de P0–P10 |
| `Pn-nombre/iteraciones.md` | Historial de la pantalla: prompt → resultado → feedback → siguiente prompt |
| `Pn-nombre/vN.png` | Captura del resultado de Stitch en la versión N |

## Flujo de trabajo

1. Abrir una sesión en Stitch y pegar [00-prompt-base.md](00-prompt-base.md).
2. Pegar el prompt v1 de la pantalla desde [01-prompts-pantallas.md](01-prompts-pantallas.md). En las pantallas siguientes a P0, adjuntar la versión aprobada de P0 como referencia de estilo.
3. Guardar la captura del resultado como `Pn-nombre/v1.png` (sin espacios en el nombre).
4. Revisarla con el checklist de abajo, anotar el feedback en `iteraciones.md` y redactar el prompt v2. Repetir hasta aprobarla.
5. Marcar la pantalla como ✅ en la tabla de estado y pasarla a Figma.

### Lecciones aprendidas

- **Una pantalla por corrección:** para corregir, seleccionar en Stitch solo la pantalla afectada. Las instrucciones generales ("en escritorio y móvil") se suelen aplicar solo a una de ellas.
- **Variantes en un prompt aparte:** las pantallas nuevas (otro estado, otra variante) se piden en un prompt aparte, no dentro de un prompt de corrección.
- **Tomar como referencia lo que ya está bien:** "copia el estilo del frame X" funciona mejor que volver a describir los tokens.
- **Límite de iteraciones:** si un detalle no se corrige en 2–3 iteraciones, se ajusta en Figma y se anota en `iteraciones.md`.

## Checklist de revisión

- [ ] Textos en español y la marca como **Consultorio Medifast** (nunca "Barrantes" como nombre del consultorio).
- [ ] Colores, tipografía (Inter), radios de 8px y espaciado de 8px según [design_system.png](design_system.png).
- [ ] Etiquetas de estado según README §5.2: *Pago pendiente, Confirmada, Atendida, No asistió, Cancelada* / *Por validar, Verificado, Rechazado*.
- [ ] Nada fuera de alcance (README §3.2): sin selector de rol, recuperación de contraseña, correos/SMS/WhatsApp, información clínica, telemedicina, reprogramación, cancelación por el paciente.
- [ ] Versión de escritorio y móvil (360 px) coherentes entre sí: mismos componentes y mismos textos.
- [ ] Estados de error, vacío y deshabilitado cuando apliquen.

## Estado

| # | Pantalla | Carpeta | Versión actual | Estado |
| --- | --- | --- | --- | --- |
| P0 | Login | [P0-login](P0-login/iteraciones.md) | v3 | ✅ Aprobada (2 ajustes pendientes en Figma) |
| P1 | Registro | — | — | ⏳ Pendiente |
| P2 | Reservar cita | — | — | ⏳ Pendiente |
| P3 | Pago Yape (simulado) | — | — | ⏳ Pendiente |
| P4 | Confirmación | — | — | ⏳ Pendiente |
| P5 | Mis citas | — | — | ⏳ Pendiente |
| P6 | Mi perfil | — | — | ⏳ Pendiente |
| P7 | Agenda | — | — | ⏳ Pendiente |
| P8 | Detalle de cita | — | — | ⏳ Pendiente |
| P9 | Reporte de citas | — | — | ⏳ Pendiente |
| P10 | Reporte de pagos | — | — | ⏳ Pendiente |
| — | Errores 403 / 404 | — | — | ⏳ Pendiente |
