# FinServe — AI-Assisted Digital Loan Origination and Underwriting Platform

FinServe is a full-stack **MERN (MongoDB, Express.js, React, Node.js)** digital lending and underwriting platform. It automates the loan origination lifecycle—from borrower registration and application submission with document verification to AI-assisted risk assessment and administrative underwriting workflows.

> **Status:** Local Development Mode (Not deployed to cloud infrastructure at this stage).

---

## 🌟 Architecture & Tech Stack

```
Borrower / Admin Browser
           │
           ▼
 React + Vite Frontend (Port 5173)
           │
           │  (REST API calls / Proxied via Vite)
           ▼
 Express.js Backend (Port 8080)
   ├── JWT Auth & Role-Based Access Control
   ├── Document Verification Engine (Deterministic 5% Tolerance)
   ├── Policy RAG (In-Memory 1536-dim Cosine Similarity Search)
   └── OpenAI Underwriting Agent (with Deterministic Mock Fallback)
           │
           ▼
 MongoDB 7.0+ (Port 27018 / 27017 via Prisma ORM)
```

| Layer | Technology | Details |
|---|---|---|
| **Frontend** | React 18, Vite, React Router, Axios | Dark glassmorphic design system (`index.css`), real-time status badges, underwriting queue |
| **Backend** | Node.js (v22+), Express.js (ES Modules) | Clean layered architecture (Routes, Controllers, Services, Middleware) |
| **Database & ORM** | MongoDB with Prisma ORM | Relational models (`User`, `LoanApplication`, `Document`, `VerificationResult`, `UnderwritingResult`, `AuditEvent`) |
| **Authentication** | BCrypt.js + JSON Web Tokens (JWT) | Role-Based Access Control (`USER`, `ADMIN`), borrower data isolation |
| **Document Processing** | Multer + Deterministic Rule Engine | 10MB upload limit, PDF/image validation, 5% income tolerance matching |
| **AI Underwriting** | OpenAI API (`gpt-4o-mini`) + Policy RAG | Cosine similarity retrieval over 14 markdown policy chunks with simulated fallback |

---

## 🚀 Key Features

### 1. 🧑‍💼 Borrower Experience
* **Account Management:** Self-service registration and login secured with BCrypt password hashing and signed JWT tokens.
* **Loan Application Submission:** Captures loan amount, tenure, monthly income, expenses, existing EMIs, credit score, and employment details.
* **Document Ingestion:** Upload Salary Slips and Employment Proof documents (`.pdf`, `.png`, `.jpg`, `.jpeg`).
* **Live Status Tracking:** Real-time visibility into application progress (`PENDING`, `UNDER_REVIEW`, `AI_RECOMMENDED`, `APPROVED`, `REJECTED`, `INFO_REQUESTED`).

### 2. 📄 Automated Document Verification
* Deterministically evaluates extracted document data against declared loan details in JavaScript.
* **Income Tolerance Match:** Configured to $\pm 5\%$ tolerance:
  $$\text{Tolerance} = \text{declared} \times 0.05$$
  $$\text{Match} \iff |\text{declared} - \text{extracted}| \le \text{Tolerance}$$
* Flags documents with `MATCH`, `MISMATCH`, or `NOT_FOUND` on individual fields (Income, Employee Name, Employment Status).

### 3. 🤖 AI-Assisted Underwriting & Policy RAG
* **Policy Knowledge Base:** Ingests lending guidelines (`dti-policy.md`, `income-requirements.md`, `credit-score.md`, `manual-review.md`) into 14 vector chunks.
* **Top-3 Semantic Search:** In-memory vector store computing cosine similarity over 1536-dimensional embeddings.
* **Deterministic Financial Ratios:**
  * Estimated monthly EMI calculated at 12% p.a.
  * Debt-to-Income (DTI) ratio evaluated against a 45% ceiling.
* **Structured Output Schema:** Returns recommendation (`APPROVE`, `REVIEW`, `REJECT`), risk level (`LOW`, `MEDIUM`, `HIGH`), confidence score, and key rationale.
* **Deterministic Mock Fallback:** When `OPENAI_API_KEY` is not provided, the local smart mock analyzes the exact same rules with zero external dependencies.
* **Non-Autonomous Guardrail:** The AI is strictly an underwriting assistant; it **never** directly approves or rejects loans.
* **Mandatory Override Auditing:** Admins overriding the AI's recommendation must provide an explicit justification that persists to the audit trail.

