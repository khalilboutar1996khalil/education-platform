#!/usr/bin/env bash
# Daily backup of the production stack: database dump + uploaded files, kept for 14 days.
#
#   ./deploy/backup.sh                      # run by hand from the project folder
#   0 3 * * * /opt/eduflow/education-platform/deploy/backup.sh >> /var/log/eduflow-backup.log 2>&1
#
# Copy the backup folder off the server from time to time: a backup on the same disk does not
# survive the loss of that disk.
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/opt/eduflow/backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"
STAMP="$(date +%Y-%m-%d_%H%M)"
COMPOSE=(docker compose -f "$PROJECT_DIR/compose.prod.yaml" --project-directory "$PROJECT_DIR")

mkdir -p "$BACKUP_DIR"

echo "[$STAMP] database..."
"${COMPOSE[@]}" exec -T postgres pg_dump -U eduflow --clean --if-exists eduflow \
    | gzip > "$BACKUP_DIR/db_$STAMP.sql.gz"

echo "[$STAMP] uploaded files..."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
"${COMPOSE[@]}" cp api:/app/var/uploads "$TMP/uploads"
tar -czf "$BACKUP_DIR/uploads_$STAMP.tar.gz" -C "$TMP" uploads

echo "[$STAMP] removing backups older than $KEEP_DAYS days..."
find "$BACKUP_DIR" -name '*.gz' -mtime +"$KEEP_DAYS" -delete

echo "[$STAMP] done: $(ls -1 "$BACKUP_DIR" | wc -l) files in $BACKUP_DIR"
