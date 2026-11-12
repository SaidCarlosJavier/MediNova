import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import PanelLayout from './components/PanelLayout';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Registro from './pages/Registro';
import { Recuperar, Restablecer } from './pages/Recuperar';
import { MisCitas, PanelInicio, Reservar, Ubicacion } from './pages/paciente/PacienteCitas';
import { Historial, MisPagos, MisRecetas, Notificaciones, PacientesACargo, Perfil } from './pages/paciente/PacienteExtra';
import Pacientes from './pages/recepcion/Pacientes';
import AgendaCheckIn from './pages/recepcion/AgendaCheckIn';
import { Disponibilidad, ListaEspera } from './pages/recepcion/EsperaDisponibilidad';
import Caja from './pages/recepcion/Caja';
import { AgendaMedico, Atencion, HistorialDePaciente, MiDisponibilidad, MisPacientes } from './pages/medico/Medico';
import { Alertas, Inventario, Movimientos, Recetas, Ventas } from './pages/farmacia/Farmacia';
import { Configuracion, Indicadores, Pagos, Reportes, Usuarios } from './pages/admin/Admin';

const MENU_PACIENTE = [
  { to: '/panel', icono: 'house', texto: 'Inicio' },
  { to: '/panel/reservar', icono: 'calendar-plus', texto: 'Reservar cita' },
  { to: '/panel/citas', icono: 'calendar-check', texto: 'Mis citas' },
  { to: '/panel/historial', icono: 'journal-medical', texto: 'Historial' },
  { to: '/panel/recetas', icono: 'prescription2', texto: 'Mis recetas' },
  { to: '/panel/pagos', icono: 'credit-card', texto: 'Mis pagos' },
  { to: '/panel/ubicacion', icono: 'geo-alt', texto: 'Ubicación' },
  { to: '/panel/notificaciones', icono: 'bell', texto: 'Notificaciones' },
  { to: '/panel/perfil', icono: 'person', texto: 'Mi perfil' },
];
const MENU_TUTOR = [
  ...MENU_PACIENTE.slice(0, 1),
  { to: '/panel/pacientes', icono: 'people', texto: 'Pacientes a mi cargo' },
  ...MENU_PACIENTE.slice(1),
];
const MENU_RECEPCION = [
  { to: '/recepcion', icono: 'people', texto: 'Pacientes y citas' },
  { to: '/recepcion/agenda', icono: 'calendar-day', texto: 'Agenda y check-in' },
  { to: '/recepcion/caja', icono: 'cash-coin', texto: 'Caja' },
  { to: '/recepcion/espera', icono: 'hourglass-split', texto: 'Lista de espera' },
  { to: '/recepcion/disponibilidad', icono: 'clock-history', texto: 'Disponibilidad' },
];
const MENU_MEDICO = [
  { to: '/medico', icono: 'calendar-day', texto: 'Mi agenda' },
  { to: '/medico/pacientes', icono: 'people', texto: 'Mis pacientes' },
  { to: '/medico/disponibilidad', icono: 'clock-history', texto: 'Mi disponibilidad' },
];
const MENU_FARMACIA = [
  { to: '/farmacia', icono: 'box-seam', texto: 'Inventario' },
  { to: '/farmacia/alertas', icono: 'exclamation-triangle', texto: 'Alertas' },
  { to: '/farmacia/recetas', icono: 'prescription2', texto: 'Recetas pendientes' },
  { to: '/farmacia/ventas', icono: 'receipt', texto: 'Ventas y boletas' },
  { to: '/farmacia/movimientos', icono: 'arrow-left-right', texto: 'Movimientos' },
];
const MENU_ADMIN = [
  { to: '/admin', icono: 'speedometer2', texto: 'Indicadores' },
  { to: '/admin/usuarios', icono: 'person-gear', texto: 'Cuentas y roles' },
  { to: '/admin/configuracion', icono: 'sliders', texto: 'Configuración' },
  { to: '/admin/pagos', icono: 'cash-stack', texto: 'Pagos e ingresos' },
  { to: '/admin/reportes', icono: 'bar-chart-line', texto: 'Reportes' },
];

/** El menú del panel de pacientes cambia según sea paciente o tutor. */
function PanelPacienteLayout() {
  const { usuario } = useAuth();
  return <PanelLayout base="/panel" menu={usuario.rol === 'ROLE_TUTOR' ? MENU_TUTOR : MENU_PACIENTE} />;
}

const Guard = ({ roles, children }) => <ProtectedRoute roles={roles}>{children}</ProtectedRoute>;

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Registro />} />
      <Route path="/recuperar" element={<Recuperar />} />
      <Route path="/restablecer" element={<Restablecer />} />

      <Route path="/panel" element={<Guard roles={['ROLE_PACIENTE', 'ROLE_TUTOR']}><PanelPacienteLayout /></Guard>}>
        <Route index element={<PanelInicio />} />
        <Route path="reservar" element={<Reservar />} />
        <Route path="citas" element={<MisCitas />} />
        <Route path="historial" element={<Historial />} />
        <Route path="recetas" element={<MisRecetas />} />
        <Route path="pagos" element={<MisPagos />} />
        <Route path="ubicacion" element={<Ubicacion />} />
        <Route path="notificaciones" element={<Notificaciones />} />
        <Route path="perfil" element={<Perfil />} />
        <Route path="pacientes" element={<Guard roles={['ROLE_TUTOR']}><PacientesACargo /></Guard>} />
      </Route>

      <Route path="/recepcion" element={<Guard roles={['ROLE_RECEPCIONISTA']}><PanelLayout base="/recepcion" menu={MENU_RECEPCION} /></Guard>}>
        <Route index element={<Pacientes />} />
        <Route path="agenda" element={<AgendaCheckIn />} />
        <Route path="caja" element={<Caja />} />
        <Route path="espera" element={<ListaEspera />} />
        <Route path="disponibilidad" element={<Disponibilidad />} />
      </Route>

      <Route path="/medico" element={<Guard roles={['ROLE_MEDICO']}><PanelLayout base="/medico" menu={MENU_MEDICO} /></Guard>}>
        <Route index element={<AgendaMedico />} />
        <Route path="pacientes" element={<MisPacientes />} />
        <Route path="disponibilidad" element={<MiDisponibilidad />} />
        <Route path="atencion/:citaId" element={<Atencion />} />
        <Route path="paciente/:pacienteId" element={<HistorialDePaciente />} />
      </Route>

      <Route path="/farmacia" element={<Guard roles={['ROLE_FARMACEUTICO']}><PanelLayout base="/farmacia" menu={MENU_FARMACIA} /></Guard>}>
        <Route index element={<Inventario />} />
        <Route path="alertas" element={<Alertas />} />
        <Route path="recetas" element={<Recetas />} />
        <Route path="ventas" element={<Ventas />} />
        <Route path="movimientos" element={<Movimientos />} />
      </Route>

      <Route path="/admin" element={<Guard roles={['ROLE_ADMIN']}><PanelLayout base="/admin" menu={MENU_ADMIN} /></Guard>}>
        <Route index element={<Indicadores />} />
        <Route path="usuarios" element={<Usuarios />} />
        <Route path="configuracion" element={<Configuracion />} />
        <Route path="pagos" element={<Pagos />} />
        <Route path="reportes" element={<Reportes />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
