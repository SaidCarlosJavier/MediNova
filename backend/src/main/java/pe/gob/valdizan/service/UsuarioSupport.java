package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.gob.valdizan.domain.Enums.NombreRol;
import pe.gob.valdizan.domain.Rol;
import pe.gob.valdizan.domain.Usuario;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.RolRepository;
import pe.gob.valdizan.repository.Repositorios.UsuarioRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/** Utilidades compartidas para crear usuarios sin duplicados (RNF-10). */
@Component
@RequiredArgsConstructor
public class UsuarioSupport {
    private static final String ALFABETO = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder encoder;

    public void validarUnicos(String documento, String correo) {
        if (usuarios.existsByDocumentoIdentidad(documento))
            throw ApiException.conflicto("Ya existe un usuario registrado con ese documento de identidad");
        if (usuarios.existsByCorreoIgnoreCase(correo))
            throw ApiException.conflicto("Ya existe un usuario registrado con ese correo electrónico");
    }

    public Rol rol(NombreRol nombre) {
        return roles.findByNombreRol(nombre).orElseThrow(() -> new IllegalStateException("Rol no configurado: " + nombre));
    }

    public <U extends Usuario> U base(U u, String documento, String nombres, String apellidos, String correo,
                                      String passwordPlano, NombreRol rol, boolean consentimiento) {
        u.setDocumentoIdentidad(documento.trim());
        u.setNombres(nombres.trim());
        u.setApellidos(apellidos.trim());
        u.setCorreo(correo.trim().toLowerCase());
        u.setPasswordHash(encoder.encode(passwordPlano));
        u.setRol(rol(rol));
        u.setEstado(true);
        u.setConsentimientoDatos(consentimiento);
        if (consentimiento) u.setFechaConsentimiento(LocalDateTime.now());
        return u;
    }

    public String passwordAleatoria() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        return sb.toString();
    }

    public static boolean vacio(String s) { return s == null || s.isBlank(); }
}
