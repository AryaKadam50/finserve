# FinServe — AWS Deployment Guide

This document details the exact steps to deploy the FinServe application to AWS EC2 with MySQL installed directly on the instance (simplest for demo; RDS alternative noted below).

---

## Infrastructure Overview

| Resource | Value |
|----------|-------|
| **Instance Type** | t3.micro (free-tier eligible) |
| **AMI** | Amazon Linux 2023 (latest x86_64) |
| **Region** | ap-south-1 (Mumbai) — change via `$AWS_REGION` |
| **Database** | MySQL 8 installed on EC2 |
| **Web Server** | Nginx (reverse proxy + static files) |
| **Application** | Spring Boot JAR as systemd service |
| **Ports** | 22 (SSH), 80 (HTTP/Nginx), 8080 (Spring Boot), 3306 (MySQL — internal only) |

### Why MySQL on EC2 (not RDS)?

For a demo/interview project, installing MySQL directly on the EC2 instance is:
- **Faster** to set up (no extra provisioning)
- **Cheaper** (no RDS instance charges)
- **Simpler** (single server)

For production, **AWS RDS** is recommended for automated backups, replication, and managed patching. To switch, simply:
1. Create an RDS MySQL instance in the same VPC
2. Update `/opt/finserve/.env` with the RDS endpoint
3. Restart the finserve-backend service

---

## Step-by-Step Deployment

### Prerequisites

- AWS CLI v2 installed and configured (`aws configure`)
- An AWS account with EC2 permissions
- SSH client (built into macOS/Linux)

### Step 1: Provision Infrastructure

```bash
chmod +x deploy/provision-ec2.sh
./deploy/provision-ec2.sh
```

This script will:
1. Find the latest Amazon Linux 2023 AMI
2. Create (or reuse) an SSH key pair (`finserve-key.pem`)
3. Create a security group with rules for SSH, HTTP, HTTPS, Spring Boot, and MySQL
4. Launch a t3.micro EC2 instance
5. Save instance info to `deploy/.instance-info`

**Output:** You'll get the EC2 public IP and SSH command.

### Step 2: Set Up the Server

```bash
chmod +x deploy/setup-server.sh
./deploy/setup-server.sh <EC2_PUBLIC_IP>
```

This script SSHes into the instance and:
1. Updates system packages
2. Installs Java 17 (Amazon Corretto)
3. Installs and starts MySQL 8
4. Creates the `finserve_db` database and `finserve_user` MySQL user
5. Creates `/opt/finserve/` with an `.env` file

**After running:** SSH in and update the DB password in `/opt/finserve/.env`:

```bash
ssh -i finserve-key.pem ec2-user@<EC2_IP>
nano /opt/finserve/.env
```

### Step 3: Deploy the Backend

```bash
chmod +x deploy/deploy-backend.sh
./deploy/deploy-backend.sh <EC2_PUBLIC_IP>
```

This script:
1. Builds the Spring Boot JAR locally (`mvn clean package`)
2. SCPs the JAR to `/opt/finserve/` on EC2
3. Installs the systemd service file
4. Starts the service

**Verify:**

```bash
curl http://<EC2_IP>:8080/api/loans
# Should return: {"success":true,"message":"Loans retrieved successfully","data":[]}
```

**View logs:**

```bash
ssh -i finserve-key.pem ec2-user@<EC2_IP>
journalctl -u finserve-backend -f
```

### Step 4: Deploy the Frontend

**Option A: EC2 + Nginx (recommended for single-server demo)**

```bash
chmod +x deploy/deploy-frontend.sh
./deploy/deploy-frontend.sh <EC2_PUBLIC_IP>
```

This builds the React app and deploys it via Nginx. The app will be accessible at `http://<EC2_IP>` (port 80).

**Option B: Vercel (fastest, separate hosting)**

```bash
cd finserve-frontend
npm run build
npx vercel --prod
```

