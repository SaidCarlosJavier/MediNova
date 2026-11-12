package pe.gob.valdizan.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.valdizan.dto.Dto.MedicoPacienteDto;
import pe.gob.valdizan.dto.Dto.MedicoResumenDto;
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.MedicoService;

import java.util.List;

@Tag(name = "Médico")
@RestController
@RequestMapping("/api/medico")
@PreAuthorize("hasRole('MEDICO')")
@RequiredArgsConstructor
public class MedicoController {
    private final MedicoService medico;

    @GetMapping("/pacientes")
    public List<MedicoPacienteDto> pacientes(@RequestParam(defaultValue = "") String q, @AuthenticationPrincipal UsuarioPrincipal p) {
        return medico.misPacientes(p.getId(), q);
    }

    @GetMapping("/resumen")
    public MedicoResumenDto resumen(@AuthenticationPrincipal UsuarioPrincipal p) { return medico.resumen(p.getId()); }
}
