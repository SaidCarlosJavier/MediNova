package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.ConfigTurno;
import pe.gob.valdizan.domain.Consultorio;
import pe.gob.valdizan.domain.Especialidad;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.*;

import java.util.List;

/** Especialidades, consultorios, médicos y turnos (RF-05, RF-14, RF-34). */
@Service
@RequiredArgsConstructor
public class CatalogoService {
    private final EspecialidadRepository especialidades;
    private final ConsultorioRepository consultorios;
    private final MedicoRepository medicos;
    private final ConfigTurnoRepository turnos;

    @Transactional(readOnly = true)
    public List<EspecialidadDto> especialidadesActivas() {
        return especialidades.findByActivoTrueOrderByNombreEspecialidad().stream().map(Mapper::especialidad).toList();
    }

    @Transactional(readOnly = true)
    public List<EspecialidadDto> todasEspecialidades() {
        return especialidades.findAll().stream().map(Mapper::especialidad).toList();
    }

    @Transactional
    public EspecialidadDto guardarEspecialidad(Long id, EspecialidadRequest r) {
        String nombre = r.nombre().trim();
        Especialidad e = id == null ? new Especialidad()
                : especialidades.findById(id).orElseThrow(() -> ApiException.noEncontrado("La especialidad no existe"));
        especialidades.findByNombreEspecialidadIgnoreCase(nombre).ifPresent(otra -> {
            if (!otra.getIdEspecialidad().equals(e.getIdEspecialidad()))
                throw ApiException.conflicto("Ya existe una especialidad con ese nombre");
        });
        e.setNombreEspecialidad(nombre);
        e.setDescripcion(r.descripcion());
        if (r.precioConsulta() != null) e.setPrecioConsulta(r.precioConsulta());
        if (r.activo() != null) e.setActivo(r.activo());
        return Mapper.especialidad(especialidades.save(e));
    }

    @Transactional(readOnly = true)
    public List<ConsultorioDto> consultoriosActivos() {
        return consultorios.findByActivoTrueOrderByNumeroPabellonAscCodigoRoomAsc().stream().map(Mapper::consultorio).toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultorioDto> todosConsultorios() {
        return consultorios.findAll().stream().map(Mapper::consultorio).toList();
    }

    @Transactional
    public ConsultorioDto guardarConsultorio(Long id, ConsultorioRequest r) {
        Consultorio c = id == null ? new Consultorio()
                : consultorios.findById(id).orElseThrow(() -> ApiException.noEncontrado("El consultorio no existe"));
        c.setNumeroPabellon(r.numeroPabellon().trim());
        c.setCodigoRoom(r.codigoRoom().trim());
        if (r.activo() != null) c.setActivo(r.activo());
        return Mapper.consultorio(consultorios.save(c));
    }

    @Transactional(readOnly = true)
    public List<MedicoDto> medicos(Long especialidadId) {
        var lista = especialidadId == null ? medicos.findByEstadoTrueOrderByApellidos()
                : medicos.findByEspecialidad_IdEspecialidadAndEstadoTrue(especialidadId);
        return lista.stream().map(Mapper::medico).toList();
    }

    @Transactional(readOnly = true)
    public List<ConfigTurnoDto> turnos() { return turnos.findAll().stream().map(Mapper::turno).toList(); }

    @Transactional
    public ConfigTurnoDto guardarTurno(ConfigTurnoDto r) {
        if (!r.horaFin().isAfter(r.horaInicio())) throw ApiException.invalida("La hora de fin debe ser posterior a la hora de inicio");
        ConfigTurno t = turnos.findByTurno(r.turno()).orElseGet(() -> {
            ConfigTurno n = new ConfigTurno();
            n.setTurno(r.turno());
            return n;
        });
        t.setHoraInicio(r.horaInicio());
        t.setHoraFin(r.horaFin());
        t.setDuracionMinutos(r.duracionMinutos());
        return Mapper.turno(turnos.save(t));
    }
}
