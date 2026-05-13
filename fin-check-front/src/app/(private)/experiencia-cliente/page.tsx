'use client';
import { useState, useMemo, type ReactNode } from 'react';
import { ArrowRight } from 'lucide-react';
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

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmt(v: number) {
  return v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}
function fmtN(v: number) {
  return v.toLocaleString('pt-BR');
}
function fmtPct(v: number) {
  return `${v.toFixed(2)}%`;
}
function fmtDate(iso: string) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

function monthToRange(mes: string, ano: string) {
  const m = parseInt(mes, 10);
  const y = parseInt(ano, 10);
  return {
    dataInicio: new Date(y, m - 1, 1).toISOString().split('T')[0],
    dataFim: new Date(y, m, 0).toISOString().split('T')[0],
  };
}

// ── Period picker ─────────────────────────────────────────────────────────────

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
  badge,
  value,
  onChange,
}: {
  label: string;
  badge: ReactNode;
  value: PeriodValue;
  onChange: (v: PeriodValue) => void;
}) {
  return (
    <div className="rounded-md border border-border bg-muted/30 p-4 space-y-3">
      {/* Header */}
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-foreground">{label}</span>
        {badge}
      </div>

      {/* Mode toggle */}
      <div className="flex gap-1">
        {(['intervalo', 'mes'] as const).map((m) => (
          <button
            key={m}
            type="button"
            onClick={() => onChange({ ...value, mode: m })}
            className={cn(
              'rounded px-3 py-1 text-xs font-medium transition-colors',
              value.mode === m
                ? 'bg-primary text-primary-foreground'
                : 'bg-muted text-muted-foreground hover:bg-accent hover:text-accent-foreground'
            )}
          >
            {m === 'intervalo' ? 'Intervalo' : 'Mês'}
          </button>
        ))}
      </div>

      {/* Inputs */}
      {value.mode === 'intervalo' ? (
        <div className="flex gap-3 flex-wrap">
          <div className="space-y-1">
            <Label className="text-xs text-muted-foreground">De</Label>
            <Input
              type="date"
              value={value.dataInicio}
              onChange={(e) => onChange({ ...value, dataInicio: e.target.value })}
              className="w-36 bg-muted"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs text-muted-foreground">Até</Label>
            <Input
              type="date"
              value={value.dataFim}
              onChange={(e) => onChange({ ...value, dataFim: e.target.value })}
              className="w-36 bg-muted"
            />
          </div>
        </div>
      ) : (
        <div className="flex gap-3 flex-wrap">
          <div className="space-y-1">
            <Label className="text-xs text-muted-foreground">Mês</Label>
            <Input
              type="number"
              min={1}
              max={12}
              placeholder="MM"
              value={value.mes}
              onChange={(e) => onChange({ ...value, mes: e.target.value })}
              className="w-20 bg-muted"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs text-muted-foreground">Ano</Label>
            <Input
              type="number"
              min={2000}
              max={2100}
              placeholder="AAAA"
              value={value.ano}
              onChange={(e) => onChange({ ...value, ano: e.target.value })}
              className="w-28 bg-muted"
            />
          </div>
        </div>
      )}
    </div>
  );
}

// ── Comparison banner (shown after submit) ────────────────────────────────────

function ComparacaoBanner({ params }: { params: ExperienciaClienteParams }) {
  return (
    <div className="flex items-center gap-2 rounded-md border border-border bg-muted/40 px-4 py-2.5 text-sm flex-wrap">
      <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Comparando</span>
      <span className="font-medium text-foreground">
        {fmtDate(params.dataInicio)} – {fmtDate(params.dataFim)}
      </span>
      <span className="text-muted-foreground text-xs px-1">vs</span>
      <span className="font-medium text-foreground">
        {fmtDate(params.dataInicioComparacao)} – {fmtDate(params.dataFimComparacao)}
      </span>
      <span className="text-xs text-muted-foreground ml-1">
        — as variações (%) mostram quanto o período principal mudou em relação ao período de comparação
      </span>
    </div>
  );
}

// ── Metric card ───────────────────────────────────────────────────────────────

function MetricCard({
  label, value, variacao, isCusto, colorClass,
}: {
  label: string;
  value: string;
  variacao: VariacaoMetrica;
  isCusto?: boolean;
  colorClass?: string;
}) {
  return (
    <div className="rounded-lg border bg-card p-4 space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={cn('text-xl font-bold', colorClass ?? 'text-foreground')}>{value}</p>
      <VariacaoBadge variacao={variacao} isCusto={isCusto} />
    </div>
  );
}

// ── Section header ────────────────────────────────────────────────────────────

