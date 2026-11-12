import { useState } from 'react';
import { Badge, Button, Col, Modal, Row, Tab, Tabs } from 'react-bootstrap';
import { api, fieldErrors, msg } from '../../api';
import { Campo, Cargando, Paginador, ROL_TEXTO, Titulo, Vacio, fmtFechaHora, hoyISO, reglas, sumarDias, useCarga, useToast, validar } from '../../components/ui';
import { useAuth } from '../../AuthContext';
import { money } from '../../components/Pago';

// ------------------------------------------------------------ Indicadores (RNF-23) + auditoría (RNF-14)
export function Indicadores() {
  const [d, cargando] = useCarga(() => api.get('/admin/indicadores').then((r) => r.data), []);
  const [pagina, setPagina] = useState(0);
  const [aud, cargandoAud] = useCarga(() => api.get('/admin/auditoria', { params: { pagina, tamanio: 8 } }).then((r) => r.data), [pagina]);
  const [ing] = useCarga(() => api.get('/pagos/resumen', { params: { desde: sumarDias(hoyISO(), -29), hasta: hoyISO() } }).then((r) => r.data).catch(() => null), []);
  if (cargando && !d) return <Cargando />;
  const tarjetas = [
    ['Total de citas', d?.totalCitas, 'calendar3'], ['Atendidas', d?.atendidas, 'check-circle'], ['Canceladas', d?.canceladas, 'x-circle'],
    ['Reprogramadas', d?.reprogramadas, 'arrow-repeat'], ['% de inasistencia', `${d?.porcentajeInasistencia ?? 0}%`, 'person-x'],
    ['Tiempo promedio de reserva', d?.tiempoPromedioReservaSeg != null ? `${d.tiempoPromedioReservaSeg} s` : 'Sin datos', 'stopwatch'],
    ...(ing ? [['Ingresos (últimos 30 días)', money(ing.total), 'cash-coin']] : []),
  ];
  return (
    <>
      <Titulo icono="speedometer2">Indicadores generales</Titulo>
      <p className="text-muted">Referencia para comparar los resultados antes y después de implementar el sistema.</p>
      <Row className="g-3 mb-4">{tarjetas.map(([t, v, ic]) => (
        <Col sm={6} lg={4} key={t}><div className="caja stat h-100"><i className={`bi bi-${ic} fs-3 text-primary`} /><div className="num">{v ?? 0}</div><div className="text-muted small">{t}</div></div></Col>))}
      </Row>
      <div className="caja">
        <h2 className="h5"><i className="bi bi-shield-check me-2" />Auditoría del historial clínico</h2>
        {cargandoAud && !aud ? <Cargando /> : !aud?.contenido.length ? <Vacio>Sin registros de auditoría.</Vacio> : (<>
          <div className="table-responsive"><table className="table table-sm mb-0"><thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Detalle</th></tr></thead>
            <tbody>{aud.contenido.map((a) => <tr key={a.id}><td>{fmtFechaHora(a.fechaHora)}</td><td>{a.usuario}</td><td><Badge bg={a.accion === 'CONSULTAR' ? 'info' : 'warning'} text="dark">{a.accion}</Badge></td><td className="small">{a.detalle}</td></tr>)}</tbody></table></div>
          <Paginador pagina={aud.pagina} totalPaginas={aud.totalPaginas} onCambio={setPagina} />
        </>)}
      </div>
    </>
  );
}

// ------------------------------------------------------------ Usuarios y roles (RF-31)
const U_VACIO = { tipo: 'MEDICO', documentoIdentidad: '', nombres: '', apellidos: '', correo: '', password: '', numeroColegiatura: '', especialidadId: '' };

