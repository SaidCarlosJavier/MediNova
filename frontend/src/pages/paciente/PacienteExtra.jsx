import { useState } from 'react';
import { Accordion, Alert, Badge, Button, Col, Modal, Row } from 'react-bootstrap';
import { useAuth } from '../../AuthContext';
import { api, fieldErrors, msg } from '../../api';
import {
  Campo, Cargando, EstadoBadge, Paginador, Titulo, Vacio, fmtFecha, fmtFechaHora, reglas, useCarga, useToast, validar,
} from '../../components/ui';
import { SinPaciente } from './PacienteCitas';
import { money } from '../../components/Pago';

// ------------------------------------------------------------ Historial clínico en modo lectura (paciente / tutor)
export function HistorialPaciente({ pacienteId, vacioTexto = 'Todavía no hay atenciones registradas.' }) {
  const [lista, cargando] = useCarga(() => api.get(`/historial/paciente/${pacienteId}`).then((r) => r.data), [pacienteId]);
  if (cargando) return <Cargando />;
  if (!lista?.length) return <Vacio icono="journal">{vacioTexto}</Vacio>;
  return (
    <Accordion alwaysOpen defaultActiveKey={[String(lista[0].idHistorial)]}>
      {lista.map((h) => (
        <Accordion.Item eventKey={String(h.idHistorial)} key={h.idHistorial}>
          <Accordion.Header>
            <span className="me-3 fw-bold">{fmtFecha(h.fechaCita)}</span> {h.especialidad} — {h.medico}
          </Accordion.Header>
          <Accordion.Body>
            {h.triaje && (
              <Alert variant="light" className="border small">
                <strong>Triaje:</strong> P.A. {h.triaje.presionArterial || '—'} · Peso {h.triaje.peso ?? '—'} kg · {h.triaje.signosVitales || 'sin otros signos'}
              </Alert>
            )}
            <p className="mb-1"><strong>Diagnóstico:</strong> {h.diagnostico}</p>
            {h.tratamiento && <p className="mb-1"><strong>Tratamiento:</strong> {h.tratamiento}</p>}
            {h.observaciones && <p className="mb-1"><strong>Observaciones:</strong> {h.observaciones}</p>}
            {h.receta && (
              <div className="mt-3">
                <div className="d-flex align-items-center gap-2 mb-2"><strong>Receta</strong><EstadoBadge estado={h.receta.estado} /></div>
                {h.receta.indicaciones && <p className="small text-muted">{h.receta.indicaciones}</p>}
                <ul className="mb-0">
                  {h.receta.detalles.map((d) => (
                    <li key={d.idDetalle}>{d.medicamento} ({d.presentacion}) — {d.dosis} · cant. {d.cantidad}{d.cantidadDispensada > 0 && <> · entregado {d.cantidadDispensada}</>}</li>
                  ))}
                </ul>
                <div className="small mt-2"><i className="bi bi-capsule me-1" />Puede recoger sus medicamentos en la farmacia del hospital.</div>
              </div>
            )}
          </Accordion.Body>
        </Accordion.Item>
      ))}
    </Accordion>
  );
}

export function Historial() {
  const { pacienteActivo } = useAuth();
  return (
    <>
      <Titulo icono="journal-medical">Historial de atenciones</Titulo>
      <p className="text-muted"><i className="bi bi-shield-lock me-1" />Información confidencial (Ley N.° 29733). Solo lectura.</p>
      {!pacienteActivo ? <SinPaciente /> : <div className="caja"><HistorialPaciente pacienteId={pacienteActivo.id} /></div>}
    </>
  );
}

