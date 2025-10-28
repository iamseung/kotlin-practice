package com.example.kotlin.scope_test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * 코틀린 Scope Functions 테스트
 *
 * 5가지 Scope Functions의 차이점:
 * 1. apply - 객체 초기화 및 설정에 사용, this로 접근, 객체 자체를 반환
 * 2. with - 객체의 여러 함수를 호출할 때, this로 접근, 람다 결과를 반환
 * 3. let - null 체크 및 변환에 사용, it으로 접근, 람다 결과를 반환
 * 4. also - 추가적인 작업(로깅 등)에 사용, it으로 접근, 객체 자체를 반환
 * 5. run - 객체 초기화 후 결과 계산, this로 접근, 람다 결과를 반환
 */
class ScopeFunctionTest {

    data class Person(
        var name: String = "",
        var age: Int = 0,
        var city: String = ""
    )

    /**
     * apply 사용 시기:
     * - 객체를 초기화하고 설정할 때
     * - 설정한 객체 자체를 반환받고 싶을 때
     * - "이 객체에 다음 속성들을 적용해라" 라는 의미
     *
     * 특징:
     * - 컨텍스트: this
     * - 반환값: 객체 자체
     */
    @Test
    fun `apply - 객체 초기화 및 설정`() {
        // apply 사용 - 객체 생성과 동시에 속성 설정
        val person = Person().apply {
            name = "홍길동"      // this.name = "홍길동" 와 동일
            age = 30            // this.age = 30 와 동일
            city = "서울"       // this.city = "서울" 와 동일
        }

        // person 객체 자체가 반환됨
        assertEquals("홍길동", person.name)
        assertEquals(30, person.age)
        assertEquals("서울", person.city)

        // 실제 사용 예시: View 설정
        val stringBuilder = StringBuilder().apply {
            append("Hello")
            append(" ")
            append("World")
        }
        assertEquals("Hello World", stringBuilder.toString())
    }

    /**
     * with 사용 시기:
     * - 객체의 여러 함수를 연속으로 호출할 때
     * - "이 객체로(with) 다음 작업들을 수행해라" 라는 의미
     * - null이 아닌 객체에 대해서만 사용
     *
     * 특징:
     * - 컨텍스트: this
     * - 반환값: 람다의 결과
     */
    @Test
    fun `with - 객체의 여러 함수 호출`() {
        val person = Person("김철수", 25, "부산")

        // with 사용 - 객체의 여러 속성을 읽어서 결과 생성
        val description = with(person) {
            // this는 person을 가리킴
            "이름: $name, 나이: $age, 도시: $city"  // 람다의 마지막 줄이 반환값
        }

        assertEquals("이름: 김철수, 나이: 25, 도시: 부산", description)

        // 실제 사용 예시: 여러 연산을 수행하고 결과 반환
        val numbers = mutableListOf(1, 2, 3, 4, 5)
        val result = with(numbers) {
            add(6)
            add(7)
            sum()  // 마지막 줄이 반환됨
        }
        assertEquals(28, result)  // 1+2+3+4+5+6+7 = 28
    }

    /**
     * let 사용 시기:
     * - null 체크를 할 때 (?. 연산자와 함께 사용)
     * - 객체를 변환할 때
     * - 지역 변수의 범위를 제한하고 싶을 때
     *
     * 특징:
     * - 컨텍스트: it (람다 파라미터)
     * - 반환값: 람다의 결과
     */
    @Test
    fun `let - null 체크 및 변환`() {
        val nullablePerson: Person? = Person("박영희", 28, "대구")

        // let 사용 - null이 아닐 때만 실행
        val result = nullablePerson?.let {
            // it은 Person 객체를 가리킴 (nullable이 아님!)
            "${it.name}님은 ${it.age}살입니다"
        }
        assertEquals("박영희님은 28살입니다", result)

        // null인 경우 테스트
        val nullPerson: Person? = null
        val nullResult = nullPerson?.let {
            "${it.name}님은 ${it.age}살입니다"
        } ?: "사람 정보 없음"  // null이면 기본값 사용
        assertEquals("사람 정보 없음", nullResult)

        // 실제 사용 예시: 값 변환
        val name = "이순신"
        val length = name.let {
            println("이름: $it")  // 로깅
            it.length              // 변환된 값 반환
        }
        assertEquals(3, length)
    }

