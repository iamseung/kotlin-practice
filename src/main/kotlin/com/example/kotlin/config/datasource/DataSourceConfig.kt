package com.example.kotlin.config.datasource

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import javax.sql.DataSource

/**
 * Master-Slave 데이터베이스 구성을 위한 Configuration 클래스
 *
 * ## 주요 기능
 * 1. Master DataSource 생성 - 쓰기 작업용
 * 2. Slave DataSource 생성 - 읽기 작업용
 * 3. RoutingDataSource 설정 - 트랜잭션 컨텍스트에 따른 자동 라우팅
 * 4. LazyConnectionDataSourceProxy - 실제 쿼리 실행 시점에 DataSource 결정
 *
 * ## DataSource 계층 구조
 * ```
 * LazyConnectionDataSourceProxy (실제 Connection 획득 지연)
 *   └── RoutingDataSource (트랜잭션 컨텍스트에 따라 라우팅)
 *        ├── Master DataSource (쓰기)
 *        └── Slave DataSource (읽기)
 * ```
 *
 * ## 설정 파일 (application.yml) 예시
 * ```yaml
 * spring:
 *   datasource:
 *     master:
 *       jdbc-url: jdbc:mysql://localhost:3306/kotlin_practice
 *       username: root
 *       password: root1234
 *       driver-class-name: com.mysql.cj.jdbc.Driver
 *     slave:
 *       jdbc-url: jdbc:mysql://localhost:3307/kotlin_practice
 *       username: root
 *       password: root1234
 *       driver-class-name: com.mysql.cj.jdbc.Driver
 * ```
 *
 * @see RoutingDataSource
 * @see DataSourceType
 */
@Configuration
class DataSourceConfig {

    /**
     * Master(Primary) DataSource 생성
     *
     * application.yml의 spring.datasource.master 설정을 읽어
     * 쓰기 작업을 담당하는 DataSource를 생성합니다.
     *
     * @return Master DataSource
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master.hikari")
    fun masterDataSource(): DataSource {
        return DataSourceBuilder.create()
            .type(HikariDataSource::class.java)
            .build()
    }

    /**
     * Slave(Replica) DataSource 생성
     *
     * application.yml의 spring.datasource.slave 설정을 읽어
     * 읽기 작업을 담당하는 DataSource를 생성합니다.
     *
     * @return Slave DataSource
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave.hikari")
    fun slaveDataSource(): DataSource {
        return DataSourceBuilder.create()
            .type(HikariDataSource::class.java)
            .build()
    }

    /**
     * Routing DataSource 생성
     *
     * Master와 Slave DataSource를 등록하고,
     * 트랜잭션 컨텍스트에 따라 자동으로 선택되도록 설정합니다.
     *
     * @param masterDataSource Master DataSource
     * @param slaveDataSource Slave DataSource
     * @return RoutingDataSource
     */
    @Bean
    fun routingDataSource(
        @Qualifier("masterDataSource") masterDataSource: DataSource,
        @Qualifier("slaveDataSource") slaveDataSource: DataSource
    ): DataSource {
        val routingDataSource = RoutingDataSource()

        // DataSource Map 생성
        val dataSourceMap = mapOf<Any, Any>(
            DataSourceType.MASTER to masterDataSource,
            DataSourceType.SLAVE to slaveDataSource
        )

        // TargetDataSources 설정
        routingDataSource.setTargetDataSources(dataSourceMap)

        // 기본 DataSource는 Master로 설정 (트랜잭션이 없는 경우)
        routingDataSource.setDefaultTargetDataSource(masterDataSource)

        return routingDataSource
    }

    /**
     * Primary DataSource 설정
     *
     * LazyConnectionDataSourceProxy를 사용하여 실제 쿼리가 실행되는 시점에
     * Connection을 획득하고 DataSource를 결정합니다.
     *
     * ## LazyConnectionDataSourceProxy를 사용하는 이유
     * - 트랜잭션 시작 시점이 아닌 실제 쿼리 실행 시점에 DataSource 결정
     * - @Transactional 어노테이션 선언 순서에 영향받지 않음
     * - AOP Proxy 문제 해결
     *
     * @param routingDataSource Routing DataSource
     * @return LazyConnectionDataSourceProxy로 감싼 DataSource
     */
    @Primary
    @Bean
    fun dataSource(@Qualifier("routingDataSource") routingDataSource: DataSource): DataSource {
        return LazyConnectionDataSourceProxy(routingDataSource)
    }
}
