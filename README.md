# 🚀 Redis Caching with MySQL

A hands-on learning project to understand **Redis caching** in a real-world scenario. Built RESTful APIs for product management using **Springboot**, **MySQL** as the primary database, and **Redis** for caching — with pagination support.

> ⚠️ This project is built purely for **learning purposes** to understand how Redis works alongside a relational database.

---

## 🧠 What I Learned

- How Redis stores and retrieves data (key-value pairs)
- How to integrate Redis as a caching layer on top of MySQL
- Cache hit vs cache miss flow
- Building REST APIs with pagination
- Testing APIs using Postman

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| Java | Backend runtime |
| Springboot | REST API framework |
| MySQL | Primary database |
| Redis | Caching layer |
| Postman | API testing |

---

## 📦 Features

- ✅ Add product details to MySQL database
- ✅ Redis caching for faster data retrieval
- ✅ Pagination (10 products per page)
- ✅ REST APIs for CRUD operations
- ✅ Tested via Postman

---

## 📁 Project Structure

```
-->
src/main/resources
/config
   .RedisCacheConfig.java
   .RedisObjectMapperConfig.java
   .RedisTemplateConfig.java
/controller
   .ProductController.java
/exception
  .GlobalExceptionHandler.java
  .ProductNotFoundException.java
  .ProductDuplicateException.java
/init
  .DataInitialer.java
  .DataInitialerProperties.java
/model/
/dto 
   --request/ProductRequest.java
   --response/ProductResponse.java, ApiResponse.java, RestPageImpl.java
/entity
   --Product.java
/mapper
ProductMapper.java
/repository
  --ProductRepository.java
util/RedisUtil.java
resouces
--application.properties
--application-dev.properties
--application-prod.properties
--application-example.properties
.gitIgnore
README.md
  .
```

> Note: Adjust the structure above to match your actual project.

---

## ⚙️ Setup & Installation

### Prerequisites
- JDK installed
- MySQL running
- Redis server running

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/your-username/redis-caching-with-mysql.git

# 2. Go into the project directory
cd redis-caching-with-mysql

# 3. Add dependencies in pom.xml



# 4. Start the server
by vsCode runner
```

---

## 🔐 Environment Variables

Create a `split properties` file in the root directory:

```env
PORT=8080

# MySQL
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=your_database

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
```

---

## 📡 API Endpoints

Base URL: `http://localhost:8080/api`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/products` | Get all products (paginated) |
| GET | `/products/:id` | Get single product |
| POST | `/products` | Add new product |
| PUT | `/products/:id` | Update product |
| DELETE | `/products/:id` | Delete product |

---

## 📄 Pagination

By default, the API returns **10 products per page**.

```
# Page 1 (products 1-10)
GET /api/products?page=1

# Page 2 (products 11 onwards)
GET /api/products?page=2
```

### Sample Response

```json
{
  "success": true,
  "page": 1,
  "totalPages": 2,
  "totalProducts": 15,
  "data": [ ... ]
}
```

---

## ⚡ How Redis Caching Works Here

```
Client Request
      |
      ▼
Check Redis Cache
      |
   ┌──┴──┐
   Hit   Miss
   |       |
Return   Fetch from MySQL
cached     |
data    Store in Redis
           |
        Return data
```

- **Cache Hit** → Data served directly from Redis (super fast ⚡)
- **Cache Miss** → Data fetched from MySQL, then stored in Redis for next time

---

## 🧪 Testing

All APIs have been tested using **Postman**.

Import the Postman collection (if added) or manually test using the endpoints listed above.

---

## 📌 Note

This is a **learning project** — not production-ready. The goal was to understand:
1. How Redis integrates with a REST API
2. How caching reduces database load
3. How pagination works in APIs

---

## 👨‍💻 Author

**Your Name**
- GitHub: [Meharban-Ali](https://github.com/meharban-ali)

---

## 📝 License

This project is open source and available under the [MIT License](LICENSE).
