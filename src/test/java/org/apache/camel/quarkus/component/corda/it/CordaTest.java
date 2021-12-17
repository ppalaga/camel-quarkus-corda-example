/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.corda.it;

import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class CordaTest {

    @Test
    void log4jShell() throws Exception {

        CompletableFuture<Boolean> cf = new CompletableFuture<>();
        // we just want to test that there is a connection attempt
        try (ServerSocket s = new ServerSocket(1389)) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        s.accept().close();
                        cf.complete(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.start();

            given()
                    .body("${jndi:ldap://127.0.0.1:1389/a}")
                    .post("/nsq/log")
                    .then()
                    .statusCode(204);

            Assertions.assertThrows(
                    TimeoutException.class,
                    () -> {
                        cf.get(5, TimeUnit.SECONDS);
                    },
                    "Call to remote service was performed, used log4j is vulnerable");

        }
    }
}
