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

Con Claude Code, este flujo está automatizado en la skill [`/diseno-stitch`](../../.claude/skills/diseno-stitch/SKILL.md).

1. Abrir una sesión en Stitch y pegar [00-prompt-base.md](00-prompt-base.md).
2. Pegar el prompt v1 de la pantalla desde [01-prompts-pantallas.md](01-prompts-pantallas.md). En las pantallas siguientes a P0, adjuntar la versión aprobada de P0 como referencia de estilo.
3. Guardar la captura del resultado como `Pn-nombre/v1.png` (sin espacios en el nombre).
4. Revisarla con el checklist de abajo, anotar el feedback en `iteraciones.md` y redactar el prompt v2. Repetir hasta aprobarla.
5. Marcar la pantalla como ✅ en la tabla de estado y pasarla a Figma.

### Lecciones aprendidas

- **Una pantalla por corrección:** para corregir, seleccionar en Stitch solo la pantalla afectada. Las instrucciones generales ("en escritorio y móvil") se suelen aplicar solo a una de ellas.
- **Variantes en un prompt aparte:** las pantallas nuevas (otro estado, otra variante) se piden en un prompt aparte, no dentro de un prompt de corrección.
- **Tomar como referencia lo que ya está bien:** "copia el estilo del frame X" funciona mejor que volver a describir los tokens.
- **Escritorio y móvil por separado:** cuando hay que corregir ambas versiones, conviene usar un prompt para los frames de escritorio y otro para los móviles, y pedir que el segundo copie el estilo del primero. En P1, así se resolvieron 14 problemas en una sola iteración.
- **Contenido exacto en los paneles decorativos:** si no se lista qué va en un panel lateral o una cabecera, Stitch inventa cards (por ejemplo, datos del médico con la especialidad equivocada).
- **Variantes duplicando el frame aprobado:** para un estado nuevo, o para un frame que no se deja corregir, pedir "duplica este frame y cambia solo…" sobre un frame ya correcto. En P2 resolvió en una iteración lo que llevaba tres. Los frames generados en paralelo arrastran elementos inventados (barra de pestañas, textos clínicos) que Stitch luego no quita.
- **Estados por dispositivo:** al pedir estados duplicando un frame, Stitch los genera solo para el dispositivo seleccionado. Pegar el prompt una vez con el frame de escritorio y otra con el móvil.
- **Indicadores calculados de las filas:** si el prompt pide totales que no salen de las filas de ejemplo, Stitch los copia tal cual (P7 mostró 18 citas con 9 filas). Calcularlos antes de escribir el prompt.
- **Rellenos neutros con hex:** excluir el lavanda no basta; hay que dar el color del relleno de cada control (#f1f5f9 o #f8fafc). En P7 volvió a aparecer en el control segmentado, el select y las cabeceras.
- **Títulos literales en los duplicados:** al generar estados, Stitch cambia los títulos de las cards en cada copia (P8: cuatro títulos distintos para la misma card). Escribir el título de cada card en cada estado, aunque no cambie.
- **Pantallas hermanas por duplicado:** si una pantalla es casi igual a otra aprobada (P10 y P9), duplicar el frame aprobado y cambiar solo el contenido resuelve el escritorio a la primera. En el móvil, listar las secciones en orden: "copia los contenidos de escritorio" dejó secciones faltantes y duplicadas.
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
| P0 | Login | [P0-login](P0-login/iteraciones.md) | v3 | ✅ Aprobada (3 ajustes pendientes en Figma) |
| P1 | Registro | [P1-registro](P1-registro/iteraciones.md) | v2 | ✅ Aprobada (6 ajustes pendientes en Figma) |
| P2 | Reservar cita | [P2-reservar](P2-reservar/iteraciones.md) | v3 | ✅ Aprobada (6 ajustes pendientes en Figma) |
| P3 | Pago Yape (simulado) | [P3-pago](P3-pago/iteraciones.md) | v4 | ✅ Aprobada (7 ajustes pendientes en Figma) |
| P4 | Confirmación | [P4-confirmacion](P4-confirmacion/iteraciones.md) | v2 | ✅ Aprobada (5 ajustes pendientes en Figma; falta la variante "Detalle de la cita", prompt en P5) |
| P5 | Mis citas | [P5-mis-citas](P5-mis-citas/iteraciones.md) | v3 | ✅ Aprobada (6 ajustes pendientes en Figma) |
| P6 | Mi perfil | [P6-perfil](P6-perfil/iteraciones.md) | v3 | ✅ Aprobada (3 ajustes pendientes en Figma) |
| P7 | Agenda | [P7-agenda](P7-agenda/iteraciones.md) | v3 | ✅ Aprobada (6 ajustes pendientes en Figma) |
| P8 | Detalle de cita | [P8-detalle-cita](P8-detalle-cita/iteraciones.md) | v3 | ✅ Aprobada (7 ajustes pendientes en Figma) |
| P9 | Reporte de citas | [P9-reporte-citas](P9-reporte-citas/iteraciones.md) | v3 | ✅ Aprobada (5 ajustes pendientes en Figma) |
| P10 | Reporte de pagos | [P10-reporte-pagos](P10-reporte-pagos/iteraciones.md) | v2 | ✅ Aprobada (4 ajustes pendientes en Figma) |
| — | Errores 403 / 404 | — | — | ⏳ Pendiente |
