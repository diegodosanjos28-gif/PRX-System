'use client';
import { usePathname } from 'next/navigation';

const titles: Record<string, string> = {
  '/dashboard':    'Dashboard',
  '/clientes':     'Clientes',
  '/recebimentos': 'Recebimentos',
  '/conciliacao':  'Conciliação de Taxas',
  '/auditoria':    'Auditoria de Taxas',
  '/mensagens':    'Mensagens',
  '/coleta':       'Coleta Manual',
  '/logs':         'Logs de Coleta',
};

export function Header() {
  const pathname = usePathname();
  const match = Object.entries(titles).find(([key]) => pathname.startsWith(key));
  const title = match?.[1] ?? 'Página';

  return (
    <header className="h-14 border-b bg-white flex items-center px-6 gap-3">
      <span className="block w-1 h-5 rounded-full bg-[#0a0a0a] flex-shrink-0" />
      <h2 className="font-semibold text-gray-800 tracking-tight">{title}</h2>
    </header>
  );
}
