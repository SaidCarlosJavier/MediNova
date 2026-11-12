import { useState } from 'react';
import { Badge, Button, Col, Modal, Row } from 'react-bootstrap';
import { api, fieldErrors, msg } from '../../api';
import PagoModal, { money } from '../../components/Pago';
import { Campo, Cargando, EstadoBadge, Paginador, Titulo, Vacio, fmtFecha, fmtFechaHora, hoyISO, sumarDias, useCarga, useToast } from '../../components/ui';

const VACIO = { nombre: '', principioActivo: '', presentacion: '', stockActual: 0, stockMinimo: 0, fechaVencimiento: '', precio: '' };

/** Inventario: CRUD de medicamentos + entradas/salidas/ajustes. */
export function Inventario() {
  const toast = useToast();
  const [q, setQ] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(0);
  const [datos, cargando, recargar] = useCarga(() => api.get('/farmacia/medicamentos', { params: { q: busqueda, pagina, tamanio: 10 } }).then((r) => r.data), [busqueda, pagina]);
  const [edit, setEdit] = useState(null); // {id?, ...}
  const [mov, setMov] = useState(null);
  const [m, setM] = useState({ tipo: 'ENTRADA', cantidad: '', motivo: '' });
  const [errores, setErrores] = useState({});

  const guardar = async () => {
    const errs = {};
    if (!edit.nombre.trim()) errs.nombre = 'Ingrese el nombre del medicamento';
    if (edit.stockActual === '' || Number(edit.stockActual) < 0) errs.stockActual = 'El stock no puede ser negativo';
    if (edit.stockMinimo === '' || Number(edit.stockMinimo) < 0) errs.stockMinimo = 'El stock mínimo no puede ser negativo';
    if (!edit.fechaVencimiento) errs.fechaVencimiento = 'Ingrese la fecha de vencimiento';
    if (edit.precio !== '' && Number(edit.precio) < 0) errs.precio = 'El precio no puede ser negativo';
    setErrores(errs);
    if (Object.keys(errs).length) return;
    const body = { ...edit, stockActual: Number(edit.stockActual), stockMinimo: Number(edit.stockMinimo), precio: edit.precio === '' ? null : Number(edit.precio) };
    try {
      if (edit.idMedicamento) await api.put(`/farmacia/medicamentos/${edit.idMedicamento}`, body); else await api.post('/farmacia/medicamentos', body);
      toast('Medicamento guardado'); setEdit(null); recargar();
    } catch (e) { setErrores(fieldErrors(e)); toast(msg(e), 'error'); }
  };

  const registrarMov = async () => {
    if (!m.cantidad || Number(m.cantidad) === 0) return setErrores({ cantidad: 'Ingrese una cantidad válida' });
    try { await api.post(`/farmacia/medicamentos/${mov.idMedicamento}/movimientos`, { ...m, cantidad: Number(m.cantidad) }); toast('Movimiento registrado'); setMov(null); setM({ tipo: 'ENTRADA', cantidad: '', motivo: '' }); setErrores({}); recargar(); }
    catch (e) { setErrores(fieldErrors(e)); toast(msg(e), 'error'); }
  };

  const activo = async (x) => { try { await api.patch(`/farmacia/medicamentos/${x.idMedicamento}/activo`, null, { params: { activo: !x.activo } }); recargar(); } catch (e) { toast(msg(e), 'error'); } };

  const F = (k, etiqueta, extra = {}) => (
    <Col md={6}><Campo id={`m-${k}`} etiqueta={etiqueta} error={errores[k]}><input id={`m-${k}`} className="form-control" value={edit[k] ?? ''} onChange={(e) => setEdit({ ...edit, [k]: e.target.value })} {...extra} /></Campo></Col>
  );

  return (
    <>
      <Titulo icono="box-seam" acciones={<Button className="btn-azul" onClick={() => { setErrores({}); setEdit({ ...VACIO }); }}><i className="bi bi-plus-circle me-1" />Nuevo medicamento</Button>}>Inventario de medicamentos</Titulo>
      <div className="caja mb-3"><form className="row g-2" onSubmit={(e) => { e.preventDefault(); setPagina(0); setBusqueda(q.trim()); }}>
        <div className="col-md-9"><input aria-label="Buscar" className="form-control" placeholder="Buscar por nombre o principio activo…" value={q} onChange={(e) => setQ(e.target.value)} /></div>
        <div className="col-md-3"><Button type="submit" className="btn-azul w-100">Buscar</Button></div>
      </form></div>
      <div className="caja">
        {cargando && !datos ? <Cargando /> : !datos?.contenido.length ? <Vacio icono="box">Sin resultados.</Vacio> : (
          <>
            <div className="table-responsive"><table className="table align-middle mb-0">
              <thead><tr><th>Medicamento</th><th>Presentación</th><th className="text-end">Stock</th><th className="text-end">Mínimo</th><th>Vence</th><th>Estado</th><th /></tr></thead>
              <tbody>{datos.contenido.map((x) => (
                <tr key={x.idMedicamento} className={x.activo ? '' : 'text-muted'}>
                  <td className="fw-semibold">{x.nombre}<div className="small text-muted">{x.principioActivo}</div></td><td>{x.presentacion}</td>
                  <td className="text-end fw-bold">{x.stockActual} {x.stockBajo && <Badge bg="danger">Bajo</Badge>}</td><td className="text-end">{x.stockMinimo}</td>
                  <td>{fmtFecha(x.fechaVencimiento)} {x.porVencer && <Badge bg="warning" text="dark">Por vencer</Badge>}</td>
                  <td>{x.activo ? <Badge bg="success">Activo</Badge> : <Badge bg="secondary">Inactivo</Badge>}</td>
                  <td className="text-end text-nowrap">
                    <Button size="sm" variant="outline-primary" className="me-1" onClick={() => { setErrores({}); setMov(x); }}>Movimiento</Button>
                    <Button size="sm" variant="outline-secondary" className="me-1" onClick={() => { setErrores({}); setEdit({ ...x, precio: x.precio ?? '' }); }}>Editar</Button>
                    <Button size="sm" variant={x.activo ? 'outline-danger' : 'outline-success'} onClick={() => activo(x)}>{x.activo ? 'Desactivar' : 'Activar'}</Button>
                  </td>
                </tr>))}</tbody>
            </table></div>
            <Paginador pagina={datos.pagina} totalPaginas={datos.totalPaginas} onCambio={setPagina} />
          </>
        )}
      </div>

      <Modal show={!!edit} onHide={() => setEdit(null)} size="lg" centered>
        <Modal.Header closeButton><Modal.Title>{edit?.idMedicamento ? 'Editar medicamento' : 'Nuevo medicamento'}</Modal.Title></Modal.Header>
        <Modal.Body>{edit && <Row>
          {F('nombre', 'Nombre')}{F('principioActivo', 'Principio activo')}{F('presentacion', 'Presentación')}{F('precio', 'Precio (S/, opcional)', { type: 'number', step: '0.01', min: '0' })}
          {F('stockActual', 'Stock actual', { type: 'number', min: '0' })}{F('stockMinimo', 'Stock mínimo', { type: 'number', min: '0' })}{F('fechaVencimiento', 'Fecha de vencimiento', { type: 'date' })}
        </Row>}</Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setEdit(null)}>Cancelar</Button><Button className="btn-azul" onClick={guardar}>Guardar</Button></Modal.Footer>
      </Modal>

      <Modal show={!!mov} onHide={() => setMov(null)} centered>
        <Modal.Header closeButton><Modal.Title>Movimiento — {mov?.nombre}</Modal.Title></Modal.Header>
        <Modal.Body>
          <p className="text-muted">Stock actual: <strong>{mov?.stockActual}</strong></p>
          <Campo id="mv-t" etiqueta="Tipo"><select id="mv-t" className="form-select" value={m.tipo} onChange={(e) => setM({ ...m, tipo: e.target.value })}><option value="ENTRADA">Entrada (ingreso de stock)</option><option value="SALIDA">Salida (merma, baja)</option><option value="AJUSTE">Ajuste (use negativo para restar)</option></select></Campo>
          <Campo id="mv-c" etiqueta="Cantidad" error={errores.cantidad}><input id="mv-c" type="number" className="form-control" value={m.cantidad} onChange={(e) => setM({ ...m, cantidad: e.target.value })} /></Campo>
          <Campo id="mv-m" etiqueta="Motivo" error={errores.motivo}><input id="mv-m" className="form-control" value={m.motivo} onChange={(e) => setM({ ...m, motivo: e.target.value })} /></Campo>
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setMov(null)}>Cancelar</Button><Button className="btn-azul" onClick={registrarMov}>Registrar</Button></Modal.Footer>
      </Modal>
    </>
  );
}

