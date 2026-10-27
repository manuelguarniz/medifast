# Prompts iniciales (v1) por pantalla

Pedir las pantallas en bloques de 2–3, después del [prompt base](00-prompt-base.md). Cuando una pantalla empieza a iterarse, su historial pasa a `Pn-nombre/iteraciones.md` y este archivo conserva solo la versión inicial.

En todas las pantallas posteriores a P0, agregar al final: *"Mantén exactamente el mismo estilo visual que la pantalla de login aprobada (adjunta)."*

## P0 – Login

Iteraciones: [P0-login/iteraciones.md](P0-login/iteraciones.md)

```text
P0 – Login (usa la imagen adjunta como referencia directa):
- Pantalla dividida en dos.
- Izquierda: panel con degradado verde muy oscuro (#123a30 → #0d5c4d), con círculos concéntricos sutiles. Contenido:
  - Logo MEDIFAST con el subtítulo "Consultorio Medifast"
  - Píldora blanca "Atención digital, más simple"
  - Titular enorme "Tu salud merece una atención sin esperas.", con "sin esperas." en verde menta
  - Subtítulo "Reserva tus citas médicas de forma rápida, segura y desde cualquier lugar."
  - Tres beneficios con íconos: "Horarios en tiempo real", "Información protegida", "Reserva en menos de 2 minutos"
  - Cita en cursiva al pie: "Cuidamos de ti, desde el primer clic."
- Derecha: fondo blanco con:
  - Eyebrow "BIENVENIDO A MEDIFAST"
  - H1 "Inicia sesión" y el texto "Ingresa tus datos para continuar."
  - Campo "Número de documento" (placeholder "DNI o CE") y campo "Contraseña" con un ícono para mostrar u ocultar
  - Checkbox "Recordarme"
  - Botón primario de ancho completo "Ingresar →"
  - Texto "¿Aún no tienes una cuenta? Regístrate gratis"
- SIN selector de rol y SIN "¿Olvidaste tu contraseña?".
- Mostrar también la variante con error: una alerta roja "Cuenta bloqueada por 15 minutos tras 5 intentos fallidos".
- En móvil, el panel izquierdo se reduce a una cabecera compacta.
```

## P1 – Registro de paciente

```text
P1 – Registro de paciente (versión completa en P1-registro/iteraciones.md):
- Mismo estilo que el login aprobado (P0 v3). Card dividida: panel verde con el titular "Crea tu cuenta" y formulario blanco en 2 columnas.
- Navbar con "Consultorio Medifast" y el enlace "Iniciar sesión".
- Campos, en orden:
  - Tipo de documento (select DNI/CE) y número de documento
  - Nombres y apellidos
  - Fecha de nacimiento (date) y celular (9 dígitos, empieza con 9)
  - Correo (ancho completo)
  - Contraseña (mín. 8 caracteres) y confirmar contraseña
- Estados: normal (documento en foco) y errores de validación en línea.
- Botón "Crear cuenta →" y el enlace "¿Ya tienes cuenta? Inicia sesión".
- Sin checkbox de términos, numeración de pasos ni documentos distintos de DNI/CE.
```

## P2 – Reservar cita (paciente)

```text
P2 – Reservar cita (paciente):
- Card informativa del tipo de cita detectado (no seleccionable): "Paciente nuevo · Primera consulta · 60 minutos · S/ 150.00".
- Tira de 7 días con flechas de semana: solo de mañana a +30 días habilitados; hoy, los domingos y los días bloqueados en gris.
- Grilla de slots de 09:00 a 12:30 cada 30 min, con los estados disponible, ocupado ("Ocupado", tachado), deshabilitado y seleccionado. Para NUEVO se seleccionan 2 bloques consecutivos y se deshabilitan los inicios sin 2 bloques libres o que terminarían después de las 13:00.
- Panel lateral con el resumen de la cita (médico, fecha, hora como rango, tipo, monto), el aviso de los 15 min y el botón "Continuar al pago →".
- Variantes en un prompt aparte: alerta informativa "Ya tienes una cita activa" con sus datos y el botón "Ver mis citas", con el formulario deshabilitado; y alerta de error "Horario no disponible, elija otro".
- Prompt detallado en P2-reservar/iteraciones.md.
```

## P3 – Pago Yape (simulado)

