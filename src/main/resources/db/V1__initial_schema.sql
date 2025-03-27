-- Criação do esquema inicial da base de dados
-- Versão: 1
-- Descrição: Criação das tabelas base do sistema Helps

-- Tabela de perfis/roles
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Tabela de usuários
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de relacionamento usuários-perfis
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- Tabela de chamados
CREATE TABLE chamados (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    data_abertura TIMESTAMP NOT NULL,
    data_inicio TIMESTAMP,
    data_fechamento TIMESTAMP,
    usuario_id BIGINT,
    helper_id BIGINT,
    CONSTRAINT fk_chamados_usuario FOREIGN KEY (usuario_id) REFERENCES users (id),
    CONSTRAINT fk_chamados_helper FOREIGN KEY (helper_id) REFERENCES users (id)
);

-- Tabela de mensagens
CREATE TABLE mensagens (
    id BIGSERIAL PRIMARY KEY,
    chamado_id BIGINT NOT NULL,
    remetente_id BIGINT NOT NULL,
    conteudo TEXT NOT NULL,
    data_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mensagens_chamado FOREIGN KEY (chamado_id) REFERENCES chamados (id) ON DELETE CASCADE,
    CONSTRAINT fk_mensagens_remetente FOREIGN KEY (remetente_id) REFERENCES users (id)
);

-- Índices para melhorar performance
CREATE INDEX idx_chamados_usuario ON chamados(usuario_id);
CREATE INDEX idx_chamados_helper ON chamados(helper_id);
CREATE INDEX idx_chamados_status ON chamados(status);
CREATE INDEX idx_mensagens_chamado ON mensagens(chamado_id);
CREATE INDEX idx_mensagens_remetente ON mensagens(remetente_id);
CREATE INDEX idx_mensagens_data ON mensagens(data_envio);

-- Inserir perfis iniciais
INSERT INTO roles (name) VALUES
('ADMIN'),
('OPERADOR'),
('HELPER'),
('USUARIO');

-- Inserir usuário administrador padrão (senha: admin123)
INSERT INTO users (username, password, name, enabled)
VALUES ('admin', '$2a$10$gJH8m2BjpNQzBPo5W/XY4.KwecjklsP55CdJkg0wLecmG0jPLwYBe', 'Administrador', true);

-- Atribuir perfil de ADMIN ao usuário administrador
INSERT INTO user_roles (user_id, role_id)
VALUES (1, (SELECT id FROM roles WHERE name = 'ADMIN'));