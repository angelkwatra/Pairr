const BASE_URL = import.meta.env.VITE_API_URL || '';

async function request(path, options = {}) {
  const token = localStorage.getItem('token');
  const headers = { 'Content-Type': 'application/json', ...options.headers };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (response.status === 401) {
    localStorage.removeItem('token');
    // Only redirect if we are not already on the login page to avoid loops
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    return;
  }

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({}));
    // Extract the specific 'error' field if present, otherwise fallback to status text or generic message
    const errorMessage = errorBody.error || errorBody.message || `Request failed: ${response.status}`;
    throw new Error(errorMessage);
  }

  if (response.status === 204) return null;
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: (path, body) => request(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path) => request(path, { method: 'DELETE' }),
};
