---
name: diseno-stitch
description: Flujo iterativo para diseñar las pantallas de MEDIFAST (P0–P10, errores 403/404) con Stitch y llevar la bitácora en docs/design/. Usar cuando el usuario pida un diseño, un prompt para Stitch, empezar una pantalla, revisar o dar feedback sobre una captura generada (vN.png o vN.M.png), o aprobar una pantalla para pasarla a Figma.
---

# Diseño de pantallas con Stitch

Cada pantalla se diseña en Stitch por iteraciones: prompt → captura → feedback → siguiente prompt, hasta aprobarla y pasarla a Figma. Todo queda registrado en `docs/design/` para que el historial sea auditable.

## Fuentes de verdad (leer antes de actuar)

- `README.md` (raíz): §3 Alcance y §3.2 Fuera del alcance, §4 Requerimientos y criterios de aceptación, §5 Reglas de negocio y §5.2 Etiquetas de estado, §7 Pantallas y §7.2 Diferencias con el prototipo. **Si un diseño lo contradice, se corrige el diseño.**
- `docs/design/README.md`: flujo, checklist de revisión, lecciones aprendidas y la tabla de estado de las pantallas.
- `docs/design/design_system.png`: tokens y componentes.
- `docs/design/00-prompt-base.md`: el contexto que se pega primero en cada sesión de Stitch.
- `docs/design/01-prompts-pantallas.md`: los prompts v1 de cada pantalla.
- Pantallas ya aprobadas (`Pn-*/iteraciones.md`): sirven como referencia de estilo y muestran el formato a seguir. P0-login es el ejemplo canónico de una pantalla simple; P3-pago y P6-perfil, del flujo en dos fases (principal y luego estados).

## Convenciones de archivos

```
docs/design/Pn-nombre/
├── iteraciones.md        ← bitácora (plantilla en templates/iteraciones.md)
├── referencia-*.png      ← referencias visuales que dio el usuario (opcional)
└── v1.png, v2.1.png, ... ← capturas de Stitch, sin espacios en el nombre
```

- Una captura por versión: `vN.png`. Si la versión tiene varios estados, una captura por estado: `vN.1.png`, `vN.2.png`… (cada una con su frame de escritorio y su móvil). Anotar en `iteraciones.md` qué estado es cada captura.

- Las carpetas se nombran como `P1-registro`, `P2-reservar`, `P3-pago`, `P4-confirmacion`, `P5-mis-citas`, `P6-perfil`, `P7-agenda`, `P8-detalle-cita`, `P9-reporte-citas`, `P10-reporte-pagos` y `errores`.
- Si el usuario guarda una captura con otro nombre (por ejemplo `01. login.png` o `2.1.png`), se mueve a `Pn-nombre/vN.png` (o `vN.M.png`) y se le avisa. Si numera distinto de lo planeado, se respeta su numeración y se anota.
- Las imágenes que el usuario pega en el chat y que sirven de referencia se copian a la carpeta de la pantalla como `referencia-*.png`.

## Modos

Identificar en qué modo está el pedido.

### A. Iniciar una pantalla

1. Crear `Pn-nombre/iteraciones.md` a partir de [templates/iteraciones.md](templates/iteraciones.md), con los RF/RN que aplican y la plantilla Thymeleaf (README §7).
2. Tomar el prompt v1 de `01-prompts-pantallas.md` y revisarlo contra el README antes de entregarlo. Si hay algo fuera de alcance o desactualizado, corregirlo en ambos archivos. (Ejemplo: el v1 de P0 pedía "Recordarme", que choca con RNF03.)
3. **Referencia de estilo:** indicar que se adjunte la última pantalla aprobada **del mismo rol** (la que ya tiene la navbar y los componentes corregidos), no siempre P0. Si el usuario pega una referencia visual, copiarla como `referencia-*.png` y listar en la cabecera de `iteraciones.md` lo que se toma y lo que no se sigue (con la regla del README que lo impide).
4. **Datos de ejemplo coherentes:** reutilizar los mismos datos que las pantallas anteriores (paciente, n.º de cita, fechas, montos), para que el flujo cuente una sola historia. Revisar las bitácoras previas antes de inventar datos.
5. **Solo el estado principal en v1:** el prompt v1 pide un único estado (escritorio + móvil). Los demás estados se dejan redactados en `### Próximos prompts` y se piden al aprobar el principal (ver "Flujo en dos fases").
6. **Navbar descrita, no referida:** en una sesión nueva de Stitch, "igual que Pn" no basta. Describir la distribución: logo a la izquierda (cuadrado menta con el pulso, "MEDIFAST" / "Consultorio Medifast"), enlaces a la derecha en una línea, cuál está activo, sin avatar; en móvil, solo el botón "Menú".
7. Actualizar la tabla de estado en `docs/design/README.md` (🔄 En iteración, v1).
8. Entregar al usuario el prompt listo para copiar, con la instrucción de pegar antes `00-prompt-base.md` si es una sesión nueva de Stitch.

