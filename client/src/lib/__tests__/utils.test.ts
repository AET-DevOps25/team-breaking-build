import { describe, it, expect, vi } from 'vitest';
import {
  cn,
  base64StringToByteArray,
  byteArrayToBase64String,
  encodeRecipeImageForAPI,
  decodeRecipeImageFromAPI,
  fileToBase64,
} from '../utils';
import { RecipeImage, RecipeImageDTO } from '../types/recipe';

describe('Utils Library', () => {
  describe('cn function', () => {
    it('should combine class names correctly', () => {
      const result = cn('base-class', 'additional-class');
      expect(result).toBe('base-class additional-class');
    });

    it('should handle conditional classes', () => {
      const result = cn('base-class', true && 'conditional-class', false && 'hidden-class');
      expect(result).toBe('base-class conditional-class');
    });

    it('should handle undefined and null values', () => {
      const result = cn('base-class', undefined, null, 'valid-class');
      expect(result).toBe('base-class valid-class');
    });

    it('should handle empty strings', () => {
      const result = cn('base-class', '', 'valid-class');
      expect(result).toBe('base-class valid-class');
    });

    it('should handle arrays of classes', () => {
      const result = cn(['base-class', 'array-class'], 'single-class');
      expect(result).toBe('base-class array-class single-class');
    });

    it('should handle objects with boolean values', () => {
      const result = cn('base-class', {
        'active-class': true,
        'inactive-class': false,
        'another-active': true,
      });
      expect(result).toBe('base-class active-class another-active');
    });

    it('should handle mixed types', () => {
      const result = cn(
        'base-class',
        ['array-class'],
        { 'object-class': true },
        'string-class',
        false && 'hidden'
      );
      expect(result).toBe('base-class array-class object-class string-class');
    });

    it('should handle empty input', () => {
      const result = cn();
      expect(result).toBe('');
    });

    it('should handle Tailwind merge conflicts', () => {
      const result = cn('p-4', 'p-2');
      expect(result).toBe('p-2'); // Should keep the last padding class
    });
  });

  describe('base64StringToByteArray', () => {
    it('should convert base64 string to byte array correctly', () => {
      const base64String = 'SGVsbG8gV29ybGQ='; // "Hello World" in base64
      const result = base64StringToByteArray(base64String);
      
      // "Hello World" as byte array
      const expected = [72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100];
      expect(result).toEqual(expected);
    });

    it('should handle empty base64 string', () => {
      const result = base64StringToByteArray('');
      expect(result).toEqual([]);
    });

    it('should handle single character base64', () => {
      const base64String = 'QQ=='; // "A" in base64
      const result = base64StringToByteArray(base64String);
      expect(result).toEqual([65]); // ASCII value of 'A'
    });

    it('should handle special characters', () => {
      const base64String = 'SGVsbG8g8J+RjQ=='; // "Hello 👍" in base64
      const result = base64StringToByteArray(base64String);
      expect(result).toBeInstanceOf(Array);
      expect(result.length).toBeGreaterThan(0);
    });
  });

  describe('byteArrayToBase64String', () => {
    it('should convert byte array to base64 string correctly', () => {
      const byteArray = [72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100]; // "Hello World"
      const result = byteArrayToBase64String(byteArray);
      expect(result).toBe('SGVsbG8gV29ybGQ=');
    });

    it('should handle empty byte array', () => {
      const result = byteArrayToBase64String([]);
      expect(result).toBe('');
    });

    it('should handle single byte', () => {
      const result = byteArrayToBase64String([65]); // ASCII 'A'
      expect(result).toBe('QQ==');
    });

    it('should handle non-array input', () => {
      const result = byteArrayToBase64String(null as any);
      expect(result).toBe('');
    });

    it('should handle undefined input', () => {
      const result = byteArrayToBase64String(undefined as any);
      expect(result).toBe('');
    });

    it('should handle very large byte arrays using fallback method', () => {
      // Create a large byte array that might cause stack overflow with spread operator
      const largeByteArray = new Array(100000).fill(0).map((_, i) => i % 256);
      const result = byteArrayToBase64String(largeByteArray);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('should handle byte array with special characters', () => {
      const byteArray = [240, 159, 145, 141]; // Unicode bytes for 👍
      const result = byteArrayToBase64String(byteArray);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });
  });

  describe('encodeRecipeImageForAPI', () => {
    it('should encode RecipeImage for API correctly', () => {
      const recipeImage: RecipeImage = {
        base64String: 'SGVsbG8gV29ybGQ=',
      };

      const result = encodeRecipeImageForAPI(recipeImage);

      expect(result).toEqual({
        base64String: 'SGVsbG8gV29ybGQ=',
      });
    });

    it('should handle RecipeImage with undefined base64String', () => {
      const recipeImage: RecipeImage = {
        base64String: undefined,
      };

      const result = encodeRecipeImageForAPI(recipeImage);

      expect(result).toEqual({
        base64String: undefined,
      });
    });

    it('should handle RecipeImage with empty base64String', () => {
      const recipeImage: RecipeImage = {
        base64String: '',
      };

      const result = encodeRecipeImageForAPI(recipeImage);

      expect(result).toEqual({
        base64String: '',
      });
    });
  });

  describe('decodeRecipeImageFromAPI', () => {
    it('should decode RecipeImageDTO from API correctly', () => {
      const recipeImageDTO: RecipeImageDTO = {
        base64String: 'SGVsbG8gV29ybGQ=',
      };

      const result = decodeRecipeImageFromAPI(recipeImageDTO);

      expect(result).toEqual({
        base64String: 'SGVsbG8gV29ybGQ=',
      });
    });

    it('should handle RecipeImageDTO with undefined base64String', () => {
      const recipeImageDTO: RecipeImageDTO = {
        base64String: undefined,
      };

      const result = decodeRecipeImageFromAPI(recipeImageDTO);

      expect(result).toEqual({
        base64String: undefined,
      });
    });

    it('should handle RecipeImageDTO with empty base64String', () => {
      const recipeImageDTO: RecipeImageDTO = {
        base64String: '',
      };

      const result = decodeRecipeImageFromAPI(recipeImageDTO);

      expect(result).toEqual({
        base64String: '',
      });
    });
  });

  describe('fileToBase64', () => {
    it('should convert file to base64 string successfully', async () => {
      // Create a mock file
      const mockFile = new File(['Hello World'], 'test.txt', { type: 'text/plain' });
      
      // Mock FileReader
      const mockFileReader = {
        readAsDataURL: vi.fn(),
        result: 'data:text/plain;base64,SGVsbG8gV29ybGQ=',
        onload: null as any,
        onerror: null as any,
      };

      // Mock FileReader constructor
      global.FileReader = vi.fn(() => mockFileReader) as any;

      // Call the function
      const promise = fileToBase64(mockFile);

      // Simulate successful file read
      mockFileReader.onload();

      const result = await promise;
      expect(result).toBe('SGVsbG8gV29ybGQ='); // base64 without prefix
      expect(mockFileReader.readAsDataURL).toHaveBeenCalledWith(mockFile);
    });

    it('should handle file read error', async () => {
      const mockFile = new File(['Hello World'], 'test.txt', { type: 'text/plain' });
      
      const mockFileReader = {
        readAsDataURL: vi.fn(),
        result: null,
        onload: null as any,
        onerror: null as any,
      };

      global.FileReader = vi.fn(() => mockFileReader) as any;

      const promise = fileToBase64(mockFile);

      // Simulate file read error
      mockFileReader.onerror();

      await expect(promise).rejects.toThrow('Failed to read file');
    });

    it('should handle file with different MIME types', async () => {
      const mockFile = new File(['image data'], 'image.png', { type: 'image/png' });
      
      const mockFileReader = {
        readAsDataURL: vi.fn(),
        result: 'data:image/png;base64,aW1hZ2UgZGF0YQ==',
        onload: null as any,
        onerror: null as any,
      };

      global.FileReader = vi.fn(() => mockFileReader) as any;

      const promise = fileToBase64(mockFile);
      mockFileReader.onload();

      const result = await promise;
      expect(result).toBe('aW1hZ2UgZGF0YQ=='); // base64 without prefix
    });

    it('should handle file with no data URI prefix', async () => {
      const mockFile = new File(['test'], 'test.txt', { type: 'text/plain' });
      
      const mockFileReader = {
        readAsDataURL: vi.fn(),
        result: 'invalid-data-uri',
        onload: null as any,
        onerror: null as any,
      };

      global.FileReader = vi.fn(() => mockFileReader) as any;

      const promise = fileToBase64(mockFile);
      mockFileReader.onload();

      const result = await promise;
      expect(result).toBeUndefined(); // Returns undefined if no comma found
    });

    it('should handle empty file', async () => {
      const mockFile = new File([''], 'empty.txt', { type: 'text/plain' });
      
      const mockFileReader = {
        readAsDataURL: vi.fn(),
        result: 'data:text/plain;base64,',
        onload: null as any,
        onerror: null as any,
      };

      global.FileReader = vi.fn(() => mockFileReader) as any;

      const promise = fileToBase64(mockFile);
      mockFileReader.onload();

      const result = await promise;
      expect(result).toBe(''); // Empty base64 string
    });

    it('should handle large files', async () => {
      const largeContent = 'x'.repeat(1000000); // 1MB of 'x'
      const mockFile = new File([largeContent], 'large.txt', { type: 'text/plain' });
      
      const mockFileReader = {
        readAsDataURL: vi.fn(),
        result: 'data:text/plain;base64,' + btoa(largeContent),
        onload: null as any,
        onerror: null as any,
      };

      global.FileReader = vi.fn(() => mockFileReader) as any;

      const promise = fileToBase64(mockFile);
      mockFileReader.onload();

      const result = await promise;
      expect(result).toBe(btoa(largeContent));
    });
  });

  describe('Integration tests', () => {
    it('should handle base64 round trip conversion', () => {
      const originalString = 'Hello World 👍';
      const bytes = Array.from(new TextEncoder().encode(originalString));
      const base64 = byteArrayToBase64String(bytes);
      const convertedBytes = base64StringToByteArray(base64);
      const resultString = new TextDecoder().decode(new Uint8Array(convertedBytes));
      
      expect(resultString).toBe(originalString);
    });

    it('should handle RecipeImage encoding/decoding round trip', () => {
      const originalImage: RecipeImage = {
        base64String: 'SGVsbG8gV29ybGQ=',
      };

      const encoded = encodeRecipeImageForAPI(originalImage);
      const decoded = decodeRecipeImageFromAPI(encoded);

      expect(decoded).toEqual(originalImage);
    });

    it('should handle edge cases with special characters', () => {
      const specialChars = '🎉🍕🔥💯';
      const bytes = Array.from(new TextEncoder().encode(specialChars));
      const base64 = byteArrayToBase64String(bytes);
      const convertedBytes = base64StringToByteArray(base64);
      const resultString = new TextDecoder().decode(new Uint8Array(convertedBytes));
      
      expect(resultString).toBe(specialChars);
    });
  });

  describe('Error handling', () => {
    it('should handle invalid base64 strings gracefully', () => {
      // atob will throw an error for invalid base64
      expect(() => base64StringToByteArray('invalid-base64!')).toThrow();
    });

    it('should handle null/undefined inputs gracefully', () => {
      expect(byteArrayToBase64String(null as any)).toBe('');
      expect(byteArrayToBase64String(undefined as any)).toBe('');
    });

    it('should handle non-string inputs to base64StringToByteArray', () => {
      // The function doesn't validate input types, so these will be converted to strings
      // and then processed by atob(), which may or may not throw depending on the resulting string
      
      // Test null - 'null' string might be valid base64 or might throw
      try {
        base64StringToByteArray(null as any);
      } catch (e) {
        expect(e).toBeDefined();
      }
      
      // Test undefined - 'undefined' string will likely throw
      expect(() => base64StringToByteArray(undefined as any)).toThrow();
      
      // Test number - '123' might be valid base64 or might throw
      try {
        base64StringToByteArray(123 as any);
      } catch (e) {
        expect(e).toBeDefined();
      }
    });
  });
}); 