/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.queue.impl.single;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.wire.AbstractMarshallable;
import net.openhft.chronicle.wire.UnrecoverableTimeoutException;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * Created by Peter Lawrey on 22/05/16.
 */
public class SimpleStoreRecovery extends AbstractMarshallable implements StoreRecovery {
    @Override
    public long recoverIndex2Index(@NotNull LongValue index2Index, @NotNull Callable<Long> action, long timeoutMS) throws UnrecoverableTimeoutException {
        Jvm.warn().on(getClass(), "Rebuilding the index2index");
        index2Index.setValue(0);
        try {
            return action.call();
        } catch (Exception e) {
            throw Jvm.rethrow(e);
        }
    }

    @Override
    public long recoverSecondaryAddress(@NotNull LongArrayValues index2indexArr, int index2, @NotNull Callable<Long> action, long timeoutMS) throws UnrecoverableTimeoutException {
        Jvm.warn().on(getClass(), "Timed out trying to get index2index[" + index2 + "]");
        index2indexArr.setValueAt(index2, 0L);
        try {
            return action.call();

        } catch (TimeoutException e) {
            throw new UnrecoverableTimeoutException(e);

        } catch (Exception e) {
            throw Jvm.rethrow(e);
        }
    }

    @Override
    public long recoverAndWriteHeader(@NotNull Wire wire,
                                      int length,
                                      long timeoutMS,
                                      @NotNull final LongValue lastPosition) throws UnrecoverableTimeoutException {
        Jvm.warn().on(getClass(), "Clearing an incomplete header so a header can be written");
        wire.bytes().writeInt(0);
        wire.pauser().reset();
        try {
            return wire.writeHeader(length, timeoutMS, TimeUnit.MILLISECONDS, lastPosition);
        } catch (@NotNull TimeoutException | EOFException e) {
            throw new UnrecoverableTimeoutException(e);
        }
    }
}
