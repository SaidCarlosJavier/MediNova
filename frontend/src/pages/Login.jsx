import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Container, Row, Col, Alert } from 'react-bootstrap';
import { useAuth } from '../AuthContext';
import { HOME, msg } from '../api';
import { Campo, reglas, validar } from '../components/ui';

export default function Login() {
  const { usuario, login } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const expirada = new URLSearchParams(loc.search).get('expirada');
  const [f, setF] = useState({ identificador: '', password: '' });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  if (usuario) return <Navigate to={HOME[usuario.rol]} replace />;

  const enviar = async (e) => {
    e.preventDefault();
    const errs = validar(f, {
      identificador: [(v) => reglas.requerido(v, 'Ingrese su correo o documento de identidad')],
      password: [(v) => reglas.requerido(v, 'Ingrese su contraseña')],
    });
    setErrores(errs);
    if (Object.keys(errs).length) return;
    setEnviando(true);
    setError('');
    try {
      const u = await login(f.identificador, f.password);
      nav(loc.state?.desde || HOME[u.rol], { replace: true });
    } catch (ex) {
      setError(msg(ex));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="auth-fondo">
      <Container>
        <Row className="justify-content-center">
          <Col md={8} lg={5}>
            <div className="auth-card">
              <div className="text-center mb-4">
                <Link to="/" className="text-decoration-none"><i className="bi bi-hospital fs-1" /></Link>
                <h1 className="h4 mt-2">Ingresar al sistema</h1>
                <p className="text-muted mb-0">Hospital Hermilio Valdizán</p>
              </div>
              {expirada && <Alert variant="warning">Su sesión expiró. Vuelva a ingresar.</Alert>}
              {error && <Alert variant="danger" role="alert">{error}</Alert>}
              <form onSubmit={enviar} noValidate>
                <Campo id="identificador" etiqueta="Correo o documento de identidad" error={errores.identificador}>
                  <input id="identificador" className="form-control form-control-lg" autoComplete="username" value={f.identificador}
                    onChange={(e) => setF({ ...f, identificador: e.target.value })} />
                </Campo>
                <Campo id="password" etiqueta="Contraseña" error={errores.password}>
                  <input id="password" type="password" className="form-control form-control-lg" autoComplete="current-password" value={f.password}
                    onChange={(e) => setF({ ...f, password: e.target.value })} />
                </Campo>
                <button className="btn btn-azul btn-lg w-100 mb-3" disabled={enviando}>{enviando ? 'Ingresando…' : 'Ingresar'}</button>
              </form>
              <div className="text-center">
                <Link to="/recuperar">¿Olvidó su contraseña?</Link>
                <hr />
                <span className="text-muted">¿Aún no tiene cuenta? </span><Link to="/registro" className="fw-semibold">Regístrese</Link>
              </div>
            </div>
          </Col>
        </Row>
      </Container>
    </div>
  );
}
