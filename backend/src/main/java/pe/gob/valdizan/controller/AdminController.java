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
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.AdminService;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Administración y reportes")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService admin;

    @GetMapping("/usuarios")
    public PageDto<UsuarioDto> usuarios(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int pagina,
                                        @RequestParam(defaultValue = "10") int tamanio) {
        return admin.listarUsuarios(q, pagina, Math.min(tamanio, 50));
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody UsuarioAdminRequest r) { return admin.crearUsuario(r); }

    @PatchMapping("/usuarios/{id}/estado")
    public UsuarioDto estado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return admin.cambiarEstado(id, r.estado(), p);
    }

    @PatchMapping("/usuarios/{id}/cargo")
    public UsuarioDto cargo(@PathVariable Long id, @Valid @RequestBody CambiarCargoRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return admin.cambiarCargo(id, r.cargo(), p);
    }

    // RF-32
    @GetMapping("/reportes/citas")
    public ReporteCitasDto citas(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return admin.reporteCitas(desde, hasta);
    }

    // RF-33
    @GetMapping("/reportes/inasistencia")
    public List<InasistenciaFila> inasistencia(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return admin.reporteInasistencia(desde, hasta);
    }

    // RNF-23
    @GetMapping("/indicadores")
    public IndicadoresDto indicadores() { return admin.indicadores(); }

    // RNF-14
    @GetMapping("/auditoria")
    public PageDto<AuditoriaDto> auditoria(@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanio) {
        return admin.auditoria(pagina, Math.min(tamanio, 100));
    }
}
