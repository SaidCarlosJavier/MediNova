import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { HOME } from '../api';

/** Ruta protegida por rol: sin sesión -> login; rol no permitido -> su propio panel. */
export default function ProtectedRoute({ roles, children }) {
  const { usuario } = useAuth();
  const loc = useLocation();
  if (!usuario) return <Navigate to="/login" state={{ desde: loc.pathname }} replace />;
  if (roles && !roles.includes(usuario.rol)) return <Navigate to={HOME[usuario.rol] || '/'} replace />;
  return children;
}
