package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.AuditoriaLog;
import pe.gob.valdizan.repository.Repositorios.AuditoriaRepository;
import pe.gob.valdizan.repository.Repositorios.UsuarioRepository;

import java.time.LocalDateTime;

/** RNF-14: auditoría de accesos y modificaciones del historial clínico (transacción propia). */
@Service
@RequiredArgsConstructor
public class AuditoriaService {
    private final AuditoriaRepository auditoria;
    private final UsuarioRepository usuarios;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId, String accion, String entidad, String detalle) {
        AuditoriaLog l = new AuditoriaLog();
        l.setUsuario(usuarios.getReferenceById(usuarioId));
        l.setAccion(accion);
        l.setEntidad(entidad);
        l.setDetalle(detalle);
        l.setFechaHora(LocalDateTime.now());
        auditoria.save(l);
    }
}
