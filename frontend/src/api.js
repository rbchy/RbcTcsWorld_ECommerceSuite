import axios from 'axios';

/** All calls go to /api (proxied to the backend). The JWT from login is sent automatically. */
const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

/** Turns the backend's JSON error ({message, fieldErrors}) into one readable sentence. */
export function errorText(err) {
  const data = err?.response?.data;
  if (!data) return 'Cannot reach the server. Is the backend running on port 8081?';
  const fields = data.fieldErrors && Object.entries(data.fieldErrors).map(([f, m]) => `${f}: ${m}`).join(', ');
  return fields ? `${data.message} (${fields})` : data.message || `Error ${err.response.status}`;
}

export const money = v => `$${Number(v ?? 0).toFixed(2)}`;

export default api;
