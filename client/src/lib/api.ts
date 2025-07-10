const AUTH_BASE_URL =
  process.env.GATEWAY_SERVICE_BASE_URL + ':' + process.env.GATEWAY_SERVICE_PORT || 'http://localhost:8089';
export const API_BASE_URL =
  process.env.KEYCLOAK_SPI_BASE_URL + ':' + process.env.KEYCLOAK_SPI_PORT || 'http://localhost:8090';

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
      if (!refreshToken) {
        window.location.href = '/login';
        throw new Error('No refresh token available');
      }

      const refreshResponse = await fetch(`${AUTH_BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ token: refreshToken }),
      });

      // If refresh fails with 401, it means the refresh token is invalid/expired
      if (refreshResponse.status === 401) {
        localStorage.removeItem('tokens');
        window.location.href = '/login';
        throw new Error('Refresh token is invalid or expired');
      }

      // If refresh fails with other status codes, logout the user
      if (!refreshResponse.ok) {
        localStorage.removeItem('tokens');
        window.location.href = '/login';
        throw new Error(`Token refresh failed with status: ${refreshResponse.status}`);
      }

      const refreshData = await refreshResponse.json();
      const newToken = refreshData.access_token;

      if (!newToken) {
        throw new Error('No access token received from refresh');
      }

      // Update tokens in localStorage with the new tokens
      const updatedTokens = {
        accessToken: newToken,
        refreshToken: refreshData.refresh_token || refreshToken, // Keep old refresh token if new one not provided
      };
      localStorage.setItem('tokens', JSON.stringify(updatedTokens));

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
        // Don't redirect to login here, just throw the error
        throw new Error(`Request failed after token refresh: ${retryResponse.status}`);
      }

      return retryResponse.json();
    } catch (error) {
      // Only redirect to login if it's a refresh token issue
      const errorMessage = error instanceof Error ? error.message : String(error);
      if (errorMessage.includes('Refresh token is invalid') || errorMessage.includes('No tokens available')) {
        window.location.href = '/login';
      }
      throw error;
    }
  }

  if (!response.ok) {
    throw new Error(`Request failed with status: ${response.status}`);
  }

  // For DELETE requests or responses with no content, return void
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T;
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
    throw new Error(`Request failed with status: ${response.status}`);
  }

  // For DELETE requests or responses with no content, return void
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as T;
  }

  return response.json();
}

// Convenience methods
export const api = {
  get: <T>(endpoint: string, options: RequestInit = {}): Promise<T> =>
    apiRequest<T>(endpoint, { ...options, method: 'GET' }),
  post: <T>(endpoint: string, data?: unknown, options: RequestInit = {}): Promise<T> =>
    apiRequest<T>(endpoint, {
      ...options,
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    }),
  put: <T>(endpoint: string, data?: unknown, options: RequestInit = {}): Promise<T> =>
    apiRequest<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    }),
  delete: <T>(endpoint: string, options: RequestInit = {}): Promise<T> =>
    apiRequest<T>(endpoint, { ...options, method: 'DELETE' }),
};

export const publicApi = {
  get: <T>(endpoint: string, options: RequestInit = {}): Promise<T> =>
    publicApiRequest<T>(endpoint, { ...options, method: 'GET' }),
  post: <T>(endpoint: string, data?: unknown, options: RequestInit = {}): Promise<T> =>
    publicApiRequest<T>(endpoint, {
      ...options,
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    }),
};
