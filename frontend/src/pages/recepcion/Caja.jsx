import { useState } from 'react';
import { Button, Col, Modal, Row } from 'react-bootstrap';
import { api, msg } from '../../api';
import { Campo, Cargando, Titulo, Vacio, fmtFechaHora, hoyISO, useCarga, useToast } from '../../components/ui';
import { money } from '../../components/Pago';

/** Caja: pagos de consultas cobrados por ventanilla o en línea, con anulación y boleta en PDF. */
export default function Caja() {
  const toast = useToast();
  const [desde, setDesde] = useState(hoyISO());
  const [hasta, setHasta] = useState(hoyISO());
  const [pagos, cargando, recargar] = useCarga(() => api.get('/pagos', { params: { desde, hasta, tipo: 'CONSULTA', tamanio: 100 } }).then((r) => r.data), [desde, hasta]);
  const [res] = useCarga(() => api.get('/pagos/resumen', { params: { desde, hasta } }).then((r) => r.data), [desde, hasta, pagos]);
  const [aAnular, setAAnular] = useState(null);
  const [motivo, setMotivo] = useState('');

  const lista = Array.isArray(pagos) ? pagos : pagos?.contenido || [];
  const boleta = async (p) => { try { (await import('../../pdf')).boletaConsulta(p); } catch { toast('No se pudo generar la boleta', 'error'); } };
  const anular = async () => {
    try { await api.patch(`/pagos/${aAnular.idPago}/anular`, { motivo }); toast('Pago anulado'); setAAnular(null); setMotivo(''); recargar(); }
    catch (e) { toast(msg(e), 'error'); }
  };

  return (
    <>
      <Titulo icono="cash-coin">Caja de consultas</Titulo>
      <div className="caja mb-3"><div className="row g-3">
        <div className="col-sm-4"><label htmlFor="cd" className="form-label">Desde</label><input id="cd" type="date" className="form-control" value={desde} max={hasta} onChange={(e) => setDesde(e.target.value)} /></div>
        <div className="col-sm-4"><label htmlFor="ch" className="form-label">Hasta</label><input id="ch" type="date" className="form-control" value={hasta} min={desde} onChange={(e) => setHasta(e.target.value)} /></div>
      </div></div>
      {res && (
        <Row className="g-3 mb-3">
          <Col sm={4}><div className="caja stat"><div className="small text-muted">Total cobrado</div><div className="num">{money(res.total)}</div></div></Col>
          <Col sm={4}><div className="caja stat"><div className="small text-muted">Pagos</div><div className="num">{res.cantidadPagos}</div></div></Col>
          <Col sm={4}><div className="caja stat"><div className="small text-muted">Anulados</div><div className="num">{res.anulados}</div></div></Col>
        </Row>
      )}
      <div className="caja">
        {cargando && !pagos ? <Cargando /> : !lista.length ? <Vacio icono="receipt">No hay pagos en el rango seleccionado. Para cobrar, abra al paciente en «Pacientes y citas» → Citas → Pagar.</Vacio> : (
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr><th>Fecha</th><th>Comprobante</th><th>Paciente</th><th>Método</th><th className="text-end">Monto</th><th>Estado</th><th /></tr></thead>
            <tbody>{lista.map((p) => (
              <tr key={p.idPago}><td>{fmtFechaHora(p.fecha)}</td><td className="text-nowrap">{p.comprobante}</td><td>{p.paciente}<div className="small text-muted">{p.pacienteDocumento}</div></td><td>{p.metodo}</td><td className="text-end">{money(p.monto)}</td>
                <td><span className={`badge ${p.estado === 'PAGADO' ? 'bg-success' : 'bg-secondary'}`}>{p.estado === 'PAGADO' ? 'Pagado' : 'Anulado'}</span></td>
                <td className="text-end text-nowrap"><Button size="sm" variant="outline-secondary" className="me-2" onClick={() => boleta(p)}><i className="bi bi-file-earmark-pdf" /> Boleta</Button>
                  {p.estado === 'PAGADO' && <Button size="sm" variant="outline-danger" onClick={() => setAAnular(p)}>Anular</Button>}</td></tr>))}</tbody>
          </table></div>
        )}
      </div>
      <Modal show={!!aAnular} onHide={() => setAAnular(null)} centered>
        <Modal.Header closeButton><Modal.Title>Anular pago {aAnular?.comprobante}</Modal.Title></Modal.Header>
        <Modal.Body><Campo id="mo" etiqueta="Motivo de la anulación"><textarea id="mo" rows={3} maxLength={250} className="form-control" value={motivo} onChange={(e) => setMotivo(e.target.value)} /></Campo></Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setAAnular(null)}>Cancelar</Button><Button variant="danger" disabled={!motivo.trim()} onClick={anular}>Anular pago</Button></Modal.Footer>
      </Modal>
    </>
  );
}
