package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.*;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.CitaRepository;
import pe.gob.valdizan.repository.Repositorios.PagoRepository;
import pe.gob.valdizan.repository.Repositorios.UsuarioRepository;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Cobros de consulta y de farmacia, comprobantes internos, anulaciones/reembolsos e ingresos. */
@Service
@RequiredArgsConstructor
public class PagoService {
    private final PagoRepository pagos;
    private final CitaRepository citas;
    private final UsuarioRepository usuarios;
    private final PasarelaService pasarela;
    private final AccesoService acceso;
    private final NotificacionService notificaciones;

    private static final String SERIE_CONSULTA = "B001";
    private static final String SERIE_FARMACIA = "B002";

    // ------------------------------------------------------------------ cobro de consulta
    @Transactional
    public PagoDto pagarCita(Long citaId, PagoRequest r, UsuarioPrincipal p) {
        Cita c = citas.findById(citaId).orElseThrow(() -> ApiException.noEncontrado("La cita no existe"));
        Paciente pac = acceso.paciente(c.getPaciente().getIdUsuario(), p);
        if (c.getEstado() == EstadoCita.CANCELADA || c.getEstado() == EstadoCita.NO_ASISTIO)
            throw ApiException.invalida("No se puede pagar una cita cancelada o con inasistencia");
        if (c.isPagada()) throw ApiException.conflicto("La consulta ya fue pagada");
        BigDecimal monto = c.getEspecialidad().getPrecioConsulta();
        if (monto == null || monto.signum() <= 0) throw ApiException.invalida("Esta consulta no tiene costo");
        if (r.metodo() == MetodoPago.EFECTIVO && !p.esPersonalAdministrativo())
            throw ApiException.invalida("El pago en efectivo solo se registra en ventanilla. Use tarjeta, Yape o Plin.");

        String concepto = "Consulta de " + c.getEspecialidad().getNombreEspecialidad() + " - " + c.getHorario().getFecha();
        Pago pago = registrar(TipoPago.CONSULTA, monto, r, pac, p, concepto);
        pago.setCita(c);
        c.setPagada(true);
        pagos.save(pago);
        notificar(pac, pago);
        return Mapper.pago(pago);
    }

    // ------------------------------------------------------------------ cobro de farmacia (lo invoca FarmaciaService en su transacción)
    @Transactional
    public Pago cobrarDispensacion(Dispensacion d, Paciente pac, BigDecimal total, PagoRequest r, UsuarioPrincipal p) {
        Pago pago = registrar(TipoPago.FARMACIA, total, r, pac, p, "Medicamentos - receta #" + d.getReceta().getIdReceta());
        pago.setDispensacion(d);
        pagos.save(pago);
        notificar(pac, pago);
        return pago;
    }

    private Pago registrar(TipoPago tipo, BigDecimal monto, PagoRequest r, Paciente pac, UsuarioPrincipal p, String concepto) {
        PasarelaService.Resultado res = pasarela.procesar(r, monto);
        Pago pago = new Pago();
        pago.setTipo(tipo);
        pago.setMetodo(r.metodo());
        pago.setEstado(EstadoPago.PAGADO);
        pago.setMonto(monto);
        pago.setMontoRecibido(res.montoRecibido());
        pago.setVuelto(res.vuelto());
        pago.setReferencia(res.referencia());
        pago.setTarjetaUltimos4(res.tarjetaUltimos4());
        pago.setConcepto(concepto);
        pago.setFecha(LocalDateTime.now());
        pago.setPaciente(pac);
        pago.setPagador(usuarios.getReferenceById(p.getId()));
        pagos.saveAndFlush(pago);
        // Correlativo basado en el id: único y sin carreras entre transacciones
        pago.setComprobante((tipo == TipoPago.CONSULTA ? SERIE_CONSULTA : SERIE_FARMACIA) + "-" + String.format("%08d", pago.getIdPago()));
        return pago;
    }

    private void notificar(Paciente pac, Pago pago) {
        notificaciones.notificarPaciente(pac, TipoNotificacion.PAGO,
                "Pago registrado: S/ " + pago.getMonto().setScale(2, java.math.RoundingMode.HALF_UP) +" (" + pago.getConcepto() + "). Comprobante " + pago.getComprobante() + ".");
    }

    // ------------------------------------------------------------------ anulación / reembolso
    /** Anula el pago de una consulta (admin o recepción). Los pagos de farmacia se anulan desde la dispensación. */
    @Transactional
    public PagoDto anular(Long id, String motivo, UsuarioPrincipal p) {
        Pago pago = pagos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El pago no existe"));
        if (pago.getTipo() == TipoPago.FARMACIA)
            throw ApiException.invalida("Los pagos de farmacia se anulan desde Farmacia → Ventas (se repone el stock)");
        marcarAnulado(pago, motivo, p.getId());
        if (pago.getCita() != null) pago.getCita().setPagada(false);
        return Mapper.pago(pago);
    }

