import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  images: {
    domains: ['images.unsplash.com'],
  },

  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://recipefy-api-gateway:8080/:path*',
      },
      {
        source: '/auth/:path*',
        destination: 'http://recipefy-keycloak-service-keycloak-spi:8080/auth/:path*',
      },
    ];
  },
};

export default nextConfig;
