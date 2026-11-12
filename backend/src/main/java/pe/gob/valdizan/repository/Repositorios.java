package pe.gob.valdizan.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.*;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Todos los repositorios Spring Data agrupados (capa repository). */
public final class Repositorios {
    private Repositorios() {}

    public interface RolRepository extends JpaRepository<Rol, Long> {
        Optional<Rol> findByNombreRol(NombreRol nombreRol);
    }

    public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
        Optional<Usuario> findByCorreoIgnoreCase(String correo);
        Optional<Usuario> findByDocumentoIdentidad(String documento);
        Optional<Usuario> findByTokenRecuperacion(String token);
        boolean existsByCorreoIgnoreCase(String correo);
        boolean existsByDocumentoIdentidad(String documento);

        @Query("select u from Usuario u where u.documentoIdentidad like concat(:q,'%') " +
                "or lower(u.nombres) like lower(concat('%',:q,'%')) or lower(u.apellidos) like lower(concat('%',:q,'%')) " +
                "or lower(u.correo) like lower(concat('%',:q,'%')) order by u.apellidos")
        Page<Usuario> buscar(@Param("q") String q, Pageable pageable);
    }

    public interface PacienteRepository extends JpaRepository<Paciente, Long> {
        Optional<Paciente> findByDocumentoIdentidad(String documento);
        List<Paciente> findByTutor_IdUsuarioOrderByApellidos(Long tutorId);

        @Query("select p from Paciente p where p.documentoIdentidad like concat(:q,'%') " +
                "or lower(p.nombres) like lower(concat('%',:q,'%')) or lower(p.apellidos) like lower(concat('%',:q,'%')) order by p.apellidos")
        Page<Paciente> buscar(@Param("q") String q, Pageable pageable);
    }

    public interface TutorRepository extends JpaRepository<Tutor, Long> {
        Optional<Tutor> findByDocumentoIdentidad(String documento);
    }

    public interface MedicoRepository extends JpaRepository<Medico, Long> {
        List<Medico> findByEspecialidad_IdEspecialidadAndEstadoTrue(Long especialidadId);
        List<Medico> findByEstadoTrueOrderByApellidos();
    }

    public interface AdministrativoRepository extends JpaRepository<Administrativo, Long> {}

    public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {
        List<Especialidad> findByActivoTrueOrderByNombreEspecialidad();
        Optional<Especialidad> findByNombreEspecialidadIgnoreCase(String nombre);
        boolean existsByNombreEspecialidadIgnoreCase(String nombre);
    }

    public interface ConsultorioRepository extends JpaRepository<Consultorio, Long> {
        List<Consultorio> findByActivoTrueOrderByNumeroPabellonAscCodigoRoomAsc();
    }

    public interface ConfigTurnoRepository extends JpaRepository<ConfigTurno, Long> {
        Optional<ConfigTurno> findByTurno(Turno turno);
    }

    public interface HorarioRepository extends JpaRepository<HorarioDisponible, Long> {
        @Query("select h from HorarioDisponible h join fetch h.medico m join fetch m.especialidad e join fetch h.consultorio " +
                "where e.idEspecialidad = :esp and h.fecha = :fecha and h.disponible = true order by h.horaInicio, m.apellidos")
        List<HorarioDisponible> disponibles(@Param("esp") Long especialidadId, @Param("fecha") LocalDate fecha);

        @Query("select h from HorarioDisponible h join fetch h.medico m join fetch m.especialidad e join fetch h.consultorio " +
                "where e.idEspecialidad = :esp and h.fecha = :fecha and h.turno = :turno and h.disponible = true order by h.horaInicio, m.apellidos")
        List<HorarioDisponible> disponiblesPorTurno(@Param("esp") Long especialidadId, @Param("fecha") LocalDate fecha, @Param("turno") Turno turno);

        @Query("select distinct h.fecha from HorarioDisponible h where h.medico.especialidad.idEspecialidad = :esp " +
                "and h.disponible = true and h.fecha >= :desde order by h.fecha")
        List<LocalDate> fechasDisponibles(@Param("esp") Long especialidadId, @Param("desde") LocalDate desde);

        @Query("select h from HorarioDisponible h join fetch h.medico m join fetch m.especialidad join fetch h.consultorio " +
                "where m.idUsuario = :medico and h.fecha = :fecha order by h.horaInicio")
        List<HorarioDisponible> gestion(@Param("medico") Long medicoId, @Param("fecha") LocalDate fecha);

        List<HorarioDisponible> findByMedico_IdUsuarioAndFechaBetween(Long medicoId, LocalDate desde, LocalDate hasta);
    }

    public interface CitaRepository extends JpaRepository<Cita, Long> {
        boolean existsByClaveActiva(String clave);
        boolean existsByClaveActivaAndIdCitaNot(String clave, Long idCita);
        boolean existsByHorario_IdHorario(Long horarioId);
        boolean existsByHorario_IdHorarioAndEstadoIn(Long horarioId, Collection<EstadoCita> estados);
        boolean existsByPaciente_IdUsuarioAndMedico_IdUsuario(Long pacienteId, Long medicoId);

        @EntityGraph(attributePaths = {"paciente", "medico", "horario", "consultorio", "especialidad", "reservadaPor"})
        @Query("select c from Cita c where c.paciente.idUsuario = :id order by c.horario.fecha desc, c.horario.horaInicio desc")
        Page<Cita> historial(@Param("id") Long pacienteId, Pageable pageable);

        @EntityGraph(attributePaths = {"paciente", "medico", "horario", "consultorio", "especialidad", "reservadaPor"})
        @Query("select c from Cita c where c.medico.idUsuario = :medico and c.horario.fecha = :fecha order by c.horario.horaInicio")
        List<Cita> agenda(@Param("medico") Long medicoId, @Param("fecha") LocalDate fecha);

        @EntityGraph(attributePaths = {"paciente", "medico", "horario", "consultorio", "especialidad", "reservadaPor"})
        @Query("select c from Cita c where c.horario.fecha = :fecha order by c.horario.horaInicio, c.medico.apellidos")
        List<Cita> delDia(@Param("fecha") LocalDate fecha);

        @EntityGraph(attributePaths = {"paciente", "medico", "horario", "consultorio", "especialidad", "reservadaPor"})
        @Query("select c from Cita c where c.medico.idUsuario = :medico and c.horario.fecha between :desde and :hasta " +
                "order by c.horario.fecha, c.horario.horaInicio")
        List<Cita> agendaRango(@Param("medico") Long medicoId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

        /** Todas las citas de un médico (para "Mis pacientes" y su resumen). */
        @EntityGraph(attributePaths = {"paciente", "horario"})
        @Query("select c from Cita c where c.medico.idUsuario = :medico")
        List<Cita> deMedico(@Param("medico") Long medicoId);

        @Query("select c from Cita c where c.horario.fecha = :fecha and c.estado in :estados and c.recordatorioEnviado = false")
        List<Cita> pendientesRecordatorio(@Param("fecha") LocalDate fecha, @Param("estados") Collection<EstadoCita> estados);

        @Query("select c from Cita c where c.horario.fecha <= :hasta and c.estado in :estados")
        List<Cita> vencidas(@Param("hasta") LocalDate hasta, @Param("estados") Collection<EstadoCita> estados);

        @Query("select c.estado, count(c) from Cita c where c.horario.fecha between :desde and :hasta group by c.estado")
        List<Object[]> contarPorEstado(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

        @Query("select c.especialidad.nombreEspecialidad, c.estado, count(c) from Cita c " +
                "where c.horario.fecha between :desde and :hasta group by c.especialidad.nombreEspecialidad, c.estado")
        List<Object[]> contarPorEspecialidadYEstado(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

        @Query("select c.estado, count(c) from Cita c group by c.estado")
        List<Object[]> contarTodasPorEstado();

        @Query("select avg(c.duracionReservaSeg) from Cita c where c.duracionReservaSeg is not null")
        Double promedioDuracionReserva();
    }

    public interface TriajeRepository extends JpaRepository<Triaje, Long> {
        Optional<Triaje> findByCita_IdCita(Long citaId);
    }

    public interface HistorialRepository extends JpaRepository<HistorialClinico, Long> {
        Optional<HistorialClinico> findByCita_IdCita(Long citaId);
        List<HistorialClinico> findByPaciente_IdUsuarioOrderByFechaRegistroDesc(Long pacienteId);
    }

    public interface RecetaRepository extends JpaRepository<RecetaMedica, Long> {
        Optional<RecetaMedica> findByHistorial_IdHistorial(Long historialId);
        Page<RecetaMedica> findByEstadoInOrderByFechaAsc(Collection<EstadoReceta> estados, Pageable pageable);
    }

    public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {
        List<Medicamento> findByActivoTrueOrderByNombre();

        @Query("select m from Medicamento m where lower(m.nombre) like lower(concat('%',:q,'%')) " +
                "or lower(m.principioActivo) like lower(concat('%',:q,'%')) order by m.nombre")
        Page<Medicamento> buscar(@Param("q") String q, Pageable pageable);

        @Query("select m from Medicamento m where m.activo = true and m.stockActual <= m.stockMinimo order by m.nombre")
        List<Medicamento> stockBajo();

        @Query("select m from Medicamento m where m.activo = true and m.fechaVencimiento is not null " +
                "and m.fechaVencimiento <= :limite order by m.fechaVencimiento")
        List<Medicamento> porVencer(@Param("limite") LocalDate limite);

        /** Bloqueo pesimista al descontar stock (dispensación concurrente). */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select m from Medicamento m where m.idMedicamento = :id")
        Optional<Medicamento> bloquear(@Param("id") Long id);
    }

    public interface MovimientoRepository extends JpaRepository<MovimientoInventario, Long> {
        Page<MovimientoInventario> findAllByOrderByFechaDesc(Pageable pageable);
    }

    public interface DispensacionRepository extends JpaRepository<Dispensacion, Long> {
        Page<Dispensacion> findByFechaBetweenOrderByFechaDesc(java.time.LocalDateTime desde, java.time.LocalDateTime hasta, Pageable pageable);
    }

    public interface PagoRepository extends JpaRepository<Pago, Long> {
        Optional<Pago> findFirstByCita_IdCitaAndEstadoOrderByFechaDesc(Long citaId, EstadoPago estado);
        Optional<Pago> findFirstByDispensacion_IdDispensacionOrderByFechaDesc(Long dispensacionId);
        List<Pago> findByPaciente_IdUsuarioOrderByFechaDesc(Long pacienteId);
        List<Pago> findByFechaBetweenOrderByFechaDesc(java.time.LocalDateTime desde, java.time.LocalDateTime hasta);
    }

    public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {
        List<ListaEspera> findByEspecialidad_IdEspecialidadAndEstadoInOrderByFechaRegistro(Long especialidadId, Collection<EstadoEspera> estados);
        List<ListaEspera> findByEstadoInOrderByFechaRegistro(Collection<EstadoEspera> estados);
        List<ListaEspera> findByPaciente_IdUsuarioAndEstadoInOrderByFechaRegistro(Long pacienteId, Collection<EstadoEspera> estados);
        List<ListaEspera> findByPaciente_IdUsuarioAndEspecialidad_IdEspecialidadAndEstadoIn(Long pacienteId, Long especialidadId, Collection<EstadoEspera> estados);
        Optional<ListaEspera> findFirstByEspecialidad_IdEspecialidadAndEstadoOrderByFechaRegistro(Long especialidadId, EstadoEspera estado);
    }

    public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
        Page<Notificacion> findByDestinatario_IdUsuarioOrderByFechaEnvioDesc(Long usuarioId, Pageable pageable);
        long countByDestinatario_IdUsuarioAndLeidoFalse(Long usuarioId);
        List<Notificacion> findByDestinatario_IdUsuarioAndLeidoFalse(Long usuarioId);
    }

    public interface AuditoriaRepository extends JpaRepository<AuditoriaLog, Long> {
        Page<AuditoriaLog> findAllByOrderByFechaHoraDesc(Pageable pageable);
    }
}
