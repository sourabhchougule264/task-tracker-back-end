# Quick Start Guide - Task Tracker Application

This guide will help you get the Task Tracker application up and running in minutes.

## 🚀 Quick Setup (5 Minutes)

### Prerequisites
- Java 21 installed
- PostgreSQL installed and running
- Maven installed

### Step 1: Database Setup (1 minute)
```bash
# Create database
createdb tasktracker

# Or using psql
psql -U postgres
CREATE DATABASE tasktracker;
\q
```

### Step 2: Configure Database (30 seconds)
Open `src/main/resources/application.properties` and update:
```properties
spring.datasource.username=postgres
spring.datasource.password=your_password
```

Or set environment variables:
```bash
# Windows PowerShell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"

# Linux/Mac
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

### Step 3: Build & Run (2 minutes)
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

### Step 4: Verify (1 minute)
Open browser or use curl:
```bash
curl http://localhost:8080/api/roles
```

Expected response: List of 3 roles (ADMIN, TASK_CREATOR, READ_ONLY)

## 🎯 First API Calls

### 1. Create Your First User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123",
    "email": "admin@test.com",
    "firstName": "Admin",
    "lastName": "User"
  }'
```

### 2. Assign Admin Role
```bash
curl -X POST http://localhost:8080/api/users/1/roles/ADMIN
```

### 3. Create Your First Project
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My First Project",
    "description": "Getting started with Task Tracker",
    "startDate": "2024-01-01",
    "endDate": "2024-12-31",
    "ownerId": 1
  }'
```

### 4. Create Your First Task
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Complete project setup",
    "dueDate": "2024-03-01",
    "status": "NEW",
    "ownerId": 1,
    "projectId": 1
  }'
```

### 5. View All Tasks
```bash
curl http://localhost:8080/api/tasks
```

## 🐳 Quick Start with Docker (3 Minutes)

### Option 1: Using Docker Compose (Recommended)
```bash
# Start both PostgreSQL and Application
docker-compose up -d

# Check logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Option 2: Manual Docker
```bash
# Start PostgreSQL
docker run -d \
  --name tasktracker-db \
  -e POSTGRES_DB=tasktracker \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:16-alpine

# Build and run application
docker build -t task-tracker-app .
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/tasktracker \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=password \
  --name task-tracker-app \
  task-tracker-app
```

## 📊 Quick Database Check

### Connect to PostgreSQL
```bash
psql -U postgres -d tasktracker
```

### View Tables
```sql
\dt

-- Expected output:
-- projects
-- roles
-- tasks
-- user_roles
-- users
```

### Check Data
```sql
-- View roles
SELECT * FROM roles;

-- View users
SELECT * FROM users;

-- View projects
SELECT * FROM projects;

-- View tasks
SELECT * FROM tasks;
```

## 🧪 Quick API Testing

### Using PowerShell
```powershell
# Get all projects
Invoke-RestMethod -Uri "http://localhost:8080/api/projects" -Method Get

# Create user
$body = @{
    username = "testuser"
    password = "test123"
    email = "test@example.com"
    firstName = "Test"
    lastName = "User"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Method Post -Body $body -ContentType "application/json"
```

### Using Postman
1. Import the cURL commands from `API_TESTING_GUIDE.md`
2. Create a new collection
3. Set base URL: `http://localhost:8080/api`
4. Start testing!

## 🔍 Troubleshooting

### Application won't start?

**Check Java version:**
```bash
java -version
# Should be Java 21
```

**Check PostgreSQL is running:**
```bash
# Windows
Get-Service postgresql*

# Linux/Mac
sudo systemctl status postgresql
```

**Check port 8080 is free:**
```bash
# Windows
netstat -ano | findstr :8080

# Linux/Mac
lsof -i :8080
```

### Database connection error?

**Test connection:**
```bash
psql -U postgres -d tasktracker
```

**Check credentials in application.properties**

### Build errors?

**Clean and rebuild:**
```bash
mvn clean
mvn install -U
```

## 📱 Quick Test Workflow

Here's a complete workflow to test all features:

```bash
# 1. Create users
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123","email":"alice@test.com","firstName":"Alice","lastName":"Smith"}'

curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"pass123","email":"bob@test.com","firstName":"Bob","lastName":"Jones"}'

# 2. Assign roles
curl -X POST http://localhost:8080/api/users/1/roles/ADMIN
curl -X POST http://localhost:8080/api/users/2/roles/TASK_CREATOR

# 3. Create project
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -d '{"name":"Website Redesign","description":"Redesign company website","startDate":"2024-01-01","endDate":"2024-06-30","ownerId":1}'

# 4. Create tasks
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"description":"Design mockups","dueDate":"2024-02-15","status":"NEW","ownerId":1,"assignedUserId":2,"projectId":1}'

curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"description":"Develop frontend","dueDate":"2024-04-30","status":"NOT_STARTED","ownerId":1,"assignedUserId":2,"projectId":1}'

# 5. Update task status
curl -X PATCH http://localhost:8080/api/tasks/1/status/IN_PROGRESS

# 6. Get all tasks for a project
curl http://localhost:8080/api/tasks/project/1

# 7. Get tasks assigned to a user
curl http://localhost:8080/api/tasks/assigned/2

# 8. Complete a task
curl -X PATCH http://localhost:8080/api/tasks/1/status/COMPLETED
```

## 📝 Default Data

After startup, the application automatically creates:
- 3 Roles: ADMIN, TASK_CREATOR, READ_ONLY

You need to manually create:
- Users
- Projects
- Tasks

## 🎓 Next Steps

1. ✅ Read `README.md` for full documentation
2. ✅ Check `API_TESTING_GUIDE.md` for all API examples
3. ✅ Review `AWS_DEPLOYMENT_GUIDE.md` for cloud deployment
4. ✅ See `IMPLEMENTATION_SUMMARY.md` for technical details

## 🆘 Need Help?

### Common URLs
- **API Base**: http://localhost:8080/api
- **Projects**: http://localhost:8080/api/projects
- **Tasks**: http://localhost:8080/api/tasks
- **Users**: http://localhost:8080/api/users
- **Roles**: http://localhost:8080/api/roles

### Log Files
Application logs are printed to console. To save:
```bash
mvn spring-boot:run > app.log 2>&1
```

### Database Reset
```sql
-- Drop all tables
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

-- Restart application to recreate tables
```

## ✅ Quick Checklist

Before you start:
- [ ] Java 21 installed
- [ ] PostgreSQL installed and running
- [ ] Database 'tasktracker' created
- [ ] application.properties configured
- [ ] Port 8080 is available

After startup:
- [ ] Application starts without errors
- [ ] Can access http://localhost:8080/api/roles
- [ ] Can create a user
- [ ] Can create a project
- [ ] Can create a task

## 🎉 You're Ready!

Your Task Tracker application is now running and ready to use!

**Application URL**: http://localhost:8080/api

Start testing the APIs or integrate with your frontend application.

Happy tracking! 🚀
