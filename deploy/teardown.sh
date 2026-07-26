#!/usr/bin/env bash
# ============================================================
# FinServe — Teardown Script
# ============================================================
# Terminates the EC2 instance and deletes the security group
# to stop all AWS charges.
#
# Usage:
#   chmod +x deploy/teardown.sh
#   ./deploy/teardown.sh
# ============================================================

set -euo pipefail

# ---------- Load instance info ----------
if [ -f deploy/.instance-info ]; then
    source deploy/.instance-info
    echo "Loaded instance info from deploy/.instance-info"
else
    echo "ERROR: deploy/.instance-info not found."
    echo "Provide values manually:"
    read -p "  Instance ID: " INSTANCE_ID
    read -p "  Security Group ID: " SG_ID
    read -p "  Region [ap-south-1]: " REGION
    REGION="${REGION:-ap-south-1}"
fi

echo ""
echo "=========================================="
echo " FinServe — Teardown"
echo "=========================================="
echo " Instance: $INSTANCE_ID"
echo " Security Group: $SG_ID"
echo " Region: $REGION"
echo ""
read -p " Are you sure you want to destroy everything? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Aborted."
    exit 0
fi

# ---------- 1. Terminate EC2 instance ----------
echo ""
echo "[1/3] Terminating EC2 instance $INSTANCE_ID..."
aws ec2 terminate-instances \
    --instance-ids "$INSTANCE_ID" \
    --region "$REGION" \
    --query 'TerminatingInstances[0].CurrentState.Name' \
    --output text

echo "       Waiting for termination..."
aws ec2 wait instance-terminated \
    --instance-ids "$INSTANCE_ID" \
    --region "$REGION"
echo "       ✓ Instance terminated"

# ---------- 2. Delete security group ----------
echo "[2/3] Deleting security group $SG_ID..."
aws ec2 delete-security-group \
    --group-id "$SG_ID" \
    --region "$REGION"
echo "       ✓ Security group deleted"

# ---------- 3. Optionally delete key pair ----------
if [ -n "${KEY_NAME:-}" ]; then
    read -p "[3/3] Delete key pair '$KEY_NAME'? (yes/no): " DEL_KEY
    if [ "$DEL_KEY" = "yes" ]; then
        aws ec2 delete-key-pair --key-name "$KEY_NAME" --region "$REGION"
        rm -f "${KEY_NAME}.pem"
        echo "       ✓ Key pair deleted"
    else
        echo "       Key pair kept"
    fi
fi

# ---------- Cleanup ----------
rm -f deploy/.instance-info

echo ""
echo "=========================================="
echo " TEARDOWN COMPLETE"
echo "=========================================="
echo " All AWS resources have been removed."
echo " No further charges will be incurred."
echo "=========================================="
