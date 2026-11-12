package pe.gob.valdizan.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.valdizan.domain.Enums.EstadoPago;
import pe.gob.valdizan.domain.Enums.MetodoPago;
import pe.gob.valdizan.domain.Enums.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pago de una consulta o de una dispensación de farmacia.
 * Nunca se guarda el número completo de tarjeta ni el CVV: solo los últimos 4 dígitos.
 */
@Entity
@Table(name = "pagos",
        indexes = {
                @Index(name = "idx_pago_fecha", columnList = "fecha"),
                @Index(name = "idx_pago_cita", columnList = "id_cita"),
                @Index(name = "idx_pago_paciente", columnList = "id_paciente")},
        uniqueConstraints = @UniqueConstraint(name = "uk_pago_comprobante", columnNames = "comprobante"))
@Getter @Setter @NoArgsConstructor
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPago;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    private TipoPago tipo;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    private EstadoPago estado = EstadoPago.PAGADO;

    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal monto;
    @Column(precision = 10, scale = 2) private BigDecimal montoRecibido;
    @Column(precision = 10, scale = 2) private BigDecimal vuelto;

    /** Código de operación devuelto por la pasarela. */
    @Column(length = 40) private String referencia;

    /** Ej. B001-00000012 (consulta) o B002-00000007 (farmacia). Interno, sin validez tributaria. */
    @Column(length = 20) private String comprobante;

    @Column(length = 4) private String tarjetaUltimos4;
    @Column(length = 200) private String concepto;
    @Column(nullable = false) private LocalDateTime fecha;

    @Column(length = 250) private String motivoAnulacion;
    private LocalDateTime fechaAnulacion;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_cita")
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_dispensacion")
    private Dispensacion dispensacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "id_paciente")
    private Paciente paciente;

    /** Quien realizó/registró el cobro (paciente, tutor, recepcionista o farmacéutico). */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_pagador")
    private Usuario pagador;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_anulado_por")
    private Usuario anuladoPor;
}
