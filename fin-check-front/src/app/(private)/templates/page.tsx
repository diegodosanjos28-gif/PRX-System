'use client';
import { useState } from 'react';
import { TemplatesTab } from '@/components/templates/TemplatesTab';
import { VariaveisTab } from '@/components/templates/VariaveisTab';
import { cn } from '@/lib/utils';

type Tab = 'templates' | 'variaveis';

export default function TemplatesPage() {
  const [activeTab, setActiveTab] = useState<Tab>('templates');

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Templates & Variáveis</h1>

      {/* Tab bar */}
      <div className="border-b">
        <nav className="-mb-px flex gap-6">
          {([
            { key: 'templates', label: 'Templates' },
            { key: 'variaveis', label: 'Variáveis' },
          ] as { key: Tab; label: string }[]).map(({ key, label }) => (
            <button
              key={key}
              onClick={() => setActiveTab(key)}
              className={cn(
                'pb-3 text-sm font-medium border-b-2 transition-colors',
                activeTab === key
                  ? 'border-foreground text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              )}
            >
              {label}
            </button>
          ))}
        </nav>
      </div>

      {activeTab === 'templates' && <TemplatesTab />}
      {activeTab === 'variaveis' && <VariaveisTab />}
    </div>
  );
}
