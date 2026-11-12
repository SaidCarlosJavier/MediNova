import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Modal } from 'react-bootstrap';
import { api, msg } from '../api';
import { Cargando, EstadoBadge, Paginador, Vacio, fmtFecha, fmtHora, useCarga, useToast } from './ui';
import PagoModal, { money } from './Pago';

/** Selección de fecha, turno y horario para una especialidad (usado en el wizard y al reprogramar). */
export function SelectorHorario({ especialidadId, seleccionado, onElegir, onSinHorarios }) {
  const [fechas, setFechas] = useState(null);
  const [fecha, setFecha] = useState('');
  const [turno, setTurno] = useState('');
  const [horarios, setHorarios] = useState(null);

  useEffect(() => {
    setFechas(null); setFecha(''); setHorarios(null);
    api.get('/horarios/fechas', { params: { especialidadId } }).then((r) => {
      setFechas(r.data);
      if (r.data.length === 0 && onSinHorarios) onSinHorarios();
    }).catch(() => setFechas([]));
  }, [especialidadId]); // eslint-disable-line

  useEffect(() => {
    if (!fecha) return;
    setHorarios(null);
    api.get('/horarios/disponibles', { params: { especialidadId, fecha, ...(turno ? { turno } : {}) } })
      .then((r) => setHorarios(r.data)).catch(() => setHorarios([]));
  }, [fecha, turno, especialidadId]);

  if (!fechas) return <Cargando texto="Buscando fechas disponibles…" />;
  if (fechas.length === 0) return <Alert variant="warning">No hay horarios disponibles por el momento para esta especialidad.</Alert>;

  return (
    <>
      <h2 className="h6 fw-bold">Día</h2>
      <div className="mb-3">
        {fechas.map((f) => (
          <button type="button" key={f} className={`chip ${fecha === f ? 'sel' : ''}`} onClick={() => setFecha(f)}>{fmtFecha(f)}</button>
        ))}
      </div>
      <h2 className="h6 fw-bold">Turno</h2>
      <div className="mb-3">
        {[['', 'Todos'], ['MANANA', 'Mañana'], ['TARDE', 'Tarde']].map(([v, t]) => (
          <button type="button" key={v} className={`chip ${turno === v ? 'sel' : ''}`} onClick={() => setTurno(v)}>{t}</button>
        ))}
      </div>
      {fecha && (
        <>
          <h2 className="h6 fw-bold">Horarios disponibles</h2>
          {!horarios ? <Cargando /> : horarios.length === 0 ? (
            <Alert variant="info">No quedan horarios para ese día y turno. Pruebe con otro.</Alert>
          ) : (
            <div className="row g-2">
              {horarios.map((h) => (
                <div className="col-sm-6 col-lg-4" key={h.idHorario}>
                  <button type="button" className={`slot ${seleccionado?.idHorario === h.idHorario ? 'sel' : ''}`} onClick={() => onElegir(h)}>
                    <div className="fs-5 fw-bold">{fmtHora(h.horaInicio)} – {fmtHora(h.horaFin)}</div>
                    <div className="small">{h.medico}</div>
                    <div className="small text-muted"><i className="bi bi-geo-alt me-1" />Pabellón {h.pabellon}, Room {h.room}</div>
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </>
  );
}

/** Wizard de reserva en 4 pasos (RNF-19): especialidad → día/turno/horario → confirmar → listo. */
export function ReservaWizard({ pacienteId, pacienteNombre, origen = 'WEB', onTerminar }) {
  const toast = useToast();
  const inicio = useRef(Date.now());
  const [paso, setPaso] = useState(1);
  const [especialidades, , ] = useCarga(() => api.get('/especialidades').then((r) => r.data), []);
  const [esp, setEsp] = useState(null);
  const [horario, setHorario] = useState(null);
  const [cita, setCita] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const [sinHorarios, setSinHorarios] = useState(false);
  const [enEspera, setEnEspera] = useState(false);

  useEffect(() => { inicio.current = Date.now(); setPaso(1); setEsp(null); setHorario(null); setCita(null); setError(''); }, [pacienteId]);

  if (!pacienteId) return <Alert variant="warning">Seleccione o registre un paciente para reservar una cita.</Alert>;

  const confirmar = async () => {
    setEnviando(true); setError('');
    try {
      const { data } = await api.post('/citas', {
        pacienteId, horarioId: horario.idHorario, origen,
        duracionReservaSeg: Math.round((Date.now() - inicio.current) / 1000),
      });
      setCita(data); setPaso(4); toast('¡Cita reservada y confirmada!');
    } catch (e) {
      setError(msg(e));
      if (e.response?.status === 409) { setHorario(null); setPaso(2); }
    } finally { setEnviando(false); }
  };

  const anotarEspera = async () => {
    try {
      await api.post('/espera', { pacienteId, especialidadId: esp.id });
      setEnEspera(true); toast('Quedó registrado en la lista de espera. Le avisaremos cuando se libere un turno.');
    } catch (e) { toast(msg(e), 'error'); }
  };

  const nuevo = () => { inicio.current = Date.now(); setPaso(1); setEsp(null); setHorario(null); setCita(null); setSinHorarios(false); setEnEspera(false); onTerminar?.(); };

  return (
    <div>
      <div className="pasos" aria-label="Progreso de la reserva">
        {['Especialidad', 'Día y horario', 'Confirmar', 'Listo'].map((t, i) => (
          <div key={t} className={`paso ${paso >= i + 1 ? 'act' : ''}`}>{i + 1}. {t}</div>
        ))}
      </div>
      {pacienteNombre && <p className="text-muted"><i className="bi bi-person-check me-1" />Reservando para: <strong>{pacienteNombre}</strong></p>}
      {error && <Alert variant="danger" role="alert">{error}</Alert>}

      {paso === 1 && (
        !especialidades ? <Cargando /> : (
          <div className="row g-3">
            {especialidades.map((e) => (
              <div className="col-md-6" key={e.id}>
                <button type="button" className="slot h-100" onClick={() => { setEsp(e); setSinHorarios(false); setEnEspera(false); setPaso(2); }}>
                  <div className="fs-4 mb-1"><i className={`bi ${e.nombre.startsWith('Psiq') ? 'bi-activity' : 'bi-chat-heart'} me-2 text-primary`} />{e.nombre}</div>
                  <div className="text-muted small">{e.descripcion}</div>
                </button>
              </div>
            ))}
          </div>
        )
      )}

      {paso === 2 && esp && (
        <>
          <h2 className="h5 mb-3">{esp.nombre}</h2>
          <SelectorHorario especialidadId={esp.id} seleccionado={horario} onElegir={setHorario} onSinHorarios={() => setSinHorarios(true)} />
          {sinHorarios && (
            <div className="mt-3">
              {enEspera ? <Alert variant="success">Está en la lista de espera de {esp.nombre}.</Alert>
                : <Button variant="outline-primary" size="lg" onClick={anotarEspera}><i className="bi bi-hourglass-split me-2" />Anotarme en la lista de espera</Button>}
            </div>
          )}
          <div className="d-flex justify-content-between mt-4">
            <Button variant="outline-secondary" size="lg" onClick={() => setPaso(1)}>Atrás</Button>
            <Button className="btn-azul" size="lg" disabled={!horario} onClick={() => setPaso(3)}>Continuar</Button>
          </div>
        </>
      )}

      {paso === 3 && horario && (
        <>
          <div className="caja mb-3">
            <h2 className="h5">Revise los datos de su cita</h2>
            <dl className="row mb-0 fs-6">
              <dt className="col-sm-4">Paciente</dt><dd className="col-sm-8">{pacienteNombre}</dd>
              <dt className="col-sm-4">Especialidad</dt><dd className="col-sm-8">{horario.especialidad}</dd>
              <dt className="col-sm-4">Médico</dt><dd className="col-sm-8">{horario.medico}</dd>
              <dt className="col-sm-4">Fecha</dt><dd className="col-sm-8">{fmtFecha(horario.fecha)}</dd>
              <dt className="col-sm-4">Hora</dt><dd className="col-sm-8">{fmtHora(horario.horaInicio)} – {fmtHora(horario.horaFin)}</dd>
              <dt className="col-sm-4">Ubicación</dt><dd className="col-sm-8">Pabellón {horario.pabellon}, Room {horario.room}</dd>
            </dl>
          </div>
          <div className="d-flex justify-content-between">
            <Button variant="outline-secondary" size="lg" onClick={() => setPaso(2)}>Atrás</Button>
            <Button className="btn-azul" size="lg" disabled={enviando} onClick={confirmar}>{enviando ? 'Reservando…' : 'Confirmar cita'}</Button>
          </div>
        </>
      )}

      {paso === 4 && cita && (
        <div className="text-center py-3">
          <i className="bi bi-check-circle-fill text-success" style={{ fontSize: '4rem' }} />
          <h2 className="h4 mt-2">¡Cita confirmada!</h2>
          <p className="fs-5 mb-1">{cita.especialidad} con {cita.medico}</p>
          <p className="fs-5 mb-1">{fmtFecha(cita.fecha)} a las {fmtHora(cita.horaInicio)}</p>
          <p className="fs-5"><i className="bi bi-geo-alt-fill text-primary me-1" />{cita.ubicacion}</p>
          <p className="text-muted">Le enviamos una notificación de confirmación{origen !== 'PRESENCIAL' ? ' y un recordatorio 24 horas antes' : ''}.</p>
          <Button className="btn-azul" size="lg" onClick={nuevo}>Reservar otra cita</Button>
        </div>
      )}
    </div>
  );
}

/** Citas de un paciente con reprogramar / cancelar / ver ubicación (RF-08, 09, 10, 14, 21, 22). */
/** Descarga el reporte PDF de una cita (con el detalle clínico si el rol tiene permiso). */
export async function descargarReporteCita(cita, toast) {
  try {
    let h = null;
    try { h = (await api.get(`/historial/cita/${cita.idCita}`)).data || null; } catch { /* sin permiso clínico (p. ej. recepción) */ }
    const { reporteCita } = await import('../pdf');
    await reporteCita(cita, h);
  } catch (e) { toast('No se pudo generar el PDF', 'error'); }
}

export function CitasPaciente({ pacienteId, recarga = 0, ventanilla = false }) {
  const toast = useToast();
  const [pagina, setPagina] = useState(0);
  const [datos, cargando, recargar] = useCarga(
    () => api.get(`/citas/paciente/${pacienteId}`, { params: { pagina, tamanio: 8 } }).then((r) => r.data),
    [pacienteId, pagina, recarga]
  );
  const [aCancelar, setACancelar] = useState(null);
  const [aReprogramar, setAReprogramar] = useState(null);
  const [nuevoHorario, setNuevoHorario] = useState(null);
  const [ocupado, setOcupado] = useState(false);
  const [aPagar, setAPagar] = useState(null);

  const pagar = async (body) => {
    const { data } = await api.post(`/pagos/cita/${aPagar.idCita}`, body);
    toast(`Pago registrado. Comprobante ${data.comprobante}`);
    import('../pdf').then((m) => m.boletaConsulta(data)).catch(() => {});
    recargar();
  };

  const cancelar = async () => {
    setOcupado(true);
    try { await api.patch(`/citas/${aCancelar.idCita}/cancelar`); toast('Cita cancelada. El turno fue liberado.'); setACancelar(null); recargar(); }
    catch (e) { toast(msg(e), 'error'); } finally { setOcupado(false); }
  };

  const reprogramar = async () => {
    setOcupado(true);
    try {
      await api.put(`/citas/${aReprogramar.idCita}/reprogramar`, { horarioId: nuevoHorario.idHorario });
      toast('Cita reprogramada correctamente.'); setAReprogramar(null); setNuevoHorario(null); recargar();
    } catch (e) { toast(msg(e), 'error'); } finally { setOcupado(false); }
  };

  if (cargando && !datos) return <Cargando />;
  if (!datos || datos.contenido.length === 0) return <Vacio icono="calendar-x">Aún no hay citas registradas.</Vacio>;

  return (
    <>
      <div className="table-responsive">
        <table className="table table-hover align-middle">
          <thead><tr><th>Fecha y hora</th><th>Especialidad</th><th>Médico</th><th>Ubicación</th><th>Estado</th><th>Pago</th><th /></tr></thead>
          <tbody>
            {datos.contenido.map((c) => (
              <tr key={c.idCita}>
                <td><strong>{fmtFecha(c.fecha)}</strong><br />{fmtHora(c.horaInicio)}</td>
                <td>{c.especialidad}</td>
                <td>{c.medico}</td>
                <td><i className="bi bi-geo-alt text-primary me-1" />Pabellón {c.pabellon}<br />Room {c.room}</td>
                <td><EstadoBadge estado={c.estado} /></td>
                <td>
                  {Number(c.precio) <= 0 ? <span className="text-muted small">Sin costo</span>
                    : c.pagada ? <span className="badge bg-success">Pagada</span>
                    : c.estado === 'CANCELADA' || c.estado === 'NO_ASISTIO' ? <span className="text-muted small">—</span>
                    : <Button size="sm" className="btn-azul" onClick={() => setAPagar(c)}>Pagar {money(c.precio)}</Button>}
                </td>
                <td className="text-end text-nowrap">
                  <Button size="sm" variant="outline-secondary" className="me-2" title="Descargar reporte de la cita en PDF" onClick={() => descargarReporteCita(c, toast)}><i className="bi bi-file-earmark-pdf" /> PDF</Button>
                  {c.modificable && (
                    <>
                      <Button size="sm" variant="outline-primary" className="me-2" onClick={() => setAReprogramar(c)}>Reprogramar</Button>
                      <Button size="sm" variant="outline-danger" onClick={() => setACancelar(c)}>Cancelar</Button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginador pagina={datos.pagina} totalPaginas={datos.totalPaginas} onCambio={setPagina} />

      <PagoModal show={!!aPagar} onHide={() => setAPagar(null)} monto={aPagar?.precio} permitirEfectivo={ventanilla} titulo="Pagar consulta"
        concepto={aPagar ? `Consulta de ${aPagar.especialidad} - ${fmtFecha(aPagar.fecha)}` : ''} onPagar={pagar} />

      <Modal show={!!aCancelar} onHide={() => setACancelar(null)} centered>
        <Modal.Header closeButton><Modal.Title>Cancelar cita</Modal.Title></Modal.Header>
        <Modal.Body>
          {aCancelar && <p className="fs-6">¿Desea cancelar la cita de <strong>{aCancelar.especialidad}</strong> del <strong>{fmtFecha(aCancelar.fecha)}</strong> a las <strong>{fmtHora(aCancelar.horaInicio)}</strong>? El turno quedará libre de inmediato{aCancelar.pagada ? ' y el pago será reembolsado' : ''}.</p>}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => setACancelar(null)}>Volver</Button>
          <Button variant="danger" disabled={ocupado} onClick={cancelar}>Sí, cancelar cita</Button>
        </Modal.Footer>
      </Modal>

      <Modal show={!!aReprogramar} onHide={() => { setAReprogramar(null); setNuevoHorario(null); }} size="lg" centered>
        <Modal.Header closeButton><Modal.Title>Reprogramar cita</Modal.Title></Modal.Header>
        <Modal.Body>
          {aReprogramar && (
            <>
              <p className="text-muted">Elija un nuevo horario de {aReprogramar.especialidad} (sujeto a disponibilidad).</p>
              <SelectorHorario especialidadId={aReprogramar.especialidadId} seleccionado={nuevoHorario} onElegir={setNuevoHorario} />
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => { setAReprogramar(null); setNuevoHorario(null); }}>Volver</Button>
          <Button className="btn-azul" disabled={!nuevoHorario || ocupado} onClick={reprogramar}>Confirmar nuevo horario</Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