export function Usuarios() {
  const { usuario } = useAuth();
  const toast = useToast();
  const [q, setQ] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(0);
  const [datos, cargando, recargar] = useCarga(() => api.get('/admin/usuarios', { params: { q: busqueda, pagina, tamanio: 10 } }).then((r) => r.data), [busqueda, pagina]);
  const [especialidades] = useCarga(() => api.get('/especialidades').then((r) => r.data), []);
  const [modal, setModal] = useState(false);
  const [f, setF] = useState(U_VACIO);
  const [errores, setErrores] = useState({});

  const crear = async () => {
    const esq = { documentoIdentidad: [reglas.documento], nombres: [(v) => reglas.requerido(v, 'Ingrese los nombres')], apellidos: [(v) => reglas.requerido(v, 'Ingrese los apellidos')], correo: [reglas.correo], password: [reglas.password] };
    if (f.tipo === 'MEDICO') { esq.numeroColegiatura = [(v) => reglas.requerido(v, 'Ingrese el número de colegiatura')]; esq.especialidadId = [(v) => reglas.requerido(v, 'Seleccione la especialidad')]; }
    const errs = validar(f, esq);
    setErrores(errs);
    if (Object.keys(errs).length) return;
    try { await api.post('/admin/usuarios', { ...f, especialidadId: f.especialidadId ? Number(f.especialidadId) : null }); toast('Usuario creado'); setModal(false); setF(U_VACIO); recargar(); }
    catch (e) { setErrores(fieldErrors(e)); toast(msg(e), 'error'); }
  };

  const estado = async (u) => { try { await api.patch(`/admin/usuarios/${u.id}/estado`, { estado: !u.estado }); toast(u.estado ? 'Cuenta desactivada' : 'Cuenta activada'); recargar(); } catch (e) { toast(msg(e), 'error'); } };
  const cargo = async (u, c) => { try { await api.patch(`/admin/usuarios/${u.id}/cargo`, { cargo: c }); toast('Rol actualizado'); recargar(); } catch (e) { toast(msg(e), 'error'); } };

  const I = (k, etiqueta, extra = {}) => (
    <Col md={6}><Campo id={`u-${k}`} etiqueta={etiqueta} error={errores[k]}><input id={`u-${k}`} className="form-control" value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} {...extra} /></Campo></Col>
  );

  return (
    <>
      <Titulo icono="person-gear" acciones={<Button className="btn-azul" onClick={() => { setErrores({}); setModal(true); }}><i className="bi bi-person-plus me-1" />Nuevo usuario</Button>}>Cuentas y roles</Titulo>
      <div className="caja mb-3"><form className="row g-2" onSubmit={(e) => { e.preventDefault(); setPagina(0); setBusqueda(q.trim()); }}>
        <div className="col-md-9"><input aria-label="Buscar usuario" className="form-control" placeholder="Buscar por documento, nombre o correo…" value={q} onChange={(e) => setQ(e.target.value)} /></div>
        <div className="col-md-3"><Button type="submit" className="btn-azul w-100">Buscar</Button></div></form></div>
      <div className="caja">
        {cargando && !datos ? <Cargando /> : !datos?.contenido.length ? <Vacio icono="people">Sin resultados.</Vacio> : (<>
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr><th>Usuario</th><th>Documento</th><th>Rol</th><th>Estado</th><th /></tr></thead>
            <tbody>{datos.contenido.map((u) => (
              <tr key={u.id}><td className="fw-semibold">{u.nombreCompleto}<div className="small text-muted">{u.correo}</div></td><td>{u.documentoIdentidad}</td>
                <td>{u.tipo === 'ADMINISTRATIVO' ? (
                  <select aria-label="Cambiar cargo" className="form-select form-select-sm" style={{ minWidth: 160 }} value={u.cargo} disabled={u.id === usuario.id} onChange={(e) => cargo(u, e.target.value)}>
                    <option value="RECEPCIONISTA">Recepcionista</option><option value="FARMACEUTICO">Farmacéutico</option><option value="ADMINISTRADOR">Administrador</option></select>
                ) : <>{ROL_TEXTO[u.rol]}{u.especialidad && <div className="small text-muted">{u.especialidad}</div>}</>}</td>
                <td>{u.estado ? <Badge bg="success">Activa</Badge> : <Badge bg="secondary">Inactiva</Badge>}</td>
                <td className="text-end"><Button size="sm" variant={u.estado ? 'outline-danger' : 'outline-success'} disabled={u.id === usuario.id} onClick={() => estado(u)}>{u.estado ? 'Desactivar' : 'Activar'}</Button></td></tr>))}</tbody>
          </table></div>
          <Paginador pagina={datos.pagina} totalPaginas={datos.totalPaginas} onCambio={setPagina} />
        </>)}
      </div>
      <Modal show={modal} onHide={() => setModal(false)} size="lg" centered>
        <Modal.Header closeButton><Modal.Title>Nuevo usuario del hospital</Modal.Title></Modal.Header>
        <Modal.Body>
          <Campo id="u-tipo" etiqueta="Tipo de usuario"><select id="u-tipo" className="form-select" value={f.tipo} onChange={(e) => setF({ ...f, tipo: e.target.value })}><option value="MEDICO">Médico</option><option value="RECEPCIONISTA">Recepcionista</option><option value="FARMACEUTICO">Farmacéutico</option><option value="ADMINISTRADOR">Administrador</option></select></Campo>
          <Row>{I('documentoIdentidad', 'Documento', { maxLength: 12 })}{I('correo', 'Correo', { type: 'email' })}{I('nombres', 'Nombres')}{I('apellidos', 'Apellidos')}{I('password', 'Contraseña inicial', { type: 'password', autoComplete: 'new-password' })}
            {f.tipo === 'MEDICO' && <>{I('numeroColegiatura', 'N.° de colegiatura')}<Col md={6}><Campo id="u-esp" etiqueta="Especialidad" error={errores.especialidadId}><select id="u-esp" className="form-select" value={f.especialidadId} onChange={(e) => setF({ ...f, especialidadId: e.target.value })}><option value="">Seleccione…</option>{especialidades?.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}</select></Campo></Col></>}</Row>
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setModal(false)}>Cancelar</Button><Button className="btn-azul" onClick={crear}>Crear usuario</Button></Modal.Footer>
      </Modal>
    </>
  );
}

