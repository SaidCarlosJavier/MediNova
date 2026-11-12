package pe.gob.valdizan.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Excepción de negocio con código HTTP asociado. */
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public static ApiException invalida(String m) { return new ApiException(HttpStatus.BAD_REQUEST, m); }
    public static ApiException noEncontrado(String m) { return new ApiException(HttpStatus.NOT_FOUND, m); }
    public static ApiException conflicto(String m) { return new ApiException(HttpStatus.CONFLICT, m); }
    public static ApiException prohibido(String m) { return new ApiException(HttpStatus.FORBIDDEN, m); }
    public static ApiException noAutenticado(String m) { return new ApiException(HttpStatus.UNAUTHORIZED, m); }
}
