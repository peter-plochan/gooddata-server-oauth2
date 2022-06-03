/*
 * Copyright 2022 GoodData Corporation
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
package com.gooddata.oauth2.server.common

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.http.client.SimpleClientHttpRequestFactory

/**
 * A properties class holding parameters for custom http clients used for communication with Oauth2 provider.
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.client.http")
@ConstructorBinding
class HttpProperties(

    /**
     * A timeout for receiving some response on a request. (in milliseconds)
     * @see SimpleClientHttpRequestFactory.setReadTimeout
     */
    @DefaultValue("5000")
    val readTimeoutMillis: Int,

    /**
     * A timeout for establishing a TCP connection. (in milliseconds)
     * @see SimpleClientHttpRequestFactory.setConnectTimeout
     */
    @DefaultValue("30000")
    val connectTimeoutMillis: Int,
) {
    init {
        check(readTimeoutMillis > 0) {
            "The value of the property spring.security.oauth2.client.http.readTimeoutMillis should be positive"
        }
        check(connectTimeoutMillis > 0) {
            "The value of the property spring.security.oauth2.client.http.connectTimeoutMillis should be positive"
        }
    }
}
