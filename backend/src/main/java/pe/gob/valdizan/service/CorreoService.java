package pe.gob.valdizan.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Envío de correo configurable: simulado por log o real si app.correo.habilitado=true y hay spring.mail.host. */
@Slf4j
@Service
public class CorreoService {
    private final boolean habilitado;
    private final String remitente;
    private final ObjectProvider<JavaMailSender> sender;

    public CorreoService(@Value("${app.correo.habilitado:false}") boolean habilitado,
                         @Value("${app.correo.remitente:no-reply@valdizan.gob.pe}") String remitente,
                         ObjectProvider<JavaMailSender> sender) {
        this.habilitado = habilitado;
        this.remitente = remitente;
        this.sender = sender;
    }

    public void enviar(String para, String asunto, String texto) {
        log.info("[CORREO SIMULADO] Para: {} | Asunto: {} | Mensaje: {}", para, asunto, texto);
        if (!habilitado || para == null || para.endsWith(".local")) return;
        JavaMailSender s = sender.getIfAvailable();
        if (s == null) {
            log.warn("Correo habilitado pero no hay servidor SMTP configurado (spring.mail.host)");
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(remitente);
            m.setTo(para);
            m.setSubject(asunto);
            m.setText(texto);
            s.send(m);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo a {}: {}", para, e.getMessage());
        }
    }
}
