# MySchool Backend

This is the backend service for the MySchool platform, built with Spring Boot.

## Stack

- **Java 17**
- **Spring Boot 3.2.x**
- **MongoDB** (via Spring Data MongoDB)
- **Spring Security & JJWT** for authentication
- **AWS SDK v2** for interacting with Cloudflare R2 storage
- **Stripe & Razorpay** Java SDKs for payments

## Getting Started

### Prerequisites
You'll need Java 17+, Maven 3.8+, and a MongoDB instance running.

### Local Development

1. Set up your environment variables. The easiest way is to copy the example configuration or just export them in your shell:
   - `MONGO_URL`
   - `JWT_SECRET`
   - `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`
   - `R2_ACCESS_KEY`, `R2_SECRET_KEY`, `R2_BUCKET`, `R2_ENDPOINT`, `R2_PUBLIC_URL`
   - `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`
   - `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`

2. Run the application:
   ```bash
   mvn spring-boot:run
   ```

   Or build the jar and run it:
   ```bash
   mvn clean package -DskipTests
   java -jar target/myschool-backend-2.0.0.jar
   ```

The server runs on port 8000 by default.

### Docker

If you prefer using Docker:

```bash
docker build -t myschool-backend .

docker run -p 8000:8000 \
  -e MONGO_URL=mongodb://host.docker.internal:27017/myschool \
  -e JWT_SECRET=your_secret \
  myschool-backend
```

## API Structure

The API is structured around several core modules under `/api/rest`:

- `/auth` - Login, registration, and password management
- `/users` - User profiles, roles, and credit management
- `/schools` - School onboarding and management
- `/images` - Resource library, user uploads, and R2 integration
- `/payments` - Subscription plans and checkout flows
- `/templates` - Maker templates for educational content
- `/digital-board` - Teacher whiteboard data
- `/lesson-plans` - Teacher lesson planning
- `/support` - Ticketing system
- `/admin` - Platform-wide stats and bulk operations

Check the main API documentation for detailed request/response formats.