Set the `VITE_API_URL` environment variable in Vercel to `http://<EC2_IP>:8080/api`.

### Step 5: Update CORS (if using Vercel)

If the frontend is on a different domain (e.g., Vercel), update the CORS config:

```bash
ssh -i finserve-key.pem ec2-user@<EC2_IP>
nano /opt/finserve/.env
# Change FRONTEND_URL to your Vercel URL, e.g.:
# FRONTEND_URL=https://finserve.vercel.app
sudo systemctl restart finserve-backend
```

### Step 6: Verify End-to-End

```bash
# Test backend
curl http://<EC2_IP>:8080/api/loans/check-eligibility \
  -H "Content-Type: application/json" \
  -d '{"monthlyIncome":75000,"requestedAmount":500000,"tenure":60}'

# Test frontend (should load the React app)
curl -s http://<EC2_IP> | head -5

# Test API through Nginx proxy
curl http://<EC2_IP>/api/loans
```

---

## Security Group Rules

| Type | Protocol | Port | Source | Purpose |
|------|----------|------|--------|---------|
| SSH | TCP | 22 | 0.0.0.0/0 | Remote access (restrict to your IP in production) |
| HTTP | TCP | 80 | 0.0.0.0/0 | Nginx (frontend + API proxy) |
| HTTPS | TCP | 443 | 0.0.0.0/0 | Future SSL |
| Custom | TCP | 8080 | 0.0.0.0/0 | Spring Boot direct access (for testing) |
| MySQL | TCP | 3306 | Self (SG) | Database (internal only — not exposed to internet) |

---

## File Locations on EC2

| Path | Contents |
|------|----------|
| `/opt/finserve/finserve-backend-*.jar` | Spring Boot application |
| `/opt/finserve/.env` | Environment variables (DB creds) |
| `/var/www/finserve/` | React build (static files) |
| `/etc/systemd/system/finserve-backend.service` | systemd service definition |
| `/etc/nginx/conf.d/finserve.conf` | Nginx site configuration |

---

## Useful Commands on EC2

```bash
# Application status
sudo systemctl status finserve-backend

# Restart application
sudo systemctl restart finserve-backend

# View application logs
journalctl -u finserve-backend -f

# Restart Nginx
sudo systemctl restart nginx

# MySQL shell
mysql -u finserve_user -p finserve_db
```

---

## Teardown (Stop AWS Charges)

```bash
chmod +x deploy/teardown.sh
./deploy/teardown.sh
```

This will:
1. **Terminate** the EC2 instance
2. **Delete** the security group
3. Optionally **delete** the SSH key pair

**Important:** Run this when you're done to avoid ongoing AWS charges. A t3.micro instance costs ~$0.0104/hour.

### Manual Teardown (if script fails)

```bash
# Get your instance ID
aws ec2 describe-instances --filters "Name=tag:Name,Values=FinServe-Server" \
  --query 'Reservations[].Instances[].InstanceId' --output text

# Terminate
aws ec2 terminate-instances --instance-ids <INSTANCE_ID>

# Wait, then delete security group
aws ec2 delete-security-group --group-name finserve-sg
```

---

## RDS Alternative (Production)

To use AWS RDS instead of local MySQL:

```bash
# Create RDS instance
aws rds create-db-instance \
  --db-instance-identifier finserve-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0 \
  --master-username finserve_admin \
  --master-user-password <SECURE_PASSWORD> \
  --allocated-storage 20 \
  --db-name finserve_db \
  --vpc-security-group-ids <SG_ID> \
  --no-publicly-accessible

# Get the endpoint
aws rds describe-db-instances \
  --db-instance-identifier finserve-db \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

Then update `/opt/finserve/.env` with `DB_HOST=<rds-endpoint>`.

**Teardown RDS:**
```bash
aws rds delete-db-instance \
  --db-instance-identifier finserve-db \
  --skip-final-snapshot
```