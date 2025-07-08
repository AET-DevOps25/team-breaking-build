import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { RecipeImage, RecipeImageDTO } from './types/recipe';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// Utility functions for image encoding/decoding

/**
 * Convert base64 string to byte array for API requests
 */
export function base64StringToByteArray(base64String: string): number[] {
  const binaryString = atob(base64String);
  const bytes = new Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}

/**
 * Convert byte array from API response to base64 string for display
 */
export function byteArrayToBase64String(byteArray: number[]): string {
  if (!Array.isArray(byteArray) || byteArray.length === 0) {
    return '';
  }

  // Use spread operator to avoid apply() issues with large arrays
  try {
    const binaryString = String.fromCharCode(...byteArray);
    return btoa(binaryString);
  } catch {
    // Fallback for very large arrays that might cause stack overflow
    let binaryString = '';
    for (let i = 0; i < byteArray.length; i++) {
      binaryString += String.fromCharCode(byteArray[i]);
    }
    return btoa(binaryString);
  }
}

/**
 * Convert RecipeImage (client-side) to RecipeImageDTO (API format)
 * Since both formats use base64 strings, this is a simple copy
 */
export function encodeRecipeImageForAPI(image: RecipeImage): RecipeImageDTO {
  return {
    base64String: image.base64String,
  };
}

/**
 * Convert RecipeImageDTO (API format) to RecipeImage (client-side)
 * Since both formats use base64 strings, this is a simple copy
 */
export function decodeRecipeImageFromAPI(imageDTO: RecipeImageDTO): RecipeImage {
  return {
    base64String: imageDTO.base64String,
  };
}

/**
 * Convert File to base64 string
 */
export async function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => {
      // Remove the data:image/...;base64, prefix
      const result = reader.result as string;
      const base64 = result.split(',')[1];
      resolve(base64);
    };
    reader.onerror = () => reject(new Error('Failed to read file'));
  });
}
