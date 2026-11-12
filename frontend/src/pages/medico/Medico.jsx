import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Col, Dropdown, Modal, Row } from 'react-bootstrap';
import { api, fieldErrors, msg } from '../../api';
import { Campo, Cargando, EstadoBadge, Titulo, Vacio, fmtFecha, fmtHora, hoyISO, sumarDias, useCarga, useToast } from '../../components/ui';
import { SelectorHorario, descargarReporteCita } from '../../components/Reserva';
import { Disponibilidad } from '../recepcion/EsperaDisponibilidad';
import { useAuth } from '../../AuthContext';
import { HistorialPaciente } from '../paciente/PacienteExtra';

/** Agenda del médico: tarjetas de resumen, rangos (día/semana/mes), búsqueda, filtros y acciones libres sobre cada cita. */
export function AgendaMedico() {
  const toast = useToast();
  const [rango, setRango] = useState('dia');
  const [fecha, setFecha] = useState(hoyISO());
  const [estado, setEstado] = useState('');
  const [q, setQ] = useState('');
  const [resumen] = useCarga(() => api.get('/medico/resumen').then((r) => r.data), []);
  const lapso = () => {
    if (rango === 'semana') return { desde: fecha, hasta: sumarDias(fecha, 6) };
    if (rango === 'mes') return { desde: fecha, hasta: sumarDias(fecha, 29) };
    return { fecha };
  };
  const [citas, cargando, recargarCitas] = useCarga(() => api.get('/citas/agenda', { params: lapso() }).then((r) => r.data), [fecha, rango]);
  const recargar = () => recargarCitas();
  const [aAccion, setAAccion] = useState(null); // { cita, tipo: 'cancelar' | 'noasistio' }
  const visibles = (citas || []).filter((c) => (!estado || c.estado === estado)
    && (!q.trim() || `${c.paciente} ${c.pacienteDocumento}`.toLowerCase().includes(q.trim().toLowerCase())));

  const confirmarAccion = async () => {
    try {
      if (aAccion.tipo === 'cancelar') await api.patch(`/citas/${aAccion.cita.idCita}/cancelar`);
      else await api.patch(`/citas/${aAccion.cita.idCita}/no-asistio`);
      toast(aAccion.tipo === 'cancelar' ? 'Cita cancelada' : 'Cita marcada como no asistió'); setAAccion(null); recargar();
    } catch (e) { toast(msg(e), 'error'); }
  };
  const exportar = async () => {
    try {
      const m = await import('../../pdf');
      m.descargarCSV(`agenda-${fecha}.csv`, ['Fecha', 'Hora', 'Paciente', 'Documento', 'Consultorio', 'Estado'],
        visibles.map((c) => [c.fecha, fmtHora(c.horaInicio), c.paciente, c.pacienteDocumento, `Pab. ${c.pabellon} Room ${c.room}`, c.estado]));
    } catch { toast('No se pudo exportar', 'error'); }
  };

  const [triaje, setTriaje] = useState(null);
  const [t, setT] = useState({ presionArterial: '', peso: '', signosVitales: '' });
  const [errores, setErrores] = useState({});

  const abrirTriaje = async (c) => {
    setTriaje(c); setErrores({});
    try { const { data } = await api.get(`/citas/${c.idCita}/triaje`); setT({ presionArterial: data?.presionArterial || '', peso: data?.peso ?? '', signosVitales: data?.signosVitales || '' }); }
    catch { setT({ presionArterial: '', peso: '', signosVitales: '' }); }
  };
  const guardarTriaje = async () => {
    try {
      await api.put(`/citas/${triaje.idCita}/triaje`, { ...t, peso: t.peso === '' ? null : Number(t.peso) });
      toast('Triaje registrado'); setTriaje(null);
    } catch (e) { setErrores(fieldErrors(e)); toast(msg(e), 'error'); }
  };

  return (
    <>
      <Titulo icono="calendar-day" acciones={<Button variant="outline-secondary" disabled={!visibles.length} onClick={exportar}><i className="bi bi-filetype-csv me-1" />Exportar CSV</Button>}>Mi agenda</Titulo>
      {resumen && (
        <Row className="g-3 mb-3">
          {[['Citas hoy', resumen.citasHoy, 'calendar-day'], ['Atendidas hoy', resumen.atendidasHoy, 'check-circle'], ['Pendientes hoy', resumen.pendientesHoy, 'hourglass-split'], ['Citas esta semana', resumen.citasSemana, 'calendar-week'], ['Mis pacientes', resumen.pacientesTotales, 'people']].map(([t, v, ic]) => (
            <Col xs={6} md key={t}><div className="caja stat h-100"><i className={`bi bi-${ic} fs-4 text-primary`} /><div className="num">{v}</div><div className="small text-muted">{t}</div></div></Col>))}
        </Row>
      )}
      <div className="caja mb-3"><div className="row g-3 align-items-end">
        <div className="col-md-3"><label className="form-label d-block">Vista</label>
          <div className="btn-group w-100" role="group" aria-label="Rango">{[['dia', 'Día'], ['semana', '7 días'], ['mes', '30 días']].map(([k, t]) => <button key={k} type="button" className={`btn ${rango === k ? 'btn-azul' : 'btn-outline-azul'}`} onClick={() => setRango(k)}>{t}</button>)}</div></div>
        <div className="col-6 col-md-2"><label htmlFor="fa" className="form-label">{rango === 'dia' ? 'Fecha' : 'Desde'}</label><input id="fa" type="date" className="form-control" value={fecha} onChange={(e) => setFecha(e.target.value)} /></div>
        <div className="col-6 col-md-3"><label htmlFor="fe" className="form-label">Estado</label><select id="fe" className="form-select" value={estado} onChange={(e) => setEstado(e.target.value)}><option value="">Todos</option>{['PROGRAMADA', 'CONFIRMADA', 'REPROGRAMADA', 'ATENDIDA', 'CANCELADA', 'NO_ASISTIO'].map((x) => <option key={x} value={x}>{x.replace('_', ' ')}</option>)}</select></div>
        <div className="col-md-4"><label htmlFor="fq" className="form-label">Buscar paciente</label><input id="fq" className="form-control" placeholder="Nombre o documento" value={q} onChange={(e) => setQ(e.target.value)} /></div>
      </div></div>
      <div className="caja">
        {cargando && !citas ? <Cargando /> : !visibles.length ? <Vacio icono="calendar-x">No hay citas para mostrar con esos filtros.</Vacio> : (
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr>{rango !== 'dia' && <th>Fecha</th>}<th>Hora</th><th>Paciente</th><th>Consultorio</th><th>Estado</th><th /></tr></thead>
            <tbody>{visibles.map((c) => {
              const cerrada = c.estado === 'CANCELADA' || c.estado === 'NO_ASISTIO';
              return (
                <tr key={c.idCita}>
                  {rango !== 'dia' && <td className="text-nowrap">{fmtFecha(c.fecha)}</td>}
                  <td className="fw-bold">{fmtHora(c.horaInicio)}</td><td>{c.paciente}<div className="small text-muted">Doc. {c.pacienteDocumento}</div></td>
                  <td>Pab. {c.pabellon} · Room {c.room}</td><td><EstadoBadge estado={c.estado} />{c.checkIn && <div className="small text-success mt-1"><i className="bi bi-check2" /> Llegó</div>}</td>
                  <td className="text-end text-nowrap">
                    {!cerrada && (<>
                      <Button size="sm" variant="outline-secondary" className="me-2" onClick={() => abrirTriaje(c)}>Triaje</Button>
                      <Link to={`/medico/atencion/${c.idCita}`} className="btn btn-sm btn-azul me-2">{c.estado === 'ATENDIDA' ? 'Ver atención' : 'Atender'}</Link>
                    </>)}
                    <Link to={`/medico/paciente/${c.pacienteId}`} className="btn btn-sm btn-outline-primary me-2">Historial</Link>
                    <Dropdown align="end" className="d-inline-block">
                      <Dropdown.Toggle size="sm" variant="outline-secondary" aria-label="Más acciones"><i className="bi bi-three-dots" /></Dropdown.Toggle>
                      <Dropdown.Menu>
                        <Dropdown.Item onClick={() => descargarReporteCita(c, toast)}><i className="bi bi-file-earmark-pdf me-2" />Reporte de la cita (PDF)</Dropdown.Item>
                        {c.estado !== 'ATENDIDA' && !cerrada && <Dropdown.Item onClick={() => setAAccion({ cita: c, tipo: 'noasistio' })}><i className="bi bi-person-x me-2" />Marcar «no asistió»</Dropdown.Item>}
                        {c.modificable && <Dropdown.Item className="text-danger" onClick={() => setAAccion({ cita: c, tipo: 'cancelar' })}><i className="bi bi-x-circle me-2" />Cancelar cita</Dropdown.Item>}
                      </Dropdown.Menu>
                    </Dropdown>
                  </td>
                </tr>);
            })}</tbody>
          </table></div>
        )}
      </div>
      <Modal show={!!aAccion} onHide={() => setAAccion(null)} centered>
        <Modal.Header closeButton><Modal.Title>{aAccion?.tipo === 'cancelar' ? 'Cancelar cita' : 'Marcar como «no asistió»'}</Modal.Title></Modal.Header>
        <Modal.Body>
          <p>{aAccion?.cita.paciente} · {fmtFecha(aAccion?.cita.fecha)} {fmtHora(aAccion?.cita.horaInicio)}</p>
          {aAccion?.tipo === 'cancelar' && <p className="small text-muted mb-0">Se avisará al paciente, el turno quedará libre y, si la consulta estaba pagada, se reembolsará automáticamente.</p>}
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setAAccion(null)}>Volver</Button><Button variant={aAccion?.tipo === 'cancelar' ? 'danger' : 'warning'} onClick={confirmarAccion}>Confirmar</Button></Modal.Footer>
      </Modal>
      <Modal show={!!triaje} onHide={() => setTriaje(null)} centered>
        <Modal.Header closeButton><Modal.Title>Triaje — {triaje?.paciente}</Modal.Title></Modal.Header>
        <Modal.Body>
          <Campo id="pa" etiqueta="Presión arterial (ej. 120/80)" error={errores.presionArterial}><input id="pa" className="form-control" value={t.presionArterial} onChange={(e) => setT({ ...t, presionArterial: e.target.value })} /></Campo>
          <Campo id="pe" etiqueta="Peso (kg)" error={errores.peso}><input id="pe" type="number" step="0.1" min="0" className="form-control" value={t.peso} onChange={(e) => setT({ ...t, peso: e.target.value })} /></Campo>
          <Campo id="sv" etiqueta="Otros signos vitales" error={errores.signosVitales}><textarea id="sv" rows={2} className="form-control" value={t.signosVitales} onChange={(e) => setT({ ...t, signosVitales: e.target.value })} /></Campo>
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setTriaje(null)}>Cancelar</Button><Button className="btn-azul" onClick={guardarTriaje}>Guardar</Button></Modal.Footer>
      </Modal>
    </>
  );
}

