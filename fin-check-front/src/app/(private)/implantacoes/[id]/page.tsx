'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import {
  ArrowLeft,
  ArrowRight,
  ChevronLeft,
  User,
  Clock,
  Calendar,
  Flag,
  CheckCircle2,
  Tag,
  FileText,
} from 'lucide-react';
import { toast } from 'sonner';
import { useImplantacao, useUpdateImplantacao } from '@/lib/hooks/useImplantacoes';
import { DemandaSection } from '@/components/implantacoes/DemandaSection';
import {
  ImplantacaoChecklist,
  DEFAULT_CHECKLIST,
} from '@/components/implantacoes/ImplantacaoChecklist';
import { ImplantacaoHistorico } from '@/components/implantacoes/ImplantacaoHistorico';
import { LoadingSpinner } from '@/components/shared/LoadingSpinner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Separator } from '@/components/ui/separator';
import { cn } from '@/lib/utils';

// ─── Configurações visuais ────────────────────────────────────────────────────

const ETAPA_CONFIG: Record<string, { label: string; className: string }> = {
  pre:        { label: 'Pré-Largada', className: 'bg-blue-500/10 text-blue-700 border-blue-200'     },
  corrida:    { label: 'Corrida',     className: 'bg-orange-500/10 text-orange-700 border-orange-200' },
  onboarding: { label: 'Onboarding',  className: 'bg-purple-500/10 text-purple-700 border-purple-200' },
  curral:     { label: 'Curral',      className: 'bg-gray-500/10 text-gray-500 border-gray-200'      },
};

const STATUS_CONFIG: Record<string, { label: string; className: string; bar: string }> = {
  fluindo:    { label: 'Fluindo',    className: 'bg-green-500/10 text-green-700 border-green-200',   bar: 'bg-green-500'  },
  aguardando: { label: 'Aguardando', className: 'bg-yellow-500/10 text-yellow-700 border-yellow-200', bar: 'bg-yellow-400' },
  travado:    { label: 'Travado',    className: 'bg-red-500/10 text-red-700 border-red-200',         bar: 'bg-red-500'    },
};

const NEXT_ETAPA: Record<string, string> = {
  pre:        'corrida',
  corrida:    'onboarding',
  onboarding: 'curral',
};

const PREV_ETAPA: Record<string, string> = {
  corrida:    'pre',
  onboarding: 'corrida',
  curral:     'onboarding',
};

// ─── Helpers ──────────────────────────────────────────────────────────────────