### B. Revisar un resultado (vN.png o vN.M.png)

0. **Verificar los archivos antes de revisar.** Listar la carpeta con fecha de modificación (`ls -lT`). Si el usuario dice que regeneró o reemplazó una captura, volver a leerla aunque ya se haya revisado. Si hay varias capturas de la misma versión, comparar checksums (`md5 -q`): si dos son idénticas, avisar al usuario antes de revisar.
1. **Inspeccionar a fondo.** Las capturas de Stitch suelen ser muy anchas, con varias pantallas una al lado de la otra, y en miniatura los detalles no se ven. Recortar cada pantalla (formulario, cabecera, logo) y leer los recortes en el scratchpad:
   ```bash
   # sips -c <alto> <ancho> --cropOffset <y> <x>  (coordenadas de la imagen original)
   sips -c 1300 1170 --cropOffset 350 2300 docs/design/Pn-x/vN.png --out <scratchpad>/recorte.png
   ```
   Comparar cada pantalla de escritorio con su equivalente móvil: Stitch a veces corrige solo una de las dos.
2. **Evaluar** con el checklist de `docs/design/README.md`, los criterios de aceptación de los RF de la pantalla y los problemas abiertos de la versión anterior. Además:
   - **Coherencia entre pantallas:** comparar los componentes compartidos (logo, píldora, inputs, botones, alertas) con las pantallas ya aprobadas, no solo entre los frames de esta versión.
   - **Huecos de flujo:** preguntar qué pasa antes y después de la pantalla (éxito, error del servidor, redirección). Si el README no lo define, preguntárselo al usuario y registrar la decisión en `### Decisiones de flujo`. Si afecta a otra pantalla, agregar el ajuste también en la bitácora de esa pantalla.
   - **Pedidos que chocan con el README:** si el usuario pide algo que contradice una regla (por ejemplo, un botón "Volver" que liberaría el horario frente a RN-13), no aplicarlo ni rechazarlo en silencio: preguntar con opciones (AskUserQuestion), explicando qué regla se toca en cada una.
   - **Propuestas sin respuesta:** si se propuso una decisión de flujo y el usuario sigue adelante sin comentarla, avisar una vez de que se adoptará tal cual; en la siguiente revisión registrarla como "adoptada sin objeción".
   - **Mensajes completos:** si la pantalla es un formulario, documentar en `iteraciones.md` la tabla completa de mensajes de validación de sus RF (aunque el diseño solo muestre algunos ejemplos), para usarla en la implementación.
