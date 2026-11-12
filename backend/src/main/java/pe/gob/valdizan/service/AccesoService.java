package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.gob.valdizan.domain.Paciente;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.CitaRepository;
import pe.gob.valdizan.repository.Repositorios.PacienteRepository;
import pe.gob.valdizan.security.UsuarioPrincipal;

/**
 * Regla "actuar en nombre de": el paciente, su tutor, el personal de recepción/administración
 * o (para citas de seguimiento) un médico que ya atiende al paciente.
 */
@Component
@RequiredArgsConstructor
public class AccesoService {
    private final PacienteRepository pacientes;
    private final CitaRepository citas;

    public Paciente paciente(Long pacienteId, UsuarioPrincipal p) {
        Paciente pac = pacientes.findById(pacienteId).orElseThrow(() -> ApiException.noEncontrado("El paciente no existe"));
        boolean propio = pac.getIdUsuario().equals(p.getId());
        boolean aCargo = pac.getTutor() != null && pac.getTutor().getIdUsuario().equals(p.getId());
        boolean medicoTratante = "ROLE_MEDICO".equals(p.getRol()) && citas.existsByPaciente_IdUsuarioAndMedico_IdUsuario(pacienteId, p.getId());
        if (!(p.esPersonalAdministrativo() || propio || aCargo || medicoTratante))
            throw ApiException.prohibido("No tiene autorización para actuar en nombre de este paciente");
        return pac;
    }
}
