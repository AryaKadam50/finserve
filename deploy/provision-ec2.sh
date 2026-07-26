#!/usr/bin/env bash
# ============================================================
# FinServe — Provision EC2 Instance
# ============================================================
# Prerequisites:
#   - AWS CLI v2 installed and configured (aws configure)
#   - A valid AWS key pair, or this script will create one
#
# Usage:
#   chmod +x deploy/provision-ec2.sh
#   ./deploy/provision-ec2.sh
# ============================================================

set -euo pipefail

# ---------- Configuration ----------
REGION="${AWS_REGION:-ap-south-1}"
INSTANCE_TYPE="t3.micro"
AMI_ID=""  # Will be auto-detected below
KEY_NAME="${KEY_NAME:-finserve-key}"
SG_NAME="finserve-sg"
TAG_NAME="FinServe-Server"

echo "=========================================="
echo " FinServe — EC2 Provisioning Script"
echo "=========================================="
echo "Region:        $REGION"
echo "Instance Type: $INSTANCE_TYPE"
echo ""

# ---------- 1. Get latest Amazon Linux 2023 AMI ----------
echo "[1/5] Finding latest Amazon Linux 2023 AMI..."
AMI_ID=$(aws ec2 describe-images \
    --region "$REGION" \
    --owners amazon \
    --filters \
        "Name=name,Values=al2023-ami-2023.*-x86_64" \
        "Name=state,Values=available" \
    --query 'sort_by(Images, &CreationDate)[-1].ImageId' \
    --output text)

echo "       AMI: $AMI_ID"

# ---------- 2. Create key pair (if it doesn't exist) ----------
echo "[2/5] Checking key pair '$KEY_NAME'..."
if ! aws ec2 describe-key-pairs --key-names "$KEY_NAME" --region "$REGION" &>/dev/null; then
    echo "       Creating key pair..."
    aws ec2 create-key-pair \
        --key-name "$KEY_NAME" \
        --region "$REGION" \
        --query 'KeyMaterial' \
        --output text > "${KEY_NAME}.pem"
    chmod 400 "${KEY_NAME}.pem"
    echo "       Key saved to ${KEY_NAME}.pem — KEEP THIS SAFE!"
else
    echo "       Key pair already exists."
fi

# ---------- 3. Create security group ----------
echo "[3/5] Creating security group '$SG_NAME'..."
VPC_ID=$(aws ec2 describe-vpcs \
    --region "$REGION" \
    --filters "Name=isDefault,Values=true" \
    --query 'Vpcs[0].VpcId' \
    --output text)

SG_ID=$(aws ec2 create-security-group \
    --group-name "$SG_NAME" \
    --description "FinServe application security group" \
    --vpc-id "$VPC_ID" \
    --region "$REGION" \
    --query 'GroupId' \
    --output text 2>/dev/null || \
    aws ec2 describe-security-groups \
        --group-names "$SG_NAME" \
        --region "$REGION" \
        --query 'SecurityGroups[0].GroupId' \
        --output text)

echo "       Security Group: $SG_ID"

# Inbound rules
echo "       Adding inbound rules..."
# SSH (port 22) — from anywhere (restrict in production)
aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" --protocol tcp --port 22 --cidr 0.0.0.0/0 \
    --region "$REGION" 2>/dev/null || true

# HTTP (port 80) — Nginx
aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" --protocol tcp --port 80 --cidr 0.0.0.0/0 \
    --region "$REGION" 2>/dev/null || true

# HTTPS (port 443)
aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" --protocol tcp --port 443 --cidr 0.0.0.0/0 \
    --region "$REGION" 2>/dev/null || true

# Spring Boot (port 8080) — from anywhere (for testing; restrict in production)
aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" --protocol tcp --port 8080 --cidr 0.0.0.0/0 \
    --region "$REGION" 2>/dev/null || true

# MySQL (port 3306) — ONLY from this security group (not public!)
aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" --protocol tcp --port 3306 \
    --source-group "$SG_ID" \
    --region "$REGION" 2>/dev/null || true

echo "       ✓ Rules: SSH(22), HTTP(80), HTTPS(443), App(8080), MySQL(3306-internal)"

# ---------- 4. Launch EC2 instance ----------
echo "[4/5] Launching EC2 instance..."
INSTANCE_ID=$(aws ec2 run-instances \
    --image-id "$AMI_ID" \
    --instance-type "$INSTANCE_TYPE" \
    --key-name "$KEY_NAME" \
    --security-group-ids "$SG_ID" \
    --region "$REGION" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$TAG_NAME}]" \
    --query 'Instances[0].InstanceId' \
    --output text)

echo "       Instance ID: $INSTANCE_ID"
echo "       Waiting for instance to be running..."
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID" --region "$REGION"

# ---------- 5. Get public IP ----------
PUBLIC_IP=$(aws ec2 describe-instances \
    --instance-ids "$INSTANCE_ID" \
    --region "$REGION" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text)

echo "[5/5] Instance is running!"
echo ""
echo "=========================================="
echo " PROVISIONING COMPLETE"
echo "=========================================="
echo " Instance ID:  $INSTANCE_ID"
echo " Public IP:    $PUBLIC_IP"
echo " Key Pair:     $KEY_NAME"
echo " Security Grp: $SG_ID ($SG_NAME)"
echo ""
echo " SSH command:"
echo "   ssh -i ${KEY_NAME}.pem ec2-user@${PUBLIC_IP}"
echo ""
echo " Next step:"
echo "   ./deploy/setup-server.sh ${PUBLIC_IP}"
echo "=========================================="

# Save instance info for other scripts
cat > deploy/.instance-info <<EOF
INSTANCE_ID=$INSTANCE_ID
PUBLIC_IP=$PUBLIC_IP
SG_ID=$SG_ID
KEY_NAME=$KEY_NAME
REGION=$REGION
EOF

echo "Instance info saved to deploy/.instance-info"
