-- Script de criacao da tabela de mensagens (H2).
CREATE TABLE IF NOT EXISTS mensagem (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    remetente VARCHAR(30)  NOT NULL,
    destino   VARCHAR(50)  NOT NULL DEFAULT '#geral',
    conteudo  VARCHAR(500) NOT NULL,
    horario   TIMESTAMP    NOT NULL
);

ALTER TABLE mensagem ADD COLUMN IF NOT EXISTS destino VARCHAR(50) NOT NULL DEFAULT '#geral';

CREATE INDEX IF NOT EXISTS idx_mensagem_horario ON mensagem(horario);
CREATE INDEX IF NOT EXISTS idx_mensagem_destino ON mensagem(destino);
