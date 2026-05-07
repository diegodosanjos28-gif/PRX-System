'use client';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { mensagemGerarSchema, MensagemGerarFormData } from '@/lib/schemas/mensagemSchema';
import { useClientes } from '@/lib/hooks/useClientes';
import { useEstabelecimentos } from '@/lib/hooks/useEstabelecimentos';
import {
  useGerarMensagem,
  useGerarParaTodos,
  useEnviarMensagem,
  useMensagensEnviadas,
} from '@/lib/hooks/useMensagens';
import { useTemplatesAtivos } from '@/lib/hooks/useTemplates';
import { DateRangePicker } from '@/components/shared/DateRangePicker';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Separator } from '@/components/ui/separator';
import { Skeleton } from '@/components/ui/skeleton';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { toast } from 'sonner';
import { useState, useRef, useEffect } from 'react';
import {
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock,
  Copy,
  Eye,
  KeyRound,
  Pencil,
  Users,
  TrendingUp,
  TrendingDown,
  Wallet,
  ReceiptText,
  BadgeDollarSign,
} from 'lucide-react';
import { Input } from '@/components/ui/input';
import { MensagemEnviada, MensagemGeradaResponse } from '@/lib/types/entities';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

// ─── constants ───────────────────────────────────────────────────────────────

const MAX_CHARS = 4096;

const PLACEHOLDER_LABELS: Record<string, string> = {
  nomeFantasia:    'Nome fantasia do cliente',
  dataInicio:      'Data de início do período',
  dataFim:         'Data de fim do período',
  estabelecimento: 'Nome do estabelecimento',
  totalTransacoes: 'Total de transações no período',
  cobradoAMais:    'Valor cobrado a mais (R$)',
  cobradoAMenos:   'Valor cobrado a menos (R$)',
  totalRecebido:   'Total recebido no período (R$)',
  totalDescontado: 'Total descontado em taxas (R$)',
};

const STATUS_BADGE: Record<string, string> = {
  sent:      'bg-gray-100 text-gray-700',
  delivered: 'bg-blue-100 text-blue-700',
  read:      'bg-green-100 text-green-700',
  failed:    'bg-red-100 text-red-700',
};
const STATUS_LABEL: Record<string, string> = {
  sent: 'Enviado', delivered: 'Entregue', read: 'Lido', failed: 'Falhou',
};

// ─── helpers ─────────────────────────────────────────────────────────────────

function fmt(n?: number | null) {
  if (n == null) return 'R$ 0,00';
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function maskPhone(phone: string): string {
  if (!phone) return '—';
  const d = phone.replace(/\D/g, '');
  if (d.length < 8) return phone;
  const last4 = d.slice(-4);
  const cc = d.slice(0, 2);
  const ddd = d.slice(2, 4);
  return `+${cc} ${ddd} *****-${last4}`;
}

// ─── interfaces ──────────────────────────────────────────────────────────────

interface ResultadoItem {
  estabelecimentoId: string;
  descricao: string;
  resultado: MensagemGeradaResponse;
  conteudo: string;
  templateParametros?: Record<string, string>;
}

// ─── sub-components ──────────────────────────────────────────────────────────

function TemplatePlaceholderText({ content }: { content: string }) {
  const parts = content.split(/(\{[^}]+\})/g);
  return (
    <span className="text-xs font-sans text-muted-foreground whitespace-pre-wrap leading-relaxed">
      {parts.map((part, i) => {
        const m = part.match(/^\{([^}]+)\}$/);
        if (m) {
          const label = PLACEHOLDER_LABELS[m[1]] ?? m[1];
          return (
            <span
              key={i}
              title={label}
              className="inline-flex items-center px-1.5 py-0.5 rounded font-mono cursor-help mx-0.5 text-white"
              style={{ background: '#3b82f6', fontSize: '0.78em', verticalAlign: 'middle' }}
            >
              {part}
            </span>
          );
        }
        return <span key={i}>{part}</span>;
      })}
    </span>
  );
}

