package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.Enums.EstadoEspera;
import pe.gob.valdizan.domain.Enums.TipoNotificacion;
import pe.gob.valdizan.domain.Especialidad;
import pe.gob.valdizan.domain.ListaEspera;
import pe.gob.valdizan.domain.Paciente;
import pe.gob.valdizan.dto.Dto.ListaEsperaDto;
import pe.gob.valdizan.dto.Dto.ListaEsperaRequest;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.EspecialidadRepository;
import pe.gob.valdizan.repository.Repositorios.ListaEsperaRepository;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.time.LocalDateTime;
import java.util.List;

/** RF-24: lista de espera por especialidad; al liberarse un turno se avisa al primero en espera. */
@Service
@RequiredArgsConstructor
public class ListaEsperaService {
    private static final List<EstadoEspera> ACTIVOS = List.of(EstadoEspera.EN_ESPERA, EstadoEspera.NOTIFICADO);

    private final ListaEsperaRepository repo;
    private final EspecialidadRepository especialidades;
    private final AccesoService acceso;
    private final NotificacionService notificaciones;

    @Transactional
    public ListaEsperaDto registrar(ListaEsperaRequest r, UsuarioPrincipal p) {
        Paciente pac = acceso.paciente(r.pacienteId(), p);
        Especialidad e = especialidades.findById(r.especialidadId()).orElseThrow(() -> ApiException.noEncontrado("La especialidad no existe"));
        if (!repo.findByPaciente_IdUsuarioAndEspecialidad_IdEspecialidadAndEstadoIn(pac.getIdUsuario(), e.getIdEspecialidad(), ACTIVOS).isEmpty())
            throw ApiException.conflicto("El paciente ya está en la lista de espera de esta especialidad");
        ListaEspera l = new ListaEspera();
        l.setPaciente(pac);
        l.setEspecialidad(e);
        l.setFechaRegistro(LocalDateTime.now());
        l.setEstado(EstadoEspera.EN_ESPERA);
        return Mapper.espera(repo.save(l));
    }

    @Transactional(readOnly = true)
    public List<ListaEsperaDto> listar(Long especialidadId) {
        var l = especialidadId == null ? repo.findByEstadoInOrderByFechaRegistro(ACTIVOS)
                : repo.findByEspecialidad_IdEspecialidadAndEstadoInOrderByFechaRegistro(especialidadId, ACTIVOS);
        return l.stream().map(Mapper::espera).toList();
    }

    @Transactional(readOnly = true)
    public List<ListaEsperaDto> delPaciente(Long pacienteId, UsuarioPrincipal p) {
        acceso.paciente(pacienteId, p);
        return repo.findByPaciente_IdUsuarioAndEstadoInOrderByFechaRegistro(pacienteId, ACTIVOS).stream().map(Mapper::espera).toList();
    }

    @Transactional
    public ListaEsperaDto cambiarEstado(Long id, EstadoEspera nuevo, UsuarioPrincipal p) {
        if (nuevo != EstadoEspera.ATENDIDO && nuevo != EstadoEspera.CANCELADO)
            throw ApiException.invalida("Solo se permite marcar como ATENDIDO o CANCELADO");
        ListaEspera l = repo.findById(id).orElseThrow(() -> ApiException.noEncontrado("El registro de espera no existe"));
        acceso.paciente(l.getPaciente().getIdUsuario(), p);
        l.setEstado(nuevo);
        return Mapper.espera(l);
    }

    /** Se invoca al liberarse un turno (cancelación/reprogramación). */
    @Transactional
    public void avisarPrimero(Especialidad e) {
        repo.findFirstByEspecialidad_IdEspecialidadAndEstadoOrderByFechaRegistro(e.getIdEspecialidad(), EstadoEspera.EN_ESPERA)
                .ifPresent(l -> {
                    l.setEstado(EstadoEspera.NOTIFICADO);
                    notificaciones.notificarPaciente(l.getPaciente(), TipoNotificacion.LISTA_ESPERA,
                            "Se liberó un turno en " + e.getNombreEspecialidad() + ". Ingrese cuanto antes para reservar su cita.");
                });
    }

    /** Al reservar, se cierra la espera del paciente en esa especialidad. */
    @Transactional
    public void cerrarPara(Long pacienteId, Long especialidadId) {
        repo.findByPaciente_IdUsuarioAndEspecialidad_IdEspecialidadAndEstadoIn(pacienteId, especialidadId, ACTIVOS)
                .forEach(l -> l.setEstado(EstadoEspera.ATENDIDO));
    }
}
