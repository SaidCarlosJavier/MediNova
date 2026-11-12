import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Container, Row, Col, Alert } from 'react-bootstrap';
import { api, fieldErrors, msg } from '../api';
import { Campo, reglas, useToast, validar } from '../components/ui';

/** RF-03: solicitar enlace (token temporal enviado por correo/log) */
export function Recuperar() {
  const [correo, setCorreo] = useState('');
  const [error, setError] = useState('');
  const [enviado, setEnviado] = useState('');
  const [cargando, setCargando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    const m = reglas.correo(correo);
    if (m) return setError(m);
    setCargando(true);
    setError('');
    try {
      const { data } = await api.post('/auth/recuperar', { correo });
      setEnviado(data.mensaje);
    } catch (ex) {
      setError(msg(ex));
    } finally {
      setCargando(false);
    }
  };

  return (
    <Marco titulo="Recuperar contraseña">
      {enviado ? (
        <Alert variant="success">{enviado}. Revise su correo (en desarrollo, el enlace aparece en el log del servidor).</Alert>
      ) : (
        <form onSubmit={enviar} noValidate>
          <p className="text-muted">Ingrese el correo con el que se registró y le enviaremos un enlace temporal.</p>
          <Campo id="correo" etiqueta="Correo electrónico" error={error}>
            <input id="correo" type="email" className="form-control form-control-lg" value={correo} onChange={(e) => setCorreo(e.target.value)} />
          </Campo>
          <button className="btn btn-azul btn-lg w-100" disabled={cargando}>{cargando ? 'Enviando…' : 'Enviar enlace'}</button>
        </form>
      )}
      <p className="text-center mt-3 mb-0"><Link to="/login">Volver a ingresar</Link></p>
    </Marco>
  );
}

/** RF-03: establecer nueva contraseña con el token */
export function Restablecer() {
  const [params] = useSearchParams();
  const nav = useNavigate();
  const toast = useToast();
  const [f, setF] = useState({ nuevaPassword: '', confirmar: '' });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const token = params.get('token');

  const enviar = async (e) => {
    e.preventDefault();
    const errs = validar(f, {
      nuevaPassword: [reglas.password],
      confirmar: [(v) => (v === f.nuevaPassword ? null : 'Las contraseñas no coinciden')],
    });
    setErrores(errs);
    if (Object.keys(errs).length) return;
    try {
      await api.post('/auth/restablecer', { token, nuevaPassword: f.nuevaPassword });
      toast('Contraseña actualizada. Ya puede ingresar.');
      nav('/login');
    } catch (ex) {
      setErrores(fieldErrors(ex));
      setError(msg(ex));
    }
  };

  return (
    <Marco titulo="Nueva contraseña">
      {!token && <Alert variant="danger">El enlace no es válido. Solicite uno nuevo.</Alert>}
      {error && <Alert variant="danger">{error}</Alert>}
      <form onSubmit={enviar} noValidate>
        <Campo id="p1" etiqueta="Nueva contraseña" error={errores.nuevaPassword}>
          <input id="p1" type="password" className="form-control form-control-lg" value={f.nuevaPassword} onChange={(e) => setF({ ...f, nuevaPassword: e.target.value })} />
        </Campo>
        <Campo id="p2" etiqueta="Repita la contraseña" error={errores.confirmar}>
          <input id="p2" type="password" className="form-control form-control-lg" value={f.confirmar} onChange={(e) => setF({ ...f, confirmar: e.target.value })} />
        </Campo>
        <button className="btn btn-azul btn-lg w-100" disabled={!token}>Guardar contraseña</button>
      </form>
    </Marco>
  );
}

const Marco = ({ titulo, children }) => (
  <div className="auth-fondo">
    <Container>
      <Row className="justify-content-center">
        <Col md={8} lg={5}>
          <div className="auth-card">
            <h1 className="h4 text-center mb-4">{titulo}</h1>
            {children}
          </div>
        </Col>
      </Row>
    </Container>
  </div>
);