function SectionHeader({ children }: { children: ReactNode }) {
  return (
    <h2 className="text-base font-semibold text-foreground border-b border-border pb-2">{children}</h2>
  );
}

// ── Group VariacaoPorGrupo[] by "grupo" ───────────────────────────────────────

function groupByGrupo(data: VariacaoPorGrupo[]): Map<string, Record<string, VariacaoPorGrupo>> {
  const map = new Map<string, Record<string, VariacaoPorGrupo>>();
  for (const v of data) {
    if (!map.has(v.grupo)) map.set(v.grupo, {});
    map.get(v.grupo)![v.metrica] = v;
  }
  return map;
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function ExperienciaClientePage() {
  const [clienteId, setClienteId] = useState('');
  const [estabelecimentoId, setEstabelecimentoId] = useState('');
  const [periodoP, setPeriodoP] = useState<PeriodValue>(defaultPeriod());
  const [periodoC, setPeriodoC] = useState<PeriodValue>(defaultPeriod());
  const [params, setParams] = useState<ExperienciaClienteParams | null>(null);

  const { data: clientes, isLoading: loadingClientes } = useClientes();
  const { data: estabelecimentos, isLoading: loadingEstab } = useEstabelecimentos(clienteId);
  const { data, isLoading } = useExperienciaCliente(params);

  const activeClientes = clientes?.filter((c) => c.ativo) ?? [];
  const activeEstabs   = estabelecimentos?.filter((e) => e.ativo) ?? [];

  function resolveDates(p: PeriodValue) {
    if (p.mode === 'intervalo') {
      if (!p.dataInicio || !p.dataFim) return null;
      return { dataInicio: p.dataInicio, dataFim: p.dataFim };
    }
    if (!p.mes || !p.ano) return null;
    return monthToRange(p.mes, p.ano);
  }

  function handleConsultar() {
    if (!estabelecimentoId) return;
    const principal  = resolveDates(periodoP);
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

  const bandeirasRec  = useMemo(() => data ? groupByGrupo(data.insights.recebimentos_porBandeira)   : new Map(), [data]);
  const descAjustes   = useMemo(() => data ? groupByGrupo(data.insights.recebimentos_porDescAjuste) : new Map(), [data]);
  const bandeirasConc = useMemo(() => data ? groupByGrupo(data.insights.conciliacao_porBandeira)    : new Map(), [data]);
  const adquirentes   = useMemo(() => data ? groupByGrupo(data.insights.conciliacao_porAdquirente)  : new Map(), [data]);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Experiência do Cliente</h1>

      {/* ── Filter card ────────────────────────────────────────────────────── */}
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
              <SelectTrigger className="w-64 bg-muted">
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
              <SelectTrigger className="w-64 bg-muted">
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

        {/* Period pickers with VS connector */}
        <div>
          <p className="text-xs text-muted-foreground mb-3">
            Selecione dois períodos para comparar — os indicadores mostram quanto o período principal variou em relação ao período de comparação.
          </p>
          <div className="flex gap-3 items-start flex-wrap">
            <div className="flex-1 min-w-[280px]">
              <PeriodoPicker
                label="Período Principal"
                badge={
                  <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary">
                    Base
                  </span>
                }
                value={periodoP}
                onChange={setPeriodoP}
              />
            </div>

            {/* VS divider */}
            <div className="flex flex-col items-center justify-center pt-10 gap-1 shrink-0">
              <ArrowRight className="h-4 w-4 text-muted-foreground" />
              <span className="text-xs font-bold text-muted-foreground">VS</span>
              <ArrowRight className="h-4 w-4 text-muted-foreground rotate-180" />
            </div>

            <div className="flex-1 min-w-[280px]">
              <PeriodoPicker
                label="Período de Comparação"
                badge={
                  <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-semibold text-muted-foreground">
                    Referência
                  </span>
                }
                value={periodoC}
                onChange={setPeriodoC}
              />
            </div>
          </div>
        </div>

        <Button onClick={handleConsultar} disabled={!canConsultar}>
          Consultar
        </Button>
      </div>

      {/* ── Results ──────────────────────────────────────────────────────────── */}
      {!params ? (
        <EmptyState message="Selecione o estabelecimento e os períodos para consultar" />
      ) : isLoading ? (
        <LoadingSpinner />
      ) : !data ? (
        <EmptyState message="Nenhum dado encontrado para os períodos selecionados" />
      ) : (
        <div className="space-y-8">

          {/* Comparison summary banner */}
          <ComparacaoBanner params={params} />

          {/* SEÇÃO 1 — Recebimentos: Visão Geral */}
          <div className="space-y-3">
            <SectionHeader>Recebimentos — Visão Geral</SectionHeader>
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
              <MetricCard label="Transações"          value={fmtN(data.totalGeralTransacoes)}       variacao={data.insights.recebimentos_quantidadeTransacoes} />
              <MetricCard label="Valor Bruto"         value={fmt(data.totalGeralBruto)}              variacao={data.insights.recebimentos_valorBruto} />
              <MetricCard label="Total em Taxas"      value={fmt(data.totalGeralTaxas)}              variacao={data.insights.recebimentos_totalTaxas}           isCusto colorClass="text-red-500" />
              <MetricCard label="Tarifa / Transação"  value={fmt(data.totalGeralTarifaTransacao)}    variacao={data.insights.recebimentos_tarifaTransacao}      isCusto colorClass="text-red-500" />
              <MetricCard label="Valor Líquido"       value={fmt(data.totalGeralLiquido)}            variacao={data.insights.recebimentos_valorLiquido}         colorClass="text-green-500" />
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
                    <TableHead className="text-right text-red-500">Total Taxas</TableHead>
                    <TableHead className="text-right text-red-500">Tarifa</TableHead>
                    <TableHead className="text-right text-green-500">Valor Líquido</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.gruposRecebimento.map((g) => {
                    const vpg = descAjustes.get(g.descAjuste) ?? {};
                    return (
                      <TableRow key={g.descAjuste}>
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
                      <TableHead className="text-right text-red-500">Total Taxas</TableHead>
                      <TableHead className="text-right text-red-500">Tarifa</TableHead>
                      <TableHead className="text-right text-green-500">Valor Líquido</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {Array.from(bandeirasRec.entries()).map(([bandeira, vpg]) => {
                      return (
                        <TableRow key={bandeira}>
                          <TableCell className="font-medium">{bandeira || '—'}</TableCell>
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
              <MetricCard label="Transações"          value={fmtN(data.insights.conciliacao_quantidadeTransacoes.valorPeriodoPrincipal)} variacao={data.insights.conciliacao_quantidadeTransacoes} />
              <MetricCard label="Valor Bruto"         value={fmt(data.insights.conciliacao_valorBruto.valorPeriodoPrincipal)}            variacao={data.insights.conciliacao_valorBruto} />
              <MetricCard label="Taxa Não Contratada" value={fmt(data.insights.conciliacao_totalTaxaNaoContratada.valorPeriodoPrincipal)} variacao={data.insights.conciliacao_totalTaxaNaoContratada} isCusto colorClass="text-red-500" />
              <MetricCard label="% Taxa Médio"        value={fmtPct(data.insights.conciliacao_percentualTaxaMedio.valorPeriodoPrincipal)} variacao={data.insights.conciliacao_percentualTaxaMedio}    isCusto />
            </div>
          </div>

          {/* SEÇÃO 5 — Conciliação: por Bandeira */}
          {bandeirasConc.size > 0 && (
            <div className="space-y-3">
              <SectionHeader>Conciliação de Taxas — Variação por Bandeira</SectionHeader>
              <ConciliacaoGrupoTable data={bandeirasConc} />
            </div>
          )}

          {/* SEÇÃO 6 — Conciliação: por Adquirente */}
          {adquirentes.size > 0 && (
            <div className="space-y-3">
              <SectionHeader>Conciliação de Taxas — Variação por Adquirente</SectionHeader>
              <ConciliacaoGrupoTable data={adquirentes} />
            </div>
          )}

        </div>
      )}
    </div>
  );
}

// ── Conciliação group table ───────────────────────────────────────────────────

function ConciliacaoGrupoTable({
  data,
}: {
  data: Map<string, Record<string, VariacaoPorGrupo>>;
}) {
  return (
    <div className="rounded-lg border overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Grupo</TableHead>
            <TableHead className="text-right">Valor Bruto</TableHead>
            <TableHead className="text-right text-red-500">Taxa Não Contratada</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {Array.from(data.entries()).map(([grupo, vpg]) => {
            return (
              <TableRow key={grupo}>
                <TableCell className="font-medium">{grupo || '—'}</TableCell>
                <TableCell className="text-right">
                  <div>{vpg.valorBruto ? fmt(vpg.valorBruto.valorPrincipal) : '—'}</div>
                  {vpg.valorBruto && <VariacaoBadge variacao={vpg.valorBruto} />}
                </TableCell>
                <TableCell className="text-right">
                  <div>{vpg.totalTaxaNaoContratada ? fmt(vpg.totalTaxaNaoContratada.valorPrincipal) : '—'}</div>
                  {vpg.totalTaxaNaoContratada && <VariacaoBadge variacao={vpg.totalTaxaNaoContratada} isCusto />}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
