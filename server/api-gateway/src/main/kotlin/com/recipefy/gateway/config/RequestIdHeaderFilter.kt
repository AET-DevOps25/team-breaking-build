package com.recipefy.gateway.config

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*

@Component
class RequestIdHeaderFilter : GlobalFilter {

    companion object {
        private const val HEADER = "X-Request-Id"
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val id = request.headers[HEADER]?.firstOrNull() ?: UUID.randomUUID().toString()

        val mutatedExchange = exchange.mutate()
            .request(request.mutate().header(HEADER, id).build())
            .build()

        return chain.filter(mutatedExchange)
    }
}