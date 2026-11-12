import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api } from './api';

const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

const leer = () => {
  try { return JSON.parse(localStorage.getItem('usuario')); } catch { return null; }
};

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(leer);
  // "Actuar en nombre de": paciente activo (para tutor: uno de sus pacientes a cargo)
  const [aCargo, setACargo] = useState([]);
  const [pacienteId, setPacienteId] = useState(() => Number(localStorage.getItem('pacienteActivo')) || null);

  const guardar = (res) => {
    localStorage.setItem('token', res.token);
    localStorage.setItem('usuario', JSON.stringify(res.usuario));
    setUsuario(res.usuario);
    return res.usuario;
  };

  const login = async (identificador, password) => guardar((await api.post('/auth/login', { identificador, password })).data);
  const registrar = async (datos) => guardar((await api.post('/auth/registro', datos)).data);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    localStorage.removeItem('pacienteActivo');
    setUsuario(null);
    setACargo([]);
    setPacienteId(null);
  }, []);

  const actualizarUsuario = (u) => {
    localStorage.setItem('usuario', JSON.stringify(u));
    setUsuario(u);
  };

  const cargarACargo = useCallback(async () => {
    if (usuario?.rol !== 'ROLE_TUTOR') return [];
    const { data } = await api.get('/pacientes/a-cargo');
    setACargo(data);
    setPacienteId((actual) => (data.some((p) => p.id === actual) ? actual : data[0]?.id ?? null));
    return data;
  }, [usuario]);

  useEffect(() => {
    if (usuario?.rol === 'ROLE_PACIENTE') setPacienteId(usuario.id);
    else if (usuario?.rol === 'ROLE_TUTOR') cargarACargo().catch(() => {});
  }, [usuario, cargarACargo]);

  useEffect(() => {
    if (pacienteId) localStorage.setItem('pacienteActivo', pacienteId);
  }, [pacienteId]);

  const pacienteActivo =
    usuario?.rol === 'ROLE_PACIENTE'
      ? { id: usuario.id, nombreCompleto: usuario.nombreCompleto, documentoIdentidad: usuario.documentoIdentidad }
      : aCargo.find((p) => p.id === pacienteId) || null;

  return (
    <AuthCtx.Provider value={{ usuario, login, registrar, logout, actualizarUsuario, aCargo, cargarACargo, pacienteId, setPacienteId, pacienteActivo }}>
      {children}
    </AuthCtx.Provider>
  );
}