/** Alertas de stock bajo y por vencer. */
export function Alertas() {
  const [a, cargando] = useCarga(() => api.get('/farmacia/alertas').then((r) => r.data), []);
  if (cargando && !a) return <Cargando />;
  const Tabla = ({ lista, vacio, col }) => (!lista?.length ? <Vacio icono="check2-circle">{vacio}</Vacio> : (
    <div className="table-responsive"><table className="table mb-0"><thead><tr><th>Medicamento</th><th className="text-end">Stock</th><th className="text-end">Mínimo</th><th>Vence</th></tr></thead>
      <tbody>{lista.map((x) => <tr key={x.idMedicamento}><td className="fw-semibold">{x.nombre}</td><td className={`text-end fw-bold ${col === 'stock' ? 'text-danger' : ''}`}>{x.stockActual}</td><td className="text-end">{x.stockMinimo}</td><td className={col === 'vence' ? 'text-danger fw-bold' : ''}>{fmtFecha(x.fechaVencimiento)}{x.fechaVencimiento < hoyISO() && ' (VENCIDO)'}</td></tr>)}</tbody></table></div>
  ));
  return (
    <>
      <Titulo icono="exclamation-triangle">Alertas de inventario</Titulo>
      <Row className="g-3">
        <Col lg={6}><div className="caja h-100"><h2 className="h5 text-danger"><i className="bi bi-arrow-down-circle me-2" />Stock bajo ({a?.stockBajo.length})</h2><Tabla lista={a?.stockBajo} vacio="Sin alertas de stock." col="stock" /></div></Col>
        <Col lg={6}><div className="caja h-100"><h2 className="h5 text-warning"><i className="bi bi-hourglass-bottom me-2" />Por vencer en 60 días ({a?.porVencer.length})</h2><Tabla lista={a?.porVencer} vacio="Sin medicamentos por vencer." col="vence" /></div></Col>
      </Row>
    </>
  );
}

