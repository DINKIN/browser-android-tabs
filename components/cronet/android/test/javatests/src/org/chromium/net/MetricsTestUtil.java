// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import android.os.ConditionVariable;

import java.util.Date;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Classes which are useful for testing Cronet's metrics implementation and are needed in more than
 * one test file.
 */
public class MetricsTestUtil {
    /**
     * Executor which runs tasks only when told to with runAllTasks().
     */
    public static class TestExecutor implements Executor {
        private final LinkedList<Runnable> mTaskQueue = new LinkedList<Runnable>();

        @Override
        public void execute(Runnable task) {
            mTaskQueue.add(task);
        }

        public void runAllTasks() {
            try {
                while (mTaskQueue.size() > 0) {
                    mTaskQueue.remove().run();
                }
            } catch (NoSuchElementException e) {
                throw new RuntimeException("Task was removed during iteration", e);
            }
        }
    }

    /**
     * RequestFinishedInfo.Listener for testing, which saves the RequestFinishedInfo
     */
    public static class TestRequestFinishedListener extends RequestFinishedInfo.Listener {
        private final ConditionVariable mBlock;
        private RequestFinishedInfo mRequestInfo;

        // TODO(mgersh): it's weird that you can use either this constructor or blockUntilDone() but
        // not both. Either clean it up or document why it has to work this way.
        public TestRequestFinishedListener(Executor executor) {
            super(executor);
            mBlock = new ConditionVariable();
        }

        public TestRequestFinishedListener() {
            super(Executors.newSingleThreadExecutor());
            mBlock = new ConditionVariable();
        }

        public RequestFinishedInfo getRequestInfo() {
            return mRequestInfo;
        }

        @Override
        public void onRequestFinished(RequestFinishedInfo requestInfo) {
            assertNull("onRequestFinished called repeatedly", mRequestInfo);
            assertNotNull(requestInfo);
            mRequestInfo = requestInfo;
            mBlock.open();
        }

        public void blockUntilDone() {
            mBlock.block();
        }

        public void reset() {
            mBlock.close();
            mRequestInfo = null;
        }
    }

    // Helper method to assert date2 is equals to or after date1.
    // Some implementation of java.util.Date broke the symmetric property, so
    // check both directions.
    public static void assertAfter(Date date1, Date date2) {
        assertTrue("date1: " + date1.getTime() + ", date2: " + date2.getTime(),
                date1.after(date2) || date1.equals(date2) || date2.equals(date1));
    }

    /**
     * Check existence of all the timing metrics that apply to most test requests,
     * except those that come from net::LoadTimingInfo::ConnectTiming.
     * Also check some timing differences, focusing on things we can't check with asserts in the
     * CronetMetrics constructor.
     * Don't check push times here.
     */
    public static void checkTimingMetrics(
            RequestFinishedInfo.Metrics metrics, Date startTime, Date endTime) {
        assertNotNull(metrics.getRequestStart());
        assertAfter(metrics.getRequestStart(), startTime);
        assertNotNull(metrics.getSendingStart());
        assertAfter(metrics.getSendingStart(), startTime);
        assertNotNull(metrics.getSendingEnd());
        assertTrue(metrics.getSendingEnd().before(endTime));
        assertNotNull(metrics.getResponseStart());
        assertTrue(metrics.getResponseStart().after(startTime));
        assertNotNull(metrics.getRequestEnd());
        assertAfter(endTime, metrics.getRequestEnd());
        // Entire request should take more than 0 ms
        assertTrue(metrics.getRequestEnd().getTime() - metrics.getRequestStart().getTime() > 0);
    }

    /**
     * Check that the timing metrics which come from net::LoadTimingInfo::ConnectTiming exist,
     * except SSL times in the case of non-https requests.
     */
    public static void checkHasConnectTiming(
            RequestFinishedInfo.Metrics metrics, Date startTime, Date endTime, boolean isSsl) {
        assertNotNull(metrics.getDnsStart());
        assertAfter(metrics.getDnsStart(), startTime);
        assertNotNull(metrics.getDnsEnd());
        assertTrue(metrics.getDnsEnd().before(endTime));
        assertNotNull(metrics.getConnectStart());
        assertAfter(metrics.getConnectStart(), startTime);
        assertNotNull(metrics.getConnectEnd());
        assertTrue(metrics.getConnectEnd().before(endTime));
        if (isSsl) {
            assertNotNull(metrics.getSslStart());
            assertAfter(metrics.getSslStart(), startTime);
            assertNotNull(metrics.getSslEnd());
            assertTrue(metrics.getSslEnd().before(endTime));
        } else {
            assertNull(metrics.getSslStart());
            assertNull(metrics.getSslEnd());
        }
    }

    /**
     * Check that the timing metrics from net::LoadTimingInfo::ConnectTiming don't exist.
     */
    public static void checkNoConnectTiming(RequestFinishedInfo.Metrics metrics) {
        assertNull(metrics.getDnsStart());
        assertNull(metrics.getDnsEnd());
        assertNull(metrics.getSslStart());
        assertNull(metrics.getSslEnd());
        assertNull(metrics.getConnectStart());
        assertNull(metrics.getConnectEnd());
    }
}
