const AUTH_BASE_URL = process.env.KEYCLOAK_BASE_URL || 'http://recipefy-keycloak-service-keycloak-spi:8080/auth';
const API_BASE_URL = process.env.GATEWAY_SERVICE_BASE_URL || 'http://recipefy-api-gateway:8080';

export async function serverApiRequest<T>(
  endpoint: string,
  accessToken?: string,
  options: RequestInit = {},
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw new Error(`Request failed with status: ${response.status}`);
  }

  // For DELETE requests or responses with no content, return void
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T;
  }

  return response.json();
}

export async function serverAuthRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${AUTH_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    },
  });

  if (!response.ok) {
    throw new Error(`Auth request failed with status: ${response.status}`);
  }

  return response.json();
}

// Convenience methods for server-side API calls
export const serverApi = {
  get: <T>(endpoint: string, accessToken?: string, options: RequestInit = {}): Promise<T> =>
    serverApiRequest<T>(endpoint, accessToken, { ...options, method: 'GET' }),
  post: <T>(endpoint: string, data?: unknown, accessToken?: string, options: RequestInit = {}): Promise<T> =>
    serverApiRequest<T>(endpoint, accessToken, {
      ...options,
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    }),
  put: <T>(endpoint: string, data?: unknown, accessToken?: string, options: RequestInit = {}): Promise<T> =>
    serverApiRequest<T>(endpoint, accessToken, {
      ...options,
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    }),
  delete: <T>(endpoint: string, accessToken?: string, options: RequestInit = {}): Promise<T> =>
    serverApiRequest<T>(endpoint, accessToken, { ...options, method: 'DELETE' }),
};

export const serverAuth = {
  get: <T>(endpoint: string, options: RequestInit = {}): Promise<T> =>
    serverAuthRequest<T>(endpoint, { ...options, method: 'GET' }),
  post: <T>(endpoint: string, data?: unknown, options: RequestInit = {}): Promise<T> =>
    serverAuthRequest<T>(endpoint, {
      ...options,
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    }),
};
