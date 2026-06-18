'use client';

import { useState } from 'react';
import type { CSSProperties } from 'react';
import Link from 'next/link';
import { ImplantacaoCliente } from '@/lib/types/entities';

// ─── Mood ─────────────────────────────────────────────────────────────────────

type MoodResult = { key: string; emoji: string; label: string; color: string; op: string };

function computeMood(count: number, maiorPrioridade: string | null): MoodResult {
  if (count === 0) {
    return { key: 'campeao',    emoji: '🏆', label: 'Campeão', color: '#207A4F', op: 'Cliente saudável' };
  }
  // critico: prioridade alta/critica OU 3+ demandas abertas independente de prioridade
  if (count >= 3 || maiorPrioridade === 'alta' || maiorPrioridade === 'critica') {
    return { key: 'critico',    emoji: '🔥', label: 'Crítico', color: '#D9534F', op: 'Intervenção urgente' };
  }
  // atenção: 1 ou 2 demandas abertas de baixa/media
  return   { key: 'preocupado', emoji: '😟', label: 'Atenção', color: '#E8A100', op: 'Atenção operacional' };
}

// ─── Coat helpers ─────────────────────────────────────────────────────────────

const COAT_PALETTE = [
  { coat: '#8B5E3C', light: '#C48A5E' },
  { coat: '#4A3728', light: '#7D5A44' },
  { coat: '#C8A97E', light: '#E8D4B4' },
  { coat: '#1C1008', light: '#3D2510' },
  { coat: '#6B4226', light: '#A06B40' },
  { coat: '#D4A85C', light: '#F0CC88' },
];

function computeCoat(id: string) {
  const sum = id.split('').reduce((a, c) => a + c.charCodeAt(0), 0);
  return COAT_PALETTE[sum % COAT_PALETTE.length];
}

// ─── StandingHorse SVG ────────────────────────────────────────────────────────

