CREATE TABLE users (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    login      VARCHAR(100) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'ADM',
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Usuário ADM padrão — substitua a senha em produção via variável de ambiente ou migration separada.
-- O hash abaixo corresponde à senha: admin123 (BCrypt com força 10)
-- Para gerar um novo hash: htpasswd -bnBC 10 "" <senha> | tr -d ':\n' | sed 's/$2y/$2a/'
INSERT INTO users (login, senha_hash, role)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADM');
