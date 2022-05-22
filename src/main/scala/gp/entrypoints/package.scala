package gp

import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext

package object entrypoints {

  private[entrypoints] lazy val parallelism = math.max(java.lang.Runtime.getRuntime.availableProcessors(), 4)

  private[entrypoints] val logicScheduler = ExecutionContext
    .fromExecutor {
      new ForkJoinPool(parallelism)
    }

}
