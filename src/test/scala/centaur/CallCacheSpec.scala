package centaur

import java.nio.file.Path

import org.scalatest.{ParallelTestExecution, Matchers, FlatSpec}

class CallCacheSpec extends FlatSpec with Matchers with ParallelTestExecution {

  def testCases(basePath: Path): List[WorkflowRequest] = {
    basePath.toFile.listFiles.toList collect { case x if x.isDirectory => x.toPath } map WorkflowRequest.apply
  }

  testCases(CentaurConfig.callCacheTestCasePath) foreach { case w =>
    if (w.name == "readFromCache" || w.name == "writeToCache" ) {
      w.name should s"successfully run ${w.name}" in {
        CacheFormulas.runCachingTurnedOffWorkflow(w).run.get //check the caching expecations
        Thread.sleep(1000)
      }
    }
  }

  testCases(CentaurConfig.callCacheTestCasePath) foreach { case w =>
    if (w.name == "A_cacheWithinWF") {
      w.name should s"successfully run ${w.name}" in {
        TestFormulas.runCachingWorkflow(w).run.get
        Thread.sleep(1000)
      }
    }
  }

  testCases(CentaurConfig.callCacheTestCasePath) foreach { case w =>
    if (w.name == "B_cacheBetweenWF") {
      w.name can s"successfully run ${w.name}" in {
        CacheFormulas.runCachingWorkflow(w).run.get
        Thread.sleep(1000)
      }
    }
  }

}