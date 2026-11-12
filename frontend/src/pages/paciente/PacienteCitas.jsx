import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Alert, Col, Row } from 'react-bootstrap';
import { useAuth } from '../../AuthContext';
import { api } from '../../api';
import { Cargando, Titulo, fmtFecha, fmtHora, useCarga } from '../../components/ui';
import { CitasPaciente, ReservaWizard } from '../../components/Reserva';

/** Aviso cuando el tutor aún no tiene paciente seleccionado. */
export function SinPaciente() {
  return (
    <Alert variant="warning" className="fs-6">
      <i className="bi bi-info-circle me-2" />
      Aún no tiene pacientes a su cargo. <Link to="/panel/pacientes" className="fw-semibold">Registre o vincule un paciente</Link> para gestionar sus citas.
    </Alert>
  );
}

export function PanelInicio() {
  const { usuario, pacienteActivo } = useAuth();
  const [proximas, cargando] = useCarga(
    () => (pacienteActivo ? api.get(`/citas/paciente/${pacienteActivo.id}`, { params: { tamanio: 20 } }).then((r) => r.data.contenido.filter((c) => c.modificable).reverse()) : Promise.resolve([])),
    [pacienteActivo?.id]
  );
  const esTutor = usuario.rol === 'ROLE_TUTOR';

  return (
    <>
      <Titulo icono="house">Hola, {usuario.nombres}</Titulo>
      {esTutor && !pacienteActivo && <SinPaciente />}
      {pacienteActivo && (
        <p className="text-muted">
          {esTutor ? <>Está gestionando la atención de <strong>{pacienteActivo.nombreCompleto}</strong>.</> : 'Aquí puede gestionar su atención.'}
        </p>
      )}
      <Row className="g-3 mb-4">
        {[
          ['/panel/reservar', 'calendar-plus', 'Reservar cita', 'En pocos pasos'],
          ['/panel/citas', 'calendar-check', 'Mis citas', 'Reprogramar o cancelar'],
          ['/panel/pagos', 'credit-card', 'Mis pagos', 'Boletas en PDF'],
          ['/panel/historial', 'journal-medical', 'Historial', 'Atenciones y recetas'],
          ['/panel/ubicacion', 'geo-alt', 'Ubicación', 'Pabellón y room'],
        ].map(([to, ic, t, s]) => (
          <Col sm={6} lg={3} xl key={to}>
            <Link to={to} className="text-decoration-none">
              <div className="caja stat h-100"><i className={`bi bi-${ic} fs-1 text-primary`} /><div className="fw-bold mt-1">{t}</div><div className="small text-muted">{s}</div></div>
            </Link>
          </Col>
        ))}
      </Row>
      {pacienteActivo && (
        <div className="caja">
          <h2 className="h5">Próximas citas</h2>
          {cargando ? <Cargando /> : !proximas?.length ? <p className="text-muted mb-0">No tiene citas vigentes.</p> : (
            <ul className="list-group list-group-flush">
              {proximas.slice(0, 4).map((c) => (
                <li className="list-group-item px-0" key={c.idCita}>
                  <strong>{fmtFecha(c.fecha)} · {fmtHora(c.horaInicio)}</strong> — {c.especialidad} con {c.medico}
                  <div className="small text-muted"><i className="bi bi-geo-alt me-1" />{c.ubicacion}</div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </>
  );
}

export function MisCitas() {
  const { pacienteActivo, usuario } = useAuth();
  return (
    <>
      <Titulo icono="calendar-check" acciones={<Link to="/panel/reservar" className="btn btn-azul">Nueva cita</Link>}>Mis citas</Titulo>
      {usuario.rol === 'ROLE_TUTOR' && pacienteActivo && <p className="text-muted">Paciente: <strong>{pacienteActivo.nombreCompleto}</strong></p>}
      {!pacienteActivo ? <SinPaciente /> : <div className="caja"><CitasPaciente pacienteId={pacienteActivo.id} /></div>}
    </>
  );
}

export function Reservar() {
  const { pacienteActivo } = useAuth();
  const [, setN] = useState(0);
  return (
    <>
      <Titulo icono="calendar-plus">Reservar cita</Titulo>
      {!pacienteActivo ? <SinPaciente /> : (
        <div className="caja">
          <ReservaWizard pacienteId={pacienteActivo.id} pacienteNombre={pacienteActivo.nombreCompleto} origen="WEB" onTerminar={() => setN((n) => n + 1)} />
        </div>
      )}
    </>
  );
}

/** RF-14: ubicación del consultorio de las próximas citas. */
export function Ubicacion() {
  const { pacienteActivo } = useAuth();
  const [citas, cargando] = useCarga(
    () => (pacienteActivo ? api.get(`/citas/paciente/${pacienteActivo.id}`, { params: { tamanio: 20 } }).then((r) => r.data.contenido.filter((c) => c.modificable).reverse()) : Promise.resolve([])),
    [pacienteActivo?.id]
  );
  return (
    <>
      <Titulo icono="geo-alt">Ubicación de mi consultorio</Titulo>
      {!pacienteActivo ? <SinPaciente /> : cargando ? <Cargando /> : !citas?.length ? (
        <div className="caja text-muted">No tiene citas vigentes. Cuando reserve una, aquí verá el pabellón y el room.</div>
      ) : (
        <Row className="g-3">
          {citas.map((c) => (
            <Col md={6} key={c.idCita}>
              <div className="caja">
                <div className="text-muted small">{fmtFecha(c.fecha)} · {fmtHora(c.horaInicio)}</div>
                <h2 className="h5">{c.especialidad} — {c.medico}</h2>
                <div className="d-flex gap-3 mt-3">
                  <div className="stat caja flex-fill"><div className="small text-muted">Pabellón</div><div className="num">{c.pabellon}</div></div>
                  <div className="stat caja flex-fill"><div className="small text-muted">Room</div><div className="num">{c.room}</div></div>
                </div>
                <p className="small text-muted mt-3 mb-0">Hospital Hermilio Valdizán — Av. Mendiburu, Ate / Santa Eugenia (referencial). Presente su documento de identidad.</p>
              </div>
            </Col>
          ))}
        </Row>
      )}
    </>
  );
}
