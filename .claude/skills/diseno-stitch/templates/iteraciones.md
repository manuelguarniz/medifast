# Pn – Nombre: iteraciones

- **Requerimientos:** RFxx · RNxx · RNFxx
- **Plantilla:** `templates/ruta/plantilla.html`
- **Referencia visual:** [referencia-x.png](referencia-x.png) _(si la hay; anotar los elementos fuera de alcance que contiene)_

| Versión | Fecha | Resultado | Estado |
| --- | --- | --- | --- |
| v1 | — | — | Prompt listo |

### Decisiones de flujo

_(Qué pasa antes y después de la pantalla, decisiones tomadas con el usuario y fecha. Indicar si es "confirmada", "propuesta" o "adoptada sin objeción". Si afecta a otra pantalla, enlazar su bitácora.)_

### Mensajes de validación (para la implementación)

_(Solo si la pantalla tiene formulario: tabla completa Campo · Regla · Mensaje, aunque el diseño muestre solo algunos ejemplos.)_

| Campo | Regla | Mensaje |
| --- | --- | --- |
| … | … | "…" |

---

## v1

**Prompt:** [prompt base](../00-prompt-base.md) + el prompt de abajo, con la última pantalla aprobada del mismo rol adjunta como referencia de estilo (y la referencia visual del usuario como referencia de layout, si la hay). Solo el estado principal.

```text
…
```

**Resultado:** _pendiente — guardar como `v1.png`._

### Próximos prompts (después de aprobar el principal)

**Estados**, duplicando el frame aprobado; pegar una vez con el frame de escritorio y otra con el móvil:

```text
Duplica este frame N veces y en cada copia cambia solo lo que se indica. No toques navbar, cards, inputs, botones ni estilos.

A. "…": …
B. "…": …
```

### Feedback

**Lo que funciona (mantener):**

- …

**Lo que hay que corregir:**

| # | Problema | Dónde | Severidad | Veces |
| --- | --- | --- | --- | --- |
| 1 | … | Escritorio / Móvil / Ambas | Alta / Media / Baja | 1.ª |

**Aprendizaje:** _(opcional)_

---

## v2

**Prompt:** en Stitch, seleccionar _<frames>_ y pegar:

```text
…
```

**Resultado:** _pendiente — guardar como `v2.png` (o `v2.1.png`, `v2.2.png`… si son estados)._

<!-- Al aprobar, agregar:

### Decisión

Se aprueba vN. Frames base para Figma: _(qué captura usar para cada estado y dispositivo)_. Ajustes a completar en Figma:

1. …

**Aprendizaje para las siguientes pantallas:** …
-->