/** Recetas PENDIENTES / PARCIALES y dispensación (total o parcial). */
export function Recetas() {
  const toast = useToast();
  const [pagina, setPagina] = useState(0);
  const [datos, cargando, recargar] = useCarga(() => api.get('/farmacia/recetas/pendientes', { params: { pagina, tamanio: 6 } }).then((r) => r.data), [pagina]);
  const [rec, setRec] = useState(null);
  const [cant, setCant] = useState({});
  const [ocupado, setOcupado] = useState(false);

  const abrir = (r) => { setRec(r); setCant(Object.fromEntries(r.detalles.map((d) => [d.idDetalle, d.cantidad - d.cantidadDispensada]))); };

  const [aCobrar, setACobrar] = useState(null);
  const [venta, setVenta] = useState(null);

  const itemsSel = () => rec.detalles.map((d) => ({ idDetalle: d.idDetalle, cantidad: Number(cant[d.idDetalle] || 0) })).filter((i) => i.cantidad > 0);
  const totalSel = () => rec.detalles.reduce((a, d) => a + Number(cant[d.idDetalle] || 0) * Number(d.precioUnitario || 0), 0);

  const iniciarCobro = () => {
    const items = itemsSel();
    if (!items.length) return toast('Indique al menos una cantidad a dispensar', 'error');
    setACobrar({ items, total: totalSel() });
  };
  const cobrar = async (pago) => {
    const { data } = await api.post(`/farmacia/recetas/${rec.idReceta}/dispensar`, { items: aCobrar.items, pago });
    setVenta(data); toast(`Venta registrada. Comprobante ${data.pago?.comprobante || ''}`);
    setRec(null); recargar();
  };
  const boleta = async (d) => { try { (await import('../../pdf')).boletaFarmacia(d); } catch { toast('No se pudo generar la boleta', 'error'); } };

  return (
    <>
      <Titulo icono="prescription2">Recetas pendientes de dispensar</Titulo>
      <div className="caja">
        {cargando && !datos ? <Cargando /> : !datos?.contenido.length ? <Vacio icono="check2-circle">No hay recetas pendientes.</Vacio> : (
          <>
            <div className="table-responsive"><table className="table align-middle mb-0">
              <thead><tr><th>Receta</th><th>Paciente</th><th>Médico</th><th>Medicamentos</th><th>Estado</th><th /></tr></thead>
              <tbody>{datos.contenido.map((r) => (
                <tr key={r.idReceta}><td>#{r.idReceta}<div className="small text-muted">{fmtFechaHora(r.fecha)}</div></td><td className="fw-semibold">{r.paciente}<div className="small text-muted">{r.pacienteDocumento}</div></td><td>{r.medico}</td>
                  <td>{r.detalles.map((d) => <div key={d.idDetalle} className="small">{d.medicamento} × {d.cantidad}</div>)}</td><td><EstadoBadge estado={r.estado} /></td>
                  <td className="text-end"><Button size="sm" className="btn-azul" onClick={() => abrir(r)}>Dispensar</Button></td></tr>))}</tbody>
            </table></div>
            <Paginador pagina={datos.pagina} totalPaginas={datos.totalPaginas} onCambio={setPagina} />
          </>
        )}
      </div>
      <Modal show={!!rec} onHide={() => setRec(null)} size="lg" centered>
        <Modal.Header closeButton><Modal.Title>Dispensar receta #{rec?.idReceta}</Modal.Title></Modal.Header>
        <Modal.Body>{rec && <>
          <p><strong>{rec.paciente}</strong> · {rec.indicaciones}</p>
          <table className="table align-middle"><thead><tr><th>Medicamento</th><th>Dosis</th><th className="text-end">P. unit.</th><th className="text-end">Recetado</th><th className="text-end">Entregado</th><th className="text-end">Stock</th><th style={{ width: 110 }}>Dispensar</th></tr></thead>
            <tbody>{rec.detalles.map((d) => (
              <tr key={d.idDetalle}><td className="fw-semibold">{d.medicamento}</td><td className="small">{d.dosis}</td><td className="text-end">{money(d.precioUnitario)}</td><td className="text-end">{d.cantidad}</td><td className="text-end">{d.cantidadDispensada}</td>
                <td className={`text-end ${d.stockActual < d.cantidad - d.cantidadDispensada ? 'text-danger fw-bold' : ''}`}>{d.stockActual}</td>
                <td><input aria-label={`Cantidad de ${d.medicamento}`} type="number" min="0" max={d.cantidad - d.cantidadDispensada} className="form-control" value={cant[d.idDetalle] ?? 0} onChange={(e) => setCant({ ...cant, [d.idDetalle]: e.target.value })} /></td></tr>))}</tbody></table>
          <div className="text-end fs-5 mb-2">Total a cobrar: <strong>{money(totalSel())}</strong></div>
          <p className="small text-muted mb-0">Si no hay stock suficiente de algún medicamento, el sistema rechaza la dispensación. Puede dispensar parcialmente y completar después.</p>
        </>}</Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setRec(null)}>Cancelar</Button><Button className="btn-azul" onClick={iniciarCobro}><i className="bi bi-cash-coin me-1" />Cobrar y dispensar</Button></Modal.Footer>
      </Modal>

      <PagoModal show={!!aCobrar} onHide={() => setACobrar(null)} monto={aCobrar?.total} permitirEfectivo titulo="Cobro de farmacia"
        concepto={rec ? `Receta #${rec.idReceta} - ${rec.paciente}` : ''} onPagar={cobrar} />

      <Modal show={!!venta} onHide={() => setVenta(null)} centered>
        <Modal.Header closeButton><Modal.Title>Venta registrada</Modal.Title></Modal.Header>
        <Modal.Body className="text-center">
          <i className="bi bi-check-circle text-success display-4" />
          <p className="mt-2 mb-1">Comprobante <strong>{venta?.pago?.comprobante}</strong></p>
          <p className="mb-0">Total: <strong>{money(venta?.total)}</strong>{venta?.pago?.vuelto > 0 && <> · Vuelto: <strong>{money(venta.pago.vuelto)}</strong></>}</p>
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setVenta(null)}>Cerrar</Button><Button className="btn-azul" onClick={() => boleta(venta)}><i className="bi bi-file-earmark-pdf me-1" />Descargar boleta PDF</Button></Modal.Footer>
      </Modal>
    </>
  );
}

