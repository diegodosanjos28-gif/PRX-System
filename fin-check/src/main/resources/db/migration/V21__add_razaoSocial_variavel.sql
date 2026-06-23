-- Adiciona {razaoSocial} ao catálogo de variáveis de sistema.
-- {cliente} passa a usar Nome Fantasia (com fallback para Razão Social).
-- {razaoSocial} sempre retorna o campo Razão Social sem fallback.

INSERT INTO template_variaveis (chave, descricao, sistema_fixo, ordem) VALUES
    ('{razaoSocial}', 'Razão Social do cliente (sempre o campo Razão Social, sem fallback)', true, 39);

UPDATE template_variaveis
SET descricao = 'Nome Fantasia do cliente (usa Razão Social como fallback se não preenchido)'
WHERE chave = '{cliente}' AND sistema_fixo = true;
