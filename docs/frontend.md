
# MEDIFAST — Guía de componentes frontend

## 1. Objetivo

Documentar el uso de los fragmentos Thymeleaf
reutilizables de MEDIFAST.

Implementación correspondiente al issue #2.

## 2. Tecnologías

- Java 21
- Spring Boot 4.1.1
- Thymeleaf
- Bootstrap 5.3.3
- Spring Security
- CSS personalizado de MEDIFAST

## 3. Estructura

Los componentes se encuentran en:

`medifast-app/src/main/resources/templates/fragments/`

| Archivo         | Responsabilidad                 |
| --------------- | ------------------------------- |
| layout.html     | Head, navbars y scripts         |
| formulario.html | Campos y errores de validación |
| mensajes.html   | Alertas flash                   |
| estado.html     | Estados de citas y pagos        |
| tabla.html      | Mensajes de tablas vacías      |
| modal.html      | Modal de observaciones          |
| citas.html      | Acciones del historial          |
| paginacion.html | Componentes de paginación      |
| reportes.html   | Componentes de reportes         |

## 4. Layout común

### Encabezado

```html
<head th:replace="~{fragments/layout :: head('Mi página')}"></head>
```

### Navbar general

```html
<nav th:replace="~{fragments/layout :: navbar}"></nav>
```

### Navbar de paciente

```html
<nav th:replace="~{fragments/layout :: navbarPaciente('citas')}"></nav>
```

### Scripts

```html
<div th:replace="~{fragments/layout :: scripts}"></div>
```

## 5. Mensajes flash

```html
<div th:replace="~{fragments/mensajes :: flash}"></div>
```

Desde un controlador:

```java
redirectAttributes.addFlashAttribute(
    "exito", "Operación realizada."
);
```

Las claves disponibles son:

- `exito`
- `error`
- `info`

## 6. Estados

### Estado de cita

```html
<span th:replace="~{fragments/estado :: cita(${cita.estado})}"></span>
```

### Estado de pago

```html
<span th:replace="~{fragments/estado :: pago(${pago?.estado})}"></span>
```

Colores:

| Estado         | Clase             |
| -------------- | ----------------- |
| PENDIENTE_PAGO | text-bg-warning   |
| POR_VALIDAR    | text-bg-warning   |
| PROGRAMADA     | text-bg-primary   |
| ATENDIDA       | text-bg-success   |
| VALIDADO       | text-bg-success   |
| NO_ASISTIO     | text-bg-secondary |
| CANCELADA      | text-bg-danger    |
| RECHAZADO      | text-bg-danger    |

## 7. Formularios

El fragmento de campos de registro existente usa
cinco argumentos:

```html
<div th:replace="~{fragments/formulario ::
    campo('Correo', 'correo', 'email', 'Ingresa tu correo', 120)}">
</div>
```

Debe utilizarse dentro de un formulario
con un objeto de Thymeleaf definido por `th:object`.

El componente incluye errores de validación
y clases de estado inválido.

No modificar la firma existente sin actualizar
Registro y Perfil.

## 8. Tablas

### Contenedor responsive

```html
<div class="medifast-tabla-contenedor">
    <table class="table table-hover">
        <!-- Contenido -->
    </table>
</div>
```

### Estado vacío

```html
<div th:if="${#lists.isEmpty(citas)}"
     th:replace="~{fragments/tabla ::
        vacio('No hay citas registradas.')}">
</div>
```

Las tablas deben permitir desplazamiento horizontal
en pantallas de 360 px cuando sea necesario.

## 9. Modal de observación

```html
<div th:replace="~{fragments/modal ::
    observacion(
        'modalCancelar',
        @{/medico/citas/{id}/cancelar(id=${cita.id})},
        'Cancelar cita'
    )}">
</div>
```

El modal incluye:

- Campo obligatorio `observacion`.
- Límite de 500 caracteres.
- Token CSRF.
- Botón para confirmar.
- Botón para cerrar.

P8 contiene actualmente una implementación
personalizada más avanzada que también conserva
los filtros de agenda, muestra errores del servidor
y reabre el modal correspondiente.

No sustituirla sin migrar esas funcionalidades.

## 10. Manejo de errores

Las vistas se ubican en:

- `templates/error/403.html`
- `templates/error/404.html`
- `templates/error/500.html`

El manejador MVC se encuentra en:

`controller/ManejadorErrores.java`

Los mensajes internos y las trazas no deben
mostrarse al usuario.

Los errores de autenticación y autorización
gestionados por Spring Security requieren
configuración independiente cuando corresponda.

## 11. Formatos

### Fecha

```html
<span th:text="${#temporals.format(fecha, 'dd/MM/yyyy')}"></span>
```

### Hora

```html
<span th:text="${#temporals.format(hora, 'HH:mm')}"></span>
```

### Importe monetario

```html
<span th:text="|S/ ${#numbers.formatDecimal(monto, 1, 'COMMA', 2, 'POINT')}|"></span>
```

Estos ejemplos se utilizan para valores
numéricos y temporales compatibles con Thymeleaf.
Las vistas con DTO ya formateados pueden conservar
sus propiedades de presentación.

## 12. Pruebas

Desde `medifast-app`:

```bash
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd test
```

Antes de integrar, verificar:

1. Compilación y pruebas automáticas.
2. Vistas 403, 404 y 500.
3. Alertas flash.
4. Colores de estado.
5. Validación del modal.
6. Protección CSRF.
7. Tablas en 360 px.
8. Compatibilidad con P0–P10.

## 13. Referencias

- GitHub issue #2.
- GitHub issue #30, sistema de diseño.
- README del proyecto.
- Diagramas funcionales de MEDIFAST.
