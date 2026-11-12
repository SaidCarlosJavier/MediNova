package pe.gob.valdizan.dto;

import org.hibernate.Hibernate;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.dto.Dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Mapeo manual entidad → DTO (se invoca siempre dentro de una transacción). */
public final class Mapper {
    private Mapper() {}

    public static UsuarioDto usuario(Usuario u0) {
        Usuario u = (Usuario) Hibernate.unproxy(u0);
        UsuarioDto.UsuarioDtoBuilder b = UsuarioDto.builder()
                .id(u.getIdUsuario()).documentoIdentidad(u.getDocumentoIdentidad()).nombres(u.getNombres())
                .apellidos(u.getApellidos()).nombreCompleto(u.getNombreCompleto()).correo(u.getCorreo())
                .rol(u.getRol().getNombreRol().name()).estado(u.isEstado());
        if (u instanceof Paciente p) {
            b.tipo("PACIENTE").tipoSeguro(p.getTipoSeguro()).telefonoContacto(p.getTelefonoContacto())
                    .fechaNacimiento(p.getFechaNacimiento()).requiereTutor(p.isRequiereTutor());
            if (p.getTutor() != null) b.tutorId(p.getTutor().getIdUsuario()).tutor(p.getTutor().getNombreCompleto());
        } else if (u instanceof Tutor t) {
            b.tipo("TUTOR").parentesco(t.getParentesco()).telefono(t.getTelefono()).direccion(t.getDireccion());
        } else if (u instanceof Medico m) {
            b.tipo("MEDICO").numeroColegiatura(m.getNumeroColegiatura())
                    .especialidadId(m.getEspecialidad().getIdEspecialidad()).especialidad(m.getEspecialidad().getNombreEspecialidad());
        } else if (u instanceof Administrativo a) {
            b.tipo("ADMINISTRATIVO").cargo(a.getCargo().name());
        }
        return b.build();
    }

    public static PacienteDto paciente(Paciente p) {
        return PacienteDto.builder().id(p.getIdUsuario()).documentoIdentidad(p.getDocumentoIdentidad()).nombres(p.getNombres())
                .apellidos(p.getApellidos()).nombreCompleto(p.getNombreCompleto()).correo(p.getCorreo())
                .tipoSeguro(p.getTipoSeguro()).telefonoContacto(p.getTelefonoContacto()).fechaNacimiento(p.getFechaNacimiento())
                .requiereTutor(p.isRequiereTutor())
                .tutorId(p.getTutor() != null ? p.getTutor().getIdUsuario() : null)
                .tutor(p.getTutor() != null ? p.getTutor().getNombreCompleto() : null).build();
    }

    public static EspecialidadDto especialidad(Especialidad e) {
        return new EspecialidadDto(e.getIdEspecialidad(), e.getNombreEspecialidad(), e.getDescripcion(), e.isActivo(),
                e.getPrecioConsulta() == null ? BigDecimal.ZERO : e.getPrecioConsulta());
    }

    public static PagoDto pago(Pago p) {
        Paciente pac = p.getPaciente();
        return PagoDto.builder().idPago(p.getIdPago()).tipo(p.getTipo()).metodo(p.getMetodo()).estado(p.getEstado()).monto(p.getMonto())
                .referencia(p.getReferencia()).comprobante(p.getComprobante()).fecha(p.getFecha()).concepto(p.getConcepto())
                .citaId(p.getCita() != null ? p.getCita().getIdCita() : null)
                .dispensacionId(p.getDispensacion() != null ? p.getDispensacion().getIdDispensacion() : null)
                .pacienteId(pac.getIdUsuario()).paciente(pac.getNombreCompleto()).pacienteDocumento(pac.getDocumentoIdentidad())
                .pagador(p.getPagador() != null ? p.getPagador().getNombreCompleto() : null).tarjetaUltimos4(p.getTarjetaUltimos4())
                .montoRecibido(p.getMontoRecibido()).vuelto(p.getVuelto()).motivoAnulacion(p.getMotivoAnulacion()).build();
    }

