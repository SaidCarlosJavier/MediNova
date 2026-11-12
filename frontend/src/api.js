import axios from 'axios';

export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api' });

// Interceptor JWT
api.interceptors.request.use((cfg) => {
  const t = localStorage.getItem('token');
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});

// Manejo global de 401: sesión expirada -> login
api.interceptors.response.use(
  (r) => r,
  (err) => {
    const url = err.config?.url || '';
    if (err.response?.status === 401 && !url.includes('/auth/login')) {
      localStorage.removeItem('token');
      localStorage.removeItem('usuario');
      if (!window.location.pathname.startsWith('/login')) window.location.href = '/login?expirada=1';
    }
    return Promise.reject(err);
  }
);

export const msg = (e) =>
  e?.response?.data?.mensaje || 'No se pudo completar la operación. Verifique su conexión a internet e intente nuevamente.';

export const fieldErrors = (e) => e?.response?.data?.errores || {};

export const HOME = {
  ROLE_PACIENTE: '/panel',
  ROLE_TUTOR: '/panel',
  ROLE_RECEPCIONISTA: '/recepcion',
  ROLE_MEDICO: '/medico',
  ROLE_FARMACEUTICO: '/farmacia',
  ROLE_ADMIN: '/admin',
};
