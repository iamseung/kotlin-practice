# Kotlin 학습 문서

이 디렉토리에는 Kotlin의 주요 개념에 대한 상세한 학습 자료가 포함되어 있습니다.

## 📚 문서 목록

### Scope Functions (스코프 함수)
객체의 컨텍스트 내에서 코드를 실행하는 함수들

- **[scope/README.md](./scope/README.md)** - Scope Functions 종합 가이드
- **[scope/let.md](./scope/let.md)** - 값 변환 및 null 안전성
- **[scope/run.md](./scope/run.md)** - 객체 설정과 결과 계산
- **[scope/with.md](./scope/with.md)** - 객체 컨텍스트에서 여러 작업 수행
- **[scope/apply.md](./scope/apply.md)** - 객체 초기화 및 설정
- **[scope/also.md](./scope/also.md)** - 부수 효과(Side-Effect) 작업

### Sealed Class (봉인 클래스)
제한된 클래스 계층 구조로 타입 안전성 보장

- **[sealed-class.md](./sealed-class.md)** - Sealed Class 완벽 가이드
  - Result 패턴
  - UI 상태 관리
  - 네트워크 응답 처리
  - 이벤트 시스템
  - Either 타입
  - 명령 패턴

### Generics (제네릭)
타입 파라미터를 사용한 재사용 가능한 코드

- **[generics.md](generics/generics.md)** - Generics 완벽 가이드
  - out T (공변성) 이해하기
  - in T (반공변성) 이해하기
  - Nothing 타입의 의미
  - Result 패턴에서 제네릭 활용
  - 타입 경계와 제약
  - reified 타입 파라미터

## 🎯 빠른 참조

### Scope Functions 비교

| 함수 | 객체 참조 | 반환값 | 주 용도 |
|------|----------|--------|---------|
| **let** | `it` | 람다 결과 | 값 변환, null 체크 |
| **run** | `this` | 람다 결과 | 설정 + 결과 반환 |
| **with** | `this` | 람다 결과 | 객체로 여러 작업 |
| **apply** | `this` | 객체 자체 | 객체 초기화 |
| **also** | `it` | 객체 자체 | 부수 효과 |

### Sealed Class vs Enum vs Abstract Class

| 특징 | Enum | Sealed Class | Abstract Class |
|------|------|--------------|----------------|
| 인스턴스 | 고정된 상수 | 여러 인스턴스 | 여러 인스턴스 |
| 상태 저장 | 제한적 | 자유롭게 | 자유롭게 |
| 타입 안전성 | ✓ | ✓ | ✗ |
| when 완전성 | ✓ | ✓ | ✗ |

## 📖 학습 순서 추천

### 초급
1. **Scope Functions**
   - apply (객체 초기화)
   - also (부수 효과)
   - let (null 안전성)

### 중급
2. **Scope Functions 심화**
   - run (설정 + 결과)
   - with (컨텍스트 작업)

3. **Sealed Class 기초**
   - Result 패턴
   - UI 상태 관리

4. **Generics 기초**
   - 제네릭 타입 파라미터
   - 기본 사용법

### 고급
5. **Generics 심화**
   - out T (공변성)
   - in T (반공변성)
   - Nothing 타입

6. **Sealed Class + Generics**
   - Result<out T> 패턴
   - Either 타입
   - 재귀적 구조
   - 복잡한 상태 관리

## 🔍 실무 적용

### Scope Functions 실무 패턴

```kotlin
// 패턴 1: apply + also
fun createUser(username: String): User {
    return User()
        .apply {
            // 객체 설정
            this.username = username
            email = "${username}@example.com"
        }
        .also {
            // 부수 효과
            logger.info("User created: ${it.username}")
            eventBus.publish(UserCreatedEvent(it))
        }
}

// 패턴 2: let + Elvis
val displayName = user?.let {
    "${it.firstName} ${it.lastName}"
} ?: "Guest"
```

### Sealed Class 실무 패턴

```kotlin
// Result 패턴
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// UI 상태 관리
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// 사용
when (val state = viewModel.state) {
    is UiState.Idle -> showWelcome()
    is UiState.Loading -> showLoading()
    is UiState.Success -> showData(state.data)
    is UiState.Error -> showError(state.message)
}
```

## 🧪 테스트 코드

각 개념에 대한 실제 동작하는 예제는 테스트 파일을 참고하세요:

### Scope Functions
- `src/test/kotlin/com/example/kotlin/single_test/LetTest.kt`
- `src/test/kotlin/com/example/kotlin/single_test/RunTest.kt`
- `src/test/kotlin/com/example/kotlin/single_test/WithTest.kt`
- `src/test/kotlin/com/example/kotlin/single_test/ApplyTest.kt`
- `src/test/kotlin/com/example/kotlin/single_test/AlsoTest.kt`

### Sealed Class
- `src/test/kotlin/com/example/kotlin/single_test/SealedClassTest.kt`

### Generics
- `src/test/kotlin/com/example/kotlin/single_test/GenericsTest.kt`

## 📚 추가 학습 자료

- [Kotlin 공식 문서](https://kotlinlang.org/docs/home.html)
- [Kotlin Scope Functions](https://kotlinlang.org/docs/scope-functions.html)
- [Kotlin Sealed Classes](https://kotlinlang.org/docs/sealed-classes.html)
- [Kotlin Generics](https://kotlinlang.org/docs/generics.html)

## 🎉 마무리

이 문서들은 Kotlin의 강력한 기능들을 실무에서 바로 활용할 수 있도록 구성되었습니다. 각 문서는:

- ✅ 명확한 개념 설명
- ✅ 실무 중심의 예제
- ✅ 베스트 프랙티스
- ✅ 안티패턴 소개
- ✅ 실제 동작하는 테스트 코드

Happy Kotlin Coding! 🚀