// ------------------------------------------------------------ Perfil (RF-04)
export function Perfil() {
  const { usuario, actualizarUsuario } = useAuth();
  const toast = useToast();
  const esTutor = usuario.rol === 'ROLE_TUTOR';
  const [f, setF] = useState({
    nombres: usuario.nombres || '', apellidos: usuario.apellidos || '', tipoSeguro: usuario.tipoSeguro || '',
    telefonoContacto: usuario.telefonoContacto || '', fechaNacimiento: usuario.fechaNacimiento || '',
    parentesco: usuario.parentesco || '', telefono: usuario.telefono || '', direccion: usuario.direccion || '', nuevaPassword: '',
  });
  const [errores, setErrores] = useState({});
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const guardar = async (e) => {
    e.preventDefault();
    const esq = { nombres: [(v) => reglas.requerido(v, 'Ingrese sus nombres')], apellidos: [(v) => reglas.requerido(v, 'Ingrese sus apellidos')], telefonoContacto: [reglas.telefono], telefono: [reglas.telefono], fechaNacimiento: [reglas.pasada] };
    if (f.nuevaPassword) esq.nuevaPassword = [reglas.password];
    const errs = validar(f, esq);
    setErrores(errs);
    if (Object.keys(errs).length) return;
    try {
      const { data } = await api.put('/auth/perfil', { ...f, fechaNacimiento: f.fechaNacimiento || null, nuevaPassword: f.nuevaPassword || null });
      actualizarUsuario(data);
      setF({ ...f, nuevaPassword: '' });
      toast('Perfil actualizado correctamente');
    } catch (ex) { setErrores(fieldErrors(ex)); toast(msg(ex), 'error'); }
  };

  const I = (k, etiqueta, extra = {}) => (
    <Col md={6}><Campo id={k} etiqueta={etiqueta} error={errores[k]}><input id={k} className="form-control" value={f[k]} onChange={set(k)} {...extra} /></Campo></Col>
  );

  return (
    <>
      <Titulo icono="person">Mi perfil</Titulo>
      <div className="caja" style={{ maxWidth: 820 }}>
        <p className="text-muted">Documento: <strong>{usuario.documentoIdentidad}</strong> · Correo: <strong>{usuario.correo}</strong></p>
        <form onSubmit={guardar} noValidate>
          <Row>
            {I('nombres', 'Nombres')}{I('apellidos', 'Apellidos')}
            {esTutor ? (<>{I('parentesco', 'Parentesco')}{I('telefono', 'Teléfono')}<Col md={12}><Campo id="direccion" etiqueta="Dirección"><input id="direccion" className="form-control" value={f.direccion} onChange={set('direccion')} /></Campo></Col></>) : (
              <>
                {I('telefonoContacto', 'Teléfono de contacto')}{I('fechaNacimiento', 'Fecha de nacimiento', { type: 'date' })}
                <Col md={6}><Campo id="tipoSeguro" etiqueta="Tipo de seguro"><select id="tipoSeguro" className="form-select" value={f.tipoSeguro} onChange={set('tipoSeguro')}><option value="">Seleccione…</option><option>SIS</option><option>EsSalud</option><option>Particular</option><option>Otro</option></select></Campo></Col>
              </>
            )}
            {I('nuevaPassword', 'Nueva contraseña (opcional)', { type: 'password', autoComplete: 'new-password' })}
          </Row>
          <Button type="submit" className="btn-azul">Guardar cambios</Button>
        </form>
      </div>
    </>
  );
}

// ------------------------------------------------------------ Notificaciones (RF-11, 12, 13)
const ICONO = { CONFIRMACION: 'check-circle text-success', RECORDATORIO: 'alarm text-warning', REPROGRAMACION: 'arrow-repeat text-primary', CANCELACION: 'x-circle text-danger', LISTA_ESPERA: 'hourglass-split text-info', PAGO: 'credit-card text-success' };

export function Notificaciones() {
  const toast = useToast();
  const [pagina, setPagina] = useState(0);
  const [datos, cargando, recargar] = useCarga(() => api.get('/notificaciones', { params: { pagina, tamanio: 10 } }).then((r) => r.data), [pagina]);

  const leer = async (n) => { if (!n.leido) { await api.patch(`/notificaciones/${n.id}/leida`).catch(() => {}); recargar(); } };
  const todas = async () => { try { await api.patch('/notificaciones/leer-todas'); toast('Todas marcadas como leídas'); recargar(); } catch (e) { toast(msg(e), 'error'); } };

  return (
    <>
      <Titulo icono="bell" acciones={<Button variant="outline-primary" onClick={todas}>Marcar todas como leídas</Button>}>Notificaciones</Titulo>
      <div className="caja">
        {cargando && !datos ? <Cargando /> : !datos?.contenido.length ? <Vacio icono="bell-slash">No tiene notificaciones.</Vacio> : (
          <ul className="list-group list-group-flush">
            {datos.contenido.map((n) => (
              <li key={n.id} className={`list-group-item d-flex gap-3 px-1 ${n.leido ? '' : 'bg-light fw-semibold'}`} role="button" onClick={() => leer(n)}>
                <i className={`bi bi-${ICONO[n.tipo] || 'bell'} fs-4`} />
                <div className="flex-grow-1">{n.mensaje}<div className="small text-muted fw-normal">{fmtFechaHora(n.fechaEnvio)}</div></div>
                {!n.leido && <Badge bg="primary" className="align-self-start">Nueva</Badge>}
              </li>
            ))}
          </ul>
        )}
        {datos && <Paginador pagina={datos.pagina} totalPaginas={datos.totalPaginas} onCambio={setPagina} />}
      </div>
    </>
  );
}

