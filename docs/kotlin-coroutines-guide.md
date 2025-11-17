# Kotlin Coroutines 완벽 가이드

## 개요

Kotlin Coroutines는 비동기 프로그래밍을 간결하고 직관적으로 작성할 수 있게 해주는 경량 스레드입니다.
콜백 지옥(Callback Hell)을 피하고, 순차적인 코드로 비동기 작업을 표현할 수 있습니다.

## 핵심 개념

### 1. 코루틴이란?

```
일반 스레드:
Thread 1 ━━━━━━━━━━━━━━━━━━━━━━━━━━━ (무거움, OS 리소스 소모)
Thread 2 ━━━━━━━━━━━━━━━━━━━━━━━━━━━
Thread 3 ━━━━━━━━━━━━━━━━━━━━━━━━━━━

코루틴:
Thread 1: [코루틴1]──[중단]───────[재개]──[완료]
          └─[코루틴2]──[중단]──[재개]──[완료]
              └─[코루틴3]──[중단]──[재개]──[완료]

⚡ 수만 개의 코루틴을 소수의 스레드에서 실행 가능
```

### 2. 주요 특징

| 특징 | 설명 | 예시 |
|------|------|------|
| **경량성** | 수만 개의 코루틴을 동시 실행 가능 | 10만 개 코루틴 = 메모리 수 MB |
| **구조화된 동시성** | 부모-자식 관계로 생명주기 관리 | 부모 취소 → 모든 자식 취소 |
| **순차적 코드** | 비동기를 동기 코드처럼 작성 | `val result = async { }.await()` |
| **예외 처리** | 일반 try-catch로 예외 처리 | `try { delay(1000) } catch (e: Exception)` |

## 코루틴 빌더

### 1. launch - Fire and Forget

실행 후 결과를 기다리지 않는 코루틴 시작 (반환값 없음)

```kotlin
fun main() = runBlocking {
    // 백그라운드에서 실행
    launch {
        delay(1000L)
        println("World!")  // 1초 후 출력
    }

    println("Hello,")  // 즉시 출력
    delay(2000L)  // 2초 대기 (코루틴 완료 기다림)
}

// 출력 순서:
// Hello,
// World!
```

**사용 사례:**
- 로깅, 이벤트 발행, 백그라운드 작업
- 결과가 필요 없는 비동기 작업

```kotlin
// 실전 예제: 사용자 생성 후 이메일 발송
fun createUser(user: User) = runBlocking {
    userRepository.save(user)

    // 이메일 발송은 별도로 실행 (결과 기다리지 않음)
    launch {
        emailService.sendWelcomeEmail(user.email)
    }

    // 즉시 반환
}
```

### 2. async - 결과 반환

결과를 반환하는 코루틴 시작 (Deferred<T> 반환)

```kotlin
fun main() = runBlocking {
    val deferred = async {
        delay(1000L)
        "Hello, Coroutines!"
    }

    println("Waiting...")
    val result = deferred.await()  // 결과 대기
    println(result)
}

// 출력 순서:
// Waiting...
// (1초 대기)
// Hello, Coroutines!
```

**병렬 실행 예제:**

```kotlin
fun main() = runBlocking {
    val time = measureTimeMillis {
        // 두 작업을 병렬로 시작
        val one = async { fetchUserData(1) }
        val two = async { fetchUserData(2) }

        // 두 결과를 기다림
        println("User 1: ${one.await()}")
        println("User 2: ${two.await()}")
    }

    println("Completed in $time ms")
    // 총 1초 소요 (순차 실행이면 2초)
}

suspend fun fetchUserData(id: Int): String {
    delay(1000L)  // API 호출 시뮬레이션
    return "User $id"
}
```

**실전 예제: 병렬 데이터 조회**

```kotlin
suspend fun getOrderDetails(orderId: Long): OrderDetail = coroutineScope {
    // 3개의 API를 병렬로 호출
    val orderDeferred = async { orderRepository.findById(orderId) }
    val itemsDeferred = async { orderItemRepository.findByOrderId(orderId) }
    val userDeferred = async { userRepository.findById(order.userId) }

    // 모든 결과를 조합
    OrderDetail(
        order = orderDeferred.await(),
        items = itemsDeferred.await(),
        user = userDeferred.await()
    )
}
// 순차 실행: 300ms + 200ms + 150ms = 650ms
// 병렬 실행: max(300, 200, 150) = 300ms ⚡
```

