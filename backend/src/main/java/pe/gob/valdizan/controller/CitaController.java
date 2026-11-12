package pe.gob.valdizan.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.CitaService;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Citas")
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {
    private final CitaService citas;

    // RF-07 / RF-20 (paciente, tutor o recepción en nombre del paciente)
    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN','MEDICO')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaDto reservar(@Valid @RequestBody ReservaRequest r, @AuthenticationPrincipal UsuarioPrincipal p) { return citas.reservar(r, p); }

    // RF-08 / RF-21
    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN','MEDICO')")
    @PutMapping("/{id}/reprogramar")
    public CitaDto reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramarRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return citas.reprogramar(id, r.horarioId(), p);
    }

    // RF-09 / RF-22
    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN','MEDICO')")
    @PatchMapping("/{id}/cancelar")
    public CitaDto cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal p) { return citas.cancelar(id, p); }

    @PreAuthorize("hasAnyRole('MEDICO','RECEPCIONISTA','ADMIN')")
    @PatchMapping("/{id}/no-asistio")
    public CitaDto noAsistio(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal p) { return citas.marcarNoAsistio(id, p); }

    // RF-23
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @PatchMapping("/{id}/check-in")
    public CitaDto checkIn(@PathVariable Long id) { return citas.checkIn(id); }

    // RF-30
    @PreAuthorize("hasRole('MEDICO')")
    @PatchMapping("/{id}/atendida")
    public CitaDto atendida(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal p) { return citas.marcarAtendida(id, p); }

    @PreAuthorize("hasAnyRole('MEDICO','RECEPCIONISTA','ADMIN')")
    @PutMapping("/{id}/triaje")
    public TriajeDto triaje(@PathVariable Long id, @Valid @RequestBody TriajeRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return citas.registrarTriaje(id, r, p);
    }

    @PreAuthorize("hasAnyRole('MEDICO','RECEPCIONISTA','ADMIN')")
    @GetMapping("/{id}/triaje")
    public TriajeDto verTriaje(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal p) { return citas.triaje(id, p); }

    // RF-10 / RF-14
    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN')")
    @GetMapping("/paciente/{pacienteId}")
    public PageDto<CitaDto> historial(@PathVariable Long pacienteId, @RequestParam(defaultValue = "0") int pagina,
                                      @RequestParam(defaultValue = "10") int tamanio, @AuthenticationPrincipal UsuarioPrincipal p) {
        return citas.historial(pacienteId, pagina, Math.min(tamanio, 50), p);
    }

    // RF-25 / RF-27: médico solo ve su propia agenda; recepción elige el médico
    @PreAuthorize("hasAnyRole('MEDICO','RECEPCIONISTA','ADMIN')")
    @GetMapping("/agenda")
    public List<CitaDto> agenda(@RequestParam(required = false) Long medicoId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                @AuthenticationPrincipal UsuarioPrincipal p) {
        Long id = "ROLE_MEDICO".equals(p.getRol()) ? p.getId() : medicoId;
        if (id == null) throw ApiException.invalida("Seleccione el médico");
        if (desde != null && hasta != null) return citas.agendaRango(id, desde, hasta);
        return citas.agenda(id, fecha == null ? LocalDate.now() : fecha);
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping("/dia")
    public List<CitaDto> delDia(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return citas.delDia(fecha == null ? LocalDate.now() : fecha);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public CitaDto obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal p) { return citas.obtener(id, p); }
}
