package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.EstadoReceta;
import pe.gob.valdizan.domain.Enums.TipoMovimiento;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.*;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Farmacia: catálogo, inventario con movimientos, alertas, dispensación de recetas con cobro y boleta,
 * y anulación de ventas (repone stock). Todo transaccional; el stock se descuenta con bloqueo pesimista.
 */
@Service
@RequiredArgsConstructor
public class FarmaciaService {
    private static final int DIAS_ALERTA_VENCIMIENTO = 60;

    private final MedicamentoRepository medicamentos;
    private final MovimientoRepository movimientos;
    private final RecetaRepository recetas;
    private final DispensacionRepository dispensaciones;
    private final AdministrativoRepository administrativos;
    private final UsuarioRepository usuarios;
    private final PagoRepository pagos;
    private final PagoService pagoService;

    private LocalDate limite() { return LocalDate.now().plusDays(DIAS_ALERTA_VENCIMIENTO); }

    private MedicamentoDto dto(Medicamento m) { return Mapper.medicamento(m, limite()); }

    @Transactional(readOnly = true)
    public PageDto<MedicamentoDto> listar(String q, int pagina, int tamanio) {
        return PageDto.de(medicamentos.buscar(q == null ? "" : q.trim(), PageRequest.of(pagina, tamanio)), this::dto);
    }

    /** Catálogo para el médico: solo medicamentos activos. */
    @Transactional(readOnly = true)
    public List<MedicamentoDto> catalogoActivo() {
        return medicamentos.findByActivoTrueOrderByNombre().stream().map(this::dto).toList();
    }

    @Transactional
    public MedicamentoDto crear(MedicamentoRequest r, UsuarioPrincipal p) {
        Medicamento m = new Medicamento();
        aplicar(m, r);
        medicamentos.save(m);
        if (m.getStockActual() > 0) registrarMovimiento(m, TipoMovimiento.ENTRADA, m.getStockActual(), "Stock inicial", p.getId());
        return dto(m);
    }

    @Transactional
    public MedicamentoDto actualizar(Long id, MedicamentoRequest r, UsuarioPrincipal p) {
        Medicamento m = medicamentos.bloquear(id).orElseThrow(() -> ApiException.noEncontrado("El medicamento no existe"));
        int anterior = m.getStockActual();
        aplicar(m, r);
        int diff = m.getStockActual() - anterior;
        if (diff != 0) registrarMovimiento(m, TipoMovimiento.AJUSTE, diff, "Ajuste desde la edición del medicamento", p.getId());
        return dto(m);
    }

    private void aplicar(Medicamento m, MedicamentoRequest r) {
        m.setNombre(r.nombre().trim());
        m.setPrincipioActivo(r.principioActivo());
        m.setPresentacion(r.presentacion());
        m.setStockActual(r.stockActual());
        m.setStockMinimo(r.stockMinimo());
        m.setFechaVencimiento(r.fechaVencimiento());
        m.setPrecio(r.precio());
        if (r.activo() != null) m.setActivo(r.activo());
    }

    @Transactional
    public MedicamentoDto cambiarActivo(Long id, boolean activo) {
        Medicamento m = medicamentos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El medicamento no existe"));
        m.setActivo(activo);
        return dto(m);
    }

    @Transactional
    public MedicamentoDto movimiento(Long id, MovimientoRequest r, UsuarioPrincipal p) {
        Medicamento m = medicamentos.bloquear(id).orElseThrow(() -> ApiException.noEncontrado("El medicamento no existe"));
        int cant = r.cantidad();
        switch (r.tipo()) {
            case ENTRADA -> {
                if (cant <= 0) throw ApiException.invalida("La cantidad de entrada debe ser mayor a cero");
                m.setStockActual(m.getStockActual() + cant);
            }
            case SALIDA -> {
                if (cant <= 0) throw ApiException.invalida("La cantidad de salida debe ser mayor a cero");
                if (m.getStockActual() < cant) throw ApiException.conflicto("Stock insuficiente de " + m.getNombre());
                m.setStockActual(m.getStockActual() - cant);
            }
            case AJUSTE -> {
                if (cant == 0) throw ApiException.invalida("El ajuste no puede ser cero");
                if (m.getStockActual() + cant < 0) throw ApiException.conflicto("El ajuste dejaría el stock en negativo");
                m.setStockActual(m.getStockActual() + cant);
            }
        }
        registrarMovimiento(m, r.tipo(), cant, r.motivo(), p.getId());
        return dto(m);
    }

