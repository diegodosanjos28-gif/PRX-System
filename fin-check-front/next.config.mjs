/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  images: {
    unoptimized: true,
  },
  // Proxy server-side para o microserviço coletor.
  // O browser chama /coletor/* (mesma origem) e o servidor Next.js
  // encaminha para o container coletor via DNS interno do Docker.
  // COLETOR_INTERNAL_URL só é lida pelo servidor Node.js — nunca exposta ao browser.
  async rewrites() {
    const coletorUrl = process.env.COLETOR_INTERNAL_URL ?? 'http://coletor:8081';
    return [
      {
        source: '/coletor/:path*',
        destination: `${coletorUrl}/:path*`,
      },
    ];
  },
};

export default nextConfig;
