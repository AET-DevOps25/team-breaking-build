import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { getUserById, getUserDisplayInfo } from '../userService';
import { UserDetails, UserDisplayInfo } from '../../types/user';

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

describe('User Service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockFetch.mockClear();
    mockLocalStorage.getItem.mockClear();
    mockLocalStorage.setItem.mockClear();
    mockLocalStorage.removeItem.mockClear();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('getUserById', () => {
    it('should fetch user by ID successfully', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123',
        username: 'testuser',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      const result = await getUserById('user-123');

      expect(mockFetch).toHaveBeenCalledWith('/auth/users/user-123', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-access-token',
        },
      });
      expect(result).toEqual(mockUserDetails);
    });

    it('should return cached user if available', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123',
        username: 'testuser',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      // First call should fetch from API and cache
      const result1 = await getUserById('user-123');
      expect(result1).toEqual(mockUserDetails);
      expect(mockFetch).toHaveBeenCalledTimes(1);

      // Second call should return cached result
      const result2 = await getUserById('user-123');
      expect(result2).toEqual(mockUserDetails);
      expect(mockFetch).toHaveBeenCalledTimes(1); // Should not make another API call
    });

    it('should return null when no tokens available', async () => {
      mockLocalStorage.getItem.mockReturnValue(null);

      const result = await getUserById('user-123');

      expect(result).toBeNull();
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should return null when access token is missing', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          refreshToken: 'test-refresh-token',
        })
      );

      const result = await getUserById('user-123');

      expect(result).toBeNull();
      expect(mockFetch).not.toHaveBeenCalled();
    });

    it('should return null when API request fails', async () => {
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

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await getUserById('user-123');

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalledWith('Failed to fetch user:', expect.any(Error));

      consoleSpy.mockRestore();
    });

    it('should return null when network error occurs', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockRejectedValueOnce(new Error('Network Error'));

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await getUserById('user-123');

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalledWith('Failed to fetch user:', expect.any(Error));

      consoleSpy.mockRestore();
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
        json: async () => {
          throw new Error('Invalid JSON');
        },
      });

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await getUserById('user-123');

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalledWith('Failed to fetch user:', expect.any(Error));

      consoleSpy.mockRestore();
    });

    it('should handle invalid tokens JSON', async () => {
      mockLocalStorage.getItem.mockReturnValue('invalid-json');

      const result = await getUserById('user-123');

      expect(result).toBeNull();
      expect(mockFetch).not.toHaveBeenCalled();
    });
  });

  describe('getUserDisplayInfo', () => {
    it('should get user display info with full name', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123',
        username: 'testuser',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      const result = await getUserDisplayInfo('user-123');

      const expectedDisplayInfo: UserDisplayInfo = {
        id: 'user-123',
        displayName: 'John Doe',
        email: 'john.doe@example.com',
        firstName: 'John',
        lastName: 'Doe',
        avatar: undefined,
      };

      expect(result).toEqual(expectedDisplayInfo);
    });

    it('should get user display info with username when no full name', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123',
        username: 'testuser',
        firstName: undefined,
        lastName: undefined,
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      const result = await getUserDisplayInfo('user-123');

      const expectedDisplayInfo: UserDisplayInfo = {
        id: 'user-123',
        displayName: 'testuser',
        email: 'john.doe@example.com',
        firstName: undefined,
        lastName: undefined,
        avatar: undefined,
      };

      expect(result).toEqual(expectedDisplayInfo);
    });

    it('should get user display info with email prefix when no username or full name', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123',
        username: undefined,
        firstName: undefined,
        lastName: undefined,
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      const result = await getUserDisplayInfo('user-123');

      const expectedDisplayInfo: UserDisplayInfo = {
        id: 'user-123',
        displayName: 'john.doe',
        email: 'john.doe@example.com',
        firstName: undefined,
        lastName: undefined,
        avatar: undefined,
      };

      expect(result).toEqual(expectedDisplayInfo);
    });

    it('should handle partial name information', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123',
        username: 'testuser',
        firstName: 'John',
        lastName: undefined,
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      const result = await getUserDisplayInfo('user-123');

      const expectedDisplayInfo: UserDisplayInfo = {
        id: 'user-123',
        displayName: 'testuser', // Should fall back to username when last name is missing
        email: 'john.doe@example.com',
        firstName: 'John',
        lastName: undefined,
        avatar: undefined,
      };

      expect(result).toEqual(expectedDisplayInfo);
    });

    it('should handle another partial name case', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123',
        username: 'testuser',
        firstName: undefined,
        lastName: 'Doe',
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      const result = await getUserDisplayInfo('user-123');

      const expectedDisplayInfo: UserDisplayInfo = {
        id: 'user-123',
        displayName: 'testuser', // Should fall back to username when first name is missing
        email: 'john.doe@example.com',
        firstName: undefined,
        lastName: 'Doe',
        avatar: undefined,
      };

      expect(result).toEqual(expectedDisplayInfo);
    });

    it('should return null when getUserById returns null', async () => {
      mockLocalStorage.getItem.mockReturnValue(null);

      const result = await getUserDisplayInfo('user-123');

      expect(result).toBeNull();
    });

    it('should return null when getUserById fails', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockRejectedValueOnce(new Error('Network Error'));

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await getUserDisplayInfo('user-123');

      expect(result).toBeNull();

      consoleSpy.mockRestore();
    });

    it('should handle errors and log them', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockRejectedValueOnce(new Error('Network Error'));

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await getUserDisplayInfo('user-123');

      expect(result).toBeNull();
      expect(consoleSpy).toHaveBeenCalledWith('Failed to get user display info:', expect.any(Error));

      consoleSpy.mockRestore();
    });

    it('should handle complex email addresses', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123',
        username: undefined,
        firstName: undefined,
        lastName: undefined,
        email: 'user.with.dots+tags@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      const result = await getUserDisplayInfo('user-123');

      const expectedDisplayInfo: UserDisplayInfo = {
        id: 'user-123',
        displayName: 'user.with.dots+tags',
        email: 'user.with.dots+tags@example.com',
        firstName: undefined,
        lastName: undefined,
        avatar: undefined,
      };

      expect(result).toEqual(expectedDisplayInfo);
    });
  });

  describe('Edge cases and error handling', () => {
    it('should handle empty user ID', async () => {
      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 400,
      });

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await getUserById('');

      expect(result).toBeNull();
      expect(mockFetch).toHaveBeenCalledWith('/auth/users/', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-access-token',
        },
      });

      consoleSpy.mockRestore();
    });

    it('should handle special characters in user ID', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123@special',
        username: 'testuser',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      const result = await getUserById('user-123@special');

      expect(result).toEqual(mockUserDetails);
      expect(mockFetch).toHaveBeenCalledWith('/auth/users/user-123@special', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer test-access-token',
        },
      });
    });

    it('should handle multiple concurrent requests for the same user', async () => {
      const mockUserDetails: UserDetails = {
        id: 'user-123',
        username: 'testuser',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        json: async () => mockUserDetails,
      });

      // Make multiple concurrent requests
      const promises = [
        getUserById('user-123'),
        getUserById('user-123'),
        getUserById('user-123'),
      ];

      const results = await Promise.all(promises);

      // All should return the same result
      results.forEach((result) => {
        expect(result).toEqual(mockUserDetails);
      });

      // But API should only be called once due to caching
      expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('should handle different users without cache interference', async () => {
      const mockUserDetails1: UserDetails = {
        id: 'user-123',
        username: 'testuser1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        emailVerified: true,
      };

      const mockUserDetails2: UserDetails = {
        id: 'user-456',
        username: 'testuser2',
        firstName: 'Jane',
        lastName: 'Smith',
        email: 'jane.smith@example.com',
        emailVerified: true,
      };

      mockLocalStorage.getItem.mockReturnValue(
        JSON.stringify({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token',
        })
      );

      mockFetch
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => mockUserDetails1,
        })
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
          json: async () => mockUserDetails2,
        });

      const result1 = await getUserById('user-123');
      const result2 = await getUserById('user-456');

      expect(result1).toEqual(mockUserDetails1);
      expect(result2).toEqual(mockUserDetails2);
      expect(mockFetch).toHaveBeenCalledTimes(2);
    });
  });
}); 