import { useState } from 'react';
import { Alert, Button, Col, Modal, Row, Tab, Tabs } from 'react-bootstrap';
import { api, fieldErrors, msg } from '../../api';
import { Campo, Cargando, Paginador, Titulo, Vacio, fmtFecha, reglas, useCarga, useToast, validar } from '../../components/ui';
import { CitasPaciente, ReservaWizard } from '../../components/Reserva';

const VACIO = { documentoIdentidad: '', nombres: '', apellidos: '', correo: '', fechaNacimiento: '', tipoSeguro: '', telefonoContacto: '', requiereTutor: false, consentimiento: false };
const TUTOR_VACIO = { documentoIdentidad: '', nombres: '', apellidos: '', correo: '', parentesco: '', telefono: '', direccion: '' };

/** RF-17 registrar paciente (y tutor), RF-18 buscar por documento, RF-20/21/22 citas presenciales, RF-24 lista de espera. */
export default function Pacientes() {
  const toast = useToast();
  const [q, setQ] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(0);
  const [sel, setSel] = useState(null);
  const [recarga, setRecarga] = useState(0);
  const [modal, setModal] = useState(false);
  const [f, setF] = useState(VACIO);
  const [t, setT] = useState(TUTOR_VACIO);
  const [errores, setErrores] = useState({});
  const [ocupado, setOcupado] = useState(false);
  const [resultado, setResultado] = useState(null);
  const [esperaEsp, setEsperaEsp] = useState('');
  const [especialidades] = useCarga(() => api.get('/especialidades').then((r) => r.data), []);

  const [datos, cargando, recargar] = useCarga(
    () => (busqueda ? api.get('/pacientes', { params: { q: busqueda, pagina, tamanio: 8 } }).then((r) => r.data) : Promise.resolve(null)),
    [busqueda, pagina]
  );

  const buscar = (e) => { e.preventDefault(); setSel(null); setPagina(0); setBusqueda(q.trim()); };

  const cerrar = () => { setModal(false); setErrores({}); setF(VACIO); setT(TUTOR_VACIO); setResultado(null); };

  const registrar = async () => {
    const esq = {
      documentoIdentidad: [reglas.documento], nombres: [(v) => reglas.requerido(v, 'Ingrese los nombres')], apellidos: [(v) => reglas.requerido(v, 'Ingrese los apellidos')],
      fechaNacimiento: [(v) => reglas.requerido(v, 'Ingrese la fecha de nacimiento'), reglas.pasada], telefonoContacto: [reglas.telefono],
      consentimiento: [(v) => (v ? null : 'Debe registrarse el consentimiento informado (Ley N.° 29733)')],
    };
    if (f.correo) esq.correo = [reglas.correo];
    let errs = validar(f, esq);
    if (f.requiereTutor) {
      const te = validar(t, { documentoIdentidad: [reglas.documento], nombres: [(v) => reglas.requerido(v, 'Ingrese los nombres')], apellidos: [(v) => reglas.requerido(v, 'Ingrese los apellidos')], correo: [reglas.correo], parentesco: [(v) => reglas.requerido(v, 'Indique el parentesco')], telefono: [reglas.telefono] });
      Object.entries(te).forEach(([k, v]) => { errs[`tutor.${k}`] = v; });
    }
    setErrores(errs);
    if (Object.keys(errs).length) return;
    setOcupado(true);
    try {
      const { data } = await api.post('/pacientes', { ...f, correo: f.correo || null, tutor: f.requiereTutor ? t : null });
      toast('Paciente registrado correctamente');
      setResultado(data);
      setSel(data.paciente);
    } catch (e) { setErrores(fieldErrors(e)); toast(msg(e), 'error'); } finally { setOcupado(false); }
  };

  const anotarEspera = async () => {
    try { await api.post('/espera', { pacienteId: sel.id, especialidadId: Number(esperaEsp) }); toast('Paciente agregado a la lista de espera'); setEsperaEsp(''); }
    catch (e) { toast(msg(e), 'error'); }
  };

  const I = (k, etiqueta, extra = {}) => (
    <Col md={6}><Campo id={`p-${k}`} etiqueta={etiqueta} error={errores[k]}><input id={`p-${k}`} className="form-control" value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} {...extra} /></Campo></Col>
  );
  const IT = (k, etiqueta, extra = {}) => (
    <Col md={6}><Campo id={`t-${k}`} etiqueta={etiqueta} error={errores[`tutor.${k}`]}><input id={`t-${k}`} className="form-control" value={t[k]} onChange={(e) => setT({ ...t, [k]: e.target.value })} {...extra} /></Campo></Col>
  );

  return (
    <>
      <Titulo icono="people" acciones={<Button className="btn-azul" onClick={() => setModal(true)}><i className="bi bi-person-plus me-1" />Registrar paciente</Button>}>Pacientes y citas presenciales</Titulo>

      <div className="caja mb-3">
        <form className="row g-2" onSubmit={buscar}>
          <div className="col-md-9"><label htmlFor="q" className="visually-hidden">Buscar paciente</label>
            <input id="q" className="form-control form-control-lg" placeholder="Buscar por documento, nombres o apellidos…" value={q} onChange={(e) => setQ(e.target.value)} /></div>
          <div className="col-md-3"><Button type="submit" className="btn-azul w-100 btn-lg"><i className="bi bi-search me-2" />Buscar</Button></div>
        </form>
        {busqueda && (cargando ? <Cargando /> : !datos?.contenido.length ? <Vacio icono="search">No se encontraron pacientes. Puede registrarlo con el botón «Registrar paciente».</Vacio> : (
          <>
            <div className="table-responsive mt-3"><table className="table table-hover align-middle mb-0">
              <thead><tr><th>Documento</th><th>Paciente</th><th>Nacimiento</th><th>Tutor</th><th /></tr></thead>
              <tbody>{datos.contenido.map((p) => (
                <tr key={p.id} className={sel?.id === p.id ? 'table-primary' : ''}>
                  <td>{p.documentoIdentidad}</td><td className="fw-semibold">{p.nombreCompleto}</td><td>{fmtFecha(p.fechaNacimiento)}</td><td>{p.tutor || '—'}</td>
                  <td className="text-end"><Button size="sm" variant="outline-primary" onClick={() => setSel(p)}>Gestionar</Button></td>
                </tr>))}</tbody>
            </table></div>
            <Paginador pagina={datos.pagina} totalPaginas={datos.totalPaginas} onCambio={setPagina} />
          </>
        ))}
      </div>

      {sel && (
        <div className="caja">
          <div className="d-flex justify-content-between flex-wrap mb-2">
            <div><h2 className="h5 mb-0">{sel.nombreCompleto}</h2><div className="small text-muted">Doc. {sel.documentoIdentidad}{sel.tutor && <> · Tutor: {sel.tutor}</>}</div></div>
            <Button size="sm" variant="outline-secondary" onClick={() => setSel(null)}>Cerrar</Button>
          </div>
          <Tabs defaultActiveKey="citas" className="mb-3">
            <Tab eventKey="citas" title="Citas del paciente"><CitasPaciente pacienteId={sel.id} recarga={recarga} ventanilla /></Tab>
            <Tab eventKey="nueva" title="Nueva cita presencial">
              <ReservaWizard pacienteId={sel.id} pacienteNombre={sel.nombreCompleto} origen="PRESENCIAL" onTerminar={() => setRecarga((n) => n + 1)} />
            </Tab>
            <Tab eventKey="espera" title="Lista de espera">
              <p className="text-muted">Si no hay horarios, registre al paciente en la lista de espera; se avisará al liberarse un turno.</p>
              <div className="row g-2" style={{ maxWidth: 520 }}>
                <div className="col-8"><select className="form-select" aria-label="Especialidad" value={esperaEsp} onChange={(e) => setEsperaEsp(e.target.value)}>
                  <option value="">Especialidad…</option>{especialidades?.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}</select></div>
                <div className="col-4"><Button className="btn-azul w-100" disabled={!esperaEsp} onClick={anotarEspera}>Agregar</Button></div>
              </div>
            </Tab>
          </Tabs>
        </div>
      )}

      <Modal show={modal} onHide={cerrar} size="lg" centered backdrop="static">
        <Modal.Header closeButton><Modal.Title>Registrar paciente</Modal.Title></Modal.Header>
        <Modal.Body>
          {resultado ? (
            <Alert variant="success">
              <h3 className="h6">Paciente registrado: {resultado.paciente.nombreCompleto}</h3>
              {resultado.passwordTemporalTutor && <p className="mb-0">Se creó la cuenta del tutor. Contraseña temporal (entréguela al tutor): <strong className="user-select-all">{resultado.passwordTemporalTutor}</strong></p>}
            </Alert>
          ) : (
            <>
              <Row>
                {I('documentoIdentidad', 'Documento de identidad', { maxLength: 12 })}{I('fechaNacimiento', 'Fecha de nacimiento', { type: 'date' })}
                {I('nombres', 'Nombres')}{I('apellidos', 'Apellidos')}{I('correo', 'Correo (opcional)', { type: 'email' })}{I('telefonoContacto', 'Teléfono de contacto')}
                <Col md={6}><Campo id="p-seg" etiqueta="Tipo de seguro"><select id="p-seg" className="form-select" value={f.tipoSeguro} onChange={(e) => setF({ ...f, tipoSeguro: e.target.value })}><option value="">Seleccione…</option><option>SIS</option><option>EsSalud</option><option>Particular</option><option>Otro</option></select></Campo></Col>
              </Row>
              <div className="form-check mb-2"><input id="p-rt" type="checkbox" className="form-check-input" checked={f.requiereTutor} onChange={(e) => setF({ ...f, requiereTutor: e.target.checked })} /><label htmlFor="p-rt" className="form-check-label">El paciente requiere tutor / apoderado</label></div>
              {f.requiereTutor && (
                <div className="p-3 mb-3 rounded-3" style={{ background: '#f4f8fc', border: '1px solid #d8e6f3' }}>
                  <h3 className="h6">Datos del tutor / apoderado</h3>
                  <p className="small text-muted">Si el documento ya está registrado como tutor, se vincula a su cuenta existente.</p>
                  <Row>{IT('documentoIdentidad', 'Documento', { maxLength: 12 })}{IT('parentesco', 'Parentesco')}{IT('nombres', 'Nombres')}{IT('apellidos', 'Apellidos')}{IT('correo', 'Correo', { type: 'email' })}{IT('telefono', 'Teléfono')}</Row>
                </div>
              )}
              <div className="form-check"><input id="p-cons" type="checkbox" className="form-check-input" checked={f.consentimiento} onChange={(e) => setF({ ...f, consentimiento: e.target.checked })} />
                <label htmlFor="p-cons" className="form-check-label small">Se informó al paciente/tutor y se obtuvo el consentimiento para el tratamiento de datos personales (Ley N.° 29733).</label></div>
              {errores.consentimiento && <div className="text-danger small mt-1">{errores.consentimiento}</div>}
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          {resultado ? <Button className="btn-azul" onClick={() => { cerrar(); recargar(); }}>Listo</Button> : <><Button variant="outline-secondary" onClick={cerrar}>Cancelar</Button><Button className="btn-azul" disabled={ocupado} onClick={registrar}>{ocupado ? 'Guardando…' : 'Registrar'}</Button></>}
        </Modal.Footer>
      </Modal>
    </>
  );
}
