import { useMutation } from '@tanstack/react-query';
import { iniciarColetaGeral, iniciarColetaCliente } from '@/lib/api/coleta';

export const useIniciarColetaGeral = () =>
  useMutation({
    mutationFn: ({ dataInicio, dataFim }: { dataInicio?: string; dataFim?: string }) =>
      iniciarColetaGeral(dataInicio, dataFim),
  });

export const useIniciarColetaCliente = () =>
  useMutation({
    mutationFn: ({
      clienteId,
      dataInicio,
      dataFim,
    }: {
      clienteId: string;
      dataInicio?: string;
      dataFim?: string;
    }) => iniciarColetaCliente(clienteId, dataInicio, dataFim),
  });
