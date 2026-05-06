'use client';
import { useState } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { useTemplates, useCreateTemplate, useUpdateTemplate, useDeleteTemplate, useTemplateVariaveisGlobais } from '@/lib/hooks/useTemplates';
import { Template, TemplateVariavel } from '@/lib/types/entities';
import { TemplateRequest } from '@/lib/types/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { toast } from 'sonner';
import { Pencil, Trash2, Plus, Eye, ChevronUp, ChevronDown, X } from 'lucide-react';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

type VariavelForm = { chave: string; descricao: string };
type FormData = { nome: string; metaId?: string; conteudo: string; ativo: boolean; variaveis: VariavelForm[] };

function TemplateForm({
  defaultValues,
  onSubmit,
  onCancel,
  submitting,
  variaveisGlobais,
}: {
  defaultValues?: Partial<FormData>;
  onSubmit: (data: FormData) => void;
  onCancel: () => void;
  submitting: boolean;
  variaveisGlobais?: TemplateVariavel[];
}) {
  const { register, handleSubmit, control, formState: { errors } } = useForm<FormData>({
    defaultValues: { ativo: true, variaveis: [], ...defaultValues },
  });

  const { fields, append, remove, move } = useFieldArray({ control, name: 'variaveis' });

  const inserirVariavelGlobal = (v: TemplateVariavel) => {
    // Strip surrounding { } from the catalog chave so it matches the valores map keys
    const chave = v.chave.replace(/^\{|\}$/g, '');
    append({ chave, descricao: v.descricao });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="space-y-1">
        <Label>Nome</Label>
        <Input {...register('nome', { required: 'Nome é obrigatório' })} />
        {errors.nome && <p className="text-xs text-red-500">{errors.nome.message}</p>}
      </div>

      <div className="space-y-1">
        <Label>Meta ID do Template</Label>
        <Input {...register('metaId')} placeholder="Ex: hello_world" />
        <p className="text-xs text-muted-foreground">
          Nome do template aprovado na Meta Business (usado na API do WhatsApp).
        </p>
      </div>

      <div className="space-y-1">
        <Label>Conteúdo</Label>
        <Textarea {...register('conteudo', { required: 'Conteúdo é obrigatório' })} rows={8} />
        <p className="text-xs text-muted-foreground">
          Use os placeholders definidos nas variáveis abaixo.
        </p>
        {errors.conteudo && <p className="text-xs text-red-500">{errors.conteudo.message}</p>}
      </div>

      {/* ── Catálogo global — referência para o usuário ── */}
      {variaveisGlobais && variaveisGlobais.length > 0 && (
        <div className="rounded-md border border-dashed bg-muted/5 p-3 space-y-2">
          <p className="text-xs font-medium text-muted-foreground">
            Variáveis disponíveis — clique para inserir na lista abaixo
          </p>
          <div className="flex flex-wrap gap-1.5">
            {variaveisGlobais.map((v) => (
              <button
                key={v.id}
                type="button"
                title={v.descricao}
                onClick={() => inserirVariavelGlobal(v)}
                className="inline-flex items-center px-2 py-1 rounded text-xs font-mono bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 transition-colors"
              >
                {v.chave}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* ── Variáveis posicionais ── */}
      <div className="space-y-2 rounded-md border p-3">
        <div className="flex items-center justify-between">
          <div>
            <Label>Variáveis do Template</Label>
            <p className="text-xs text-muted-foreground mt-0.5">
              Cada variável vira um parâmetro posicional {'{{1}}'}, {'{{2}}'}, ... na Meta API.
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="gap-1 shrink-0"
            onClick={() => append({ chave: '', descricao: '' })}
          >
            <Plus className="h-3.5 w-3.5" /> Adicionar
          </Button>
        </div>

        {fields.length === 0 ? (
          <p className="text-xs text-muted-foreground italic py-1">Nenhuma variável adicionada.</p>
        ) : (
          <div className="space-y-2 pt-1">
            {/* header row */}
            <div className="grid grid-cols-[36px_1fr_1fr_80px] gap-2 px-1">
              <span className="text-xs text-muted-foreground text-center">Pos.</span>
              <span className="text-xs text-muted-foreground">Placeholder</span>
              <span className="text-xs text-muted-foreground">Descrição</span>
            </div>

            {fields.map((field, index) => (
              <div key={field.id} className="grid grid-cols-[36px_1fr_1fr_80px] items-start gap-2">
                {/* positional badge */}
                <span className="mt-2 text-center text-xs font-mono font-semibold text-blue-700 bg-blue-50 border border-blue-200 rounded px-1 py-0.5 leading-tight">
                  {`{{${index + 1}}}`}
                </span>

                {/* chave */}
                <div>
                  <Input
                    {...register(`variaveis.${index}.chave`, { required: 'Obrigatório' })}
                    placeholder="{nomeDaVariavel}"
                    className="font-mono text-sm h-8"
                  />
                  {errors.variaveis?.[index]?.chave && (
                    <p className="text-xs text-red-500 mt-0.5">{errors.variaveis[index]?.chave?.message}</p>
                  )}
                </div>

                {/* descricao */}
                <div>
                  <Input
                    {...register(`variaveis.${index}.descricao`, { required: 'Obrigatório' })}
                    placeholder="Descrição do parâmetro"
                    className="text-sm h-8"
                  />
                  {errors.variaveis?.[index]?.descricao && (
                    <p className="text-xs text-red-500 mt-0.5">{errors.variaveis[index]?.descricao?.message}</p>
                  )}
                </div>

                {/* reorder + remove */}
                <div className="flex gap-0.5 justify-end">
                  <Button
                    type="button" variant="ghost" size="icon"
                    className="h-8 w-8"
                    onClick={() => move(index, index - 1)}
                    disabled={index === 0}
                  >
                    <ChevronUp className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    type="button" variant="ghost" size="icon"
                    className="h-8 w-8"
                    onClick={() => move(index, index + 1)}
                    disabled={index === fields.length - 1}
                  >
                    <ChevronDown className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    type="button" variant="ghost" size="icon"
                    className="h-8 w-8 text-red-500 hover:text-red-700"
                    onClick={() => remove(index)}
                  >
                    <X className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex items-center gap-2">
        <input type="checkbox" id="ativo" {...register('ativo')} className="h-4 w-4" />
        <Label htmlFor="ativo">Ativo</Label>
      </div>

      <div className="flex gap-2 justify-end">
        <Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button>
        <Button type="submit" disabled={submitting}>{submitting ? 'Salvando...' : 'Salvar'}</Button>
      </div>
    </form>
  );
}

function PreviewModal({ template, onClose }: { template: Template; onClose: () => void }) {
  const variaveis = [...template.variaveis].sort((a, b) => a.ordem - b.ordem);

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{template.nome}</DialogTitle>
        </DialogHeader>

        {template.metaId && (
          <p className="text-xs text-muted-foreground">
            Meta ID: <span className="font-mono text-foreground">{template.metaId}</span>
          </p>
        )}

        <pre className="text-sm whitespace-pre-wrap font-sans bg-muted/30 rounded p-3">
          {template.conteudo}
        </pre>

        {variaveis.length > 0 && (
          <div className="space-y-2">
            <p className="text-xs font-medium text-muted-foreground">
              Parâmetros posicionais ({variaveis.length}):
            </p>
            <div className="divide-y rounded-md border text-sm">
              {variaveis.map((v) => (
                <div key={v.id} className="flex items-center gap-3 px-3 py-2">
                  <span className="font-mono font-semibold text-blue-700 bg-blue-50 border border-blue-200 rounded px-1.5 py-0.5 text-xs whitespace-nowrap">
                    {`{{${v.ordem}}}`}
                  </span>
                  <span className="font-mono text-foreground">{v.chave}</span>
                  <span className="text-muted-foreground text-xs">— {v.descricao}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

export function TemplatesTab() {
  const { data: templates, isLoading } = useTemplates();
  const { data: variaveisGlobais } = useTemplateVariaveisGlobais();
  const { mutate: criar, isPending: criando } = useCreateTemplate();
  const { mutate: atualizar, isPending: atualizando } = useUpdateTemplate();
  const { mutate: excluir } = useDeleteTemplate();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Template | null>(null);
  const [previewTarget, setPreviewTarget] = useState<Template | null>(null);

  const openCreate = () => { setEditTarget(null); setDialogOpen(true); };
  const openEdit = (t: Template) => { setEditTarget(t); setDialogOpen(true); };
  const closeDialog = () => { setDialogOpen(false); setEditTarget(null); };

  const handleSubmit = (data: FormData) => {
    const request: TemplateRequest = {
      nome: data.nome,
      metaId: data.metaId || undefined,
      conteudo: data.conteudo,
      ativo: data.ativo,
      variaveis: data.variaveis.map((v, i) => ({
        chave: v.chave,
        descricao: v.descricao,
        ordem: i + 1,
      })),
    };

    if (editTarget) {
      atualizar({ id: editTarget.id, data: request }, {
        onSuccess: () => { toast.success('Template atualizado'); closeDialog(); },
        onError: () => toast.error('Erro ao atualizar template'),
      });
    } else {
      criar(request, {
        onSuccess: () => { toast.success('Template criado'); closeDialog(); },
        onError: () => toast.error('Erro ao criar template'),
      });
    }
  };

  const handleExcluir = (t: Template) => {
    if (!confirm(`Excluir template "${t.nome}"?`)) return;
    excluir(t.id, {
      onSuccess: () => toast.success('Template excluído'),
      onError: () => toast.error('Erro ao excluir template'),
    });
  };

  const editDefaultValues = (t: Template): Partial<FormData> => ({
    nome: t.nome,
    metaId: t.metaId,
    conteudo: t.conteudo,
    ativo: t.ativo,
    variaveis: [...t.variaveis]
      .sort((a, b) => a.ordem - b.ordem)
      .map(v => ({ chave: v.chave, descricao: v.descricao })),
  });

  if (isLoading) return <p className="text-sm text-muted-foreground">Carregando...</p>;

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button onClick={openCreate} size="sm" className="gap-2">
          <Plus className="h-4 w-4" /> Novo Template
        </Button>
      </div>

      {!templates?.length ? (
        <p className="text-sm text-muted-foreground">Nenhum template cadastrado.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nome</TableHead>
              <TableHead>Meta ID</TableHead>
              <TableHead>Variáveis</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Criado em</TableHead>
              <TableHead className="text-right">Ações</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {templates.map((t) => (
              <TableRow key={t.id}>
                <TableCell className="font-medium">{t.nome}</TableCell>
                <TableCell className="text-sm font-mono text-muted-foreground">{t.metaId ?? '—'}</TableCell>
                <TableCell>
                  {t.variaveis.length === 0 ? (
                    <span className="text-xs text-muted-foreground">—</span>
                  ) : (
                    <div className="flex flex-wrap gap-1">
                      {[...t.variaveis]
                        .sort((a, b) => a.ordem - b.ordem)
                        .map((v) => (
                          <span
                            key={v.id}
                            className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-xs font-mono bg-blue-50 text-blue-700 border border-blue-200"
                          >
                            <span className="text-blue-400">{`{{${v.ordem}}}`}</span>
                            {v.chave}
                          </span>
                        ))}
                    </div>
                  )}
                </TableCell>
                <TableCell>
                  <Badge variant={t.ativo ? 'default' : 'secondary'}>
                    {t.ativo ? 'Ativo' : 'Inativo'}
                  </Badge>
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {format(new Date(t.createdAt), 'dd/MM/yyyy', { locale: ptBR })}
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex gap-2 justify-end">
                    <Button variant="ghost" size="sm" onClick={() => setPreviewTarget(t)}>
                      <Eye className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => openEdit(t)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="sm" onClick={() => handleExcluir(t)} className="text-red-500 hover:text-red-700">
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Dialog open={dialogOpen} onOpenChange={closeDialog}>
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editTarget ? 'Editar Template' : 'Novo Template'}</DialogTitle>
          </DialogHeader>
          <TemplateForm
            defaultValues={editTarget ? editDefaultValues(editTarget) : undefined}
            onSubmit={handleSubmit}
            onCancel={closeDialog}
            submitting={criando || atualizando}
            variaveisGlobais={variaveisGlobais}
          />
        </DialogContent>
      </Dialog>

      {previewTarget && (
        <PreviewModal template={previewTarget} onClose={() => setPreviewTarget(null)} />
      )}
    </div>
  );
}
