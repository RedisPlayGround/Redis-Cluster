package com.example.rediscluster

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest
class SimpleTest {

    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, String>

    val dummyValue = "banana"

    @Test
    fun setValues() {
        val ops = redisTemplate.opsForValue()
        for (i in 0 until 1000) {
            val key = "name:$i"
            ops.set(key, dummyValue)
        }
    }

    @Test
    fun getValues() {
        val ops = redisTemplate.opsForValue()
        for (i in 0 until 1000) {
            val key = "name:$i"
            val value = ops.get(key)
            Assertions.assertThat(value).isEqualTo(dummyValue)
        }
    }
}