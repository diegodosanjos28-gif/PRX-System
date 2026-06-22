/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  images: {
    unoptimized: true,
  },
  // Proxy server-side para a API principal e para o microserviço coletor.
  // O browser chama /api/* e /coletor/* (mesma origem) e o servidor Next.js
  // encaminha via DNS interno do Docker — nenhuma URL interna exposta ao browser.
  // As variáveis só são lidas pelo servidor Node.js, nunca pelo bundle do cliente.
  async rewrites() {
    const apiUrl     = process.env.API_INTERNAL_URL     ?? 'http://api:8080';
    const coletorUrl = process.env.COLETOR_INTERNAL_URL ?? 'http://coletor:8081';
    return [
      {
        source: '/api/:path*',
        destination: `${apiUrl}/api/:path*`,
      },
      {
        source: '/coletor/:path*',
        destination: `${coletorUrl}/:path*`,
      },
    ];
  },
};

export default nextConfig;
