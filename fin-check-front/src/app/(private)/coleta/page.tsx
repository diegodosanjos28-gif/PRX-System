'use client';
import { useState } from 'react';
import { useIniciarColetaGeral, useIniciarColetaCliente } from '@/lib/hooks/useColeta';
import { useClientes } from '@/lib/hooks/useClientes';
import { DateRangePicker } from '@/components/shared/DateRangePicker';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { toast } from 'sonner';
import { CheckCircle, RefreshCw, Globe, User } from 'lucide-react';

type Modo = 'geral' | 'cliente';

interface Feedback {
  mensagem: string;
  timestamp: string;
}

export default function ColetaPage() {
  const [modo, setModo] = useState<Modo>('geral');
  const [clienteId, setClienteId] = useState('');
  const [range, setRange] = useState({ dataInicio: '', dataFim: '' });
  const [feedback, setFeedback] = useState<Feedback | null>(null);

  const { data: clientes } = useClientes();
  const { mutate: coletarGeral, isPending: coletandoGeral } = useIniciarColetaGeral();
  const { mutate: coletarCliente, isPending: coletandoCliente } = useIniciarColetaCliente();

  const isPending = coletandoGeral || coletandoCliente;

  const handleColetar = () => {
    setFeedback(null);
    const datas = {
      dataInicio: range.dataInicio || undefined,
      dataFim: range.dataFim || undefined,
    };

    if (modo === 'geral') {
      coletarGeral(datas, {
        onSuccess: (res) => {
          setFeedback({ mensagem: res.mensagem, timestamp: res.timestamp });
          toast.success('Coleta iniciada com sucesso');
        },
        onError: () => toast.error('Erro ao iniciar coleta. Verifique se o microserviço está ativo.'),
      });
    } else {
      if (!clienteId) {
        toast.error('Selecione um cliente');
        return;
      }
      coletarCliente({ clienteId, ...datas }, {
        onSuccess: (res) => {
          setFeedback({ mensagem: res.mensagem, timestamp: res.timestamp });
          toast.success('Coleta iniciada com sucesso');
        },
        onError: () => toast.error('Erro ao iniciar coleta. Verifique se o microserviço está ativo.'),
      });
    }
  };

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-2xl font-bold">Coleta Manual</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Dispara a coleta de dados do Conciflex para um cliente ou para todos os clientes ativos.
          A coleta é executada em background — acompanhe os Logs de Coleta para verificar o resultado.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Configuração</CardTitle>
          <CardDescription>Escolha o modo de coleta e o período desejado.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          {/* Modo */}
          <div className="space-y-2">
            <Label>Modo</Label>
            <RadioGroup
              value={modo}
              onValueChange={(v) => { setModo(v as Modo); setClienteId(''); }}
              className="flex gap-6"
            >
              <div className="flex items-center gap-2">
                <RadioGroupItem value="geral" id="modo-geral" />
                <Label htmlFor="modo-geral" className="flex items-center gap-1.5 cursor-pointer">
                  <Globe className="h-4 w-4 text-muted-foreground" />
                  Geral — todos os clientes
                </Label>
              </div>
              <div className="flex items-center gap-2">
                <RadioGroupItem value="cliente" id="modo-cliente" />
                <Label htmlFor="modo-cliente" className="flex items-center gap-1.5 cursor-pointer">
                  <User className="h-4 w-4 text-muted-foreground" />
                  Por cliente
                </Label>
              </div>
            </RadioGroup>
          </div>

          {/* Seletor de cliente */}
          {modo === 'cliente' && (
            <div className="space-y-1">
              <Label>Cliente</Label>
              <Select value={clienteId} onValueChange={setClienteId}>
                <SelectTrigger>
                  <SelectValue placeholder="Selecione um cliente" />
                </SelectTrigger>
                <SelectContent>
                  {clientes?.filter((c) => c.ativo).map((c) => (
                    <SelectItem key={c.id} value={c.id}>{c.razaoSocial}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {/* Período (opcional) */}
          <div className="space-y-1">
            <Label>
              Período{' '}
              <span className="text-muted-foreground font-normal">(opcional — padrão: D-1)</span>
            </Label>
            <DateRangePicker
              dataInicio={range.dataInicio}
              dataFim={range.dataFim}
              onChange={setRange}
            />
          </div>

          <Button
            onClick={handleColetar}
            disabled={isPending || (modo === 'cliente' && !clienteId)}
            className="gap-2"
          >
            {isPending ? (
              <RefreshCw className="h-4 w-4 animate-spin" />
            ) : (
              <RefreshCw className="h-4 w-4" />
            )}
            {isPending ? 'Aguardando resposta...' : 'Iniciar Coleta'}
          </Button>
        </CardContent>
      </Card>

      {feedback && (
        <Card className="border-green-200 bg-green-50">
          <CardContent className="pt-4 flex items-start gap-3">
            <CheckCircle className="h-5 w-5 text-green-600 mt-0.5 shrink-0" />
            <div className="space-y-1">
              <p className="font-medium text-green-800">{feedback.mensagem}</p>
              <p className="text-xs text-green-600">
                Iniciado em {new Date(feedback.timestamp).toLocaleString('pt-BR')}
              </p>
              <p className="text-xs text-green-700 mt-2">
                A coleta está rodando em background no microserviço.
                Acesse <strong>Logs de Coleta</strong> para acompanhar o andamento.
              </p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
