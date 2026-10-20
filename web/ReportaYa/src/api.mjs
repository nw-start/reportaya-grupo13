import {getToken, clearToken} from './session.mjs';

export async function request(path, method = 'GET', body, base = '') {
 const controller = new AbortController();
 const timer = setTimeout(() => controller.abort(), 60000);
 try {
  const headers = body === undefined ? {} : {'Content-Type': 'application/json'};
  const token = getToken();
  if (token && path !== '/api/auth/login') headers.Authorization = 'Bearer ' + token;
  const response = await fetch(base + path, {
   method,
   headers,
   body: body === undefined ? undefined : JSON.stringify(body),
   signal: controller.signal
  });
  if (response.status === 401 && path !== '/api/auth/login' && token === getToken()) {
   clearToken();
   if (typeof window !== 'undefined') window.dispatchEvent(new Event('session-expired'));
  }
  if (!response.ok) {
   const error = await response.json().catch(() => ({}));
   const message = Array.isArray(error.detail)
    ? error.detail.map(item => item.msg).join(' · ')
    : error.detail;
   throw new Error(message || `Error ${response.status}. No se pudo completar la operación.`);
  }
  if (response.status === 204) return null;
  return await response.json();
 } catch (error) {
  if (error.name === 'AbortError') throw new Error('La API tardó demasiado. Espera unos segundos y vuelve a intentarlo.');
  if (error instanceof TypeError) throw new Error('No se pudo conectar. Comprueba que el servidor del portal siga ejecutándose.');
  throw error;
 } finally {
  clearTimeout(timer);
 }
}
export const api = {
 login: datos => request('/api/auth/login', 'POST', datos),
 me: () => request('/api/auth/me'),
 listar: () => request('/api/incidencias'),
 buscar: criterio => request('/api/incidencias/buscar?criterio=' + encodeURIComponent(criterio)),
 detalle: id => request('/api/incidencias/' + id),
 tipos: () => request('/api/tipos-incidencia'),
 usuarios: () => request('/api/usuarios'),
 registrar: datos => request('/api/incidencias', 'POST', datos),
 actualizar: (id, datos) => request('/api/incidencias/' + id, 'PATCH', datos),
 eliminar: id => request('/api/incidencias/' + id, 'DELETE')
};
