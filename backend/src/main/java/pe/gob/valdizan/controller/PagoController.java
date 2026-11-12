package pe.gob.valdizan.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.gob.valdizan.domain.Enums.MetodoPago;
import pe.gob.valdizan.domain.Enums.TipoPago;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.PagoService;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Pagos (pasarela simulada)")
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {
    private final PagoService pagos;

    /** Paga la consulta de una cita: paciente/tutor (tarjeta, Yape, Plin) o ventanilla (también efectivo). */
    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN')")
    @PostMapping("/cita/{citaId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoDto pagarCita(@PathVariable Long citaId, @Valid @RequestBody PagoRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return pagos.pagarCita(citaId, r, p);
    }

    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN')")
    @GetMapping("/paciente/{pacienteId}")
    public List<PagoDto> delPaciente(@PathVariable Long pacienteId, @AuthenticationPrincipal UsuarioPrincipal p) {
        return pagos.delPaciente(pacienteId, p);
    }

    @PreAuthorize("hasAnyRole('PACIENTE','TUTOR','RECEPCIONISTA','ADMIN','FARMACEUTICO')")
    @GetMapping("/{id}")
    public PagoDto obtener(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal p) { return pagos.obtener(id, p); }

    /** Caja (recepción) y reporte de ingresos (administración). */
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping
    public PageDto<PagoDto> listar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   @RequestParam(required = false) TipoPago tipo, @RequestParam(required = false) MetodoPago metodo,
                                   @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamanio) {
        return pagos.listar(desde, hasta, tipo, metodo, pagina, Math.min(tamanio, 100));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @GetMapping("/resumen")
    public ResumenIngresosDto resumen(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return pagos.resumen(desde, hasta);
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN')")
    @PatchMapping("/{id}/anular")
    public PagoDto anular(@PathVariable Long id, @Valid @RequestBody AnularRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return pagos.anular(id, r.motivo(), p);
    }
}