function StandingHorse({ id, moodKey }: { id: string; moodKey: string }) {
  const { coat, light } = computeCoat(id);
  const gradId = `hg${id.replace(/[^a-zA-Z0-9]/g, '')}`;

  const MOUTH: Record<string, string> = {
    campeao:    'M40 44 q7 6 14 0',
    tranquilo:  'M41 44 q6 3 12 0',
    preocupado: 'M41 45 q6 -4 12 0',
    irritado:   'M40 45 q7 -6 14 0',
    critico:    'M38 46 q9 -9 16 0',
  };
  const BROW1: Record<string, string> = {
    campeao:    'M38 31 q5 -3 8 0',
    tranquilo:  'M38 32 q5 -1 8 0',
    preocupado: 'M38 31 q5 2 8 -1',
    irritado:   'M38 29 q5 5 8 -2',
    critico:    'M38 27 q5 7 8 -3',
  };
  const BROW2: Record<string, string> = {
    campeao:    'M50 31 q5 -3 8 0',
    tranquilo:  'M50 32 q5 -1 8 0',
    preocupado: 'M50 32 q5 1 8 -2',
    irritado:   'M50 30 q5 4 8 -2',
    critico:    'M50 28 q5 6 8 -3',
  };

  const mk = moodKey in MOUTH ? moodKey : 'tranquilo';

  return (
    <svg viewBox="0 0 170 180" width="84" height="84" aria-hidden="true">
      <defs>
        <linearGradient id={gradId} x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor={light} />
          <stop offset="100%" stopColor={coat} />
        </linearGradient>
      </defs>
      {/* Back legs */}
      <rect x="116" y="118" width="13" height="50" rx="5" fill={coat} />
      <rect x="132" y="118" width="13" height="50" rx="5" fill={coat} />
      {/* Back hooves */}
      <ellipse cx="122" cy="168" rx="8" ry="5" fill="#1C0A00" />
      <ellipse cx="138" cy="168" rx="8" ry="5" fill="#1C0A00" />
      {/* Tail */}
      <path d="M148 98 q24 4 20 42 q-2 18 -10 28" stroke={coat} strokeWidth="11" fill="none" strokeLinecap="round" />
      <path d="M148 98 q28 6 24 44 q-4 20 -14 30" stroke={light} strokeWidth="4" fill="none" strokeLinecap="round" opacity=".5" />
      {/* Body */}
      <path d="M56 94 q32 -30 94 -16 q22 4 16 42 q-4 22 -68 20 q-38 -2 -42 -46 z" fill={`url(#${gradId})`} />
      {/* Neck */}
      <path d="M62 92 q-18 -32 -8 -56 q4 -12 22 -14 q16 2 20 18 q6 18 -14 52 z" fill={`url(#${gradId})`} />
      {/* Front legs */}
      <rect x="64" y="118" width="13" height="50" rx="5" fill={coat} />
      <rect x="80" y="118" width="13" height="50" rx="5" fill={coat} />
      {/* Front hooves */}
      <ellipse cx="70" cy="168" rx="8" ry="5" fill="#1C0A00" />
      <ellipse cx="86" cy="168" rx="8" ry="5" fill="#1C0A00" />
      {/* Head */}
      <path d="M48 24 q-14 0 -16 16 q-2 16 8 24 q10 8 26 6 q14 -2 18 -14 q6 -18 -6 -28 q-10 -8 -30 -4 z" fill={`url(#${gradId})`} />
      {/* Snout */}
      <path d="M30 52 q-4 -2 -6 6 q-2 8 4 12 q6 4 16 2 q10 -2 10 -10 q0 -10 -8 -12 q-8 -2 -16 2 z" fill={light} />
      <ellipse cx="33" cy="61" rx="3" ry="2" fill={coat} opacity=".5" />
      <ellipse cx="45" cy="63" rx="3" ry="2" fill={coat} opacity=".5" />
      {/* Ears */}
      <path d="M52 16 q0 -12 8 -10 q8 2 6 14 q-4 4 -12 2 z" fill={coat} />
      <path d="M68 12 q4 -12 12 -8 q6 4 2 14 q-4 4 -12 -2 z" fill={coat} />
      {/* Mane */}
      <path d="M56 22 q-8 12 -10 42 q4 -4 8 -2 q4 -22 10 -38 z" fill="#1C0A00" opacity=".65" />
      {/* Eye */}
      <ellipse cx="44" cy="32" rx="4" ry="4.5" fill="#1C0A00" />
      <ellipse cx="43" cy="31" rx="1.5" ry="1.5" fill="#fff" />
      {/* Brows */}
      <path d={BROW1[mk]} stroke="#1C0A00" strokeWidth="2.5" fill="none" strokeLinecap="round" />
      <path d={BROW2[mk]} stroke="#1C0A00" strokeWidth="2.5" fill="none" strokeLinecap="round" />
      {/* Mouth */}
      <path d={MOUTH[mk]} stroke="#1C0A00" strokeWidth="2" fill="none" strokeLinecap="round" />
    </svg>
  );
}

// ─── Curral filter ────────────────────────────────────────────────────────────

type CurralFilter = 'todos' | 'saudaveis' | 'atencao' | 'criticos' | 'abertas' | 'sem-abertas';

interface FilterChip {
  key: CurralFilter;
  label: string;
  activeColor?: string;
}

const FILTER_CHIPS: FilterChip[] = [
  { key: 'todos',       label: 'Todos' },
  { key: 'saudaveis',   label: '🟢 Saudáveis',            activeColor: '#3BA776' },
  { key: 'atencao',     label: '🟡 Atenção',               activeColor: '#E8A100' },
  { key: 'criticos',    label: '🔴 Críticos',              activeColor: '#D9534F' },
  { key: 'abertas',     label: 'Com demandas abertas' },
  { key: 'sem-abertas', label: 'Sem demandas abertas' },
];

