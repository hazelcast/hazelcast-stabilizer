/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.simulator.tests.platform.nexmark;

import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.datamodel.KeyedWindowResult;
import com.hazelcast.jet.datamodel.Tuple2;
import com.hazelcast.jet.datamodel.WindowResult;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.simulator.test.annotations.Prepare;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.TimeStep;
import com.hazelcast.simulator.tests.platform.nexmark.model.Bid;

import java.util.List;

import static com.hazelcast.function.ComparatorEx.comparing;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.aggregate.AggregateOperations.topN;
import static com.hazelcast.jet.pipeline.WindowDefinition.sliding;
import static com.hazelcast.jet.pipeline.WindowDefinition.tumbling;
import static com.hazelcast.simulator.tests.platform.nexmark.processor.EventSourceP.eventSource;

public class Q05HotItemsTest extends BenchmarkBase {
    // properties
    public int eventsPerSecond = 100_000;
    public int numDistinctKeys = 1_000;
    public long windowSize = 1_000L;
    public long slideBy = 2_000L;
    public String pgString = "none"; // none, at-least-once or exactly-once:
    public long snapshotIntervalMillis = 1_000;
    public int warmupSeconds = 5;
    public int measurementSeconds = 55;
    public int latencyReportingThresholdMs = 10;

    private Job job;

    @Override
    StreamStage<Tuple2<Long, Long>> addComputation(Pipeline pipeline) throws ValidationException {
        int eventsPerSecond = this.eventsPerSecond;
        int numDistinctKeys = this.numDistinctKeys;
        long windowSize = this.windowSize;
        long slideBy = this.slideBy;
        StreamStage<Bid> bids = pipeline
                .readFrom(eventSource("bids", eventsPerSecond, INITIAL_SOURCE_DELAY_MILLIS, (seq, timestamp) ->
                        new Bid(seq, timestamp, seq % numDistinctKeys, 0)))
                .withNativeTimestamps(0);

        // NEXMark Query 5 start
        StreamStage<WindowResult<List<KeyedWindowResult<Long, Long>>>> queryResult = bids
                .window(sliding(windowSize, slideBy))
                .groupingKey(Bid::auctionId)
                .aggregate(counting())
                .window(tumbling(slideBy))
                .aggregate(topN(10, comparing(KeyedWindowResult::result)));
        // NEXMark Query 5 end

        return queryResult.apply(determineLatency(WindowResult::end));
    }

    @Prepare(global = true)
    public void prepareJetJob() {
        ProcessingGuarantee guarantee = ProcessingGuarantee.valueOf(pgString.toUpperCase().replace('-', '_'));
        BenchmarkProperties props = new BenchmarkProperties(
                eventsPerSecond,
                numDistinctKeys,
                windowSize,
                slideBy,
                guarantee,
                snapshotIntervalMillis,
                warmupSeconds,
                measurementSeconds,
                latencyReportingThresholdMs
        );
        job = this.run(targetInstance, props);
    }

    @TimeStep
    public void doNothing() throws Exception {
        Thread.sleep(10);
    }

    @Teardown(global = true)
    public void tearDownJetJob() {
        job.cancel();
    }
}

