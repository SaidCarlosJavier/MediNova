package pe.gob.valdizan.domain;

/** Enumeraciones del dominio agrupadas (import pe.gob.valdizan.domain.Enums.*). */
public final class Enums {
    private Enums() {}

    public enum NombreRol { ROLE_PACIENTE, ROLE_TUTOR, ROLE_RECEPCIONISTA, ROLE_MEDICO, ROLE_ADMIN, ROLE_FARMACEUTICO }

    public enum Cargo {
        RECEPCIONISTA, ADMINISTRADOR, FARMACEUTICO;

        public NombreRol rol() {
            return switch (this) {
                case RECEPCIONISTA -> NombreRol.ROLE_RECEPCIONISTA;
                case ADMINISTRADOR -> NombreRol.ROLE_ADMIN;
                case FARMACEUTICO -> NombreRol.ROLE_FARMACEUTICO;
            };
        }
    }

    /** MANANA = turno mañana (se evita la Ñ en identificadores y valores de BD). */
    public enum Turno { MANANA, TARDE }

    public enum EstadoCita { RESERVADA, CONFIRMADA, REPROGRAMADA, CANCELADA, ASISTIO, ATENDIDA, NO_ASISTIO }

    public enum Origen { MOVIL, WEB, PRESENCIAL }

    public enum EstadoReceta { PENDIENTE, DISPENSADA, PARCIAL }

    public enum TipoMovimiento { ENTRADA, SALIDA, AJUSTE }

    public enum TipoNotificacion { CONFIRMACION, RECORDATORIO, REPROGRAMACION, CANCELACION, LISTA_ESPERA, PAGO }

    public enum MetodoPago { EFECTIVO, TARJETA, YAPE, PLIN }

    public enum EstadoPago { PAGADO, ANULADO }

    public enum TipoPago { CONSULTA, FARMACIA }

    public enum EstadoEspera { EN_ESPERA, NOTIFICADO, ATENDIDO, CANCELADO }
}