### 3. runBlocking - 브릿지

일반 함수와 코루틴 세계를 연결 (main 함수, 테스트에서 사용)

```kotlin
fun main() = runBlocking {  // 메인 함수에서 코루틴 사용
    launch {
        delay(1000L)
        println("Coroutine World!")
    }
    println("Hello,")
}
```

**⚠️ 주의사항:**
- 현재 스레드를 **블로킹**함 (일반 코루틴과 다름)
- 프로덕션 코드에서는 사용 지양
- 테스트와 메인 함수에서만 사용

```kotlin
// ✅ OK: 테스트 코드
@Test
fun `코루틴 테스트`() = runBlocking {
    val result = async { fetchData() }.await()
    assertEquals("expected", result)
}

// ❌ Bad: 프로덕션 코드에서 runBlocking 사용
@Service
class UserService {
    fun getUser(id: Long): User = runBlocking {  // 스레드 블로킹!
        userRepository.findById(id)
    }
}

// ✅ Good: suspend 함수로 변경
@Service
class UserService {
    suspend fun getUser(id: Long): User {
        return userRepository.findById(id)
    }
}
```

## Suspend 함수

### 개념

**suspend** 키워드는 함수가 코루틴 내에서 실행될 수 있고, 중단(suspend) 가능함을 나타냅니다.

```kotlin
// suspend 함수 정의
suspend fun fetchData(): String {
    delay(1000L)  // 코루틴 중단 (스레드는 블로킹 안 됨)
    return "Data"
}

// ❌ 일반 함수에서 호출 불가
fun main() {
    fetchData()  // 컴파일 에러!
}

// ✅ 코루틴 내에서만 호출 가능
fun main() = runBlocking {
    val data = fetchData()  // OK
    println(data)
}
```

### delay vs Thread.sleep 비교

```kotlin
// ❌ Thread.sleep - 스레드 블로킹
Thread.sleep(1000L)  // 스레드가 1초간 아무것도 못함

// ✅ delay - 코루틴만 중단
delay(1000L)  // 코루틴만 중단, 스레드는 다른 코루틴 실행 가능
```

**시각적 비교:**

```
Thread.sleep(1000):
Thread 1: [작업1] ─── (1초 대기, 스레드 블로킹) ─── [작업2]
          다른 작업 실행 불가 ❌

delay(1000):
Thread 1: [코루틴1] ─── (중단) ─── [코루틴1 재개]
          └─[코루틴2 실행]──[코루틴2 완료]
          └─[코루틴3 실행]──[코루틴3 완료]
          다른 코루틴 실행 가능 ✅
```

### 실전 suspend 함수 패턴

```kotlin
// 패턴 1: API 호출
suspend fun fetchUser(id: Long): User {
    return withContext(Dispatchers.IO) {
        apiClient.get("/users/$id")
    }
}

// 패턴 2: 데이터베이스 조회
suspend fun findOrderById(id: Long): Order? {
    return withContext(Dispatchers.IO) {
        orderRepository.findById(id).orElse(null)
    }
}

// 패턴 3: 여러 suspend 함수 조합
suspend fun getOrderWithUser(orderId: Long): OrderWithUser {
    val order = findOrderById(orderId) ?: throw NotFoundException()
    val user = fetchUser(order.userId)
    return OrderWithUser(order, user)
}

// 패턴 4: 병렬 처리
suspend fun getUserStats(userId: Long): UserStats = coroutineScope {
    val ordersDeferred = async { orderRepository.countByUserId(userId) }
    val pointsDeferred = async { pointRepository.sumByUserId(userId) }

    UserStats(
        orderCount = ordersDeferred.await(),
        totalPoints = pointsDeferred.await()
    )
}
```

## 코루틴 스코프 (Coroutine Scope)

### 개념: 구조화된 동시성

코루틴 스코프는 코루틴의 생명주기를 관리하는 컨테이너입니다.

