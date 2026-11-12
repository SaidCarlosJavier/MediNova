import { useState } from 'react';
import { Button, Modal } from 'react-bootstrap';
import { api, msg } from '../../api';
import { Campo, Cargando, EstadoBadge, Titulo, Vacio, fmtFechaHora, fmtHora, hoyISO, sumarDias, useCarga, useToast } from '../../components/ui';

/** RF-24 lista de espera por especialidad. */
export function ListaEspera() {
  const toast = useToast();
  const [esp, setEsp] = useState('');
  const [especialidades] = useCarga(() => api.get('/especialidades').then((r) => r.data), []);
  const [lista, cargando, recargar] = useCarga(() => api.get('/espera', { params: esp ? { especialidadId: esp } : {} }).then((r) => r.data), [esp]);

  const cambiar = async (id, estado) => {
    try { await api.patch(`/espera/${id}`, null, { params: { estado } }); toast('Registro actualizado'); recargar(); } catch (e) { toast(msg(e), 'error'); }
  };

  return (
    <>
      <Titulo icono="hourglass-split">Lista de espera</Titulo>
      <div className="caja mb-3" style={{ maxWidth: 420 }}>
        <label htmlFor="esp" className="form-label">Especialidad</label>
        <select id="esp" className="form-select" value={esp} onChange={(e) => setEsp(e.target.value)}><option value="">Todas</option>{especialidades?.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}</select>
      </div>
      <div className="caja">
        <p className="small text-muted">Al liberarse un turno, el sistema notifica automáticamente al primero en espera (estado «Notificado»). Puede marcarlo como atendido una vez que reserve.</p>
        {cargando && !lista ? <Cargando /> : !lista?.length ? <Vacio icono="check2-circle">No hay pacientes en espera.</Vacio> : (
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr><th>#</th><th>Paciente</th><th>Especialidad</th><th>Registrado</th><th>Estado</th><th /></tr></thead>
            <tbody>{lista.map((l, i) => (
              <tr key={l.id}><td>{i + 1}</td><td className="fw-semibold">{l.paciente}<div className="small text-muted">{l.pacienteDocumento}</div></td><td>{l.especialidad}</td><td>{fmtFechaHora(l.fechaRegistro)}</td><td><EstadoBadge estado={l.estado} /></td>
                <td className="text-end text-nowrap"><Button size="sm" variant="outline-success" className="me-2" onClick={() => cambiar(l.id, 'ATENDIDO')}>Atendido</Button><Button size="sm" variant="outline-danger" onClick={() => cambiar(l.id, 'CANCELADO')}>Quitar</Button></td></tr>))}</tbody>
          </table></div>
        )}
      </div>
    </>
  );
}

