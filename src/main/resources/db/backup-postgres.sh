#!/bin/bash
# Script para backup automático do PostgreSQL
# Recomendado executar como cron job diário

set -e

# Configurações
BACKUP_DIR="/var/helps/backups"
POSTGRES_CONTAINER="helps-postgres"
POSTGRES_USER="helps"
POSTGRES_DB="helps_db"
DATE=$(date +"%Y-%m-%d_%H-%M-%S")
BACKUP_FILE="${BACKUP_DIR}/helps_db_${DATE}.sql.gz"
MAX_BACKUPS=7  # Manter apenas os últimos 7 backups

# Criar diretório de backup se não existir
mkdir -p "${BACKUP_DIR}"

echo "Iniciando backup do banco de dados ${POSTGRES_DB}..."

# Realizar backup usando Docker exec
docker exec ${POSTGRES_CONTAINER} pg_dump -U ${POSTGRES_USER} -d ${POSTGRES_DB} | gzip > "${BACKUP_FILE}"

# Verificar resultado
if [ $? -eq 0 ]; then
    echo "Backup concluído com sucesso: ${BACKUP_FILE}"

    # Definir permissões para o arquivo de backup
    chmod 640 "${BACKUP_FILE}"

    # Calcular tamanho do backup
    BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    echo "Tamanho do backup: ${BACKUP_SIZE}"

    # Limpar backups antigos
    echo "Limpando backups antigos..."
    ls -tp "${BACKUP_DIR}"/*.sql.gz | grep -v '/$' | tail -n +$((MAX_BACKUPS+1)) | xargs -I {} rm -- {} || true

    # Manter log de backups realizados
    echo "Backup criado em: $(date)" >> "${BACKUP_DIR}/backup_history.log"

    # Opcional: Copiar para armazenamento externo
    # rsync -avz "${BACKUP_FILE}" user@remote-server:/path/to/backup/
else
    echo "ERRO: Falha ao criar backup do banco de dados!"
    exit 1
fi

echo "Processo de backup finalizado!"