```
CoroutineScope
├─ 코루틴 1
│  ├─ 자식 코루틴 1-1
│  └─ 자식 코루틴 1-2
├─ 코루틴 2
└─ 코루틴 3
   └─ 자식 코루틴 3-1

규칙:
- 부모가 취소되면 모든 자식도 취소됨
- 자식이 실패하면 부모도 취소됨 (예외 전파)
- 부모는 모든 자식이 완료될 때까지 대기
```

### 주요 스코프 빌더

#### 1. coroutineScope - 순차 실행 보장

```kotlin
suspend fun loadData() = coroutineScope {
    val user = async { fetchUser() }
    val orders = async { fetchOrders() }

    // 모든 자식 완료 대기
    UserData(user.await(), orders.await())
}
// loadData()는 모든 async 완료 후 반환
```

**특징:**
- 모든 자식 코루틴이 완료될 때까지 대기
- 자식 중 하나라도 실패하면 전체 취소
- suspend 함수 내에서만 사용

```kotlin
suspend fun processOrders() = coroutineScope {
    launch { processOrder1() }  // 실패!
    launch { processOrder2() }
    launch { processOrder3() }
}
// processOrder1() 실패 → 나머지 2, 3도 모두 취소됨 ❌
```

#### 2. supervisorScope - 독립 실행

```kotlin
suspend fun loadDataSafely() = supervisorScope {
    val user = async { fetchUser() }      // 실패 가능
    val orders = async { fetchOrders() }  // 독립적으로 실행

    val userData = try { user.await() } catch (e: Exception) { null }
    val ordersData = try { orders.await() } catch (e: Exception) { emptyList() }

    UserData(userData, ordersData)
}
// fetchUser() 실패해도 fetchOrders()는 계속 실행 ✅
```

**특징:**
- 자식 중 하나가 실패해도 다른 자식들은 계속 실행
- 독립적인 작업들을 실행할 때 사용

```kotlin
// 실전 예제: 여러 API 호출 (일부 실패 허용)
suspend fun loadDashboard() = supervisorScope {
    val stats = async { fetchStats() }
    val news = async { fetchNews() }
    val weather = async { fetchWeather() }

    Dashboard(
        stats = stats.await() ?: defaultStats,
        news = news.await() ?: emptyList,
        weather = weather.await() ?: defaultWeather
    )
}
// weather API 실패해도 stats, news는 정상 표시 ✅
```

### GlobalScope (⚠️ 사용 지양)

```kotlin
// ❌ Bad: GlobalScope 사용
GlobalScope.launch {
    // 앱 전체 생명주기 동안 실행
    // 취소가 어렵고 메모리 누수 위험
}

// ✅ Good: 적절한 스코프 사용
class MyActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scope.launch {
            // 액티비티 생명주기에 맞춰 관리
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()  // 액티비티 종료 시 모든 코루틴 취소
    }
}
```

## 디스패처 (Dispatcher)

코루틴이 실행될 스레드를 결정하는 컨텍스트

### 주요 디스패처

| 디스패처 | 용도 | 예시 |
|---------|------|------|
| **Dispatchers.Main** | UI 작업 (Android/Desktop) | UI 업데이트, 버튼 클릭 처리 |
| **Dispatchers.IO** | I/O 작업 (네트워크, 파일, DB) | API 호출, 파일 읽기, DB 쿼리 |
| **Dispatchers.Default** | CPU 집약적 작업 | 이미지 처리, 정렬, 계산 |
| **Dispatchers.Unconfined** | 테스트용 (프로덕션 사용 금지) | 단위 테스트 |

### withContext로 디스패처 전환

```kotlin
suspend fun fetchAndDisplay() {
    // Main 스레드에서 시작
    showLoading()

    // IO 스레드로 전환
    val data = withContext(Dispatchers.IO) {
        apiClient.fetchData()  // 네트워크 호출
    }

    // 자동으로 Main 스레드로 복귀
    displayData(data)
    hideLoading()
}
```

**시각적 흐름:**

```
[Main Thread]
    showLoading()
    ↓
[IO Thread]
    apiClient.fetchData()  (네트워크 대기)
    ↓
[Main Thread]
    displayData(data)
    hideLoading()
```

