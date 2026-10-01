'use client';

import { useState } from 'react';
import Image from 'next/image';
import type { CSSProperties } from 'react';
import Link from 'next/link';
import { ImplantacaoCliente, ImplantacaoClasse } from '@/lib/types/entities';
import { ImplantacaoDespacharDialog } from './ImplantacaoDespacharDialog';

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

// ─── Cavalo: arte oficial ─────────────────────────────────────────────────────
// A sela faz parte da própria arte — não há nenhuma camada desenhada por cima.
//
// Os quatro PNGs extraídos da sprite têm canvas idêntico de 599×697 e o cavalo na
// mesma posição em todos, então trocar de Classe não faz o animal pular nem mudar
// de tamanho. 73×85 preserva a razão 599/697 — a imagem só é escalada, nunca
// deformada. O cavalo fica com ~70×84px dentro do slot de 86px do card.
//
// Era 102×85 quando a arte era paisagem (1374×1145); as novas são retrato, daí a
// largura menor. A ALTURA foi mantida, de modo que a presença visual no card não muda.
const HORSE_W = 73;
const HORSE_H = 85;

// Classe → arte. A sela faz parte de cada PNG: não há recoloração, filtro nem
// camada desenhada por cima. `null` mantém o cavalo de sela de couro marrom, que
// é uma representação distinta de BRONZE.
const HORSE_POR_CLASSE: Record<ImplantacaoClasse, string> = {
  PRIME:   '/curral/cavalo-prime.png',    // sela azul (cristal/diamante)
  GOLD:    '/curral/cavalo-gold.png',     // sela dourada
  PLATIUM: '/curral/cavalo-platium.png',  // sela prateada
  BRONZE:  '/curral/cavalo-bronze.png',   // sela bronze
};
const HORSE_SEM_CLASSE = '/curral/cavalo-sem-classe.png'; // sela de couro marrom

/**
 * Cavalo do Curral — a arte oficial, sem recriação e sem sobreposições.
 *
 * <p>A sela faz parte da própria arte: a Classe escolhe o PNG, e é só isso. Nenhuma
 * cor é processada em tempo de execução.
 *
 * <p>O projeto já usa `images: { unoptimized: true }`, então o Next serve o PNG
 * byte a byte: nada é recomprimido nem reamostrado. Clientes da mesma Classe
 * compartilham o mesmo arquivo estático.
 */