function StepIndicator({ step }: { step: 1 | 2 | 3 }) {
  const labels = ['Configurar', 'Revisar mensagem', 'Enviado'];
  return (
    <div className="flex items-center mb-6">
      {labels.map((label, idx) => {
        const s = (idx + 1) as 1 | 2 | 3;
        const done   = s < step;
        const active = s === step;
        return (
          <div key={s} className="flex items-center flex-1 last:flex-none">
            <div className={`flex items-center gap-2 ${done ? 'text-green-600' : active ? 'text-blue-600' : 'text-muted-foreground'}`}>
              <div className={`h-6 w-6 rounded-full flex items-center justify-center text-xs font-bold border-2 flex-shrink-0 ${
                done   ? 'bg-green-100 border-green-500' :
                active ? 'bg-blue-100 border-blue-500'  :
                         'bg-muted border-muted-foreground/30'
              }`}>
                {done ? <Check className="h-3 w-3" /> : s}
              </div>
              <span className="text-xs font-medium whitespace-nowrap">{label}</span>
            </div>
            {s < 3 && (
              <div className={`flex-1 h-0.5 mx-2 ${s < step ? 'bg-green-400' : 'bg-muted-foreground/20'}`} />
            )}
          </div>
        );
      })}
    </div>
  );
}

function MensagemEditor({
  conteudo,
  pulsing,
  onChange,
}: {
  conteudo: string;
  pulsing: boolean;
  onChange: (v: string) => void;
}) {
  const taRef = useRef<HTMLTextAreaElement>(null);
  const [copied, setCopied] = useState(false);

  const count = conteudo.length;
  const countColor = count > 4000 ? 'text-red-500' : count > 3500 ? 'text-amber-500' : 'text-muted-foreground';

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(conteudo);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      toast.error('Não foi possível copiar');
    }
  };

  const handlePencil = () => {
    const ta = taRef.current;
    if (!ta) return;
    ta.focus();
    ta.setSelectionRange(ta.value.length, ta.value.length);
  };

  return (
    <div className="space-y-1">
      <div className="relative">
        <div className="absolute top-2 right-2 flex items-center gap-1 z-10">
          <button
            type="button"
            title="Copiar mensagem"
            onClick={handleCopy}
            className="inline-flex items-center gap-1 px-2 py-1 rounded text-xs border bg-background hover:bg-muted transition-colors text-muted-foreground hover:text-foreground"
          >
            {copied ? <Check className="h-3 w-3 text-green-600" /> : <Copy className="h-3 w-3" />}
            {copied ? 'Copiado' : 'Copiar'}
          </button>
          <button
            type="button"
            title="Editar mensagem"
            onClick={handlePencil}
            className="inline-flex items-center justify-center h-7 w-7 rounded border bg-background hover:bg-muted transition-colors text-muted-foreground hover:text-foreground"
          >
            <Pencil className="h-3 w-3" />
          </button>
        </div>
        <Textarea
          ref={taRef}
          value={conteudo}
          onChange={(e) => onChange(e.target.value)}
          rows={8}
          className={`pr-28 pt-2 resize-y transition-all duration-700 ${
            pulsing ? 'ring-2 ring-blue-400 ring-offset-1' : ''
          }`}
        />
      </div>
      <p className={`text-xs text-right ${countColor}`}>
        {count.toLocaleString('pt-BR')} / {MAX_CHARS.toLocaleString('pt-BR')} caracteres
      </p>
    </div>
  );
}

// ── Metric card used in the inline dashboard ──────────────────────────────────

type MetricVariant = 'default' | 'danger' | 'warning' | 'success' | 'accent';

function MetricCard({
  label,
  value,
  icon: Icon,
  variant = 'default',
}: {
  label: string;
  value: string | number;
  icon?: React.ElementType;
  variant?: MetricVariant;
}) {
  const wrapperCls: Record<MetricVariant, string> = {
    default: 'bg-card border',
    danger:  'bg-red-50 border-red-200',
    warning: 'bg-amber-50 border-amber-200',
    success: 'bg-emerald-50 border-emerald-200',
    accent:  'bg-orange-50 border-orange-200',
  };
  const valueCls: Record<MetricVariant, string> = {
    default: 'text-foreground',
    danger:  'text-red-700',
    warning: 'text-amber-700',
    success: 'text-emerald-700',
    accent:  'text-orange-700',
  };
  const iconCls: Record<MetricVariant, string> = {
    default: 'text-muted-foreground',
    danger:  'text-red-400',
    warning: 'text-amber-400',
    success: 'text-emerald-400',
    accent:  'text-orange-400',
  };
  return (
    <div className={`rounded-xl border p-3 flex flex-col gap-1.5 ${wrapperCls[variant]}`}>
      <div className="flex items-center justify-between">
        <p className="text-xs text-muted-foreground leading-tight">{label}</p>
        {Icon && <Icon className={`h-3.5 w-3.5 ${iconCls[variant]}`} />}
      </div>
      <p className={`text-base font-bold leading-none ${valueCls[variant]}`}>{value}</p>
    </div>
  );
}

// ── Full inline dashboard for a single resultado ───────────────────────────────