### 실전 패턴

```kotlin
// 패턴 1: API 호출
suspend fun fetchUser(id: Long): User {
    return withContext(Dispatchers.IO) {
        apiClient.get("/users/$id")
    }
}

// 패턴 2: 파일 읽기
suspend fun readFile(path: String): String {
    return withContext(Dispatchers.IO) {
        File(path).readText()
    }
}

// 패턴 3: 무거운 계산
suspend fun processImage(bitmap: Bitmap): Bitmap {
    return withContext(Dispatchers.Default) {
        // CPU 집약적 이미지 처리
        applyFilters(bitmap)
    }
}

// 패턴 4: 복합 작업
suspend fun loadAndProcess(url: String): ProcessedData {
    // IO: 다운로드
    val rawData = withContext(Dispatchers.IO) {
        downloadData(url)
    }

    // Default: 처리
    val processed = withContext(Dispatchers.Default) {
        processData(rawData)
    }

    return processed
}
```

## 예외 처리

### 기본 예외 처리

```kotlin
// try-catch로 예외 처리
fun main() = runBlocking {
    try {
        val result = async {
            delay(100)
            throw Exception("Failed!")
        }
        result.await()  // 예외 발생
    } catch (e: Exception) {
        println("Caught: ${e.message}")
    }
}
```

### launch vs async 예외 처리 차이

```kotlin
// launch: 즉시 예외 전파
fun main() = runBlocking {
    try {
        launch {
            throw Exception("Error in launch")  // 즉시 전파
        }
    } catch (e: Exception) {
        println("Caught")  // ❌ 여기서 잡히지 않음!
    }
}

// async: await() 호출 시 예외 발생
fun main() = runBlocking {
    try {
        val deferred = async {
            throw Exception("Error in async")
        }
        deferred.await()  // ✅ 여기서 예외 발생
    } catch (e: Exception) {
        println("Caught: ${e.message}")  // ✅ 여기서 잡힘
    }
}
```

### CoroutineExceptionHandler

```kotlin
val handler = CoroutineExceptionHandler { _, exception ->
    println("Caught exception: $exception")
}

fun main() = runBlocking {
    val scope = CoroutineScope(Job() + handler)

    scope.launch {
        throw Exception("Failed!")
    }

    delay(100)  // 핸들러가 예외 처리
}
```

### 실전 예외 처리 패턴

```kotlin
// 패턴 1: 안전한 API 호출
suspend fun fetchUserSafely(id: Long): User? {
    return try {
        withContext(Dispatchers.IO) {
            apiClient.get("/users/$id")
        }
    } catch (e: Exception) {
        logger.error("Failed to fetch user $id", e)
        null
    }
}

// 패턴 2: 부분 실패 허용 (supervisorScope)
suspend fun loadDashboard(): Dashboard = supervisorScope {
    val stats = async { fetchStats() }
    val news = async { fetchNews() }

    Dashboard(
        stats = try { stats.await() } catch (e: Exception) {
            logger.warn("Stats failed", e)
            defaultStats
        },
        news = try { news.await() } catch (e: Exception) {
            logger.warn("News failed", e)
            emptyList()
        }
    )
}

// 패턴 3: 재시도 로직
suspend fun fetchWithRetry(maxRetries: Int = 3): String {
    repeat(maxRetries) { attempt ->
        try {
            return fetchData()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            delay(1000 * (attempt + 1))  // 지수 백오프
        }
    }
    error("Unreachable")
}
```

## 취소 (Cancellation)

### 기본 취소

```kotlin
fun main() = runBlocking {
    val job = launch {
        repeat(1000) { i ->
            println("Job: I'm working $i...")
            delay(500L)
        }
    }

    delay(1300L)
    println("I'm tired of waiting!")
    job.cancel()  // 코루틴 취소
    job.join()    // 취소 완료 대기
    println("Now I can quit.")
}

// 출력:
// Job: I'm working 0...
// Job: I'm working 1...
// Job: I'm working 2...
// I'm tired of waiting!
// Now I can quit.
```

### 취소 협력 (Cooperative Cancellation)

