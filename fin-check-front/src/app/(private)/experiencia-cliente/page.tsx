'use client';
import { useState, useMemo, type ReactNode } from 'react';
import { useClientes } from '@/lib/hooks/useClientes';
import { useEstabelecimentos } from '@/lib/hooks/useEstabelecimentos';
import { useExperienciaCliente, ExperienciaClienteParams } from '@/lib/hooks/useExperienciaCliente';
import { VariacaoBadge } from '@/components/shared/VariacaoBadge';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { EmptyState } from '@/components/shared/EmptyState';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow, TableFooter,
} from '@/components/ui/table';
import { VariacaoMetrica, VariacaoPorGrupo } from '@/lib/types/entities';
import { cn } from '@/lib/utils';

// ── Helpers ────────────────────────────────────────────────────────────────────

function fmt(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function fmtN(value: number): string {
  return value.toLocaleString('pt-BR');
}

function fmtPct(value: number): string {
  return `${value.toFixed(2)}%`;
}

function monthToRange(mes: string, ano: string): { dataInicio: string; dataFim: string } {
  const m = parseInt(mes, 10);
  const y = parseInt(ano, 10);
  const start = new Date(y, m - 1, 1);
  const end = new Date(y, m, 0);
  return {
    dataInicio: start.toISOString().split('T')[0],
    dataFim: end.toISOString().split('T')[0],
  };
}

// ── Period picker with interval / month toggle ────────────────────────────────

type PeriodMode = 'intervalo' | 'mes';

interface PeriodValue {
  mode: PeriodMode;
  dataInicio: string;
  dataFim: string;
  mes: string;
  ano: string;
}

const defaultPeriod = (): PeriodValue => ({
  mode: 'intervalo', dataInicio: '', dataFim: '', mes: '', ano: '',
});

function PeriodoPicker({
  label,
  value,
  onChange,
}: {
  label: string;
  value: PeriodValue;
  onChange: (v: PeriodValue) => void;
}) {
  return (
    <div className="space-y-2">
      <p className="text-sm font-semibold text-white/70">{label}</p>
      <div className="flex gap-1">
        {(['intervalo', 'mes'] as const).map((m) => (
          <button
            key={m}
            type="button"
            onClick={() => onChange({ ...value, mode: m })}
            className={cn(
              'rounded px-3 py-1 text-xs font-medium transition-colors',
              value.mode === m
                ? 'bg-white text-black'
                : 'bg-white/10 text-white/60 hover:bg-white/15 hover:text-white'
            )}
          >
            {m === 'intervalo' ? 'Intervalo' : 'Mês'}
          </button>
        ))}
      </div>
      {value.mode === 'intervalo' ? (
        <div className="flex gap-2">
          <div className="space-y-1">
            <Label className="text-xs text-white/50">De</Label>
            <Input
              type="date"
              value={value.dataInicio}
              onChange={(e) => onChange({ ...value, dataInicio: e.target.value })}
              className="w-36"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs text-white/50">Até</Label>
            <Input
              type="date"
              value={value.dataFim}
              onChange={(e) => onChange({ ...value, dataFim: e.target.value })}
              className="w-36"
            />
          </div>
        </div>
      ) : (
        <div className="flex gap-2">
          <div className="space-y-1">
            <Label className="text-xs text-white/50">Mês</Label>
            <Input
              type="number"
              min={1}
              max={12}
              placeholder="MM"
              value={value.mes}
              onChange={(e) => onChange({ ...value, mes: e.target.value })}
              className="w-20"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs text-white/50">Ano</Label>
            <Input
              type="number"
              min={2000}
              max={2100}
              placeholder="AAAA"
              value={value.ano}
              onChange={(e) => onChange({ ...value, ano: e.target.value })}
              className="w-28"
            />
          </div>
        </div>
      )}
    </div>
  );
}

// ── Metric card with variation badge ─────────────────────────────────────────

function MetricCard({
  label,
  value,
  variacao,
  isCusto,
  colorClass,
  threshold,
}: {
  label: string;
  value: string;
  variacao: VariacaoMetrica;
  isCusto?: boolean;
  colorClass?: string;
  threshold: number;
}) {
  const below = Math.abs(variacao.variacaoPercentual) < threshold;
  return (
    <div className={cn('rounded-lg border bg-card p-4 space-y-1 transition-opacity', below && threshold > 0 && 'opacity-30')}>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={cn('text-xl font-bold', colorClass)}>{value}</p>
      <VariacaoBadge variacao={variacao} isCusto={isCusto} />
    </div>
  );
}

// ── Group variation data by "grupo" field ─────────────────────────────────────

function groupByGrupo(data: VariacaoPorGrupo[]): Map<string, Record<string, VariacaoPorGrupo>> {
  const map = new Map<string, Record<string, VariacaoPorGrupo>>();
  for (const v of data) {
    if (!map.has(v.grupo)) map.set(v.grupo, {});
    map.get(v.grupo)![v.metrica] = v;
  }
  return map;
}

// ── Section header ────────────────────────────────────────────────────────────

function SectionHeader({ children }: { children: ReactNode }) {
  return (
    <h2 className="text-base font-semibold text-white/80 border-b border-white/10 pb-2">{children}</h2>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function ExperienciaClientePage() {
  const [clienteId, setClienteId] = useState('');
  const [estabelecimentoId, setEstabelecimentoId] = useState('');
  const [periodoP, setPeriodoP] = useState<PeriodValue>(defaultPeriod());
  const [periodoC, setPeriodoC] = useState<PeriodValue>(defaultPeriod());
  const [threshold, setThreshold] = useState(0);
  const [params, setParams] = useState<ExperienciaClienteParams | null>(null);

  const { data: clientes, isLoading: loadingClientes } = useClientes();
  const { data: estabelecimentos, isLoading: loadingEstab } = useEstabelecimentos(clienteId);
  const { data, isLoading } = useExperienciaCliente(params);

  const activeClientes = clientes?.filter((c) => c.ativo) ?? [];
  const activeEstabs = estabelecimentos?.filter((e) => e.ativo) ?? [];

  function resolveDates(p: PeriodValue): { dataInicio: string; dataFim: string } | null {
    if (p.mode === 'intervalo') {
      if (!p.dataInicio || !p.dataFim) return null;
      return { dataInicio: p.dataInicio, dataFim: p.dataFim };
    }
    if (!p.mes || !p.ano) return null;
    return monthToRange(p.mes, p.ano);
  }

  function handleConsultar() {
    if (!estabelecimentoId) return;
    const principal = resolveDates(periodoP);
    const comparacao = resolveDates(periodoC);
    if (!principal || !comparacao) return;
    setParams({
      estabelecimentoId,
      dataInicio: principal.dataInicio,
      dataFim: principal.dataFim,
      dataInicioComparacao: comparacao.dataInicio,
      dataFimComparacao: comparacao.dataFim,
    });
  }

  const canConsultar = !!estabelecimentoId && !!resolveDates(periodoP) && !!resolveDates(periodoC);

  // Memoised groupings
  const bandeirasRec = useMemo(
    () => (data ? groupByGrupo(data.insights.recebimentos_porBandeira) : new Map()),
    [data]
  );
  const descAjustes = useMemo(
    () => (data ? groupByGrupo(data.insights.recebimentos_porDescAjuste) : new Map()),
    [data]
  );
  const bandeirasConc = useMemo(
    () => (data ? groupByGrupo(data.insights.conciliacao_porBandeira) : new Map()),
    [data]
  );
  const adquirentes = useMemo(
    () => (data ? groupByGrupo(data.insights.conciliacao_porAdquirente) : new Map()),
    [data]
  );

  const below = (v: number) => threshold > 0 && Math.abs(v) < threshold;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Experiência do Cliente</h1>

      {/* ── Filter card ─────────────────────────────────────────────────────── */}
      <div className="rounded-lg border bg-card p-5 space-y-5">
        {/* Client + Establishment */}
        <div className="flex gap-4 flex-wrap items-end">
          <div className="space-y-1.5">
            <Label>Cliente</Label>
            <Select
              value={clienteId}
              onValueChange={(v) => { setClienteId(v); setEstabelecimentoId(''); setParams(null); }}
              disabled={loadingClientes}
            >
              <SelectTrigger className="w-64">
                <SelectValue placeholder="Selecione o cliente" />
              </SelectTrigger>
              <SelectContent>
                {activeClientes.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.nomeFantasia ?? c.razaoSocial}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label>Estabelecimento</Label>
            <Select
              value={estabelecimentoId}
              onValueChange={(v) => { setEstabelecimentoId(v); setParams(null); }}
              disabled={!clienteId || loadingEstab}
            >
              <SelectTrigger className="w-64">
                <SelectValue
                  placeholder={clienteId ? 'Selecione o estabelecimento' : 'Selecione o cliente primeiro'}
                />
              </SelectTrigger>
              <SelectContent>
                {activeEstabs.map((e) => (
                  <SelectItem key={e.id} value={e.id}>
                    {e.descricao}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Period pickers */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <PeriodoPicker label="Período Principal" value={periodoP} onChange={setPeriodoP} />
          <PeriodoPicker label="Período de Comparação" value={periodoC} onChange={setPeriodoC} />
        </div>

        {/* Threshold slider */}
        <div className="space-y-1.5">
          <Label className="text-sm">
            Mostrar variações acima de:{' '}
            <span className="font-bold text-white">{threshold}%</span>
          </Label>
          <div className="flex items-center gap-3">
            <span className="text-xs text-white/40">0%</span>
            <input
              type="range"
              min={0}
              max={20}
              step={1}
              value={threshold}
              onChange={(e) => setThreshold(Number(e.target.value))}
              className="flex-1 accent-white"
            />
            <span className="text-xs text-white/40">20%</span>
          </div>
        </div>

        <Button onClick={handleConsultar} disabled={!canConsultar}>
          Consultar
        </Button>
      </div>

      {/* ── Results ─────────────────────────────────────────────────────────── */}
      {!params ? (
        <EmptyState message="Selecione o estabelecimento e os períodos para consultar" />
      ) : isLoading ? (
        <LoadingSpinner />
      ) : !data ? (
        <EmptyState message="Nenhum dado encontrado para os períodos selecionados" />
      ) : (
        <div className="space-y-8">

          {/* SEÇÃO 1 — Recebimentos: Visão Geral */}
          <div className="space-y-3">
            <SectionHeader>Recebimentos — Visão Geral</SectionHeader>
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
              <MetricCard
                label="Transações"
                value={fmtN(data.totalGeralTransacoes)}
                variacao={data.insights.recebimentos_quantidadeTransacoes}
                threshold={threshold}
              />
              <MetricCard
                label="Valor Bruto"
                value={fmt(data.totalGeralBruto)}
                variacao={data.insights.recebimentos_valorBruto}
                threshold={threshold}
              />
              <MetricCard
                label="Total em Taxas"
                value={fmt(data.totalGeralTaxas)}
                variacao={data.insights.recebimentos_totalTaxas}
                isCusto
                colorClass="text-red-400"
                threshold={threshold}
              />
              <MetricCard
                label="Tarifa por Transação"
                value={fmt(data.totalGeralTarifaTransacao)}
                variacao={data.insights.recebimentos_tarifaTransacao}
                isCusto
                colorClass="text-red-400"
                threshold={threshold}
              />
              <MetricCard
                label="Valor Líquido"
                value={fmt(data.totalGeralLiquido)}
                variacao={data.insights.recebimentos_valorLiquido}
                colorClass="text-green-400"
                threshold={threshold}
              />
            </div>
          </div>

          {/* SEÇÃO 2 — Recebimentos: por Tipo de Lançamento */}
          <div className="space-y-3">
            <SectionHeader>Recebimentos — Variação por Tipo de Lançamento</SectionHeader>
            <div className="rounded-lg border overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Tipo de Lançamento</TableHead>
                    <TableHead className="text-right">Qtd</TableHead>
                    <TableHead className="text-right">Valor Bruto</TableHead>
                    <TableHead className="text-right text-red-400">Total Taxas</TableHead>
                    <TableHead className="text-right text-red-400">Tarifa</TableHead>
                    <TableHead className="text-right text-green-400">Valor Líquido</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.gruposRecebimento.map((g) => {
                    const vpg = descAjustes.get(g.descAjuste) ?? {};
                    const rowBelow = [
                      vpg.valorBruto?.variacaoPercentual,
                      vpg.totalTaxas?.variacaoPercentual,
                      vpg.tarifaTransacao?.variacaoPercentual,
                      vpg.valorLiquido?.variacaoPercentual,
                    ].every((v) => v !== undefined && below(v));
                    return (
                      <TableRow
                        key={g.descAjuste}
                        className={cn(rowBelow && threshold > 0 && 'opacity-30')}
                      >
                        <TableCell className="max-w-xs truncate text-xs">{g.descAjuste}</TableCell>
                        <TableCell className="text-right">{fmtN(g.quantidadeTransacoes)}</TableCell>
                        <TableCell className="text-right">
                          <div>{fmt(g.valorBrutoTotal)}</div>
                          {vpg.valorBruto && <VariacaoBadge variacao={vpg.valorBruto} />}
                        </TableCell>
                        <TableCell className="text-right">
                          <div>{fmt(g.totalTaxas)}</div>
                          {vpg.totalTaxas && <VariacaoBadge variacao={vpg.totalTaxas} isCusto />}
                        </TableCell>
                        <TableCell className="text-right">
                          <div>{fmt(g.tarifaTransacaoTotal)}</div>
                          {vpg.tarifaTransacao && <VariacaoBadge variacao={vpg.tarifaTransacao} isCusto />}
                        </TableCell>
                        <TableCell className="text-right">
                          <div>{fmt(g.valorLiquidoTotal)}</div>
                          {vpg.valorLiquido && <VariacaoBadge variacao={vpg.valorLiquido} />}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
                <TableFooter>
                  <TableRow className="font-bold">
                    <TableCell>TOTAIS</TableCell>
                    <TableCell className="text-right">{fmtN(data.totalGeralTransacoes)}</TableCell>
                    <TableCell className="text-right">{fmt(data.totalGeralBruto)}</TableCell>
                    <TableCell className="text-right">{fmt(data.totalGeralTaxas)}</TableCell>
                    <TableCell className="text-right">{fmt(data.totalGeralTarifaTransacao)}</TableCell>
                    <TableCell className="text-right">{fmt(data.totalGeralLiquido)}</TableCell>
                  </TableRow>
                </TableFooter>
              </Table>
            </div>
          </div>

          {/* SEÇÃO 3 — Recebimentos: por Bandeira */}
          {bandeirasRec.size > 0 && (
            <div className="space-y-3">
              <SectionHeader>Recebimentos — Variação por Bandeira</SectionHeader>
              <div className="rounded-lg border overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Bandeira</TableHead>
                      <TableHead className="text-right">Valor Bruto</TableHead>
                      <TableHead className="text-right text-red-400">Total Taxas</TableHead>
                      <TableHead className="text-right text-red-400">Tarifa</TableHead>
                      <TableHead className="text-right text-green-400">Valor Líquido</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {Array.from(bandeirasRec.entries()).map(([bandeira, vpg]) => {
                      const rowBelow = [
                        vpg.valorBruto?.variacaoPercentual,
                        vpg.totalTaxas?.variacaoPercentual,
                        vpg.tarifaTransacao?.variacaoPercentual,
                        vpg.valorLiquido?.variacaoPercentual,
                      ].every((v) => v !== undefined && below(v));
                      return (
                        <TableRow key={bandeira} className={cn(rowBelow && threshold > 0 && 'opacity-30')}>
                          <TableCell className="font-medium">{bandeira ?? '—'}</TableCell>
                          <TableCell className="text-right">
                            <div>{vpg.valorBruto ? fmt(vpg.valorBruto.valorPrincipal) : '—'}</div>
                            {vpg.valorBruto && <VariacaoBadge variacao={vpg.valorBruto} />}
                          </TableCell>
                          <TableCell className="text-right">
                            <div>{vpg.totalTaxas ? fmt(vpg.totalTaxas.valorPrincipal) : '—'}</div>
                            {vpg.totalTaxas && <VariacaoBadge variacao={vpg.totalTaxas} isCusto />}
                          </TableCell>
                          <TableCell className="text-right">
                            <div>{vpg.tarifaTransacao ? fmt(vpg.tarifaTransacao.valorPrincipal) : '—'}</div>
                            {vpg.tarifaTransacao && <VariacaoBadge variacao={vpg.tarifaTransacao} isCusto />}
                          </TableCell>
                          <TableCell className="text-right">
                            <div>{vpg.valorLiquido ? fmt(vpg.valorLiquido.valorPrincipal) : '—'}</div>
                            {vpg.valorLiquido && <VariacaoBadge variacao={vpg.valorLiquido} />}
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>
            </div>
          )}

          {/* SEÇÃO 4 — Conciliação de Taxas: Visão Geral */}
          <div className="space-y-3">
            <SectionHeader>Conciliação de Taxas — Visão Geral</SectionHeader>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <MetricCard
                label="Transações"
                value={fmtN(data.insights.conciliacao_quantidadeTransacoes.valorPeriodoPrincipal)}
                variacao={data.insights.conciliacao_quantidadeTransacoes}
                threshold={threshold}
              />
              <MetricCard
                label="Valor Bruto"
                value={fmt(data.insights.conciliacao_valorBruto.valorPeriodoPrincipal)}
                variacao={data.insights.conciliacao_valorBruto}
                threshold={threshold}
              />
              <MetricCard
                label="Taxa Não Contratada"
                value={fmt(data.insights.conciliacao_totalTaxaNaoContratada.valorPeriodoPrincipal)}
                variacao={data.insights.conciliacao_totalTaxaNaoContratada}
                isCusto
                colorClass="text-red-400"
                threshold={threshold}
              />
              <MetricCard
                label="% Taxa Médio"
                value={fmtPct(data.insights.conciliacao_percentualTaxaMedio.valorPeriodoPrincipal)}
                variacao={data.insights.conciliacao_percentualTaxaMedio}
                isCusto
                threshold={threshold}
              />
            </div>
          </div>

          {/* SEÇÃO 5 — Conciliação: por Bandeira */}
          {bandeirasConc.size > 0 && (
            <div className="space-y-3">
              <SectionHeader>Conciliação de Taxas — Variação por Bandeira</SectionHeader>
              <ConciliacaoGrupoTable data={bandeirasConc} threshold={threshold} />
            </div>
          )}

          {/* SEÇÃO 6 — Conciliação: por Adquirente */}
          {adquirentes.size > 0 && (
            <div className="space-y-3">
              <SectionHeader>Conciliação de Taxas — Variação por Adquirente</SectionHeader>
              <ConciliacaoGrupoTable data={adquirentes} threshold={threshold} />
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ── Conciliação group table (reused for bandeira and adquirente) ───────────────

function ConciliacaoGrupoTable({
  data,
  threshold,
}: {
  data: Map<string, Record<string, VariacaoPorGrupo>>;
  threshold: number;
}) {
  const below = (v: number) => threshold > 0 && Math.abs(v) < threshold;

  return (
    <div className="rounded-lg border overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Grupo</TableHead>
            <TableHead className="text-right">Valor Bruto</TableHead>
            <TableHead className="text-right text-red-400">Taxa Não Contratada</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {Array.from(data.entries()).map(([grupo, vpg]) => {
            const rowBelow = [
              vpg.valorBruto?.variacaoPercentual,
              vpg.totalTaxaNaoContratada?.variacaoPercentual,
            ].every((v) => v !== undefined && below(v));
            return (
              <TableRow key={grupo} className={cn(rowBelow && threshold > 0 && 'opacity-30')}>
                <TableCell className="font-medium">{grupo ?? '—'}</TableCell>
                <TableCell className="text-right">
                  <div>{vpg.valorBruto ? fmt(vpg.valorBruto.valorPrincipal) : '—'}</div>
                  {vpg.valorBruto && <VariacaoBadge variacao={vpg.valorBruto} />}
                </TableCell>
                <TableCell className="text-right">
                  <div>{vpg.totalTaxaNaoContratada ? fmt(vpg.totalTaxaNaoContratada.valorPrincipal) : '—'}</div>
                  {vpg.totalTaxaNaoContratada && (
                    <VariacaoBadge variacao={vpg.totalTaxaNaoContratada} isCusto />
                  )}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
