-- Registra exatamente quando uma demanda foi concluída.
-- Preenchido pelo backend apenas na transição false→true; limpo na transição true→false.
-- NULL nas demandas já concluídas antes desta migration: comportamento intencional.

ALTER TABLE implantacao_demandas
    ADD COLUMN concluida_em TIMESTAMP NULL;
