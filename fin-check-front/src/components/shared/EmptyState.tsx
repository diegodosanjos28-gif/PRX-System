import { InboxIcon } from 'lucide-react';

interface Props { message?: string; }
export function EmptyState({ message = 'Nenhum registro encontrado' }: Props) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-muted-foreground gap-2">
      <InboxIcon className="h-10 w-10" />
      <p className="text-sm">{message}</p>
    </div>
  );
}
