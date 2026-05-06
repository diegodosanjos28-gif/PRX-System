'use client';
import { Grid3x3, LayoutDashboard } from 'lucide-react';
import { Button } from '@/components/ui/button';

export type ViewMode = 'grid' | 'dashboard';

interface Props {
  view: ViewMode;
  onChange: (view: ViewMode) => void;
}

export function ViewToggle({ view, onChange }: Props) {
  return (
    <div className="flex items-center gap-1 rounded-md border p-1">
      <Button
        variant={view === 'grid' ? 'default' : 'ghost'}
        size="sm"
        onClick={() => onChange('grid')}
        className="gap-1.5"
      >
        <Grid3x3 className="h-4 w-4" />
        Tabela
      </Button>
      <Button
        variant={view === 'dashboard' ? 'default' : 'ghost'}
        size="sm"
        onClick={() => onChange('dashboard')}
        className="gap-1.5"
      >
        <LayoutDashboard className="h-4 w-4" />
        Dashboard
      </Button>
    </div>
  );
}