```text
P3 – Pago Yape (simulado):
- Card centrada con el monto grande "S/ 150.00", un QR de ejemplo y el número de Yape de ejemplo.
- Contador regresivo "14:59" con el aviso de horario bloqueado.
- Zona drag & drop obligatoria para la constancia (JPG, PNG o PDF hasta 5 MB).
- Campo opcional "N.º de operación".
- Leyenda de advertencia visible: "Simulación — no se realizó ningún cobro".
- Botón "Enviar constancia →".
- Sin botón "Volver": nota "Puedes retomar el pago desde Mis citas mientras el tiempo no venza."
- Prompt detallado y estados (archivo adjunto, error, tiempo vencido) en P3-pago/iteraciones.md.
```

## P4 – Confirmación

```text
P4 – Confirmación:
- Ícono de check verde y el título "¡Cita reservada!".
- Card con: n.º de cita, médico, fecha, hora, tipo, monto y el badge de pago "Por validar".
- Botones "Descargar PDF" (primario) e "Ir a mis citas" (secundario).
- Sin mención a correos.
```

## P5 – Mis citas (paciente)

```text
P5 – Mis citas (paciente):
- Historial de la más reciente a la más antigua.
- Columnas: fecha, hora, tipo, monto, estado de la cita, estado del pago y acciones.
- Acciones según el estado: "Continuar pago" (pendiente y vigente, con un mini contador), "Ver detalle", "Descargar PDF" y "Ver constancia".
- Las canceladas muestran el motivo. Las vencidas se muestran como "Vencida" y sin acciones.
- Como máximo una cita activa.
- Versión con tarjetas apiladas en móvil y estado vacío "No tienes citas registradas" con el botón "Reservar cita".
- Pestañas "Próximas" (solo la cita activa, como card) e "Historial" (el resto, en tabla). "Reservar cita" solo si no hay cita activa. "Ver constancia ↗" abre una pestaña nueva.
- Prompt detallado, tabla de acciones por estado y prompts de estados en P5-mis-citas/iteraciones.md.
```

## P6 – Mi perfil

```text
P6 – Mi perfil:
- Card "Datos personales": tipo y n.º de documento en solo lectura; nombres, apellidos, fecha de nacimiento, celular y correo editables. Botón "Guardar cambios".
- Card "Cambiar contraseña": contraseña actual, nueva y confirmación.
- Card "Cambiar contraseña": contraseña actual, nueva y confirmación (RF11).
- Fecha de nacimiento editable; tipo y n.º de documento en solo lectura con candado.
- Estados (éxito y errores) en un prompt aparte. Prompt detallado en P6-perfil/iteraciones.md.
```

## P7 – Agenda (médico)

```text
P7 – Agenda (médico):
- Filtros: Día / Semana (toggle), selector de fecha y estado.
- Tabla ordenada por fecha y hora con: paciente (nombre y DNI parcial), fecha y hora, tipo (NUEVO/CONTROL), estado de la cita, estado del pago y el botón "Ver".
- Contadores rápidos arriba (citas de hoy, por validar).
- Tarjetas apiladas en móvil.
- Navbar superior de médico (sin menú lateral) y estados (semana, filtro, vacío) en un prompt aparte. Prompt detallado en P7-agenda/iteraciones.md.
```

## P8 – Detalle de cita (médico)

```text
P8 – Detalle de cita (médico):
- Card con los datos del paciente y de la cita.
- Visor de la constancia (imagen/PDF) con el n.º de operación.
- Barra de acciones según el estado: "Validar pago" (éxito), "Rechazar pago" (peligro), "Marcar atendida", "No asistió" y "Cancelar cita". Ver la tabla de acciones según el estado en P8-detalle-cita/iteraciones.md.
- Modal "Rechazar pago" / "Cancelar cita" con el textarea obligatorio del motivo (hasta 500 caracteres) y los botones "Volver" y la acción en rojo.
- "Marcar atendida" y "No asistió" deshabilitados con una ayuda visible; los botones de pago se ocultan si no hay pago por validar. Prompt detallado en P8-detalle-cita/iteraciones.md.
```

## P9 – Reporte de citas

```text
P9 – Reporte de citas:
- Filtros: rango de fechas, atajos Día/Semana/Mes y estado.
- 4 KPI cards: total de citas, atendidas, canceladas y no asistieron.
- Tabla de resultados y el botón "Exportar Excel" (ícono de hoja de cálculo).
```

## P10 – Reporte de pagos

```text
P10 – Reporte de pagos:
- Filtro de período (rango de fechas y atajo de mes). Muestra solo pagos Verificados.
- 3 KPI cards: cantidad de pagos, total en S/ y ticket promedio.
- Tabla con una fila de total al final y el botón "Exportar Excel".
```

## Errores 403 / 404

```text
Páginas de error 403 ("No tienes permiso para ver esta página") y 404 ("No encontramos esta página"), con el estilo de estado vacío y el botón "Volver al inicio".
```
