package centaur

import java.nio.file.Path

import scala.language.postfixOps
import org.scalatest._

/**
  * Tests with this tag expect their initial submission to Cromwell to fail
  */
object SubmissionFailureTest extends Tag("SubmissionFailureTest")
/**
  * Tests with this tag expect to fail
  */
object FailureTest extends Tag("FailureTest")
/**
  * Tests with this tag expect to succeed
  */
object SuccessTest extends Tag("SuccessTest")

object ShortTest extends Tag("ShortTest")


class CentaurSpec extends FlatSpec with Matchers with ParallelTestExecution{
  def testCases(basePath: Path): List[WorkflowRequest] = {
    basePath.toFile.listFiles.toList collect { case x if x.isDirectory => x.toPath } map WorkflowRequest.apply
  }

  testCases(CentaurConfig.successfulTestCasePath) foreach { case w =>
    if(w.name == "hello") {
      it should s"successfully run ${w.name}" taggedAs ShortTest in {
        TestFormulas.runSuccessfulWorkflow(w).run.get
      }
    }
  }

//  testCases(CentaurConfig.successfulTestCasePath) foreach { case w =>
//    it should s"successfully run ${w.name}" taggedAs SuccessTest in {
//      TestFormulas.runSuccessfulWorkflow(w).run.get
//    }
//  }
//
//    testCases(CentaurConfig.failingTestCasePath) foreach { case w =>
//      it should s"fail ${w.name}" taggedAs FailureTest in {
//        TestFormulas.runFailingWorkflow(w).run.get
//      }
//    }
//
//  testCases(CentaurConfig.submissionFailureTestCasePath) foreach { case w =>
//    it should s"fail ${w.name}" taggedAs SubmissionFailureTest in {
//      // TODO: This returns a string error message. With extra test metadata, we can verify that the error message is correct!
//      TestFormulas.runSubmissionFailureWorkflow(w).run.get
//    }
//  }
}