// ------------------------------------------------------------ Tutor: pacientes a cargo + información del rol
const PAC_VACIO = { documentoIdentidad: '', nombres: '', apellidos: '', correo: '', fechaNacimiento: '', tipoSeguro: '', telefonoContacto: '', consentimiento: false };

export function PacientesACargo() {
  const { aCargo, cargarACargo, setPacienteId } = useAuth();
  const toast = useToast();
  const [modal, setModal] = useState(null); // 'nuevo' | 'vincular'
  const [f, setF] = useState(PAC_VACIO);
  const [v, setV] = useState({ documentoIdentidad: '', fechaNacimiento: '' });
  const [errores, setErrores] = useState({});
  const [ocupado, setOcupado] = useState(false);

  const cerrar = () => { setModal(null); setErrores({}); setF(PAC_VACIO); setV({ documentoIdentidad: '', fechaNacimiento: '' }); };

  const registrar = async () => {
    const errs = validar(f, {
      documentoIdentidad: [reglas.documento], nombres: [(x) => reglas.requerido(x, 'Ingrese los nombres')], apellidos: [(x) => reglas.requerido(x, 'Ingrese los apellidos')],
      fechaNacimiento: [(x) => reglas.requerido(x, 'Ingrese la fecha de nacimiento'), reglas.pasada], telefonoContacto: [reglas.telefono],
      consentimiento: [(x) => (x ? null : 'Debe registrar el consentimiento informado (Ley N.° 29733)')],
    });
    setErrores(errs);
    if (Object.keys(errs).length) return;
    setOcupado(true);
    try {
      const { data } = await api.post('/pacientes', { ...f, correo: f.correo || null, requiereTutor: true });
      await cargarACargo(); setPacienteId(data.paciente.id); toast('Paciente registrado y vinculado a su cuenta'); cerrar();
    } catch (e) { setErrores(fieldErrors(e)); toast(msg(e), 'error'); } finally { setOcupado(false); }
  };

  const vincular = async () => {
    const errs = validar(v, { documentoIdentidad: [(x) => reglas.requerido(x, 'Ingrese el documento')], fechaNacimiento: [(x) => reglas.requerido(x, 'Ingrese la fecha de nacimiento')] });
    setErrores(errs);
    if (Object.keys(errs).length) return;
    setOcupado(true);
    try {
      const { data } = await api.post('/pacientes/vincular', v);
      await cargarACargo(); setPacienteId(data.id); toast('Paciente vinculado a su cuenta'); cerrar();
    } catch (e) { setErrores(fieldErrors(e)); toast(msg(e), 'error'); } finally { setOcupado(false); }
  };

  const I = (k, etiqueta, extra = {}) => (
    <Col md={6}><Campo id={`n-${k}`} etiqueta={etiqueta} error={errores[k]}><input id={`n-${k}`} className="form-control" value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} {...extra} /></Campo></Col>
  );

  return (
    <>
      <Titulo icono="people" acciones={<>
        <Button variant="outline-primary" onClick={() => setModal('vincular')}><i className="bi bi-link-45deg me-1" />Vincular existente</Button>
        <Button className="btn-azul" onClick={() => setModal('nuevo')}><i className="bi bi-person-plus me-1" />Registrar paciente</Button>
      </>}>Pacientes a mi cargo</Titulo>

      <div className="caja mb-4">
        {!aCargo.length ? <Vacio icono="person-plus">Todavía no tiene pacientes a su cargo.</Vacio> : (
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr><th>Paciente</th><th>Documento</th><th>Nacimiento</th><th>Seguro</th><th /></tr></thead>
            <tbody>{aCargo.map((p) => (
              <tr key={p.id}><td className="fw-semibold">{p.nombreCompleto}</td><td>{p.documentoIdentidad}</td><td>{fmtFecha(p.fechaNacimiento)}</td><td>{p.tipoSeguro || '—'}</td>
                <td className="text-end"><Button size="sm" variant="outline-primary" onClick={() => { setPacienteId(p.id); toast(`Ahora actúa en nombre de ${p.nombreCompleto}`); }}>Actuar en su nombre</Button></td></tr>
            ))}</tbody>
          </table></div>
        )}
      </div>

      <div className="caja" id="rol-tutor">
        <h2 className="h5"><i className="bi bi-info-circle me-2" />¿Qué puede hacer como tutor / apoderado?</h2>
        <ul className="mb-2">
          <li>Reservar, reprogramar y cancelar citas en nombre del paciente (use el selector «Actuar en nombre de» de la parte superior).</li>
          <li>Recibir las notificaciones de confirmación, recordatorio, reprogramación y cancelación.</li>
          <li>Consultar el historial de atenciones y recetas, solo en modo lectura.</li>
        </ul>
        <p className="small text-muted mb-0">Solo debe actuar por pacientes de quienes es familiar o responsable legal. Sus acciones quedan registradas con su nombre.</p>
      </div>

      <Modal show={modal === 'nuevo'} onHide={cerrar} size="lg" centered>
        <Modal.Header closeButton><Modal.Title>Registrar paciente a mi cargo</Modal.Title></Modal.Header>
        <Modal.Body>
          <Row>
            {I('documentoIdentidad', 'Documento de identidad', { maxLength: 12 })}{I('fechaNacimiento', 'Fecha de nacimiento', { type: 'date' })}
            {I('nombres', 'Nombres')}{I('apellidos', 'Apellidos')}
            {I('telefonoContacto', 'Teléfono de contacto')}
            <Col md={6}><Campo id="n-seg" etiqueta="Tipo de seguro"><select id="n-seg" className="form-select" value={f.tipoSeguro} onChange={(e) => setF({ ...f, tipoSeguro: e.target.value })}><option value="">Seleccione…</option><option>SIS</option><option>EsSalud</option><option>Particular</option><option>Otro</option></select></Campo></Col>
          </Row>
          <div className="form-check">
            <input id="n-cons" type="checkbox" className="form-check-input" checked={f.consentimiento} onChange={(e) => setF({ ...f, consentimiento: e.target.checked })} />
            <label htmlFor="n-cons" className="form-check-label small">Declaro ser el familiar o responsable legal y doy el consentimiento informado para el tratamiento de los datos personales del paciente (Ley N.° 29733).</label>
          </div>
          {errores.consentimiento && <div className="text-danger small mt-1">{errores.consentimiento}</div>}
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={cerrar}>Cancelar</Button><Button className="btn-azul" disabled={ocupado} onClick={registrar}>Registrar</Button></Modal.Footer>
      </Modal>

      <Modal show={modal === 'vincular'} onHide={cerrar} centered>
        <Modal.Header closeButton><Modal.Title>Vincular paciente existente</Modal.Title></Modal.Header>
        <Modal.Body>
          <p className="text-muted small">Por seguridad, ingrese el documento y la fecha de nacimiento del paciente. Solo se pueden vincular pacientes que aún no tienen tutor.</p>
          <Campo id="v-doc" etiqueta="Documento del paciente" error={errores.documentoIdentidad}><input id="v-doc" className="form-control" value={v.documentoIdentidad} onChange={(e) => setV({ ...v, documentoIdentidad: e.target.value })} /></Campo>
          <Campo id="v-nac" etiqueta="Fecha de nacimiento del paciente" error={errores.fechaNacimiento}><input id="v-nac" type="date" className="form-control" value={v.fechaNacimiento} onChange={(e) => setV({ ...v, fechaNacimiento: e.target.value })} /></Campo>
        </Modal.Body>
        <Modal.Footer><Button variant="outline-secondary" onClick={cerrar}>Cancelar</Button><Button className="btn-azul" disabled={ocupado} onClick={vincular}>Vincular</Button></Modal.Footer>
      </Modal>
    </>
  );
}