function applyMoodFilter(
  impls: ImplantacaoCliente[],
  filter: CurralFilter,
): ImplantacaoCliente[] {
  if (filter === 'todos')       return impls;
  if (filter === 'saudaveis')   return impls.filter((i) => i.demandasAbertasCount === 0);
  if (filter === 'atencao')     return impls.filter((i) =>
    i.demandasAbertasCount > 0 &&
    i.demandasAbertasCount < 3 &&
    (i.maiorPrioridadeAberta === 'baixa' || i.maiorPrioridadeAberta === 'media'),
  );
  if (filter === 'criticos')    return impls.filter((i) =>
    i.demandasAbertasCount >= 3 ||
    i.maiorPrioridadeAberta === 'alta' ||
    i.maiorPrioridadeAberta === 'critica',
  );
  if (filter === 'abertas')     return impls.filter((i) => i.demandasAbertasCount > 0);
  if (filter === 'sem-abertas') return impls.filter((i) => i.demandasAbertasCount === 0);
  return impls;
}

// ─── Component ───────────────────────────────────────────────────────────────

interface Props {
  implantacoes: ImplantacaoCliente[];
}

export function ImplantacaoCurral({ implantacoes }: Props) {
  const [filter, setFilter] = useState<CurralFilter>('todos');

  const curralAll      = implantacoes.filter((i) => i.etapa === 'curral');
  const curralFiltered = applyMoodFilter(curralAll, filter);

  if (curralAll.length === 0) return null;

  return (
    <>
      {/* ── Styles ─────────────────────────────────────────────────────────── */}
      <style>{`
        @keyframes derby-spin {
          from { transform: rotate(0deg); }
          to   { transform: rotate(360deg); }
        }
        @keyframes critBlink {
          0%,100% {
            box-shadow: 0 3px 10px rgba(50,35,10,.14), inset 0 1px 0 rgba(255,255,255,.7);
          }
          50% {
            box-shadow: 0 0 0 4px rgba(217,83,79,.3),
                        0 3px 10px rgba(50,35,10,.14),
                        inset 0 1px 0 rgba(255,255,255,.7);
          }
        }
        @keyframes corral-drift {
          from { background-position: 0 0; }
          to   { background-position: 60px 0; }
        }
        .windmill-blades {
          transform-origin: 35px 46px;
          animation: derby-spin 8s linear infinite;
        }
        .corral-scene {
          position: relative; border-radius: 22px; overflow: hidden;
          border: 1px solid #B4CC8E;
          box-shadow: 0 12px 40px rgba(16,24,40,.16);
          background: linear-gradient(180deg,#7FC4E8 0%,#B6E0F2 26%,#CFF0DA 46%,#BFE29A 60%,#A8DC72 100%);
          padding-top: 96px;
        }
        .corral-farm-clouds {
          position: absolute; left: 0; right: 0; top: 0; height: 64px;
          background:
            radial-gradient(30px 18px at 18% 50%, #fff 70%, transparent 72%),
            radial-gradient(24px 15px at 23% 58%, #fff 70%, transparent 72%),
            radial-gradient(44px 26px at 46% 30%, #fff 70%, transparent 72%),
            radial-gradient(34px 20px at 54% 40%, #fff 70%, transparent 72%),
            radial-gradient(46px 28px at 76% 42%, #fff 70%, transparent 72%),
            radial-gradient(38px 22px at 84% 50%, #fff 70%, transparent 72%);
          background-repeat: no-repeat;
          opacity: .92;
          animation: corral-drift 70s linear infinite;
          pointer-events: none; z-index: 1;
        }
        .corral-farm-hill {
          position: absolute; left: 0; right: 0; top: 48px; height: 80px;
          background:
            radial-gradient(220px 110px at 22% 100%, #69AD57 70%, transparent 72%),
            radial-gradient(260px 120px at 76% 100%, #5E9E54 70%, transparent 72%);
          background-repeat: no-repeat;
          pointer-events: none; z-index: 1;
        }
        .corral-hay {
          position: absolute; top: 88px; left: 0; right: 0; height: 14px;
          background: repeating-linear-gradient(90deg,#D9B25C 0 3px,#C99B3E 3px 6px);
          opacity: .5; z-index: 3; pointer-events: none;
        }
        .corral-fence {
          position: relative; z-index: 4; height: 20px; margin-top: -2px; background: transparent;
        }
        .corral-fence::before {
          content: '';
          position: absolute; left: 0; right: 0; top: 3px; height: 4px;
          background: #fff;
          box-shadow: 0 8px 0 #fff, 0 1px 5px rgba(0,0,0,.12);
        }
        .corral-fence::after {
          content: '';
          position: absolute; inset: 0;
          background: repeating-linear-gradient(90deg,#fff 0 6px,transparent 6px 46px);
          pointer-events: none;
        }
        .corral-pasture {
          position: relative; z-index: 3;
          background:
            radial-gradient(circle at 20% 20%, rgba(255,255,255,.10), transparent 18%),
            radial-gradient(circle at 80% 40%, rgba(255,255,255,.08), transparent 20%),
            radial-gradient(circle at 50% 90%, rgba(60,120,30,.14), transparent 40%),
            linear-gradient(180deg,#BFEF9A 0%,#A8DC72 45%,#8FCB5B 75%,#7EC04E 100%);
          padding: 18px 20px 28px;
          border-left: 6px solid rgba(255,255,255,.55);
          border-right: 6px solid rgba(255,255,255,.55);
        }
        .corral-pasture::before {
          content: '';
          position: absolute; inset: 0; pointer-events: none;
          background:
            repeating-linear-gradient(90deg,rgba(255,255,255,.05) 0 2px,transparent 2px 14px),
            repeating-linear-gradient(0deg,transparent 0 22px,rgba(80,150,40,.05) 22px 24px);
        }
        .corral-horse-grid {
          display: grid;
          grid-template-columns: repeat(8, minmax(0, 1fr));
          gap: 18px 14px;
          max-height: 520px; overflow-y: auto; overflow-x: hidden;
          padding: 18px 4px 8px 2px;
          position: relative; z-index: 1;
        }
        .corral-horse-grid::-webkit-scrollbar { width: 6px }
        .corral-horse-grid::-webkit-scrollbar-track { background: rgba(255,255,255,.25); border-radius: 8px }
        .corral-horse-grid::-webkit-scrollbar-thumb { background: rgba(60,120,30,.35); border-radius: 8px }
        @media (max-width: 1200px) { .corral-horse-grid { grid-template-columns: repeat(6, minmax(0,1fr)); } }
        @media (max-width: 960px)  { .corral-horse-grid { grid-template-columns: repeat(4, minmax(0,1fr)); } }
        @media (max-width: 640px)  { .corral-horse-grid { grid-template-columns: repeat(3, minmax(0,1fr)); } }
        @media (max-width: 420px)  { .corral-horse-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } }
        .corral-horse {
          position: relative; cursor: pointer;
          display: flex; flex-direction: column; align-items: center; text-align: center;
          padding: 10px 6px 12px; border-radius: 14px;
          background: rgba(255,255,255,.46); backdrop-filter: blur(4px);
          border: 2px solid var(--mood-color);
          box-shadow: 0 3px 10px rgba(50,35,10,.14), inset 0 1px 0 rgba(255,255,255,.7);
          text-decoration: none;
          transition: transform .18s, box-shadow .18s, border-color .18s;
          min-width: 0;
        }
        .corral-horse:hover {
          transform: translateY(-5px) scale(1.06);
          box-shadow: 0 10px 22px rgba(50,35,10,.22); z-index: 10;
        }
        .corral-horse.mood-critico { animation: critBlink 1.6s ease-in-out infinite; }
        .corral-horse .ch-mood {
          position: absolute; top: -10px; right: -8px;
          font-size: 15px; background: #fff; border-radius: 50%;
          width: 28px; height: 28px; display: grid; place-items: center;
          box-shadow: 0 2px 6px rgba(0,0,0,.18);
          border: 2px solid var(--mood-color); z-index: 4; line-height: 1;
        }
        .corral-horse .ch-badge {
          position: absolute; top: -10px; left: -6px;
          font-size: 9px; font-weight: 800;
          background: var(--mood-color); color: #fff;
          min-width: 18px; height: 18px; border-radius: 999px;
          display: none; place-items: center; padding: 0 4px;
          box-shadow: 0 2px 5px rgba(0,0,0,.2); z-index: 4;
        }
        .corral-horse.has-open .ch-badge { display: grid; }
        .corral-horse .ch-body {
          width: 86px; height: 86px;
          display: flex; align-items: flex-end; justify-content: center; position: relative;
        }
        .corral-horse .ch-shadow {
          position: absolute; bottom: 4px; left: 50%; transform: translateX(-50%);
          width: 56px; height: 9px; border-radius: 50%;
          background: rgba(70,45,15,.18); filter: blur(2px);
        }
        .corral-horse .ch-label {
          font-size: 11px; font-weight: 800; color: #1C2024;
          margin-top: 6px; line-height: 1.2; word-break: break-word; max-width: 100%;
        }
        .corral-horse .ch-status {
          font-size: 9px; font-weight: 700; color: var(--mood-color);
          margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%;
        }
        .corral-filter-chip {
          border: 1px solid #E6E9EC; background: #fff; color: #6B7178;
          border-radius: 999px; padding: 6px 12px; font-size: 11.5px; font-weight: 800;
          cursor: pointer; font-family: inherit; transition: .15s;
        }
        .corral-filter-chip:hover { border-color: #00A19B; color: #007F7A; background: #E3F5F4; }
        .corral-filter-chip.active { background: #2B2F33; color: #fff; border-color: #2B2F33; }
        .corral-filter-chip.active-green  { background: #3BA776; border-color: #3BA776; color: #fff; }
        .corral-filter-chip.active-amber  { background: #E8A100; border-color: #E8A100; color: #fff; }
        .corral-filter-chip.active-red    { background: #D9534F; border-color: #D9534F; color: #fff; }
      `}</style>

      {/* ── Section head ──────────────────────────────────────────────────── */}
      <div style={{
        display: 'flex', alignItems: 'baseline', gap: 12,
        margin: '34px 0 16px', flexWrap: 'wrap',
      }}>
        <h2 style={{ fontSize: 20, fontWeight: 800, letterSpacing: '.2px', color: '#1C2024' }}>
          Currais Operacionais — Clientes Implantados
        </h2>
        <span style={{
          fontSize: 11, fontWeight: 700, letterSpacing: '.5px', textTransform: 'uppercase',
          color: '#007F7A', background: '#E3F5F4', padding: '4px 10px', borderRadius: 999,
        }}>
          Pós-onboarding
        </span>
        <span style={{
          marginLeft: 'auto', fontSize: 12.5, color: '#6B7178', fontWeight: 500,
        }}>
          Clique em um cavalo para ver as demandas operacionais
        </span>
        <Link
          href="/implantacoes/nova"
          style={{
            display: 'inline-flex', alignItems: 'center', gap: 8,
            padding: '10px 16px', borderRadius: 12,
            background: 'linear-gradient(135deg,#00A19B,#007F7A)',
            color: '#fff', fontWeight: 700, fontSize: 13,
            textDecoration: 'none', letterSpacing: '.2px',
            boxShadow: '0 6px 16px rgba(0,161,155,.35)',
          }}
        >
          🐴 Nova Implantação
        </Link>
      </div>

      {/* ── Filter chips ──────────────────────────────────────────────────── */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 14 }}>
        {FILTER_CHIPS.map((chip) => {
          const isActive = filter === chip.key;
          let activeClass = 'active';
          if (isActive && chip.activeColor === '#3BA776') activeClass = 'active-green';
          else if (isActive && chip.activeColor === '#E8A100') activeClass = 'active-amber';
          else if (isActive && chip.activeColor === '#D9534F') activeClass = 'active-red';

          return (
            <button
              key={chip.key}
              type="button"
              className={`corral-filter-chip${isActive ? ` ${activeClass}` : ''}`}
              onClick={() => setFilter(chip.key)}
            >
              {chip.label}
            </button>
          );
        })}
      </div>

      {/* ── Farm scene ────────────────────────────────────────────────────── */}
      <div className="corral-scene">
        {/* Clouds */}
        <div className="corral-farm-clouds" />
        {/* Hills */}
        <div className="corral-farm-hill" />

        {/* Windmill */}
        <svg
          viewBox="0 0 56 78"
          width="56"
          style={{ position: 'absolute', top: 14, left: 54, zIndex: 2 }}
          aria-hidden="true"
        >
          <rect x="26" y="30" width="4" height="48" fill="#9C8157" />
          <rect x="20" y="76" width="16" height="3" fill="#7A6440" />
          <g transform="translate(28,28)">
            <g className="windmill-blades" style={{ transformOrigin: '0 0' }}>
              <path d="M0 0 L3 -22 L-3 -22 Z" fill="#C0392B" />
              <path d="M0 0 L22 3 L22 -3 Z" fill="#E74C3C" />
              <path d="M0 0 L-3 22 L3 22 Z" fill="#C0392B" />
              <path d="M0 0 L-22 -3 L-22 3 Z" fill="#E74C3C" />
            </g>
            <circle r="3.5" fill="#7A6440" />
          </g>
        </svg>

        {/* Barn */}
        <svg
          viewBox="0 0 120 74"
          width="120"
          style={{ position: 'absolute', top: 24, right: 48, zIndex: 2 }}
          aria-hidden="true"
        >
          <path d="M8 30 L60 6 L112 30 L112 32 L8 32 Z" fill="#B23A2E" />
          <rect x="12" y="32" width="100" height="42" fill="#C0392B" />
          <rect x="50" y="44" width="20" height="30" fill="#8E2A20" />
          <path d="M50 44 h20 M60 44 v30 M50 54 h20" stroke="#fff" strokeWidth="1.5" fill="none" />
          <rect x="20" y="40" width="14" height="12" fill="#fff" opacity=".85" />
          <rect x="86" y="40" width="14" height="12" fill="#fff" opacity=".85" />
          <path d="M56 12 l8 0 l0 -6 l-8 0 z" fill="#8E2A20" />
        </svg>

        {/* Hay */}
        <div className="corral-hay" />

        {/* Fence */}
        <div className="corral-fence" />

        {/* Pasture */}
        <div className="corral-pasture">
          {/* Pasture header */}
          <div style={{
            display: 'flex', alignItems: 'center', gap: 10,
            marginBottom: 2, position: 'relative', zIndex: 1,
          }}>
            <span style={{ fontSize: 20 }}>🐴</span>
            <span style={{
              fontSize: 13, fontWeight: 900, color: '#2B5A1E',
              letterSpacing: '.5px', textTransform: 'uppercase',
              textShadow: '0 1px 0 rgba(255,255,255,.6)',
            }}>
              Curral dos Campeões
            </span>
            <span style={{
              background: '#2B5A1E', color: '#fff',
              fontSize: 11, fontWeight: 800,
              padding: '2px 9px', borderRadius: 999,
              boxShadow: '0 2px 6px rgba(0,0,0,.22)',
            }}>
              {curralAll.length}
            </span>
          </div>

          {/* Horse grid */}
          <div className="corral-horse-grid">
            {curralFiltered.length === 0 ? (
              <div style={{
                gridColumn: '1 / -1',
                textAlign: 'center', padding: '40px 20px',
                color: '#6B7178', fontSize: 13, fontWeight: 600,
              }}>
                Nenhum cliente neste filtro.
              </div>
            ) : (
              curralFiltered.map((impl) => {
                const mood      = computeMood(impl.demandasAbertasCount, impl.maiorPrioridadeAberta);
                const openCount = impl.demandasAbertasCount;
                const isCrit    = mood.key === 'critico';
                const hasOpen   = openCount > 0;

                return (
                  <Link
                    key={impl.id}
                    href={`/implantacoes/${impl.id}`}
                    className={[
                      'corral-horse',
                      isCrit  ? 'mood-critico' : '',
                      hasOpen ? 'has-open'     : '',
                    ].filter(Boolean).join(' ')}
                    style={{ '--mood-color': mood.color } as CSSProperties}
                  >
                    <div className="ch-mood">{mood.emoji}</div>
                    <div className="ch-badge">{openCount}</div>
                    <div className="ch-body">
                      <StandingHorse id={impl.id} moodKey={mood.key} />
                      <div className="ch-shadow" />
                    </div>
                    <div className="ch-label">{impl.clienteRazaoSocial}</div>
                    <div className="ch-status">{mood.op}</div>
                  </Link>
                );
              })
            )}
          </div>
        </div>
      </div>
    </>
  );
}