// ------------------------------------------------------------ Configuración: especialidades, consultorios y turnos (RF-34)
export function Configuracion() {
  const toast = useToast();
  const [esp, , recEsp] = useCarga(() => api.get('/admin/especialidades').then((r) => r.data), []);
  const [con, , recCon] = useCarga(() => api.get('/admin/consultorios').then((r) => r.data), []);
  const [tur, , recTur] = useCarga(() => api.get('/turnos').then((r) => r.data), []);
  const [e, setE] = useState(null);
  const [c, setC] = useState(null);
  const [errores, setErrores] = useState({});

  const guardarEsp = async () => {
    if (!e.nombre?.trim()) return setErrores({ nombre: 'Ingrese el nombre de la especialidad' });
    try { const body = { ...e, precioConsulta: Number(e.precioConsulta || 0) }; e.id ? await api.put(`/admin/especialidades/${e.id}`, body) : await api.post('/admin/especialidades', body); toast('Especialidad guardada'); setE(null); recEsp(); }
    catch (x) { setErrores(fieldErrors(x)); toast(msg(x), 'error'); }
  };
  const guardarCon = async () => {
    const errs = {};
    if (!c.numeroPabellon?.trim()) errs.numeroPabellon = 'Ingrese el número de pabellón';
    if (!c.codigoRoom?.trim()) errs.codigoRoom = 'Ingrese el código de room';
    setErrores(errs);
    if (Object.keys(errs).length) return;
    try { c.id ? await api.put(`/admin/consultorios/${c.id}`, c) : await api.post('/admin/consultorios', c); toast('Consultorio guardado'); setC(null); recCon(); }
    catch (x) { setErrores(fieldErrors(x)); toast(msg(x), 'error'); }
  };
  const guardarTurno = async (t) => {
    try { await api.put('/admin/turnos', { ...t, duracionMinutos: Number(t.duracionMinutos) }); toast('Turno actualizado'); recTur(); } catch (x) { toast(msg(x), 'error'); }
  };

  return (
    <>
      <Titulo icono="sliders">Configuración</Titulo>
      <div className="caja">
        <Tabs defaultActiveKey="esp" className="mb-3">
          <Tab eventKey="esp" title="Especialidades">
            <div className="text-end mb-2"><Button className="btn-azul" size="sm" onClick={() => { setErrores({}); setE({ nombre: '', descripcion: '', precioConsulta: 0, activo: true }); }}>Nueva especialidad</Button></div>
            <table className="table align-middle"><thead><tr><th>Nombre</th><th>Descripción</th><th className="text-end">Tarifa</th><th>Estado</th><th /></tr></thead>
              <tbody>{esp?.map((x) => <tr key={x.id}><td className="fw-semibold">{x.nombre}</td><td className="small">{x.descripcion}</td><td className="text-end">{money(x.precioConsulta)}</td><td>{x.activo ? <Badge bg="success">Activa</Badge> : <Badge bg="secondary">Inactiva</Badge>}</td><td className="text-end"><Button size="sm" variant="outline-primary" onClick={() => { setErrores({}); setE(x); }}>Editar</Button></td></tr>)}</tbody></table>
          </Tab>
          <Tab eventKey="con" title="Consultorios">
            <div className="text-end mb-2"><Button className="btn-azul" size="sm" onClick={() => { setErrores({}); setC({ numeroPabellon: '', codigoRoom: '', activo: true }); }}>Nuevo consultorio</Button></div>
            <table className="table align-middle"><thead><tr><th>Pabellón</th><th>Room</th><th>Estado</th><th /></tr></thead>
              <tbody>{con?.map((x) => <tr key={x.id}><td>{x.numeroPabellon}</td><td>{x.codigoRoom}</td><td>{x.activo ? <Badge bg="success">Activo</Badge> : <Badge bg="secondary">Inactivo</Badge>}</td><td className="text-end"><Button size="sm" variant="outline-primary" onClick={() => { setErrores({}); setC(x); }}>Editar</Button></td></tr>)}</tbody></table>
          </Tab>
          <Tab eventKey="tur" title="Turnos">
            <p className="text-muted small">Define la ventana horaria y la duración de cada cita al generar horarios.</p>
            {tur?.map((t) => <TurnoFila key={t.turno} t={t} onGuardar={guardarTurno} />)}
          </Tab>
        </Tabs>
      </div>

      <Modal show={!!e} onHide={() => setE(null)} centered>
        <Modal.Header closeButton><Modal.Title>{e?.id ? 'Editar' : 'Nueva'} especialidad</Modal.Title></Modal.Header>
        <Modal.Body>{e && <>
          <Campo id="e-n" etiqueta="Nombre" error={errores.nombre}><input id="e-n" className="form-control" value={e.nombre} onChange={(x) => setE({ ...e, nombre: x.target.value })} /></Campo>
          <Campo id="e-d" etiqueta="Descripción" error={errores.descripcion}><textarea id="e-d" rows={3} className="form-control" value={e.descripcion || ''} onChange={(x) => setE({ ...e, descripcion: x.target.value })} /></Campo>
          <Campo id="e-p" etiqueta="Tarifa de la consulta (S/)" error={errores.precioConsulta} ayuda="Use 0 si la consulta no tiene costo."><input id="e-p" type="number" min="0" step="0.50" className="form-control" value={e.precioConsulta ?? 0} onChange={(x) => setE({ ...e, precioConsulta: x.target.value })} /></Campo>
          <div className="form-check"><input id="e-a" type="checkbox" className="form-check-input" checked={e.activo} onChange={(x) => setE({ ...e, activo: x.target.checked })} /><label htmlFor="e-a" className="form-check-label">Activa (visible para reservar)</label></div></>}</Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setE(null)}>Cancelar</Button><Button className="btn-azul" onClick={guardarEsp}>Guardar</Button></Modal.Footer>
      </Modal>
      <Modal show={!!c} onHide={() => setC(null)} centered>
        <Modal.Header closeButton><Modal.Title>{c?.id ? 'Editar' : 'Nuevo'} consultorio</Modal.Title></Modal.Header>
        <Modal.Body>{c && <>
          <Campo id="c-p" etiqueta="Número de pabellón" error={errores.numeroPabellon}><input id="c-p" className="form-control" value={c.numeroPabellon} onChange={(x) => setC({ ...c, numeroPabellon: x.target.value })} /></Campo>
          <Campo id="c-r" etiqueta="Código de room" error={errores.codigoRoom}><input id="c-r" className="form-control" value={c.codigoRoom} onChange={(x) => setC({ ...c, codigoRoom: x.target.value })} /></Campo>
          <div className="form-check"><input id="c-a" type="checkbox" className="form-check-input" checked={c.activo} onChange={(x) => setC({ ...c, activo: x.target.checked })} /><label htmlFor="c-a" className="form-check-label">Activo</label></div></>}</Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setC(null)}>Cancelar</Button><Button className="btn-azul" onClick={guardarCon}>Guardar</Button></Modal.Footer>
      </Modal>
    </>
  );
}

