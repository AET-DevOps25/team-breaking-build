import { dirname } from 'path';
import { fileURLToPath } from 'url';
import { FlatCompat } from '@eslint/eslintrc';

// Import the actual plugin modules:
import noRelativeImportPaths from 'eslint-plugin-no-relative-import-paths';
import tailwindcss from 'eslint-plugin-tailwindcss';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const compat = new FlatCompat({
  baseDirectory: __dirname,
});

const eslintConfig = [
  ...compat.extends('next/core-web-vitals', 'next/typescript', 'plugin:tailwindcss/recommended'),
  {
    plugins: {
      tailwindcss,
      'no-relative-import-paths': noRelativeImportPaths,
    },
    rules: {
      indent: 'error',
      'no-relative-import-paths/no-relative-import-paths': [
        'error',
        {
          allowSameFolder: true,
          prefix: '@',
          rootDir: 'src',
        },
      ],
    },
  },
];

export default eslintConfig;
