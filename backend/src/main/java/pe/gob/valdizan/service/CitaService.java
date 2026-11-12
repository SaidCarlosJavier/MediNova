package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.EstadoCita;
import pe.gob.valdizan.domain.Enums.Origen;
import pe.gob.valdizan.domain.Enums.TipoNotificacion;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.*;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Núcleo del sistema: reserva, reprogramación, cancelación, check-in, atención y triaje (RF-07..RF-11, RF-20..RF-23, RF-30). */
@Service
@RequiredArgsConstructor
public class CitaService {
    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CitaRepository citas;
    private final HorarioRepository horarios;
    private final UsuarioRepository usuarios;
    private final TriajeRepository triajes;
    private final AccesoService acceso;
    private final NotificacionService notificaciones;
    private final ListaEsperaService listaEspera;
    private final PagoService pagoService;

    // ------------------------------------------------------------------ reservar
    @Transactional
    public CitaDto reservar(ReservaRequest r, UsuarioPrincipal p) {
        Paciente pac = acceso.paciente(r.pacienteId(), p);
        HorarioDisponible h = horarios.findById(r.horarioId())
                .orElseThrow(() -> ApiException.noEncontrado("El horario seleccionado no existe"));
        validarFuturo(h);
        if (!h.isDisponible()) throw ApiException.conflicto("El horario seleccionado ya no está disponible");

        Cita c = new Cita();
        c.setPaciente(pac);
        c.setHorario(h);
        c.setMedico(h.getMedico());
        c.setConsultorio(h.getConsultorio());
        c.setEspecialidad(h.getMedico().getEspecialidad());
        boolean personal = "ROLE_RECEPCIONISTA".equals(p.getRol()) || "ROLE_MEDICO".equals(p.getRol());
        c.setOrigen(personal ? Origen.PRESENCIAL : (r.origen() != null && r.origen() != Origen.PRESENCIAL ? r.origen() : Origen.WEB));
        c.setFechaHoraReserva(LocalDateTime.now());
        c.setEstado(EstadoCita.RESERVADA);
        c.setReservadaPor(usuarios.getReferenceById(p.getId()));
        c.setDuracionReservaSeg(r.duracionReservaSeg() != null && r.duracionReservaSeg() >= 0 ? r.duracionReservaSeg() : null);

        validarDuplicidad(c, null);
        h.marcarComoOcupado();
        c.confirmar();
        c.setClaveActiva(c.construirClave());
        citas.saveAndFlush(c);
        horarios.save(h);

        listaEspera.cerrarPara(pac.getIdUsuario(), c.getEspecialidad().getIdEspecialidad());
        notificaciones.notificarPaciente(pac, TipoNotificacion.CONFIRMACION, "Su cita fue confirmada: " + detalle(c));
        return Mapper.cita(c);
    }

    // ------------------------------------------------------------------ reprogramar
    @Transactional
    public CitaDto reprogramar(Long citaId, Long nuevoHorarioId, UsuarioPrincipal p) {
        Cita c = cargar(citaId);
        validarAcceso(c, p);
        if (!c.isModificable()) throw ApiException.invalida("Solo se pueden reprogramar citas reservadas, confirmadas o reprogramadas");
        HorarioDisponible nuevo = horarios.findById(nuevoHorarioId)
                .orElseThrow(() -> ApiException.noEncontrado("El nuevo horario no existe"));
        HorarioDisponible viejo = c.getHorario();
        if (viejo.getIdHorario().equals(nuevo.getIdHorario())) throw ApiException.invalida("Seleccione un horario distinto al actual");
        validarFuturo(nuevo);
        if (!nuevo.isDisponible()) throw ApiException.conflicto("El nuevo horario ya no está disponible");
        if (!nuevo.getMedico().getEspecialidad().getIdEspecialidad().equals(c.getEspecialidad().getIdEspecialidad()))
            throw ApiException.invalida("El nuevo horario debe ser de la misma especialidad");
        if (citas.existsByClaveActivaAndIdCitaNot(c.construirClave(nuevo), c.getIdCita()))
            throw ApiException.conflicto("El paciente ya tiene una cita activa en esa especialidad, fecha y hora");

        nuevo.marcarComoOcupado();
        viejo.liberar();
        c.reprogramar(nuevo);
        citas.saveAndFlush(c);
        horarios.save(nuevo);
        horarios.save(viejo);

        notificaciones.notificarPaciente(c.getPaciente(), TipoNotificacion.REPROGRAMACION, "Su cita fue reprogramada: " + detalle(c));
        listaEspera.avisarPrimero(c.getEspecialidad());
        return Mapper.cita(c);
    }

