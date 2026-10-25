# Prompt base (contexto)

Pegar al inicio de cada sesión de Stitch, antes de pedir cualquier pantalla. Si Stitch corta el texto, pegar al menos las secciones **Sistema de diseño** y **No incluir** junto con el prompt de la pantalla.

```text
Diseña una aplicación web responsive (escritorio y móvil desde 360 px) llamada MEDIFAST para el "Consultorio Medifast" del Dr. Moisés Barrantes Cabrera (Medicina Interna y Reumatología, Trujillo, Perú). Todo el texto de la interfaz debe estar en español de Perú. La app se implementará con Bootstrap 5 + Thymeleaf, así que usa componentes compatibles con Bootstrap (navbar, cards, tablas, badges, modals, form-control, alerts).

PROPÓSITO
Los pacientes se registran, ven los horarios libres y reservan una cita adjuntando la constancia de un pago con Yape SIMULADO. El médico revisa la constancia, valida o rechaza el pago, gestiona su agenda, marca si el paciente asistió y exporta reportes a Excel. Es un sistema administrativo: NO maneja información clínica.

SISTEMA DE DISEÑO (respetar estrictamente)
- Colores:
  - Primario #0d5c4d (verde clínico profundo)
  - Éxito #10b981
  - Advertencia #f59e0b
  - Peligro #ef4444
  - Secundario neutro #64748b
  - Informativo #0284c7
  - Superficie base #f8fafc
  - Fondo de tarjetas blanco
  - Acento verde menta claro (#9fe0c4 aprox.) solo para palabras destacadas en titulares sobre fondo oscuro
- Tipografía Inter:
  - H1 32px/800
  - H2 24px/700
  - Subtítulos y títulos de card 20px/600
  - Cuerpo 16px/400–500
  - Etiquetas, tablas y ayudas 14px/500
- Espaciado en múltiplos de 8px. Radio de borde de 8px en inputs y botones, 12–16px en cards. Sombras muy suaves.
- Botones:
  - Primario: relleno #0d5c4d, texto blanco, con flecha "→" en las acciones principales
  - Secundario: borde neutro
  - Peligro: rojo
  - Deshabilitado: verde desaturado
  - Mostrar los estados hover, focus y active
- Inputs: etiqueta en negrita arriba del campo, borde gris claro #cbd5e1 de 1px, radio de 8px y placeholder gris. Al enfocar, borde primario con un halo suave. Error: borde rojo con mensaje debajo e ícono de alerta. Solo lectura con fondo gris claro.
- Badges tipo píldora con punto de color:
  - Estados de cita: "Pago pendiente" (ámbar), "Confirmada" (azul info), "Atendida" (verde), "No asistió" (gris), "Cancelada" (rojo)
  - Estados de pago: "Por validar" (ámbar), "Verificado" (verde), "Rechazado" (rojo)
- Alertas flash: éxito, error, información y advertencia, con fondo tenue, borde de 1px del mismo color, radio de 8px, ícono a la izquierda y texto en negrita al inicio.
- Slots de horario: botones cuadrados con la hora en tres estados (Disponible, Seleccionado relleno primario, Ocupado gris tachado). Los slots no se mueven de lugar.
- Contador regresivo mm:ss en una píldora oscura dentro de un aviso verde claro: "Tu horario está bloqueado temporalmente para completar el pago".
- Zona de subida drag & drop con borde punteado: "JPG, PNG o PDF hasta 5 MB".
- Tablas en escritorio que pasan a tarjetas apiladas en móvil.
- Estado vacío con ícono, título, texto y botón primario.
- Íconos lineales finos (estilo Bootstrap Icons / Lucide).

LAYOUT
- Navbar superior verde #0d5c4d con el logo MEDIFAST (ícono de pulso cardíaco en un cuadrado redondeado verde menta) y el subtítulo "Consultorio Medifast". Cambia según el rol:
  - Paciente: "Reservar cita", "Mis citas", "Mi perfil", "Salir"
  - Médico: "Agenda", "Reporte de citas", "Reporte de pagos", "Salir", con el nombre "Dr. Moisés Barrantes" visible
- En móvil, la navbar colapsa en un botón "Menú".
- Navbar pública (login y registro): solo el logo a la izquierda y un enlace de texto a la derecha ("Iniciar sesión" o "Crear cuenta"). Sin flecha atrás, título de página ni ícono de usuario.
- Pie de página discreto: "Consultorio Medifast · Dr. Moisés Barrantes Cabrera · Trujillo, Perú".
- Contenido sobre fondo #f8fafc, dentro de cards blancas con un máximo de ~1200px de ancho.

REGLAS DE NEGOCIO QUE LA UI DEBE REFLEJAR
- Tipos de cita detectados automáticamente: NUEVO = 60 min / S/ 150; CONTROL = 30 min / S/ 80.
- Solo se reserva desde mañana hasta 30 días después. El horario por defecto es de lunes a sábado, 09:00–13:00, en bloques de 30 min.
- El paciente tiene como máximo UNA cita activa. No puede cancelar ni reprogramar.
- La reserva queda apartada 15 min hasta que adjunte la constancia. Si vence, se cancela con el motivo "Tiempo de reserva vencido".
- Solo el médico cancela, y siempre con un motivo obligatorio. Rechazar un pago = cancelar la cita con motivo.
- "Atendida" solo se puede marcar si el pago está Verificado y la hora de la cita ya llegó.

NO INCLUIR (fuera de alcance)
- Selector de rol en el login
- Recuperación de contraseña ni opción "Recordarme" (la sesión expira a los 30 min)
- Notificaciones o envío de correos, SMS o WhatsApp
- Historia clínica, diagnósticos, recetas o telemedicina
- Citas virtuales
- Reprogramar citas
- Pagos reales o pedir credenciales bancarias
- Edición de horarios desde la interfaz
- Alta de médicos
- Chatbots o IA
```

## Cambios

| Fecha | Cambio | Motivo |
| --- | --- | --- |
| 2026-10-25 | Versión inicial | — |
| 2026-10-25 | Se definen el borde y el foco de los inputs, el estilo único de alertas y el subtítulo del logo; se excluye "Recordarme" | Feedback de [P0 v1](P0-login/iteraciones.md) |
| 2026-10-25 | Se define la navbar pública de login y registro | Feedback de [P1 v1](P1-registro/iteraciones.md) |
