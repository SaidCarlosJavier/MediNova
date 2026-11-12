package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.EstadoCita;
import pe.gob.valdizan.dto.Dto.HistorialDto;
import pe.gob.valdizan.dto.Dto.HistorialRequest;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.*;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.time.LocalDateTime;
import java.util.List;

/** RF-28/29: historial clínico y recetas. Acceso: médico (solo si ha atendido al paciente), paciente/tutor en lectura. Todo se audita (RNF-14). */
@Service
@RequiredArgsConstructor
public class ClinicoService {
    private final HistorialRepository historiales;
    private final RecetaRepository recetas;
    private final CitaRepository citas;
    private final TriajeRepository triajes;
    private final MedicamentoRepository medicamentos;
    private final AccesoService acceso;
    private final AuditoriaService auditoria;

    @Transactional
    public HistorialDto registrar(Long citaId, HistorialRequest r, UsuarioPrincipal p) {
        Cita c = citas.findById(citaId).orElseThrow(() -> ApiException.noEncontrado("La cita no existe"));
        if (!c.getMedico().getIdUsuario().equals(p.getId()))
            throw ApiException.prohibido("Solo el médico asignado puede registrar el historial de esta cita");
        if (c.getEstado() == EstadoCita.CANCELADA || c.getEstado() == EstadoCita.NO_ASISTIO)
            throw ApiException.invalida("No se puede registrar historial en una cita cancelada o con inasistencia");

        boolean nuevo = historiales.findByCita_IdCita(citaId).isEmpty();
        HistorialClinico h = historiales.findByCita_IdCita(citaId).orElseGet(() -> {
            HistorialClinico n = new HistorialClinico();
            n.setCita(c);
            n.setPaciente(c.getPaciente());
            n.setMedico(c.getMedico());
            return n;
        });
        h.setDiagnostico(r.diagnostico());
        h.setTratamiento(r.tratamiento());
        h.setObservaciones(r.observaciones());
        h.setFechaRegistro(LocalDateTime.now());
        historiales.save(h);

        RecetaMedica receta = recetas.findByHistorial_IdHistorial(h.getIdHistorial()).orElse(null);
        if (r.detalles() != null && !r.detalles().isEmpty()) {
            if (receta != null && receta.getEstado() != Enums.EstadoReceta.PENDIENTE)
                throw ApiException.conflicto("La receta ya fue dispensada y no puede modificarse");
            if (receta == null) {
                receta = new RecetaMedica();
                receta.setHistorial(h);
            }
            receta.setFecha(LocalDateTime.now());
            receta.setIndicaciones(r.indicaciones());
            receta.getDetalles().clear();
            for (var d : r.detalles()) {
                Medicamento m = medicamentos.findById(d.medicamentoId())
                        .orElseThrow(() -> ApiException.noEncontrado("El medicamento seleccionado no existe"));
                if (!m.isActivo()) throw ApiException.invalida("El medicamento " + m.getNombre() + " no está activo en el catálogo");
                DetalleReceta det = new DetalleReceta();
                det.setMedicamento(m);
                det.setDosis(d.dosis());
                det.setCantidad(d.cantidad());
                receta.agregar(det);
            }
            recetas.save(receta);
        }
        auditoria.registrar(p.getId(), nuevo ? "CREAR" : "MODIFICAR", "HistorialClinico",
                "Historial " + h.getIdHistorial() + " de paciente " + c.getPaciente().getIdUsuario());
        return Mapper.historial(h, receta, triajes.findByCita_IdCita(citaId).orElse(null));
    }

    /** Historial completo de un paciente. Médicos: solo si tienen/tuvieron citas con él. */
    @Transactional
    public List<HistorialDto> porPaciente(Long pacienteId, UsuarioPrincipal p) {
        if ("ROLE_MEDICO".equals(p.getRol())) {
            if (!citas.existsByPaciente_IdUsuarioAndMedico_IdUsuario(pacienteId, p.getId()))
                throw ApiException.prohibido("Solo el médico tratante puede consultar el historial de este paciente");
        } else if ("ROLE_PACIENTE".equals(p.getRol()) || "ROLE_TUTOR".equals(p.getRol())) {
            acceso.paciente(pacienteId, p); // lectura propia o del tutor
        } else {
            throw ApiException.prohibido("Su rol no tiene acceso al historial clínico");
        }
        List<HistorialDto> res = historiales.findByPaciente_IdUsuarioOrderByFechaRegistroDesc(pacienteId).stream()
                .map(h -> Mapper.historial(h, recetas.findByHistorial_IdHistorial(h.getIdHistorial()).orElse(null),
                        triajes.findByCita_IdCita(h.getCita().getIdCita()).orElse(null))).toList();
        auditoria.registrar(p.getId(), "CONSULTAR", "HistorialClinico", "Consulta del historial del paciente " + pacienteId);
        return res;
    }

    @Transactional
    public HistorialDto porCita(Long citaId, UsuarioPrincipal p) {
        Cita c = citas.findById(citaId).orElseThrow(() -> ApiException.noEncontrado("La cita no existe"));
        if ("ROLE_MEDICO".equals(p.getRol())) {
            if (!c.getMedico().getIdUsuario().equals(p.getId())) throw ApiException.prohibido("Solo el médico asignado puede ver este registro");
        } else acceso.paciente(c.getPaciente().getIdUsuario(), p);
        HistorialClinico h = historiales.findByCita_IdCita(citaId).orElse(null);
        if (h == null) return null;
        auditoria.registrar(p.getId(), "CONSULTAR", "HistorialClinico", "Consulta del historial " + h.getIdHistorial());
        return Mapper.historial(h, recetas.findByHistorial_IdHistorial(h.getIdHistorial()).orElse(null),
                triajes.findByCita_IdCita(citaId).orElse(null));
    }
}
