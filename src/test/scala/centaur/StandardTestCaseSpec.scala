package centaur

import java.nio.file.Path

import cats.data.Validated.{Invalid, Valid}
import cats.syntax.traverse._
import cats.std.list._
import centaur.test.Test
import centaur.test.standard.{ChainedTestCases, StandardTestCase}
import centaur.test.workflow.Workflow

import scala.language.postfixOps
import org.scalatest._

class StandardTestCaseSpec extends FlatSpec with Matchers with ParallelTestExecution {
  def testCases(basePath: Path): List[StandardTestCase] = {
    // IntelliJ will give some red squiggles in the following block. It lies.
    basePath.toFile.listFiles.toList collect { case x if x.isFile => x.toPath } traverse StandardTestCase.fromPath match {
      case Valid(l) => l
      case Invalid(e) => throw new IllegalStateException("\n" + e.unwrap.mkString("\n") + "\n")
    }
  }

  // Optional test cases are provided by the end user as opposed to the ones built in to the system
  val optionalTestCases = CentaurConfig.optionalTestPath map testCases getOrElse List.empty
  optionalTestCases ++ testCases(CentaurConfig.standardTestCasePath) foreach {
    case t => executeStandardTest(t, t.testFunction)
  }

  def executeStandardTest(testCase: StandardTestCase, f: Workflow => Test[_]): Unit = {
    def nameTest = it should s"${testCase.testFormat.testSpecString} ${testCase.workflow.name}"
    def runTest = f(testCase.workflow).run.get

    // Make tags, but enforce lowercase:
    val tags = (testCase.testOptions.tags :+ testCase.workflow.name :+ testCase.testFormat.name) map { x => Tag(x.toLowerCase) }

    tags match {
      case Nil => runOrDont(nameTest, testCase.testOptions.ignore, runTest)
      case head :: Nil => runOrDont(nameTest taggedAs head, testCase.testOptions.ignore, runTest)
      case head :: tail => runOrDont(nameTest taggedAs(head, tail: _*), testCase.testOptions.ignore, runTest)
    }
  }

//  def findTest(name: String): StandardTestCase = {
//    testCases(CentaurConfig.standardTestCasePath) foreach {
//      case t if t.workflow.name == name => t
//    }
//  }
//
//  val sequentialTestCases = Map("direct_logs" -> "find_logs") foreach {
//    case (k, v) => executeChainedTest(findTest(v), findTest(k), findTest(v).testFunction)
//  }
//
//  def executeChainedTest(primaryCase: StandardTestCase, secondaryCase: StandardTestCase, f: Workflow => Test[_]): Unit = {
//    def nameTests = it should s"${primaryCase.testFormat.testSpecString} ${primaryCase.workflow.name} \n" +
//                             s"and ${secondaryCase.testFormat.testSpecString} ${secondaryCase.workflow.name}"
//    def runTests = { ChainedTestCases(primaryCase, secondaryCase) }
//  }

  private def runOrDont(itVerbString: ItVerbString, ignore: Boolean, runTest: => Any) = {
    if (ignore) {
      itVerbString ignore runTest
    } else {
      itVerbString in runTest
    }
  }

  private def runOrDont(itVerbStringTaggedAs: ItVerbStringTaggedAs, ignore: Boolean, runTest: => Any) = {
    if (ignore) {
      itVerbStringTaggedAs ignore runTest
    } else {
      itVerbStringTaggedAs in runTest
    }
  }
}
