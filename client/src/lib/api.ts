const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3000';

async function getAuthHeader(): Promise<Record<string, string>> {
  const tokens = localStorage.getItem('tokens');
  if (!tokens) {
    window.location.href = '/login';
    return {};
  }

  const { accessToken } = JSON.parse(tokens);
  if (!accessToken) {
    window.location.href = '/login';
    return {};
  }

  return {
    Authorization: `Bearer ${accessToken}`,
  };
}

export async function apiRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const headers = await getAuthHeader();

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
      ...options.headers,
    },
  });

  if (response.status === 401) {
    // Token expired, try to refresh
    try {
      // Get refresh token from localStorage and make refresh request
      const tokens = localStorage.getItem('tokens');
      if (!tokens) {
        window.location.href = '/login';
        throw new Error('No tokens available');
      }

      const { refreshToken } = JSON.parse(tokens);
      const refreshResponse = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ token: refreshToken }),
      });

      if (!refreshResponse.ok) {
        window.location.href = '/login';
        throw new Error('Token refresh failed');
      }

      const refreshData = await refreshResponse.json();
      const newToken = refreshData.access_token;

      // Retry the request with the new token
      const retryResponse = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${newToken}`,
          ...options.headers,
        },
      });

      if (!retryResponse.ok) {
        throw new Error('Request failed after token refresh');
      }

      return retryResponse.json();
    } catch (error) {
      // If refresh fails, redirect to login
      window.location.href = '/login';
      throw error;
    }
  }

  if (!response.ok) {
    throw new Error('Request failed');
  }

  return response.json();
}

export async function publicApiRequest<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  if (!response.ok) {
    throw new Error('Request failed');
  }

  return response.json();
}

export const api = {
  get: <T>(endpoint: string, options?: RequestInit) => apiRequest<T>(endpoint, { ...options, method: 'GET' }),

  post: <T>(endpoint: string, data: unknown, options?: RequestInit) =>
    apiRequest<T>(endpoint, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data),
    }),

  put: <T>(endpoint: string, data: unknown, options?: RequestInit) =>
    apiRequest<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  delete: <T>(endpoint: string, options?: RequestInit) => apiRequest<T>(endpoint, { ...options, method: 'DELETE' }),
};

export const publicApi = {
  get: <T>(endpoint: string, options?: RequestInit) => publicApiRequest<T>(endpoint, { ...options, method: 'GET' }),

  post: <T>(endpoint: string, data: unknown, options?: RequestInit) =>
    publicApiRequest<T>(endpoint, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data),
    }),

  put: <T>(endpoint: string, data: unknown, options?: RequestInit) =>
    publicApiRequest<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  delete: <T>(endpoint: string, options?: RequestInit) =>
    publicApiRequest<T>(endpoint, { ...options, method: 'DELETE' }),
};
