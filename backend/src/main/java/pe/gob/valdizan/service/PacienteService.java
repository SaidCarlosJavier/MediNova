package pe.gob.valdizan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.Enums.NombreRol;
import pe.gob.valdizan.domain.Paciente;
import pe.gob.valdizan.domain.Tutor;
import pe.gob.valdizan.dto.Dto.*;
import pe.gob.valdizan.dto.Mapper;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.PacienteRepository;
import pe.gob.valdizan.repository.Repositorios.TutorRepository;
import pe.gob.valdizan.security.UsuarioPrincipal;

import java.util.List;

import static pe.gob.valdizan.service.UsuarioSupport.vacio;

/** RF-01 (tutor vincula pacientes), RF-17 (recepción registra paciente y tutor), RF-18 (búsqueda por documento). */
@Service
@RequiredArgsConstructor
public class PacienteService {
    private final PacienteRepository pacientes;
    private final TutorRepository tutores;
    private final UsuarioSupport soporte;
    private final CorreoService correo;

    @Transactional
    public RegistroPacienteResponse registrar(PacienteRequest r, UsuarioPrincipal p) {
        String doc = r.documentoIdentidad().trim();
        // Pacientes que no gestionan su cuenta: correo interno y contraseña aleatoria (actúa el tutor/recepción)
        String correoNorm = vacio(r.correo()) ? doc.toLowerCase() + "@pacientes.valdizan.local" : r.correo().trim().toLowerCase();
        soporte.validarUnicos(doc, correoNorm);

        Tutor tutor = null;
        String passwordTemporal = null;
        if ("ROLE_TUTOR".equals(p.getRol())) {
            tutor = tutores.findById(p.getId()).orElseThrow(() -> ApiException.noEncontrado("El tutor no existe"));
        } else if (r.tutor() != null) {
            TutorRequest t = r.tutor();
            tutor = tutores.findByDocumentoIdentidad(t.documentoIdentidad().trim()).orElse(null);
            if (tutor == null) {
                String tCorreo = t.correo().trim().toLowerCase();
                soporte.validarUnicos(t.documentoIdentidad().trim(), tCorreo);
                passwordTemporal = soporte.passwordAleatoria();
                tutor = soporte.base(new Tutor(), t.documentoIdentidad(), t.nombres(), t.apellidos(), tCorreo, passwordTemporal,
                        NombreRol.ROLE_TUTOR, true);
                tutor.setParentesco(t.parentesco().trim());
                tutor.setTelefono(t.telefono());
                tutor.setDireccion(t.direccion());
                tutores.save(tutor);
                correo.enviar(tCorreo, "Cuenta de tutor - Hospital Hermilio Valdizán",
                        "Se creó su cuenta de tutor/apoderado. Contraseña temporal: " + passwordTemporal + ". Cámbiela al ingresar.");
            }
        } else if (r.requiereTutor()) {
            throw ApiException.invalida("Indique los datos del tutor/apoderado del paciente");
        }

        Paciente pac = soporte.base(new Paciente(), doc, r.nombres(), r.apellidos(), correoNorm, soporte.passwordAleatoria(),
                NombreRol.ROLE_PACIENTE, r.consentimiento());
        pac.setTipoSeguro(r.tipoSeguro());
        pac.setTelefonoContacto(r.telefonoContacto());
        pac.setFechaNacimiento(r.fechaNacimiento());
        pac.setTutor(tutor);
        pac.setRequiereTutor(r.requiereTutor() || tutor != null);
        pacientes.save(pac);
        return new RegistroPacienteResponse(Mapper.paciente(pac), passwordTemporal);
    }

    @Transactional
    public PacienteDto vincular(VincularRequest r, UsuarioPrincipal p) {
        Paciente pac = pacientes.findByDocumentoIdentidad(r.documentoIdentidad().trim())
                .filter(x -> r.fechaNacimiento().equals(x.getFechaNacimiento()))
                .orElseThrow(() -> ApiException.noEncontrado("No se encontró un paciente con esos datos"));
        if (pac.getTutor() != null) throw ApiException.conflicto("El paciente ya tiene un tutor/apoderado asignado");
        Tutor tutor = tutores.findById(p.getId()).orElseThrow(() -> ApiException.noEncontrado("El tutor no existe"));
        pac.setTutor(tutor);
        pac.setRequiereTutor(true);
        return Mapper.paciente(pac);
    }

    @Transactional(readOnly = true)
    public List<PacienteDto> aCargo(Long tutorId) {
        return pacientes.findByTutor_IdUsuarioOrderByApellidos(tutorId).stream().map(Mapper::paciente).toList();
    }

    @Transactional(readOnly = true)
    public PacienteDto porDocumento(String documento) {
        return pacientes.findByDocumentoIdentidad(documento.trim()).map(Mapper::paciente)
                .orElseThrow(() -> ApiException.noEncontrado("No se encontró un paciente con ese documento"));
    }

    @Transactional(readOnly = true)
    public PageDto<PacienteDto> buscar(String q, int pagina, int tamanio) {
        return PageDto.de(pacientes.buscar(q == null ? "" : q.trim(), PageRequest.of(pagina, tamanio)), Mapper::paciente);
    }
}