코루틴은 **협력적으로** 취소됩니다. 코루틴 스스로가 취소를 확인해야 합니다.

```kotlin
// ❌ 취소되지 않는 코루틴 (협력 안 함)
val job = launch(Dispatchers.Default) {
    var i = 0
    while (true) {  // 취소 체크 없음
        i++
    }
}
delay(100)
job.cancel()  // 취소 요청해도 계속 실행됨!

// ✅ 취소 가능한 코루틴 (협력)
val job = launch(Dispatchers.Default) {
    var i = 0
    while (isActive) {  // 취소 상태 확인
        i++
    }
}
delay(100)
job.cancel()  // 정상 취소됨
```

### 취소 확인 방법

```kotlin
// 방법 1: isActive 확인
while (isActive) {
    // 작업 수행
}

// 방법 2: ensureActive() 호출
while (true) {
    ensureActive()  // 취소되었으면 CancellationException 발생
    // 작업 수행
}

// 방법 3: yield() 호출 (일시 중단 포인트)
while (true) {
    yield()  // 다른 코루틴에게 실행 기회 양보 + 취소 확인
    // 작업 수행
}
```

### 취소 시 리소스 정리

```kotlin
suspend fun processFile() {
    val file = openFile()
    try {
        while (isActive) {
            processChunk(file)
        }
    } finally {
        file.close()  // 취소되어도 반드시 실행
    }
}

// withContext(NonCancellable)로 정리 작업 보장
suspend fun processWithCleanup() {
    try {
        // 메인 작업
        processData()
    } finally {
        withContext(NonCancellable) {
            // 취소 불가능한 정리 작업
            cleanup()
        }
    }
}
```

### 실전 취소 패턴

```kotlin
// 패턴 1: 타임아웃 처리
suspend fun fetchWithTimeout(timeoutMs: Long): String {
    return withTimeout(timeoutMs) {
        fetchData()  // timeoutMs 내에 완료되지 않으면 취소
    }
}

// 패턴 2: 타임아웃 시 null 반환
suspend fun fetchOrNull(timeoutMs: Long): String? {
    return withTimeoutOrNull(timeoutMs) {
        fetchData()
    }
}

// 패턴 3: 조건부 취소
class DataProcessor {
    private var job: Job? = null

    fun startProcessing() {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                processChunk()
                delay(100)
            }
        }
    }

    fun stopProcessing() {
        job?.cancel()
        job = null
    }
}

// 패턴 4: 부모-자식 취소 관계
suspend fun processOrders() = coroutineScope {
    val job1 = launch { processOrder1() }
    val job2 = launch { processOrder2() }
    val job3 = launch { processOrder3() }

    // 전체 스코프 취소 시 모든 자식 취소됨
}
```

## 플로우 (Flow)

### 개념

Flow는 **비동기 데이터 스트림**을 나타냅니다. 여러 값을 순차적으로 방출(emit)할 수 있습니다.

```
일반 함수:
fetchData() ────────────> 단일 값 반환

Flow:
fetchDataFlow() ──> 값1 ──> 값2 ──> 값3 ──> 완료
                  (시간차 방출)
```

### 기본 사용법

```kotlin
// Flow 생성
fun simpleFlow(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)  // 비동기 작업
        emit(i)     // 값 방출
    }
}

// Flow 수집 (collect)
fun main() = runBlocking {
    simpleFlow().collect { value ->
        println(value)
    }
}

// 출력 (0.1초 간격):
// 1
// 2
// 3
```

### Flow vs Sequence vs List

```kotlin
// List: 모든 값을 즉시 반환
fun getNumbers(): List<Int> {
    return listOf(1, 2, 3)  // 즉시 생성
}

// Sequence: 지연 평가 (동기)
fun getNumbersSeq(): Sequence<Int> = sequence {
    for (i in 1..3) {
        Thread.sleep(100)  // 블로킹
        yield(i)
    }
}

// Flow: 지연 평가 (비동기)
fun getNumbersFlow(): Flow<Int> = flow {
    for (i in 1..3) {
        delay(100)  // 논블로킹
        emit(i)
    }
}
```

### Flow 연산자