/** RF-29 tratamiento, receta (solo medicamentos activos del catálogo) y RF-30 marcar como atendida. */
export function Atencion() {
  const { citaId } = useParams();
  const nav = useNavigate();
  const toast = useToast();
  const [cita, cargando] = useCarga(() => api.get(`/citas/${citaId}`).then((r) => r.data), [citaId]);
  const [catalogo] = useCarga(() => api.get('/farmacia/catalogo').then((r) => r.data), []);
  const [f, setF] = useState({ diagnostico: '', tratamiento: '', observaciones: '', indicaciones: '' });
  const [items, setItems] = useState([]);
  const [recetaBloqueada, setRecetaBloqueada] = useState(false);
  const [errores, setErrores] = useState({});
  const [ocupado, setOcupado] = useState(false);
  const toast2 = toast;
  const [seg, setSeg] = useState(false);
  const [horario, setHorario] = useState(null);

  const agendarSeguimiento = async () => {
    try {
      await api.post('/citas', { pacienteId: cita.pacienteId, horarioId: horario.idHorario, origen: 'PRESENCIAL' });
      toast2('Cita de seguimiento agendada'); setSeg(false); setHorario(null);
    } catch (e) { toast2(msg(e), 'error'); }
  };

  useEffect(() => {
    api.get(`/historial/cita/${citaId}`).then((r) => {
      const h = r.data;
      if (h) {
        setF({ diagnostico: h.diagnostico || '', tratamiento: h.tratamiento || '', observaciones: h.observaciones || '', indicaciones: h.receta?.indicaciones || '' });
        setItems((h.receta?.detalles || []).map((d) => ({ medicamentoId: d.medicamentoId, dosis: d.dosis, cantidad: d.cantidad })));
        setRecetaBloqueada(!!h.receta && h.receta.estado !== 'PENDIENTE');
      }
    }).catch(() => {});
  }, [citaId]);

  const setItem = (i, k, v) => setItems(items.map((x, j) => (j === i ? { ...x, [k]: v } : x)));

  const guardar = async (atendida) => {
    const errs = {};
    if (!f.diagnostico.trim()) errs.diagnostico = 'Ingrese el diagnóstico';
    items.forEach((it, i) => {
      if (!it.medicamentoId) errs[`d${i}`] = 'Seleccione el medicamento';
      else if (!it.dosis?.trim()) errs[`d${i}`] = 'Ingrese la dosis';
      else if (!(Number(it.cantidad) >= 1)) errs[`d${i}`] = 'La cantidad mínima es 1';
    });
    setErrores(errs);
    if (Object.keys(errs).length) return;
    setOcupado(true);
    try {
      await api.put(`/historial/cita/${citaId}`, {
        ...f, detalles: recetaBloqueada ? [] : items.map((it) => ({ medicamentoId: Number(it.medicamentoId), dosis: it.dosis, cantidad: Number(it.cantidad) })),
      });
      if (atendida) { await api.patch(`/citas/${citaId}/atendida`); toast('Atención registrada y cita marcada como atendida'); nav('/medico'); }
      else toast('Historial y receta guardados');
    } catch (e) { setErrores({ ...fieldErrors(e) }); toast(msg(e), 'error'); } finally { setOcupado(false); }
  };

  if (cargando && !cita) return <Cargando />;
  if (!cita) return <Alert variant="danger">No se pudo cargar la cita.</Alert>;
  const cerrada = cita.estado === 'ATENDIDA';

  return (
    <>
      <Titulo icono="clipboard2-pulse" acciones={<><Button variant="outline-secondary" className="me-2" onClick={() => descargarReporteCita(cita, toast)}><i className="bi bi-file-earmark-pdf me-1" />Reporte PDF</Button><Link to={`/medico/paciente/${cita.pacienteId}`} className="btn btn-outline-primary">Ver historial previo</Link></>}>Atención médica</Titulo>
      <div className="caja mb-3">
        <strong>{cita.paciente}</strong> · Doc. {cita.pacienteDocumento}<br />
        <span className="text-muted">{cita.especialidad} · {fmtFecha(cita.fecha)} {fmtHora(cita.horaInicio)} · <EstadoBadge estado={cita.estado} /></span>
      </div>
      <div className="caja mb-3">
        <h2 className="h5">Historial clínico</h2>
        <Campo id="dx" etiqueta="Diagnóstico" error={errores.diagnostico}><textarea id="dx" rows={2} className="form-control" value={f.diagnostico} onChange={(e) => setF({ ...f, diagnostico: e.target.value })} /></Campo>
        <Campo id="tx" etiqueta="Tratamiento / indicaciones" error={errores.tratamiento}><textarea id="tx" rows={3} className="form-control" value={f.tratamiento} onChange={(e) => setF({ ...f, tratamiento: e.target.value })} /></Campo>
        <Campo id="ob" etiqueta="Observaciones" error={errores.observaciones}><textarea id="ob" rows={2} className="form-control" value={f.observaciones} onChange={(e) => setF({ ...f, observaciones: e.target.value })} /></Campo>
      </div>
      <div className="caja mb-3">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <h2 className="h5 mb-0">Receta médica</h2>
          {!recetaBloqueada && <Button size="sm" variant="outline-primary" onClick={() => setItems([...items, { medicamentoId: '', dosis: '', cantidad: 1 }])}><i className="bi bi-plus-lg me-1" />Agregar medicamento</Button>}
        </div>
        {recetaBloqueada && <Alert variant="info">La receta ya fue dispensada en farmacia y no puede modificarse.</Alert>}
        {!items.length && <p className="text-muted mb-0">Sin medicamentos. Puede dejar la receta vacía.</p>}
        {items.map((it, i) => (
          <div className="row g-2 mb-2 align-items-start" key={i}>
            <div className="col-md-5"><select aria-label="Medicamento" className="form-select" disabled={recetaBloqueada} value={it.medicamentoId} onChange={(e) => setItem(i, 'medicamentoId', e.target.value)}>
              <option value="">Medicamento…</option>{catalogo?.map((m) => <option key={m.idMedicamento} value={m.idMedicamento}>{m.nombre} ({m.presentacion})</option>)}</select></div>
            <div className="col-md-4"><input aria-label="Dosis" className="form-control" placeholder="Dosis (ej. 1 tableta cada 12 h)" disabled={recetaBloqueada} value={it.dosis} onChange={(e) => setItem(i, 'dosis', e.target.value)} /></div>
            <div className="col-6 col-md-2"><input aria-label="Cantidad" type="number" min="1" className="form-control" disabled={recetaBloqueada} value={it.cantidad} onChange={(e) => setItem(i, 'cantidad', e.target.value)} /></div>
            <div className="col-6 col-md-1">{!recetaBloqueada && <Button variant="outline-danger" onClick={() => setItems(items.filter((_, j) => j !== i))} aria-label="Quitar"><i className="bi bi-trash" /></Button>}</div>
            {errores[`d${i}`] && <div className="text-danger small">{errores[`d${i}`]}</div>}
          </div>
        ))}
        {items.length > 0 && <Campo id="ind" etiqueta="Indicaciones generales de la receta"><textarea id="ind" rows={2} className="form-control" disabled={recetaBloqueada} value={f.indicaciones} onChange={(e) => setF({ ...f, indicaciones: e.target.value })} /></Campo>}
      </div>
      <div className="d-flex gap-2 flex-wrap">
        <Button variant="outline-primary" size="lg" disabled={ocupado} onClick={() => guardar(false)}>Guardar</Button>
        <Button variant="outline-secondary" size="lg" onClick={() => setSeg(true)}><i className="bi bi-calendar-plus me-2" />Agendar seguimiento</Button>
        {!cerrada && <Button className="btn-azul" size="lg" disabled={ocupado} onClick={() => guardar(true)}><i className="bi bi-check2-circle me-2" />Guardar y marcar como atendida</Button>}
      </div>
      <Modal show={seg} onHide={() => setSeg(false)} size="lg" centered>
        <Modal.Header closeButton><Modal.Title>Cita de seguimiento — {cita.paciente}</Modal.Title></Modal.Header>
        <Modal.Body><SelectorHorario especialidadId={cita.especialidadId} seleccionado={horario} onElegir={setHorario} /></Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setSeg(false)}>Cancelar</Button><Button className="btn-azul" disabled={!horario} onClick={agendarSeguimiento}>Agendar</Button></Modal.Footer>
      </Modal>
    </>
  );
}

