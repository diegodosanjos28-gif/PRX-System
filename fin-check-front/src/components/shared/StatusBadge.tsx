import { Badge } from '@/components/ui/badge';

type Variant = 'success' | 'error' | 'warning' | 'info' | 'neutral';

const variantClasses: Record<Variant, string> = {
  success: 'bg-green-100 text-green-800 border-green-200',
  error: 'bg-red-100 text-red-800 border-red-200',
  warning: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  info: 'bg-blue-100 text-blue-800 border-blue-200',
  neutral: 'bg-gray-100 text-gray-800 border-gray-200',
};

interface Props { label: string; variant: Variant; }
export function StatusBadge({ label, variant }: Props) {
  return <Badge className={variantClasses[variant]} variant="outline">{label}</Badge>;
}
