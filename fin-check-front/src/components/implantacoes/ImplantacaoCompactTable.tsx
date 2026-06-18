'use client';

import Link from 'next/link';
import { ImplantacaoCliente } from '@/lib/types/entities';

// ─── Helpers ─────────────────────────────────────────────────────────────────

function computeProgress(progressJson: unknown): number {
  if (!Array.isArray(progressJson) || progressJson.length === 0) return 0;
  const items = progressJson as { concluido: boolean }[];
  const done = items.filter((i) => i.concluido).length;
  return Math.round((done / items.length) * 100);
}

const PRAZO_TOTAL = 30;

function daysRemaining(etapaIniciadaEm: string | null): number | null {
  if (!etapaIniciadaEm) return null;
  const elapsed = Math.floor((Date.now() - new Date(etapaIniciadaEm).getTime()) / 86_400_000);
  return PRAZO_TOTAL - elapsed;
}

function timerCls(rem: number | null): string {
  if (rem === null) return 'none';
  if (rem < 0)  return 'late';
  if (rem <= 3) return 'crit';
  if (rem <= 7) return 'warn';
  return 'ok';
}

function timerLabel(rem: number | null): string {
  if (rem === null) return '—';
  return rem < 0 ? `⏱ +${Math.abs(rem)}d` : `⏱ ${rem}d`;
}

// ─── Config ───────────────────────────────────────────────────────────────────

const ETAPA_BADGE: Record<string, { label: string; bg: string; color: string }> = {
  pre:        { label: 'Pré-Largada', bg: '#FCF3DC', color: '#8A6300' },
  corrida:    { label: 'Corrida',     bg: '#E3EEFF', color: '#1A4FA0' },
  onboarding: { label: 'Onboarding', bg: '#F3E8FF', color: '#6A1B9A' },
};

const STATUS_BADGE: Record<string, { label: string; bg: string; color: string }> = {
  fluindo:    { label: 'Fluindo',    bg: '#E3F5F4', color: '#007F7A' },
  aguardando: { label: 'Aguardando', bg: '#FCF3DC', color: '#8A6300' },
  travado:    { label: 'Travado',    bg: '#FBE7E6', color: '#A8322E' },
};

const TIMER_COLOR: Record<string, string> = {
  ok: '#00A19B', warn: '#E8A100', crit: '#D9534F', late: '#B0201C', none: '#6B7178',
};

// ─── Shared th style ─────────────────────────────────────────────────────────

const TH: React.CSSProperties = {
  background: '#2B2F33',
  color: '#fff',
  padding: '9px 14px',
  textAlign: 'left',
  fontSize: 10.5,
  fontWeight: 700,
  letterSpacing: '.4px',
  textTransform: 'uppercase',
  whiteSpace: 'nowrap',
};

// ─── Component ───────────────────────────────────────────────────────────────

interface Props {
  implantacoes: ImplantacaoCliente[];
}

export function ImplantacaoCompactTable({ implantacoes }: Props) {
  const rows = implantacoes.filter((i) => i.etapa !== 'curral');
  if (rows.length === 0) return null;

  return (
    <div style={{ marginTop: 18 }}>
      <style>{`
        .ict-row:hover { background: #E3F5F4 !important; }
      `}</style>

      {/* Section head */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
        <h3 style={{ fontSize: 15, fontWeight: 800, letterSpacing: '.2px', color: '#1C2024' }}>
          Resumo compacto da implantação
        </h3>
        <span style={{
          fontSize: 10, fontWeight: 700, letterSpacing: '.5px', textTransform: 'uppercase',
          color: '#007F7A', background: '#E3F5F4',
          padding: '3px 9px', borderRadius: 999,
        }}>
          Ao vivo
        </span>
      </div>

      {/* Table */}
      <div style={{
        overflowX: 'auto',
        borderRadius: 16,
        border: '1px solid #E6E9EC',
        boxShadow: '0 1px 2px rgba(16,24,40,.05)',
        background: '#fff',
      }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12.5 }}>
          <thead>
            <tr>
              <th style={{ ...TH, borderRadius: '10px 0 0 0' }}>Cliente</th>
              <th style={TH}>Etapa</th>
              <th style={TH}>Percentual</th>
              <th style={TH}>Prazo restante</th>
              <th style={{ ...TH, borderRadius: '0 10px 0 0' }}>Status</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((impl, idx) => {
              const pct         = computeProgress(impl.progressJson);
              const rem         = daysRemaining(impl.etapaIniciadaEm);
              const tCls        = timerCls(rem);
              const etapaBadge  = ETAPA_BADGE[impl.etapa];
              const statusBadge = impl.status ? STATUS_BADGE[impl.status] : null;
              const isLast      = idx === rows.length - 1;

              return (
                <tr
                  key={impl.id}
                  className="ict-row"
                  style={{ borderBottom: isLast ? 'none' : '1px solid #E6E9EC', cursor: 'pointer' }}
                >
                  <td style={{ padding: '8px 14px' }}>
                    <Link
                      href={`/implantacoes/${impl.id}`}
                      style={{ fontWeight: 700, color: '#1C2024', textDecoration: 'none' }}
                    >
                      {impl.clienteRazaoSocial}
                    </Link>
                    {impl.clienteNomeFantasia && (
                      <div style={{ fontSize: 11, color: '#6B7178', marginTop: 2 }}>
                        {impl.clienteNomeFantasia}
                      </div>
                    )}
                  </td>

                  <td style={{ padding: '8px 14px', whiteSpace: 'nowrap' }}>
                    {etapaBadge ? (
                      <span style={{
                        fontSize: 10, fontWeight: 700,
                        padding: '3px 9px', borderRadius: 999,
                        background: etapaBadge.bg, color: etapaBadge.color,
                        letterSpacing: '.2px',
                      }}>
                        {etapaBadge.label}
                      </span>
                    ) : (
                      <span style={{ color: '#6B7178' }}>—</span>
                    )}
                  </td>

                  <td style={{ padding: '8px 14px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 7, minWidth: 110 }}>
                      <div style={{ flex: 1, height: 5, borderRadius: 3, background: '#E6E9EC', overflow: 'hidden' }}>
                        <div style={{
                          height: '100%', borderRadius: 3,
                          background: '#00A19B', boxShadow: '0 0 4px #00A19B',
                          width: `${pct}%`, transition: 'width .6s ease',
                        }} />
                      </div>
                      <span style={{ fontSize: 11, fontWeight: 800, color: '#1C2024', minWidth: 32, textAlign: 'right' }}>
                        {pct}%
                      </span>
                    </div>
                  </td>

                  <td style={{ padding: '8px 14px', whiteSpace: 'nowrap' }}>
                    <span style={{
                      fontSize: 10, fontWeight: 800,
                      padding: '2px 8px', borderRadius: 999,
                      color: '#fff',
                      background: TIMER_COLOR[tCls] ?? '#6B7178',
                    }}>
                      {timerLabel(rem)}
                    </span>
                  </td>

                  <td style={{ padding: '8px 14px', whiteSpace: 'nowrap' }}>
                    {statusBadge ? (
                      <span style={{
                        fontSize: 10, fontWeight: 700,
                        padding: '3px 9px', borderRadius: 999,
                        background: statusBadge.bg, color: statusBadge.color,
                      }}>
                        {statusBadge.label}
                      </span>
                    ) : (
                      <span style={{ color: '#6B7178' }}>—</span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
