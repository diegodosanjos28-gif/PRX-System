'use client';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { LayoutDashboard, Users, MessageSquare, FileText, DollarSign, BarChart2, LogOut, RefreshCw, LayoutTemplate, LineChart } from 'lucide-react';
import Image from 'next/image';
import logoDark from '@/assets/logo-dark.jpeg';
import { useAuthStore } from '@/lib/stores/authStore';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

const links = [
  { href: '/dashboard',    label: 'Dashboard',           icon: LayoutDashboard },
  { href: '/clientes',     label: 'Clientes',             icon: Users           },
  { href: '/recebimentos', label: 'Recebimentos',         icon: DollarSign      },
  { href: '/conciliacao',  label: 'Conciliação de Taxas', icon: BarChart2       },
  { href: '/experiencia-cliente', label: 'Experiência do Cliente', icon: LineChart },
  { href: '/mensagens',    label: 'Mensagens',            icon: MessageSquare   },
  { href: '/templates',    label: 'Templates',            icon: LayoutTemplate  },
  { href: '/coleta',       label: 'Coleta Manual',        icon: RefreshCw       },
  { href: '/logs',         label: 'Logs de Coleta',       icon: FileText        },
];

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, clearAuth } = useAuthStore();

  const handleLogout = () => {
    clearAuth();
    router.replace('/login');
  };

  return (
    <aside className="fixed left-0 top-0 h-screen w-64 flex flex-col bg-[#0a0a0a] border-r border-white/10">
      {/* Logo area */}
      <div className="flex items-center justify-center px-6 py-5 border-b border-white/10">
        <Image
          src={logoDark}
          alt="PRX"
          width={110}
          height={37}
          className="object-contain"
          priority
        />
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {links.map(({ href, label, icon: Icon }) => {
          const active = pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                'group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-150',
                active
                  ? 'bg-white text-black'
                  : 'text-white/50 hover:bg-white/8 hover:text-white'
              )}
            >
              <Icon
                className={cn(
                  'h-4 w-4 flex-shrink-0 transition-colors',
                  active ? 'text-black' : 'text-white/40 group-hover:text-white'
                )}
              />
              {label}
            </Link>
          );
        })}
      </nav>

      {/* User footer */}
      <div className="px-3 py-4 border-t border-white/10 space-y-1">
        <p className="px-3 text-xs text-white/30 truncate">{user?.login}</p>
        <Button
          variant="ghost"
          size="sm"
          className="w-full justify-start gap-2 text-white/40 hover:text-white hover:bg-white/8"
          onClick={handleLogout}
        >
          <LogOut className="h-4 w-4" />
          Sair
        </Button>
      </div>
    </aside>
  );
}
