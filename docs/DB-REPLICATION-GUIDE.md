# MySQL Master-Slave Replication 구성 가이드

Spring Boot + Kotlin 환경에서 MySQL Master-Slave Replication을 구성하여 읽기/쓰기 작업을 분산하는 완벽한 가이드입니다.

## 목차

1. [개요](#개요)
2. [아키텍처](#아키텍처)
3. [구성 요소](#구성-요소)
4. [설치 및 설정](#설치-및-설정)
5. [사용 방법](#사용-방법)
6. [고급 기능](#고급-기능)
7. [트러블슈팅](#트러블슈팅)
8. [성능 최적화](#성능-최적화)
9. [FAQ](#faq)

---

## 개요

### Master-Slave Replication이란?

Master-Slave Replication은 데이터베이스 확장성과 가용성을 높이기 위한 아키텍처 패턴입니다.

**핵심 개념:**
- **Master (Primary)**: 모든 쓰기 작업(INSERT, UPDATE, DELETE)을 처리
- **Slave (Replica)**: 모든 읽기 작업(SELECT)을 처리하며, Master의 데이터를 실시간 복제

### 주요 장점

1. **읽기 성능 향상**
   - 읽기 작업을 여러 Slave DB로 분산
   - Master DB의 부하 감소

2. **확장성(Scalability)**
   - Slave를 수평적으로 확장 가능
   - 트래픽 증가에 유연하게 대응

3. **고가용성(High Availability)**
   - Master 장애 시 Slave를 Master로 승격 가능
   - 데이터 백업 용도로도 활용

4. **투명한 라우팅**
   - `@Transactional(readOnly = true)`만으로 자동 Slave 라우팅
   - 기존 코드 변경 최소화

---

## 아키텍처

### 전체 구조

```
┌─────────────────────────────────────────────────┐
│           Spring Boot Application              │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │     Service Layer                        │  │
│  │  @Transactional(readOnly=true/false)     │  │
│  └──────────────┬───────────────────────────┘  │
│                 │                               │
│  ┌──────────────▼───────────────────────────┐  │
│  │  LazyConnectionDataSourceProxy           │  │
│  │  (실제 쿼리 실행 시점에 Connection 획득)  │  │
│  └──────────────┬───────────────────────────┘  │
│                 │                               │
│  ┌──────────────▼───────────────────────────┐  │
│  │       RoutingDataSource                  │  │
│  │  (readOnly 속성에 따라 DataSource 선택)   │  │
│  └──────────┬────────────────┬──────────────┘  │
│             │                │                  │
│    ┌────────▼─────┐   ┌─────▼──────────┐      │
│    │    Master    │   │     Slave      │      │
│    │  DataSource  │   │   DataSource   │      │
│    └────────┬─────┘   └─────┬──────────┘      │
└─────────────┼─────────────────┼─────────────────┘
              │                 │
         ┌────▼────┐      ┌────▼────┐
         │ Master  │─────▶│  Slave  │
         │   DB    │ 복제  │   DB    │
         │ :3306   │      │ :3307   │
         └─────────┘      └─────────┘
```

### 데이터 흐름

#### 쓰기 작업 (INSERT/UPDATE/DELETE)
```kotlin
@Transactional  // readOnly = false (기본값)
fun createUser(user: User): User {
    return userRepository.save(user)
}
```
```
Service → LazyProxy → RoutingDataSource
  → readOnly=false 감지 → Master DataSource 선택
  → Master DB (3306) 실행
```

#### 읽기 작업 (SELECT)
```kotlin
@Transactional(readOnly = true)
fun findUser(id: Long): User? {
    return userRepository.findById(id).orElse(null)
}
```
```
Service → LazyProxy → RoutingDataSource
  → readOnly=true 감지 → Slave DataSource 선택
  → Slave DB (3307) 실행
```

---

## 구성 요소

### 1. DataSourceType (Enum)

**위치**: `src/main/kotlin/com/example/kotlin/config/datasource/DataSourceType.kt`

```kotlin
enum class DataSourceType {
    MASTER,  // 쓰기 작업용
    SLAVE    // 읽기 작업용
}
```

**역할**: DataSource를 구분하는 키로 사용

---

### 2. RoutingDataSource

**위치**: `src/main/kotlin/com/example/kotlin/config/datasource/RoutingDataSource.kt`

```kotlin
class RoutingDataSource : AbstractRoutingDataSource() {
    override fun determineCurrentLookupKey(): Any {
        val isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
        return if (isReadOnly) DataSourceType.SLAVE else DataSourceType.MASTER
    }
}
```

**핵심 메서드**:
- `determineCurrentLookupKey()`: 현재 트랜잭션 컨텍스트를 확인하여 사용할 DataSource 결정
- `TransactionSynchronizationManager.isCurrentTransactionReadOnly()`:
  - `true` → Slave 선택
  - `false` → Master 선택

**동작 시점**:
- LazyConnectionDataSourceProxy와 함께 사용 시 **실제 SQL 실행 직전**에 호출
- 트랜잭션 시작 시점이 아닌 **쿼리 실행 시점**에 결정되므로 정확한 라우팅 가능

---

### 3. DataSourceConfig

**위치**: `src/main/kotlin/com/example/kotlin/config/datasource/DataSourceConfig.kt`

```kotlin
@Configuration
class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master.hikari")
    fun masterDataSource(): DataSource { ... }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave.hikari")
    fun slaveDataSource(): DataSource { ... }

    @Bean
    fun routingDataSource(
        @Qualifier("masterDataSource") masterDataSource: DataSource,
        @Qualifier("slaveDataSource") slaveDataSource: DataSource
    ): DataSource {
        val routingDataSource = RoutingDataSource()
        val dataSourceMap = mapOf<Any, Any>(
            DataSourceType.MASTER to masterDataSource,
            DataSourceType.SLAVE to slaveDataSource
        )
        routingDataSource.setTargetDataSources(dataSourceMap)
        routingDataSource.setDefaultTargetDataSource(masterDataSource)
        return routingDataSource
    }

    @Primary
    @Bean
    fun dataSource(@Qualifier("routingDataSource") routingDataSource: DataSource): DataSource {
        return LazyConnectionDataSourceProxy(routingDataSource)
    }
}
```

**Bean 구성 순서**:
1. `masterDataSource` 생성 (HikariCP)
2. `slaveDataSource` 생성 (HikariCP)
3. `routingDataSource` 생성 (Master/Slave 등록)
4. `LazyConnectionDataSourceProxy`로 감싸서 Primary DataSource 등록

**LazyConnectionDataSourceProxy를 사용하는 이유**:
- 트랜잭션 시작 시점이 아닌 **실제 쿼리 실행 시점**에 Connection 획득
- `@Transactional` AOP가 먼저 실행되고, 그 다음에 DataSource 결정
- AOP Proxy 순서 문제 해결

---

### 4. @ForceMaster 어노테이션

**위치**: `src/main/kotlin/com/example/kotlin/annotation/ForceMaster.kt`

```kotlin
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ForceMaster
```

**사용 목적**:
- Replication Lag 문제 해결
- Master에 쓴 직후 바로 읽어야 하는 경우
- 일관성이 중요한 읽기 작업

**예시**:
```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createOrder(order: Order): Order {
        // Master에 주문 생성
        return orderRepository.save(order)
    }

    // 주문 생성 직후 조회 시 Replication Lag으로 인해
    // Slave에서 조회 시 데이터가 없을 수 있음
    @ForceMaster  // ← 강제로 Master 사용
    @Transactional(readOnly = true)
    fun getOrderImmediately(orderId: Long): Order? {
        return orderRepository.findById(orderId).orElse(null)
    }
}
```

---

### 5. DataSourceAspect

**위치**: `src/main/kotlin/com/example/kotlin/aspect/DataSourceAspect.kt`

```kotlin
@Aspect
@Component
@Order(0)  // 트랜잭션 AOP보다 먼저 실행
class DataSourceAspect {

    @Around("@annotation(com.example.kotlin.annotation.ForceMaster)")
    fun forceMaster(joinPoint: ProceedingJoinPoint): Any? {
        val wasReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()

        return try {
            // readOnly를 false로 강제 설정 → Master 사용
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)
            joinPoint.proceed()
        } finally {
            // 원래 상태로 복구
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(wasReadOnly)
        }
    }
}
```

**동작 원리**:
1. `@ForceMaster`가 붙은 메서드 실행 전 가로채기
2. 트랜잭션 readOnly 속성을 `false`로 강제 설정
3. RoutingDataSource가 `readOnly=false`를 감지하여 **Master 선택**
4. 메서드 실행 후 원래 상태로 복구

**Order 우선순위**:
- `@Order(0)`: 트랜잭션 AOP보다 먼저 실행
- Spring의 `@Transactional`은 기본적으로 `Ordered.LOWEST_PRECEDENCE`

---

## 설치 및 설정

### 1. Docker Compose로 MySQL Master-Slave 구성

#### docker-compose.yml

```yaml
version: '3.8'

services:
  mysql-master:
    image: mysql:8.0
    container_name: kotlin-practice-mysql-master
    environment:
      MYSQL_ROOT_PASSWORD: root1234
      MYSQL_DATABASE: kotlin_practice
    ports:
      - "3306:3306"
    volumes:
      - mysql_master_data:/var/lib/mysql
      - ./docker/mysql/master/my.cnf:/etc/mysql/my.cnf
    command:
      - --server-id=1
      - --log-bin=mysql-bin
      - --binlog-format=ROW
      - --gtid-mode=ON
      - --enforce-gtid-consistency=ON

  mysql-slave:
    image: mysql:8.0
    container_name: kotlin-practice-mysql-slave
    environment:
      MYSQL_ROOT_PASSWORD: root1234
      MYSQL_DATABASE: kotlin_practice
    ports:
      - "3307:3306"
    volumes:
      - mysql_slave_data:/var/lib/mysql
      - ./docker/mysql/slave/my.cnf:/etc/mysql/my.cnf
    command:
      - --server-id=2
      - --relay-log=relay-log
      - --skip-slave-start
    depends_on:
      - mysql-master

volumes:
  mysql_master_data:
  mysql_slave_data:

networks:
  mysql-network:
```

#### 주요 설정 옵션

| 옵션 | Master | Slave | 설명 |
|------|--------|-------|------|
| `server-id` | 1 | 2 | 복제 그룹 내 고유 식별자 |
| `log-bin` | ✅ | ✅ | 바이너리 로그 활성화 |
| `binlog-format` | ROW | ROW | 행 기반 복제 (권장) |
| `gtid-mode` | ON | ON | Global Transaction ID 사용 |
| `relay-log` | ❌ | ✅ | Slave의 릴레이 로그 |
| `skip-slave-start` | ❌ | ✅ | 자동 복제 시작 방지 (수동 설정) |

---

### 2. MySQL 컨테이너 시작

```bash
# 컨테이너 시작
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f mysql-master
docker-compose logs -f mysql-slave
```

---

### 3. Replication 초기화

#### 자동 초기화 스크립트 실행

```bash
# 실행 권한 부여
chmod +x docker/mysql/init-replication.sh

# Replication 설정 실행
./docker/mysql/init-replication.sh
```

#### 수동 설정 (상세)

**Step 1: Master에서 Replication 사용자 생성**

```bash
docker exec -it kotlin-practice-mysql-master mysql -uroot -proot1234
```

```sql
-- Replication 전용 사용자 생성
CREATE USER 'repl'@'%' IDENTIFIED BY 'repl1234';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;

-- Master 상태 확인
SHOW MASTER STATUS;
```

**출력 예시:**
```
+------------------+----------+--------------+------------------+
| File             | Position | Binlog_Do_DB | Binlog_Ignore_DB |
+------------------+----------+--------------+------------------+
| mysql-bin.000003 |      157 |              |                  |
+------------------+----------+--------------+------------------+
```

**Step 2: Slave 설정**

```bash
docker exec -it kotlin-practice-mysql-slave mysql -uroot -proot1234
```

```sql
-- Slave 중지
STOP SLAVE;

-- Master 정보 설정 (GTID 사용)
CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_USER='repl',
    MASTER_PASSWORD='repl1234',
    MASTER_AUTO_POSITION=1;

-- Slave 시작
START SLAVE;

-- Replication 상태 확인
SHOW SLAVE STATUS\G
```

**확인 포인트:**
```
Slave_IO_Running: Yes      ← IO Thread 실행 중
Slave_SQL_Running: Yes     ← SQL Thread 실행 중
Seconds_Behind_Master: 0   ← Lag 시간 (0에 가까울수록 좋음)
Last_IO_Error:             ← 에러 없어야 함
Last_SQL_Error:            ← 에러 없어야 함
```

---

### 4. Replication 동작 테스트

#### Master에서 데이터 삽입

```sql
-- Master DB 접속
docker exec -it kotlin-practice-mysql-master mysql -uroot -proot1234 kotlin_practice

-- 테스트 테이블 생성
CREATE TABLE test_replication (
    id INT PRIMARY KEY AUTO_INCREMENT,
    message VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 데이터 삽입
INSERT INTO test_replication (message) VALUES ('Hello from Master!');
```

#### Slave에서 복제 확인

```sql
-- Slave DB 접속
docker exec -it kotlin-practice-mysql-slave mysql -uroot -proot1234 kotlin_practice

-- 데이터 확인 (Master에서 삽입한 데이터가 보여야 함)
SELECT * FROM test_replication;
```

**예상 결과:**
```
+----+--------------------+---------------------+
| id | message            | created_at          |
+----+--------------------+---------------------+
|  1 | Hello from Master! | 2025-01-15 10:30:00 |
+----+--------------------+---------------------+
```

---

### 5. application.yml 설정

```yaml
spring:
  datasource:
    # Master DataSource (쓰기 작업용)
    master:
      hikari:
        jdbc-url: jdbc:mysql://localhost:3306/kotlin_practice?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
        username: root
        password: root1234
        driver-class-name: com.mysql.cj.jdbc.Driver
        maximum-pool-size: 10      # Master 풀 크기
        minimum-idle: 5
        connection-timeout: 30000   # 30초
        idle-timeout: 600000        # 10분
        max-lifetime: 1800000       # 30분
        pool-name: MasterHikariPool

    # Slave DataSource (읽기 작업용)
    slave:
      hikari:
        jdbc-url: jdbc:mysql://localhost:3307/kotlin_practice?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
        username: root
        password: root1234
        driver-class-name: com.mysql.cj.jdbc.Driver
        maximum-pool-size: 20      # Slave 풀 크기 (읽기가 많으므로 크게)
        minimum-idle: 10
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        pool-name: SlaveHikariPool
        read-only: true            # Slave는 읽기 전용

  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
        show_sql: true
    open-in-view: false
```

#### HikariCP 설정 가이드

| 설정 | Master | Slave | 설명 |
|------|--------|-------|------|
| `maximum-pool-size` | 10 | 20 | 최대 Connection 수 (읽기가 많으면 Slave를 크게) |
| `minimum-idle` | 5 | 10 | 최소 유휴 Connection 수 |
| `connection-timeout` | 30000 | 30000 | Connection 획득 타임아웃 (ms) |
| `idle-timeout` | 600000 | 600000 | 유휴 Connection 제거 시간 (10분) |
| `max-lifetime` | 1800000 | 1800000 | Connection 최대 수명 (30분) |
| `read-only` | false | true | Slave는 읽기 전용 플래그 |

---

### 6. build.gradle 의존성 추가

```gradle
dependencies {
    // Spring Boot Starter
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // AOP (필수!)
    implementation 'org.springframework.boot:spring-boot-starter-aop'

    // MySQL Driver
    runtimeOnly 'com.mysql:mysql-connector-j'

    // Kotlin
    implementation 'org.jetbrains.kotlin:kotlin-reflect'
}
```

---

## 사용 방법

### 기본 사용법

#### 1. 읽기 작업 (Slave 사용)

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository
) {

    /**
     * @Transactional(readOnly = true)
     * → Slave DB에서 실행
     */
    @Transactional(readOnly = true)
    fun findUser(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    @Transactional(readOnly = true)
    fun findAllUsers(): List<User> {
        return userRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun searchUsers(keyword: String): List<User> {
        return userRepository.findByNameContaining(keyword)
    }
}
```

**실행 로그 예시:**
```
DEBUG c.e.k.c.d.RoutingDataSource - DataSource routing - readOnly: true, selected: SLAVE
Hibernate: select user0_.id as id1_0_, user0_.name as name2_0_ from user user0_ where user0_.id=?
```

---

#### 2. 쓰기 작업 (Master 사용)

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository
) {

    /**
     * @Transactional (readOnly = false가 기본값)
     * → Master DB에서 실행
     */
    @Transactional
    fun createUser(user: User): User {
        return userRepository.save(user)
    }

    @Transactional
    fun updateUser(id: Long, name: String): User {
        val user = userRepository.findById(id).orElseThrow()
        user.name = name
        return userRepository.save(user)
    }

    @Transactional
    fun deleteUser(id: Long) {
        userRepository.deleteById(id)
    }
}
```

**실행 로그 예시:**
```
DEBUG c.e.k.c.d.RoutingDataSource - DataSource routing - readOnly: false, selected: MASTER
Hibernate: insert into user (name, email, id) values (?, ?, ?)
```

---

#### 3. 혼합 작업 (읽기 + 쓰기)

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository
) {

    /**
     * 트랜잭션 내에서 읽기와 쓰기가 혼합된 경우
     * → readOnly = false (기본값)이므로 모든 작업이 Master에서 실행
     */
    @Transactional
    fun createOrder(userId: Long, productId: Long, quantity: Int): Order {
        // 모두 Master DB에서 실행
        val user = userRepository.findById(userId).orElseThrow()
        val product = productRepository.findById(productId).orElseThrow()

        val order = Order(
            user = user,
            product = product,
            quantity = quantity
        )

        return orderRepository.save(order)
    }
}
```

**주의사항**:
- 하나의 트랜잭션 내에서는 **하나의 DataSource**만 사용됨
- `@Transactional`이 `readOnly=false`면 모든 쿼리가 Master에서 실행
- 읽기와 쓰기를 분리하려면 **별도의 메서드**로 나누고 각각 트랜잭션 설정

---

### 고급 사용법

#### 1. @ForceMaster로 Master 강제 사용

**시나리오**: 주문 생성 직후 조회 시 Replication Lag 문제

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository
) {

    @Transactional
    fun createOrder(order: Order): Order {
        // Master에 저장
        return orderRepository.save(order)
    }

    /**
     * 문제: 주문 생성 직후 조회 시 Slave에서 조회하면
     * Replication Lag으로 인해 데이터가 아직 없을 수 있음
     *
     * 해결: @ForceMaster로 Master에서 직접 조회
     */
    @ForceMaster  // ← 강제로 Master 사용
    @Transactional(readOnly = true)
    fun getOrderImmediately(orderId: Long): Order? {
        return orderRepository.findById(orderId).orElse(null)
    }

    /**
     * 일반 조회는 Slave 사용 (시간이 지나 Replication 완료됨)
     */
    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): Order? {
        return orderRepository.findById(orderId).orElse(null)
    }
}
```

**사용 예시:**
```kotlin
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): OrderResponse {
        val order = orderService.createOrder(request.toEntity())

        // 생성 직후 바로 조회 (Master 사용)
        val savedOrder = orderService.getOrderImmediately(order.id!!)

        return OrderResponse.from(savedOrder!!)
    }
}
```

---

#### 2. 복잡한 읽기 작업 분리

```kotlin
@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * 단순 조회: Slave 사용
     */
    @Transactional(readOnly = true)
    fun getProduct(id: Long): Product? {
        return productRepository.findById(id).orElse(null)
    }

    /**
     * 복잡한 집계 쿼리: Slave 사용
     * (Master 부하를 줄이기 위해)
     */
    @Transactional(readOnly = true)
    fun getProductStatistics(categoryId: Long): ProductStatistics {
        val products = productRepository.findByCategoryId(categoryId)

        return ProductStatistics(
            totalCount = products.size,
            averagePrice = products.map { it.price }.average(),
            maxPrice = products.maxByOrNull { it.price }?.price ?: 0
        )
    }
}
```

---

#### 3. QueryDSL과 함께 사용

```kotlin
@Repository
class ProductRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ProductRepositoryCustom {

    /**
     * QueryDSL 쿼리도 트랜잭션 컨텍스트를 따름
     *
     * Service에서 @Transactional(readOnly = true)로 호출하면
     * → Slave DB에서 실행
     */
    override fun findProductsWithCategory(keyword: String): List<Product> {
        return queryFactory
            .selectFrom(product)
            .join(product.category, category).fetchJoin()
            .where(product.name.contains(keyword))
            .fetch()
    }
}

@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    @Transactional(readOnly = true)  // ← Slave 사용
    fun searchProducts(keyword: String): List<Product> {
        return productRepository.findProductsWithCategory(keyword)
    }
}
```

---

#### 4. 다중 Slave 로드밸런싱 (확장)

여러 Slave가 있을 때 Round-Robin 방식으로 분산:

```kotlin
class RoutingDataSource : AbstractRoutingDataSource() {

    private val slaveIndex = AtomicInteger(0)
    private val slaveCount = 3  // Slave 3대

    override fun determineCurrentLookupKey(): Any {
        return if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            // Round-Robin으로 Slave 선택
            val index = slaveIndex.getAndIncrement() % slaveCount
            "SLAVE_$index"  // SLAVE_0, SLAVE_1, SLAVE_2 순환
        } else {
            DataSourceType.MASTER
        }
    }
}
```

**DataSourceConfig 수정:**
```kotlin
@Bean
fun routingDataSource(
    @Qualifier("masterDataSource") masterDataSource: DataSource,
    @Qualifier("slave1DataSource") slave1: DataSource,
    @Qualifier("slave2DataSource") slave2: DataSource,
    @Qualifier("slave3DataSource") slave3: DataSource
): DataSource {
    val routingDataSource = RoutingDataSource()

    val dataSourceMap = mapOf<Any, Any>(
        DataSourceType.MASTER to masterDataSource,
        "SLAVE_0" to slave1,
        "SLAVE_1" to slave2,
        "SLAVE_2" to slave3
    )

    routingDataSource.setTargetDataSources(dataSourceMap)
    routingDataSource.setDefaultTargetDataSource(masterDataSource)

    return routingDataSource
}
```

---

## 트러블슈팅

### 1. Replication이 동작하지 않음

#### 증상
```sql
SHOW SLAVE STATUS\G

Slave_IO_Running: No
Slave_SQL_Running: No
Last_IO_Error: error connecting to master
```

#### 원인 및 해결

**원인 1: Master 접속 정보 오류**
```sql
-- Master 호스트명 확인
SHOW VARIABLES LIKE 'hostname';

-- Slave 설정 재확인
CHANGE MASTER TO
    MASTER_HOST='mysql-master',  -- Docker 컨테이너명 또는 IP
    MASTER_USER='repl',
    MASTER_PASSWORD='repl1234',
    MASTER_AUTO_POSITION=1;

START SLAVE;
```

**원인 2: 네트워크 문제 (Docker)**
```bash
# 같은 Docker 네트워크에 있는지 확인
docker network inspect kotlin-practice_mysql-network

# Master에 Slave에서 접속 테스트
docker exec -it kotlin-practice-mysql-slave mysql -h mysql-master -u repl -prepl1234
```

**원인 3: Firewall/포트 문제**
```bash
# Master 포트 확인
docker exec -it kotlin-practice-mysql-master netstat -tuln | grep 3306

# Slave에서 telnet 테스트
docker exec -it kotlin-practice-mysql-slave telnet mysql-master 3306
```

---

### 2. Replication Lag (복제 지연)

#### 증상
```sql
SHOW SLAVE STATUS\G

Seconds_Behind_Master: 120  -- 2분 지연
```

#### 원인 및 해결

**원인 1: Master의 쓰기 부하가 높음**
```sql
-- Master의 바이너리 로그 크기 확인
SHOW BINARY LOGS;

-- 대량 INSERT/UPDATE는 배치로 나눠서 실행
-- 예: 10000건 → 1000건씩 10번
```

**원인 2: Slave의 리소스 부족**
```bash
# Slave 리소스 모니터링
docker stats kotlin-practice-mysql-slave

# Docker Compose에서 리소스 제한 늘리기
deploy:
  resources:
    limits:
      cpus: '2.0'
      memory: 4G
```

**원인 3: 네트워크 대역폭 부족**
```yaml
# Binlog 압축 활성화 (MySQL 8.0.20+)
command:
  - --binlog-transaction-compression=ON
```

**모니터링 쿼리:**
```sql
-- Replication 상태 계속 확인
WATCH "docker exec -i kotlin-practice-mysql-slave mysql -uroot -proot1234 -e 'SHOW SLAVE STATUS\G' | grep -E 'Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master'"
```

---

### 3. DataSource 라우팅이 안됨

#### 증상
```
readOnly = true인데도 Master DB로 쿼리가 가는 경우
```

#### 원인 및 해결

**원인 1: LazyConnectionDataSourceProxy 없음**
```kotlin
// ❌ 잘못된 설정
@Primary
@Bean
fun dataSource(...): DataSource {
    return routingDataSource(...)  // Lazy 없이 바로 반환
}

// ✅ 올바른 설정
@Primary
@Bean
fun dataSource(...): DataSource {
    return LazyConnectionDataSourceProxy(routingDataSource(...))
}
```

**원인 2: @Transactional 누락**
```kotlin
// ❌ 트랜잭션이 없으면 기본값(Master) 사용
fun findUser(id: Long): User? {
    return userRepository.findById(id).orElse(null)
}

// ✅ readOnly=true 명시
@Transactional(readOnly = true)
fun findUser(id: Long): User? {
    return userRepository.findById(id).orElse(null)
}
```

**원인 3: AOP Proxy 문제**
```kotlin
// ❌ 같은 클래스 내부 호출은 AOP 적용 안됨
@Service
class UserService {
    fun publicMethod() {
        privateReadMethod()  // ← @Transactional 적용 안됨!
    }

    @Transactional(readOnly = true)
    private fun privateReadMethod() { ... }
}

// ✅ 별도 Service로 분리하거나 외부에서 호출
```

---

### 4. Connection Pool 부족

#### 증상
```
HikariPool-1 - Connection is not available, request timed out after 30000ms.
```

#### 해결

**방법 1: Pool 크기 증가**
```yaml
spring:
  datasource:
    master:
      hikari:
        maximum-pool-size: 20  # 10 → 20으로 증가
    slave:
      hikari:
        maximum-pool-size: 40  # 20 → 40으로 증가
```

**방법 2: Connection 누수 확인**
```kotlin
// ❌ 트랜잭션이 너무 길면 Connection 점유 시간 증가
@Transactional
fun longRunningMethod() {
    // 10초 걸리는 외부 API 호출 (Connection 계속 점유)
    externalApiService.call()

    // DB 작업
    userRepository.save(user)
}

// ✅ 트랜잭션 최소화
fun longRunningMethod() {
    // 외부 API 호출 (트랜잭션 밖)
    val result = externalApiService.call()

    // DB 작업만 트랜잭션으로 묶음
    saveUser(result)
}

@Transactional
private fun saveUser(data: Data) {
    userRepository.save(user)
}
```

**방법 3: Connection Timeout 조정**
```yaml
spring:
  datasource:
    master:
      hikari:
        connection-timeout: 60000  # 30초 → 60초
```

---

### 5. @ForceMaster가 동작하지 않음

#### 증상
```
@ForceMaster를 붙였는데도 Slave로 쿼리가 가는 경우
```

#### 해결

**원인 1: AOP 의존성 누락**
```gradle
// build.gradle에 추가 필수!
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

**원인 2: @EnableAspectJAutoProxy 누락 (Spring Boot는 자동이지만 확인)**
```kotlin
@Configuration
@EnableAspectJAutoProxy
class AopConfig
```

**원인 3: Order 우선순위 문제**
```kotlin
@Aspect
@Component
@Order(0)  // ← 반드시 추가! 트랜잭션보다 먼저 실행되어야 함
class DataSourceAspect { ... }
```

**디버깅:**
```kotlin
@Aspect
@Component
@Order(0)
class DataSourceAspect {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Around("@annotation(com.example.kotlin.annotation.ForceMaster)")
    fun forceMaster(joinPoint: ProceedingJoinPoint): Any? {
        log.info("@ForceMaster AOP triggered!")  // ← 로그 확인

        val wasReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
        log.info("Before: readOnly = {}", wasReadOnly)

        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)
        log.info("After: readOnly = false (forced)")

        return joinPoint.proceed()
    }
}
```

---

## 성능 최적화

### 1. HikariCP 튜닝

#### 최적 설정 가이드

```yaml
spring:
  datasource:
    master:
      hikari:
        # Connection Pool 크기
        maximum-pool-size: 10        # CPU 코어 수 * 2 + 디스크 수
        minimum-idle: 5              # maximum-pool-size의 50%

        # Timeout 설정
        connection-timeout: 30000    # 30초 (기본값)
        idle-timeout: 600000         # 10분
        max-lifetime: 1800000        # 30분

        # 성능 최적화
        auto-commit: false           # 명시적 트랜잭션 사용

    slave:
      hikari:
        maximum-pool-size: 20        # 읽기가 많으면 Master보다 크게
        minimum-idle: 10
```

#### Pool 크기 계산 공식

```
최적 Pool 크기 = (CPU 코어 수 * 2) + 디스크 수

예시:
- 4 Core CPU, SSD 1개 → (4 * 2) + 1 = 9 ≈ 10
- 8 Core CPU, SSD 2개 → (8 * 2) + 2 = 18 ≈ 20
```

**주의**: Pool이 크다고 무조건 좋은 것은 아님!
- 너무 크면: Context Switching 증가, 메모리 낭비
- 너무 작으면: Connection 대기 시간 증가

---

### 2. Read-Write 비율에 따른 Pool 크기 조정

```yaml
# 시나리오 1: 읽기:쓰기 = 7:3
spring:
  datasource:
    master:
      hikari:
        maximum-pool-size: 10
    slave:
      hikari:
        maximum-pool-size: 20

# 시나리오 2: 읽기:쓰기 = 9:1 (조회가 압도적으로 많음)
spring:
  datasource:
    master:
      hikari:
        maximum-pool-size: 5
    slave:
      hikari:
        maximum-pool-size: 30
```

---

### 3. 쿼리 최적화

#### N+1 문제 해결

```kotlin
// ❌ N+1 문제
@Transactional(readOnly = true)
fun getAllPosts(): List<Post> {
    return postRepository.findAll()  // 1번
        .map { post ->
            post.comments.size  // N번 (각 Post마다 쿼리 발생)
            post
        }
}

// ✅ Fetch Join으로 해결
@Query("SELECT p FROM Post p LEFT JOIN FETCH p.comments")
fun findAllWithComments(): List<Post>

@Transactional(readOnly = true)
fun getAllPosts(): List<Post> {
    return postRepository.findAllWithComments()  // 1번만!
}
```

---

### 4. Slave 모니터링 및 자동 Failover

#### Slave 상태 Health Check

```kotlin
@Component
class ReplicationHealthIndicator(
    @Qualifier("slaveDataSource") private val slaveDataSource: DataSource
) : HealthIndicator {

    override fun health(): Health {
        return try {
            slaveDataSource.connection.use { conn ->
                val stmt = conn.createStatement()
                val rs = stmt.executeQuery("SHOW SLAVE STATUS")

                if (rs.next()) {
                    val ioRunning = rs.getString("Slave_IO_Running") == "Yes"
                    val sqlRunning = rs.getString("Slave_SQL_Running") == "Yes"
                    val secondsBehind = rs.getInt("Seconds_Behind_Master")

                    if (ioRunning && sqlRunning && secondsBehind < 10) {
                        Health.up()
                            .withDetail("lag", "$secondsBehind seconds")
                            .build()
                    } else {
                        Health.down()
                            .withDetail("io_running", ioRunning)
                            .withDetail("sql_running", sqlRunning)
                            .withDetail("lag", secondsBehind)
                            .build()
                    }
                } else {
                    Health.unknown().build()
                }
            }
        } catch (e: Exception) {
            Health.down(e).build()
        }
    }
}
```

**Health Check 엔드포인트:**
```bash
curl http://localhost:8080/actuator/health

{
  "status": "UP",
  "components": {
    "replicationHealth": {
      "status": "UP",
      "details": {
        "lag": "2 seconds"
      }
    }
  }
}
```

---

### 5. 캐시 활용으로 Slave 부하 감소

```kotlin
@Service
class ProductService(
    private val productRepository: ProductRepository
) {

    /**
     * 자주 조회되는 데이터는 캐시 사용
     * → Slave 부하 감소
     */
    @Cacheable("products")
    @Transactional(readOnly = true)
    fun getProduct(id: Long): Product? {
        return productRepository.findById(id).orElse(null)
    }

    @CacheEvict("products", key = "#product.id")
    @Transactional
    fun updateProduct(product: Product): Product {
        return productRepository.save(product)
    }
}
```

**캐시 설정:**
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
```

---

## FAQ

### Q1. 트랜잭션 없이도 Slave를 사용할 수 있나요?

**A**: 기본적으로 트랜잭션이 없으면 Master를 사용합니다. Slave를 사용하려면 `@Transactional(readOnly = true)` 필수입니다.

```kotlin
// ❌ Master 사용
fun findUser(id: Long) = userRepository.findById(id)

// ✅ Slave 사용
@Transactional(readOnly = true)
fun findUser(id: Long) = userRepository.findById(id)
```

---

### Q2. Replication Lag는 얼마나 발생하나요?

**A**:
- **일반적인 환경**: 1초 이하
- **대량 쓰기 발생 시**: 수 초 ~ 수십 초
- **네트워크 불안정**: 수 분

**모니터링 방법:**
```sql
SHOW SLAVE STATUS\G
-- Seconds_Behind_Master 확인
```

**대응 방법:**
- 중요한 데이터: `@ForceMaster`로 Master 사용
- 실시간성이 덜 중요한 데이터: Slave 사용 (통계, 대시보드 등)

---

### Q3. Slave가 다운되면 어떻게 되나요?

**A**: Slave 연결 실패 시 Exception 발생

**Fallback 전략 구현:**
```kotlin
class RoutingDataSource : AbstractRoutingDataSource() {

    override fun determineCurrentLookupKey(): Any {
        val isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly()

        return if (isReadOnly) {
            try {
                // Slave 상태 확인
                if (isSlaveAvailable()) {
                    DataSourceType.SLAVE
                } else {
                    log.warn("Slave is down, fallback to Master")
                    DataSourceType.MASTER
                }
            } catch (e: Exception) {
                DataSourceType.MASTER
            }
        } else {
            DataSourceType.MASTER
        }
    }

    private fun isSlaveAvailable(): Boolean {
        // Health Check 로직
        return true
    }
}
```

---

### Q4. 여러 Slave를 추가하려면?

**A**: `DataSourceConfig`에 Slave를 추가하고 로드밸런싱 로직 구현

```kotlin
@Bean
fun routingDataSource(...): DataSource {
    val routingDataSource = RoutingDataSource()

    val dataSourceMap = mapOf<Any, Any>(
        DataSourceType.MASTER to masterDataSource,
        "SLAVE_0" to slave1DataSource,
        "SLAVE_1" to slave2DataSource,
        "SLAVE_2" to slave3DataSource
    )

    routingDataSource.setTargetDataSources(dataSourceMap)
    return routingDataSource
}
```

**Round-Robin 구현은 "고급 사용법 > 다중 Slave 로드밸런싱" 참고**

---

### Q5. Master-Master 구성은 지원하나요?

**A**: 현재 구현은 Master-Slave 전용입니다. Master-Master(다중 쓰기)는:
- 데이터 충돌 위험
- 트랜잭션 일관성 보장 어려움
- 일반적으로 권장되지 않음

**대안:**
- **Active-Standby**: Master 1대 + Standby Master 1대 (장애 대비)
- **Sharding**: 데이터를 여러 Master로 분산

---

### Q6. 프로덕션 환경에서 주의할 점은?

**A**:
1. **Replication 모니터링 필수**
   - `Seconds_Behind_Master` 지속 확인
   - 알림 설정 (Slack, PagerDuty 등)

2. **Slave 백업**
   - Slave가 다운되면 Master에 부하 집중
   - Slave 2대 이상 권장

3. **DDL 주의**
   ```sql
   -- Master에서 ALTER TABLE 실행 시
   -- Replication Lag 급증 가능
   ALTER TABLE users ADD COLUMN new_field VARCHAR(100);
   ```
   - 야간 배포 권장
   - Replication 상태 확인 후 진행

4. **Transaction Timeout 설정**
   ```yaml
   spring:
     transaction:
       default-timeout: 30  # 30초
   ```

---

## 마무리

이 가이드를 통해 Spring Boot + Kotlin 환경에서 MySQL Master-Slave Replication을 완벽하게 구성할 수 있습니다.

**핵심 요약:**
- ✅ `@Transactional(readOnly = true)` → Slave 자동 사용
- ✅ `@Transactional` → Master 자동 사용
- ✅ `@ForceMaster` → Replication Lag 문제 해결
- ✅ `LazyConnectionDataSourceProxy` → 정확한 라우팅
- ✅ HikariCP 튜닝 → 성능 최적화

**다음 단계:**
1. Docker Compose로 로컬 환경 구축
2. 테스트 데이터로 Replication 확인
3. 실제 Service에 `@Transactional(readOnly = true)` 적용
4. 모니터링 설정 (Replication Lag, Pool 상태)
5. 프로덕션 배포 전 부하 테스트

Happy Coding! 🚀
