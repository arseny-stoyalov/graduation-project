package gp

import java.util.concurrent.{ForkJoinPool, SynchronousQueue, ThreadPoolExecutor, TimeUnit}
import scala.concurrent.ExecutionContext

package object entrypoints {

  private[entrypoints] lazy val parallelism = math.max(java.lang.Runtime.getRuntime.availableProcessors(), 4)

  private[entrypoints] val logicScheduler = ExecutionContext
    .fromExecutor {
      new ForkJoinPool(parallelism)
    }

  private[entrypoints] val ioScheduler = ExecutionContext
    .fromExecutor {
      new ThreadPoolExecutor(
        0,
        Int.MaxValue,
        60,
        TimeUnit.SECONDS,
        new SynchronousQueue[Runnable](false)
      )
    }

}
