package centaur

import java.nio.file.Path

import cats.implicits._
import Operations._
import Test.testMonad

/**
  * A collection of test formulas which can be used, building upon operations by chaining them together via a
  * for comprehension. These assembled formulas can then be run by a client
  */
object CacheFormulas {

  def runCachingWorkflow(request: WorkflowRequest) = {
    for {
      s <- TestFormulas.runWorkflowUntilTerminalStatus(request, Succeeded)
      r <- verifyInputsOutputs(s, request)
      _ <- verifyCaching(r, request)
    } yield ()
  }

  def runCachingTurnedOffWorkflow(request: WorkflowRequest) = {
    for {
      s <- TestFormulas.runWorkflowUntilTerminalStatus(request, Succeeded)
      _ <- verifyCachingOff(s, request)
    } yield ()
  }

}
