#!/bin/bash
# Script para backup automático do banco de dados PostgreSQL

set -e

# Configurações
BACKUP_DIR="/backups"
POSTGRES_DB=${POSTGRES_DB:-helps_db}
POSTGRES_USER=${POSTGRES_USER:-helps}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
POSTGRES_HOST="postgres"
POSTGRES_PORT=5432
DATE=$(date +"%Y-%m-%d_%H-%M-%S")
BACKUP_FILE="${BACKUP_DIR}/${POSTGRES_DB}_${DATE}.sql.gz"

# Garantir que o diretório de backup existe
mkdir -p ${BACKUP_DIR}

# Limpar backups antigos (manter apenas os últimos 7)
echo "Limpando backups antigos..."
ls -tp ${BACKUP_DIR}/*.sql.gz 2>/dev/null | grep -v '/$' | tail -n +8 | xargs -I {} rm -- {} || true

# Executar o backup
echo "Iniciando backup do banco ${POSTGRES_DB}..."
export PGPASSWORD="${POSTGRES_PASSWORD}"

pg_dump -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U ${POSTGRES_USER} ${POSTGRES_DB} | gzip > ${BACKUP_FILE}

# Verificar o resultado
if [ $? -eq 0 ]; then
    echo "Backup concluído com sucesso: ${BACKUP_FILE}"
    # Definir permissões para o arquivo de backup
    chmod 640 ${BACKUP_FILE}

    # Informações sobre o tamanho do backup
    BACKUP_SIZE=$(du -h ${BACKUP_FILE} | cut -f1)
    echo "Tamanho do backup: ${BACKUP_SIZE}"
else
    echo "Erro ao criar backup do banco de dados!"
    exit 1
fi

# Registrar data do backup
echo "Backup criado em: $(date)" >> ${BACKUP_DIR}/backup_history.log

echo "Processo de backup finalizado!"