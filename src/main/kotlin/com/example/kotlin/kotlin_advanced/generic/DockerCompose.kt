package com.example.kotlin.kotlin_advanced.generic

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun main() {
    val yml = dockerCompose {
        version { 3 }
        service(name = "db") {
            image { "mysql" }
            env("USER" - "myuser")
            env("PASSWORD" - "mypassword")
            port(host = 9999, container = 3306)
        }
    }

    val yml2 = dockerCompose {
        service("") {
            service("") {

            }
        }
    }

    println(yml.render("   "))
}

/*
    DockerCompose.() -> Unit = "DockerCompose를 this로 가진 채로 실행되는 람다"
    그래서 블록 { } 안에서 DockerCompose의 멤버에 this. 없이 바로 접근할 수 있는 것.
 */
fun dockerCompose(init: DockerCompose.() -> Unit): DockerCompose {
    val dockerCompose = DockerCompose()
    dockerCompose.init()
    return dockerCompose
}

class DockerCompose {

    private var version: Int by onceNotNull() // private var version: Int by Delegates.notNull()
    private val services = mutableListOf<Service>()

    fun version(init: () -> Int) {
        version = init()
    }

    fun service(name: String, init: Service.() -> Unit) {
        val service = Service(name)
        service.init()
        services.add(service)
    }

    fun render(indent: String): String {
        return StringBuilder().apply {
            appendNew("version: '$version'")
            appendNew("services:")
            appendNew(services.joinToString("\n") { it.render(indent) }.addIndent(indent, 1) )
        }.toString()
    }
}

class Service(val name: String) {
    private var image: String by onceNotNull()
    private val environments = mutableListOf<Environment>()
    private val portRules = mutableListOf<PortRule>()

    fun image(init: () -> String) {
        image = init()
    }

    fun env(environment: Environment) {
        this.environments.add(environment)
    }

    fun port(host: Int, container: Int) {
        portRules.add(PortRule(host, container))
    }

    fun render(indent: String): String {
        return StringBuilder().apply {
            appendNew("$name:")
            appendNew("image: $image", indent, 1)
            appendNew("environment:")
            environments.joinToString("\n") { "- ${it.key}: ${it.value}" }
                .addIndent(indent, 1)
                .also(::appendNew)
            appendNew("port:")
            portRules.joinToString("\n") { "- \"${it.host}:${it.container}\"" }
                .addIndent(indent, 1)
                .also(::appendNew)
        }.toString()
    }
}

class Environment(
    val key: String,
    val value: String
)

operator fun String.minus(other: String): Environment {
    return Environment(key = this, value = other)
}

data class PortRule(
    val host: Int,
    val container: Int,
)

fun <T> onceNotNull() = object : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (this.value == null) {
            throw IllegalArgumentException("변수가 초기화되지 않았습니다.")
        }

        return value!!
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (this.value != null) {
            throw IllegalArgumentException("이 변수는 한번만 값을 초기화 할 수 있습니다.")
        }

        this.value = value
    }
}

fun StringBuilder.appendNew(str: String, indent: String = "", times: Int = 0) {
    (1..times).forEach { _ -> this.append(indent) }
    this.append(str)
    this.append("\n")
}

fun String.addIndent(indent: String = "", times: Int = 0): String {
    val allIndent = (1..times).joinToString("") { indent }
    return this.split("\n")
        .joinToString("\n") { "$allIndent$it" }
}

@DslMarker
annotation class YamlDsl