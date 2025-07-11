'use client';

import { createContext, useContext, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

interface User {
  id: string;
  email: string;
  firstName?: string;
  lastName?: string;
}

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>;
  logout: () => void;
  refreshAccessToken: () => Promise<string>;
  getAuthHeaders: () => Promise<HeadersInit>;
  fetchWithAuth: (url: string, options?: RequestInit) => Promise<Response>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const AUTH_BASE_URL = '/api/server/auth';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    // Check for existing session on mount
    const storedUser = localStorage.getItem('user');
    const storedTokens = localStorage.getItem('tokens');

    if (storedUser && storedTokens) {
      setUser(JSON.parse(storedUser));
    }
    setIsLoading(false);
  }, []);

  const login = async (email: string, password: string) => {
    try {
      const response = await fetch(`${AUTH_BASE_URL}/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          password,
        }),
      });

      if (!response.ok) {
        throw new Error('Login failed');
      }

      const data = await response.json();
      const tokens: AuthTokens = {
        accessToken: data.access_token,
        refreshToken: data.refresh_token,
      };

      // Store tokens in localStorage
      localStorage.setItem('tokens', JSON.stringify(tokens));

      // Fetch user info with the new access token
      await fetchUserInfo(tokens.accessToken);

      router.push('/');
    } catch (error) {
      throw error;
    }
  };

  const register = async (email: string, password: string, firstName: string, lastName: string) => {
    try {
      const response = await fetch(`${AUTH_BASE_URL}/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          password,
          firstName,
          lastName,
        }),
      });

      if (!response.ok) {
        throw new Error('Registration failed');
      }

      // After successful registration, log the user in
      // The login function will handle fetching user info
      await login(email, password);
    } catch (error) {
      throw error;
    }
  };

  const refreshAccessToken = async (): Promise<string> => {
    const storedTokens = localStorage.getItem('tokens');
    if (!storedTokens) {
      throw new Error('No refresh token available');
    }

    const { refreshToken } = JSON.parse(storedTokens);
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await fetch(`${AUTH_BASE_URL}/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        token: refreshToken,
      }),
    });

    if (!response.ok) {
      throw new Error(`Token refresh failed: ${response.status}`);
    }

    const data = await response.json();

    if (!data.access_token) {
      throw new Error('No access token received from refresh');
    }

    const tokens: AuthTokens = {
      accessToken: data.access_token,
      refreshToken: data.refresh_token || refreshToken, // Keep old refresh token if new one not provided
    };

    localStorage.setItem('tokens', JSON.stringify(tokens));
    return tokens.accessToken;
  };

  const fetchUserInfo = async (accessToken: string) => {
    try {
      const response = await fetch(`${AUTH_BASE_URL}/userinfo`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${accessToken}`,
        },
      });

      if (!response.ok) {
        throw new Error('Failed to fetch user info');
      }

      const userData = await response.json();
      const user: User = {
        id: userData.sub || userData.id,
        email: userData.email,
        firstName: userData.given_name || userData.firstName,
        lastName: userData.family_name || userData.lastName,
      };

      localStorage.setItem('user', JSON.stringify(user));
      setUser(user);
    } catch (error) {
      throw error;
    }
  };

  const getAuthHeaders = async (): Promise<HeadersInit> => {
    const storedTokens = localStorage.getItem('tokens');
    if (!storedTokens) {
      throw new Error('No tokens available');
    }

    const { accessToken } = JSON.parse(storedTokens);
    return {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
    };
  };

  const fetchWithAuth = async (url: string, options: RequestInit = {}): Promise<Response> => {
    const headers = await getAuthHeaders();

    const response = await fetch(url, {
      ...options,
      headers: {
        ...headers,
        ...options.headers,
      },
    });

    // If we get a 401, try to refresh the token and retry
    if (response.status === 401) {
      try {
        const newAccessToken = await refreshAccessToken();
        const newHeaders = {
          ...headers,
          Authorization: `Bearer ${newAccessToken}`,
          ...options.headers,
        };

        // Retry the request with the new token
        return await fetch(url, {
          ...options,
          headers: newHeaders,
        });
      } catch (refreshError) {
        // Only logout if it's a refresh token issue
        const errorMessage = refreshError instanceof Error ? refreshError.message : String(refreshError);
        if (errorMessage.includes('No refresh token available') || errorMessage.includes('Token refresh failed')) {
          logout();
        }
        throw new Error('Authentication failed');
      }
    }

    return response;
  };

  const logout = () => {
    localStorage.removeItem('tokens');
    localStorage.removeItem('user');
    setUser(null);
    router.push('/login');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
        refreshAccessToken,
        getAuthHeaders,
        fetchWithAuth,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