### 4. 👨‍💼 Administrator Underwriting Queue
* Filterable administrative queue with summary statistics (Total, Pending, Approved, Rejected).
* Deep-dive review page with borrower financials, document verification tables, and the interactive AI assessment panel.
* Complete immutable audit timeline for every application event.

---

## 💻 Local Setup & Development

### Prerequisites
* **Node.js** v18+ (tested on Node v22)
* **npm** v9+
* **MongoDB** (running locally)

---

### Step 1: Clone the Repository
```bash
git clone https://github.com/AryaKadam50/finserve.git
cd finserve
```

---

### Step 2: Configure & Start MongoDB
Prisma requires MongoDB to run with replica set support enabled (for atomic transactions):

```bash
# Start MongoDB with replica set rs0 on port 27018
mkdir -p /tmp/mongo_data
mongod --dbpath /tmp/mongo_data --port 27018 --replSet rs0 --bind_ip 127.0.0.1 --fork --logpath /tmp/mongo_data/mongo.log

# Initialize the replica set (only needed once)
node -e "
import('mongodb').then(async ({ MongoClient }) => {
  const client = new MongoClient('mongodb://127.0.0.1:27018/?directConnection=true');
  await client.connect();
  try { await client.db('admin').command({ replSetInitiate: {} }); } catch(e){}
  await client.close();
});"
```

---

### Step 3: Setup the Backend
```bash
cd backend
npm install

# Push Prisma schema to your MongoDB database
npx prisma generate
npx prisma db push

# Start the Express server
npm run dev
```
* The backend will run on **`http://localhost:8080`**.
* It automatically ingests all 14 policy chunks into vector memory on startup.
* It auto-bootstraps the default Administrator account (`admin@finserve.com` / `admin123`).

---

### Step 4: Setup the Frontend
In a new terminal window:
```bash
cd finserve-frontend
npm install
npm run dev
```
* The frontend will start on **`http://localhost:5173`**.
* API requests (`/api/*`) are automatically proxied to port `8080`.

---

## 🔑 Default Credentials

| Role | Email | Password | Access |
|---|---|---|---|
| **Administrator** | `admin@finserve.com` | `admin123` | Underwriting Queue, AI Review, Approval/Rejection, Audit Timeline |
| **Borrower** | *Create via Register page* | *Your password* | Loan Application, Document Uploads, Status Tracking |

---

## 🧪 Running Automated Tests

Run the backend unit test suite (verifying financial EMI math, document extraction rules, RAG similarity search, and AI mock logic):

```bash
cd backend
npm test
```

---

## 📂 Project Structure

```
finserve/
├── backend/
│   ├── prisma/
│   │   └── schema.prisma        # Database models & relationships
│   ├── src/
│   │   ├── app.js               # Express application configuration
│   │   ├── server.js            # Entry point & bootstrap logic
│   │   ├── config/              # Prisma client singleton
│   │   ├── controllers/         # HTTP request handlers
│   │   ├── middleware/          # Auth, JWT, RBAC, Multer, Rate Limiting, Error Handling
│   │   ├── policies/            # Markdown lending policies for RAG
│   │   ├── routes/              # Express API routers
│   │   ├── services/            # Core business logic, RAG, & AI Underwriting
│   │   └── utils/               # Response helpers & serializers
│   └── tests/                   # Native Node.js test suite
├── finserve-frontend/
│   ├── src/
│   │   ├── components/          # Navbar, ProtectedRoute, StatusBadge
│   │   ├── context/             # AuthContext (JWT management)
│   │   ├── pages/               # Home, Register, Login, ApplyLoan, AdminDashboard, ApplicationReview
│   │   └── services/api.js      # Axios client with JWT interceptor
│   └── vite.config.js           # Vite dev server + proxy setup
└── README.md
```

---

## 📄 License
This project is for educational and portfolio demonstration purposes.