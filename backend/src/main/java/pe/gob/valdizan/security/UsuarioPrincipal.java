package pe.gob.valdizan.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.gob.valdizan.domain.Usuario;

import java.util.Collection;
import java.util.List;

/** Identidad autenticada (id, rol) disponible en controladores con @AuthenticationPrincipal. */
@Getter
@AllArgsConstructor
public class UsuarioPrincipal implements UserDetails {
    private final Long id;
    private final String correo;
    private final String password;
    private final String rol;
    private final boolean activo;
    private final String nombreCompleto;

    public static UsuarioPrincipal de(Usuario u) {
        return new UsuarioPrincipal(u.getIdUsuario(), u.getCorreo(), u.getPasswordHash(),
                u.getRol().getNombreRol().name(), u.isEstado(), u.getNombreCompleto());
    }

    public boolean esPersonalAdministrativo() {
        return "ROLE_RECEPCIONISTA".equals(rol) || "ROLE_ADMIN".equals(rol);
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority(rol)); }
    @Override public String getUsername() { return correo; }
    @Override public boolean isEnabled() { return activo; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
