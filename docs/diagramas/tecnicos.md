# Diagramas técnicos

> Diagramas en [Mermaid](https://mermaid.js.org/): GitHub y VS Code (extensión *Markdown Preview Mermaid Support*) los renderizan directamente. Para el informe, exportarlos a PNG/SVG con [mermaid.live](https://mermaid.live) o con mermaid-cli (ver [índice](README.md)).
>
> La fuente de verdad de requerimientos y reglas es el [README del proyecto](../../README.md). Si un diagrama lo contradice, se corrige el diagrama.

Describen **cómo está construido el sistema**: arquitectura, clases y la interacción entre componentes. Evidencian los principios que exige la rúbrica del APF3: **MVC, DAO, SOLID** (y **TDD** en las pruebas de los servicios).

| # | Diagrama |
| --- | --- |
| T-01 | Arquitectura MVC por capas |
| T-02 | Clases por capas (MVC, servicios y DAO) |
| T-03 | Clases del dominio |
| T-04 | Secuencia: reservar y adjuntar la constancia |
| T-05 | Secuencia: validar pago y registrar la atención |

---

## T-01 — Arquitectura MVC por capas

Monolito Spring Boot en localhost. La petición pasa por Spring Security, el controlador delega en los servicios (reglas de negocio) y estos acceden a MySQL solo a través de los DAO.

```mermaid
flowchart TB
    NAV["Navegador: Paciente / Médico"]

    subgraph APP["Aplicación Spring Boot (localhost:8080)"]
        direction TB
        SEC["Spring Security: bcrypt, roles, CSRF, bloqueo por intentos (Guava)"]
        subgraph MVC["MVC"]
            direction LR
            C["Controladores: Spring MVC"]
            V["Vistas: Thymeleaf + Bootstrap 5 (P0 a P10)"]
        end
        S["Servicios: reglas de negocio (interfaces + impl, SOLID, TDD)"]
        DAO["DAO: interfaces *Dao que extienden JpaRepository"]
        JOB["Tareas programadas: vencimiento de 15 min, generación de bloques"]
        UT["Utilidades: Excel (POI), PDF (OpenPDF), archivos (Commons IO)"]
        LOG["Logback: auditoría"]
    end

    DB[("MySQL 8.4")]
    FS[["Carpeta local uploads/comprobantes"]]

    NAV --> SEC --> C
    C --> V
    C --> S
    JOB --> S
    S --> DAO --> DB
    S --> UT --> FS
    S -.-> LOG
```

---

## T-02 — Clases por capas (MVC, servicios y DAO)

Muestra los principios de la rúbrica: **MVC** (controladores + vistas), **SOLID** (interfaces pequeñas, una responsabilidad, inyección de dependencias) y **DAO** (interfaces que extienden `JpaRepository`). Cada interfaz de servicio tiene su implementación en `service/impl/` (`ReservaServiceImpl`, etc.).

```mermaid
classDiagram
    direction TB

    class AuthController
    class SecurityConfig
    class UsuarioDetailsService
    class EventosAutenticacion
    class RedireccionPorRol
    class UsuarioActual {
        +idPaciente(Authentication) Long
        +idMedico(Authentication) Long
        +medico(Authentication) Medico
    }
    class PerfilController
    class ReservaController
    class MisCitasController
    class AgendaController
    class ReporteController

    class PacienteService {
        <<interface>>
        +registrar(RegistroDto dto) Paciente
        +actualizar(Long idPaciente, PerfilDto dto) Paciente
        +cambiarContrasena(Long idUsuario, String actual, String nueva)
    }
    class ReservaService {
        <<interface>>
        +disponibilidad(Long idPaciente, Long idMedico) DisponibilidadDto
        +detectarTipo(Long idPaciente, Long idMedico) TipoCita
        +reservar(Long idPaciente, Long idHorario) Cita
        +historial(Long idPaciente) MisCitasDto
        +vencerReservas(LocalDateTime ahora) int
    }
    class PagoController
    class PagoService {
        <<interface>>
        +paraPagar(Long idCita, Long idPaciente) PagoPendienteDto
        +registrarPago(Long idCita, Long idPaciente, String nroOperacion, MultipartFile comprobante) Pago
    }
    class GestionCitaService {
        <<interface>>
        +agenda(Long idMedico, FiltroAgendaDto filtro) AgendaDto
        +detalle(Long idCita, Long idMedico) DetalleCitaDto
        +validarPago(Long idCita, Long idMedico)
        +rechazarPago(Long idCita, Long idMedico, String observacion)
        +marcarAtendida(Long idCita, Long idMedico)
        +marcarNoAsistio(Long idCita, Long idMedico)
        +cancelar(Long idCita, Long idMedico, String observacion)
    }
    class ReporteService {
        <<interface>>
        +citas(Long idMedico, FiltroReporteDto filtro) ReporteCitasDto
	+pagos(Long idMedico, FiltroReporteDto filtro) ResumenPagosDto
    }
    class GeneracionHorarioService {
        <<interface>>
        +generar(LocalDate desde, LocalDate hasta) int
    }
    class GeneracionHorarioServiceImpl
    class GeneracionHorarioJob {
        +ejecutar()
        ApplicationReadyEvent y 02:00 America/Lima
    }
    class ProgramacionConfig {
        @EnableScheduling
    }
    class ExportadorReporte {
        <<interface>>
        +exportar(Reporte reporte) byte[]
    }
    class ExcelExportador {
        Apache POI
    }
    class ConfirmacionPdfService {
        <<interface>>
        +generar(Long idCita, Long idPaciente) byte[]
    }
    class ConstanciaService {
        <<interface>>
        +paraPaciente(Long idCita, Long idPaciente) ConstanciaDto
        +paraMedico(Long idCita, Long idMedico) ConstanciaDto
    }
    class AlmacenamientoService {
        <<interface>>
        +guardar(MultipartFile archivo) String
        +cargar(String ruta) Resource
    }
    class AlmacenamientoLocal {
        Apache Commons IO
    }
    class IntentosLoginService {
        Guava Cache
        +registrarFallo(String documento)
        +estaBloqueado(String documento) boolean
        +reiniciar(String documento)
    }
    class Auditoria {
        <<util>>
        +registrar(Evento evento, String documento, String detalle)
    }
    class VencimientoReservasJob {
        Scheduled cada minuto
        +ejecutar()
    }

    class JpaRepository {
        <<Spring Data>>
    }
    class UsuarioDao {
        <<interface>>
    }
    class PacienteDao {
        <<interface>>
    }
    class MedicoDao {
        <<interface>>
    }
    class TarifaDao {
        <<interface>>
    }
    class HorarioDao {
        <<interface>>
        +bloquesLibres(Long idMedico, LocalDate desde, LocalDate hasta) List~Horario~
        +findAllByMedicoIdAndFechaBetween(Long idMedico, LocalDate desde, LocalDate hasta) List~Horario~
    }
    class HorarioSemanalDao {
        <<interface>>
        +findAllByMedicoId(Long idMedico) List~HorarioSemanal~
    }
    class ConsultorioDao {
        <<interface>>
        +findAllByActivoTrue() List~Consultorio~
    }
    class CitaDao {
        <<interface>>
        +existeAtendida(Long idPaciente, Long idMedico) boolean
        +pendientesVencidas(LocalDateTime ahora) List~Cita~
    }
    class VistaCitaDao {
        <<interface>>
        +findAllByIdMedicoAndFechaBetweenOrderByFechaAscHoraInicioAsc(Long idMedico, LocalDate desde, LocalDate hasta) List~VistaCita~
    }
    class FiltroAgendaDto {
        <<record>>
        +Vista vista
        +LocalDate fecha
        +EstadoCitaEnum estado
        +desde() LocalDate
        +hasta() LocalDate
    }
    class AgendaDto {
        <<record>>
        +List~VistaCita~ citas
        +long total
        +long porValidar
        +long confirmadas
        +long atendidas
        +List~HorarioSemanal~ horario
    }
    class PagoDao {
        <<interface>>
        +pagosValidados(Long idMedico, LocalDateTime desde, LocalDateTime hastaExclusivo) List~PagoValidadoDto~
    }

    SecurityConfig ..> UsuarioDetailsService
    SecurityConfig --> RedireccionPorRol
    SecurityConfig ..> AuthController : rutas /login y /
    UsuarioDetailsService ..> UsuarioDao
    UsuarioDetailsService --> IntentosLoginService
    EventosAutenticacion --> IntentosLoginService
    EventosAutenticacion --> Auditoria
    UsuarioActual ..> PacienteDao
    UsuarioActual ..> MedicoDao
    PerfilController --> PacienteService
    ReservaController --> ReservaService
    MisCitasController --> ReservaService
    MisCitasController --> ConfirmacionPdfService
    MisCitasController --> ConstanciaService
    ConfirmacionPdfService ..> VistaCitaDao
    ConstanciaService ..> CitaDao
    ConstanciaService ..> PagoDao
    AgendaController --> GestionCitaService
    AgendaController --> UsuarioActual
    AgendaController ..> FiltroAgendaDto
    GestionCitaService ..> AgendaDto
    GestionCitaService ..> DetalleCitaDto
    AgendaController ..> MotivoDto
    AgendaController --> ConstanciaService
    ReporteController --> ReporteService
    ReporteController --> UsuarioActual
    ReporteController --> ExportadorReporte
    VencimientoReservasJob --> ReservaService
    GeneracionHorarioJob --> GeneracionHorarioService
    GeneracionHorarioJob ..> ConsultorioDao
    GeneracionHorarioServiceImpl ..|> GeneracionHorarioService
    GeneracionHorarioServiceImpl ..> MedicoDao
    GeneracionHorarioServiceImpl ..> HorarioSemanalDao
    GeneracionHorarioServiceImpl ..> HorarioDao

    ExcelExportador ..|> ExportadorReporte
    AlmacenamientoLocal ..|> AlmacenamientoService

    PacienteService ..> UsuarioDao
    PacienteService ..> PacienteDao
    ReservaService ..> HorarioDao
    ReservaService ..> CitaDao
    ReservaService ..> TarifaDao
    ReservaService ..> PagoDao
    PagoController --> PagoService
    PagoController --> UsuarioActual
    PagoService ..> CitaDao
    PagoService ..> PagoDao
    PagoService ..> TarifaDao
    PagoService ..> VistaCitaDao
    GestionCitaService ..> CitaDao
    GestionCitaService ..> PagoDao
    GestionCitaService ..> VistaCitaDao
    GestionCitaService ..> HorarioSemanalDao
    ReporteService ..> VistaCitaDao
    ReporteService ..> PagoDao

    UsuarioDao --|> JpaRepository
    PacienteDao --|> JpaRepository
    MedicoDao --|> JpaRepository
    TarifaDao --|> JpaRepository
    HorarioDao --|> JpaRepository
    HorarioSemanalDao --|> JpaRepository
    ConsultorioDao --|> JpaRepository
    CitaDao --|> JpaRepository
    PagoDao --|> JpaRepository
    VistaCitaDao --|> JpaRepository
```

---

## T-03 — Clases del dominio

Entidades JPA del paquete `model/`. Las fechas y horas de la cita se obtienen de sus bloques (`CitaHorario` → `Horario`). Corresponden 1 a 1 con las tablas del [modelo físico](base-de-datos.md#bd-03--modelo-físico-mysql-84).

```mermaid
classDiagram
    direction LR

    class Consultorio {
        -Long id
        -String nombre
        -String direccion
        -int diasAnticipacionMin
        -int diasAnticipacionMax
        -int minutosBloque
        -int minutosApartado
        -boolean activo
    }
    class Usuario {
        -Long id
        -TipoDocumento tipoDocumento
        -String numeroDocumento
        -String contrasenaHash
        -Rol rol
        -boolean activo
    }
    class Paciente {
        -Long id
        -String nombres
        -String apellidos
        -LocalDate fechaNacimiento
        -String celular
        -String correo
        +actualizarDatos(PerfilDto datos)
    }
    class Medico {
        -Long id
        -String nombres
        -String apellidos
        -String cmp
        -String especialidad
        -boolean activo
    }
    class Tarifa {
        -Long id
        -TipoCita tipoCita
        -int duracionMin
        -BigDecimal precio
        +bloquesNecesarios(int minutosBloque) int
    }
    class HorarioSemanal {
        -Long id
        -int diaSemana
        -LocalTime horaInicio
        -LocalTime horaFin
    }
    class Horario {
        -Long id
        -LocalDate fecha
        -LocalTime horaInicio
        -LocalTime horaFin
        -boolean bloqueado
        +inicio() LocalDateTime
    }
    class Cita {
        -Long id
        -TipoCita tipoCita
        -EstadoCita estado
        -LocalDateTime expiraEn
        -String observacion
        -CanceladaPor canceladaPor
        +estaActiva() boolean
        +estaVencida(LocalDateTime ahora) boolean
        +programar()
        +marcarAtendida()
        +marcarNoAsistio()
        +cancelar(String observacion, CanceladaPor por)
        +inicio() LocalDateTime
    }
    class CitaHorario {
        -boolean activo
        +liberar()
    }
    class Pago {
        -Long id
        -String metodo
        -BigDecimal monto
        -String nroOperacion
        -String comprobanteRuta
        -String comprobanteTipo
        -EstadoPago estado
        -LocalDateTime fechaPago
        -LocalDateTime validadoEn
        +validar(LocalDateTime ahora)
        +rechazar()
    }

    class TipoDocumento {
        <<enumeration>>
        DNI
        CE
    }
    class Rol {
        <<enumeration>>
        PACIENTE
        MEDICO
    }
    class TipoCita {
        <<enumeration>>
        NUEVO
        CONTROL
    }
    class EstadoCita {
        <<enumeration>>
        PENDIENTE_PAGO
        PROGRAMADA
        ATENDIDA
        NO_ASISTIO
        CANCELADA
    }
    class EstadoPago {
        <<enumeration>>
        POR_VALIDAR
        VALIDADO
        RECHAZADO
    }
    class CanceladaPor {
        <<enumeration>>
        SISTEMA
        MEDICO
    }

    Consultorio "1" o-- "*" Medico : agrupa
    Usuario "1" -- "0..1" Paciente : credencial
    Usuario "1" -- "0..1" Medico : credencial
    Medico "1" *-- "2" Tarifa : cobra
    Medico "1" *-- "*" HorarioSemanal : define
    Medico "1" --> "*" Horario : ofrece
    Paciente "1" --> "*" Cita : reserva
    Medico "1" --> "*" Cita : atiende
    Cita "1" *-- "1..2" CitaHorario : ocupa
    CitaHorario "*" --> "1" Horario : bloque
    Cita "1" *-- "0..1" Pago : se paga con
```

---

## T-04 — Secuencia: reservar y adjuntar la constancia

Cubre RF01, RF03, RF16 y las reglas RN-01, RN-02, RN-04 y RN-09.

```mermaid
sequenceDiagram
    autonumber
    actor Pac as Paciente
    participant C as ReservaController
    participant S as ReservaService
    participant PC as PagoController
    participant P as PagoService
    participant D as CitaDao / HorarioDao / PagoDao
    participant DB as MySQL
    participant J as VencimientoReservasJob

    Pac->>C: GET /paciente/reservar
    C->>S: disponibilidad(idPaciente, idMedico)
    S->>D: citas ATENDIDA del paciente con el médico
    S->>S: detectar tipo NUEVO o CONTROL y su tarifa
    S->>D: bloques libres de mañana a +30 días
    C-->>Pac: P2 con horarios, tipo y monto

    Pac->>C: POST /paciente/reservas (bloque elegido)
    C->>S: reservar(idPaciente, idHorario)
    S->>DB: INSERT cita PENDIENTE_PAGO + cita_horario (transacción)
    alt el paciente ya tiene una cita activa (UNIQUE paciente_activo)
        DB-->>S: clave duplicada
        S-->>C: CitaActivaException
        C-->>Pac: Muestra su cita activa
    else otro paciente tomó el bloque (UNIQUE horario_activo)
        DB-->>S: clave duplicada
        S-->>C: HorarioNoDisponibleException
        C-->>Pac: Horario no disponible, elija otro
    else reserva creada
        DB-->>S: OK
        S-->>C: cita con expira_en = ahora + 15 min
        C-->>Pac: P3 Pago Yape con contador
    end

    Pac->>PC: POST /paciente/pago (citaId, constancia, n.º de operación)
    PC->>P: registrarPago(idCita, idPaciente, nroOperacion, archivo)
    P->>DB: SELECT cita … FOR UPDATE (dueño, PENDIENTE_PAGO y plazo vigente)
    P->>P: tipo real JPG/PNG/PDF, ≤ 5 MB, n.º de operación solo dígitos
    P->>DB: guarda el archivo con nombre UUID, INSERT pago POR_VALIDAR y UPDATE cita PROGRAMADA
    PC-->>Pac: P4 Confirmación + Descargar PDF

    loop cada minuto
        J->>S: vencerReservas(ahora)
        S->>DB: citas PENDIENTE_PAGO con expira_en vencido
        S->>DB: UPDATE cita CANCELADA (SISTEMA) y cita_horario.activo = FALSE
    end
```

---

## T-05 — Secuencia: validar pago y registrar la atención

Cubre RF08, RF12, RF13 y RF14, y las reglas RN-07, RN-13 y RN-14.

```mermaid
sequenceDiagram
    autonumber
    actor Med as Médico
    participant C as AgendaController
    participant G as GestionCitaService
    participant K as ConstanciaService
    participant D as CitaDao / PagoDao / VistaCitaDao
    participant DB as MySQL

    Med->>C: GET /medico/citas/{id}
    C->>G: detalle(idCita, idMedico)
    G->>D: cita del médico (RF12), paciente, pago y tarifa
    C-->>Med: P8 Detalle de cita
    Med->>C: GET /medico/citas/{id}/constancia
    C->>K: paraMedico(idCita, idMedico)
    K-->>Med: archivo de la constancia

    alt constancia válida
        Med->>C: POST validar
        C->>G: validarPago(idCita, idMedico)
        G->>DB: UPDATE pago VALIDADO, validado_en = ahora
    else constancia no válida
        Med->>C: POST rechazar (motivo obligatorio, ≤ 500)
        C->>G: rechazarPago(idCita, idMedico, observacion)
        G->>DB: pago RECHAZADO, cita CANCELADA (MEDICO), cita_horario.activo = FALSE
    end

    Med->>C: POST atendida o no asistió
    C->>G: marcarAtendida(idCita, idMedico) o marcarNoAsistio(idCita, idMedico)
    G->>G: verificar hora de inicio ya llegó (RN-14)
    alt la hora de inicio no llegó
        G-->>C: AccionNoPermitidaException
        C-->>Med: P8 "La cita aún no empieza."
    else atendida sin pago VALIDADO
        G-->>C: PagoNoValidadoException (RN-07)
        C-->>Med: P8 "Primero valida el pago."
    else válido
        G->>DB: UPDATE cita ATENDIDA o NO_ASISTIO
        C-->>Med: P8 con la alerta de éxito
    end
```
