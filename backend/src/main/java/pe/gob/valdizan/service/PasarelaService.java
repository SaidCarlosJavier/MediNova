package pe.gob.valdizan.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pe.gob.valdizan.dto.Dto.PagoRequest;
import pe.gob.valdizan.exception.ApiException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.YearMonth;

/**
 * PASARELA DE PAGOS SIMULADA (demostración). Valida los datos como lo haría una pasarela real,
 * pero no mueve dinero. Para producción, reemplazar {@link #procesar} por la integración con
 * Culqi / Niubiz / Izipay / Mercado Pago, manteniendo la misma firma.
 *
 * Tarjetas de prueba: 4111 1111 1111 1111 (aprobada) · 4000 0000 0000 0002 (rechazada).
 * Yape/Plin: celular de 9 dígitos que empiece con 9; si termina en 000 se simula rechazo.
 */
@Service
public class PasarelaService {
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Resultado(String referencia, String tarjetaUltimos4, BigDecimal montoRecibido, BigDecimal vuelto) {}

    public Resultado procesar(PagoRequest r, BigDecimal monto) {
        return switch (r.metodo()) {
            case EFECTIVO -> efectivo(r, monto);
            case TARJETA -> tarjeta(r);
            case YAPE, PLIN -> billetera(r);
        };
    }

    private Resultado efectivo(PagoRequest r, BigDecimal monto) {
        BigDecimal recibido = r.montoRecibido() == null ? monto : r.montoRecibido();
        if (recibido.compareTo(monto) < 0) throw ApiException.invalida("El monto recibido es menor al total a pagar");
        return new Resultado("EFE-" + codigo(8), null, recibido, recibido.subtract(monto));
    }

    private Resultado tarjeta(PagoRequest r) {
        String num = r.numeroTarjeta() == null ? "" : r.numeroTarjeta().replaceAll("[\\s-]", "");
        if (!num.matches("\\d{13,19}") || !luhn(num)) throw ApiException.invalida("El número de tarjeta no es válido");
        if (r.titular() == null || r.titular().trim().length() < 3) throw ApiException.invalida("Ingrese el nombre del titular de la tarjeta");
        validarVencimiento(r.vencimiento());
        if (r.cvv() == null || !r.cvv().matches("\\d{3,4}")) throw ApiException.invalida("El código de seguridad (CVV) no es válido");
        if (num.endsWith("0002"))
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Pago rechazado por el banco emisor. Pruebe con otra tarjeta o método de pago.");
        return new Resultado("TRJ-" + codigo(10), num.substring(num.length() - 4), null, null);
    }

    private Resultado billetera(PagoRequest r) {
        String cel = r.celular() == null ? "" : r.celular().replaceAll("\\s", "");
        if (!cel.matches("9\\d{8}")) throw ApiException.invalida("Ingrese un celular válido de 9 dígitos que empiece con 9");
        if (cel.endsWith("000"))
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "El pago con " + r.metodo() + " fue rechazado. Verifique su saldo e intente nuevamente.");
        return new Resultado((r.metodo().name().substring(0, 3)) + "-" + codigo(10), null, null, null);
    }

    private void validarVencimiento(String v) {
        if (v == null || !v.matches("(0[1-9]|1[0-2])/\\d{2}")) throw ApiException.invalida("La fecha de vencimiento debe tener el formato MM/AA");
        YearMonth ym = YearMonth.of(2000 + Integer.parseInt(v.substring(3)), Integer.parseInt(v.substring(0, 2)));
        if (ym.isBefore(YearMonth.now())) throw ApiException.invalida("La tarjeta está vencida");
    }

    static boolean luhn(String n) {
        int suma = 0;
        boolean doble = false;
        for (int i = n.length() - 1; i >= 0; i--) {
            int d = n.charAt(i) - '0';
            if (doble) { d *= 2; if (d > 9) d -= 9; }
            suma += d;
            doble = !doble;
        }
        return suma % 10 == 0;
    }

    private String codigo(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        return sb.toString();
    }
}
