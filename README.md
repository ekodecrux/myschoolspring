# MySchool Platform — Spring Boot Edition

A full-stack school management and digital resource platform. This repository is the **Spring Boot migration** of the original FastAPI backend, keeping the React frontend and all supporting files intact.

---

## Repository Structure

```
myschoolspring/
├── backend/                  # Spring Boot backend (Java 17, Maven)
│   ├── src/
│   │   └── main/
│   │       ├── java/com/myschool/backend/
│   │       │   ├── config/           # Security, MongoDB, WebClient config
│   │       │   ├── controller/       # REST controllers (Auth, User, Image, Payment, etc.)
│   │       │   ├── exception/        # Global exception handler & custom exceptions
│   │       │   ├── models/
│   │       │   │   ├── entity/       # MongoDB document models
│   │       │   │   ├── request/      # Request DTOs
│   │       │   │   └── response/     # Response DTOs
│   │       │   ├── repository/       # Spring Data MongoDB repositories
│   │       │   ├── security/         # JWT filter & token provider
│   │       │   ├── service/          # Business logic services
│   │       │   └── util/             # Code generators & utilities
│   │       └── resources/
│   │           └── application.yml   # Application configuration
│   ├── pom.xml               # Maven build file
│   ├── Dockerfile            # Docker build for backend
│   └── README.md             # Backend-specific documentation
├── frontend/                 # React + TailwindCSS frontend
│   ├── src/                  # React source code
│   ├── public/               # Static assets
│   └── package.json          # Frontend dependencies
├── docs/                     # API and deployment documentation
├── API_DOCUMENTATION.md      # Full REST API reference
├── R2_CORS_CONFIGURATION.md  # Cloudflare R2 CORS setup
├── R2_MIGRATION_GUIDE.md     # R2 storage migration guide
└── flutter_models.dart       # Flutter/Dart model definitions
```

---

## Technology Stack

| Layer        | Original (FastAPI)        | Migrated (Spring Boot)              |
|--------------|---------------------------|-------------------------------------|
| Language     | Python 3.11               | Java 17                             |
| Framework    | FastAPI + Uvicorn         | Spring Boot 3.x + Tomcat            |
| Database     | MongoDB (Motor async)     | MongoDB (Spring Data MongoDB)       |
| Auth         | PyJWT + python-jose       | JJWT + Spring Security              |
| Email        | aiosmtplib                | Spring Mail (JavaMailSender)        |
| Storage      | boto3 (Cloudflare R2/S3)  | AWS SDK v2 (S3Client)               |
| Payments     | Stripe Python SDK         | Stripe Java SDK                     |
| Build        | pip / requirements.txt    | Maven / pom.xml                     |
| Frontend     | React + TailwindCSS       | React + TailwindCSS (unchanged)     |

---

## API Endpoints (Base: `/api/rest`)

| Module          | Prefix              | Key Endpoints                                          |
|-----------------|---------------------|--------------------------------------------------------|
| Auth            | `/auth`             | login, register, password reset, OTP verify            |
| Users           | `/users`            | profile, credits, subscription, bulk upload            |
| Images          | `/images`           | fetch, search, my-images, download, PDF thumbnail      |
| School Mgmt     | `/school`           | create school, add teacher/student, manage users       |
| Payments        | `/payment`          | create session, webhook, subscription history          |
| Support         | `/support`          | submit ticket, list, resolve                           |
| Templates       | `/templates`        | list, save, delete maker templates                     |
| Digital Boards  | `/digital-boards`   | CRUD for teacher digital boards                        |
| Lesson Plans    | `/lesson-plans`     | CRUD for teacher lesson plans                          |
| Admin           | `/admin`            | dashboard stats, bulk import, image categories         |
| Health          | `/health`           | health check                                           |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB 6.x (local or Atlas)
- Node.js 18+ (for frontend)

### Backend Setup

```bash
cd backend

# Configure environment variables in src/main/resources/application.yml
# (fill in MONGO_URL, JWT_SECRET, SMTP, R2, Stripe keys)

# Build and run
mvn clean install
mvn spring-boot:run
```

The backend starts on **port 8080** by default.

### Frontend Setup

```bash
cd frontend
npm install
npm start
```

The frontend dev server starts on **port 3000** by default.

### Docker (Backend)

```bash
cd backend
docker build -t myschool-backend .
docker run -p 8080:8080 --env-file .env myschool-backend
```

---

## Migration Notes

The Spring Boot backend is a **feature-complete conversion** of the original FastAPI backend:

- All REST API routes, HTTP methods, and URL paths are preserved exactly.
- JWT authentication flow (login → token → protected routes) is identical.
- MongoDB collection names and document structures are unchanged.
- Role-based access control (SUPER_ADMIN, SCHOOL_ADMIN, TEACHER, STUDENT) is fully preserved.
- Email notifications, Cloudflare R2 storage, and Stripe payment integration are all ported.
- The frontend requires **no changes** — it continues to call the same API endpoints.

---

## License

Proprietary — MySchool Platform. All rights reserved.