function ResultadoDashboard({
  item,
  enviando,
  pulsing,
  onEnviar,
  onChangeConteudo,
}: {
  item: ResultadoItem;
  enviando: boolean;
  pulsing: boolean;
  onEnviar: (item: ResultadoItem) => void;
  onChangeConteudo: (estId: string, v: string) => void;
}) {
  const { resumoAuditoria, resumoRecebimento } = item.resultado;
  const [detalhesOpen, setDetalhesOpen] = useState(false);

  const hasAuditBandeiras  = (resumoAuditoria?.porBandeira?.length ?? 0) > 0;
  const hasRecebBandeiras  = (resumoRecebimento?.porBandeira?.length ?? 0) > 0;

  return (
    <div className="space-y-4">

      {/* ── KPI row 1: auditoria ── */}
      <div className="grid grid-cols-3 gap-2">
        <MetricCard
          label="Transações"
          value={(resumoAuditoria?.totalTransacoes ?? 0).toLocaleString('pt-BR')}
          icon={ReceiptText}
          variant="default"
        />
        <MetricCard
          label="Cobrado a Mais"
          value={fmt(resumoAuditoria?.totalCobradoAMais)}
          icon={TrendingUp}
          variant="danger"
        />
        <MetricCard
          label="Cobrado a Menos"
          value={fmt(resumoAuditoria?.totalCobradoAMenos)}
          icon={TrendingDown}
          variant="warning"
        />
      </div>

      {/* ── KPI row 2: recebimentos ── */}
      <div className="grid grid-cols-2 gap-2">
        <MetricCard
          label="Total Recebido"
          value={fmt(resumoRecebimento?.totalRecebido)}
          icon={Wallet}
          variant="success"
        />
        <MetricCard
          label="Total em Taxas"
          value={fmt(resumoRecebimento?.totalDescontado)}
          icon={BadgeDollarSign}
          variant="accent"
        />
      </div>

      {/* ── Bandeira breakdowns (collapsible) ── */}
      {(hasAuditBandeiras || hasRecebBandeiras) && (
        <div className="rounded-xl border overflow-hidden">
          <button
            type="button"
            onClick={() => setDetalhesOpen((v) => !v)}
            className="w-full flex items-center justify-between px-4 py-2.5 text-sm font-medium bg-muted/30 hover:bg-muted/50 transition-colors"
          >
            <span>Detalhes por Bandeira</span>
            {detalhesOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          </button>

          {detalhesOpen && (
            <div className="p-4 space-y-5">

              {/* Auditoria por bandeira */}
              {hasAuditBandeiras && (
                <div className="space-y-1.5">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">Auditoria de Taxas</p>
                  <div className="rounded-lg border text-xs overflow-hidden">
                    <table className="w-full">
                      <thead className="bg-muted/40">
                        <tr>
                          <th className="text-left px-3 py-2 font-medium">Bandeira</th>
                          <th className="text-right px-3 py-2 font-medium">Qtd</th>
                          <th className="text-right px-3 py-2 font-medium">Diferença Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        {resumoAuditoria.porBandeira.map((b) => (
                          <tr key={b.bandeira} className="border-t">
                            <td className="px-3 py-2">{b.bandeira}</td>
                            <td className="text-right px-3 py-2 tabular-nums">{b.quantidade}</td>
                            <td className={`text-right px-3 py-2 font-semibold tabular-nums ${
                              b.diferencaTotal > 0 ? 'text-red-600' : b.diferencaTotal < 0 ? 'text-emerald-600' : 'text-muted-foreground'
                            }`}>
                              {fmt(b.diferencaTotal)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Recebimentos por bandeira */}
              {hasRecebBandeiras && (
                <div className="space-y-1.5">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">Recebimentos</p>
                  <div className="rounded-lg border text-xs overflow-x-auto">
                    <table className="w-full min-w-[420px]">
                      <thead className="bg-muted/40">
                        <tr>
                          <th className="text-left px-3 py-2 font-medium">Bandeira</th>
                          <th className="text-right px-3 py-2 font-medium">Qtd</th>
                          <th className="text-right px-3 py-2 font-medium">Bruto</th>
                          <th className="text-right px-3 py-2 font-medium">Taxas</th>
                          <th className="text-right px-3 py-2 font-medium">Líquido</th>
                        </tr>
                      </thead>
                      <tbody>
                        {resumoRecebimento.porBandeira.map((b) => (
                          <tr key={b.bandeira} className="border-t">
                            <td className="px-3 py-2">{b.bandeira}</td>
                            <td className="text-right px-3 py-2 tabular-nums">{b.quantidade}</td>
                            <td className="text-right px-3 py-2 tabular-nums">{fmt(b.totalBruto)}</td>
                            <td className="text-right px-3 py-2 tabular-nums text-orange-600">{fmt(b.totalTaxa)}</td>
                            <td className="text-right px-3 py-2 tabular-nums text-emerald-600 font-semibold">{fmt(b.totalLiquido)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      <Separator />

      {/* ── Message editor ── */}
      <div className="space-y-1.5">
        <Label>Mensagem — revise antes de enviar</Label>
        <MensagemEditor
          conteudo={item.conteudo}
          pulsing={pulsing}
          onChange={(v) => onChangeConteudo(item.estabelecimentoId, v)}
        />
      </div>

      <Button
        onClick={() => onEnviar(item)}
        disabled={enviando || !item.conteudo}
        className="w-full"
        size="lg"
      >
        {enviando ? 'Enviando...' : `Enviar para ${item.descricao} via WhatsApp`}
      </Button>
    </div>
  );
}

// ─── main component ───────────────────────────────────────────────────────────

export function MensagemGerarForm() {
  const [resultados,      setResultados]      = useState<ResultadoItem[]>([]);
  const [selectedEstId,   setSelectedEstId]   = useState<string | null>(null);
  const [previewOpen,     setPreviewOpen]     = useState(true);
  const [conteudoModal,   setConteudoModal]   = useState<MensagemEnviada | null>(null);
  const [step,            setStep]            = useState<1 | 2 | 3>(1);
  const [pulsingId,       setPulsingId]       = useState<string | null>(null);
  const [bulkPreview,     setBulkPreview]     = useState(false);
  const [metaToken,       setMetaToken]       = useState('');
  const [tokenVisible,    setTokenVisible]    = useState(false);

  const resultsRef = useRef<HTMLDivElement>(null);

  const { data: clientes }        = useClientes();
  const { data: templatesAtivos } = useTemplatesAtivos();

  const { control, handleSubmit, watch, setValue, formState: { errors } } = useForm<MensagemGerarFormData>({
    resolver: zodResolver(mensagemGerarSchema),
    defaultValues: { enviarParaTodos: false, modo: 'template', estabelecimentoIds: [] },
  });

  const enviarParaTodos    = watch('enviarParaTodos');
  const clienteId          = watch('clienteId');
  const modo               = watch('modo');
  const templateId         = watch('templateId');
  const estabelecimentoIds = watch('estabelecimentoIds') ?? [];
  const dataInicio         = watch('dataInicio');
  const dataFim            = watch('dataFim');

  const { data: estabelecimentos } = useEstabelecimentos(clienteId ?? '');
  const { mutateAsync: gerar,  isPending: gerando }       = useGerarMensagem();
  const { mutate: gerarTodos,  isPending: gerandoTodos }  = useGerarParaTodos();
  const { mutate: enviar,      isPending: enviando }       = useEnviarMensagem();

  const selectedTemplate = templatesAtivos?.find((t) => t.id === templateId);
  const ativos           = estabelecimentos?.filter((e) => e.ativo) ?? [];
  const activeClients    = clientes?.filter((c) => c.ativo) ?? [];

  const singleEstId = !enviarParaTodos && estabelecimentoIds.length === 1
    ? estabelecimentoIds[0] : '';
  const { data: logPage, isLoading: logLoading } = useMensagensEnviadas(singleEstId);

  const selectedItem = resultados.find((r) => r.estabelecimentoId === selectedEstId) ?? null;

  useEffect(() => { if (templateId) setPreviewOpen(true); }, [templateId]);
  useEffect(() => { if (!enviarParaTodos) setBulkPreview(false); }, [enviarParaTodos]);

  const isFormValid = (() => {
    if (!dataInicio || !dataFim) return false;
    if (modo === 'template' && !templateId) return false;
    if (!enviarParaTodos) {
      if (!clienteId) return false;
      if (estabelecimentoIds.length === 0) return false;
    }
    return true;
  })();

  const toggleEstabelecimento = (id: string) => {
    const cur = estabelecimentoIds;
    setValue(
      'estabelecimentoIds',
      cur.includes(id) ? cur.filter((x) => x !== id) : [...cur, id],
      { shouldValidate: true },
    );
  };

  const updateConteudo = (estId: string, conteudo: string) =>
    setResultados((prev) => prev.map((r) => r.estabelecimentoId === estId ? { ...r, conteudo } : r));

  // ── handlers ──

  const onGerar = async (data: MensagemGerarFormData) => {
    if (data.enviarParaTodos) {
      gerarTodos(
        { dataInicio: data.dataInicio, dataFim: data.dataFim, modo: data.modo, templateId: data.templateId, metaAccessToken: metaToken || undefined },
        {
          onSuccess: (res) => {
            setBulkPreview(false);
            if (res.erros === 0) {
              toast.success(`Mensagens enviadas! Total: ${res.enviados}/${res.total}`);
            } else {
              toast.warning(`Enviadas: ${res.enviados}/${res.total}. Falhas: ${res.erros}.`);
            }
          },
          onError: () => toast.error('Erro ao enviar mensagens em lote'),
        },
      );
      return;
    }

    setResultados([]);
    setSelectedEstId(null);
    setStep(1);
    const ests = ativos.filter((e) => data.estabelecimentoIds?.includes(e.id));
    let firstId: string | null = null;

    for (const est of ests) {
      try {
        const res = await gerar({
          clienteId: data.clienteId!,
          estabelecimentoId: est.id,
          dataInicio: data.dataInicio,
          dataFim: data.dataFim,
          modo: data.modo,
          templateId: data.templateId,
        });
        setResultados((prev) => [
          ...prev,
          {
            estabelecimentoId: est.id,
            descricao: est.descricao,
            resultado: res,
            conteudo: res.mensagem,
            templateParametros: res.templateParametros,
          },
        ]);
        if (!firstId) {
          firstId = est.id;
          setSelectedEstId(firstId);
        }
      } catch {
        toast.error(`Erro ao gerar mensagem para ${est.descricao}`);
      }
    }

    if (firstId) {
      setStep(2);
      setTimeout(() => resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 100);
      setPulsingId(firstId);
      setTimeout(() => setPulsingId(null), 3000);
    }
  };

  const onEnviar = (item: ResultadoItem) => {
    enviar(
      {
        clienteId: clienteId!,
        conteudo: item.conteudo,
        estabelecimentoId: item.estabelecimentoId,
        templateId: selectedTemplate?.id,
        templateNome: selectedTemplate?.nome,
        modoGeracao: modo,
        templateParametros: item.templateParametros,
        metaAccessToken: metaToken || undefined,
      },
      {
        onSuccess: () => {
          toast.success(`Mensagem enviada para ${item.descricao}!`);
          setResultados((prev) => {
            const next = prev.filter((r) => r.estabelecimentoId !== item.estabelecimentoId);
            if (next.length === 0) {
              setStep(3);
              setSelectedEstId(null);
              setTimeout(() => setStep(1), 4000);
            } else {
              setSelectedEstId(next[0].estabelecimentoId);
            }
            return next;
          });
        },
        onError: () => toast.error('Erro ao enviar mensagem'),
      },
    );
  };

  const isPending = gerando || gerandoTodos;

  const modalCliente         = conteudoModal ? clientes?.find((c) => c.id === conteudoModal.clienteId) : null;
  const modalEstabelecimento = conteudoModal ? ativos.find((e) => e.id === conteudoModal.estabelecimentoId) : null;

  // ── render ──

  return (
    <>
      {!enviarParaTodos && <StepIndicator step={step} />}

      <div className="grid grid-cols-1 lg:grid-cols-[480px_1fr] gap-8 items-start">

        {/* ── LEFT: form + establishment selector ── */}
        <div className="space-y-6">
          <form onSubmit={handleSubmit(onGerar)} className="space-y-4">

            {/* bulk toggle */}
            <label className="flex items-center gap-3 cursor-pointer select-none p-3 rounded-lg border bg-muted/20 hover:bg-muted/30 transition-colors">
              <Controller
                control={control}
                name="enviarParaTodos"
                render={({ field }) => (
                  <input
                    type="checkbox"
                    className="h-4 w-4 rounded accent-primary"
                    checked={field.value}
                    onChange={(e) => {
                      field.onChange(e.target.checked);
                      setResultados([]);
                      setSelectedEstId(null);
                      setStep(1);
                      setValue('estabelecimentoIds', []);
                      setValue('clienteId', undefined);
                    }}
                  />
                )}
              />
              <div className="flex items-center gap-2">
                <Users className="h-4 w-4 text-muted-foreground" />
                <span className="text-sm font-medium">Enviar para todos os clientes</span>
              </div>
            </label>

            {enviarParaTodos && (
              <div className="flex items-start gap-2 px-3 py-2 rounded-md bg-amber-50 border border-amber-300 text-sm text-amber-800">
                <Users className="h-4 w-4 flex-shrink-0 mt-0.5" />
                <span>
                  Todos os clientes e estabelecimentos ativos serão processados.
                  As mensagens serão enviadas sem revisão individual.
                </span>
              </div>
            )}

            {!enviarParaTodos && (
              <>
                <div className="space-y-1">
                  <Label>Cliente</Label>
                  <Controller
                    control={control}
                    name="clienteId"
                    render={({ field }) => (
                      <Select
                        onValueChange={(val) => {
                          field.onChange(val);
                          setValue('estabelecimentoIds', []);
                          setResultados([]);
                          setSelectedEstId(null);
                          setStep(1);
                        }}
                        value={field.value ?? ''}
                      >
                        <SelectTrigger><SelectValue placeholder="Selecione um cliente" /></SelectTrigger>
                        <SelectContent>
                          {clientes?.filter((c) => c.ativo).map((c) => (
                            <SelectItem key={c.id} value={c.id}>{c.razaoSocial}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                  {errors.clienteId && <p className="text-xs text-red-500">{errors.clienteId.message}</p>}
                </div>

                {clienteId && (
                  <div className="space-y-1">
                    <Label>Estabelecimentos</Label>
                    {ativos.length === 0 ? (
                      <p className="text-sm text-muted-foreground">Nenhum estabelecimento ativo</p>
                    ) : (
                      <div className="rounded-md border divide-y max-h-48 overflow-y-auto">
                        {ativos.map((e) => (
                          <label key={e.id} className="flex items-center gap-3 px-3 py-2 cursor-pointer hover:bg-muted/50 select-none">
                            <input
                              type="checkbox"
                              className="h-4 w-4 rounded border-input accent-primary"
                              checked={estabelecimentoIds.includes(e.id)}
                              onChange={() => toggleEstabelecimento(e.id)}
                            />
                            <span className="text-sm">{e.descricao}</span>
                          </label>
                        ))}
                      </div>
                    )}
                    {errors.estabelecimentoIds && <p className="text-xs text-red-500">{errors.estabelecimentoIds.message}</p>}
                  </div>
                )}
              </>
            )}

            {/* date range */}
            <Controller control={control} name="dataInicio" render={({ field: f1 }) => (
              <Controller control={control} name="dataFim" render={({ field: f2 }) => (
                <DateRangePicker
                  dataInicio={f1.value ?? ''}
                  dataFim={f2.value ?? ''}
                  onChange={({ dataInicio, dataFim }) => { f1.onChange(dataInicio); f2.onChange(dataFim); }}
                />
              )} />
            )} />

            {/* generation mode */}
            <div className="space-y-2">
              <Label>Modo de Geração</Label>
              <Controller control={control} name="modo" render={({ field }) => (
                <RadioGroup value={field.value} onValueChange={field.onChange} className="flex gap-4">
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="template" id="modo-template" />
                    <Label htmlFor="modo-template">Template fixo</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="ia" id="modo-ia" />
                    <Label htmlFor="modo-ia">Geração por IA (Claude)</Label>
                  </div>
                </RadioGroup>
              )} />
            </div>

            {/* template selector */}
            {modo === 'template' && (
              <div className="space-y-2">
                <div className="space-y-1">
                  <Label>Template</Label>
                  <Controller
                    control={control}
                    name="templateId"
                    render={({ field }) => (
                      <Select
                        onValueChange={(val) => field.onChange(Number(val))}
                        value={field.value ? String(field.value) : ''}
                      >
                        <SelectTrigger><SelectValue placeholder="Selecione um template" /></SelectTrigger>
                        <SelectContent>
                          {templatesAtivos?.map((t) => (
                            <SelectItem key={t.id} value={String(t.id)}>{t.nome}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                  />
                </div>

                {selectedTemplate && (
                  <div className="rounded-md border overflow-hidden">
                    <button
                      type="button"
                      onClick={() => setPreviewOpen((v) => !v)}
                      className="w-full flex items-center justify-between px-3 py-2 text-sm font-medium bg-muted/30 hover:bg-muted/50 transition-colors"
                    >
                      <span>Pré-visualização do template</span>
                      {previewOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                    </button>
                    {previewOpen && (
                      <div className="p-3 space-y-2">
                        <TemplatePlaceholderText content={selectedTemplate.conteudo} />
                        {selectedTemplate.variaveis.length > 0 && (
                          <div className="flex flex-wrap gap-1 pt-2 border-t">
                            {selectedTemplate.variaveis.map((v) => (
                              <span
                                key={v.id}
                                title={PLACEHOLDER_LABELS[v.chave] ?? v.chave}
                                className="inline-flex items-center px-2 py-0.5 rounded font-mono cursor-help text-white"
                                style={{ background: '#3b82f6', fontSize: '0.78em' }}
                              >
                                {v.chave}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* bulk two-step or standard gerar */}
            {enviarParaTodos ? (
              <div className="space-y-3">
                {!bulkPreview ? (
                  <Button
                    type="button"
                    variant="outline"
                    disabled={!isFormValid || isPending}
                    onClick={() => setBulkPreview(true)}
                  >
                    <Users className="h-4 w-4 mr-2" />
                    Gerar Prévia para Todos
                  </Button>
                ) : (
                  <div className="space-y-3">
                    <div className="rounded-md border p-3 bg-muted/20 space-y-2 text-sm">
                      <p className="font-medium text-muted-foreground">
                        {activeClients.length} cliente{activeClients.length !== 1 ? 's' : ''} ativo{activeClients.length !== 1 ? 's' : ''} serão contactados no período selecionado
                      </p>
                      <div className="rounded border text-xs overflow-hidden">
                        <table className="w-full">
                          <thead className="bg-muted/50">
                            <tr>
                              <th className="text-left px-2 py-1.5 font-medium">Cliente</th>
                              <th className="text-left px-2 py-1.5 font-medium">Status</th>
                            </tr>
                          </thead>
                          <tbody>
                            {activeClients.map((c) => (
                              <tr key={c.id} className="border-t">
                                <td className="px-2 py-1.5">{c.razaoSocial}</td>
                                <td className="px-2 py-1.5">
                                  <span className="inline-flex items-center gap-1 text-green-700">
                                    <CheckCircle2 className="h-3 w-3" /> Pronto
                                  </span>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                    <div className="rounded-md border bg-muted/10 p-3 space-y-2">
                      <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                        <KeyRound className="h-4 w-4" />
                        <span>Access Token Meta</span>
                        <span className="text-xs font-normal">(opcional)</span>
                      </div>
                      <div className="relative">
                        <Input
                          type={tokenVisible ? 'text' : 'password'}
                          value={metaToken}
                          onChange={(e) => setMetaToken(e.target.value)}
                          placeholder="Usa o token configurado no servidor se vazio"
                          className="pr-20 font-mono text-xs"
                        />
                        <button
                          type="button"
                          onClick={() => setTokenVisible((v) => !v)}
                          className="absolute right-2 top-1/2 -translate-y-1/2 text-xs text-muted-foreground hover:text-foreground"
                        >
                          {tokenVisible ? 'Ocultar' : 'Mostrar'}
                        </button>
                      </div>
                    </div>
                    <Button
                      type="submit"
                      disabled={isPending}
                      className="bg-red-600 hover:bg-red-700 text-white"
                    >
                      {gerandoTodos ? 'Enviando para todos...' : 'Confirmar e Enviar para Todos'}
                    </Button>
                  </div>
                )}
              </div>
            ) : (
              <Button type="submit" disabled={isPending || !isFormValid}>
                {gerando ? 'Gerando...' : 'Gerar Mensagem'}
              </Button>
            )}
          </form>

          {/* ── Establishment selector (shown once results exist) ── */}
          {resultados.length > 0 && (
            <div ref={resultsRef} className="space-y-2 scroll-mt-4">
              <Separator />
              <div className="flex items-center justify-between">
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
                  {resultados.length} resultado{resultados.length !== 1 ? 's' : ''} gerado{resultados.length !== 1 ? 's' : ''}
                </p>
              </div>

              {/* token override */}
              <div className="rounded-md border bg-muted/10 p-3 space-y-2">
                <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                  <KeyRound className="h-4 w-4" />
                  <span>Access Token Meta</span>
                  <span className="text-xs font-normal">(opcional)</span>
                </div>
                <div className="relative">
                  <Input
                    type={tokenVisible ? 'text' : 'password'}
                    value={metaToken}
                    onChange={(e) => setMetaToken(e.target.value)}
                    placeholder="Usa o token configurado no servidor se vazio"
                    className="pr-20 font-mono text-xs"
                  />
                  <button
                    type="button"
                    onClick={() => setTokenVisible((v) => !v)}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-xs text-muted-foreground hover:text-foreground"
                  >
                    {tokenVisible ? 'Ocultar' : 'Mostrar'}
                  </button>
                </div>
              </div>

              <div className="space-y-1">
                {resultados.map((item) => {
                  const selected = selectedEstId === item.estabelecimentoId;
                  return (
                    <button
                      key={item.estabelecimentoId}
                      type="button"
                      onClick={() => setSelectedEstId(item.estabelecimentoId)}
                      className={`w-full text-left px-3 py-2.5 rounded-lg border text-sm transition-all ${
                        selected
                          ? 'border-blue-500 bg-blue-50 text-blue-800 font-medium shadow-sm'
                          : 'border-border hover:bg-muted/40 text-foreground'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="truncate">{item.descricao}</span>
                        {selected && (
                          <span className="text-xs text-blue-500 flex-shrink-0">visualizando →</span>
                        )}
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* ── RIGHT: dashboard when results exist, log panel otherwise ── */}
        <div className="space-y-3 sticky top-4">

          {resultados.length > 0 ? (
            <>
              <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wide">
                Dashboard — {selectedItem?.descricao ?? 'Selecione um estabelecimento'}
              </h3>
              {selectedItem ? (
                <ResultadoDashboard
                  item={selectedItem}
                  enviando={enviando}
                  pulsing={pulsingId === selectedItem.estabelecimentoId}
                  onEnviar={onEnviar}
                  onChangeConteudo={updateConteudo}
                />
              ) : (
                <p className="text-sm text-muted-foreground py-4">
                  Selecione um estabelecimento à esquerda para ver o dashboard.
                </p>
              )}
            </>
          ) : singleEstId ? (
            <>
              <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wide">
                Mensagens Enviadas — {ativos.find((e) => e.id === singleEstId)?.descricao}
              </h3>
              {logLoading ? (
                <div className="space-y-2">
                  {[...Array(3)].map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
                </div>
              ) : !logPage?.content?.length ? (
                <p className="text-sm text-muted-foreground py-2">
                  Nenhuma mensagem enviada para este estabelecimento.
                </p>
              ) : (
                <div className="rounded-md border text-sm overflow-x-auto">
                  <table className="w-full min-w-[340px]">
                    <thead className="bg-muted/50">
                      <tr>
                        <th className="text-left px-3 py-2 font-medium whitespace-nowrap">Data de Envio</th>
                        <th className="text-left px-3 py-2 font-medium">Template</th>
                        <th className="text-left px-3 py-2 font-medium">Modo</th>
                        <th className="text-left px-3 py-2 font-medium">Status</th>
                        <th className="w-8 px-3 py-2" />
                      </tr>
                    </thead>
                    <tbody>
                      {logPage.content.map((m) => (
                        <tr
                          key={m.id}
                          className="border-t align-middle cursor-pointer hover:bg-blue-50/60 transition-colors"
                          onClick={() => setConteudoModal(m)}
                        >
                          <td className="px-3 py-2 whitespace-nowrap text-muted-foreground">
                            {format(new Date(m.enviadoEm), 'dd/MM/yyyy HH:mm', { locale: ptBR })}
                          </td>
                          <td className="px-3 py-2 text-muted-foreground">{m.templateNome ?? '—'}</td>
                          <td className="px-3 py-2">
                            <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${
                              m.modoGeracao === 'ia' ? 'bg-purple-100 text-purple-700' : 'bg-gray-100 text-gray-700'
                            }`}>
                              {m.modoGeracao === 'ia' ? 'IA' : 'Template'}
                            </span>
                          </td>
                          <td className="px-3 py-2">
                            <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${
                              STATUS_BADGE[m.statusEntrega] ?? 'bg-gray-100 text-gray-600'
                            }`}>
                              {STATUS_LABEL[m.statusEntrega] ?? m.statusEntrega}
                            </span>
                          </td>
                          <td className="px-3 py-2">
                            <span
                              title="Ver conteúdo da mensagem"
                              className="inline-flex items-center justify-center h-6 w-6 rounded text-muted-foreground hover:text-foreground"
                            >
                              <Eye className="h-4 w-4" />
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          ) : (
            <div className="flex flex-col items-center justify-center gap-3 min-h-[200px] h-full rounded-lg border border-dashed text-muted-foreground">
              <Clock className="h-8 w-8 opacity-30" />
              <p className="text-sm text-center max-w-[200px] leading-snug">
                Selecione um único estabelecimento para ver o histórico de mensagens.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* message content modal */}
      <Dialog open={!!conteudoModal} onOpenChange={(open) => { if (!open) setConteudoModal(null); }}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Mensagem Enviada</DialogTitle>
          </DialogHeader>
          {conteudoModal && (
            <div className="space-y-3">
              <div className="text-xs text-muted-foreground space-y-1">
                <p>{format(new Date(conteudoModal.enviadoEm), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR })}</p>
                {modalCliente?.whatsapp && (
                  <p>Enviado para: <span className="font-mono">{maskPhone(modalCliente.whatsapp)}</span></p>
                )}
                {modalEstabelecimento && <p>Estabelecimento: {modalEstabelecimento.descricao}</p>}
                {conteudoModal.templateNome && <p>Template: {conteudoModal.templateNome}</p>}
              </div>
              <pre className="text-sm whitespace-pre-wrap font-sans leading-relaxed rounded-md bg-muted/40 p-3">
                {conteudoModal.conteudo}
              </pre>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
