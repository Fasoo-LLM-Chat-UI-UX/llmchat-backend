package kr.ac.kau.llmchat.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.core.jackson.TypeNameResolver
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfiguration(private val objectMapper: ObjectMapper) {

    @PostConstruct
    fun initialize() {
        val innerClassAwareTypeNameResolver = object : TypeNameResolver() {
            override fun getNameOfClass(cls: Class<*>): String {
                return cls.name
                    .substringAfterLast(".")
                    .replace("$", "")
            }
        }

        ModelConverters
            .getInstance()
            .addConverter(ModelResolver(objectMapper, innerClassAwareTypeNameResolver))
    }
}
