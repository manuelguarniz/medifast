# MEDIFAST — Diagramas

Catálogo de todos los diagramas del proyecto, clasificados en tres grupos. Todos están en Mermaid.

| Grupo | Archivo | Contenido |
| --- | --- | --- |
| **Funcionales** | [funcionales.md](funcionales.md) | Qué hace el sistema y para quién: casos de uso, procesos AS-IS y TO-BE, estados, navegación y cronograma |
| **Técnicos** | [tecnicos.md](tecnicos.md) | Cómo está construido: arquitectura MVC por capas, clases y secuencias |
| **Base de datos** | [base-de-datos.md](base-de-datos.md) | Cómo se guardan los datos: modelos conceptual, lógico y físico, restricciones, índices y diccionario de datos |

## Catálogo

| # | Diagrama | Tipo | Uso en el informe |
| --- | --- | --- | --- |
| **F-01** | [Casos de uso](funcionales.md#f-01--casos-de-uso) | UML casos de uso | Análisis |
| **F-02** | [Proceso AS-IS](funcionales.md#f-02--proceso-as-is-gestión-manual-actual) | Flujo con carriles (lógica BPMN) | Procesos de negocio |
| **F-03** | [Proceso TO-BE](funcionales.md#f-03--proceso-to-be-con-medifast) | Flujo con carriles (lógica BPMN) | Procesos de negocio |
| **F-04** | [Estados de la cita](funcionales.md#f-04--estados-de-la-cita) | UML estados | Reglas de negocio (RN-05) |
| **F-05** | [Estados del pago](funcionales.md#f-05--estados-del-pago) | UML estados | Reglas de negocio (RN-07) |
| **F-06** | [Mapa de navegación (P0–P10)](funcionales.md#f-06--mapa-de-navegación) | Flujo | UX/UI |
| **F-07** | [Cronograma](funcionales.md#f-07--cronograma) | Gantt | Planificación |
| **T-01** | [Arquitectura MVC por capas](tecnicos.md#t-01--arquitectura-mvc-por-capas) | Componentes | Arquitectura (rúbrica APF3) |
| **T-02** | [Clases por capas (MVC, servicios, DAO)](tecnicos.md#t-02--clases-por-capas-mvc-servicios-y-dao) | UML clases | Arquitectura (rúbrica APF3) |
| **T-03** | [Clases del dominio](tecnicos.md#t-03--clases-del-dominio) | UML clases | Diagrama de clases |
| **T-04** | [Secuencia: reservar y pagar](tecnicos.md#t-04--secuencia-reservar-y-adjuntar-la-constancia) | UML secuencia | Diseño |
| **T-05** | [Secuencia: validar pago y atender](tecnicos.md#t-05--secuencia-validar-pago-y-registrar-la-atención) | UML secuencia | Diseño |
| **BD-01** | [Modelo conceptual](base-de-datos.md#bd-01--modelo-conceptual) | Entidad–relación | Diseño de base de datos |
| **BD-02** | [Modelo lógico](base-de-datos.md#bd-02--modelo-lógico) | Entidad–relación | Diseño de base de datos |
| **BD-03** | [Modelo físico (MySQL 8.4)](base-de-datos.md#bd-03--modelo-físico-mysql-84) | Entidad–relación con tipos | Diseño de base de datos |
| — | [Restricciones e índices](base-de-datos.md#restricciones-e-índices) | Tabla | Diseño de base de datos |
| — | [Diccionario de datos](base-de-datos.md#diccionario-de-datos) | Tabla | Diseño de base de datos |

## Exportar a imágenes

```bash
# Desde la raíz del proyecto; genera un .md con las imágenes y los PNG en docs/img/
npx -p @mermaid-js/mermaid-cli mmdc -i docs/diagramas/funcionales.md   -o docs/img/funcionales.md   -e png
npx -p @mermaid-js/mermaid-cli mmdc -i docs/diagramas/tecnicos.md      -o docs/img/tecnicos.md      -e png
npx -p @mermaid-js/mermaid-cli mmdc -i docs/diagramas/base-de-datos.md -o docs/img/base-de-datos.md -e png
```
