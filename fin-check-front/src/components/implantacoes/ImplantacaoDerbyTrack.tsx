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

function trackPos(etapa: string, pct: number): number {
  const p = pct / 100;
  if (etapa === 'pre')        return 3  + p * 24;
  if (etapa === 'corrida')    return 30 + p * 32;
  if (etapa === 'onboarding') return 64 + p * 27;
  return 93;
}

const PRAZO_TOTAL = 30;

function daysRemaining(etapaIniciadaEm: string | null): number | null {
  if (!etapaIniciadaEm) return null;
  const elapsed = Math.floor((Date.now() - new Date(etapaIniciadaEm).getTime()) / 86_400_000);
  return PRAZO_TOTAL - elapsed;
}

function timerCls(rem: number | null): string {
  if (rem === null) return '';
  if (rem < 0)  return 'late';
  if (rem <= 3) return 'crit';
  if (rem <= 7) return 'warn';
  return 'ok';
}

function timerLabel(rem: number | null): string {
  if (rem === null) return '';
  return rem < 0 ? `⏱ +${Math.abs(rem)}d` : `⏱ ${rem}d`;
}

const TIMER_BG: Record<string, string> = {
  ok: '#00A19B', warn: '#E8A100', crit: '#D9534F', late: '#B0201C',
};

const STATUS_STYLE = {
  fluindo:    { color: '#00A19B', badge: 'EM PISTA' },
  aguardando: { color: '#E8A100', badge: 'ATENÇÃO'  },
  travado:    { color: '#D9534F', badge: 'TRAVADO'  },
} as const;

const STAGE_META = {
  pre:        { label: 'Pré-Largada', emoji: '🚦', accent: '#E8A100' },
  corrida:    { label: 'Corrida',      emoji: '🏇', accent: '#2E6FD6' },
  onboarding: { label: 'Onboarding',   emoji: '📋', accent: '#9C27B0' },
  curral:     { label: 'Curral',       emoji: '🏁', accent: '#FFFFFF' },
} as const;

// ─── Component ───────────────────────────────────────────────────────────────

interface Props {
  implantacoes: ImplantacaoCliente[];
}