    // ------------------------------------------------------------------ cancelar
    @Transactional
    public CitaDto cancelar(Long citaId, UsuarioPrincipal p) {
        Cita c = cargar(citaId);
        validarAcceso(c, p);
        String detalle = detalle(c);
        c.cancelar();
        c.getHorario().liberar();
        if (c.isPagada()) pagoService.reembolsarPorCancelacion(c, p.getId());
        citas.saveAndFlush(c);
        horarios.save(c.getHorario());

        notificaciones.notificarPaciente(c.getPaciente(), TipoNotificacion.CANCELACION, "Su cita fue cancelada: " + detalle);
        listaEspera.avisarPrimero(c.getEspecialidad());
        return Mapper.cita(c);
    }

    // ------------------------------------------------------------------ check-in (RF-23)
    @Transactional
    public CitaDto checkIn(Long citaId) {
        Cita c = cargar(citaId);
        if (!c.getHorario().getFecha().equals(LocalDate.now()))
            throw ApiException.invalida("El check-in solo puede registrarse el día de la cita");
        c.registrarCheckIn();
        return Mapper.cita(c);
    }

    /** Inasistencia registrada por el médico o recepción (solo el día de la cita o después). */
    @Transactional
    public CitaDto marcarNoAsistio(Long citaId, UsuarioPrincipal p) {
        Cita c = cargar(citaId);
        if ("ROLE_MEDICO".equals(p.getRol())) validarMedicoDeLaCita(c, p);
        if (!c.isModificable()) throw ApiException.invalida("Solo puede registrarse inasistencia en citas vigentes");
        if (c.getHorario().getFecha().isAfter(LocalDate.now()))
            throw ApiException.invalida("Aún no puede registrarse la inasistencia: la cita es en una fecha futura");
        c.marcarNoAsistio();
        return Mapper.cita(c);
    }

    // ------------------------------------------------------------------ atención (RF-30)
    @Transactional
    public CitaDto marcarAtendida(Long citaId, UsuarioPrincipal p) {
        Cita c = cargar(citaId);
        validarMedicoDeLaCita(c, p);
        c.marcarAtendida();
        return Mapper.cita(c);
    }

    @Transactional
    public TriajeDto registrarTriaje(Long citaId, TriajeRequest r, UsuarioPrincipal p) {
        Cita c = cargar(citaId);
        if ("ROLE_MEDICO".equals(p.getRol())) validarMedicoDeLaCita(c, p);
        if (c.getEstado() == EstadoCita.CANCELADA || c.getEstado() == EstadoCita.NO_ASISTIO)
            throw ApiException.invalida("No se puede registrar triaje en una cita cancelada o con inasistencia");
        Triaje t = triajes.findByCita_IdCita(citaId).orElseGet(() -> {
            Triaje n = new Triaje();
            n.setCita(c);
            return n;
        });
        t.setPresionArterial(r.presionArterial());
        t.setPeso(r.peso());
        t.setSignosVitales(r.signosVitales());
        t.setFechaRegistro(LocalDateTime.now());
        return Mapper.triaje(triajes.save(t));
    }

    @Transactional(readOnly = true)
    public TriajeDto triaje(Long citaId, UsuarioPrincipal p) {
        Cita c = cargar(citaId);
        if ("ROLE_MEDICO".equals(p.getRol())) validarMedicoDeLaCita(c, p);
        return triajes.findByCita_IdCita(citaId).map(Mapper::triaje).orElse(null);
    }

    // ------------------------------------------------------------------ consultas
    @Transactional(readOnly = true)
    public CitaDto obtener(Long id, UsuarioPrincipal p) {
        Cita c = cargar(id);
        if ("ROLE_MEDICO".equals(p.getRol())) validarMedicoDeLaCita(c, p);
        else validarAcceso(c, p);
        return Mapper.cita(c);
    }