/** RF-19 modificar disponibilidad de horarios (bloquear/liberar y generar). */
export function Disponibilidad({ medicoFijo = null }) {
  const toast = useToast();
  const [medicoId, setMedicoId] = useState(medicoFijo ? String(medicoFijo) : '');
  const [fecha, setFecha] = useState(hoyISO());
  const [medicos] = useCarga(() => (medicoFijo ? Promise.resolve([]) : api.get('/medicos').then((r) => r.data)), []);
  const [consultorios] = useCarga(() => api.get('/consultorios').then((r) => r.data), []);
  const [horarios, cargando, recargar] = useCarga(
    () => (medicoId ? api.get('/horarios/gestion', { params: { medicoId, fecha } }).then((r) => r.data) : Promise.resolve([])), [medicoId, fecha]);
  const [modal, setModal] = useState(false);
  const [g, setG] = useState({ consultorioId: '', desde: hoyISO(), hasta: sumarDias(hoyISO(), 6), turno: 'MANANA' });

  const alternar = async (h) => {
    try { await api.patch(`/horarios/${h.idHorario}/disponibilidad`, null, { params: { disponible: !h.disponible } }); toast(h.disponible ? 'Horario bloqueado' : 'Horario habilitado'); recargar(); }
    catch (e) { toast(msg(e), 'error'); }
  };
  const eliminar = async (h) => {
    try { await api.delete(`/horarios/${h.idHorario}`); toast('Horario eliminado'); recargar(); } catch (e) { toast(msg(e), 'error'); }
  };
  const generar = async () => {
    try {
      const { data } = await api.post('/horarios/generar', { medicoId: Number(medicoId), consultorioId: Number(g.consultorioId), desde: g.desde, hasta: g.hasta, turno: g.turno });
      toast(`Se crearon ${data.creados} horarios`); setModal(false); recargar();
    } catch (e) { toast(msg(e), 'error'); }
  };

  return (
    <>
      <Titulo icono="clock-history" acciones={<Button className="btn-azul" disabled={!medicoId} onClick={() => setModal(true)}><i className="bi bi-plus-circle me-1" />Generar horarios</Button>}>Disponibilidad de horarios</Titulo>
      <div className="caja mb-3"><div className="row g-3">
        {!medicoFijo && <div className="col-md-6"><label htmlFor="dm" className="form-label">Médico</label><select id="dm" className="form-select" value={medicoId} onChange={(e) => setMedicoId(e.target.value)}><option value="">Seleccione…</option>{medicos?.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto} — {m.especialidad}</option>)}</select></div>}
        <div className="col-md-4"><label htmlFor="df" className="form-label">Fecha</label><input id="df" type="date" className="form-control" value={fecha} onChange={(e) => setFecha(e.target.value)} /></div>
      </div></div>
      <div className="caja">
        {!medicoId ? <Vacio icono="person-badge">Seleccione un médico.</Vacio> : cargando && !horarios ? <Cargando /> : !horarios?.length ? <Vacio icono="calendar-x">No hay horarios ese día. Use «Generar horarios».</Vacio> : (
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr><th>Hora</th><th>Turno</th><th>Consultorio</th><th>Estado</th><th /></tr></thead>
            <tbody>{horarios.map((h) => (
              <tr key={h.idHorario}><td className="fw-bold">{fmtHora(h.horaInicio)} – {fmtHora(h.horaFin)}</td><td>{h.turno === 'MANANA' ? 'Mañana' : 'Tarde'}</td><td>Pab. {h.pabellon} · Room {h.room}</td>
                <td>{h.disponible ? <span className="badge bg-success">Libre</span> : <span className="badge bg-secondary">Ocupado / bloqueado</span>}</td>
                <td className="text-end text-nowrap"><Button size="sm" variant="outline-primary" className="me-2" onClick={() => alternar(h)}>{h.disponible ? 'Bloquear' : 'Habilitar'}</Button><Button size="sm" variant="outline-danger" onClick={() => eliminar(h)}><i className="bi bi-trash" /></Button></td></tr>))}</tbody>
          </table></div>
        )}
      </div>
      <Modal show={modal} onHide={() => setModal(false)} centered>
        <Modal.Header closeButton><Modal.Title>Generar horarios</Modal.Title></Modal.Header>
        <Modal.Body>
          <Campo id="g-c" etiqueta="Consultorio"><select id="g-c" className="form-select" value={g.consultorioId} onChange={(e) => setG({ ...g, consultorioId: e.target.value })}><option value="">Seleccione…</option>{consultorios?.map((c) => <option key={c.id} value={c.id}>{c.ubicacion}</option>)}</select></Campo>
          <div className="row"><div className="col-6"><Campo id="g-d" etiqueta="Desde"><input id="g-d" type="date" className="form-control" value={g.desde} min={hoyISO()} onChange={(e) => setG({ ...g, desde: e.target.value })} /></Campo></div>
            <div className="col-6"><Campo id="g-h" etiqueta="Hasta"><input id="g-h" type="date" className="form-control" value={g.hasta} min={g.desde} onChange={(e) => setG({ ...g, hasta: e.target.value })} /></Campo></div></div>
          <Campo id="g-t" etiqueta="Turno" ayuda="Las horas y la duración se configuran en Administración → Turnos."><select id="g-t" className="form-select" value={g.turno} onChange={(e) => setG({ ...g, turno: e.target.value })}><option value="MANANA">Mañana</option><option value="TARDE">Tarde</option></select></Campo>
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setModal(false)}>Cancelar</Button><Button className="btn-azul" disabled={!g.consultorioId} onClick={generar}>Generar</Button></Modal.Footer>
      </Modal>
    </>
  );
}
