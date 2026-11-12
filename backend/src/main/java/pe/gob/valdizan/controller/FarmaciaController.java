package pe.gob.valdizan.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.FarmaciaService;

import java.util.List;

@Tag(name = "Farmacia")
@RestController
@RequestMapping("/api/farmacia")
@RequiredArgsConstructor
public class FarmaciaController {
    private final FarmaciaService farmacia;

    /** El médico elige solo entre medicamentos activos. */
    @PreAuthorize("hasAnyRole('MEDICO','FARMACEUTICO','ADMIN')")
    @GetMapping("/catalogo")
    public List<MedicamentoDto> catalogo() { return farmacia.catalogoActivo(); }

    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    @GetMapping("/medicamentos")
    public PageDto<MedicamentoDto> listar(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "10") int tamanio) {
        return farmacia.listar(q, pagina, Math.min(tamanio, 50));
    }

    @PreAuthorize("hasRole('FARMACEUTICO')")
    @PostMapping("/medicamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentoDto crear(@Valid @RequestBody MedicamentoRequest r, @AuthenticationPrincipal UsuarioPrincipal p) { return farmacia.crear(r, p); }

    @PreAuthorize("hasRole('FARMACEUTICO')")
    @PutMapping("/medicamentos/{id}")
    public MedicamentoDto actualizar(@PathVariable Long id, @Valid @RequestBody MedicamentoRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return farmacia.actualizar(id, r, p);
    }

    @PreAuthorize("hasRole('FARMACEUTICO')")
    @PatchMapping("/medicamentos/{id}/activo")
    public MedicamentoDto activo(@PathVariable Long id, @RequestParam boolean activo) { return farmacia.cambiarActivo(id, activo); }

    @PreAuthorize("hasRole('FARMACEUTICO')")
    @PostMapping("/medicamentos/{id}/movimientos")
    public MedicamentoDto movimiento(@PathVariable Long id, @Valid @RequestBody MovimientoRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return farmacia.movimiento(id, r, p);
    }

    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    @GetMapping("/movimientos")
    public PageDto<MovimientoDto> movimientos(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "15") int tamanio) {
        return farmacia.historialMovimientos(pagina, Math.min(tamanio, 50));
    }

    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    @GetMapping("/alertas")
    public AlertasDto alertas() { return farmacia.alertas(); }

    @PreAuthorize("hasRole('FARMACEUTICO')")
    @GetMapping("/recetas/pendientes")
    public PageDto<RecetaDto> pendientes(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamanio) {
        return farmacia.recetasPendientes(pagina, Math.min(tamanio, 50));
    }

    @PreAuthorize("hasRole('FARMACEUTICO')")
    @PostMapping("/recetas/{id}/dispensar")
    public DispensacionDto dispensar(@PathVariable Long id, @Valid @RequestBody(required = false) DispensarRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return farmacia.dispensar(id, r, p);
    }

    /** Historial de ventas/dispensaciones (para reimprimir boletas). */
    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    @GetMapping("/dispensaciones")
    public PageDto<DispensacionDto> dispensaciones(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamanio) {
        java.time.LocalDate h = hasta == null ? java.time.LocalDate.now() : hasta;
        java.time.LocalDate d = desde == null ? h.minusDays(30) : desde;
        return farmacia.listarDispensaciones(d, h, pagina, Math.min(tamanio, 50));
    }

    @PreAuthorize("hasAnyRole('FARMACEUTICO','ADMIN')")
    @GetMapping("/dispensaciones/{id}")
    public DispensacionDto dispensacion(@PathVariable Long id) { return farmacia.obtenerDispensacion(id); }

    @PreAuthorize("hasRole('FARMACEUTICO')")
    @PatchMapping("/dispensaciones/{id}/anular")
    public DispensacionDto anular(@PathVariable Long id, @Valid @RequestBody AnularRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return farmacia.anularDispensacion(id, r.motivo(), p);
    }
}