    @Transactional(readOnly = true)
    public PageDto<CitaDto> historial(Long pacienteId, int pagina, int tamanio, UsuarioPrincipal p) {
        acceso.paciente(pacienteId, p);
        return PageDto.de(citas.historial(pacienteId, PageRequest.of(pagina, tamanio)), Mapper::cita);
    }

    @Transactional(readOnly = true)
    public List<CitaDto> agenda(Long medicoId, LocalDate fecha) {
        return citas.agenda(medicoId, fecha).stream().map(Mapper::cita).toList();
    }

    /** Agenda del médico en un rango de fechas (semana, mes...), máximo 62 días. */
    @Transactional(readOnly = true)
    public List<CitaDto> agendaRango(Long medicoId, LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) throw ApiException.invalida("La fecha final debe ser igual o posterior a la inicial");
        if (desde.plusDays(62).isBefore(hasta)) throw ApiException.invalida("El rango máximo es de 62 días");
        return citas.agendaRango(medicoId, desde, hasta).stream().map(Mapper::cita).toList();
    }

    @Transactional(readOnly = true)
    public List<CitaDto> delDia(LocalDate fecha) {
        return citas.delDia(fecha).stream().map(Mapper::cita).toList();
    }

    // ------------------------------------------------------------------ tareas programadas
    /** RF-12: recordatorio 24 h antes (citas de mañana). */
    @Transactional
    public int enviarRecordatorios() {
        List<Cita> l = citas.pendientesRecordatorio(LocalDate.now().plusDays(1), Cita.MODIFICABLES);
        for (Cita c : l) {
            notificaciones.notificarPaciente(c.getPaciente(), TipoNotificacion.RECORDATORIO, "Recordatorio: mañana tiene una cita. " + detalle(c));
            c.setRecordatorioEnviado(true);
        }
        return l.size();
    }

    /** Citas vigentes cuyo día ya terminó sin check-in pasan a NO_ASISTIO (base de los reportes RF-33). */
    @Transactional
    public int marcarInasistencias() {
        List<Cita> l = citas.vencidas(LocalDate.now(), Cita.MODIFICABLES);
        l.forEach(Cita::marcarNoAsistio);
        return l.size();
    }

    // ------------------------------------------------------------------ auxiliares
    /** RF-35: validación de duplicados a nivel de servicio (además de la restricción única en BD). */
    void validarDuplicidad(Cita c, Long excluirId) {
        String clave = c.construirClave();
        boolean existe = excluirId == null ? citas.existsByClaveActiva(clave) : citas.existsByClaveActivaAndIdCitaNot(clave, excluirId);
        if (existe) throw ApiException.conflicto("El paciente ya tiene una cita activa en esta especialidad, fecha y hora");
    }

    private void validarFuturo(HorarioDisponible h) {
        if (!LocalDateTime.of(h.getFecha(), h.getHoraInicio()).isAfter(LocalDateTime.now()))
            throw ApiException.invalida("No se puede reservar un horario que ya pasó");
    }

    private Cita cargar(Long id) {
        return citas.findById(id).orElseThrow(() -> ApiException.noEncontrado("La cita no existe"));
    }

    private void validarAcceso(Cita c, UsuarioPrincipal p) {
        if (p.esPersonalAdministrativo()) return;
        if ("ROLE_MEDICO".equals(p.getRol())) { validarMedicoDeLaCita(c, p); return; }
        acceso.paciente(c.getPaciente().getIdUsuario(), p);
    }

    private void validarMedicoDeLaCita(Cita c, UsuarioPrincipal p) {
        if (!c.getMedico().getIdUsuario().equals(p.getId()))
            throw ApiException.prohibido("Solo el médico asignado puede realizar esta acción sobre la cita");
    }

    private String detalle(Cita c) {
        HorarioDisponible h = c.getHorario();
        return c.getEspecialidad().getNombreEspecialidad() + " con " + c.getMedico().getNombreCompleto() + " el "
                + h.getFecha().format(F_FECHA) + " a las " + h.getHoraInicio().format(F_HORA) + " (" + c.getConsultorio().getUbicacion() + ")";
    }
}
