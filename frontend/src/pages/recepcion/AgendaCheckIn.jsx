import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { api, msg } from '../../api';
import { Cargando, EstadoBadge, Titulo, Vacio, fmtHora, hoyISO, useCarga, useToast } from '../../components/ui';

/** RF-25 agenda diaria por médico + RF-23 check-in de asistencia. */
export default function AgendaCheckIn() {
  const toast = useToast();
  const [fecha, setFecha] = useState(hoyISO());
  const [medicoId, setMedicoId] = useState('');
  const [medicos] = useCarga(() => api.get('/medicos').then((r) => r.data), []);
  const [citas, cargando, recargar] = useCarga(() => api.get('/citas/dia', { params: { fecha } }).then((r) => r.data), [fecha]);

  const lista = (citas || []).filter((c) => !medicoId || String(c.medicoId) === medicoId);

  const checkIn = async (c) => {
    try { await api.patch(`/citas/${c.idCita}/check-in`); toast(`Asistencia registrada: ${c.paciente}`); recargar(); }
    catch (e) { toast(msg(e), 'error'); }
  };

  return (
    <>
      <Titulo icono="calendar-day">Agenda del día y check-in</Titulo>
      <div className="caja mb-3">
        <div className="row g-3">
          <div className="col-md-4"><label htmlFor="f" className="form-label">Fecha</label><input id="f" type="date" className="form-control" value={fecha} onChange={(e) => setFecha(e.target.value)} /></div>
          <div className="col-md-5"><label htmlFor="m" className="form-label">Médico</label>
            <select id="m" className="form-select" value={medicoId} onChange={(e) => setMedicoId(e.target.value)}><option value="">Todos los médicos</option>
              {medicos?.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto} — {m.especialidad}</option>)}</select></div>
        </div>
      </div>
      <div className="caja">
        {cargando && !citas ? <Cargando /> : !lista.length ? <Vacio icono="calendar-x">No hay citas para los filtros elegidos.</Vacio> : (
          <div className="table-responsive"><table className="table table-hover align-middle mb-0">
            <thead><tr><th>Hora</th><th>Paciente</th><th>Doc.</th><th>Médico</th><th>Consultorio</th><th>Estado</th><th /></tr></thead>
            <tbody>{lista.map((c) => (
              <tr key={c.idCita}>
                <td className="fw-bold">{fmtHora(c.horaInicio)}</td><td>{c.paciente}</td><td>{c.pacienteDocumento}</td><td>{c.medico}<div className="small text-muted">{c.especialidad}</div></td>
                <td>Pab. {c.pabellon} · Room {c.room}</td><td><EstadoBadge estado={c.estado} /></td>
                <td className="text-end">{c.modificable && fecha === hoyISO() && <Button size="sm" className="btn-azul" onClick={() => checkIn(c)}><i className="bi bi-person-check me-1" />Check-in</Button>}</td>
              </tr>))}</tbody>
          </table></div>
        )}
      </div>
    </>
  );
}
