package pe.gob.valdizan.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.gob.valdizan.dto.Dto.HistorialDto;
import pe.gob.valdizan.dto.Dto.HistorialRequest;
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.ClinicoService;

import java.util.List;

/** Historial clínico: escritura solo médico tratante; lectura médico tratante, paciente o su tutor (Ley 29733). */
@Tag(name = "Historial clínico")
@RestController
@RequestMapping("/api/historial")
@RequiredArgsConstructor
public class ClinicoController {
    private final ClinicoService clinico;

    @PreAuthorize("hasRole('MEDICO')")
    @PutMapping("/cita/{citaId}")
    public HistorialDto registrar(@PathVariable Long citaId, @Valid @RequestBody HistorialRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return clinico.registrar(citaId, r, p);
    }

    @PreAuthorize("hasAnyRole('MEDICO','PACIENTE','TUTOR')")
    @GetMapping("/cita/{citaId}")
    public HistorialDto porCita(@PathVariable Long citaId, @AuthenticationPrincipal UsuarioPrincipal p) { return clinico.porCita(citaId, p); }

    @PreAuthorize("hasAnyRole('MEDICO','PACIENTE','TUTOR')")
    @GetMapping("/paciente/{pacienteId}")
    public List<HistorialDto> porPaciente(@PathVariable Long pacienteId, @AuthenticationPrincipal UsuarioPrincipal p) {
        return clinico.porPaciente(pacienteId, p);
    }
}
