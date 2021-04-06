/*
 * Copyright 2021 GoodData Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gooddata.oauth2.server.reactive

import com.gooddata.oauth2.server.common.CookieSerializer
import com.gooddata.oauth2.server.common.CookieServiceProperties
import com.gooddata.oauth2.server.common.SPRING_SEC_OAUTH2_AUTHZ_CLIENT
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.netty.handler.codec.http.cookie.CookieHeaderNames
import kotlinx.coroutines.reactive.awaitFirst
import net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals
import net.javacrumbs.jsonunit.core.Configuration
import net.javacrumbs.jsonunit.core.Option
import net.javacrumbs.jsonunit.core.util.ResourceUtils.resource
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.registration.ClientRegistrations
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.util.CollectionUtils.toMultiValueMap
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.time.Duration
import java.time.Instant
import java.util.Optional

internal class CookieServerOAuth2AuthorizedClientRepositoryTest {

    private val clientRegistrationRepository: ReactiveClientRegistrationRepository = mockk()

    private val properties = CookieServiceProperties(Duration.ofDays(1), CookieHeaderNames.SameSite.Lax, "")

    private val cookieSerializer = CookieSerializer(properties)

    private val cookieService = spyk(ReactiveCookieService(properties, cookieSerializer))

    private val principal: Authentication = mockk()

    private val exchange: ServerWebExchange = mockk()

    private val repository = CookieServerOAuth2AuthorizedClientRepository(clientRegistrationRepository, cookieService)

    @Test
    fun `should not load client when nothing is stored in cookies`() {
        every { exchange.request.cookies } returns toMultiValueMap(emptyMap())

        val client = repository.loadAuthorizedClient<OAuth2AuthorizedClient>(
            "registrationId", principal, exchange
        )

        expectThat(client.blockOptional()) {
            get(Optional<OAuth2AuthorizedClient>::isEmpty).isTrue()
        }
    }

    @Test
    fun `should not load client when nonsense is stored in cookies`() {
        every { exchange.request.cookies } returns toMultiValueMap(
            mapOf(SPRING_SEC_OAUTH2_AUTHZ_CLIENT to listOf(HttpCookie(SPRING_SEC_OAUTH2_AUTHZ_CLIENT, "something")))
        )

        val client = repository.loadAuthorizedClient<OAuth2AuthorizedClient>(
            "registrationId", principal, exchange
        )

        expectThat(client.blockOptional()) {
            get(Optional<OAuth2AuthorizedClient>::isEmpty).isTrue()
        }
    }

    @Test
    fun `should not load client from cookie if registration id does not match`() {
        val body = resource("simplified_oauth2_authorized_client.json").readText()
        every { exchange.request.cookies } returns toMultiValueMap(
            mapOf(
                SPRING_SEC_OAUTH2_AUTHZ_CLIENT to listOf(
                    HttpCookie(
                        SPRING_SEC_OAUTH2_AUTHZ_CLIENT,
                        cookieSerializer.encodeCookie(body),
                    )
                )
            )
        )

        val client = repository.loadAuthorizedClient<OAuth2AuthorizedClient>(
            "registrationId", principal, exchange
        )

        expectThrows<IllegalStateException> { client.awaitFirst() }
    }

    @Test
    fun `should load client from cookie`() {
        val body = resource("simplified_oauth2_authorized_client.json").readText()
        every { exchange.request.cookies } returns toMultiValueMap(
            mapOf(
                SPRING_SEC_OAUTH2_AUTHZ_CLIENT to listOf(
                    HttpCookie(
                        SPRING_SEC_OAUTH2_AUTHZ_CLIENT,
                        cookieSerializer.encodeCookie(body),
                    )
                )
            )
        )
        every { clientRegistrationRepository.findByRegistrationId(any()) } returns Mono.just(
            ClientRegistrations
                .fromIssuerLocation("https://dev-6-eq6djb.eu.auth0.com/")
                .registrationId("localhost")
                .clientId("clientId")
                .build()
        )

        val client = repository.loadAuthorizedClient<OAuth2AuthorizedClient>(
            "localhost", principal, exchange
        ).blockOptional().get()

        expectThat(client) {
            get(OAuth2AuthorizedClient::getPrincipalName)
                .isEqualTo("auth0|5f6dee2c5924f0006f077df0")
        }
    }

    @Test
    fun `should save client`() {
        val client = OAuth2AuthorizedClient(
            ClientRegistrations
                .fromIssuerLocation("https://dev-6-eq6djb.eu.auth0.com/")
                .registrationId("localhost")
                .clientId("clientId")
                .build(),
            "principalName",
            OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "value", Instant.EPOCH, Instant.EPOCH.plusSeconds(1))
        )

        val slot = slot<String>()
        every { cookieService.createCookie(any(), any(), capture(slot)) } returns Unit

        val response = repository.saveAuthorizedClient(client, principal, exchange)
        expectThat(response.blockOptional()) {
            get(Optional<Void>::isEmpty).isTrue()
        }

        verify(exactly = 1) { cookieService.createCookie(exchange, SPRING_SEC_OAUTH2_AUTHZ_CLIENT, any()) }

        assertJsonEquals(
            resource("mock_authorized_client.json").readText(),
            slot.captured,
            Configuration.empty().withOptions(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    fun `should remove client from cookies`() {
        every { cookieService.invalidateCookie(any(), any()) } returns Unit

        val response = repository.removeAuthorizedClient("registrationId", principal, exchange)
        expectThat(response.blockOptional()) {
            get(Optional<Void>::isEmpty).isTrue()
        }

        verify(exactly = 1) { cookieService.invalidateCookie(exchange, SPRING_SEC_OAUTH2_AUTHZ_CLIENT) }
    }
}
