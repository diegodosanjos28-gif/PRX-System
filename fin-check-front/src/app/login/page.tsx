'use client';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import Image from 'next/image';
import logoDark from '@/assets/logo-dark.jpeg';
import { login } from '@/lib/api/auth';
import { useAuthStore } from '@/lib/stores/authStore';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

const schema = z.object({
  login: z.string().min(1, 'Login é obrigatório'),
  senha: z.string().min(1, 'Senha é obrigatória'),
});
type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const setAuth = useAuthStore((s) => s.setAuth);
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const { mutate, isPending } = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      const payload = JSON.parse(atob(data.token.split('.')[1]));
      setAuth(data.token, { login: payload.sub, role: payload.role });
      router.replace('/dashboard');
    },
    onError: () => toast.error('Credenciais inválidas'),
  });

  return (
    <div className="min-h-screen flex">
      {/* Left panel — brand hero (desktop only) */}
      <div className="hidden lg:flex lg:w-1/2 xl:w-3/5 relative flex-col items-center justify-center bg-[#0a0a0a] overflow-hidden">
        <div
          className="absolute inset-0 pointer-events-none"
          style={{
            background:
              'radial-gradient(ellipse 60% 40% at 50% 50%, rgba(255,255,255,0.07) 0%, transparent 70%)',
          }}
        />
        <div className="relative z-10 flex flex-col items-center gap-6 px-16">
          <Image
            src={logoDark}
            alt="PRX"
            width={260}
            height={87}
            className="object-contain"
            priority
          />
          <p className="text-white/25 text-xs tracking-[0.25em] uppercase">
            Conciliação Financeira
          </p>
        </div>
        <div className="absolute bottom-8 left-0 right-0 flex justify-center">
          <span className="text-white/10 text-xs tracking-widest uppercase">PRX · Sistema Interno</span>
        </div>
      </div>

      {/* Right panel — form */}
      <div className="flex-1 flex flex-col items-center justify-center px-8 bg-white">
        <div className="w-full max-w-sm space-y-8">
          {/* Logo — dark pill, always visible on any screen size */}
          <div className="flex justify-center">
            <div className="rounded-xl overflow-hidden bg-[#0a0a0a] px-6 py-3">
              <Image
                src={logoDark}
                alt="PRX"
                width={100}
                height={33}
                className="object-contain block"
                priority
              />
            </div>
          </div>

          <div className="space-y-1">
            <h2 className="text-2xl font-bold text-gray-900 tracking-tight">Acessar sistema</h2>
            <p className="text-sm text-gray-400">Entre com suas credenciais para continuar</p>
          </div>

          <form onSubmit={handleSubmit((d) => mutate(d))} className="space-y-5">
            <div className="space-y-1.5">
              <Label htmlFor="login" className="text-gray-700 font-medium">Login</Label>
              <Input
                id="login"
                {...register('login')}
                autoFocus
                className="h-11 border-gray-200 focus:border-gray-900 focus:ring-gray-900 rounded-lg"
                placeholder="seu.login"
              />
              {errors.login && (
                <p className="text-xs text-red-500">{errors.login.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="senha" className="text-gray-700 font-medium">Senha</Label>
              <Input
                id="senha"
                type="password"
                {...register('senha')}
                className="h-11 border-gray-200 focus:border-gray-900 focus:ring-gray-900 rounded-lg"
                placeholder="••••••••"
              />
              {errors.senha && (
                <p className="text-xs text-red-500">{errors.senha.message}</p>
              )}
            </div>

            <Button
              type="submit"
              disabled={isPending}
              className="w-full h-11 bg-[#0a0a0a] hover:bg-black text-white font-medium rounded-lg transition-all duration-150 mt-2"
            >
              {isPending ? 'Entrando...' : 'Entrar'}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
