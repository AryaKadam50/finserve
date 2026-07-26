#!/usr/bin/env bash
# ============================================================
# FinServe — Server Setup Script
# ============================================================
# Run this AFTER provisioning the EC2 instance.
# It SSHes into the instance and installs Java 17, MySQL 8,
# and configures the database.
#
# Usage:
#   chmod +x deploy/setup-server.sh
#   ./deploy/setup-server.sh <EC2_PUBLIC_IP>
# ============================================================

set -euo pipefail

# ---------- Configuration ----------
EC2_IP="${1:?Usage: ./deploy/setup-server.sh <EC2_PUBLIC_IP>}"
KEY_FILE="${KEY_NAME:-finserve-key}.pem"
SSH_USER="ec2-user"
DB_NAME="finserve_db"
DB_USER="finserve_user"
DB_PASS="${DB_PASSWORD:-finserve123}"

echo "=========================================="
echo " FinServe — Server Setup"
echo "=========================================="
echo " Target: ${SSH_USER}@${EC2_IP}"
echo ""

# ---------- Run setup commands on the remote server ----------
ssh -i "$KEY_FILE" -o StrictHostKeyChecking=no "${SSH_USER}@${EC2_IP}" bash -s << REMOTE_SCRIPT
set -e

echo ">>> Updating system packages..."
sudo dnf update -y

echo ">>> Installing Java 17 (Amazon Corretto)..."
sudo dnf install -y java-17-amazon-corretto-devel
java -version

echo ">>> Installing MySQL (MariaDB) Server..."
sudo dnf install -y mariadb105-server
sudo systemctl start mariadb
sudo systemctl enable mariadb

echo ">>> Configuring MySQL database and user..."
sudo mysql -u root << SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME}
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost'
  IDENTIFIED BY '${DB_PASS}';

ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';

GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;

SHOW DATABASES LIKE '${DB_NAME}';
SQL

echo ">>> MySQL setup complete."

echo ">>> Creating application directory..."
sudo mkdir -p /opt/finserve
sudo chown \$USER:\$USER /opt/finserve

echo ">>> Creating environment configuration..."
cat > /opt/finserve/.env << ENV
DB_HOST=localhost
DB_PORT=3306
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USER}
DB_PASSWORD=${DB_PASS}
FRONTEND_URL=*
ENV

echo ""
echo "=========================================="
echo " SERVER SETUP COMPLETE"
echo "=========================================="
echo " Java:  \$(java -version 2>&1 | head -1)"
echo " MySQL: \$(mysql --version)"
echo " Service mariadb: \$(systemctl is-active mariadb)"
echo " App dir: /opt/finserve"
echo "=========================================="

REMOTE_SCRIPT

echo ""
echo "Next step: ./deploy/deploy-backend.sh ${EC2_IP}"