    private void registrarMovimiento(Medicamento m, TipoMovimiento tipo, int cant, String motivo, Long usuarioId) {
        MovimientoInventario mv = new MovimientoInventario();
        mv.setMedicamento(m);
        mv.setTipo(tipo);
        mv.setCantidad(cant);
        mv.setMotivo(motivo);
        mv.setFecha(LocalDateTime.now());
        mv.setUsuario(usuarios.getReferenceById(usuarioId));
        movimientos.save(mv);
    }

    @Transactional(readOnly = true)
    public PageDto<MovimientoDto> historialMovimientos(int pagina, int tamanio) {
        return PageDto.de(movimientos.findAllByOrderByFechaDesc(PageRequest.of(pagina, tamanio)), Mapper::movimiento);
    }

    @Transactional(readOnly = true)
    public AlertasDto alertas() {
        return new AlertasDto(medicamentos.stockBajo().stream().map(this::dto).toList(),
                medicamentos.porVencer(limite()).stream().map(this::dto).toList());
    }

    // ------------------------------------------------------------------ recetas y dispensación
    @Transactional(readOnly = true)
    public PageDto<RecetaDto> recetasPendientes(int pagina, int tamanio) {
        return PageDto.de(recetas.findByEstadoInOrderByFechaAsc(List.of(EstadoReceta.PENDIENTE, EstadoReceta.PARCIAL), PageRequest.of(pagina, tamanio)), Mapper::receta);
    }

