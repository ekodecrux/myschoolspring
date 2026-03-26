# MySchool Backend - Spring Boot

This is the **Spring Boot** conversion of the original FastAPI Python backend.
All API endpoints, business logic, and functionality have been preserved 1:1.

## Technology Stack

| Component       | FastAPI (Original)    | Spring Boot (New)              |
|-----------------|-----------------------|--------------------------------|
| Language        | Python 3.11           | Java 17                        |
| Framework       | FastAPI               | Spring Boot 3.2.3              |
| Database ORM    | Motor (async MongoDB) | Spring Data MongoDB            |
| Authentication  | python-jose JWT       | JJWT 0.12.5                    |
| Password Hash   | passlib bcrypt        | Spring Security BCrypt         |
| Storage (R2)    | boto3 S3              | AWS SDK v2 S3                  |
| Payments        | razorpay, stripe      | razorpay-java, stripe-java     |
| Email           | smtplib               | Spring Mail (JavaMailSender)   |
| Build Tool      | pip / requirements    | Maven                          |
| Server Port     | 8000                  | 8000                           |

## API Endpoints (Identical to FastAPI)

### Authentication (`/api/rest/auth`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| POST   | `/login`                          | Login with email/password          |
| POST   | `/register`                       | Self-registration                  |
| POST   | `/newPasswordChallenge`           | Handle first-login password change |
| POST   | `/refreshToken`                   | Refresh access token               |
| GET    | `/forgotPassword?email=xxx`       | Send password reset email          |
| POST   | `/confirmPassword`                | Confirm password reset with OTP    |
| POST   | `/changePassword`                 | Change password (authenticated)    |
| GET    | `/sendOtp?phoneNumber=xxx`        | Send OTP to phone                  |
| POST   | `/loginViaOtp`                    | Login via OTP                      |

### Users (`/api/rest/users`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | `/details`                        | Get current user details           |
| PUT    | `/update`                         | Update user profile                |
| GET    | `/list`                           | List users (role-filtered)         |
| GET    | `/search?query=xxx`               | Search users                       |
| POST   | `/add`                            | Add a new user                     |
| POST   | `/updateCredits`                  | Update user credits                |
| PATCH  | `/{userId}/disable`               | Enable/disable user                |

### Schools (`/api/rest/schools`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| POST   | `/create`                         | Create school (Super Admin)        |
| GET    | `/list`                           | List all schools (Super Admin)     |
| GET    | `/public/active`                  | Active schools (no auth)           |
| GET    | `/{schoolCode}`                   | Get school details                 |
| PATCH  | `/{schoolCode}/toggle-status`     | Enable/disable school              |

### Images (`/api/rest/images`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | `/list`                           | List resource images with filters  |
| POST   | `/upload`                         | Upload resource image to R2        |
| POST   | `/{imageId}/approve`              | Approve/reject image               |
| DELETE | `/{imageId}`                      | Delete resource image              |
| GET    | `/my`                             | List user's personal images        |
| POST   | `/my/upload`                      | Upload to personal library         |
| DELETE | `/my/{imageId}`                   | Delete personal image              |
| GET    | `/r2/list`                        | List R2 folder contents            |
| GET    | `/filters`                        | Get distinct filter values         |
| GET    | `/proxy?url=xxx`                  | Proxy image (CORS bypass)          |

### Payments (`/api/rest/payments`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | `/plans`                          | Get subscription plans             |
| POST   | `/razorpay/create-order`          | Create Razorpay order              |
| POST   | `/razorpay/verify`                | Verify Razorpay payment            |
| POST   | `/stripe/create-session`          | Create Stripe checkout session     |
| POST   | `/stripe/webhook`                 | Stripe webhook handler             |
| GET    | `/history`                        | Get payment history                |
| POST   | `/admin/add-credits`              | Add credits (Super Admin)          |

### Templates (`/api/rest/templates`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | `/list`                           | List user + system templates       |
| POST   | `/save`                           | Save/update template               |
| DELETE | `/{templateId}`                   | Delete template                    |

### Digital Board (`/api/rest/digital-board`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | `/list`                           | List user's boards                 |
| GET    | `/{boardId}`                      | Get specific board                 |
| POST   | `/save`                           | Save/update board                  |
| DELETE | `/{boardId}`                      | Delete board                       |

### Lesson Plans (`/api/rest/lesson-plans`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | `/list`                           | List user's lesson plans           |
| GET    | `/{planId}`                       | Get specific lesson plan           |
| POST   | `/save`                           | Save/update lesson plan            |
| DELETE | `/{planId}`                       | Delete lesson plan                 |

### Support (`/api/rest/support`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| POST   | `/ticket`                         | Create support ticket              |
| GET    | `/tickets`                        | List tickets                       |
| POST   | `/tickets/{ticketId}/reply`       | Reply to ticket                    |
| PATCH  | `/tickets/{ticketId}/status`      | Update ticket status               |

### Admin (`/api/rest/admin`)
| Method | Endpoint                          | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | `/dashboard`                      | Platform stats (Super Admin)       |
| POST   | `/bulk-upload-users`              | Bulk user upload                   |
| PUT    | `/templates/{templateId}`         | Update system template             |

### Health
| Method | Endpoint    | Description     |
|--------|-------------|-----------------|
| GET    | `/health`   | Health check    |
| GET    | `/api/health` | Health check  |

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- MongoDB 6+

### Running Locally

```bash
# Clone and navigate
cd springboot-backend

# Copy and configure environment
cp .env.example .env
# Edit .env with your values

# Run with Maven
mvn spring-boot:run

# Or build and run jar
mvn clean package -DskipTests
java -jar target/myschool-backend-2.0.0.jar
```

### Docker

```bash
# Build image
docker build -t myschool-backend .

# Run container
docker run -p 8000:8000 \
  -e MONGO_URL=mongodb://host.docker.internal:27017 \
  -e JWT_SECRET=your_secret \
  myschool-backend
```

## Key Differences from FastAPI

1. **Synchronous vs Async**: Spring Boot uses synchronous MongoDB operations (Spring Data MongoDB). For async, Spring WebFlux can be added.
2. **JWT**: Uses JJWT library instead of python-jose. Token format is identical.
3. **Dependency Injection**: Uses Spring's `@Autowired` instead of FastAPI's `Depends()`.
4. **Validation**: Uses Jakarta Bean Validation (`@Valid`, `@NotBlank`) instead of Pydantic.
5. **Error Handling**: Uses `@RestControllerAdvice` global exception handler instead of FastAPI exception handlers.
6. **CORS**: Configured in `SecurityConfig` instead of FastAPI middleware.
