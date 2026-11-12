package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.NombreRol;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.UsuarioRepository;
import pe.gob.valdizan.security.JwtService;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.time.LocalDateTime;
import java.util.UUID;

import static pe.gob.valdizan.service.UsuarioSupport.vacio;

/** login, registro, recuperarContrasena, perfil (métodos del diagrama de Usuario). logout es del lado cliente (JWT stateless). */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarios;
    private final UsuarioSupport soporte;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CorreoService correo;

    @Value("${app.frontend-url}") private String frontendUrl;
    @Value("${app.recuperacion.minutos:30}") private long minutosToken;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        String id = r.identificador().trim();
        Usuario u = usuarios.findByCorreoIgnoreCase(id).or(() -> usuarios.findByDocumentoIdentidad(id))
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> ApiException.noAutenticado("Credenciales incorrectas"));
        if (!u.isEstado()) throw ApiException.prohibido("Su cuenta está desactivada. Comuníquese con el hospital.");
        return new AuthResponse(jwt.generar(UsuarioPrincipal.de(u)), Mapper.usuario(u));
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest r) {
        String doc = r.documentoIdentidad().trim();
        String correoNorm = r.correo().trim().toLowerCase();
        soporte.validarUnicos(doc, correoNorm);
        Usuario u;
        if ("TUTOR".equals(r.tipo())) {
            if (vacio(r.parentesco())) throw ApiException.invalida("Indique su parentesco con el paciente");
            Tutor t = soporte.base(new Tutor(), doc, r.nombres(), r.apellidos(), correoNorm, r.password(), NombreRol.ROLE_TUTOR, true);
            t.setParentesco(r.parentesco().trim());
            t.setTelefono(r.telefono());
            t.setDireccion(r.direccion());
            u = t;
        } else {
            if (r.fechaNacimiento() == null) throw ApiException.invalida("Ingrese su fecha de nacimiento");
            Paciente p = soporte.base(new Paciente(), doc, r.nombres(), r.apellidos(), correoNorm, r.password(), NombreRol.ROLE_PACIENTE, true);
            p.setTipoSeguro(r.tipoSeguro());
            p.setTelefonoContacto(r.telefonoContacto());
            p.setFechaNacimiento(r.fechaNacimiento());
            p.setRequiereTutor(Boolean.TRUE.equals(r.requiereTutor()));
            u = p;
        }
        usuarios.save(u);
        return new AuthResponse(jwt.generar(UsuarioPrincipal.de(u)), Mapper.usuario(u));
    }

    /** RF-03: genera un token temporal; la respuesta es siempre la misma para no revelar si el correo existe. */
    @Transactional
    public void recuperar(String correoSolicitado) {
        usuarios.findByCorreoIgnoreCase(correoSolicitado.trim()).filter(Usuario::isEstado).ifPresent(u -> {
            String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            u.setTokenRecuperacion(token);
            u.setTokenExpira(LocalDateTime.now().plusMinutes(minutosToken));
            correo.enviar(u.getCorreo(), "Recuperación de contraseña - Hospital Hermilio Valdizán",
                    "Para restablecer su contraseña ingrese a: " + frontendUrl + "/restablecer?token=" + token
                            + " (válido por " + minutosToken + " minutos)");
        });
    }

    @Transactional
    public void restablecer(RestablecerRequest r) {
        Usuario u = usuarios.findByTokenRecuperacion(r.token())
                .filter(x -> x.getTokenExpira() != null && x.getTokenExpira().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> ApiException.invalida("El enlace de recuperación es inválido o ya expiró"));
        u.setPasswordHash(encoder.encode(r.nuevaPassword()));
        u.setTokenRecuperacion(null);
        u.setTokenExpira(null);
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfil(Long id) {
        return Mapper.usuario(usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("El usuario no existe")));
    }

    @Transactional
    public UsuarioDto actualizarPerfil(Long id, PerfilRequest r) {
        Usuario u = (Usuario) Hibernate.unproxy(usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("El usuario no existe")));
        u.setNombres(r.nombres().trim());
        u.setApellidos(r.apellidos().trim());
        if (u instanceof Paciente p) {
            p.setTipoSeguro(r.tipoSeguro());
            p.setTelefonoContacto(r.telefonoContacto());
            if (r.fechaNacimiento() != null) p.setFechaNacimiento(r.fechaNacimiento());
        } else if (u instanceof Tutor t) {
            if (!vacio(r.parentesco())) t.setParentesco(r.parentesco().trim());
            t.setTelefono(r.telefono());
            t.setDireccion(r.direccion());
        }
        if (!vacio(r.nuevaPassword())) u.setPasswordHash(encoder.encode(r.nuevaPassword()));
        return Mapper.usuario(u);
    }
}