    public static DispensacionDto dispensacion(Dispensacion d, Pago pago) {
        RecetaMedica r = d.getReceta();
        List<LineaDispensacionDto> lineas = d.getDetalles().stream().map(x -> {
            BigDecimal pu = x.getPrecioUnitario() == null ? BigDecimal.ZERO : x.getPrecioUnitario();
            return LineaDispensacionDto.builder().idMedicamento(x.getMedicamento().getIdMedicamento()).medicamento(x.getMedicamento().getNombre())
                    .presentacion(x.getMedicamento().getPresentacion()).cantidad(x.getCantidad()).precioUnitario(pu)
                    .subtotal(pu.multiply(BigDecimal.valueOf(x.getCantidad()))).build();
        }).toList();
        HistorialClinico h = r.getHistorial();
        return DispensacionDto.builder().idDispensacion(d.getIdDispensacion()).fecha(d.getFecha())
                .total(d.getTotal() == null ? BigDecimal.ZERO : d.getTotal()).anulada(d.isAnulada()).motivoAnulacion(d.getMotivoAnulacion())
                .farmaceutico(d.getFarmaceutico().getNombreCompleto()).recetaId(r.getIdReceta()).paciente(h.getPaciente().getNombreCompleto())
                .pacienteDocumento(h.getPaciente().getDocumentoIdentidad()).medico(h.getMedico().getNombreCompleto())
                .receta(receta(r)).lineas(lineas).pago(pago == null ? null : pago(pago)).build();
    }

    public static ConsultorioDto consultorio(Consultorio c) {
        return new ConsultorioDto(c.getIdConsultorio(), c.getNumeroPabellon(), c.getCodigoRoom(), c.getUbicacion(), c.isActivo());
    }

    public static MedicoDto medico(Medico m) {
        return new MedicoDto(m.getIdUsuario(), m.getNombreCompleto(), m.getNumeroColegiatura(),
                m.getEspecialidad().getIdEspecialidad(), m.getEspecialidad().getNombreEspecialidad());
    }

    public static ConfigTurnoDto turno(ConfigTurno t) {
        return new ConfigTurnoDto(t.getTurno(), t.getHoraInicio(), t.getHoraFin(), t.getDuracionMinutos());
    }

    public static HorarioDto horario(HorarioDisponible h) {
        Medico m = h.getMedico();
        return HorarioDto.builder().idHorario(h.getIdHorario()).fecha(h.getFecha()).horaInicio(h.getHoraInicio()).horaFin(h.getHoraFin())
                .disponible(h.isDisponible()).turno(h.getTurno()).medicoId(m.getIdUsuario()).medico(m.getNombreCompleto())
                .especialidadId(m.getEspecialidad().getIdEspecialidad()).especialidad(m.getEspecialidad().getNombreEspecialidad())
                .consultorioId(h.getConsultorio().getIdConsultorio()).pabellon(h.getConsultorio().getNumeroPabellon())
                .room(h.getConsultorio().getCodigoRoom()).build();
    }

    public static CitaDto cita(Cita c) {
        HorarioDisponible h = c.getHorario();
        return CitaDto.builder().idCita(c.getIdCita()).fechaHoraReserva(c.getFechaHoraReserva()).estado(c.getEstado())
                .origen(c.getOrigen()).checkIn(c.isCheckIn()).fecha(h.getFecha()).horaInicio(h.getHoraInicio()).horaFin(h.getHoraFin())
                .pacienteId(c.getPaciente().getIdUsuario()).paciente(c.getPaciente().getNombreCompleto())
                .pacienteDocumento(c.getPaciente().getDocumentoIdentidad())
                .medicoId(c.getMedico().getIdUsuario()).medico(c.getMedico().getNombreCompleto())
                .especialidadId(c.getEspecialidad().getIdEspecialidad()).especialidad(c.getEspecialidad().getNombreEspecialidad())
                .consultorioId(c.getConsultorio().getIdConsultorio()).pabellon(c.getConsultorio().getNumeroPabellon())
                .room(c.getConsultorio().getCodigoRoom()).ubicacion(c.getConsultorio().getUbicacion())
                .reservadaPor(c.getReservadaPor() != null ? c.getReservadaPor().getNombreCompleto() : null)
                .modificable(c.isModificable())
                .precio(c.getEspecialidad().getPrecioConsulta() == null ? BigDecimal.ZERO : c.getEspecialidad().getPrecioConsulta())
                .pagada(c.isPagada()).build();
    }

