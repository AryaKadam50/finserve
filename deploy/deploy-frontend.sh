#!/usr/bin/env bash
# ============================================================
# FinServe — Deploy Frontend Script
# ============================================================
# Builds the React app and deploys it to the EC2 instance
# via Nginx, OR provides Vercel deployment commands.
#
# Usage:
#   chmod +x deploy/deploy-frontend.sh
#   ./deploy/deploy-frontend.sh <EC2_PUBLIC_IP>
#
# For Vercel deployment instead:
#   ./deploy/deploy-frontend.sh --vercel
# ============================================================

set -euo pipefail

MODE="${1:?Usage: ./deploy/deploy-frontend.sh <EC2_PUBLIC_IP> | --vercel}"

# ---------- Build React App ----------
echo "=========================================="
echo " FinServe — Frontend Deployment"
echo "=========================================="

echo "[1/3] Building React app..."
cd finserve-frontend
npm ci
npm run build
cd ..

DIST_DIR="finserve-frontend/dist"
if [ ! -d "$DIST_DIR" ]; then
    echo "ERROR: dist directory not found"
    exit 1
fi
echo "       ✓ Build complete ($(du -sh "$DIST_DIR" | cut -f1))"

# ---------- Deploy ----------
if [ "$MODE" = "--vercel" ]; then
    # ----- Vercel deployment -----
    echo ""
    echo "[2/3] Deploying to Vercel..."
    echo ""
    echo "Run these commands manually:"
    echo ""
    echo "  cd finserve-frontend"
    echo "  npx vercel --prod"
    echo ""
    echo "Or for first-time setup:"
    echo "  npm i -g vercel"
    echo "  cd finserve-frontend"
    echo "  vercel login"
    echo "  vercel --prod"
    echo ""
    echo "Set the VITE_API_URL environment variable in Vercel to:"
    echo "  http://<EC2_PUBLIC_IP>:8080/api"
    echo ""
else
    # ----- EC2 + Nginx deployment -----
    EC2_IP="$MODE"
    KEY_FILE="${KEY_NAME:-finserve-key}.pem"
    SSH_USER="ec2-user"

    echo "[2/3] Installing Nginx on EC2..."
    ssh -i "$KEY_FILE" -o StrictHostKeyChecking=no "${SSH_USER}@${EC2_IP}" << 'REMOTE'
        sudo dnf install -y nginx
        sudo systemctl enable nginx
REMOTE

    echo "[3/3] Uploading build files and Nginx config..."

    # Copy Nginx config
    scp -i "$KEY_FILE" -o StrictHostKeyChecking=no \
        deploy/nginx.conf "${SSH_USER}@${EC2_IP}:/tmp/finserve-nginx.conf"

    # Copy build files
    tar -czf /tmp/finserve-frontend.tar.gz -C "$DIST_DIR" .
    scp -i "$KEY_FILE" -o StrictHostKeyChecking=no \
        /tmp/finserve-frontend.tar.gz "${SSH_USER}@${EC2_IP}:/tmp/"

    ssh -i "$KEY_FILE" -o StrictHostKeyChecking=no "${SSH_USER}@${EC2_IP}" << 'REMOTE'
        # Set up web root
        sudo mkdir -p /var/www/finserve
        sudo tar -xzf /tmp/finserve-frontend.tar.gz -C /var/www/finserve/
        sudo chown -R nginx:nginx /var/www/finserve

        # Install main clean Nginx configuration to avoid orphaned location blocks
        sudo tee /etc/nginx/nginx.conf > /dev/null << 'EOF'
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log notice;
pid /run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    keepalive_timeout 65;
    types_hash_max_size 4096;

    include /etc/nginx/conf.d/*.conf;
}
EOF

        # Install FinServe Nginx site config
        sudo cp /tmp/finserve-nginx.conf /etc/nginx/conf.d/finserve.conf

        # Test and restart Nginx
        sudo nginx -t
        sudo systemctl restart nginx

        echo "✓ Frontend deployed and Nginx restarted successfully"
REMOTE

    rm -f /tmp/finserve-frontend.tar.gz

    echo ""
    echo "=========================================="
    echo " FRONTEND DEPLOYMENT COMPLETE"
    echo "=========================================="
    echo " URL: http://${EC2_IP}"
    echo " API proxy: http://${EC2_IP}/api → :8080"
    echo "=========================================="
fi
