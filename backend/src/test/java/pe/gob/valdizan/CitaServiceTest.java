package pe.gob.valdizan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.EstadoCita;
import pe.gob.valdizan.domain.Enums.NombreRol;
import pe.gob.valdizan.domain.Enums.TipoNotificacion;
import pe.gob.valdizan.dto.Dto.CitaDto;
import pe.gob.valdizan.dto.Dto.ReservaRequest;
import pe.gob.valdizan.exception.ApiException;
import pe.gob.valdizan.repository.Repositorios.*;
import pe.gob.valdizan.security.UsuarioPrincipal;
import pe.gob.valdizan.service.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CitaServiceTest {
    @Mock CitaRepository citas;
    @Mock HorarioRepository horarios;
    @Mock UsuarioRepository usuarios;
    @Mock TriajeRepository triajes;
    @Mock AccesoService acceso;
    @Mock NotificacionService notificaciones;
    @Mock ListaEsperaService listaEspera;
    @Mock PagoService pagoService;
    @InjectMocks CitaService service;

    Paciente paciente;
    Medico medico;
    Especialidad especialidad;
    Consultorio consultorio;
    UsuarioPrincipal principal;

    @BeforeEach
    void preparar() {
        especialidad = new Especialidad();
        especialidad.setIdEspecialidad(1L);
        especialidad.setNombreEspecialidad("Psiquiatría");

        medico = new Medico();
        medico.setIdUsuario(10L);
        medico.setNombres("Ana");
        medico.setApellidos("Salazar");
        medico.setEspecialidad(especialidad);

        consultorio = new Consultorio();
        consultorio.setIdConsultorio(1L);
        consultorio.setNumeroPabellon("A");
        consultorio.setCodigoRoom("101");

        paciente = new Paciente();
        paciente.setIdUsuario(20L);
        paciente.setNombres("Sofía");
        paciente.setApellidos("Vargas");
        paciente.setDocumentoIdentidad("40000002");

        principal = new UsuarioPrincipal(20L, "paciente@x.pe", "x", NombreRol.ROLE_PACIENTE.name(), true, "Sofía Vargas");
        when(acceso.paciente(eq(20L), any())).thenReturn(paciente);
        when(citas.saveAndFlush(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
    }

    private HorarioDisponible horario(long id, int dias, int hora) {
        HorarioDisponible h = new HorarioDisponible();
        h.setIdHorario(id);
        h.setFecha(LocalDate.now().plusDays(dias));
        h.setHoraInicio(LocalTime.of(hora, 0));
        h.setHoraFin(LocalTime.of(hora, 30));
        h.setMedico(medico);
        h.setConsultorio(consultorio);
        h.setDisponible(true);
        when(horarios.findById(id)).thenReturn(Optional.of(h));
        return h;
    }

    private Cita citaActiva(HorarioDisponible h) {
        Cita c = new Cita();
        c.setIdCita(99L);
        c.setPaciente(paciente);
        c.setMedico(medico);
        c.setConsultorio(consultorio);
        c.setEspecialidad(especialidad);
        c.setHorario(h);
        c.setEstado(EstadoCita.CONFIRMADA);
        c.setClaveActiva(c.construirClave());
        h.setDisponible(false);
        when(citas.findById(99L)).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void reservaExitosaOcupaElHorarioYNotifica() {
        HorarioDisponible h = horario(1L, 2, 9);
        CitaDto dto = service.reservar(new ReservaRequest(20L, 1L, null, 45), principal);

        assertEquals(EstadoCita.CONFIRMADA, dto.estado());
        assertFalse(h.isDisponible(), "el horario debe quedar ocupado");
        verify(notificaciones).notificarPaciente(eq(paciente), eq(TipoNotificacion.CONFIRMACION), contains("Psiquiatría"));
        verify(listaEspera).cerrarPara(20L, 1L);
    }

    @Test
    void reservaDuplicadaDevuelveConflictoYNoOcupaElHorario() {
        HorarioDisponible h = horario(1L, 2, 9);
        when(citas.existsByClaveActiva(anyString())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.reservar(new ReservaRequest(20L, 1L, null, null), principal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(h.isDisponible());
        verify(citas, never()).saveAndFlush(any());
    }

    @Test
    void reservaEnHorarioOcupadoEsRechazada() {
        HorarioDisponible h = horario(1L, 2, 9);
        h.setDisponible(false);
        ApiException ex = assertThrows(ApiException.class, () -> service.reservar(new ReservaRequest(20L, 1L, null, null), principal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void reservaEnHorarioPasadoEsRechazada() {
        horario(1L, -1, 9);
        ApiException ex = assertThrows(ApiException.class, () -> service.reservar(new ReservaRequest(20L, 1L, null, null), principal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void cancelarLiberaElTurnoNotificaYAvisaALaListaDeEspera() {
        HorarioDisponible h = horario(1L, 2, 9);
        Cita c = citaActiva(h);

        CitaDto dto = service.cancelar(99L, principal);

        assertEquals(EstadoCita.CANCELADA, dto.estado());
        assertTrue(h.isDisponible(), "el turno debe liberarse de inmediato");
        assertNull(c.getClaveActiva(), "la clave de duplicidad debe liberarse");
        verify(notificaciones).notificarPaciente(eq(paciente), eq(TipoNotificacion.CANCELACION), anyString());
        verify(listaEspera).avisarPrimero(especialidad);
    }

    @Test
    void noSePuedeCancelarUnaCitaYaAtendida() {
        Cita c = citaActiva(horario(1L, 2, 9));
        c.setEstado(EstadoCita.ATENDIDA);
        ApiException ex = assertThrows(ApiException.class, () -> service.cancelar(99L, principal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void reprogramarIntercambiaLosHorarios() {
        HorarioDisponible viejo = horario(1L, 2, 9);
        HorarioDisponible nuevo = horario(2L, 3, 10);
        Cita c = citaActiva(viejo);

        CitaDto dto = service.reprogramar(99L, 2L, principal);

        assertEquals(EstadoCita.REPROGRAMADA, dto.estado());
        assertTrue(viejo.isDisponible());
        assertFalse(nuevo.isDisponible());
        assertSame(nuevo, c.getHorario());
        verify(notificaciones).notificarPaciente(eq(paciente), eq(TipoNotificacion.REPROGRAMACION), anyString());
    }

    @Test
    void reprogramarAUnHorarioOcupadoEsConflicto() {
        HorarioDisponible viejo = horario(1L, 2, 9);
        HorarioDisponible nuevo = horario(2L, 3, 10);
        nuevo.setDisponible(false);
        citaActiva(viejo);

        ApiException ex = assertThrows(ApiException.class, () -> service.reprogramar(99L, 2L, principal));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertFalse(viejo.isDisponible(), "el horario original se conserva");
    }

    @Test
    void reprogramarAlMismoHorarioEsInvalido() {
        HorarioDisponible viejo = horario(1L, 2, 9);
        citaActiva(viejo);
        ApiException ex = assertThrows(ApiException.class, () -> service.reprogramar(99L, 1L, principal));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void checkInSoloElDiaDeLaCita() {
        citaActiva(horario(1L, 2, 9));
        ApiException ex = assertThrows(ApiException.class, () -> service.checkIn(99L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void soloElMedicoAsignadoMarcaLaCitaComoAtendida() {
        citaActiva(horario(1L, 0, 9));
        UsuarioPrincipal otro = new UsuarioPrincipal(77L, "otro@x.pe", "x", NombreRol.ROLE_MEDICO.name(), true, "Otro");
        ApiException ex = assertThrows(ApiException.class, () -> service.marcarAtendida(99L, otro));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());

        UsuarioPrincipal asignado = new UsuarioPrincipal(10L, "m@x.pe", "x", NombreRol.ROLE_MEDICO.name(), true, "Ana");
        assertEquals(EstadoCita.ATENDIDA, service.marcarAtendida(99L, asignado).estado());
    }
}
