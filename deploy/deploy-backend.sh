#!/usr/bin/env bash
# ============================================================
# FinServe — Deploy Backend Script
# ============================================================
# Builds the Spring Boot JAR locally, copies it to EC2,
# and sets up a systemd service.
#
# Usage:
#   chmod +x deploy/deploy-backend.sh
#   ./deploy/deploy-backend.sh <EC2_PUBLIC_IP>
# ============================================================

set -euo pipefail

# ---------- Configuration ----------
EC2_IP="${1:?Usage: ./deploy/deploy-backend.sh <EC2_PUBLIC_IP>}"
KEY_FILE="${KEY_NAME:-finserve-key}.pem"
SSH_USER="ec2-user"
JAR_NAME="finserve-backend-0.0.1-SNAPSHOT.jar"
REMOTE_DIR="/opt/finserve"

echo "=========================================="
echo " FinServe — Backend Deployment"
echo "=========================================="

# ---------- 1. Build the JAR ----------
echo "[1/4] Building Spring Boot JAR..."
cd finserve-backend
if command -v mvn &> /dev/null; then
    mvn clean package -DskipTests
elif [ -f "./mvnw" ]; then
    ./mvnw clean package -DskipTests
else
    echo "ERROR: Neither 'mvn' nor './mvnw' found. Please install Maven."
    exit 1
fi
cd ..

JAR_PATH="finserve-backend/target/$JAR_NAME"
if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: JAR not found at $JAR_PATH"
    exit 1
fi
echo "       JAR built: $JAR_PATH ($(du -h "$JAR_PATH" | cut -f1))"

# ---------- 2. Copy JAR to EC2 ----------
echo "[2/4] Uploading JAR to EC2..."
scp -i "$KEY_FILE" -o StrictHostKeyChecking=no \
    "$JAR_PATH" "${SSH_USER}@${EC2_IP}:${REMOTE_DIR}/${JAR_NAME}"
echo "       ✓ Uploaded to ${REMOTE_DIR}/${JAR_NAME}"

# ---------- 3. Copy systemd service file ----------
echo "[3/4] Installing systemd service..."
scp -i "$KEY_FILE" -o StrictHostKeyChecking=no \
    deploy/finserve.service "${SSH_USER}@${EC2_IP}:/tmp/finserve.service"

ssh -i "$KEY_FILE" -o StrictHostKeyChecking=no "${SSH_USER}@${EC2_IP}" << 'REMOTE'
    sudo mv /tmp/finserve.service /etc/systemd/system/finserve-backend.service
    sudo systemctl daemon-reload
    sudo systemctl enable finserve-backend
    sudo systemctl restart finserve-backend
    echo "       ✓ Service installed and started"
REMOTE

# ---------- 4. Verify ----------
echo "[4/4] Verifying deployment..."
sleep 8
ssh -i "$KEY_FILE" -o StrictHostKeyChecking=no "${SSH_USER}@${EC2_IP}" << 'REMOTE'
    echo "=== Service Status ==="
    sudo systemctl status finserve-backend --no-pager | head -20

    echo "=== API Endpoint Verification ==="
    curl -s http://localhost:8080/api/loans || echo "API starting up, retrying..."
REMOTE

echo ""
echo "=========================================="
echo " BACKEND DEPLOYMENT COMPLETE"
echo "=========================================="
echo " API URL: http://${EC2_IP}:8080/api"
echo "=========================================="