function TurnoFila({ t, onGuardar }) {
  const [v, setV] = useState({ ...t, horaInicio: t.horaInicio.substring(0, 5), horaFin: t.horaFin.substring(0, 5) });
  return (
    <div className="row g-2 align-items-end mb-3">
      <div className="col-md-2"><strong>{t.turno === 'MANANA' ? 'Mañana' : 'Tarde'}</strong></div>
      <div className="col-md-3"><label className="form-label small" htmlFor={`ti-${t.turno}`}>Inicio</label><input id={`ti-${t.turno}`} type="time" className="form-control" value={v.horaInicio} onChange={(e) => setV({ ...v, horaInicio: e.target.value })} /></div>
      <div className="col-md-3"><label className="form-label small" htmlFor={`tf-${t.turno}`}>Fin</label><input id={`tf-${t.turno}`} type="time" className="form-control" value={v.horaFin} onChange={(e) => setV({ ...v, horaFin: e.target.value })} /></div>
      <div className="col-md-2"><label className="form-label small" htmlFor={`td-${t.turno}`}>Duración (min)</label><input id={`td-${t.turno}`} type="number" min="10" max="120" className="form-control" value={v.duracionMinutos} onChange={(e) => setV({ ...v, duracionMinutos: e.target.value })} /></div>
      <div className="col-md-2"><Button className="btn-azul w-100" onClick={() => onGuardar(v)}>Guardar</Button></div>
    </div>
  );
}

