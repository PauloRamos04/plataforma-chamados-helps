-- Adição de suporte a anexos
-- Versão: 2
-- Descrição: Implementa o esquema para anexos de chamados e mensagens

-- Tabela de anexos
CREATE TABLE anexos (
    id BIGSERIAL PRIMARY KEY,
    file_id VARCHAR(100) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    chamado_id BIGINT,
    mensagem_id BIGINT,
    usuario_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_image BOOLEAN,
    description VARCHAR(500),
    CONSTRAINT fk_anexos_chamado FOREIGN KEY (chamado_id) REFERENCES chamados (id) ON DELETE CASCADE,
    CONSTRAINT fk_anexos_mensagem FOREIGN KEY (mensagem_id) REFERENCES mensagens (id) ON DELETE CASCADE,
    CONSTRAINT fk_anexos_usuario FOREIGN KEY (usuario_id) REFERENCES users (id),
    CONSTRAINT check_anexo_ref CHECK (
        (chamado_id IS NOT NULL AND mensagem_id IS NULL) OR
        (chamado_id IS NULL AND mensagem_id IS NOT NULL)
    )
);

-- Tabela de notificações
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    chamado_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_chamado FOREIGN KEY (chamado_id) REFERENCES chamados (id) ON DELETE CASCADE
);

-- Índices para melhorar performance
CREATE INDEX idx_anexos_file_id ON anexos(file_id);
CREATE INDEX idx_anexos_chamado ON anexos(chamado_id);
CREATE INDEX idx_anexos_mensagem ON anexos(mensagem_id);
CREATE INDEX idx_anexos_usuario ON anexos(usuario_id);
CREATE INDEX idx_anexos_is_image ON anexos(is_image);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(read);
CREATE INDEX idx_notifications_chamado ON notifications(chamado_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);