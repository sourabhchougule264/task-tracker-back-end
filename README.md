# Task Tracker Application

A comprehensive Task Tracking application built with Spring Boot and PostgreSQL that enables project management, task tracking, user management, and role-based access control.

## Features

- ✅ Create, Update, Delete Projects
- ✅ Create, Update, Delete Tasks
- ✅ Assign tasks to users
- ✅ User management
- ✅ Role-based access control (ADMIN, TASK_CREATOR, READ_ONLY)
- ✅ Task status management (NEW, IN_PROGRESS, BLOCKED, COMPLETED, NOT_STARTED)
- ✅ RESTful API architecture
- ✅ PostgreSQL database integration
- ✅ Exception handling
- ✅ Modular and SOLID principles
- ✅ **Two authentication options**: Database or AWS Cognito

## Authentication Options

### Option 1: Database-Based Roles (Current Implementation)
- Roles stored in PostgreSQL
- Custom user management
- Full control over authentication logic
- **Good for**: Learning, local development, offline work

### Option 2: AWS Cognito (Recommended for Production)
- AWS-managed authentication
- Built-in SSO, MFA, and social login
- FREE for up to 50,000 users/month
- Enterprise-grade security
- **Good for**: Production, scalability, modern cloud apps

📘 **See documentation:**
- `AWS_COGNITO_INTEGRATION.md` - Complete Cognito setup guide
- `DATABASE_VS_COGNITO_COMPARISON.md` - Detailed comparison
- `COGNITO_IMPLEMENTATION_CHECKLIST.md` - Step-by-step migration

## Technology Stack

- **Backend**: Spring Boot 3.5.10
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven
- **Java Version**: 21
- **Additional Libraries**: Lombok

## Prerequisites

- JDK 21 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher
- AWS Account (optional, for cloud deployment)

## Database Setup

1. Install PostgreSQL
2. Create a database named `tasktracker`:

```sql
CREATE DATABASE tasktracker;
```

3. Update `application.properties` with your database credentials:

```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

Or set environment variables:
```bash
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

## Installation & Running

1. Clone the repository
2. Navigate to the project directory
3. Build the project:

```bash
mvn clean install
```

4. Run the application:

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080/api`

## API Endpoints

### Project APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/projects` | Create a new project |
| GET | `/api/projects` | Get all projects |
| GET | `/api/projects/{id}` | Get project by ID |
| GET | `/api/projects/owner/{ownerId}` | Get projects by owner |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project |

**Sample Project JSON:**
```json
{
  "name": "Mobile App Development",
  "description": "Develop a mobile application",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "ownerId": 1
}
```

### Task APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/tasks` | Create a new task |
| GET | `/api/tasks` | Get all tasks |
| GET | `/api/tasks/{id}` | Get task by ID |
| GET | `/api/tasks/project/{projectId}` | Get tasks by project |
| GET | `/api/tasks/assigned/{userId}` | Get tasks assigned to user |
| GET | `/api/tasks/status/{status}` | Get tasks by status |
| PUT | `/api/tasks/{id}` | Update task |
| PATCH | `/api/tasks/{taskId}/assign/{userId}` | Assign task to user |
| PATCH | `/api/tasks/{taskId}/status/{status}` | Update task status |
| DELETE | `/api/tasks/{id}` | Delete task |

**Sample Task JSON:**
```json
{
  "description": "Design database schema",
  "dueDate": "2024-06-30",
  "status": "NEW",
  "ownerId": 1,
  "assignedUserId": 2,
  "projectId": 1
}
```

**Task Status Values:**
- `NEW`
- `IN_PROGRESS`
- `BLOCKED`
- `COMPLETED`
- `NOT_STARTED`

### User APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create a new user |
| GET | `/api/users` | Get all users |
| GET | `/api/users/{id}` | Get user by ID |
| GET | `/api/users/username/{username}` | Get user by username |
| PUT | `/api/users/{id}` | Update user |
| POST | `/api/users/{userId}/roles/{roleType}` | Assign role to user |
| DELETE | `/api/users/{userId}/roles/{roleType}` | Remove role from user |
| DELETE | `/api/users/{id}` | Delete user |

