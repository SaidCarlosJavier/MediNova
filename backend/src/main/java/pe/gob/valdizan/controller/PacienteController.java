package pe.gob.valdizan.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.gob.valdizan.domain.Enums.EstadoEspera;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.ListaEsperaService;
import pe.gob.valdizan.service.NotificacionService;
import pe.gob.valdizan.service.PacienteService;

import java.util.List;
import java.util.Map;

@Tag(name = "Pacientes, tutores, lista de espera y notificaciones")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PacienteController {
    private final PacienteService pacientes;
    private final ListaEsperaService espera;
    private final NotificacionService notificaciones;

    // RF-01 / RF-17: el tutor registra pacientes a su cargo; recepción registra paciente y tutor
    @PreAuthorize("hasAnyRole('TUTOR','RECEPCIONISTA','ADMIN')")
    @PostMapping("/pacientes")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroPacienteResponse registrar(@Valid @RequestBody PacienteRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return pacientes.registrar(r, p);
    }

    @PreAuthorize("hasRole('TUTOR')")
    @PostMapping("/pacientes/vincular")
    public PacienteDto vincular(@Valid @RequestBody VincularRequest r, @AuthenticationPrincipal UsuarioPrincipal p) { return pacientes.vincular(r, p); }

    // "pacientes a mi cargo"
    @PreAuthorize("hasRole('TUTOR')")
    @GetMapping("/pacientes/a-cargo")
    public List<PacienteDto> aCargo(@AuthenticationPrincipal UsuarioPrincipal p) { return pacientes.aCargo(p.getId()); }

    // RF-18
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping("/pacientes/documento/{documento}")
    public PacienteDto porDocumento(@PathVariable String documento) { return pacientes.porDocumento(documento); }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping("/pacientes")
    public PageDto<PacienteDto> buscar(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int pagina,
                                       @RequestParam(defaultValue = "10") int tamanio) {
        return pacientes.buscar(q, pagina, Math.min(tamanio, 50));
    }

    // RF-24 lista de espera
    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN')")
    @PostMapping("/espera")
    @ResponseStatus(HttpStatus.CREATED)
    public ListaEsperaDto registrarEspera(@Valid @RequestBody ListaEsperaRequest r, @AuthenticationPrincipal UsuarioPrincipal p) { return espera.registrar(r, p); }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping("/espera")
    public List<ListaEsperaDto> listarEspera(@RequestParam(required = false) Long especialidadId) { return espera.listar(especialidadId); }

    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN')")
    @GetMapping("/espera/paciente/{pacienteId}")
    public List<ListaEsperaDto> esperaPaciente(@PathVariable Long pacienteId, @AuthenticationPrincipal UsuarioPrincipal p) { return espera.delPaciente(pacienteId, p); }

    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN')")
    @PatchMapping("/espera/{id}")
    public ListaEsperaDto estadoEspera(@PathVariable Long id, @RequestParam EstadoEspera estado, @AuthenticationPrincipal UsuarioPrincipal p) {
        return espera.cambiarEstado(id, estado, p);
    }

    // RF-11/12/13 notificaciones
    @GetMapping("/notificaciones")
    public PageDto<NotificacionDto> notificaciones(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio,
                                                   @AuthenticationPrincipal UsuarioPrincipal p) {
        return notificaciones.listar(p, pagina, Math.min(tamanio, 50));
    }

    @GetMapping("/notificaciones/no-leidas")
    public Map<String, Long> noLeidas(@AuthenticationPrincipal UsuarioPrincipal p) { return Map.of("total", notificaciones.noLeidas(p)); }

    @PatchMapping("/notificaciones/{id}/leida")
    public MensajeDto leida(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal p) {
        notificaciones.marcarLeido(id, p);
        return new MensajeDto("Notificación marcada como leída");
    }

    @PatchMapping("/notificaciones/leer-todas")
    public MensajeDto leerTodas(@AuthenticationPrincipal UsuarioPrincipal p) {
        notificaciones.leerTodas(p);
        return new MensajeDto("Todas las notificaciones fueron marcadas como leídas");
    }
}