```kotlin
// map: 값 변환
flow {
    emit(1)
    emit(2)
    emit(3)
}.map { it * it }
 .collect { println(it) }  // 1, 4, 9

// filter: 값 필터링
(1..10).asFlow()
    .filter { it % 2 == 0 }
    .collect { println(it) }  // 2, 4, 6, 8, 10

// transform: 복잡한 변환
(1..3).asFlow()
    .transform { value ->
        emit("Making $value")
        emit(value)
    }
    .collect { println(it) }
// Making 1
// 1
// Making 2
// 2
// Making 3
// 3

// take: 개수 제한
(1..100).asFlow()
    .take(3)
    .collect { println(it) }  // 1, 2, 3

// reduce: 누적 계산
val sum = (1..5).asFlow()
    .reduce { acc, value -> acc + value }
println(sum)  // 15
```

### Flow 컨텍스트

```kotlin
// flowOn: Flow의 실행 컨텍스트 변경
fun fetchData(): Flow<String> = flow {
    // IO 스레드에서 실행
    emit(fetchFromNetwork())
    emit(fetchFromDatabase())
}.flowOn(Dispatchers.IO)

fun main() = runBlocking {
    fetchData()
        .collect { value ->
            // Main 스레드에서 실행
            updateUI(value)
        }
}
```

**시각적 흐름:**

```
[IO Thread]
    fetch emission 1
    fetch emission 2
    ↓ flowOn(IO)
[Main Thread]
    collect emission 1
    collect emission 2
```

### 실전 Flow 패턴

```kotlin
// 패턴 1: 데이터베이스 변경 감지
class OrderRepository {
    fun observeOrders(): Flow<List<Order>> = flow {
        while (true) {
            emit(database.getAllOrders())
            delay(1000)  // 1초마다 갱신
        }
    }.flowOn(Dispatchers.IO)
}

// 사용
orderRepository.observeOrders()
    .collect { orders ->
        updateUI(orders)
    }

// 패턴 2: 검색어 자동완성 (debounce)
searchQueryFlow
    .debounce(300)  // 300ms 동안 입력 없으면 실행
    .filter { it.length >= 2 }
    .distinctUntilChanged()  // 이전 값과 같으면 무시
    .flatMapLatest { query ->
        searchRepository.search(query)
    }
    .collect { results ->
        displayResults(results)
    }

// 패턴 3: 여러 Flow 결합
fun getCombinedData(): Flow<CombinedData> =
    combine(
        userFlow,
        ordersFlow,
        statsFlow
    ) { user, orders, stats ->
        CombinedData(user, orders, stats)
    }

// 패턴 4: Flow 에러 처리
userRepository.getUsers()
    .catch { e ->
        logger.error("Failed to fetch users", e)
        emit(emptyList())  // 기본값 방출
    }
    .collect { users ->
        displayUsers(users)
    }

// 패턴 5: 진행 상태와 함께 데이터 로드
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
}

fun loadData(): Flow<UiState<Data>> = flow {
    emit(UiState.Loading)
    try {
        val data = fetchData()
        emit(UiState.Success(data))
    } catch (e: Exception) {
        emit(UiState.Error(e))
    }
}
```

### StateFlow와 SharedFlow

```kotlin
// StateFlow: 상태 홀더 (항상 최신 값 유지)
class UserViewModel {
    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    fun loadUser(id: Long) {
        viewModelScope.launch {
            _userState.value = userRepository.getUser(id)
        }
    }
}

// SharedFlow: 이벤트 브로드캐스트
class EventBus {
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun publish(event: Event) {
        _events.emit(event)
    }
}

// 사용 차이
// StateFlow: UI가 최신 상태 필요 (현재 사용자, 설정 등)
// SharedFlow: 일회성 이벤트 (토스트, 네비게이션 등)
```

## 실전 예제

### 예제 1: 사용자 등록 플로우

