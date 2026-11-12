package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.Enums.TipoNotificacion;
import pe.gob.valdizan.domain.Notificacion;
import pe.gob.valdizan.domain.Paciente;
import pe.gob.valdizan.domain.Usuario;
import pe.gob.valdizan.dto.Dto.NotificacionDto;
import pe.gob.valdizan.dto.Dto.PageDto;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.NotificacionRepository;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {
    private final NotificacionRepository repo;
    private final CorreoService correo;

    /** Notifica al paciente y, si tiene tutor, también al tutor (RF-11/12/13). */
    @Transactional
    public void notificarPaciente(Paciente p, TipoNotificacion tipo, String mensaje) {
        enviar(p, tipo, mensaje);
        if (p.getTutor() != null) {
            enviar(p.getTutor(), tipo, "[Paciente: " + p.getNombreCompleto() + "] " + mensaje);
        }
    }

    private void enviar(Usuario destinatario, TipoNotificacion tipo, String mensaje) {
        Notificacion n = new Notificacion(destinatario, tipo, mensaje);
        n.enviar();
        repo.save(n);
        correo.enviar(destinatario.getCorreo(), "Hospital Hermilio Valdizán - " + tipo.name(), mensaje);
    }

    @Transactional(readOnly = true)
    public PageDto<NotificacionDto> listar(UsuarioPrincipal p, int pagina, int tamanio) {
        return PageDto.de(repo.findByDestinatario_IdUsuarioOrderByFechaEnvioDesc(p.getId(), PageRequest.of(pagina, tamanio)), Mapper::notificacion);
    }

    @Transactional(readOnly = true)
    public long noLeidas(UsuarioPrincipal p) { return repo.countByDestinatario_IdUsuarioAndLeidoFalse(p.getId()); }

    @Transactional
    public void marcarLeido(Long id, UsuarioPrincipal p) {
        Notificacion n = repo.findById(id).orElseThrow(() -> ApiException.noEncontrado("La notificación no existe"));
        if (!n.getDestinatario().getIdUsuario().equals(p.getId())) throw ApiException.prohibido("La notificación no le pertenece");
        n.marcarLeido();
    }

    @Transactional
    public void leerTodas(UsuarioPrincipal p) {
        List<Notificacion> l = repo.findByDestinatario_IdUsuarioAndLeidoFalse(p.getId());
        l.forEach(Notificacion::marcarLeido);
    }
}
