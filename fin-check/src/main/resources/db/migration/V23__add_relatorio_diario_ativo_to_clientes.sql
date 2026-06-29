-- Adiciona controle individual de relatório diário por WhatsApp no cadastro do cliente.
-- DEFAULT true garante que clientes existentes continuem recebendo relatórios sem necessidade
-- de atualização manual de cada registro.

ALTER TABLE clientes
    ADD COLUMN relatorio_diario_ativo BOOLEAN NOT NULL DEFAULT true;