```kotlin
@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val pointService: PointService
) {
    suspend fun registerUser(request: RegisterRequest): User = coroutineScope {
        // 1. 사용자 생성
        val user = User(
            email = request.email,
            name = request.name
        )
        userRepository.save(user)

        // 2. 병렬로 웰컴 이메일 발송 + 가입 포인트 지급
        val emailJob = launch(Dispatchers.IO) {
            emailService.sendWelcomeEmail(user.email)
        }

        val pointJob = launch(Dispatchers.IO) {
            pointService.grantSignupPoints(user.id)
        }

        // 3. 모두 완료 대기
        emailJob.join()
        pointJob.join()

        user
    }
}
```

### 예제 2: 주문 처리 시스템

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val itemRepository: ItemRepository,
    private val paymentService: PaymentService
) {
    suspend fun createOrder(request: CreateOrderRequest): Order = coroutineScope {
        // 1. 재고 확인 (병렬)
        val items = request.itemIds.map { itemId ->
            async(Dispatchers.IO) {
                itemRepository.findById(itemId) ?: throw ItemNotFoundException()
            }
        }.awaitAll()

        // 2. 재고 검증
        items.forEach { item ->
            if (item.stockQuantity < 1) {
                throw OutOfStockException(item.id)
            }
        }

        // 3. 주문 생성
        val order = Order(
            userId = request.userId,
            items = items,
            totalAmount = items.sumOf { it.basePrice }
        )
        orderRepository.save(order)

        // 4. 결제 처리
        try {
            withContext(Dispatchers.IO) {
                paymentService.processPayment(order)
            }
            order.confirm()
        } catch (e: PaymentException) {
            order.cancel()
            throw e
        }

        // 5. 재고 감소
        launch(Dispatchers.IO) {
            items.forEach { it.decreaseStock(1) }
            itemRepository.saveAll(items)
        }

        order
    }
}
```

### 예제 3: 대시보드 데이터 로딩

```kotlin
@Service
class DashboardService {
    suspend fun loadDashboard(userId: Long): Dashboard = supervisorScope {
        // 여러 API를 병렬로 호출 (일부 실패 허용)
        val userDeferred = async { fetchUser(userId) }
        val ordersDeferred = async { fetchRecentOrders(userId) }
        val statsDeferred = async { fetchUserStats(userId) }
        val recommendationsDeferred = async { fetchRecommendations(userId) }

        Dashboard(
            user = try {
                userDeferred.await()
            } catch (e: Exception) {
                logger.warn("Failed to fetch user", e)
                null
            },
            orders = try {
                ordersDeferred.await()
            } catch (e: Exception) {
                logger.warn("Failed to fetch orders", e)
                emptyList()
            },
            stats = try {
                statsDeferred.await()
            } catch (e: Exception) {
                logger.warn("Failed to fetch stats", e)
                defaultStats
            },
            recommendations = try {
                recommendationsDeferred.await()
            } catch (e: Exception) {
                logger.warn("Failed to fetch recommendations", e)
                emptyList()
            }
        )
    }
}
```

## 성능 최적화

### 1. 불필요한 코루틴 생성 방지

```kotlin
// ❌ Bad: 불필요한 launch
suspend fun processData(data: List<String>) {
    data.forEach { item ->
        launch {  // 매번 새 코루틴 생성
            process(item)
        }
    }
}

// ✅ Good: 직접 처리
suspend fun processData(data: List<String>) {
    data.forEach { item ->
        process(item)  // suspend 함수 직접 호출
    }
}

// ✅ Good: 실제 병렬 처리 필요한 경우
suspend fun processDataParallel(data: List<String>) = coroutineScope {
    data.map { item ->
        async { process(item) }
    }.awaitAll()
}
```

### 2. 디스패처 선택 최적화

```kotlin
// ❌ Bad: 잘못된 디스패처
suspend fun fetchData() = withContext(Dispatchers.Main) {
    // 네트워크 호출을 Main에서? ❌
    apiClient.fetchData()
}

// ✅ Good: 적절한 디스패처
suspend fun fetchData() = withContext(Dispatchers.IO) {
    apiClient.fetchData()
}
```

### 3. 구조화된 동시성 활용

```kotlin
// ❌ Bad: GlobalScope 사용
fun loadData() {
    GlobalScope.launch {
        // 생명주기 관리 불가
        val data = fetchData()
        updateUI(data)
    }
}

