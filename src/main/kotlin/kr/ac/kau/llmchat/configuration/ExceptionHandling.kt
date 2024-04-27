package kr.ac.kau.llmchat.configuration

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.NativeWebRequest
import org.zalando.problem.Problem
import org.zalando.problem.Status
import org.zalando.problem.spring.web.advice.AdviceTrait
import org.zalando.problem.spring.web.advice.ProblemHandling
import org.zalando.problem.spring.web.advice.security.SecurityAdviceTrait

interface IllegalArgumentExceptionTrait : AdviceTrait {
    @ExceptionHandler
    fun handleIllegalArgumentException(
        e: IllegalArgumentException,
        request: NativeWebRequest,
    ): ResponseEntity<Problem> {
        return create(Status.BAD_REQUEST, e, request)
    }
}

@ControllerAdvice
class ExceptionHandling : ProblemHandling, IllegalArgumentExceptionTrait, SecurityAdviceTrait