export function ImplantacaoDerbyTrack({ implantacoes }: Props) {
  const stageKeys = ['pre', 'corrida', 'onboarding', 'curral'] as const;

  const grouped = Object.fromEntries(
    stageKeys.map((k) => [k, implantacoes.filter((i) => i.etapa === k)]),
  ) as Record<(typeof stageKeys)[number], ImplantacaoCliente[]>;

  const trackImplantacoes = implantacoes.filter((i) => i.etapa !== 'curral');
  const curralImplantacoes = grouped.curral;

  return (
    <>
      {/* ── CSS Animations ───────────────────────────────────────────────── */}
      <style>{`
        @keyframes derby-drift {
          from { background-position: 0 0; }
          to   { background-position: 60px 0; }
        }
        @keyframes derby-gallop {
          0%,100% { transform: translateY(0);   }
          50%      { transform: translateY(-3px); }
        }
        @keyframes derby-puff {
          0%,100% { transform: translateY(0) scale(1);    opacity: .7; }
          50%     { transform: translateY(-5px) scale(1.18); opacity: .3; }
        }
        .derby-clouds {
          background:
            radial-gradient(36px 22px at 12% 22%, #fff 70%, transparent 72%),
            radial-gradient(28px 18px at 17% 26%, #fff 70%, transparent 72%),
            radial-gradient(30px 20px at  8% 28%, #fff 70%, transparent 72%),
            radial-gradient(44px 26px at 46% 14%, #fff 70%, transparent 72%),
            radial-gradient(32px 20px at 53% 18%, #fff 70%, transparent 72%),
            radial-gradient(40px 24px at 78% 24%, #fff 70%, transparent 72%),
            radial-gradient(28px 18px at 84% 27%, #fff 70%, transparent 72%);
          background-repeat: no-repeat;
          opacity: .95;
          animation: derby-drift 60s linear infinite;
        }
        .derby-horse { transition: left 1s cubic-bezier(.22,1,.36,1); }
        .derby-gallop { animation: derby-gallop .6s ease-in-out infinite; }
        .derby-dust span {
          display: block; border-radius: 50%;
          background: rgba(160,210,120,.90);
          animation: derby-puff 1.1s ease-in-out infinite;
        }
        .derby-dust span:nth-child(1) { width:13px; height:13px; animation-delay:0s;   }
        .derby-dust span:nth-child(2) { width: 9px; height: 9px; animation-delay:.18s; opacity:.8; }
        .derby-dust span:nth-child(3) { width: 6px; height: 6px; animation-delay:.36s; opacity:.6; }
      `}</style>

      {/* ── Outer wrapper ────────────────────────────────────────────────── */}
      <div style={{
        border: '1px solid #E6E9EC',
        borderRadius: 22,
        boxShadow: '0 12px 40px rgba(16,24,40,.16)',
        overflow: 'hidden',
        background: '#fff',
      }}>

        {/* ── Stage header (dark bar) ─────────────────────────────────── */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1.05fr 1.7fr 1fr 96px',
          background: 'linear-gradient(180deg,#2B2F33 0%,#363B41 100%)',
          color: '#fff',
          padding: '14px 14px 16px',
          gap: 12,
          alignItems: 'stretch',
          position: 'relative',
          zIndex: 6,
        }}>
          {(['pre', 'corrida', 'onboarding'] as const).map((key) => {
            const meta  = STAGE_META[key];
            const count = grouped[key]?.length ?? 0;
            return (
              <div key={key} style={{
                position: 'relative',
                padding: '14px 16px 13px',
                borderRadius: 12,
                background: 'linear-gradient(180deg,rgba(255,255,255,.10),rgba(255,255,255,.04))',
                boxShadow: 'inset 0 1px 0 rgba(255,255,255,.12), 0 6px 14px rgba(0,0,0,.22)',
                borderTop: `3px solid ${meta.accent}`,
              }}>
                <div style={{ fontSize: 13, fontWeight: 800, letterSpacing: '.3px', display: 'flex', alignItems: 'center', gap: 8 }}>
                  {meta.emoji} {meta.label}
                </div>
                <div style={{ fontSize: 10, color: '#AEB6BD', fontWeight: 700, marginTop: 4, letterSpacing: '.4px', textTransform: 'uppercase' }}>
                  Implantações
                </div>
                {count > 0 && (
                  <div style={{
                    position: 'absolute', top: 12, right: 14,
                    fontSize: 12, fontWeight: 800, color: '#fff',
                    background: meta.accent,
                    minWidth: 24, height: 24,
                    display: 'grid', placeItems: 'center',
                    padding: '0 7px', borderRadius: 999,
                    boxShadow: '0 3px 8px rgba(0,0,0,.3)',
                  }}>{count}</div>
                )}
              </div>
            );
          })}

          {/* Finish col */}
          <div style={{
            borderRadius: 12, display: 'grid', placeItems: 'center',
            borderTop: '3px solid #fff',
            background: 'repeating-linear-gradient(45deg,#1c2024 0 8px,#fff 8px 16px)',
            position: 'relative',
            boxShadow: '0 6px 14px rgba(0,0,0,.25)',
            overflow: 'hidden',
          }}>
            <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,161,155,.55)', mixBlendMode: 'multiply' }} />
            <div style={{ position: 'relative', zIndex: 2, textAlign: 'center', lineHeight: 1.2 }}>
              <div style={{ fontSize: 22 }}>🏁</div>
              <div style={{ fontSize: 8, fontWeight: 900, letterSpacing: '.5px', color: '#fff', textShadow: '0 1px 3px rgba(0,0,0,.6)', marginTop: 2 }}>
                CURRAL
              </div>
              <div style={{ fontSize: 14, fontWeight: 800, color: '#fff' }}>
                {curralImplantacoes.length}
              </div>
            </div>
          </div>
        </div>

        {/* ── Sky scene ──────────────────────────────────────────────────── */}
        <div style={{
          position: 'relative',
          height: 110,
          background: 'linear-gradient(180deg,#7FC4E8 0%,#B6E0F2 55%,#E4F4FB 100%)',
          overflow: 'hidden',
          flexShrink: 0,
        }}>
          {/* Sun */}
          <div style={{
            position: 'absolute', top: 14, right: 120,
            width: 50, height: 50, borderRadius: '50%',
            background: 'radial-gradient(circle,#FFF3C4,#FFE07A)',
            boxShadow: '0 0 36px rgba(255,224,122,.7)',
            zIndex: 0,
          }} />

          {/* Clouds */}
          <div className="derby-clouds" style={{ position: 'absolute', inset: 0 }} />

          {/* Hills */}
          <div style={{
            position: 'absolute', left: 0, right: 0, bottom: 36, height: 50,
            background: `
              radial-gradient(140px 70px at 20% 100%, #69AD57 70%, transparent 72%),
              radial-gradient(180px 80px at 60% 100%, #5E9E54 70%, transparent 72%),
              radial-gradient(150px 70px at 92% 100%, #69AD57 70%, transparent 72%)
            `,
            backgroundRepeat: 'no-repeat',
            zIndex: 1,
          }} />

          {/* Trees */}
          <div style={{
            position: 'absolute', left: 0, right: 0, bottom: 22, height: 40,
            background: `
              radial-gradient(26px 26px at  5% 100%,#5E9E54 62%,transparent 64%),
              radial-gradient(34px 34px at 14% 100%,#74B85E 62%,transparent 64%),
              radial-gradient(28px 28px at 24% 100%,#5E9E54 62%,transparent 64%),
              radial-gradient(38px 38px at 35% 100%,#74B85E 62%,transparent 64%),
              radial-gradient(26px 26px at 47% 100%,#5E9E54 62%,transparent 64%),
              radial-gradient(36px 36px at 58% 100%,#74B85E 62%,transparent 64%),
              radial-gradient(30px 30px at 70% 100%,#5E9E54 62%,transparent 64%),
              radial-gradient(40px 40px at 82% 100%,#74B85E 62%,transparent 64%),
              radial-gradient(28px 28px at 95% 100%,#5E9E54 62%,transparent 64%)
            `,
            backgroundRepeat: 'no-repeat',
            zIndex: 2,
          }} />

          {/* Fence */}
          <div style={{ position: 'absolute', left: 0, right: 0, bottom: 10, height: 22, zIndex: 3 }}>
            <div style={{
              position: 'absolute', left: 0, right: 0, top: 3, height: 3,
              background: '#fff',
              boxShadow: '0 8px 0 #fff, 0 1px 4px rgba(0,0,0,.12)',
            }} />
            <div style={{
              position: 'absolute', inset: 0,
              background: 'repeating-linear-gradient(90deg,#fff 0 4px,transparent 4px 38px)',
            }} />
          </div>

          {/* Turf gradient transition */}
          <div style={{
            position: 'absolute', left: 0, right: 0, bottom: 0, height: 16,
            background: 'linear-gradient(180deg,#74B85E,#4FA63F)',
            zIndex: 4,
          }} />
        </div>

        {/* ── Track lanes ────────────────────────────────────────────────── */}
        <div style={{
          position: 'relative',
          background: 'linear-gradient(180deg,#72C35A 0%,#61B84E 50%,#4FA63F 100%)',
          borderTop: '3px solid #3A8F2E',
          minHeight: trackImplantacoes.length === 0 ? 100 : undefined,
          overflowX: 'auto',
        }}>
          {/* Stage separator — Pré-Largada / Corrida */}
          <div style={{
            position: 'absolute', top: 0, bottom: 0, left: '29%',
            borderLeft: '2px dashed rgba(255,255,255,.92)',
            zIndex: 4, pointerEvents: 'none',
          }}>
            <span style={{
              position: 'absolute', top: 6, left: 6,
              fontSize: 8, fontWeight: 900, color: '#fff',
              background: 'rgba(28,32,36,.50)',
              padding: '2px 5px', borderRadius: 999,
              letterSpacing: '.4px', whiteSpace: 'nowrap',
            }}>LARGADA</span>
          </div>

          {/* Onboarding separator at 64% */}
          <div style={{
            position: 'absolute', top: 0, bottom: 0, left: '64%',
            borderLeft: '2px dashed rgba(255,255,255,.55)',
            zIndex: 4, pointerEvents: 'none',
          }}>
            <span style={{
              position: 'absolute', top: 6, left: 6,
              fontSize: 8, fontWeight: 900, color: '#fff',
              background: 'rgba(28,32,36,.50)',
              padding: '2px 5px', borderRadius: 999,
              letterSpacing: '.4px', whiteSpace: 'nowrap',
            }}>ONBOARDING</span>
          </div>

          {/* Finish line */}
          <div style={{
            position: 'absolute', top: 0, bottom: 0, right: 96, width: 18,
            background: 'repeating-linear-gradient(0deg,#1c2024 0 10px,#fff 10px 20px)',
            boxShadow: '-3px 0 0 #00A19B, 3px 0 0 #00A19B, 0 0 20px rgba(0,161,155,.40)',
            opacity: .96, zIndex: 5, pointerEvents: 'none',
          }}>
            <span style={{ position: 'absolute', top: -2, left: '50%', transform: 'translateX(-50%)', fontSize: 14 }}>🏁</span>
          </div>

          {/* Empty state */}
          {trackImplantacoes.length === 0 && (
            <div style={{
              textAlign: 'center', padding: '36px 0',
              color: 'rgba(255,255,255,.75)', fontSize: 13, fontWeight: 600,
            }}>
              Nenhuma implantação em andamento
            </div>
          )}

          {/* One lane per implantação */}
          {trackImplantacoes.map((impl, idx) => {
            const pct  = computeProgress(impl.progressJson);
            const pos  = trackPos(impl.etapa, pct);
            const st   = STATUS_STYLE[(impl.status ?? 'fluindo') as keyof typeof STATUS_STYLE] ?? STATUS_STYLE.fluindo;
            const rem  = daysRemaining(impl.etapaIniciadaEm);
            const tCls = timerCls(rem);
            const isTravado = impl.status === 'travado';
            const stageMeta = STAGE_META[impl.etapa as keyof typeof STAGE_META];

            return (
              <Link
                key={impl.id}
                href={`/implantacoes/${impl.id}`}
                style={{
                  display: 'block',
                  position: 'relative',
                  height: 82,
                  textDecoration: 'none',
                  background: idx % 2 === 0 ? 'rgba(0,0,0,.04)' : 'rgba(255,255,255,.06)',
                }}
                title={`${impl.clienteRazaoSocial}${impl.responsavel ? ` — ${impl.responsavel}` : ''}`}
              >
                {/* Lane bottom line */}
                <div style={{
                  position: 'absolute', left: 0, right: 0, bottom: 0, height: 2,
                  background: 'rgba(255,255,255,.85)',
                  pointerEvents: 'none',
                }} />

                {/* Horse group */}
                <div
                  className="derby-horse"
                  style={{
                    position: 'absolute',
                    top: '50%',
                    left: `${pos}%`,
                    transform: 'translate(-50%, -50%)',
                    zIndex: 2,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    width: 130,
                    cursor: 'pointer',
                  }}
                >
                  {/* ── Name card ──────────────────────────────────────── */}
                  <div style={{
                    position: 'relative',
                    background: 'rgba(255,255,255,.96)',
                    backdropFilter: 'blur(2px)',
                    border: '1px solid rgba(255,255,255,.8)',
                    borderTop: `2px solid ${st.color}`,
                    borderRadius: 8,
                    boxShadow: '0 3px 8px rgba(0,0,0,.18)',
                    padding: '3px 7px 4px',
                    marginBottom: -1,
                    zIndex: 4,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    whiteSpace: 'nowrap',
                  }}>
                    {/* Status badge */}
                    <div style={{
                      position: 'absolute', top: -9, right: -6,
                      fontSize: 9, fontWeight: 800,
                      padding: '2px 7px', borderRadius: 999,
                      color: '#fff',
                      background: st.color,
                      boxShadow: '0 3px 8px rgba(0,0,0,.22)',
                      zIndex: 5, letterSpacing: '.3px',
                    }}>{st.badge}</div>

                    {/* Caret */}
                    <div style={{
                      position: 'absolute', left: '50%', bottom: -6,
                      transform: 'translateX(-50%)',
                      width: 0, height: 0,
                      borderLeft: '6px solid transparent',
                      borderRight: '6px solid transparent',
                      borderTop: '6px solid rgba(255,255,255,.96)',
                    }} />

                    {/* Client name */}
                    <div style={{
                      fontSize: 10, fontWeight: 800, letterSpacing: '.1px',
                      lineHeight: 1.1, color: '#1C2024',
                      maxWidth: 108, overflow: 'hidden', textOverflow: 'ellipsis',
                    }}>
                      {impl.clienteRazaoSocial}
                    </div>

                    {/* Stage + progress + responsável */}
                    <div style={{
                      fontSize: 8.5, color: '#6B7178', fontWeight: 700,
                      marginTop: 1, display: 'flex', alignItems: 'center',
                      gap: 3, justifyContent: 'center',
                    }}>
                      <span>{stageMeta?.emoji} {pct}%</span>
                      {impl.responsavel && (
                        <>
                          <span>·</span>
                          <span style={{ color: '#007F7A' }}>👤 {impl.responsavel}</span>
                        </>
                      )}
                    </div>

                    {/* Mini progress bar */}
                    <div style={{
                      height: 4, width: '100%', minWidth: 108,
                      borderRadius: 3, background: 'rgba(0,0,0,.1)',
                      marginTop: 4, overflow: 'hidden',
                    }}>
                      <div style={{
                        height: '100%', background: st.color,
                        borderRadius: 3, width: `${pct}%`,
                        boxShadow: `0 0 6px ${st.color}`,
                        transition: 'width .8s ease',
                      }} />
                    </div>
                  </div>

                  {/* ── Horse body ─────────────────────────────────────── */}
                  <div
                    className={isTravado ? undefined : 'derby-gallop'}
                    style={{ position: 'relative', display: 'inline-flex', alignItems: 'flex-end' }}
                  >
                    {/* Dust particles behind horse */}
                    <div className="derby-dust" style={{
                      position: 'absolute', bottom: 6, right: '88%',
                      display: 'flex', alignItems: 'flex-end',
                      gap: 4, zIndex: 0, opacity: .75,
                    }}>
                      <span /><span /><span />
                    </div>

                    {/* Elliptic shadow */}
                    <div style={{
                      position: 'absolute', bottom: 2, left: '52%',
                      transform: 'translateX(-50%)',
                      width: 70, height: 9, borderRadius: '50%',
                      background: 'rgba(0,60,0,.22)', filter: 'blur(3px)', zIndex: 1,
                    }} />

                    {/* Horse emoji */}
                    <span style={{
                      fontSize: 44, lineHeight: 1,
                      display: 'block', position: 'relative', zIndex: 2,
                      userSelect: 'none',
                      filter: isTravado
                        ? 'drop-shadow(0 4px 4px rgba(217,83,79,.55))'
                        : 'drop-shadow(0 4px 3px rgba(20,10,0,.35))',
                    }}>
                      🏇
                    </span>
                  </div>

                  {/* ── Timer tag ──────────────────────────────────────── */}
                  {rem !== null && (
                    <div style={{
                      marginTop: 3, fontSize: 8.5, fontWeight: 800, letterSpacing: '.2px',
                      padding: '2px 7px', borderRadius: 999, color: '#fff',
                      display: 'inline-flex', alignItems: 'center', gap: 2,
                      boxShadow: '0 1px 4px rgba(0,0,0,.22)',
                      background: TIMER_BG[tCls] ?? '#6B7178',
                    }}>
                      {timerLabel(rem)}
                    </div>
                  )}
                </div>
              </Link>
            );
          })}
        </div>

        {/* ── Curral (finished) section ───────────────────────────────────── */}
        {curralImplantacoes.length > 0 && (
          <div style={{
            borderTop: '1px solid #E6E9EC',
            padding: '10px 16px 12px',
            background: '#F4F6F7',
          }}>
            <div style={{
              fontSize: 11, fontWeight: 700, color: '#6B7178',
              marginBottom: 8, letterSpacing: '.3px', textTransform: 'uppercase',
            }}>
              🏆 Curral — Concluídas ({curralImplantacoes.length})
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {curralImplantacoes.map((impl) => (
                <Link
                  key={impl.id}
                  href={`/implantacoes/${impl.id}`}
                  style={{
                    display: 'inline-flex', alignItems: 'center', gap: 5,
                    padding: '4px 10px', borderRadius: 8,
                    background: '#E4F4EC', border: '1px solid #B7E2CC',
                    fontSize: 12, fontWeight: 600, color: '#207A4F',
                    textDecoration: 'none',
                  }}
                >
                  🏁 {impl.clienteRazaoSocial}
                </Link>
              ))}
            </div>
          </div>
        )}
      </div>
    </>
  );
}
