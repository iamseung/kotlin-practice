package example

import com.example.kotlin.kotlin_advanced.generic.MyFruit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// 테스트를 진행하려면 상속 가능해야 하기 때문에 open 으로 지정
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class SequenceTest {
    private val fruits = mutableSetOf<Fruit>()

    @Setup
    fun init() {
        (1..2_000_000).forEach { _ -> fruits.add(Fruit.random()) }
    }

    @Benchmark
    fun kotlinSequence() {
        val avg = fruits.asSequence()
            .filter { it.name == "사과" }
            .map { it.price }
            .take(10_000)
            .average()
    }

    @Benchmark
    fun kotlinIterator() {
        val avg = fruits
            .filter { it.name == "사과" }
            .map { it.price }
            .take(10_000)
            .average()
    }
}

data class Fruit(
    val name: String,
    val price: Long,
) {
    companion object {
        private val NAME_CANDIDATES = listOf("사과","바나나","포도","레몬","수박")
        fun random(): Fruit {
            val randNum1 = Random.nextInt(0,NAME_CANDIDATES.size)
            val randNum2 = Random.nextLong(1000, 20_001)
            return Fruit(
                NAME_CANDIDATES[randNum1],
                randNum2
            )
        }
    }
}