**Sample User JSON:**
```json
{
  "username": "john.doe",
  "password": "password123",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Role APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/roles` | Create a new role |
| GET | `/api/roles` | Get all roles |
| GET | `/api/roles/{id}` | Get role by ID |
| GET | `/api/roles/name/{name}` | Get role by name |
| DELETE | `/api/roles/{id}` | Delete role |

**Role Types:**
- `ADMIN` - Full access to manage projects, tasks, and users
- `TASK_CREATOR` - Can create and update projects and tasks
- `READ_ONLY` - Can only view tasks and mark assigned tasks as complete

**Sample Role JSON:**
```json
{
  "name": "ADMIN",
  "description": "Administrator role with full access"
}
```

## Project Structure

```
tasktrackerapp/
├── src/
│   ├── main/
│   │   ├── java/com/task/tracker/tasktrackerapp/
│   │   │   ├── config/
│   │   │   │   └── DataInitializer.java
│   │   │   ├── controller/
│   │   │   │   ├── ProjectController.java
│   │   │   │   ├── TaskController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── RoleController.java
│   │   │   ├── dto/
│   │   │   │   ├── ProjectDTO.java
│   │   │   │   ├── TaskDTO.java
│   │   │   │   └── UserDTO.java
│   │   │   ├── entity/
│   │   │   │   ├── Project.java
│   │   │   │   ├── Task.java
│   │   │   │   ├── User.java
│   │   │   │   └── Role.java
│   │   │   ├── enums/
│   │   │   │   ├── TaskStatus.java
│   │   │   │   └── RoleType.java
│   │   │   ├── exception/
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── repository/
│   │   │   │   ├── ProjectRepository.java
│   │   │   │   ├── TaskRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── RoleRepository.java
│   │   │   ├── service/
│   │   │   │   ├── ProjectService.java
│   │   │   │   ├── TaskService.java
│   │   │   │   ├── UserService.java
│   │   │   │   └── RoleService.java
│   │   │   └── TasktrackerappApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## AWS Deployment Options

### Option 1: Amazon RDS + EC2
- Deploy PostgreSQL on Amazon RDS
- Deploy Spring Boot app on EC2 instance

### Option 2: Amazon ECS/EKS
- Containerize application with Docker
- Deploy on Amazon ECS (Fargate) or EKS

### Option 3: Elastic Beanstalk
- Package application as WAR/JAR
- Deploy directly to Elastic Beanstalk

### Additional AWS Services Integration
- **Amazon S3**: Store task attachments
- **Amazon Cognito**: SSO authentication
- **Amazon CloudWatch**: Monitoring and logging
- **AWS Lambda**: Scheduled tasks and notifications
- **Amazon SES**: Email notifications
- **Amazon ElastiCache**: Caching layer

## Testing APIs

You can test the APIs using:
- **Postman**: Import the endpoints and test
- **cURL**: Command-line testing
- **Swagger UI**: (Can be added with springdoc-openapi dependency)

### Example cURL Commands:

**Create a Project:**
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Project",
    "description": "Project description",
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "ownerId": 1
  }'
```

**Get All Tasks:**
```bash
curl -X GET http://localhost:8080/api/tasks
```

**Update Task Status:**
```bash
curl -X PATCH http://localhost:8080/api/tasks/1/status/IN_PROGRESS
```

## Future Enhancements

- [ ] Spring Security with JWT authentication
- [ ] OAuth2/SSO integration with AWS Cognito
- [ ] API documentation with Swagger/OpenAPI
- [ ] Unit and integration tests
- [ ] Docker containerization
- [ ] CI/CD pipeline with AWS CodePipeline
- [ ] File upload for task attachments
- [ ] Email notifications
- [ ] Task comments and activity log
- [ ] Dashboard and analytics
- [ ] Search and advanced filtering

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Contact

For questions or support, please contact the development team.
