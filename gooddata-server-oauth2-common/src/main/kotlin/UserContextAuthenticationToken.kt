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
package com.gooddata.oauth2.server.common

import org.springframework.security.authentication.AbstractAuthenticationToken

class UserContextAuthenticationToken(
    val organization: Organization,
    val user: User,
) : AbstractAuthenticationToken(emptyList()) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): String = user.id

    override fun getPrincipal(): String = user.id
}
