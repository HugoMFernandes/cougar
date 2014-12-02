/*
 * Copyright 2014, The Sporting Exchange Limited
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
 */

package com.betfair.cougar.client;

import com.betfair.cougar.api.ExecutionContext;
import com.betfair.cougar.api.RequestUUID;
import com.betfair.cougar.api.UUIDGenerator;
import com.betfair.cougar.api.geolocation.GeoLocationDetails;
import com.betfair.cougar.api.security.IdentityResolver;
import com.betfair.cougar.api.security.IdentityToken;
import com.betfair.cougar.api.security.IdentityTokenResolver;
import com.betfair.cougar.client.api.ContextEmitter;
import com.betfair.cougar.client.api.GeoLocationSerializer;
import com.betfair.cougar.transport.api.ExecutionContextComponent;
import com.betfair.cougar.util.RequestUUIDImpl;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * Standard context emitter for use with http client transports
 */
public class HttpContextEmitter<HR> implements ContextEmitter<HR, List<Header>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpContextEmitter.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime();
    private final String uuidHeader;
    private final String uuidParentsHeader;
    private final IdentityResolver identityResolver;
    private final GeoLocationSerializer geoLocationSerializer;
    private IdentityTokenResolver identityTokenResolver;

    public HttpContextEmitter(IdentityResolver identityResolver, IdentityTokenResolver identityTokenResolver, GeoLocationSerializer geoLocationSerializer, String uuidHeader, String uuidParentsHeader) {
        this.identityResolver = identityResolver;
        this.identityTokenResolver = identityTokenResolver;
        this.geoLocationSerializer = geoLocationSerializer;
        this.uuidHeader = uuidHeader;
        this.uuidParentsHeader = uuidParentsHeader;
    }

    private Set<ExecutionContextComponent> handling;

    @Override
    public ExecutionContextComponent[] handledComponents() {
        return ExecutionContextComponent.values();
    }

    @Override
    public void handling(Set<ExecutionContextComponent> actual) {
        this.handling = actual;
    }

    @Override
    public void emit(ExecutionContext ctx, HR request, List<Header> result) {
        if (handling.contains(ExecutionContextComponent.TraceLoggingEnabled)) {
            if (ctx.traceLoggingEnabled()) {
                result.add(new BasicHeader("X-Trace-Me", "true"));
            }
        }
        if (handling.contains(ExecutionContextComponent.Location)) {
            GeoLocationDetails gld = ctx.getLocation();
            if (gld != null) {
                geoLocationSerializer.serialize(gld, result);
            }
        }
        if (handling.contains(ExecutionContextComponent.RequestUuid)) {
            if (uuidHeader != null) {
                RequestUUID requestUUID = ctx.getRequestUUID() != null ? ctx.getRequestUUID().getNewSubUUID() : new RequestUUIDImpl();
                result.add(new BasicHeader(uuidHeader, requestUUID.getLocalUUIDComponent()));
                if (uuidParentsHeader != null && requestUUID.getRootUUIDComponent() != null) {
                    result.add(new BasicHeader(uuidParentsHeader, requestUUID.getRootUUIDComponent()+ UUIDGenerator.COMPONENT_SEPARATOR+requestUUID.getParentUUIDComponent()));
                }
            }
        }
        // time headers
        if (handling.contains(ExecutionContextComponent.ReceivedTime)) {
            if (ctx.getReceivedTime() != null) {
                result.add(new BasicHeader("X-ReceivedTime", DATE_TIME_FORMATTER.print(ctx.getReceivedTime().getTime())));
            }
        }
        if (handling.contains(ExecutionContextComponent.RequestedTime)) {
            result.add(new BasicHeader("X-RequestTime", DATE_TIME_FORMATTER.print(System.currentTimeMillis())));
        }
        if (handling.contains(ExecutionContextComponent.Identity)) {
            if (identityTokenResolver != null && identityResolver != null &&
                    identityTokenResolver.isRewriteSupported()) {
                final List<IdentityToken> identityTokens = identityResolver.tokenise(ctx.getIdentity());
                identityTokenResolver.rewrite(identityTokens, request);
                if (LOGGER.isDebugEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    for (IdentityToken it: identityTokens) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(it.getName()).append("=").append(it.getValue());
                    }
                    LOGGER.info("Rewrote tokens " + sb + " to http request");
                }
            }
        }
    }
}