// ------------------------------------------------------------ Reportes (RF-32, RF-33)
export function Reportes() {
  const [desde, setDesde] = useState(sumarDias(hoyISO(), -30));
  const [hasta, setHasta] = useState(hoyISO());
  const [aplicado, setAplicado] = useState({ desde, hasta });
  const params = aplicado;
  const [rep, cargando] = useCarga(() => api.get('/admin/reportes/citas', { params }).then((r) => r.data), [aplicado]);
  const [ina] = useCarga(() => api.get('/admin/reportes/inasistencia', { params }).then((r) => r.data), [aplicado]);
  const [ing] = useCarga(() => api.get('/pagos/resumen', { params }).then((r) => r.data).catch(() => null), [aplicado]);
  const toast = useToast();
  const pdf = async () => { try { (await import('../../pdf')).reporteAdmin({ ...aplicado, rep, ina, ing }); } catch { toast('No se pudo generar el PDF', 'error'); } };
  const csv = async () => {
    const m = await import('../../pdf');
    const filas = [
      ...Object.entries(rep?.porEstado || {}).map(([k, v]) => ['Citas por estado', k, v, '']),
      ...(ina || []).map((i) => ['Inasistencia', i.especialidad, i.noAsistio, `${i.porcentaje}%`]),
      ...Object.entries(ing?.porMetodo || {}).map(([k, v]) => ['Ingresos por método', k, '', Number(v).toFixed(2)]),
      ...(ing?.porDia || []).map((d) => ['Ingresos por día', d.fecha, '', Number(d.monto).toFixed(2)]),
    ];
    m.descargarCSV(`reporte-${aplicado.desde}_${aplicado.hasta}.csv`, ['Sección', 'Concepto', 'Cantidad', 'Valor'], filas);
  };
  const max = Math.max(1, ...Object.values(rep?.porEstado || { x: 1 }));
  const maxIna = Math.max(1, ...(ina || []).map((i) => i.porcentaje), 1);

  return (
    <>
      <Titulo icono="bar-chart-line" acciones={<><Button variant="outline-secondary" className="me-2" disabled={!rep} onClick={csv}><i className="bi bi-filetype-csv me-1" />CSV</Button><Button className="btn-azul" disabled={!rep} onClick={pdf}><i className="bi bi-file-earmark-pdf me-1" />PDF</Button></>}>Reportes</Titulo>
      <div className="caja mb-3"><form className="row g-3 align-items-end" onSubmit={(e) => { e.preventDefault(); if (hasta >= desde) setAplicado({ desde, hasta }); }}>
        <div className="col-md-4"><label htmlFor="rd" className="form-label">Desde</label><input id="rd" type="date" className="form-control" value={desde} onChange={(e) => setDesde(e.target.value)} /></div>
        <div className="col-md-4"><label htmlFor="rh" className="form-label">Hasta</label><input id="rh" type="date" className="form-control" value={hasta} min={desde} onChange={(e) => setHasta(e.target.value)} /></div>
        <div className="col-md-4"><Button type="submit" className="btn-azul w-100">Generar reporte</Button></div></form></div>

      {cargando && !rep ? <Cargando /> : rep && (
        <Row className="g-3">
          <Col lg={6}><div className="caja h-100">
            <h2 className="h5">Citas por estado ({rep.total} en total)</h2>
            <Row className="text-center mb-3 g-2">{[['Atendidas', rep.atendidas], ['Canceladas', rep.canceladas], ['Reprogramadas', rep.reprogramadas]].map(([t, v]) => <Col key={t}><div className="stat caja py-2"><div className="num fs-3">{v}</div><div className="small text-muted">{t}</div></div></Col>)}</Row>
            <table className="table table-sm align-middle mb-0"><tbody>{Object.entries(rep.porEstado).map(([k, v]) => (
              <tr key={k}><td style={{ width: 130 }} className="small">{k.replace('_', ' ')}</td><td><div className="barra" style={{ width: `${(v / max) * 100}%` }} /></td><td className="text-end fw-bold" style={{ width: 50 }}>{v}</td></tr>))}</tbody></table>
          </div></Col>
          <Col lg={6}><div className="caja h-100">
            <h2 className="h5">Inasistencia por especialidad</h2>
            {!ina?.length ? <Vacio icono="bar-chart">Sin datos en el periodo.</Vacio> : (<>
              <table className="table table-sm"><thead><tr><th>Especialidad</th><th className="text-end">Citas</th><th className="text-end">No asistió</th><th className="text-end">%</th></tr></thead>
                <tbody>{ina.map((i) => <tr key={i.especialidad}><td>{i.especialidad}</td><td className="text-end">{i.total}</td><td className="text-end">{i.noAsistio}</td><td className="text-end fw-bold">{i.porcentaje}%</td></tr>)}</tbody></table>
              {ina.map((i) => <div key={i.especialidad} className="mb-2"><div className="small">{i.especialidad}</div><div className="barra" style={{ width: `${(i.porcentaje / maxIna) * 100}%`, background: '#d9534f' }} /></div>)}
            </>)}
          </div></Col>
        </Row>
      )}
    </>
  );
}