function StandingHorse({ classe }: { classe: ImplantacaoClasse | null }) {
  return (
    <div className="ch-horse-wrap">
      <Image
        src={classe ? HORSE_POR_CLASSE[classe] : HORSE_SEM_CLASSE}
        alt=""
        width={HORSE_W}
        height={HORSE_H}
        className="ch-horse-img"
        aria-hidden="true"
      />
    </div>
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

// ─── Setores por classe ───────────────────────────────────────────────────────
// A classe define a prioridade MACRO dos setores. Dentro de cada setor a ordem de
// chegada é preservada tal como vem da API (createdAt DESC) — nenhum algoritmo de
// ordenação interna foi introduzido.

/** Chave interna para os registros ainda não classificados. Não é uma quinta classe. */
const SEM_CLASSE = 'SEM_CLASSE' as const;

type SetorKey = ImplantacaoClasse | typeof SEM_CLASSE;

const SETOR_ORDEM: SetorKey[] = ['PRIME', 'GOLD', 'PLATIUM', 'BRONZE', SEM_CLASSE];

const SETOR_CONFIG: Record<SetorKey, { emoji: string; titulo: string }> = {
  PRIME:      { emoji: '💎', titulo: 'Classe Prime' },
  GOLD:       { emoji: '🥇', titulo: 'Classe Gold' },
  PLATIUM:    { emoji: '🥈', titulo: 'Classe Platium' },
  BRONZE:     { emoji: '🥉', titulo: 'Classe Bronze' },
  [SEM_CLASSE]: { emoji: '🐎', titulo: 'Sem classe' },
};

function setorDe(impl: ImplantacaoCliente): SetorKey {
  return impl.classe ?? SEM_CLASSE;
}

// ─── Component ───────────────────────────────────────────────────────────────

interface Props {
  implantacoes: ImplantacaoCliente[];
}

export function ImplantacaoCurral({ implantacoes }: Props) {
  const [filter, setFilter] = useState<CurralFilter>('todos');
  const [despacharOpen, setDespacharOpen] = useState(false);

  const curralAll      = implantacoes.filter((i) => i.etapa === 'curral');
  const curralFiltered = applyMoodFilter(curralAll, filter);

  // Setores na ordem macro, já sem os vazios — um setor sem resultado após o filtro é
  // omitido para não deixar buraco na tela, sem alterar o significado do filtro.
  const setores = SETOR_ORDEM
    .map((key) => ({
      key,
      config: SETOR_CONFIG[key],
      itens: curralFiltered.filter((i) => setorDe(i) === key),
    }))
    .filter((s) => s.itens.length > 0);

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
        /* Rolagem única de todos os setores — antes vivia na própria grade. Mover para
           cá permite que cada setor cresça quantas linhas precisar. */
        .corral-sectors {
          max-height: 520px; overflow-y: auto; overflow-x: hidden;
          padding: 6px 4px 8px 2px;
          position: relative; z-index: 1;
        }
        .corral-sectors::-webkit-scrollbar { width: 6px }
        .corral-sectors::-webkit-scrollbar-track { background: rgba(255,255,255,.25); border-radius: 8px }
        .corral-sectors::-webkit-scrollbar-thumb { background: rgba(60,120,30,.35); border-radius: 8px }
        .corral-sector { position: relative; }
        /* Placa de madeira do setor, no vocabulário do celeiro */
        .corral-sector-head {
          display: inline-flex; align-items: center; gap: 8px;
          margin: 10px 0 2px; padding: 5px 13px 5px 10px;
          border-radius: 8px;
          background: linear-gradient(180deg,#F6E6C8,#E3C89A);
          border: 1.5px solid rgba(124,86,42,.55);
          box-shadow: 0 2px 5px rgba(60,40,15,.22), inset 0 1px 0 rgba(255,255,255,.65);
        }
        .corral-sector-head .cs-emoji { font-size: 14px; line-height: 1 }
        .corral-sector-head .cs-title {
          font-size: 11.5px; font-weight: 900; letter-spacing: .6px;
          text-transform: uppercase; color: #5C3E1A;
          text-shadow: 0 1px 0 rgba(255,255,255,.5);
        }
        .corral-sector-head .cs-count {
          font-size: 10px; font-weight: 800; color: #fff;
          background: rgba(92,62,26,.82);
          padding: 1px 7px; border-radius: 999px;
        }
        /* Cerca de divisa: mesmos trilhos e mourões brancos da cerca do curral */
        .corral-sector-fence {
          position: relative; height: 16px; margin: 14px 0 2px;
        }
        .corral-sector-fence::before {
          content: '';
          position: absolute; left: 0; right: 0; top: 3px; height: 3px;
          background: rgba(255,255,255,.92);
          box-shadow: 0 6px 0 rgba(255,255,255,.92), 0 1px 4px rgba(0,0,0,.12);
        }
        .corral-sector-fence::after {
          content: '';
          position: absolute; inset: 0;
          background: repeating-linear-gradient(90deg,
            rgba(255,255,255,.92) 0 5px, transparent 5px 44px);
          pointer-events: none;
        }
        .corral-horse-grid {
          display: grid;
          grid-template-columns: repeat(8, minmax(0, 1fr));
          gap: 18px 14px;
          padding: 12px 2px 4px 2px;
          position: relative; z-index: 1;
        }
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
        /* O wrapper é maior que o slot de propósito: a margem transparente do PNG
           transborda sem pintar nada, e o cavalo aproveita o card inteiro. */
        .corral-horse .ch-horse-wrap {
          position: relative; flex: none;
          width: ${HORSE_W}px; height: ${HORSE_H}px;
        }
        .corral-horse .ch-horse-img {
          display: block; width: ${HORSE_W}px; height: ${HORSE_H}px;
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
        {/* Coluna de ações: "Nova Implantação" (inalterada) e "Despachar" logo abaixo,
            ambas esticadas para a mesma largura. */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'stretch', gap: 8 }}>
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
          <button
            type="button"
            onClick={() => setDespacharOpen(true)}
            style={{
              display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
              padding: '10px 16px', borderRadius: 12, border: 'none',
              background: 'linear-gradient(135deg,#D9534F,#B23B37)',
              color: '#fff', fontWeight: 700, fontSize: 13,
              letterSpacing: '.2px', cursor: 'pointer',
              boxShadow: '0 6px 16px rgba(217,83,79,.35)',
            }}
          >
            Despachar
          </button>
        </div>
      </div>

      <ImplantacaoDespacharDialog
        open={despacharOpen}
        onOpenChange={setDespacharOpen}
        candidatos={curralAll}
      />

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

          {/* Setores por classe — o scroll vive aqui, para que os setores cresçam
              verticalmente e a rolagem continue sendo uma só, como antes. */}
          <div className="corral-sectors">
            {setores.length === 0 ? (
              <div style={{
                textAlign: 'center', padding: '40px 20px',
                color: '#6B7178', fontSize: 13, fontWeight: 600,
              }}>
                Nenhum cliente neste filtro.
              </div>
            ) : setores.map((setor, idx) => (
              <div key={setor.key} className="corral-sector">
                {/* Placa do setor */}
                <div className="corral-sector-head">
                  <span className="cs-emoji" aria-hidden="true">{setor.config.emoji}</span>
                  <span className="cs-title">{setor.config.titulo}</span>
                  <span className="cs-count">{setor.itens.length}</span>
                </div>

                <div className="corral-horse-grid">
                  {setor.itens.map((impl) => {
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
                      <StandingHorse classe={impl.classe} />
                      <div className="ch-shadow" />
                    </div>
                    <div className="ch-label">{impl.clienteRazaoSocial}</div>
                    <div className="ch-status">{mood.op}</div>
                  </Link>
                );
                  })}
                </div>

                {/* Cerca de divisa — só entre setores, nunca após o último */}
                {idx < setores.length - 1 && (
                  <div className="corral-sector-fence" aria-hidden="true" />
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  );
}
