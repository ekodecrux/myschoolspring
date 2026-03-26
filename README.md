# MySchool Platform

Welcome to the MySchool platform repository! This project contains both the frontend and backend code for our school management and digital resource platform.

We recently moved our backend from Python (FastAPI) to Java (Spring Boot) to better handle our scaling needs, but the frontend remains the same React application.

## What's in here?

- `backend/` - The new Spring Boot 3.x backend. It's written in Java 17 and uses Maven.
- `frontend/` - Our React frontend, styled with TailwindCSS.
- `docs/` - Various documentation files, including API references and guides.

## Tech Stack

**Backend:**
- Java 17
- Spring Boot 3
- MongoDB (via Spring Data)
- Spring Security with JWT for authentication
- AWS SDK for Cloudflare R2 storage
- Stripe Java SDK for payments

**Frontend:**
- React
- TailwindCSS

## How to run it locally

### 1. Start the Backend

Make sure you have Java 17 and Maven installed, plus a running MongoDB instance.

```bash
cd backend
```

You'll need to set up your environment variables first. Open `src/main/resources/application.yml` and add your MongoDB connection string, JWT secret, SMTP credentials, Cloudflare R2 keys, and Stripe keys.

Then build and run:
```bash
mvn clean install
mvn spring-boot:run
```
The server will start on port 8080.

If you prefer Docker:
```bash
docker build -t myschool-backend .
docker run -p 8080:8080 --env-file .env myschool-backend
```

### 2. Start the Frontend

You'll need Node.js installed.

```bash
cd frontend
npm install
npm start
```
This will start the development server on port 3000.

## Notes on the Migration

If you worked on the old FastAPI codebase, you'll find the new Spring Boot structure pretty familiar. We kept all the API endpoints exactly the same so the frontend wouldn't break. The database collections and role-based access control (Super Admin, School Admin, Teacher, Student) also work exactly as they did before.
