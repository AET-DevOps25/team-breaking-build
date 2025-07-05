package com.recipefy.gateway.config

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class UserIdHeaderFilter : GlobalFilter {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {

        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication as BearerTokenAuthentication }
            .flatMap { auth ->
                val principal = auth.principal as OAuth2AuthenticatedPrincipal
                val userId = principal.getAttribute<String>("sub")
                val mutated = exchange.mutate().request(
                    exchange.request.mutate()
                        .header("X-User-Id", userId)
                        .build()
                ).build()

                chain.filter(mutated)
            }
    }
}