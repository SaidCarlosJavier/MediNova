package pe.gob.valdizan.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pe.gob.valdizan.dto.Dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Respuesta JSON uniforme: timestamp, status, mensaje, errores por campo. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> resp(HttpStatus s, String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(s).body(new ErrorResponse(LocalDateTime.now(), s.value(), mensaje, errores));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> api(ApiException e) { return resp(e.getStatus(), e.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> errores.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return resp(HttpStatus.BAD_REQUEST, "Hay campos con datos inválidos. Revise el formulario.", errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> restricciones(ConstraintViolationException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> errores.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return resp(HttpStatus.BAD_REQUEST, "Hay datos inválidos en la solicitud.", errores);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> solicitudInvalida(Exception e) {
        return resp(HttpStatus.BAD_REQUEST, "La solicitud es inválida o tiene un formato incorrecto.", null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> bloqueoOptimista(ObjectOptimisticLockingFailureException e) {
        return resp(HttpStatus.CONFLICT, "El horario acaba de ser reservado por otra persona. Elija otro horario.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return resp(HttpStatus.CONFLICT, "La operación genera un duplicado o entra en conflicto con datos existentes.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> acceso(AccessDeniedException e) {
        return resp(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción.", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> autenticacion(AuthenticationException e) {
        return resp(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión para continuar.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodo(HttpRequestMethodNotSupportedException e) {
        return resp(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(NoResourceFoundException e) {
        return resp(HttpStatus.NOT_FOUND, "Recurso no encontrado.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generico(Exception e) {
        log.error("Error no controlado", e);
        return resp(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intente nuevamente.", null);
    }
}