/** RF-28 historial clínico del paciente (solo médico tratante). */
export function HistorialDePaciente() {
  const { pacienteId } = useParams();
  return (
    <>
      <Titulo icono="journal-medical">Historial clínico del paciente</Titulo>
      <Alert variant="warning" className="small"><i className="bi bi-shield-lock me-1" />Acceso confidencial y auditado (Ley N.° 29733). Use esta información solo para la atención del paciente.</Alert>
      <div className="caja"><HistorialPaciente pacienteId={pacienteId} /></div>
    </>
  );
}


/** Pacientes atendidos o con cita con el médico: búsqueda y acceso rápido a historial. */
export function MisPacientes() {
  const [q, setQ] = useState('');
  const [lista, cargando] = useCarga(() => api.get('/medico/pacientes').then((r) => r.data), []);
  const visibles = (lista || []).filter((p) => !q.trim() || `${p.nombreCompleto} ${p.documentoIdentidad}`.toLowerCase().includes(q.trim().toLowerCase()));
  return (
    <>
      <Titulo icono="people">Mis pacientes</Titulo>
      <div className="caja mb-3" style={{ maxWidth: 420 }}><label htmlFor="mp" className="form-label">Buscar</label><input id="mp" className="form-control" placeholder="Nombre o documento" value={q} onChange={(e) => setQ(e.target.value)} /></div>
      <div className="caja">
        {cargando && !lista ? <Cargando /> : !visibles.length ? <Vacio icono="people">No hay pacientes para mostrar.</Vacio> : (
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr><th>Paciente</th><th>Última cita</th><th>Próxima cita</th><th className="text-end">Citas</th><th className="text-end">Atendidas</th><th /></tr></thead>
            <tbody>{visibles.map((p) => (
              <tr key={p.id}><td className="fw-semibold">{p.nombreCompleto}<div className="small text-muted">Doc. {p.documentoIdentidad}</div></td><td>{p.ultimaCita ? fmtFecha(p.ultimaCita) : '—'}</td><td>{p.proximaCita ? fmtFecha(p.proximaCita) : '—'}</td>
                <td className="text-end">{p.totalCitas}</td><td className="text-end">{p.atendidas}</td>
                <td className="text-end"><Link to={`/medico/paciente/${p.id}`} className="btn btn-sm btn-outline-primary">Historial</Link></td></tr>))}</tbody>
          </table></div>
        )}
      </div>
    </>
  );
}

/** El médico administra sus propios horarios (bloquear, habilitar, generar). */
export function MiDisponibilidad() {
  const { usuario } = useAuth();
  return <Disponibilidad medicoFijo={usuario.id} />;
}
