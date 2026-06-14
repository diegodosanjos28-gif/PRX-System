'use client';

import { AlertTriangle, Clock, CheckCircle2 } from 'lucide-react';
import { ImplantacaoCliente } from '@/lib/types/entities';
import { cn } from '@/lib/utils';

// ─── Tipos ────────────────────────────────────────────────────────────────────

type Severidade = 'critico' | 'atencao';

interface Alerta {
  id:        string;
  mensagem:  string;
  severidade: Severidade;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

const ETAPA_LABELS: Record<string, string> = {
  curral:     'Curral',
  pre:        'Pré-Largada',
  corrida:    'Corrida',
  onboarding: 'Onboarding',
};

function diasDesde(dataIso: string): number {
  return Math.floor(
    (Date.now() - new Date(dataIso).getTime()) / 86_400_000,
  );
}

function gerarAlertas(implantacoes: ImplantacaoCliente[]): Alerta[] {
  const alertas: Alerta[] = [];

  for (const impl of implantacoes) {
    const nome  = impl.clienteRazaoSocial;
    const etapa = ETAPA_LABELS[impl.etapa] ?? impl.etapa;

    // Regra 1 — status travado
    if (impl.status === 'travado') {
      alertas.push({
        id:         `${impl.id}:travado`,
        mensagem:   `${nome} está travado na etapa ${etapa}.`,
        severidade: 'critico',
      });
    }

    // Regra 2 — status aguardando
    if (impl.status === 'aguardando') {
      alertas.push({
        id:         `${impl.id}:aguardando`,
        mensagem:   `${nome} está aguardando retorno na etapa ${etapa}.`,
        severidade: 'atencao',
      });
    }

    // Regra 3 — último movimento antigo
    if (impl.ultimoMovimento) {
      const dias = diasDesde(impl.ultimoMovimento);

      if (dias >= 7) {
        alertas.push({
          id:         `${impl.id}:stale`,
          mensagem:   `${nome} sem movimentação há ${dias} dias (${etapa}).`,
          severidade: 'critico',
        });
      } else if (dias >= 3) {
        alertas.push({
          id:         `${impl.id}:stale`,
          mensagem:   `${nome} sem movimentação há ${dias} dias (${etapa}).`,
          severidade: 'atencao',
        });
      }
    }
  }

  // Críticos primeiro, depois atenção
  return alertas.sort((a, b) =>
    a.severidade === b.severidade ? 0 : a.severidade === 'critico' ? -1 : 1,
  );
}

// ─── Componente ───────────────────────────────────────────────────────────────

interface Props {
  implantacoes: ImplantacaoCliente[];
}

export function ImplantacaoAttentionPanel({ implantacoes }: Props) {
  const alertas = gerarAlertas(implantacoes);
  const criticos = alertas.filter((a) => a.severidade === 'critico').length;

  return (
    <div className="anim-fade-in-up rounded-lg border bg-white" style={{ animationDelay: '80ms' }}>
      {/* Cabeçalho */}
      <div className="flex items-center gap-2 border-b px-4 py-2.5">
        <AlertTriangle className="h-4 w-4 text-orange-500 flex-shrink-0" />
        <span className="text-sm font-semibold text-gray-800">Pontos de Atenção</span>

        <div className="ml-auto flex items-center gap-1.5">
          {criticos > 0 && (
            <span className="rounded-full bg-red-500/10 px-2 py-0.5 text-xs font-medium text-red-600">
              {criticos} crítico{criticos > 1 ? 's' : ''}
            </span>
          )}
          {alertas.length > 0 && (
            <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500">
              {alertas.length} alerta{alertas.length > 1 ? 's' : ''}
            </span>
          )}
        </div>
      </div>

      {/* Corpo */}
      <div className="max-h-44 overflow-y-auto divide-y">
        {alertas.length === 0 ? (
          <div className="flex items-center gap-2 px-4 py-3 text-sm text-green-700">
            <CheckCircle2 className="h-4 w-4 flex-shrink-0" />
            <span>Nenhum ponto crítico no momento.</span>
          </div>
        ) : (
          alertas.map((a) => {
            const isCritico = a.severidade === 'critico';
            return (
              <div
                key={a.id}
                className={cn(
                  'flex items-start gap-2.5 px-4 py-2.5 text-sm',
                  isCritico
                    ? 'bg-red-50/70 text-red-800'
                    : 'bg-yellow-50/70 text-yellow-800',
                )}
              >
                {isCritico ? (
                  <AlertTriangle className="h-3.5 w-3.5 flex-shrink-0 mt-0.5 text-red-500" />
                ) : (
                  <Clock className="h-3.5 w-3.5 flex-shrink-0 mt-0.5 text-yellow-500" />
                )}
                <span>{a.mensagem}</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
