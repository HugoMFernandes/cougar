/*
 * Copyright #{YEAR}, The Sporting Exchange Limited
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

package com.betfair.cougar.client.api;

import com.betfair.cougar.api.ExecutionContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public class CompoundContextEmitter<HR,C> implements ContextEmitter<HR,C> {
    private CopyOnWriteArrayList<ContextEmitter<HR,C>> emitters;

    public CompoundContextEmitter(List<ContextEmitter<HR, C>> emitters) {
        this.emitters = new CopyOnWriteArrayList<>(emitters);
    }

    public void addEmitter(ContextEmitter<HR, C> emitter) {
        emitters.add(emitter);
    }

    @Override
    public void emit(ExecutionContext ctx, HR request, C container) {
        for (ContextEmitter<HR,C> emitter : emitters) {
            emitter.emit(ctx, request, container);
        }
    }
}
