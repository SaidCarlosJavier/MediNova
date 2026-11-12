import { useState } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { Container, Row, Col, Alert } from 'react-bootstrap';
import { useAuth } from '../AuthContext';
import { HOME, fieldErrors, msg } from '../api';
import { Campo, reglas, useToast, validar } from '../components/ui';

const VACIO = {
  tipo: 'PACIENTE', documentoIdentidad: '', nombres: '', apellidos: '', correo: '', password: '', confirmar: '',
  tipoSeguro: '', telefonoContacto: '', fechaNacimiento: '', requiereTutor: false,
  parentesco: '', telefono: '', direccion: '', consentimiento: false,
};

export default function Registro() {
  const { usuario, registrar } = useAuth();
  const [params] = useSearchParams();
  const nav = useNavigate();
  const toast = useToast();
  const [f, setF] = useState({ ...VACIO, tipo: params.get('tipo') === 'TUTOR' ? 'TUTOR' : 'PACIENTE' });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  if (usuario) return <Navigate to={HOME[usuario.rol]} replace />;

  const set = (k) => (e) => setF({ ...f, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  const esTutor = f.tipo === 'TUTOR';

  const enviar = async (e) => {
    e.preventDefault();
    const esquema = {
      documentoIdentidad: [reglas.documento],
      nombres: [(v) => reglas.requerido(v, 'Ingrese sus nombres')],
      apellidos: [(v) => reglas.requerido(v, 'Ingrese sus apellidos')],
      correo: [reglas.correo],
      password: [reglas.password],
      confirmar: [(v) => (v === f.password ? null : 'Las contraseñas no coinciden')],
      consentimiento: [(v) => (v ? null : 'Debe aceptar el tratamiento de sus datos personales (Ley N.° 29733)')],
    };
    if (esTutor) esquema.parentesco = [(v) => reglas.requerido(v, 'Indique su parentesco con el paciente')];
    else esquema.fechaNacimiento = [(v) => reglas.requerido(v, 'Ingrese su fecha de nacimiento'), reglas.pasada];
    esquema.telefonoContacto = [reglas.telefono];
    esquema.telefono = [reglas.telefono];
    const errs = validar(f, esquema);
    setErrores(errs);
    if (Object.keys(errs).length) return;

    const { confirmar, ...datos } = f;
    datos.fechaNacimiento = datos.fechaNacimiento || null;
    setEnviando(true);
    setError('');
    try {
      const u = await registrar(datos);
      toast('Cuenta creada correctamente. ¡Bienvenido!');
      nav(HOME[u.rol], { replace: true });
    } catch (ex) {
      setErrores(fieldErrors(ex));
      setError(msg(ex));
    } finally {
      setEnviando(false);
    }
  };

  const I = ({ k, etiqueta, tipo = 'text', ...r }) => (
    <Campo id={k} etiqueta={etiqueta} error={errores[k]}>
      <input id={k} type={tipo} className="form-control" value={f[k]} onChange={set(k)} {...r} />
    </Campo>
  );

  return (
    <div className="auth-fondo">
      <Container>
        <Row className="justify-content-center">
          <Col md={10} lg={7}>
            <div className="auth-card">
              <div className="text-center mb-3">
                <Link to="/" className="text-decoration-none"><i className="bi bi-hospital fs-1" /></Link>
                <h1 className="h4 mt-2">Crear cuenta</h1>
              </div>
              {error && <Alert variant="danger" role="alert">{error}</Alert>}
              <form onSubmit={enviar} noValidate>
                <div className="mb-3 d-flex gap-2" role="radiogroup" aria-label="Tipo de registro">
                  {[['PACIENTE', 'bi-person', 'Soy paciente'], ['TUTOR', 'bi-people', 'Soy tutor / apoderado']].map(([v, ic, t]) => (
                    <button type="button" key={v} className={`btn flex-fill ${f.tipo === v ? 'btn-azul' : 'btn-outline-azul'}`} onClick={() => setF({ ...f, tipo: v })}>
                      <i className={`bi ${ic} me-2`} />{t}
                    </button>
                  ))}
                </div>
                {esTutor && (
                  <Alert variant="info" className="small">
                    Como tutor podrá reservar, reprogramar y cancelar citas y recibir notificaciones en nombre de los pacientes a su cargo.
                    Después de registrarse podrá vincular o registrar a sus pacientes.
                  </Alert>
                )}
                <Row>
                  <Col md={6}>{I({ k: 'documentoIdentidad', etiqueta: 'Documento de identidad (DNI / CE)', maxLength: 12 })}</Col>
                  <Col md={6}>{I({ k: 'correo', etiqueta: 'Correo electrónico', tipo: 'email', autoComplete: 'email' })}</Col>
                  <Col md={6}>{I({ k: 'nombres', etiqueta: 'Nombres' })}</Col>
                  <Col md={6}>{I({ k: 'apellidos', etiqueta: 'Apellidos' })}</Col>
                  <Col md={6}>{I({ k: 'password', etiqueta: 'Contraseña (mínimo 8 caracteres)', tipo: 'password', autoComplete: 'new-password' })}</Col>
                  <Col md={6}>{I({ k: 'confirmar', etiqueta: 'Repita la contraseña', tipo: 'password', autoComplete: 'new-password' })}</Col>
                  {esTutor ? (
                    <>
                      <Col md={6}>{I({ k: 'parentesco', etiqueta: 'Parentesco con el paciente (ej. madre, hijo)' })}</Col>
                      <Col md={6}>{I({ k: 'telefono', etiqueta: 'Teléfono' })}</Col>
                      <Col md={12}>{I({ k: 'direccion', etiqueta: 'Dirección (opcional)' })}</Col>
                    </>
                  ) : (
                    <>
                      <Col md={6}>{I({ k: 'fechaNacimiento', etiqueta: 'Fecha de nacimiento', tipo: 'date', max: new Date().toISOString().slice(0, 10) })}</Col>
                      <Col md={6}>{I({ k: 'telefonoContacto', etiqueta: 'Teléfono de contacto' })}</Col>
                      <Col md={6}>
                        <Campo id="tipoSeguro" etiqueta="Tipo de seguro">
                          <select id="tipoSeguro" className="form-select" value={f.tipoSeguro} onChange={set('tipoSeguro')}>
                            <option value="">Seleccione…</option>
                            <option>SIS</option><option>EsSalud</option><option>Particular</option><option>Otro</option>
                          </select>
                        </Campo>
                      </Col>
                      <Col md={6} className="d-flex align-items-center">
                        <div className="form-check">
                          <input id="requiereTutor" type="checkbox" className="form-check-input" checked={f.requiereTutor} onChange={set('requiereTutor')} />
                          <label htmlFor="requiereTutor" className="form-check-label">Necesito que un tutor/apoderado gestione mi atención</label>
                        </div>
                      </Col>
                    </>
                  )}
                </Row>
                <div className="p-3 rounded-3 mb-3" style={{ background: '#f4f8fc', border: '1px solid #d8e6f3' }}>
                  <div className="form-check">
                    <input id="consentimiento" type="checkbox" className="form-check-input" checked={f.consentimiento} onChange={set('consentimiento')} />
                    <label htmlFor="consentimiento" className="form-check-label small">
                      <strong>Consentimiento informado (Ley N.° 29733).</strong> Autorizo el tratamiento de mis datos personales y de salud por el Hospital
                      Hermilio Valdizán, únicamente para la gestión de mi atención. Solo se recopilan los datos mínimos necesarios y pueden ser
                      consultados solo por personal autorizado.
                    </label>
                  </div>
                  {errores.consentimiento && <div className="text-danger small mt-1" role="alert">{errores.consentimiento}</div>}
                </div>
                <button className="btn btn-azul btn-lg w-100" disabled={enviando}>{enviando ? 'Registrando…' : 'Crear cuenta'}</button>
              </form>
              <p className="text-center mt-3 mb-0">¿Ya tiene cuenta? <Link to="/login" className="fw-semibold">Ingrese aquí</Link></p>
            </div>
          </Col>
        </Row>
      </Container>
    </div>
  );
}
