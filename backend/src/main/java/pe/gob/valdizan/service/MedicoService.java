package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.Cita;
import pe.gob.valdizan.domain.Enums.EstadoCita;
import pe.gob.valdizan.domain.Paciente;
import pe.gob.valdizan.dto.Dto.MedicoPacienteDto;
import pe.gob.valdizan.dto.Dto.MedicoResumenDto;
import pe.gob.valdizan.repository.Repositorios.CitaRepository;

import java.time.LocalDate;
import java.util.*;

/** Vistas propias del médico: sus pacientes y su resumen del día/semana. */
@Service
@RequiredArgsConstructor
public class MedicoService {
    private final CitaRepository citas;

    @Transactional(readOnly = true)
    public List<MedicoPacienteDto> misPacientes(Long medicoId, String q) {
        LocalDate hoy = LocalDate.now();
        Map<Long, List<Cita>> porPaciente = new LinkedHashMap<>();
        for (Cita c : citas.deMedico(medicoId)) porPaciente.computeIfAbsent(c.getPaciente().getIdUsuario(), k -> new ArrayList<>()).add(c);
        String filtro = q == null ? "" : q.trim().toLowerCase();

        List<MedicoPacienteDto> res = new ArrayList<>();
        for (List<Cita> lista : porPaciente.values()) {
            Paciente p = lista.get(0).getPaciente();
            if (!filtro.isEmpty() && !(p.getNombreCompleto().toLowerCase().contains(filtro) || p.getDocumentoIdentidad().startsWith(filtro))) continue;
            LocalDate ultima = lista.stream().map(c -> c.getHorario().getFecha()).filter(f -> !f.isAfter(hoy)).max(Comparator.naturalOrder()).orElse(null);
            LocalDate proxima = lista.stream().filter(Cita::isModificable).map(c -> c.getHorario().getFecha()).filter(f -> !f.isBefore(hoy))
                    .min(Comparator.naturalOrder()).orElse(null);
            long atendidas = lista.stream().filter(c -> c.getEstado() == EstadoCita.ATENDIDA).count();
            res.add(new MedicoPacienteDto(p.getIdUsuario(), p.getNombreCompleto(), p.getDocumentoIdentidad(), ultima, proxima, lista.size(), atendidas));
        }
        res.sort(Comparator.comparing(MedicoPacienteDto::nombreCompleto));
        return res;
    }

    @Transactional(readOnly = true)
    public MedicoResumenDto resumen(Long medicoId) {
        LocalDate hoy = LocalDate.now();
        LocalDate finSemana = hoy.plusDays(6);
        List<Cita> todas = citas.deMedico(medicoId);
        List<Cita> delDia = todas.stream().filter(c -> c.getHorario().getFecha().equals(hoy)
                && c.getEstado() != EstadoCita.CANCELADA).toList();
        long atendidas = delDia.stream().filter(c -> c.getEstado() == EstadoCita.ATENDIDA).count();
        long semana = todas.stream().filter(c -> !c.getHorario().getFecha().isBefore(hoy) && !c.getHorario().getFecha().isAfter(finSemana)
                && c.getEstado() != EstadoCita.CANCELADA).count();
        long pacientes = todas.stream().map(c -> c.getPaciente().getIdUsuario()).distinct().count();
        return new MedicoResumenDto(delDia.size(), atendidas, delDia.size() - atendidas, semana, pacientes, hoy);
    }
}