    /** Reembolso automático al cancelar una cita pagada. */
    @Transactional
    public void reembolsarPorCancelacion(Cita c, Long usuarioId) {
        pagos.findFirstByCita_IdCitaAndEstadoOrderByFechaDesc(c.getIdCita(), EstadoPago.PAGADO)
                .ifPresent(pg -> marcarAnulado(pg, "Cancelación de la cita (reembolso)", usuarioId));
        c.setPagada(false);
    }

    @Transactional
    public void anularPagoDispensacion(Long dispensacionId, String motivo, Long usuarioId) {
        pagos.findFirstByDispensacion_IdDispensacionOrderByFechaDesc(dispensacionId)
                .filter(pg -> pg.getEstado() == EstadoPago.PAGADO)
                .ifPresent(pg -> marcarAnulado(pg, motivo, usuarioId));
    }

    private void marcarAnulado(Pago pago, String motivo, Long usuarioId) {
        if (pago.getEstado() == EstadoPago.ANULADO) throw ApiException.conflicto("El pago ya fue anulado");
        pago.setEstado(EstadoPago.ANULADO);
        pago.setMotivoAnulacion(motivo);
        pago.setFechaAnulacion(LocalDateTime.now());
        pago.setAnuladoPor(usuarios.getReferenceById(usuarioId));
    }

    // ------------------------------------------------------------------ consultas
    @Transactional(readOnly = true)
    public PagoDto obtener(Long id, UsuarioPrincipal p) {
        Pago pago = pagos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El pago no existe"));
        if (!p.esPersonalAdministrativo() && !"ROLE_FARMACEUTICO".equals(p.getRol())) acceso.paciente(pago.getPaciente().getIdUsuario(), p);
        return Mapper.pago(pago);
    }

    @Transactional(readOnly = true)
    public List<PagoDto> delPaciente(Long pacienteId, UsuarioPrincipal p) {
        acceso.paciente(pacienteId, p);
        return pagos.findByPaciente_IdUsuarioOrderByFechaDesc(pacienteId).stream().map(Mapper::pago).toList();
    }

    @Transactional(readOnly = true)
    public PageDto<PagoDto> listar(LocalDate desde, LocalDate hasta, TipoPago tipo, MetodoPago metodo, int pagina, int tamanio) {
        rango(desde, hasta);
        List<Pago> todos = pagos.findByFechaBetweenOrderByFechaDesc(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()).stream()
                .filter(x -> tipo == null || x.getTipo() == tipo).filter(x -> metodo == null || x.getMetodo() == metodo).toList();
        int ini = Math.min(pagina * tamanio, todos.size());
        List<PagoDto> pag = todos.subList(ini, Math.min(ini + tamanio, todos.size())).stream().map(Mapper::pago).toList();
        var page = new PageImpl<>(pag, PageRequest.of(pagina, tamanio), todos.size());
        return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ResumenIngresosDto resumen(LocalDate desde, LocalDate hasta) {
        rango(desde, hasta);
        List<Pago> todos = pagos.findByFechaBetweenOrderByFechaDesc(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        List<Pago> validos = todos.stream().filter(x -> x.getEstado() == EstadoPago.PAGADO).toList();
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        Arrays.stream(MetodoPago.values()).forEach(m -> porMetodo.put(m.name(), BigDecimal.ZERO));
        Map<String, BigDecimal> porTipo = new LinkedHashMap<>();
        Arrays.stream(TipoPago.values()).forEach(t -> porTipo.put(t.name(), BigDecimal.ZERO));
        validos.forEach(x -> {
            porMetodo.merge(x.getMetodo().name(), x.getMonto(), BigDecimal::add);
            porTipo.merge(x.getTipo().name(), x.getMonto(), BigDecimal::add);
        });
        Map<LocalDate, BigDecimal> dias = validos.stream().collect(Collectors.groupingBy(x -> x.getFecha().toLocalDate(), TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, Pago::getMonto, BigDecimal::add)));
        BigDecimal total = validos.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenIngresosDto(desde, hasta, total, validos.size(), todos.size() - validos.size(), porMetodo, porTipo,
                dias.entrySet().stream().map(e -> new DiaIngreso(e.getKey(), e.getValue())).toList());
    }

    private void rango(LocalDate d, LocalDate h) {
        if (d == null || h == null) throw ApiException.invalida("Indique el rango de fechas");
        if (h.isBefore(d)) throw ApiException.invalida("La fecha final debe ser igual o posterior a la inicial");
    }
}