3. **Registrar en `iteraciones.md`:**
   - Fila de la versión en la tabla de resumen.
   - Sección `## vN` con la imagen embebida.
   - **Corregido respecto a vN-1** (con referencias #n).
   - **Pendiente**: tabla con #, Problema, Dónde, Severidad (Alta / Media / Baja) y Veces (cuántas iteraciones lleva sin corregirse).
   - **Aprendizaje**, si se descubrió algo sobre cómo responde Stitch.
4. **Decidir:**
   - Si quedan problemas de severidad Alta o Media que Stitch puede resolver, redactar el prompt vN+1 en una nueva sección `## vN+1`. Si el problema afecta a un frame que luego se va a duplicar para los estados (inputs, navbar), corregirlo antes de pedir los estados; si no, se hereda.
   - Si Stitch deja un frame sin cambios tras dos intentos, no volver a describir las correcciones: pedir que **duplique un frame ya correcto** y cambie solo lo propio del estado.
   - Lo que el usuario declare tolerable (por ejemplo, el logo de la navbar) no se sigue reportando como pendiente: se anota una vez y pasa a los ajustes de Figma.
   - Si un problema lleva 3 iteraciones sin corregirse, o solo quedan ajustes mecánicos, **aprobar** y listar los "Ajustes en Figma" (modo C). No seguir iterando por un solo texto o color.
5. Si el problema es de alcance general (tokens, estilo de componentes, cosas fuera de alcance), corregir también `00-prompt-base.md` y registrar el cambio en su tabla **Cambios**, para que no se repita en las demás pantallas.
6. Actualizar la versión actual en la tabla de estado de `docs/design/README.md`.

### C. Aprobar

1. En `iteraciones.md`: estado ✅ en la tabla, sección **Decisión** con los ajustes concretos a completar en Figma (qué componente crear, desde qué pantalla, qué variantes) y los aprendizajes.
2. En `docs/design/README.md`: marcar ✅ en la tabla de estado. Si surgió una lección nueva, agregarla a **Lecciones aprendidas**.
3. Sugerir la siguiente pantalla según la prioridad del README (§7.3): P1 → P10.

## Flujo en dos fases

Es el flujo por defecto desde P3: resolvió P3, P5 y P6 en 3–4 iteraciones cortas, sin que Stitch reintrodujera elementos fuera de alcance.

1. **Fase 1, estado principal:** v1 con un solo estado. Se corrige (un prompt por dispositivo) hasta aprobarlo.
2. **Fase 2, estados:** un prompt "Duplica este frame N veces y en cada copia cambia solo lo que se indica…", con una letra por estado (A, B, C) y los textos literales. Se pega **dos veces**: con el frame de escritorio seleccionado y con el móvil, porque Stitch genera los duplicados solo para el dispositivo seleccionado. Si genera solo algunos, repetir con las letras que falten.
3. Los valores repetidos entre duplicados (por ejemplo, un contador) suelen quedar iguales: no iterar por eso, se ajustan en Figma.

## Reglas para redactar prompts de Stitch

- **Interfaz en español de Perú, con las etiquetas exactas de README §5.2.** Los prompts pueden ir en español.
- **Corrección:** indicar qué frames seleccionar en Stitch ("selecciona solo los frames de escritorio") y empezar con "Edita solo estas pantallas… no cambies nada más del layout".
- **Referencia a lo que ya está bien:** "copia exactamente el estilo de X del frame móvil" funciona mejor que volver a describir los tokens.
- **Valores concretos y numerados:** colores hex, px, radios, textos literales entre comillas. Una instrucción por punto.
- **Variantes aparte:** las pantallas nuevas (otro estado, otra variante) se piden en un prompt separado. Dentro de un prompt de corrección, Stitch las ignora.
- **Exclusiones explícitas:** terminar con lo que NO debe aparecer cuando el riesgo existe (selector de rol, recuperación de contraseña, "Recordarme", correos, información clínica, reprogramación, cancelación por el paciente).
- **Estados por pantalla:** identificar siempre los estados relevantes (normal o foco, error de validación, vacío, deshabilitado, éxito) en escritorio y en móvil (360 px), y pedirlos en la fase 2.
- **Un prompt por dispositivo** en las correcciones: primero escritorio y luego móvil, pidiendo al segundo que copie al primero.

## Al responder al usuario

- **Breve:** qué se corrigió, la tabla de pendientes y el prompt siguiente en un bloque `text` listo para copiar (o la decisión de aprobar). Cuando hay varios frames, resumir el resultado en una tabla frame × dispositivo (✅ / ⚠️ / ❌).
- **Qué seleccionar en Stitch:** cada prompt va precedido de qué frame seleccionar y con qué nombre guardar el resultado.
- **Enlaces** a los archivos actualizados.
- **Sin commits:** no hacer commit salvo que el usuario lo pida. `main` está protegida por un hook, así que el trabajo va en una rama `feature/...`.
