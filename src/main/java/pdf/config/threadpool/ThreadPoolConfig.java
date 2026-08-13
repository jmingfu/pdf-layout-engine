package pdf.config.threadpool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 基于SpringBoot框架的个人练手项目-线程池配置
 *
 * @author JMF
 * @date 2026-08-12 16:58
 * @date 2026-08-12
 */
@Configuration
public class ThreadPoolConfig {
    @Bean("PrescriptionExecutor")
    public ExecutorService prescriptionExecutor() {
        // 根据服务器核心数和业务量配置线程池参数
        int corePoolSize = 12;
        int maxPoolSize = 16;
        long keepAliveTime = 1L;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(20);
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("pdf-generate-%d")
                .build();
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                handler
        );
    }
}
