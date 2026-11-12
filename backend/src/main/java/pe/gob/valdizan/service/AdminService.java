package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.*;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.*;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.time.LocalDate;
import java.util.*;

/** RF-31 cuentas y roles; RF-32/33 reportes; RNF-23 indicadores; RNF-14 auditoría. */
@Service
@RequiredArgsConstructor
public class AdminService {
    private final UsuarioRepository usuarios;
    private final EspecialidadRepository especialidades;
    private final CitaRepository citas;
    private final AuditoriaRepository auditoria;
    private final UsuarioSupport soporte;

    @Transactional(readOnly = true)
    public PageDto<UsuarioDto> listarUsuarios(String q, int pagina, int tamanio) {
        return PageDto.de(usuarios.buscar(q == null ? "" : q.trim(), PageRequest.of(pagina, tamanio)), Mapper::usuario);
    }

    @Transactional
    public UsuarioDto crearUsuario(UsuarioAdminRequest r) {
        String correo = r.correo().trim().toLowerCase();
        soporte.validarUnicos(r.documentoIdentidad().trim(), correo);
        Usuario u;
        if ("MEDICO".equals(r.tipo())) {
            if (UsuarioSupport.vacio(r.numeroColegiatura())) throw ApiException.invalida("Ingrese el número de colegiatura");
            if (r.especialidadId() == null) throw ApiException.invalida("Seleccione la especialidad del médico");
            Medico m = soporte.base(new Medico(), r.documentoIdentidad(), r.nombres(), r.apellidos(), correo, r.password(), NombreRol.ROLE_MEDICO, true);
            m.setNumeroColegiatura(r.numeroColegiatura().trim());
            m.setEspecialidad(especialidades.findById(r.especialidadId()).orElseThrow(() -> ApiException.noEncontrado("La especialidad no existe")));
            u = m;
        } else {
            Cargo cargo = switch (r.tipo()) {
                case "RECEPCIONISTA" -> Cargo.RECEPCIONISTA;
                case "FARMACEUTICO" -> Cargo.FARMACEUTICO;
                default -> Cargo.ADMINISTRADOR;
            };
            Administrativo a = soporte.base(new Administrativo(), r.documentoIdentidad(), r.nombres(), r.apellidos(), correo, r.password(), cargo.rol(), true);
            a.setCargo(cargo);
            u = a;
        }
        return Mapper.usuario(usuarios.save(u));
    }

    @Transactional
    public UsuarioDto cambiarEstado(Long id, boolean estado, UsuarioPrincipal admin) {
        if (admin.getId().equals(id) && !estado) throw ApiException.invalida("No puede desactivar su propia cuenta");
        Usuario u = usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("El usuario no existe"));
        u.setEstado(estado);
        return Mapper.usuario(u);
    }

    /** Cambia el cargo (y por lo tanto el rol) de personal administrativo. */
    @Transactional
    public UsuarioDto cambiarCargo(Long id, Cargo cargo, UsuarioPrincipal admin) {
        Usuario u = (Usuario) Hibernate.unproxy(usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("El usuario no existe")));
        if (!(u instanceof Administrativo a)) throw ApiException.invalida("Solo se puede cambiar el cargo del personal administrativo");
        if (admin.getId().equals(id) && cargo != Cargo.ADMINISTRADOR) throw ApiException.invalida("No puede quitarse a sí mismo el rol de administrador");
        a.setCargo(cargo);
        a.setRol(soporte.rol(cargo.rol()));
        return Mapper.usuario(a);
    }

    // ------------------------------------------------------------------ reportes
    @Transactional(readOnly = true)
    public ReporteCitasDto reporteCitas(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        Map<String, Long> por = new LinkedHashMap<>();
        Arrays.stream(EstadoCita.values()).forEach(e -> por.put(e.name(), 0L));
        citas.contarPorEstado(desde, hasta).forEach(o -> por.put(((EstadoCita) o[0]).name(), (Long) o[1]));
        long total = por.values().stream().mapToLong(Long::longValue).sum();
        return new ReporteCitasDto(desde, hasta, total, por, por.get("ATENDIDA"), por.get("CANCELADA"), por.get("REPROGRAMADA"));
    }

    @Transactional(readOnly = true)
    public List<InasistenciaFila> reporteInasistencia(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        Map<String, long[]> acum = new TreeMap<>();
        for (Object[] o : citas.contarPorEspecialidadYEstado(desde, hasta)) {
            EstadoCita e = (EstadoCita) o[1];
            if (e == EstadoCita.CANCELADA) continue; // las canceladas no cuentan como programadas
            long[] v = acum.computeIfAbsent((String) o[0], k -> new long[2]);
            v[0] += (Long) o[2];
            if (e == EstadoCita.NO_ASISTIO) v[1] += (Long) o[2];
        }
        return acum.entrySet().stream().map(en -> new InasistenciaFila(en.getKey(), en.getValue()[0], en.getValue()[1],
                en.getValue()[0] == 0 ? 0 : Math.round(en.getValue()[1] * 10000.0 / en.getValue()[0]) / 100.0)).toList();
    }

    /** RNF-23: indicadores globales para comparar pre/post implementación. */
    @Transactional(readOnly = true)
    public IndicadoresDto indicadores() {
        Map<EstadoCita, Long> m = new EnumMap<>(EstadoCita.class);
        citas.contarTodasPorEstado().forEach(o -> m.put((EstadoCita) o[0], (Long) o[1]));
        long total = m.values().stream().mapToLong(Long::longValue).sum();
        long noAsistio = m.getOrDefault(EstadoCita.NO_ASISTIO, 0L);
        long base = total - m.getOrDefault(EstadoCita.CANCELADA, 0L);
        double pct = base == 0 ? 0 : Math.round(noAsistio * 10000.0 / base) / 100.0;
        Double prom = citas.promedioDuracionReserva();
        return new IndicadoresDto(total, m.getOrDefault(EstadoCita.ATENDIDA, 0L), m.getOrDefault(EstadoCita.CANCELADA, 0L),
                m.getOrDefault(EstadoCita.REPROGRAMADA, 0L), noAsistio, pct, prom == null ? null : Math.round(prom * 10) / 10.0);
    }

    @Transactional(readOnly = true)
    public PageDto<AuditoriaDto> auditoria(int pagina, int tamanio) {
        return PageDto.de(auditoria.findAllByOrderByFechaHoraDesc(PageRequest.of(pagina, tamanio)),
                a -> new AuditoriaDto(a.getIdLog(), a.getAccion(), a.getFechaHora(), a.getEntidad(), a.getDetalle(), a.getUsuario().getNombreCompleto()));
    }

    private void validarRango(LocalDate d, LocalDate h) {
        if (d == null || h == null) throw ApiException.invalida("Indique el rango de fechas");
        if (h.isBefore(d)) throw ApiException.invalida("La fecha final debe ser igual o posterior a la inicial");
    }
}
