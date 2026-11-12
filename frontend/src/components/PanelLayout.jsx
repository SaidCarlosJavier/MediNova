import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Badge, Dropdown } from 'react-bootstrap';
import { useAuth } from '../AuthContext';
import { api } from '../api';
import { ROL_TEXTO } from './ui';

/** Layout de panel: sidebar colapsable + topbar. En tutores muestra el selector "Actuar en nombre de". */
export default function PanelLayout({ menu, base }) {
  const { usuario, logout, aCargo, pacienteId, setPacienteId } = useAuth();
  const nav = useNavigate();
  const [cerrado, setCerrado] = useState(() => window.innerWidth < 992);
  const [noLeidas, setNoLeidas] = useState(0);
  const conNotificaciones = ['ROLE_PACIENTE', 'ROLE_TUTOR'].includes(usuario.rol);

  useEffect(() => {
    if (!conNotificaciones) return undefined;
    const cargar = () => api.get('/notificaciones/no-leidas').then((r) => setNoLeidas(r.data.total)).catch(() => {});
    cargar();
    const t = setInterval(cargar, 60000);
    return () => clearInterval(t);
  }, [conNotificaciones]);

  const salir = () => { logout(); nav('/login'); };
  const alElegir = () => { if (window.innerWidth < 992) setCerrado(true); };

  return (
    <div className="panel">
      <aside className={`sidebar ${cerrado ? 'cerrado' : ''}`} aria-label="Menú principal">
        <div className="marca"><i className="bi bi-hospital me-2" />H. Hermilio Valdizán</div>
        <nav className="flex-grow-1 py-2">
          {menu.map((m) => (
            <NavLink key={m.to} to={m.to} end={m.to === base} className="item" onClick={alElegir}>
              <i className={`bi bi-${m.icono} fs-5`} />
              <span>{m.texto}</span>
            </NavLink>
          ))}
        </nav>
        <div className="p-3 small text-white-50">Salud mental · Lima, Perú</div>
      </aside>

      <div className="contenido">
        <header className="topbar-panel">
          <button className="btn btn-outline-secondary" onClick={() => setCerrado(!cerrado)} aria-label="Mostrar u ocultar menú">
            <i className="bi bi-list fs-5" />
          </button>

          {usuario.rol === 'ROLE_TUTOR' && (
            <div className="d-flex align-items-center gap-2">
              <label htmlFor="actuar" className="fw-semibold small mb-0 text-nowrap"><i className="bi bi-person-heart me-1" />Actuar en nombre de:</label>
              <select id="actuar" className="form-select" style={{ minWidth: 220 }} value={pacienteId || ''} onChange={(e) => setPacienteId(Number(e.target.value))}>
                {aCargo.length === 0 && <option value="">Sin pacientes a cargo</option>}
                {aCargo.map((p) => <option key={p.id} value={p.id}>{p.nombreCompleto}</option>)}
              </select>
            </div>
          )}

          <div className="ms-auto d-flex align-items-center gap-3">
            {conNotificaciones && (
              <NavLink to={`${base}/notificaciones`} className="position-relative text-decoration-none text-dark" aria-label="Notificaciones">
                <i className="bi bi-bell fs-4" />
                {noLeidas > 0 && <Badge bg="danger" pill className="position-absolute top-0 start-100 translate-middle">{noLeidas}</Badge>}
              </NavLink>
            )}
            <Dropdown align="end">
              <Dropdown.Toggle variant="light" className="border">
                <i className="bi bi-person-circle me-2" />{usuario.nombres}
              </Dropdown.Toggle>
              <Dropdown.Menu>
                <Dropdown.ItemText className="small text-muted">{ROL_TEXTO[usuario.rol]}</Dropdown.ItemText>
                {conNotificaciones && <Dropdown.Item onClick={() => nav(`${base}/perfil`)}><i className="bi bi-person me-2" />Mi perfil</Dropdown.Item>}
                <Dropdown.Item onClick={salir}><i className="bi bi-box-arrow-right me-2" />Cerrar sesión</Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          </div>
        </header>
        <main className="main-panel">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
