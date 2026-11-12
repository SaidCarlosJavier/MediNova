package pe.gob.valdizan.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.valdizan.domain.*;
import pe.gob.valdizan.domain.Enums.Cargo;
import pe.gob.valdizan.domain.Enums.NombreRol;
import pe.gob.valdizan.domain.Enums.Turno;
import pe.gob.valdizan.repository.Repositorios.*;
import pe.gob.valdizan.service.HorarioService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Datos semilla idempotentes (solo se crea lo que no existe). Datos ficticios, sin información clínica real. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {
    static final String PASS_ADMIN = "Admin2026*";
    static final String PASS_GENERAL = "Valdizan2026*";

    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final EspecialidadRepository especialidades;
    private final ConsultorioRepository consultorios;
    private final ConfigTurnoRepository turnos;
    private final MedicamentoRepository medicamentos;
    private final MovimientoRepository movimientos;
    private final PasswordEncoder encoder;
    private final HorarioService horarioService;

    @Override
    @Transactional
    public void run(String... args) {
        for (NombreRol r : NombreRol.values()) if (roles.findByNombreRol(r).isEmpty()) roles.save(new Rol(r));

        Especialidad psiquiatria = especialidad("Psiquiatría", "Diagnóstico y tratamiento de trastornos mentales, con manejo farmacológico.", "60.00");
        Especialidad psicologia = especialidad("Psicología", "Evaluación y psicoterapia individual para el bienestar emocional.", "45.00");

        turno(Turno.MANANA, LocalTime.of(8, 0), LocalTime.of(12, 0));
        turno(Turno.TARDE, LocalTime.of(14, 0), LocalTime.of(18, 0));

        Consultorio c1 = consultorio("A", "101");
        Consultorio c2 = consultorio("A", "102");
        Consultorio c3 = consultorio("B", "201");

        Administrativo admin = administrativo("10000001", "Carlos", "Quispe Rojas", "admin@valdizan.gob.pe", PASS_ADMIN, Cargo.ADMINISTRADOR);
        administrativo("10000002", "Lucía", "Mamani Torres", "recepcion@valdizan.gob.pe", PASS_GENERAL, Cargo.RECEPCIONISTA);
        administrativo("10000003", "Jorge", "Huamán Díaz", "farmacia@valdizan.gob.pe", PASS_GENERAL, Cargo.FARMACEUTICO);

        Medico m1 = medico("20000001", "Ana", "Salazar Vega", "psiquiatra@valdizan.gob.pe", "CMP-10001", psiquiatria);
        Medico m2 = medico("20000002", "Ricardo", "Paredes Luna", "psicologo@valdizan.gob.pe", "CPsP-20002", psicologia);

        Tutor tutor = tutor("30000001", "María", "Flores Castro", "tutor@valdizan.gob.pe", "Madre");
        paciente("40000001", "Pedro", "Ramos Flores", "paciente.tutelado@valdizan.gob.pe", LocalDate.of(2001, 5, 14), tutor);
        paciente("40000002", "Sofía", "Vargas Núñez", "paciente@valdizan.gob.pe", LocalDate.of(1990, 9, 3), null);

        LocalDate hoy = LocalDate.now();
        for (Turno t : Turno.values()) {
            horarioService.generar(m1, c1, hoy, hoy.plusDays(6), t);
            horarioService.generar(m2, t == Turno.MANANA ? c2 : c3, hoy, hoy.plusDays(6), t);
        }

        if (medicamentos.count() == 0) {
            medicamento("Sertralina 50 mg", "Sertralina", "Tableta x 30", 200, 40, 540, "0.80");
            medicamento("Fluoxetina 20 mg", "Fluoxetina", "Cápsula x 30", 180, 40, 500, "0.50");
            medicamento("Risperidona 2 mg", "Risperidona", "Tableta x 30", 120, 30, 480, "0.90");
            medicamento("Clonazepam 2 mg", "Clonazepam", "Tableta x 30", 90, 30, 365, "0.70");
            medicamento("Carbonato de Litio 300 mg", "Litio", "Tableta x 50", 60, 20, 420, "1.10");
            medicamento("Quetiapina 100 mg", "Quetiapina", "Tableta x 30", 25, 30, 600, "1.50");
            medicamento("Escitalopram 10 mg", "Escitalopram", "Tableta x 28", 150, 30, 520, "1.20");
            medicamento("Valproato de sodio 500 mg", "Ácido valproico", "Tableta x 50", 70, 25, 45, "0.95");
            medicamento("Haloperidol 5 mg", "Haloperidol", "Tableta x 20", 100, 25, 450, "0.40");
            medicamento("Olanzapina 10 mg", "Olanzapina", "Tableta x 28", 85, 25, 560, "1.60");
        }
        log.info("Datos semilla verificados (admin: {})", admin.getCorreo());
    }

    private Especialidad especialidad(String nombre, String desc, String tarifa) {
        Especialidad e = especialidades.findByNombreEspecialidadIgnoreCase(nombre).orElseGet(() -> {
            Especialidad n = new Especialidad();
            n.setNombreEspecialidad(nombre);
            n.setDescripcion(desc);
            return n;
        });
        // Bases de datos creadas antes de existir la tarifa: se completa solo si falta
        if (e.getPrecioConsulta() == null || (e.getIdEspecialidad() == null)) e.setPrecioConsulta(new BigDecimal(tarifa));
        return especialidades.save(e);
    }

    private void turno(Turno t, LocalTime ini, LocalTime fin) {
        if (turnos.findByTurno(t).isPresent()) return;
        ConfigTurno c = new ConfigTurno();
        c.setTurno(t);
        c.setHoraInicio(ini);
        c.setHoraFin(fin);
        c.setDuracionMinutos(30);
        turnos.save(c);
    }

    private Consultorio consultorio(String pabellon, String room) {
        return consultorios.findAll().stream().filter(c -> c.getNumeroPabellon().equals(pabellon) && c.getCodigoRoom().equals(room))
                .findFirst().orElseGet(() -> {
                    Consultorio c = new Consultorio();
                    c.setNumeroPabellon(pabellon);
                    c.setCodigoRoom(room);
                    return consultorios.save(c);
                });
    }

    private <U extends Usuario> U base(U u, String doc, String nom, String ape, String correo, String pass, NombreRol rol) {
        u.setDocumentoIdentidad(doc);
        u.setNombres(nom);
        u.setApellidos(ape);
        u.setCorreo(correo);
        u.setPasswordHash(encoder.encode(pass));
        u.setRol(roles.findByNombreRol(rol).orElseThrow());
        u.setEstado(true);
        u.setConsentimientoDatos(true);
        u.setFechaConsentimiento(LocalDateTime.now());
        return u;
    }

    private Administrativo administrativo(String doc, String nom, String ape, String correo, String pass, Cargo cargo) {
        return (Administrativo) usuarios.findByCorreoIgnoreCase(correo).orElseGet(() -> {
            Administrativo a = base(new Administrativo(), doc, nom, ape, correo, pass, cargo.rol());
            a.setCargo(cargo);
            return usuarios.save(a);
        });
    }

    private Medico medico(String doc, String nom, String ape, String correo, String colegiatura, Especialidad e) {
        return (Medico) usuarios.findByCorreoIgnoreCase(correo).orElseGet(() -> {
            Medico m = base(new Medico(), doc, nom, ape, correo, PASS_GENERAL, NombreRol.ROLE_MEDICO);
            m.setNumeroColegiatura(colegiatura);
            m.setEspecialidad(e);
            return usuarios.save(m);
        });
    }

    private Tutor tutor(String doc, String nom, String ape, String correo, String parentesco) {
        return (Tutor) usuarios.findByCorreoIgnoreCase(correo).orElseGet(() -> {
            Tutor t = base(new Tutor(), doc, nom, ape, correo, PASS_GENERAL, NombreRol.ROLE_TUTOR);
            t.setParentesco(parentesco);
            t.setTelefono("987654321");
            t.setDireccion("Av. Mendiburu 100, Ate, Lima (referencial)");
            return usuarios.save(t);
        });
    }

    private void paciente(String doc, String nom, String ape, String correo, LocalDate nac, Tutor tutor) {
        if (usuarios.findByCorreoIgnoreCase(correo).isPresent()) return;
        Paciente p = base(new Paciente(), doc, nom, ape, correo, PASS_GENERAL, NombreRol.ROLE_PACIENTE);
        p.setFechaNacimiento(nac);
        p.setTipoSeguro("SIS");
        p.setTelefonoContacto("912345678");
        p.setTutor(tutor);
        p.setRequiereTutor(tutor != null);
        usuarios.save(p);
    }

    private void medicamento(String nombre, String activo, String pres, int stock, int minimo, int diasVence, String precio) {
        Medicamento m = new Medicamento();
        m.setNombre(nombre);
        m.setPrincipioActivo(activo);
        m.setPresentacion(pres);
        m.setStockActual(stock);
        m.setStockMinimo(minimo);
        m.setFechaVencimiento(LocalDate.now().plusDays(diasVence));
        m.setPrecio(new BigDecimal(precio));
        medicamentos.save(m);
        MovimientoInventario mv = new MovimientoInventario();
        mv.setMedicamento(m);
        mv.setTipo(Enums.TipoMovimiento.ENTRADA);
        mv.setCantidad(stock);
        mv.setFecha(LocalDateTime.now());
        mv.setMotivo("Stock inicial (datos semilla)");
        mv.setUsuario(usuarios.findByCorreoIgnoreCase("farmacia@valdizan.gob.pe").orElseThrow());
        movimientos.save(mv);
    }
}