    public static TriajeDto triaje(Triaje t) {
        return t == null ? null : new TriajeDto(t.getIdTriaje(), t.getCita().getIdCita(), t.getPresionArterial(), t.getPeso(),
                t.getSignosVitales(), t.getFechaRegistro());
    }

    public static ListaEsperaDto espera(ListaEspera l) {
        return new ListaEsperaDto(l.getIdEspera(), l.getPaciente().getIdUsuario(), l.getPaciente().getNombreCompleto(),
                l.getPaciente().getDocumentoIdentidad(), l.getEspecialidad().getIdEspecialidad(),
                l.getEspecialidad().getNombreEspecialidad(), l.getFechaRegistro(), l.getEstado());
    }

    public static NotificacionDto notificacion(Notificacion n) {
        return new NotificacionDto(n.getIdNotificacion(), n.getMensaje(), n.getTipo(), n.getFechaEnvio(), n.isLeido());
    }

    public static MedicamentoDto medicamento(Medicamento m, LocalDate limiteVencimiento) {
        boolean bajo = m.isActivo() && m.getStockActual() <= m.getStockMinimo();
        boolean vence = m.isActivo() && m.getFechaVencimiento() != null && !m.getFechaVencimiento().isAfter(limiteVencimiento);
        return new MedicamentoDto(m.getIdMedicamento(), m.getNombre(), m.getPrincipioActivo(), m.getPresentacion(), m.getStockActual(),
                m.getStockMinimo(), m.getFechaVencimiento(), m.getPrecio(), m.isActivo(), bajo, vence);
    }

    public static MovimientoDto movimiento(MovimientoInventario m) {
        return new MovimientoDto(m.getIdMovimiento(), m.getTipo(), m.getCantidad(), m.getFecha(), m.getMotivo(),
                m.getMedicamento().getIdMedicamento(), m.getMedicamento().getNombre(), m.getUsuario().getNombreCompleto());
    }

    public static RecetaDto receta(RecetaMedica r) {
        HistorialClinico h = r.getHistorial();
        List<DetalleRecetaDto> det = r.getDetalles().stream().map(d -> DetalleRecetaDto.builder()
                .idDetalle(d.getIdDetalle()).medicamentoId(d.getMedicamento().getIdMedicamento()).medicamento(d.getMedicamento().getNombre())
                .presentacion(d.getMedicamento().getPresentacion()).dosis(d.getDosis()).cantidad(d.getCantidad())
                .cantidadDispensada(d.getCantidadDispensada()).stockActual(d.getMedicamento().getStockActual())
                .precioUnitario(d.getMedicamento().getPrecio() == null ? BigDecimal.ZERO : d.getMedicamento().getPrecio()).build()).toList();
        return RecetaDto.builder().idReceta(r.getIdReceta()).fecha(r.getFecha()).indicaciones(r.getIndicaciones()).estado(r.getEstado())
                .historialId(h.getIdHistorial()).paciente(h.getPaciente().getNombreCompleto())
                .pacienteDocumento(h.getPaciente().getDocumentoIdentidad()).medico(h.getMedico().getNombreCompleto()).detalles(det).build();
    }

    public static HistorialDto historial(HistorialClinico h, RecetaMedica receta, Triaje triaje) {
        return HistorialDto.builder().idHistorial(h.getIdHistorial()).citaId(h.getCita().getIdCita()).fechaRegistro(h.getFechaRegistro())
                .fechaCita(h.getCita().getHorario().getFecha()).diagnostico(h.getDiagnostico()).tratamiento(h.getTratamiento())
                .observaciones(h.getObservaciones()).medico(h.getMedico().getNombreCompleto())
                .especialidad(h.getMedico().getEspecialidad().getNombreEspecialidad()).pacienteId(h.getPaciente().getIdUsuario())
                .paciente(h.getPaciente().getNombreCompleto()).receta(receta == null ? null : receta(receta)).triaje(triaje(triaje)).build();
    }
}
