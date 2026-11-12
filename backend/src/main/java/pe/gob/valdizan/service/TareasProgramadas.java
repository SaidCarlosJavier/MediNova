package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** RF-12: recordatorio diario 24 h antes; cierre de inasistencias al final del día. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class TareasProgramadas {
    private final CitaService citas;

    @Scheduled(cron = "${app.recordatorio.cron}", zone = "America/Lima")
    public void recordatorios() { log.info("Recordatorios enviados: {}", citas.enviarRecordatorios()); }

    @Scheduled(cron = "${app.inasistencia.cron}", zone = "America/Lima")
    public void inasistencias() { log.info("Citas marcadas como inasistencia: {}", citas.marcarInasistencias()); }
}
