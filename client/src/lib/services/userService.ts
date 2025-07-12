import { api } from '@/lib/api';
import { UserDetails, UserDisplayInfo } from '@/lib/types/user';

// Cache for user information to avoid repeated API calls
const userCache = new Map<string, UserDetails>();

// Direct auth service call for user details (not through API gateway)
async function fetchUserFromAuthService(userId: string): Promise<UserDetails> {
  const tokens = localStorage.getItem('tokens');
  if (!tokens) {
    throw new Error('No authentication tokens available');
  }

  const { accessToken } = JSON.parse(tokens);
  if (!accessToken) {
    throw new Error('No access token available');
  }

  const response = await fetch(`/auth/users/${userId}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    throw new Error(`Request failed with status: ${response.status}`);
  }

  return response.json();
}

export async function getUserById(userId: string): Promise<UserDetails | null> {
  try {
    // Check cache first
    if (userCache.has(userId)) {
      return userCache.get(userId)!;
    }

    const response = await fetchUserFromAuthService(userId);
    
    // Cache the result
    userCache.set(userId, response);
    
    return response;
  } catch (error) {
    console.error('Failed to fetch user:', error);
    return null;
  }
}

export async function getUserDisplayInfo(userId: string): Promise<UserDisplayInfo | null> {
  try {
    const userDetails = await getUserById(userId);
    
    if (!userDetails) {
      return null;
    }

    const displayName = userDetails.firstName && userDetails.lastName
      ? `${userDetails.firstName} ${userDetails.lastName}`
      : userDetails.username || userDetails.email.split('@')[0];

    return {
      id: userDetails.id,
      displayName,
      email: userDetails.email,
      firstName: userDetails.firstName,
      lastName: userDetails.lastName,
      avatar: undefined, // Will be handled by the avatar component
    };
  } catch (error) {
    console.error('Failed to get user display info:', error);
    return null;
  }
}

export function clearUserCache(): void {
  userCache.clear();
}

export function removeUserFromCache(userId: string): void {
  userCache.delete(userId);
} 