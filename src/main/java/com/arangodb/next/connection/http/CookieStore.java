/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.next.connection.http;


import io.netty.handler.codec.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.arangodb.next.connection.http.HttpConnection.THREAD_PREFIX;

/**
 * @author Michele Rastelli
 */
final class CookieStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(CookieStore.class);

    // state managed by scheduler thread {@link HttpConnection.THREAD_PREFIX}
    private final Map<Cookie, Long> cookies = new HashMap<>();

    HttpClient addCookies(final HttpClient httpClient) {
        assert Thread.currentThread().getName().startsWith(THREAD_PREFIX) : "Wrong thread!";

        removeExpiredCookies();
        HttpClient c = httpClient;
        for (Cookie cookie : cookies.keySet()) {
            LOGGER.debug("sending cookie: {}", cookie);
            c = c.cookie(cookie);
        }
        return c;
    }

    void saveCookies(HttpClientResponse resp) {
        assert Thread.currentThread().getName().startsWith(THREAD_PREFIX) : "Wrong thread!";

        resp.cookies().values().stream().flatMap(Collection::stream)
                .forEach(cookie -> {
                    LOGGER.debug("saving cookie: {}", cookie);
                    cookies.put(cookie, new Date().getTime());
                });
    }

    void clear() {
        assert Thread.currentThread().getName().startsWith(THREAD_PREFIX) : "Wrong thread!";

        cookies.clear();
    }

    private void removeExpiredCookies() {
        long now = new Date().getTime();
        cookies.entrySet().removeIf(entry ->
                entry.getKey().maxAge() >= 0 && entry.getValue() + entry.getKey().maxAge() * 1000 < now);
    }

}
