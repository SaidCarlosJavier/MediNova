package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.EstadoCita;
import pe.gob.valdizan.domain.Enums.Turno;
import pe.gob.valdizan.dto.Dto.GenerarHorariosRequest;
import pe.gob.valdizan.dto.Dto.HorarioDto;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.*;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Disponibilidad (RF-06) y gestión de horarios por recepción/administración (RF-19, RF-34). */
@Service
@RequiredArgsConstructor
public class HorarioService {
    private static final Set<EstadoCita> OCUPAN = EnumSet.of(EstadoCita.RESERVADA, EstadoCita.CONFIRMADA, EstadoCita.REPROGRAMADA, EstadoCita.ASISTIO);

    private final HorarioRepository horarios;
    private final MedicoRepository medicos;
    private final ConsultorioRepository consultorios;
    private final ConfigTurnoRepository turnos;
    private final CitaRepository citas;

    @Transactional(readOnly = true)
    public List<HorarioDto> disponibles(Long especialidadId, LocalDate fecha, Turno turno) {
        List<HorarioDisponible> l = turno == null ? horarios.disponibles(especialidadId, fecha)
                : horarios.disponiblesPorTurno(especialidadId, fecha, turno);
        LocalDateTime ahora = LocalDateTime.now();
        return l.stream().filter(h -> LocalDateTime.of(h.getFecha(), h.getHoraInicio()).isAfter(ahora)).map(Mapper::horario).toList();
    }

    @Transactional(readOnly = true)
    public List<LocalDate> fechasDisponibles(Long especialidadId) {
        return horarios.fechasDisponibles(especialidadId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<HorarioDto> gestion(Long medicoId, LocalDate fecha) {
        return horarios.gestion(medicoId, fecha).stream().map(Mapper::horario).toList();
    }

    /** El médico solo puede administrar sus propios horarios; recepción/administración los de cualquiera. */
    private void validarPropietario(Long medicoId, UsuarioPrincipal p) {
        if ("ROLE_MEDICO".equals(p.getRol()) && !p.getId().equals(medicoId))
            throw ApiException.prohibido("Solo puede administrar sus propios horarios");
    }

    @Transactional
    public int generar(GenerarHorariosRequest r, UsuarioPrincipal p) {
        validarPropietario(r.medicoId(), p);
        Medico m = medicos.findById(r.medicoId()).orElseThrow(() -> ApiException.noEncontrado("El médico no existe"));
        Consultorio c = consultorios.findById(r.consultorioId()).orElseThrow(() -> ApiException.noEncontrado("El consultorio no existe"));
        return generar(m, c, r.desde(), r.hasta(), r.turno());
    }

    /** Idempotente: no duplica horarios existentes del mismo médico, fecha y hora. */
    @Transactional
    public int generar(Medico m, Consultorio c, LocalDate desde, LocalDate hasta, Turno turno) {
        if (hasta.isBefore(desde)) throw ApiException.invalida("La fecha de fin debe ser igual o posterior a la de inicio");
        if (desde.isBefore(LocalDate.now())) throw ApiException.invalida("No se pueden generar horarios en fechas pasadas");
        if (desde.plusDays(62).isBefore(hasta)) throw ApiException.invalida("El rango máximo es de 62 días");
        ConfigTurno cfg = turnos.findByTurno(turno).orElseThrow(() -> ApiException.invalida("El turno no está configurado"));

        Set<String> existentes = new HashSet<>();
        horarios.findByMedico_IdUsuarioAndFechaBetween(m.getIdUsuario(), desde, hasta)
                .forEach(h -> existentes.add(h.getFecha() + "|" + h.getHoraInicio()));

        List<HorarioDisponible> nuevos = new ArrayList<>();
        for (LocalDate f = desde; !f.isAfter(hasta); f = f.plusDays(1)) {
            for (LocalTime t = cfg.getHoraInicio(); !t.plusMinutes(cfg.getDuracionMinutos()).isAfter(cfg.getHoraFin()); t = t.plusMinutes(cfg.getDuracionMinutos())) {
                if (existentes.contains(f + "|" + t)) continue;
                HorarioDisponible h = new HorarioDisponible();
                h.setFecha(f);
                h.setHoraInicio(t);
                h.setHoraFin(t.plusMinutes(cfg.getDuracionMinutos()));
                h.setTurno(turno);
                h.setMedico(m);
                h.setConsultorio(c);
                h.setDisponible(true);
                nuevos.add(h);
            }
        }
        horarios.saveAll(nuevos);
        return nuevos.size();
    }

    @Transactional
    public HorarioDto cambiarDisponibilidad(Long id, boolean valor, UsuarioPrincipal p) {
        HorarioDisponible h = horarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("El horario no existe"));
        validarPropietario(h.getMedico().getIdUsuario(), p);
        if (citas.existsByHorario_IdHorarioAndEstadoIn(id, OCUPAN))
            throw ApiException.conflicto("El horario tiene una cita activa; cancélela o reprográmela primero");
        h.setDisponible(valor);
        return Mapper.horario(h);
    }

    @Transactional
    public void eliminar(Long id, UsuarioPrincipal p) {
        HorarioDisponible h = horarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("El horario no existe"));
        validarPropietario(h.getMedico().getIdUsuario(), p);
        if (citas.existsByHorario_IdHorario(id))
            throw ApiException.conflicto("El horario tiene citas asociadas; bloquéelo en lugar de eliminarlo");
        horarios.delete(h);
    }
}
