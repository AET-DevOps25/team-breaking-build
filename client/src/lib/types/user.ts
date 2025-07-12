export interface UserDetails {
  id: string;
  username?: string;
  firstName?: string;
  lastName?: string;
  email: string;
  emailVerified?: boolean;
}

export interface UserDisplayInfo {
  id: string;
  displayName: string;
  email: string;
  firstName?: string;
  lastName?: string;
  avatar?: string;
} 