/** Ventas de farmacia: reimprimir boleta y anular (devuelve stock y reembolsa). */
export function Ventas() {
  const toast = useToast();
  const [desde, setDesde] = useState(sumarDias(hoyISO(), -7));
  const [hasta, setHasta] = useState(hoyISO());
  const [pagina, setPagina] = useState(0);
  const [datos, cargando, recargar] = useCarga(() => api.get('/farmacia/dispensaciones', { params: { desde, hasta, pagina, tamanio: 10 } }).then((r) => r.data), [desde, hasta, pagina]);
  const [aAnular, setAAnular] = useState(null);
  const [motivo, setMotivo] = useState('');
  const boleta = async (d) => { try { (await import('../../pdf')).boletaFarmacia(d); } catch { toast('No se pudo generar la boleta', 'error'); } };
  const anular = async () => {
    try { await api.patch(`/farmacia/dispensaciones/${aAnular.idDispensacion}/anular`, { motivo }); toast('Venta anulada: stock restituido'); setAAnular(null); setMotivo(''); recargar(); }
    catch (e) { toast(msg(e), 'error'); }
  };
  const total = (datos?.contenido || []).filter((d) => !d.anulada).reduce((a, d) => a + Number(d.total), 0);
  return (
    <>
      <Titulo icono="receipt">Ventas y boletas</Titulo>
      <div className="caja mb-3"><div className="row g-3">
        <div className="col-sm-4"><label htmlFor="vd" className="form-label">Desde</label><input id="vd" type="date" className="form-control" value={desde} max={hasta} onChange={(e) => { setPagina(0); setDesde(e.target.value); }} /></div>
        <div className="col-sm-4"><label htmlFor="vh" className="form-label">Hasta</label><input id="vh" type="date" className="form-control" value={hasta} min={desde} onChange={(e) => { setPagina(0); setHasta(e.target.value); }} /></div>
        <div className="col-sm-4 d-flex align-items-end"><div>Total de esta página: <strong>{money(total)}</strong></div></div>
      </div></div>
      <div className="caja">
        {cargando && !datos ? <Cargando /> : !datos?.contenido.length ? <Vacio icono="receipt">No hay ventas en el rango.</Vacio> : (<>
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr><th>Fecha</th><th>Comprobante</th><th>Paciente</th><th>Medicamentos</th><th className="text-end">Total</th><th>Estado</th><th /></tr></thead>
            <tbody>{datos.contenido.map((d) => (
              <tr key={d.idDispensacion}><td>{fmtFechaHora(d.fecha)}</td><td className="text-nowrap">{d.pago?.comprobante || '—'}</td><td>{d.paciente}<div className="small text-muted">{d.pacienteDocumento}</div></td>
                <td>{d.lineas.map((l) => <div key={l.idMedicamento} className="small">{l.medicamento} × {l.cantidad}</div>)}</td><td className="text-end">{money(d.total)}</td>
                <td><span className={`badge ${d.anulada ? 'bg-secondary' : 'bg-success'}`}>{d.anulada ? 'Anulada' : 'Pagada'}</span></td>
                <td className="text-end text-nowrap"><Button size="sm" variant="outline-secondary" className="me-2" onClick={() => boleta(d)}><i className="bi bi-file-earmark-pdf" /> Boleta</Button>
                  {!d.anulada && <Button size="sm" variant="outline-danger" onClick={() => setAAnular(d)}>Anular</Button>}</td></tr>))}</tbody>
          </table></div>
          <Paginador pagina={datos.pagina} totalPaginas={datos.totalPaginas} onCambio={setPagina} />
        </>)}
      </div>
      <Modal show={!!aAnular} onHide={() => setAAnular(null)} centered>
        <Modal.Header closeButton><Modal.Title>Anular venta {aAnular?.pago?.comprobante}</Modal.Title></Modal.Header>
        <Modal.Body><p className="small text-muted">Se devolverá el stock al inventario y se reembolsará el pago. La receta volverá a quedar pendiente.</p>
          <Campo id="va" etiqueta="Motivo de la anulación"><textarea id="va" rows={3} maxLength={250} className="form-control" value={motivo} onChange={(e) => setMotivo(e.target.value)} /></Campo></Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setAAnular(null)}>Cancelar</Button><Button variant="danger" disabled={!motivo.trim()} onClick={anular}>Anular venta</Button></Modal.Footer>
      </Modal>
    </>
  );
}

