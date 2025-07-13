import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { apiRequest, publicApiRequest, api, publicApi } from '../api';

// Mock fetch globally
const mockFetch = vi.fn();
global.fetch = mockFetch;

// Mock localStorage
const mockLocalStorage = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
};
Object.defineProperty(window, 'localStorage', { value: mockLocalStorage });

// Mock window.location
delete (window as any).location;
(window as any).location = { href: '' };

describe('API Library', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockFetch.mockClear();
    mockLocalStorage.getItem.mockClear();
    mockLocalStorage.setItem.mockClear();
    mockLocalStorage.removeItem.mockClear();
    window.location.href = '';
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('apiRequest', () => {
    it('should make authenticated request successfully', async () => {
      // Mock tokens in localStorage
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      // Mock successful response
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'test-data' }),
      });

      const result = await apiRequest('/test-endpoint');

      expect(mockFetch).toHaveBeenCalledWith('/api/test-endpoint', {
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-access-token',
        },
      });
      expect(result).toEqual({ data: 'test-data' });
    });

    it('should redirect to login when no tokens available', async () => {
      mockLocalStorage.getItem.mockReturnValue(null);

      // Mock fetch to return a proper response since apiRequest still calls fetch
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'test-data' }),
      });

      await apiRequest('/test-endpoint');

      expect(window.location.href).toBe('/login');
    });

    it('should redirect to login when access token is missing', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          refreshToken: 'test-refresh-token',
        })
      );

      // Mock fetch to return a proper response since apiRequest still calls fetch
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'test-data' }),
      });

      await apiRequest('/test-endpoint');

      expect(window.location.href).toBe('/login');
    });

    it('should handle 401 error and refresh token successfully', async () => {
      // Mock tokens in localStorage
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'expired-token',
          refreshToken: 'test-refresh-token',
        })
      );

      // Mock initial 401 response
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      // Mock successful refresh response
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          access_token: 'new-access-token',
          refresh_token: 'new-refresh-token',
        }),
      });

      // Mock successful retry response
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'test-data' }),
      });

      const result = await apiRequest('/test-endpoint');

      expect(mockFetch).toHaveBeenCalledTimes(3);
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        'tokens',
        JSON.stringify({
          accessToken: 'new-access-token',
          refreshToken: 'new-refresh-token',
        })
      );
      expect(result).toEqual({ data: 'test-data' });
    });

    it('should handle 401 refresh token error and redirect to login', async () => {
      // Mock tokens in localStorage
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'expired-token',
          refreshToken: 'expired-refresh-token',
        })
      );

      // Mock initial 401 response
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      // Mock failed refresh response (401)
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      await expect(apiRequest('/test-endpoint')).rejects.toThrow(
        'Refresh token is invalid or expired'
      );

      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('tokens');
      expect(window.location.href).toBe('/login');
    });

    it('should handle refresh token failure and redirect to login', async () => {
      // Mock tokens in localStorage
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'expired-token',
          refreshToken: 'test-refresh-token',
        })
      );

      // Mock initial 401 response
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      // Mock failed refresh response (500)
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
      });

      await expect(apiRequest('/test-endpoint')).rejects.toThrow(
        'Token refresh failed with status: 500'
      );

      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('tokens');
      expect(window.location.href).toBe('/login');
    });

    it('should handle non-401 errors properly', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
      });

      await expect(apiRequest('/test-endpoint')).rejects.toThrow(
        'Request failed with status: 404'
      );
    });

    it('should handle 204 response correctly', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204,
        headers: new Headers({ 'content-length': '0' }),
      });

      const result = await apiRequest('/test-endpoint');

      expect(result).toBeUndefined();
    });

    it('should handle empty content-length response correctly', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-length': '0' }),
      });

      const result = await apiRequest('/test-endpoint');

      expect(result).toBeUndefined();
    });

    it('should handle missing refresh token in localStorage', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'expired-token',
        })
      );

      // Mock initial 401 response
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      await expect(apiRequest('/test-endpoint')).rejects.toThrow(
        'No refresh token available'
      );

      expect(window.location.href).toBe('/login');
    });

    it('should handle missing access token in refresh response', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'expired-token',
          refreshToken: 'test-refresh-token',
        })
      );

      // Mock initial 401 response
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      // Mock refresh response without access token
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          refresh_token: 'new-refresh-token',
        }),
      });

      await expect(apiRequest('/test-endpoint')).rejects.toThrow(
        'No access token received from refresh'
      );
    });

    it('should handle retry request failure after token refresh', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'expired-token',
          refreshToken: 'test-refresh-token',
        })
      );

      // Mock initial 401 response
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      // Mock successful refresh response
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          access_token: 'new-access-token',
          refresh_token: 'new-refresh-token',
        }),
      });

      // Mock failed retry response
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
      });

      await expect(apiRequest('/test-endpoint')).rejects.toThrow(
        'Request failed after token refresh: 500'
      );
    });
  });

  describe('publicApiRequest', () => {
    it('should make public request successfully', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'public-data' }),
      });

      const result = await publicApiRequest('/public-endpoint');

      expect(mockFetch).toHaveBeenCalledWith('/api/public-endpoint', {
        headers: {
          'Content-Type': 'application/json',
        },
      });
      expect(result).toEqual({ data: 'public-data' });
    });

    it('should handle public request errors', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 404,
      });

      await expect(publicApiRequest('/public-endpoint')).rejects.toThrow(
        'Request failed with status: 404'
      );
    });

    it('should handle 204 response correctly', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204,
        headers: new Headers({ 'content-length': '0' }),
      });

      const result = await publicApiRequest('/public-endpoint');

      expect(result).toBeUndefined();
    });
  });

  describe('API convenience methods', () => {
    beforeEach(() => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );
    });

    it('should make GET request', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'get-data' }),
      });

      const result = await api.get('/test-endpoint');

      expect(mockFetch).toHaveBeenCalledWith('/api/test-endpoint', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-access-token',
        },
      });
      expect(result).toEqual({ data: 'get-data' });
    });

    it('should make POST request with data', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'post-data' }),
      });

      const testData = { name: 'test' };
      const result = await api.post('/test-endpoint', testData);

      expect(mockFetch).toHaveBeenCalledWith('/api/test-endpoint', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-access-token',
        },
        body: JSON.stringify(testData),
      });
      expect(result).toEqual({ data: 'post-data' });
    });

    it('should make POST request without data', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'post-data' }),
      });

      const result = await api.post('/test-endpoint');

      expect(mockFetch).toHaveBeenCalledWith('/api/test-endpoint', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-access-token',
        },
        body: undefined,
      });
      expect(result).toEqual({ data: 'post-data' });
    });

    it('should make PUT request with data', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'put-data' }),
      });

      const testData = { name: 'updated' };
      const result = await api.put('/test-endpoint', testData);

      expect(mockFetch).toHaveBeenCalledWith('/api/test-endpoint', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-access-token',
        },
        body: JSON.stringify(testData),
      });
      expect(result).toEqual({ data: 'put-data' });
    });

    it('should make DELETE request', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 204,
        headers: new Headers({ 'content-length': '0' }),
      });

      const result = await api.delete('/test-endpoint');

      expect(mockFetch).toHaveBeenCalledWith('/api/test-endpoint', {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-access-token',
        },
      });
      expect(result).toBeUndefined();
    });
  });

  describe('Public API convenience methods', () => {
    it('should make public GET request', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'public-get-data' }),
      });

      const result = await publicApi.get('/public-endpoint');

      expect(mockFetch).toHaveBeenCalledWith('/api/public-endpoint', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });
      expect(result).toEqual({ data: 'public-get-data' });
    });

    it('should make public POST request', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ data: 'public-post-data' }),
      });

      const testData = { name: 'public-test' };
      const result = await publicApi.post('/public-endpoint', testData);

      expect(mockFetch).toHaveBeenCalledWith('/api/public-endpoint', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(testData),
      });
      expect(result).toEqual({ data: 'public-post-data' });
    });
  });

  describe('Error handling edge cases', () => {
    it('should handle network errors', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      await expect(apiRequest('/test-endpoint')).rejects.toThrow('Network error');
    });

    it('should handle JSON parsing errors', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => {
          throw new Error('Invalid JSON');
        },
      });

      await expect(apiRequest('/test-endpoint')).rejects.toThrow('Invalid JSON');
    });

    it('should handle refresh token JSON parsing errors', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'expired-token',
          refreshToken: 'test-refresh-token',
        })
      );

      // Mock initial 401 response
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      // Mock refresh response with JSON parsing error
      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => {
          throw new Error('Invalid JSON in refresh response');
        },
      });

      await expect(apiRequest('/test-endpoint')).rejects.toThrow(
        'Invalid JSON in refresh response'
      );
    });
  });
}); 