// ------------------------------------------------------------ Mis pagos (boletas de consulta y farmacia)
export function MisPagos() {
  const { pacienteActivo } = useAuth();
  const toast = useToast();
  const [lista, cargando] = useCarga(() => (pacienteActivo ? api.get(`/pagos/paciente/${pacienteActivo.id}`).then((r) => r.data) : Promise.resolve([])), [pacienteActivo?.id]);

  const boleta = async (p) => {
    try {
      const m = await import('../../pdf');
      if (p.tipo === 'FARMACIA') {
        const { data } = await api.get(`/farmacia/dispensaciones/${p.dispensacionId}`).catch(() => ({ data: null }));
        if (data) return m.boletaFarmacia(data);
      }
      return m.boletaConsulta(p);
    } catch { toast('No se pudo generar la boleta', 'error'); }
  };
  const total = (lista || []).filter((p) => p.estado === 'PAGADO').reduce((a, p) => a + Number(p.monto), 0);

  return (
    <>
      <Titulo icono="credit-card">Mis pagos</Titulo>
      {!pacienteActivo ? <SinPaciente /> : cargando ? <Cargando /> : !lista?.length ? <Vacio icono="receipt">Aún no tiene pagos registrados. Puede pagar su consulta desde «Mis citas».</Vacio> : (
        <div className="caja">
          <p className="text-muted">Total pagado: <strong>{money(total)}</strong></p>
          <div className="table-responsive"><table className="table align-middle mb-0">
            <thead><tr><th>Fecha</th><th>Comprobante</th><th>Concepto</th><th>Método</th><th className="text-end">Monto</th><th>Estado</th><th /></tr></thead>
            <tbody>{lista.map((p) => (
              <tr key={p.idPago}>
                <td>{fmtFechaHora(p.fecha)}</td><td className="text-nowrap">{p.comprobante}</td><td>{p.concepto}</td>
                <td>{p.metodo}{p.tarjetaUltimos4 && <span className="text-muted small"> ·· {p.tarjetaUltimos4}</span>}</td>
                <td className="text-end">{money(p.monto)}</td>
                <td><span className={`badge ${p.estado === 'PAGADO' ? 'bg-success' : 'bg-secondary'}`}>{p.estado === 'PAGADO' ? 'Pagado' : 'Anulado'}</span></td>
                <td className="text-end"><Button size="sm" variant="outline-secondary" onClick={() => boleta(p)}><i className="bi bi-file-earmark-pdf me-1" />Boleta</Button></td>
              </tr>))}</tbody>
          </table></div>
        </div>
      )}
    </>
  );
}