/** Historial de movimientos de inventario. */
export function Movimientos() {
  const [pagina, setPagina] = useState(0);
  const [datos, cargando] = useCarga(() => api.get('/farmacia/movimientos', { params: { pagina, tamanio: 15 } }).then((r) => r.data), [pagina]);
  return (
    <>
      <Titulo icono="arrow-left-right">Movimientos de inventario</Titulo>
      <div className="caja">
        {cargando && !datos ? <Cargando /> : !datos?.contenido.length ? <Vacio>Sin movimientos.</Vacio> : (<>
          <div className="table-responsive"><table className="table mb-0"><thead><tr><th>Fecha</th><th>Medicamento</th><th>Tipo</th><th className="text-end">Cantidad</th><th>Motivo</th><th>Usuario</th></tr></thead>
            <tbody>{datos.contenido.map((x) => <tr key={x.id}><td>{fmtFechaHora(x.fecha)}</td><td>{x.medicamento}</td><td><Badge bg={x.tipo === 'ENTRADA' ? 'success' : x.tipo === 'SALIDA' ? 'danger' : 'info'}>{x.tipo}</Badge></td><td className="text-end">{x.cantidad}</td><td className="small">{x.motivo}</td><td className="small">{x.usuario}</td></tr>)}</tbody></table></div>
          <Paginador pagina={datos.pagina} totalPaginas={datos.totalPaginas} onCambio={setPagina} />
        </>)}
      </div>
    </>
  );
}
