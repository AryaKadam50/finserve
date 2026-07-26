# FinServe — Mini Loan Origination System

🔗 **Live Demo:** http://13.127.86.18

A full-stack loan origination and management system built for interview demonstrations. Features customer loan applications, admin management, and rule-based eligibility checking.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React.js (Vite) |
| Backend | Java 17 + Spring Boot 3.2 |
| API | REST |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Auth | BCrypt password hashing |
| Cloud | AWS EC2 + Nginx |

## Architecture

```
┌─────────────────┐     HTTP      ┌──────────────────────────────────────────┐     JDBC     ┌─────────┐
│   React.js UI   │ ──────────→   │  Spring Boot                              │ ──────────→  │  MySQL  │
│  (Vite + Axios) │   REST API    │  Controller → Service → Repository → JPA │              │   8.x   │
└─────────────────┘               └──────────────────────────────────────────┘              └─────────┘
```

## Features

### Customer Side
- ✅ Register / Login (BCrypt password hashing)
- ✅ Apply for a loan (amount, income, tenure, employment type, purpose)
- ✅ Track application status (PENDING / APPROVED / REJECTED / UNDER_REVIEW)
- ✅ View loan details

### Admin Side
- ✅ View all applications in a dashboard table
- ✅ Approve or reject pending applications
- ✅ Admin role check

### Eligibility Engine
- ✅ Rule-based check: monthly income ≥ ₹50,000 → eligible
- ✅ Standalone `/check-eligibility` endpoint
- ✅ Auto-runs on loan submission

---

## Quick Start — Local Development

### Prerequisites
- **Java 17+** (Amazon Corretto or OpenJDK)
- **Maven 3.8+** (or use the included Maven wrapper)
- **MySQL 8.x** running locally
- **Node.js 18+** and **npm 9+**

### 1. Database Setup

```bash
# Option A: Run the schema file
mysql -u root -p < schema.sql

# Option B: Let Hibernate auto-create (ddl-auto=update is set)
# Just create the database:
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS finserve_db;"
```

### 2. Backend

```bash
cd finserve-backend

# Set DB credentials (or edit application.properties)
export DB_USERNAME=root
export DB_PASSWORD=your_password

# Build and run
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080/api`.

**Seeded admin account:** `admin@finserve.com` / `admin123`

### 3. Frontend

```bash
cd finserve-frontend

# Install dependencies
npm install

# Start dev server
npm run dev
```

The UI will be available at `http://localhost:5173`.

---

## API Endpoints

| Method | Endpoint | Description | Status Code |
|--------|----------|-------------|-------------|
| `POST` | `/api/users/register` | Register a new user | 201 |
| `POST` | `/api/users/login` | Authenticate user | 200 |
| `POST` | `/api/loans` | Submit loan application | 201 |
| `GET` | `/api/loans` | Get all loans (admin) | 200 |
| `GET` | `/api/loans/{id}` | Get loan by ID | 200 |
| `PUT` | `/api/loans/{id}/status` | Update loan status | 200 |
| `DELETE` | `/api/loans/{id}` | Delete a loan | 204 |
| `GET` | `/api/users/{userId}/loans` | Get user's loans | 200 |
| `POST` | `/api/loans/check-eligibility` | Check eligibility | 200 |

### Example: Check Eligibility

```bash
curl -X POST http://localhost:8080/api/loans/check-eligibility \
  -H "Content-Type: application/json" \
  -d '{
    "monthlyIncome": 75000,
    "requestedAmount": 500000,
    "tenure": 60
  }'
```

Response:
```json
{
  "eligible": true,
  "message": "Congratulations! You are eligible for the loan."
}
```

---

## Database Schema

### `users`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(150) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | NOT NULL (BCrypt) |
| phone | VARCHAR(20) | |
| role | ENUM('USER','ADMIN') | DEFAULT 'USER' |

### `loan_applications`
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| user_id | BIGINT | FK → users.id |
| amount | DECIMAL(15,2) | NOT NULL |
| tenure | INT | NOT NULL (months) |
| monthly_income | DECIMAL(15,2) | NOT NULL |
| employment_type | VARCHAR(50) | NOT NULL |
| purpose | VARCHAR(255) | |
| status | ENUM | DEFAULT 'PENDING' |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

---

## Backend Package Structure

```
finserve-backend/
├── src/main/java/com/finserve/
│   ├── FinserveApplication.java
│   ├── controller/
│   │   ├── LoanController.java
│   │   └── UserController.java
│   ├── service/
│   │   ├── LoanService.java
│   │   └── UserService.java
│   ├── repository/
│   │   ├── LoanRepository.java
│   │   └── UserRepository.java
│   ├── model/
│   │   ├── LoanApplication.java
│   │   ├── LoanStatus.java
│   │   ├── User.java
│   │   └── UserRole.java
│   ├── dto/
│   │   ├── ApiResponse.java
│   │   ├── EligibilityRequest.java
│   │   ├── EligibilityResponse.java
│   │   ├── LoanApplicationRequest.java
│   │   ├── LoanStatusUpdateRequest.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   └── RegisterRequest.java
│   ├── exception/
│   │   ├── BadRequestException.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── ResourceNotFoundException.java
│   └── config/
│       ├── CorsConfig.java
│       └── SecurityConfig.java
├── src/main/resources/
│   ├── application.properties
│   └── data.sql
└── pom.xml
```

---

## Frontend Pages

| Page | Route | Access |
|------|-------|--------|
| Home | `/` | Public |
| Register | `/register` | Public |
| Login | `/login` | Public |
| Apply for Loan | `/apply` | User |
| My Applications | `/my-applications` | User |
| Loan Details | `/loans/:id` | User |
| Admin Dashboard | `/admin` | Admin |

---

## Postman Collection

Import `postman/FinServe.postman_collection.json` into Postman. The collection includes all endpoints with example request/response bodies and uses variables for `baseUrl`, `userId`, and `loanId`.

---

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for complete AWS deployment instructions.

### Quick Deploy

```bash
# 1. Provision EC2
./deploy/provision-ec2.sh

# 2. Set up server (Java, MySQL)
./deploy/setup-server.sh <EC2_IP>

# 3. Deploy backend
./deploy/deploy-backend.sh <EC2_IP>

# 4. Deploy frontend
./deploy/deploy-frontend.sh <EC2_IP>
```

### Tear Down (stop AWS charges)

```bash
./deploy/teardown.sh
```

---

## Assumptions & Notes

1. **Tenure** is measured in **months** throughout the system
2. **Currency** is **INR (₹)** as shown in the UI
3. **No JWT/OAuth** — simple login with BCrypt; state managed in React Context + localStorage
4. **Admin user** is seeded via `data.sql` on first run
5. **Eligibility rule**: monthly income ≥ ₹50,000 → eligible (non-ML, rule-based)
6. **Hibernate `ddl-auto=update`** generates tables automatically; `schema.sql` is provided for manual setup

---

## License

This project is for interview demonstration purposes.