    /**
     * Dispensa total (items vacío) o parcial. Rechaza si no hay stock o está vencido; descuenta stock,
     * registra el movimiento, cobra (si el total es mayor a cero) y actualiza la receta. Si el pago es
     * rechazado por la pasarela, toda la operación se revierte.
     */
    @Transactional
    public DispensacionDto dispensar(Long recetaId, DispensarRequest r, UsuarioPrincipal p) {
        RecetaMedica receta = recetas.findById(recetaId).orElseThrow(() -> ApiException.noEncontrado("La receta no existe"));
        if (receta.getEstado() == EstadoReceta.DISPENSADA) throw ApiException.conflicto("La receta ya fue dispensada por completo");
        Administrativo farmaceutico = administrativos.findById(p.getId())
                .orElseThrow(() -> ApiException.prohibido("Solo el personal de farmacia puede dispensar"));

        Map<Long, Integer> pedido = new LinkedHashMap<>();
        if (r == null || r.items() == null || r.items().isEmpty()) receta.getDetalles().forEach(d -> pedido.put(d.getIdDetalle(), d.pendiente()));
        else r.items().forEach(i -> pedido.merge(i.idDetalle(), i.cantidad(), Integer::sum));

        Dispensacion disp = new Dispensacion();
        disp.setReceta(receta);
        disp.setFarmaceutico(farmaceutico);
        disp.setFecha(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        for (var e : pedido.entrySet()) {
            DetalleReceta det = receta.getDetalles().stream().filter(d -> d.getIdDetalle().equals(e.getKey())).findFirst()
                    .orElseThrow(() -> ApiException.invalida("El detalle indicado no pertenece a la receta"));
            int cant = e.getValue();
            if (cant <= 0) continue;
            if (cant > det.pendiente()) throw ApiException.invalida("La cantidad supera lo pendiente de " + det.getMedicamento().getNombre());
            Medicamento m = medicamentos.bloquear(det.getMedicamento().getIdMedicamento()).orElseThrow();
            if (!m.isActivo()) throw ApiException.conflicto(m.getNombre() + " no está activo en el catálogo");
            if (m.getFechaVencimiento() != null && m.getFechaVencimiento().isBefore(LocalDate.now()))
                throw ApiException.conflicto(m.getNombre() + " está vencido y no puede dispensarse");
            if (m.getStockActual() < cant)
                throw ApiException.conflicto("Stock insuficiente de " + m.getNombre() + " (disponible: " + m.getStockActual() + ")");
            m.setStockActual(m.getStockActual() - cant);
            det.setCantidadDispensada(det.getCantidadDispensada() + cant);
            registrarMovimiento(m, TipoMovimiento.SALIDA, cant, "Dispensación de receta #" + recetaId, p.getId());
            BigDecimal pu = m.getPrecio() == null ? BigDecimal.ZERO : m.getPrecio();
            DetalleDispensacion dd = new DetalleDispensacion();
            dd.setMedicamento(m);
            dd.setCantidad(cant);
            dd.setPrecioUnitario(pu);
            disp.agregar(dd);
            total = total.add(pu.multiply(BigDecimal.valueOf(cant)));
        }
        if (disp.getDetalles().isEmpty()) throw ApiException.invalida("No hay cantidades para dispensar");
        disp.setTotal(total);
        dispensaciones.saveAndFlush(disp);

        boolean completa = receta.getDetalles().stream().allMatch(d -> d.pendiente() == 0);
        receta.setEstado(completa ? EstadoReceta.DISPENSADA : EstadoReceta.PARCIAL);

        Pago pago = null;
        if (total.signum() > 0) {
            if (r == null || r.pago() == null) throw ApiException.invalida("Seleccione el método de pago para cobrar los medicamentos");
            pago = pagoService.cobrarDispensacion(disp, receta.getHistorial().getPaciente(), total, r.pago(), p);
        }
        return Mapper.dispensacion(disp, pago);
    }

    /** Anula una venta: repone el stock, revierte la receta y anula el pago. */
    @Transactional
    public DispensacionDto anularDispensacion(Long id, String motivo, UsuarioPrincipal p) {
        Dispensacion d = dispensaciones.findById(id).orElseThrow(() -> ApiException.noEncontrado("La dispensación no existe"));
        if (d.isAnulada()) throw ApiException.conflicto("La dispensación ya fue anulada");
        RecetaMedica receta = d.getReceta();
        for (DetalleDispensacion dd : d.getDetalles()) {
            Medicamento m = medicamentos.bloquear(dd.getMedicamento().getIdMedicamento()).orElseThrow();
            m.setStockActual(m.getStockActual() + dd.getCantidad());
            registrarMovimiento(m, TipoMovimiento.ENTRADA, dd.getCantidad(), "Anulación de dispensación #" + id + ": " + motivo, p.getId());
            receta.getDetalles().stream()
                    .filter(x -> x.getMedicamento().getIdMedicamento().equals(m.getIdMedicamento()) && x.getCantidadDispensada() >= dd.getCantidad())
                    .findFirst().ifPresent(x -> x.setCantidadDispensada(x.getCantidadDispensada() - dd.getCantidad()));
        }
        boolean nada = receta.getDetalles().stream().allMatch(x -> x.getCantidadDispensada() == 0);
        boolean todo = receta.getDetalles().stream().allMatch(x -> x.pendiente() == 0);
        receta.setEstado(nada ? EstadoReceta.PENDIENTE : todo ? EstadoReceta.DISPENSADA : EstadoReceta.PARCIAL);
        d.setAnulada(true);
        d.setFechaAnulacion(LocalDateTime.now());
        d.setMotivoAnulacion(motivo);
        pagoService.anularPagoDispensacion(id, "Anulación de venta: " + motivo, p.getId());
        return Mapper.dispensacion(d, pagos.findFirstByDispensacion_IdDispensacionOrderByFechaDesc(id).orElse(null));
    }

    @Transactional(readOnly = true)
    public DispensacionDto obtenerDispensacion(Long id) {
        Dispensacion d = dispensaciones.findById(id).orElseThrow(() -> ApiException.noEncontrado("La dispensación no existe"));
        return Mapper.dispensacion(d, pagos.findFirstByDispensacion_IdDispensacionOrderByFechaDesc(id).orElse(null));
    }

    @Transactional(readOnly = true)
    public PageDto<DispensacionDto> listarDispensaciones(LocalDate desde, LocalDate hasta, int pagina, int tamanio) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) throw ApiException.invalida("Indique un rango de fechas válido");
        return PageDto.de(dispensaciones.findByFechaBetweenOrderByFechaDesc(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(),
                PageRequest.of(pagina, tamanio)),
                d -> Mapper.dispensacion(d, pagos.findFirstByDispensacion_IdDispensacionOrderByFechaDesc(d.getIdDispensacion()).orElse(null)));
    }
}
