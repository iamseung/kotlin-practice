package com.example.kotlin.kotlin_advanced.reflection

import com.example.kotlin.kotlin_advanced.generic.Animal
import com.example.kotlin.kotlin_advanced.generic.Cage
import com.example.kotlin.kotlin_advanced.generic.Carp
import com.example.kotlin.kotlin_advanced.generic.GoldFish
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.cast

fun main() {
    /*
    val cage = Cage()
    cage.put(Carp("잉어"))
    cage.getFirst() as Carp // 위험한 코드

    val typeSafeCage = TypeSafeCage()
    typeSafeCage.putOne(Carp("잉어"))

    // 타입 안전하게 잉어를 가져옴
    val carp = typeSafeCage.getOne(Carp::class)
    val one = typeSafeCage.getOne<Carp>()
     */

    val superTypeToken1 = object : SuperTypeToken<List<GoldFish>>() {}
    val superTypeToken2 = object : SuperTypeToken<List<GoldFish>>() {}
    val superTypeToken3 = object : SuperTypeToken<List<Carp>>() {}

    val superTypeSafeCage = SuperTypeSafeCage()
    superTypeSafeCage.putOne(superTypeToken2, listOf(GoldFish("금붕어1"), GoldFish("금붕어2")))
    val result = superTypeSafeCage.getOne(superTypeToken2)
    println(result)
}

class TypeSafeCage {
    private val animals: MutableMap<KClass<*>, Animal> = mutableMapOf()

    // type: KClass<T> -> 타입 토큰
    fun <T : Animal> getOne(type: KClass<T>): T {
        return type.cast(animals[type])
    }

    fun <T : Animal> putOne(type: KClass<T>, animal: T) {
        animals[type] = type.cast(animal)
    }

    inline fun <reified T : Animal> getOne(): T {
        return this.getOne(T::class)
    }

    // 타입 T가 추론됨
    inline fun <reified T : Animal> putOne(animal: T) {
        this.putOne(T::class, animal)
    }
}

class SuperTypeSafeCage {
    private val animals: MutableMap<SuperTypeToken<*>, Any> = mutableMapOf()

    fun <T : Any> getOne(token: SuperTypeToken<T>): T {
        return this.animals[token] as T
    }

    fun <T : Any> putOne(token: SuperTypeToken<T>, animal: T) {
        animals[token] = animal
    }
}

// 인스턴스화를 막기 위해 추상 클래스로 생성
// SuperTypeToken을 구현한 클래스가 인스턴스화 되자마자 T 정보를 내부 변수에 저장해 버린다..!
abstract class SuperTypeToken<T> {
    // 여기서 this, 해당 클래스를 상속받는 클래스를 바라보게 됨
    // arguments, 타입 매개변수
    val type: KType = this::class.supertypes[0].arguments[0].type!!

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as SuperTypeToken<*>
        if (type != other.type) return false
        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}