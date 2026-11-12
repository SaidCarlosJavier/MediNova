package pe.gob.valdizan.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expiracionMs;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expiracionMs) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiracionMs = expiracionMs;
    }

    public String generar(UsuarioPrincipal p) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder().subject(String.valueOf(p.getId())).claim("rol", p.getRol())
                .issuedAt(new Date(ahora)).expiration(new Date(ahora + expiracionMs)).signWith(key).compact();
    }

    /** Devuelve el id de usuario del token o lanza JwtException si es inválido/expirado. */
    public Long idUsuario(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        try {
            return Long.valueOf(c.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("Token inválido");
        }
    }
}