function fmtDate(val: string | null | undefined) {
  if (!val) return '—';
  return new Date(val).toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

function fmtDateTime(val: string | null | undefined) {
  if (!val) return '—';
  return new Date(val).toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

// ─── InfoCard ─────────────────────────────────────────────────────────────────

interface InfoCardProps {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  children: React.ReactNode;
}

function InfoCard({ icon: Icon, label, children }: InfoCardProps) {
  return (
    <div className="rounded-lg border bg-white p-3.5 space-y-1.5">
      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <Icon className="h-3.5 w-3.5 flex-shrink-0" />
        <span>{label}</span>
      </div>
      <div className="text-sm font-medium leading-snug">{children}</div>
    </div>
  );
}

// ─── Página ───────────────────────────────────────────────────────────────────

export default function ImplantacaoDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: impl, isLoading } = useImplantacao(id);
  const { mutate: updateImpl, isPending: isUpdating } = useUpdateImplantacao();
  const [confirmingRetreat, setConfirmingRetreat] = useState(false);

  if (isLoading) return <LoadingSpinner />;
  if (!impl) return <p className="text-muted-foreground">Implantação não encontrada.</p>;

  const toggleChecklistItem = (index: number) => {
    const rawItems = impl.progressJson;
    if (!Array.isArray(rawItems) || rawItems.length === 0) return;

    const updated = (rawItems as { label: string; concluido: boolean }[]).map((item, i) =>
      i === index ? { ...item, concluido: !item.concluido } : item,
    );

    updateImpl(
      {
        id: impl.id,
        data: {
          clienteId:   impl.clienteId,
          etapa:       impl.etapa,
          status:      impl.status ?? null,
          responsavel: impl.responsavel ?? undefined,
          donoContato: impl.donoContato ?? undefined,
          adquirentes: impl.adquirentes ?? undefined,
          observacoes: impl.observacoes ?? undefined,
          progressJson: updated,
        },
      },
      { onError: () => toast.error('Erro ao salvar checklist') },
    );
  };

  // ── Avanço de etapa ───────────────────────────────────────────────────────
  const checklistItems = Array.isArray(impl.progressJson)
    ? (impl.progressJson as { concluido: boolean }[])
    : [];
  const allDone   = checklistItems.length > 0 && checklistItems.every((item) => item.concluido);
  const nextEtapa = NEXT_ETAPA[impl.etapa];

  const advanceEtapa = () => {
    if (!nextEtapa) return;
    const labels = DEFAULT_CHECKLIST[nextEtapa] ?? [];
    // [] para etapas sem checklist (curral) — garante que o backend limpa o anterior
    const newProgressJson = labels.length > 0
      ? labels.map((label) => ({ label, concluido: false }))
      : null;

    updateImpl(
      {
        id: impl.id,
        data: {
          clienteId:       impl.clienteId,
          etapa:           nextEtapa,
          status:          nextEtapa === 'curral' ? null : 'fluindo',
          responsavel:     impl.responsavel ?? undefined,
          donoContato:     impl.donoContato ?? undefined,
          adquirentes:     impl.adquirentes ?? undefined,
          observacoes:     impl.observacoes ?? undefined,
          progressJson:    newProgressJson,
          etapaIniciadaEm: new Date().toISOString().replace('Z', ''),
        },
      },
      {
        onSuccess: () => toast.success(`Avançado para ${ETAPA_CONFIG[nextEtapa]?.label ?? nextEtapa}`),
        onError:   () => toast.error('Erro ao avançar etapa'),
      },
    );
  };

  // ── Retroceder etapa ─────────────────────────────────────────────────────
  const prevEtapa = PREV_ETAPA[impl.etapa];

  const retreatEtapa = () => {
    if (!prevEtapa) return;
    const labels = DEFAULT_CHECKLIST[prevEtapa] ?? [];
    const newProgressJson = labels.length > 0
      ? labels.map((label) => ({ label, concluido: false }))
      : null;

    updateImpl(
      {
        id: impl.id,
        data: {
          clienteId:       impl.clienteId,
          etapa:           prevEtapa,
          status:          'fluindo',
          responsavel:     impl.responsavel ?? undefined,
          donoContato:     impl.donoContato ?? undefined,
          adquirentes:     impl.adquirentes ?? undefined,
          observacoes:     impl.observacoes ?? undefined,
          progressJson:    newProgressJson,
          etapaIniciadaEm: new Date().toISOString().replace('Z', ''),
        },
      },
      {
        onSuccess: () => {
          setConfirmingRetreat(false);
          toast.success(`Retrocedido para ${ETAPA_CONFIG[prevEtapa]?.label ?? prevEtapa}`);
        },
        onError: () => toast.error('Erro ao retroceder etapa'),
      },
    );
  };

  const etapa  = ETAPA_CONFIG[impl.etapa]  ?? { label: impl.etapa,   className: '', bar: '' };
  const status = impl.status
    ? (STATUS_CONFIG[impl.status] ?? { label: impl.status, className: '', bar: '' })
    : null;

  const adquirentes = Array.isArray(impl.adquirentes) && impl.adquirentes.length > 0
    ? impl.adquirentes
    : null;

  const isTravado = impl.status === 'travado';

  return (
    <div className="space-y-5 anim-fade-in-up">

      {/* ── 1. Cabeçalho ────────────────────────────────────────────── */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          {/* Nome + badges inline */}
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-bold leading-tight">
              {impl.clienteRazaoSocial}
            </h1>
            <Badge variant="outline" className={etapa.className}>
              {etapa.label}
            </Badge>
            {status && (
              <Badge variant="outline" className={status.className}>
                {status.label}
              </Badge>
            )}
          </div>

          {/* Nome fantasia */}
          {impl.clienteNomeFantasia && (
            <p className="mt-0.5 text-muted-foreground text-sm">
              {impl.clienteNomeFantasia}
            </p>
          )}

          {/* Responsável no sub-cabeçalho */}
          {impl.responsavel && (
            <p className="mt-1 flex items-center gap-1.5 text-sm text-muted-foreground">
              <User className="h-3.5 w-3.5 flex-shrink-0" />
              {impl.responsavel}
            </p>
          )}
        </div>

        {/* Botões de ação */}
        <div className="flex flex-wrap items-center gap-2 flex-shrink-0">
          {/* Voltar etapa — discreto, só aparece se há etapa anterior */}
          {prevEtapa && (
            <Button
              variant="ghost"
              size="sm"
              className="text-xs text-muted-foreground hover:text-orange-700"
              onClick={() => setConfirmingRetreat(true)}
            >
              <ChevronLeft className="h-3.5 w-3.5 mr-0.5" />
              Voltar etapa
            </Button>
          )}

          <Button variant="outline" size="sm" asChild>
            <Link href={`/implantacoes/${id}/editar`}>Editar</Link>
          </Button>
          <Button variant="outline" size="sm" asChild>
            <Link href="/implantacoes" className="flex items-center gap-1">
              <ArrowLeft className="h-3.5 w-3.5" />
              Voltar
            </Link>
          </Button>
        </div>
      </div>

      {/* Banner de alerta para travado */}
      {isTravado && (
        <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-4 py-2.5 text-sm text-red-700">
          <CheckCircle2 className="h-4 w-4 flex-shrink-0" />
          <span>Esta implantação está <strong>travada</strong>. Verifique as demandas abertas e tome uma ação.</span>
        </div>
      )}

      {/* ── 2. Cards de resumo ──────────────────────────────────────── */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        <InfoCard icon={Flag} label="Etapa">
          <Badge variant="outline" className={cn('text-xs', etapa.className)}>
            {etapa.label}
          </Badge>
        </InfoCard>

        <InfoCard icon={CheckCircle2} label="Status">
          {status ? (
            <Badge variant="outline" className={cn('text-xs', status.className)}>
              {status.label}
            </Badge>
          ) : (
            <span className="text-muted-foreground">—</span>
          )}
        </InfoCard>

        <InfoCard icon={User} label="Responsável">
          {impl.responsavel ?? <span className="text-muted-foreground">—</span>}
        </InfoCard>

        <InfoCard icon={User} label="Dono do Contato">
          {impl.donoContato ?? <span className="text-muted-foreground">—</span>}
        </InfoCard>

        <InfoCard icon={Clock} label="Último Movimento">
          {fmtDateTime(impl.ultimoMovimento)}
        </InfoCard>

        <InfoCard icon={Calendar} label="Início da Etapa">
          {fmtDate(impl.etapaIniciadaEm)}
        </InfoCard>

        {impl.dataEntradaCurral && (
          <InfoCard icon={Calendar} label="Entrada no Curral">
            {fmtDate(impl.dataEntradaCurral)}
          </InfoCard>
        )}
      </div>

      {/* ── 3. Adquirentes ──────────────────────────────────────────── */}
      {adquirentes && (
        <div className="rounded-lg border bg-white p-4">
          <div className="mb-2.5 flex items-center gap-1.5 text-sm font-semibold text-gray-700">
            <Tag className="h-4 w-4 text-muted-foreground flex-shrink-0" />
            Adquirentes
          </div>
          <div className="flex flex-wrap gap-1.5">
            {adquirentes.map((a) => (
              <Badge key={a} variant="secondary" className="text-xs font-medium">
                {a}
              </Badge>
            ))}
          </div>
        </div>
      )}

      {/* ── 4. Observações ──────────────────────────────────────────── */}
      {impl.observacoes && (
        <div className="rounded-lg border border-amber-200 bg-amber-50/60 p-4">
          <div className="mb-1.5 flex items-center gap-1.5 text-sm font-semibold text-amber-800">
            <FileText className="h-4 w-4 flex-shrink-0" />
            Observações
          </div>
          <p className="text-sm text-amber-900 whitespace-pre-wrap leading-relaxed">
            {impl.observacoes}
          </p>
        </div>
      )}

      {/* ── 5. Checklist Operacional ────────────────────────────────── */}
      <ImplantacaoChecklist
        progressJson={impl.progressJson}
        etapa={impl.etapa}
        onToggle={toggleChecklistItem}
        isPending={isUpdating}
      />

      {/* ── 5b. Sugestão de avanço de etapa ─────────────────────────── */}
      {allDone && impl.etapa !== 'curral' && nextEtapa && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-green-200 bg-green-50 px-4 py-3 anim-fade-in">
          <div>
            <p className="text-sm font-semibold text-green-800">
              🏁 Etapa concluída!
            </p>
            <p className="mt-0.5 text-xs text-green-700">
              Deseja avançar para{' '}
              <strong>{ETAPA_CONFIG[nextEtapa]?.label ?? nextEtapa}</strong>?
            </p>
          </div>
          <Button
            size="sm"
            className="bg-green-600 hover:bg-green-700 text-white flex-shrink-0"
            onClick={advanceEtapa}
            disabled={isUpdating}
          >
            <ArrowRight className="mr-1.5 h-3.5 w-3.5" />
            {isUpdating ? 'Avançando…' : 'Avançar Etapa'}
          </Button>
        </div>
      )}

      <Separator />

      {/* ── 6. Demandas ─────────────────────────────────────────────── */}
      <DemandaSection
        implantacaoId={impl.id}
        demandas={impl.demandas ?? []}
      />

      <Separator />

      {/* ── 7. Histórico Operacional ─────────────────────────────────── */}
      <ImplantacaoHistorico impl={impl} />

      {/* ── Dialog: Confirmar retrocesso de etapa ────────────────────── */}
      {prevEtapa && (
        <Dialog open={confirmingRetreat} onOpenChange={(open) => !open && setConfirmingRetreat(false)}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>
                Retroceder para {ETAPA_CONFIG[prevEtapa]?.label ?? prevEtapa}?
              </DialogTitle>
              <DialogDescription>
                O checklist atual será substituído pelo checklist padrão da etapa anterior.
                Esta ação não pode ser desfeita.
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setConfirmingRetreat(false)}
                disabled={isUpdating}
              >
                Cancelar
              </Button>
              <Button
                variant="destructive"
                onClick={retreatEtapa}
                disabled={isUpdating}
              >
                {isUpdating ? 'Retrocedendo…' : 'Confirmar retrocesso'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}
