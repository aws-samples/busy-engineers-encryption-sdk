/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package example;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class KMSRequestCountingLogAppender extends AppenderSkeleton {
    private static final ThreadLocal<AtomicLong> REQUEST_COUNTER = ThreadLocal.withInitial(AtomicLong::new);

    public static long getCount() {
        return REQUEST_COUNTER.get().get();
    }

    public static long resetCount() {
        return REQUEST_COUNTER.get().getAndSet(0);
    }

    @Override protected void append(final LoggingEvent loggingEvent) {
        if (loggingEvent.getMessage().toString().startsWith("Sending Request: POST https://kms.")) {
            REQUEST_COUNTER.get().incrementAndGet();
        }
    }

    @Override public void close() {
        // no-op
    }

    @Override public boolean requiresLayout() {
        return false;
    }
}