    /**
     * also 사용 시기:
     * - 객체에 추가적인 작업을 수행할 때 (로깅, 검증 등)
     * - "객체를 사용하고 또한(also) 이것도 해라" 라는 의미
     * - 객체의 속성을 변경하지 않고 side-effect만 수행
     *
     * 특징:
     * - 컨텍스트: it (람다 파라미터)
     * - 반환값: 객체 자체
     */
    @Test
    fun `also - 추가 작업 수행`() {
        val logs = mutableListOf<String>()

        // also 사용 - 객체 생성 후 로깅하고 객체 반환
        val person = Person().apply {
            name = "최민수"
            age = 35
            city = "인천"
        }.also {
            // it은 Person 객체를 가리킴
            logs.add("Person 객체 생성됨: ${it.name}")
            logs.add("나이: ${it.age}, 도시: ${it.city}")
        }

        // person 객체 자체가 반환됨
        assertEquals("최민수", person.name)
        assertEquals(2, logs.size)
        assertTrue(logs[0].contains("최민수"))

        // 실제 사용 예시: 리스트에 추가하면서 로깅
        val people = mutableListOf<Person>()
        val newPerson = Person("정다은", 27, "광주").also {
            people.add(it)
            println("${it.name}를 리스트에 추가했습니다")
        }

        assertEquals(1, people.size)
        assertEquals("정다은", newPerson.name)
    }

    /**
     * run 사용 시기:
     * - 객체 초기화와 동시에 결과값을 계산할 때
     * - nullable 객체에서 여러 함수를 호출하고 결과를 받을 때
     * - apply와 let을 합친 형태
     *
     * 특징:
     * - 컨텍스트: this
     * - 반환값: 람다의 결과
     */
    @Test
    fun `run - 객체 초기화 후 결과 계산`() {
        // run 사용 - 객체 생성하고 즉시 계산
        val isAdult = Person().run {
            name = "강민지"
            age = 20
            city = "수원"
            age >= 19  // 람다의 마지막 줄이 반환값
        }

        assertTrue(isAdult)

        // nullable 객체와 함께 사용
        val nullablePerson: Person? = Person("윤서준", 17, "울산")
        val result = nullablePerson?.run {
            // this는 Person 객체
            if (age >= 19) "성인" else "미성년자"
        }
        assertEquals("미성년자", result)

        // 실제 사용 예시: 복잡한 계산
        val score = run {
            val korean = 90
            val english = 85
            val math = 95
            (korean + english + math) / 3.0  // 평균 계산
        }
        assertEquals(90.0, score, 0.01)
    }

    /**
     * 비교 테스트: 모든 함수를 함께 사용
     * 실제 시나리오: 사용자 등록 프로세스
     */
    @Test
    fun `실제 사용 시나리오 - 사용자 등록`() {
        val logs = mutableListOf<String>()
        val userDatabase = mutableListOf<Person>()

        // 1. apply: 객체 생성 및 초기화
        val newUser = Person().apply {
            name = "김지원"
            age = 29
            city = "대전"
        }

        // 2. also: 로깅 (side-effect)
        .also {
            logs.add("새 사용자 생성: ${it.name}")
        }

        // 3. let: null 체크 및 검증
        newUser.let {
            if (it.age >= 19) {
                // 4. run: 복잡한 검증 로직 실행
                it.run {
                    val isValid = name.isNotEmpty() && age > 0 && city.isNotEmpty()
                    if (isValid) {
                        userDatabase.add(this)
                        logs.add("사용자 등록 완료: $name")
                    }
                }
            }
        }

        // 5. with: 데이터베이스의 정보로 리포트 생성
        val report = with(userDatabase) {
            "총 사용자 수: ${size}, 등록된 사용자: ${joinToString { it.name }}"
        }

        assertEquals(1, userDatabase.size)
        assertEquals(2, logs.size)
        assertTrue(report.contains("김지원"))
    }

    /**
     * 요약 및 선택 가이드:
     *
     * apply   : 객체 설정 후 객체 반환 → "이 객체 설정해서 돌려줘"
     * with    : 객체로 작업 후 결과 반환 → "이 객체로 계산해서 결과 줘"
     * let     : it으로 작업 후 결과 반환 → "이걸 변환해서 결과 줘" (null 안전)
     * also    : 추가 작업 후 객체 반환 → "이것도 하고 객체 돌려줘" (로깅)
     * run     : this로 작업 후 결과 반환 → "이 컨텍스트에서 계산해서 결과 줘"
     *
     * 반환값으로 선택:
     * - 객체 자체를 반환: apply, also → 체이닝 가능
     * - 람다 결과를 반환: with, let, run → 값 변환
     *
     * 컨텍스트로 선택:
     * - this 사용: apply, with, run → 객체의 멤버 직접 접근
     * - it 사용: let, also → 파라미터처럼 명시적 사용
     */
    @Test
    fun `선택 가이드 예제`() {
        // 체이닝이 필요하면: apply, also
        val person1 = Person()
            .apply { name = "A"; age = 20 }
            .also { println("Created: ${it.name}") }

        // 값 변환이 필요하면: let, with, run
        val age1 = Person("B", 25, "서울").let { it.age }
        val age2 = with(Person("C", 30, "부산")) { age }
        val age3 = Person("D", 35, "대구").run { age }

        assertEquals("A", person1.name)
        assertEquals(25, age1)
        assertEquals(30, age2)
        assertEquals(35, age3)
    }
}