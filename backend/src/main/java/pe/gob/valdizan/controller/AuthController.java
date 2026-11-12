package pe.gob.valdizan.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.AuthService;

@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.registrar(r));
    }

    @PostMapping("/recuperar")
    public MensajeDto recuperar(@Valid @RequestBody RecuperarRequest r) {
        auth.recuperar(r.correo());
        return new MensajeDto("Si el correo está registrado, recibirá instrucciones para restablecer su contraseña");
    }

    @PostMapping("/restablecer")
    public MensajeDto restablecer(@Valid @RequestBody RestablecerRequest r) {
        auth.restablecer(r);
        return new MensajeDto("Contraseña actualizada correctamente. Ya puede iniciar sesión");
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal UsuarioPrincipal p) { return auth.perfil(p.getId()); }

    @PutMapping("/perfil")
    public UsuarioDto perfil(@AuthenticationPrincipal UsuarioPrincipal p, @Valid @RequestBody PerfilRequest r) {
        return auth.actualizarPerfil(p.getId(), r);
    }
}
