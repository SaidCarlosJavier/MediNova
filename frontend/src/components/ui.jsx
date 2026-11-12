import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { Toast, ToastContainer, Spinner as BsSpinner, Badge, Pagination } from 'react-bootstrap';

// ------------------------------------------------------------ Toasts
const ToastCtx = createContext(() => {});
export const useToast = () => useContext(ToastCtx);

export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const notificar = useCallback((mensaje, tipo = 'success') => {
    const id = Date.now() + Math.random();
    setItems((l) => [...l, { id, mensaje, tipo }]);
    setTimeout(() => setItems((l) => l.filter((x) => x.id !== id)), 5000);
  }, []);
  return (
    <ToastCtx.Provider value={notificar}>
      {children}
      <ToastContainer position="top-end" className="p-3" style={{ zIndex: 2000, position: 'fixed' }}>
        {items.map((t) => (
          <Toast key={t.id} bg={t.tipo === 'error' ? 'danger' : t.tipo} onClose={() => setItems((l) => l.filter((x) => x.id !== t.id))} show>
            <Toast.Body className="text-white fw-semibold d-flex align-items-start gap-2">
              <i className={`bi ${t.tipo === 'error' ? 'bi-exclamation-octagon' : 'bi-check-circle'} fs-5`} />
              <span>{t.mensaje}</span>
            </Toast.Body>
          </Toast>
        ))}
      </ToastContainer>
    </ToastCtx.Provider>
  );
}

// ------------------------------------------------------------ Carga y utilidades
export const Cargando = ({ texto = 'Cargando…' }) => (
  <div className="text-center py-5 text-muted" role="status">
    <BsSpinner animation="border" variant="primary" className="mb-2" />
    <div>{texto}</div>
  </div>
);

export const Vacio = ({ icono = 'inbox', children }) => (
  <div className="text-center text-muted py-4">
    <i className={`bi bi-${icono} fs-1 d-block mb-2`} />
    {children}
  </div>
);

export const Titulo = ({ icono, children, acciones }) => (
  <div className="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
    <h1 className="h3 mb-0">{icono && <i className={`bi bi-${icono} me-2`} />}{children}</h1>
    <div className="d-flex gap-2 flex-wrap">{acciones}</div>
  </div>
);

/** Hook de carga: devuelve [datos, cargando, recargar]. */
export function useCarga(fn, deps = []) {
  const [datos, setDatos] = useState(null);
  const [cargando, setCargando] = useState(true);
  const toast = useToast();
  const fnRef = useRef(fn);
  fnRef.current = fn;
  const recargar = useCallback(async () => {
    setCargando(true);
    try {
      setDatos(await fnRef.current());
    } catch (e) {
      toast(e?.response?.data?.mensaje || 'No se pudo cargar la información', 'error');
    } finally {
      setCargando(false);
    }
  }, []); // eslint-disable-line
  useEffect(() => { recargar(); }, deps); // eslint-disable-line
  return [datos, cargando, recargar];
}

export function Paginador({ pagina, totalPaginas, onCambio }) {
  if (!totalPaginas || totalPaginas <= 1) return null;
  return (
    <Pagination className="justify-content-center mt-3 mb-0">
      <Pagination.Prev disabled={pagina === 0} onClick={() => onCambio(pagina - 1)} />
      <Pagination.Item active>{pagina + 1} / {totalPaginas}</Pagination.Item>
      <Pagination.Next disabled={pagina + 1 >= totalPaginas} onClick={() => onCambio(pagina + 1)} />
    </Pagination>
  );
}

// ------------------------------------------------------------ Formato
export const fmtFecha = (f) => (f ? new Date(f + (f.length === 10 ? 'T00:00:00' : '')).toLocaleDateString('es-PE', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' }) : '—');
export const fmtFechaHora = (f) => (f ? new Date(f).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' }) : '—');
export const fmtHora = (h) => (h ? h.substring(0, 5) : '');
export const hoyISO = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
export const sumarDias = (iso, n) => {
  const d = new Date(iso + 'T00:00:00');
  d.setDate(d.getDate() + n);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

const COLORES = {
  RESERVADA: 'info', CONFIRMADA: 'primary', REPROGRAMADA: 'warning', CANCELADA: 'secondary',
  ASISTIO: 'success', ATENDIDA: 'success', NO_ASISTIO: 'danger',
  PENDIENTE: 'warning', PARCIAL: 'info', DISPENSADA: 'success',
  EN_ESPERA: 'warning', NOTIFICADO: 'info', ATENDIDO: 'success',
};
const TEXTOS = { NO_ASISTIO: 'NO ASISTIÓ', EN_ESPERA: 'EN ESPERA', ASISTIO: 'ASISTIÓ' };
export const EstadoBadge = ({ estado }) => (
  <Badge bg={COLORES[estado] || 'secondary'} className="px-2 py-2">{TEXTOS[estado] || estado}</Badge>
);

export const ROL_TEXTO = {
  ROLE_PACIENTE: 'Paciente', ROLE_TUTOR: 'Tutor / Apoderado', ROLE_RECEPCIONISTA: 'Recepción',
  ROLE_MEDICO: 'Médico', ROLE_ADMIN: 'Administrador', ROLE_FARMACEUTICO: 'Farmacéutico',
};

// ------------------------------------------------------------ Validaciones de cliente (reflejan las del servidor)
export const reglas = {
  requerido: (v, m = 'Este campo es obligatorio') => (v === undefined || v === null || String(v).trim() === '' ? m : null),
  correo: (v) => (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v || '') ? null : 'Ingrese un correo válido'),
  documento: (v) => (/^[A-Za-z0-9]{8,12}$/.test(v || '') ? null : 'El documento debe tener entre 8 y 12 caracteres alfanuméricos'),
  password: (v) => ((v || '').length >= 8 && v.length <= 64 ? null : 'La contraseña debe tener entre 8 y 64 caracteres'),
  telefono: (v) => (!v || /^[0-9+ ]{7,15}$/.test(v) ? null : 'Teléfono inválido'),
  pasada: (v) => (!v || v < hoyISO() ? null : 'La fecha de nacimiento debe ser pasada'),
};

/** Ejecuta {campo: [reglas...]} y devuelve un objeto de errores (vacío si todo es válido). */
export function validar(valores, esquema) {
  const errores = {};
  Object.entries(esquema).forEach(([campo, lista]) => {
    for (const r of lista) {
      const m = r(valores[campo]);
      if (m) { errores[campo] = m; break; }
    }
  });
  return errores;
}

export function Campo({ id, etiqueta, error, ayuda, children }) {
  return (
    <div className="mb-3">
      {etiqueta && <label htmlFor={id} className="form-label">{etiqueta}</label>}
      {children}
      {ayuda && !error && <div className="form-text">{ayuda}</div>}
      {error && <div className="text-danger small mt-1" role="alert"><i className="bi bi-exclamation-circle me-1" />{error}</div>}
    </div>
  );
}

export const Img = ({ src, alt, className }) => {
  const [ok, setOk] = useState(true);
  return ok ? <img src={src} alt={alt} className={className} loading="lazy" onError={() => setOk(false)} /> : null;
};