// ------------------------------------------------------------ Mis recetas (con descarga en PDF)
export function MisRecetas() {
  const { pacienteActivo } = useAuth();
  const toast = useToast();
  const [lista, cargando] = useCarga(() => (pacienteActivo ? api.get(`/historial/paciente/${pacienteActivo.id}`).then((r) => r.data.filter((h) => h.receta)) : Promise.resolve([])), [pacienteActivo?.id]);
  const pdf = async (h) => { try { (await import('../../pdf')).recetaPdf(h, pacienteActivo.documentoIdentidad); } catch { toast('No se pudo generar el PDF', 'error'); } };
  return (
    <>
      <Titulo icono="prescription2">Mis recetas</Titulo>
      {!pacienteActivo ? <SinPaciente /> : cargando ? <Cargando /> : !lista?.length ? <Vacio icono="prescription2">No tiene recetas emitidas.</Vacio> : (
        <Row className="g-3">{lista.map((h) => (
          <Col md={6} key={h.idHistorial}>
            <div className="caja h-100">
              <div className="d-flex justify-content-between"><div className="text-muted small">{fmtFecha(h.fechaCita)} · {h.medico}</div><EstadoBadge estado={h.receta.estado} /></div>
              <h2 className="h6 mt-1">{h.especialidad}</h2>
              <ul className="small">{h.receta.detalles.map((d) => <li key={d.idDetalle}>{d.medicamento} ({d.presentacion}) — {d.dosis} · cant. {d.cantidad}</li>)}</ul>
              <Button size="sm" variant="outline-secondary" onClick={() => pdf(h)}><i className="bi bi-file-earmark-pdf me-1" />Descargar PDF</Button>
            </div>
          </Col>))}</Row>
      )}
    </>
  );
}
