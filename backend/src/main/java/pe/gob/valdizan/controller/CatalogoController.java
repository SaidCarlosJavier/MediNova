package pe.gob.valdizan.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.gob.valdizan.domain.Enums.Turno;
import pe.gob.valdizan.dto.Dto.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.CatalogoService;
import pe.gob.valdizan.service.HorarioService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Catálogo y horarios")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogoController {
    private final CatalogoService catalogo;
    private final HorarioService horarios;

    // RF-05
    @GetMapping("/especialidades")
    public List<EspecialidadDto> especialidades() { return catalogo.especialidadesActivas(); }

    @GetMapping("/consultorios")
    public List<ConsultorioDto> consultorios() { return catalogo.consultoriosActivos(); }

    @GetMapping("/medicos")
    public List<MedicoDto> medicos(@RequestParam(required = false) Long especialidadId) { return catalogo.medicos(especialidadId); }

    @GetMapping("/turnos")
    public List<ConfigTurnoDto> turnos() { return catalogo.turnos(); }

    // RF-06
    @GetMapping("/horarios/disponibles")
    public List<HorarioDto> disponibles(@RequestParam Long especialidadId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                        @RequestParam(required = false) Turno turno) {
        return horarios.disponibles(especialidadId, fecha, turno);
    }

    @GetMapping("/horarios/fechas")
    public List<LocalDate> fechas(@RequestParam Long especialidadId) { return horarios.fechasDisponibles(especialidadId); }

    // RF-19 / RF-34
    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN','MEDICO')")
    @GetMapping("/horarios/gestion")
    public List<HorarioDto> gestion(@RequestParam(required = false) Long medicoId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                    @AuthenticationPrincipal UsuarioPrincipal p) {
        Long id = "ROLE_MEDICO".equals(p.getRol()) ? p.getId() : medicoId;
        if (id == null) throw ApiException.invalida("Seleccione el médico");
        return horarios.gestion(id, fecha);
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN','MEDICO')")
    @PostMapping("/horarios/generar")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> generar(@Valid @RequestBody GenerarHorariosRequest r, @AuthenticationPrincipal UsuarioPrincipal p) {
        return Map.of("creados", horarios.generar(r, p));
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN','MEDICO')")
    @PatchMapping("/horarios/{id}/disponibilidad")
    public HorarioDto disponibilidad(@PathVariable Long id, @RequestParam boolean disponible, @AuthenticationPrincipal UsuarioPrincipal p) {
        return horarios.cambiarDisponibilidad(id, disponible, p);
    }

    @PreAuthorize("hasAnyRole('RECEPCIONISTA','ADMIN','MEDICO')")
    @DeleteMapping("/horarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal p) { horarios.eliminar(id, p); }

    // RF-34 configuración (admin)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/especialidades")
    public List<EspecialidadDto> todasEspecialidades() { return catalogo.todasEspecialidades(); }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/especialidades")
    @ResponseStatus(HttpStatus.CREATED)
    public EspecialidadDto crearEspecialidad(@Valid @RequestBody EspecialidadRequest r) { return catalogo.guardarEspecialidad(null, r); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/especialidades/{id}")
    public EspecialidadDto editarEspecialidad(@PathVariable Long id, @Valid @RequestBody EspecialidadRequest r) { return catalogo.guardarEspecialidad(id, r); }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/consultorios")
    public List<ConsultorioDto> todosConsultorios() { return catalogo.todosConsultorios(); }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/consultorios")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultorioDto crearConsultorio(@Valid @RequestBody ConsultorioRequest r) { return catalogo.guardarConsultorio(null, r); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/consultorios/{id}")
    public ConsultorioDto editarConsultorio(@PathVariable Long id, @Valid @RequestBody ConsultorioRequest r) { return catalogo.guardarConsultorio(id, r); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/turnos")
    public ConfigTurnoDto guardarTurno(@Valid @RequestBody ConfigTurnoDto r) { return catalogo.guardarTurno(r); }
}
