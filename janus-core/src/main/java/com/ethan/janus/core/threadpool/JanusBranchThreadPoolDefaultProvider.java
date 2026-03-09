package com.ethan.janus.core.threadpool;

import com.ethan.janus.core.exception.JanusException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class JanusBranchThreadPoolDefaultProvider implements JanusBranchThreadPoolMetricsProvider {

    @Override
    public int getQueueSize(ExecutorService executor) {
        this.checkExecutorType(executor);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        return threadPoolExecutor.getQueue().size();
    }

    @Override
    public int getQueueCapacity(ExecutorService executor) {
        this.checkExecutorType(executor);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        // 线程池队列
        BlockingQueue<Runnable> queue = threadPoolExecutor.getQueue();
        // 队列当前 size
        int currentSize = queue.size();
        // 队列剩余 size
        int remainingCapacity = queue.remainingCapacity();
        // 队列总 size
        return currentSize + remainingCapacity;
    }

    private void checkExecutorType(ExecutorService executor) {
        if (!(executor instanceof ThreadPoolExecutor)) {
            throw new JanusException("线程池[janusBranchThreadPool]并非 java.util.concurrent.ThreadPoolExecutor 类型，" +
                    "而是使用者自定义的[" + executor.getClass().getName() + "]类型。" +
                    "\n请自己提供 JanusBranchThreadPoolMetricsProvider 接口实现类。");
        }
    }
}
