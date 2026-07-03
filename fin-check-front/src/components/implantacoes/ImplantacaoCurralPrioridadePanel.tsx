'use client';

import { useMemo } from 'react';
import { Target } from 'lucide-react';
import { useRocksConcluidos } from '@/lib/hooks/useImplantacoes';
import { ImplantacaoCliente } from '@/lib/types/entities';

// ── helpers ───────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  const [y, m, d] = iso.split('T')[0].split('-');
  return `${d}/${m}/${y}`;
}

// ── types ─────────────────────────────────────────────────────────────────────

interface PrioridadeItem {
  implantacaoId: string;
  nome: string;
  demandasAbertas: number;
  // null = nunca teve demanda concluída com concluidaEm — alta prioridade
  ultimaAtuacao: string | null;
  // reservado para futura categorização A/B/C por importância/lucro
  // categoria?: 'A' | 'B' | 'C';
}

// ── sorting ───────────────────────────────────────────────────────────────────

function sortPrioridade(a: PrioridadeItem, b: PrioridadeItem): number {
  // 1. sem histórico sempre primeiro
  if (!a.ultimaAtuacao && b.ultimaAtuacao) return -1;
  if (a.ultimaAtuacao && !b.ultimaAtuacao) return 1;

  // 2. maior tempo sem atuação — ISO string comparison: menor string = data mais antiga
  if (a.ultimaAtuacao && b.ultimaAtuacao) {
    if (a.ultimaAtuacao < b.ultimaAtuacao) return -1;
    if (a.ultimaAtuacao > b.ultimaAtuacao) return 1;
  }

  // 3. mais demandas abertas
  if (b.demandasAbertas !== a.demandasAbertas) return b.demandasAbertas - a.demandasAbertas;

  // 4. nome alfabético
  return a.nome.localeCompare(b.nome, 'pt-BR');
}

// ── component ─────────────────────────────────────────────────────────────────

interface Props {
  implantacoes: ImplantacaoCliente[];
}

const MAX_ITEMS = 8;

export function ImplantacaoCurralPrioridadePanel({ implantacoes }: Props) {
  // Reutiliza o cache do useRocksConcluidos — sem nova requisição HTTP
  const { data: rocks, isLoading } = useRocksConcluidos();

  const items = useMemo<PrioridadeItem[]>(() => {
    if (!rocks) return [];

    // Filtra apenas clientes no curral
    const curralClients = implantacoes.filter((i) => i.etapa === 'curral');

    // Maior concluidaEm por implantacaoId — "última atuação" do cliente
    const lastAtuacao: Record<string, string> = {};
    for (const r of rocks) {
      if (!lastAtuacao[r.implantacaoId] || r.concluidaEm > lastAtuacao[r.implantacaoId]) {
        lastAtuacao[r.implantacaoId] = r.concluidaEm;
      }
    }

    const raw: PrioridadeItem[] = curralClients.map((c) => ({
      implantacaoId: c.id,
      nome: c.clienteNomeFantasia ?? c.clienteRazaoSocial,
      demandasAbertas: c.demandasAbertasCount,
      ultimaAtuacao: lastAtuacao[c.id] ?? null,
    }));

    return raw.sort(sortPrioridade).slice(0, MAX_ITEMS);
  }, [implantacoes, rocks]);

  if (isLoading) return null;

  return (
    <div
      style={{
        background: '#fff',
        border: '1px solid #E6E9EC',
        borderRadius: 16,
        padding: '20px 24px 16px',
        boxShadow: '0 1px 4px rgba(0,0,0,.06)',
      }}
    >
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
        <Target
          style={{ width: 18, height: 18, color: '#D9534F', flexShrink: 0 }}
          strokeWidth={2.2}
        />
        <span style={{ fontSize: 13, fontWeight: 800, color: '#1C2024', letterSpacing: '.3px' }}>
          Clientes para Atuar
        </span>
        {items.length > 0 && (
          <span style={{ marginLeft: 'auto', fontSize: 11, fontWeight: 700, color: '#6B7178' }}>
            top {items.length}
          </span>
        )}
      </div>

      <p style={{ fontSize: 11, color: '#9BA3AE', marginBottom: 12, marginLeft: 28 }}>
        Ordenado por tempo sem atuação e demandas abertas
      </p>

      {/* Empty */}
      {items.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '32px 20px', color: '#9BA3AE' }}>
          <p style={{ fontSize: 13, fontWeight: 600 }}>Nenhum cliente no Curral ainda.</p>
        </div>
      ) : (
        <div>
          {items.map((item, idx) => (
            <div
              key={item.implantacaoId}
              style={{
                display: 'flex', alignItems: 'flex-start', gap: 12,
                padding: '10px 0',
                borderBottom: idx < items.length - 1 ? '1px dashed #F0F2F4' : 'none',
              }}
            >
              {/* Badge de posição — vermelho no 1º, âmbar no 2º, neutro nos demais */}
              <span
                style={{
                  flexShrink: 0,
                  width: 22, height: 22, borderRadius: '50%',
                  background: idx === 0 ? '#D9534F' : idx === 1 ? '#E8A100' : '#E6E9EC',
                  color: idx <= 1 ? '#fff' : '#6B7178',
                  fontSize: 10, fontWeight: 900,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  marginTop: 1,
                }}
              >
                {idx + 1}
              </span>

              <div style={{ flex: 1, minWidth: 0 }}>
                <p style={{ fontSize: 13, fontWeight: 700, color: '#1C2024', marginBottom: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {item.nome}
                </p>
                <p style={{ fontSize: 11, color: '#9BA3AE', marginBottom: 4 }}>
                  {item.ultimaAtuacao
                    ? `Última atuação: ${formatDate(item.ultimaAtuacao)}`
                    : 'Sem histórico de atuação'}
                </p>
                {item.demandasAbertas > 0 ? (
                  <span
                    style={{
                      display: 'inline-block',
                      fontSize: 10, fontWeight: 700,
                      background: '#FFF3E0', color: '#C07A00',
                      padding: '2px 8px', borderRadius: 999,
                    }}
                  >
                    {item.demandasAbertas}{' '}
                    {item.demandasAbertas === 1 ? 'demanda aberta' : 'demandas abertas'}
                  </span>
                ) : (
                  <span style={{ fontSize: 10, color: '#9BA3AE', fontStyle: 'italic' }}>
                    Cliente sem demandas — mande uma mensagem e seja prestativo.
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