// ✅ Good: 적절한 스코프
class ViewModel {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    fun loadData() {
        scope.launch {
            val data = fetchData()
            updateUI(data)
        }
    }

    fun cleanup() {
        scope.cancel()  // 모든 코루틴 취소
    }
}
```

## 테스트

### 기본 테스트

```kotlin
@Test
fun `코루틴 테스트`() = runBlocking {
    val result = async {
        delay(100)
        "Hello"
    }.await()

    assertEquals("Hello", result)
}
```

### kotlinx-coroutines-test 사용

```kotlin
dependencies {
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

```kotlin
@Test
fun `delay 테스트`() = runTest {
    val startTime = currentTime

    delay(1000)  // 가상 시간으로 즉시 완료

    val elapsed = currentTime - startTime
    assertEquals(1000, elapsed)  // 실제로는 즉시 완료됨
}

@Test
fun `Flow 테스트`() = runTest {
    val flow = flow {
        emit(1)
        delay(100)
        emit(2)
        delay(100)
        emit(3)
    }

    val results = flow.toList()
    assertEquals(listOf(1, 2, 3), results)
}
```

## 흔한 실수

### 1. suspend 함수를 blocking으로 호출

```kotlin
// ❌ Bad
fun getData(): String {
    return runBlocking {
        fetchData()  // 스레드 블로킹!
    }
}

// ✅ Good
suspend fun getData(): String {
    return fetchData()
}
```

### 2. GlobalScope 남용

```kotlin
// ❌ Bad
GlobalScope.launch {
    // 취소 불가, 메모리 누수 위험
}

// ✅ Good
viewModelScope.launch {
    // ViewModel 생명주기에 맞춰 자동 취소
}
```

### 3. 취소 협력 안 함

```kotlin
// ❌ Bad
launch {
    while (true) {  // 취소 확인 안 함
        doWork()
    }
}

// ✅ Good
launch {
    while (isActive) {  // 취소 확인
        doWork()
    }
}
```

### 4. 예외 처리 누락

```kotlin
// ❌ Bad
launch {
    fetchData()  // 예외 발생 시 앱 크래시
}

// ✅ Good
launch {
    try {
        fetchData()
    } catch (e: Exception) {
        handleError(e)
    }
}
```

## 의존성 추가

### build.gradle.kts

```kotlin
dependencies {
    // 코루틴 코어
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Android의 경우
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 테스트
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

## 요약

### 핵심 개념

| 개념 | 설명 | 예시 |
|------|------|------|
| **코루틴** | 경량 스레드 | `launch { }`, `async { }` |
| **suspend** | 중단 가능 함수 | `suspend fun fetchData()` |
| **디스패처** | 실행 스레드 결정 | `Dispatchers.IO`, `Dispatchers.Main` |
| **스코프** | 생명주기 관리 | `coroutineScope { }`, `viewModelScope` |
| **Flow** | 비동기 스트림 | `flow { emit(value) }` |

### 언제 무엇을 사용할까?

| 상황 | 사용 함수 | 이유 |
|------|----------|------|
| 결과 필요 없는 백그라운드 작업 | `launch` | 반환값 없음 |
| 결과 반환 필요 | `async` + `await` | Deferred 반환 |
| 여러 작업 병렬 실행 | `async` 여러 개 | 동시 실행 |
| 메인 함수/테스트 | `runBlocking` | 코루틴 진입점 |
| 네트워크/파일/DB | `withContext(IO)` | I/O 작업 |
| CPU 집약 작업 | `withContext(Default)` | 계산 작업 |
| 데이터 스트림 | `Flow` | 여러 값 순차 방출 |

## 추가 학습 자료

### 공식 문서
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Coroutines API Reference](https://kotlin.github.io/kotlinx.coroutines/)

### 추천 학습 순서
1. **기본 개념**: launch, async, runBlocking
2. **suspend 함수**: delay, withContext
3. **스코프**: coroutineScope, supervisorScope
4. **디스패처**: Dispatchers.IO, Main, Default
5. **취소**: Job, isActive, 협력적 취소
6. **예외 처리**: try-catch, CoroutineExceptionHandler
7. **Flow**: flow, collect, 연산자

Happy Kotlin Coroutines! 🚀
