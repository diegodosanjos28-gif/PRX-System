-- Adiciona {maiorTaxa} e {operadoraMaisCara} ao catálogo de variáveis de sistema.
-- {maiorTaxa}          → maior percentual de taxa praticada no período (todos os estabelecimentos do cliente)
-- {operadoraMaisCara}  → nome da bandeira/operadora com a maior taxa praticada

INSERT INTO template_variaveis (chave, descricao, sistema_fixo, ordem) VALUES
    ('{maiorTaxa}',         'Maior taxa percentual praticada no período (todos os estabelecimentos do cliente)', true, 40),
    ('{operadoraMaisCara}', 'Nome da bandeira com a maior taxa praticada no período',                           true, 41);
