import { dirname } from "path";
import { fileURLToPath } from "url";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const compat = new FlatCompat({
  baseDirectory: __dirname,
});

const eslintConfig = [
  ...compat.extends("next/core-web-vitals", "next/typescript"),
  "plugin:tailwindcss/recommended",
  "plugin:no-relative-import-paths/recommended",
  {
    plugins: ["tailwindcss", "no-relative-import-paths"],
    rules: {
      "indent": "error",
      "no-relative-import-paths/no-relative-import-paths": [
        "error",
        {
          allowSameFolder: true,
          prefix: "@",
          rootDir: "src",
        },
      ],
    },
  },
];

export default eslintConfig;
