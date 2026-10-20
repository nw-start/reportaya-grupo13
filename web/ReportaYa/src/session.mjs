let accessToken = '';

export function setToken(token) {
 accessToken = token;
}

export function getToken() {
 return accessToken;
}

export function clearToken() {
 accessToken = '';
}

export function tokenExpiresAt(token) {
 try {
  const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
  const payload = JSON.parse(atob(part));
  return Number.isFinite(payload.exp) ? payload.exp * 1000 : 0;
 } catch {
  return 0;
 }
}