// ------------------------------------------------------------ Pagos e ingresos
export function Pagos() {
  const toast = useToast();
  const [desde, setDesde] = useState(sumarDias(hoyISO(), -30));
  const [hasta, setHasta] = useState(hoyISO());
  const [tipo, setTipo] = useState('');
  const [metodo, setMetodo] = useState('');
  const [pagina, setPagina] = useState(0);
  const p = { desde, hasta, ...(tipo && { tipo }), ...(metodo && { metodo }) };
  const [res, , recRes] = useCarga(() => api.get('/pagos/resumen', { params: { desde, hasta } }).then((r) => r.data), [desde, hasta]);
  const [lista, cargando, recargar] = useCarga(() => api.get('/pagos', { params: { ...p, pagina, tamanio: 10 } }).then((r) => r.data), [desde, hasta, tipo, metodo, pagina]);
  const [aAnular, setAAnular] = useState(null);
  const [motivo, setMotivo] = useState('');
  const maxDia = Math.max(1, ...(res?.porDia || []).map((d) => Number(d.monto)));

  const anular = async () => {
    try { await api.patch(`/pagos/${aAnular.idPago}/anular`, { motivo }); toast('Pago anulado'); setAAnular(null); setMotivo(''); recargar(); recRes(); }
    catch (e) { toast(msg(e), 'error'); }
  };
  const boleta = async (x) => { try { (await import('../../pdf')).boletaConsulta(x); } catch { toast('No se pudo generar la boleta', 'error'); } };
  const csv = async () => {
    try {
      const { data } = await api.get('/pagos', { params: { ...p, pagina: 0, tamanio: 100 } });
      (await import('../../pdf')).descargarCSV(`pagos-${desde}_${hasta}.csv`, ['Fecha', 'Comprobante', 'Tipo', 'Paciente', 'Documento', 'Método', 'Estado', 'Monto'],
        data.contenido.map((x) => [x.fecha, x.comprobante, x.tipo, x.paciente, x.pacienteDocumento, x.metodo, x.estado, Number(x.monto).toFixed(2)]));
    } catch (e) { toast(msg(e), 'error'); }
  };

  return (
    <>
      <Titulo icono="cash-stack" acciones={<Button variant="outline-secondary" onClick={csv}><i className="bi bi-filetype-csv me-1" />Exportar CSV</Button>}>Pagos e ingresos</Titulo>
      <div className="caja mb-3"><div className="row g-3">
        <div className="col-6 col-md-3"><label htmlFor="pd" className="form-label">Desde</label><input id="pd" type="date" className="form-control" value={desde} max={hasta} onChange={(e) => { setPagina(0); setDesde(e.target.value); }} /></div>
        <div className="col-6 col-md-3"><label htmlFor="ph" className="form-label">Hasta</label><input id="ph" type="date" className="form-control" value={hasta} min={desde} onChange={(e) => { setPagina(0); setHasta(e.target.value); }} /></div>
        <div className="col-6 col-md-3"><label htmlFor="pt" className="form-label">Concepto</label><select id="pt" className="form-select" value={tipo} onChange={(e) => { setPagina(0); setTipo(e.target.value); }}><option value="">Todos</option><option value="CONSULTA">Consultas</option><option value="FARMACIA">Farmacia</option></select></div>
        <div className="col-6 col-md-3"><label htmlFor="pm" className="form-label">Método</label><select id="pm" className="form-select" value={metodo} onChange={(e) => { setPagina(0); setMetodo(e.target.value); }}><option value="">Todos</option><option value="TARJETA">Tarjeta</option><option value="YAPE">Yape</option><option value="PLIN">Plin</option><option value="EFECTIVO">Efectivo</option></select></div>
      </div></div>

      {res && (
        <Row className="g-3 mb-3">
          <Col sm={6} lg={3}><div className="caja stat h-100"><div className="small text-muted">Ingresos netos</div><div className="num">{money(res.total)}</div></div></Col>
          <Col sm={6} lg={3}><div className="caja stat h-100"><div className="small text-muted">Pagos / anulados</div><div className="num">{res.cantidadPagos} / {res.anulados}</div></div></Col>
          <Col lg={3}><div className="caja h-100"><div className="small text-muted mb-1">Por concepto</div>{Object.entries(res.porTipo).map(([k, v]) => <div key={k} className="d-flex justify-content-between"><span>{k === 'CONSULTA' ? 'Consultas' : 'Farmacia'}</span><strong>{money(v)}</strong></div>)}</div></Col>
          <Col lg={3}><div className="caja h-100"><div className="small text-muted mb-1">Por método</div>{Object.entries(res.porMetodo).map(([k, v]) => <div key={k} className="d-flex justify-content-between"><span>{k}</span><strong>{money(v)}</strong></div>)}</div></Col>
        </Row>
      )}
      {res?.porDia?.length > 0 && (
        <div className="caja mb-3"><h2 className="h6">Ingresos por día</h2>
          {res.porDia.map((d) => <div key={d.fecha} className="d-flex align-items-center gap-2 mb-1"><span className="small" style={{ width: 90 }}>{d.fecha}</span><div className="barra flex-grow-1" style={{ maxWidth: `${(Number(d.monto) / maxDia) * 100}%` }} /><strong className="small">{money(d.monto)}</strong></div>)}
        </div>
      )}

      <div className="caja">
        {cargando && !lista ? <Cargando /> : !lista?.contenido.length ? <Vacio icono="receipt">No hay pagos con esos filtros.</Vacio> : (<>
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr><th>Fecha</th><th>Comprobante</th><th>Paciente</th><th>Concepto</th><th>Método</th><th className="text-end">Monto</th><th>Estado</th><th /></tr></thead>
            <tbody>{lista.contenido.map((x) => (
              <tr key={x.idPago}><td>{fmtFechaHora(x.fecha)}</td><td className="text-nowrap">{x.comprobante}</td><td>{x.paciente}</td><td className="small">{x.concepto}</td><td>{x.metodo}</td><td className="text-end">{money(x.monto)}</td>
                <td><span className={`badge ${x.estado === 'PAGADO' ? 'bg-success' : 'bg-secondary'}`}>{x.estado === 'PAGADO' ? 'Pagado' : 'Anulado'}</span></td>
                <td className="text-end text-nowrap">{x.tipo === 'CONSULTA' && <Button size="sm" variant="outline-secondary" className="me-2" onClick={() => boleta(x)}><i className="bi bi-file-earmark-pdf" /></Button>}
                  {x.estado === 'PAGADO' && x.tipo === 'CONSULTA' && <Button size="sm" variant="outline-danger" onClick={() => setAAnular(x)}>Anular</Button>}</td></tr>))}</tbody>
          </table></div>
          <Paginador pagina={lista.pagina} totalPaginas={lista.totalPaginas} onCambio={setPagina} />
        </>)}
      </div>
      <Modal show={!!aAnular} onHide={() => setAAnular(null)} centered>
        <Modal.Header closeButton><Modal.Title>Anular pago {aAnular?.comprobante}</Modal.Title></Modal.Header>
        <Modal.Body><Campo id="pam" etiqueta="Motivo de la anulación"><textarea id="pam" rows={3} maxLength={250} className="form-control" value={motivo} onChange={(e) => setMotivo(e.target.value)} /></Campo></Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={() => setAAnular(null)}>Cancelar</Button><Button variant="danger" disabled={!motivo.trim()} onClick={anular}>Anular pago</Button></Modal.Footer>
      </Modal>
    </>
  );
}
