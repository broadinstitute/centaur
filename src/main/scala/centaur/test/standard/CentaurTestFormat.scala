package centaur.test.standard

import cats.data.Validated.{Valid, _}
import centaur.test.standard.CentaurTestFormat._
import centaur.test.ErrorOr
import com.typesafe.config.Config
import configs.Result.{Failure, Success}
import configs.syntax._

sealed abstract class CentaurTestFormat(val name: String) {
  def testSpecString: String = this match {
    case WorkflowSuccessTest => "successfully run"
    case WorkflowFailureTest => "fail during execution"
    case RunTwiceExpectingCallCachingTest => "call cache the second run of"
    case RunTwiceExpectingNoCallCachingTest => "NOT call cache the second run of"
    case RunFailingTwiceExpectingNoCallCachingTest => "Fail the first run and NOT call cache the second run of"
    case CromwellRestartWithResume => "survive a Cromwell restart and resume jobs"
    case CromwellRestartWithoutResume => "survive a Cromwell restart"
  }
}

object CentaurTestFormat {

  case object WorkflowSuccessTest extends CentaurTestFormat("WorkflowSuccess")
  case object WorkflowFailureTest extends CentaurTestFormat("WorkflowFailure")
  case object RunTwiceExpectingCallCachingTest extends CentaurTestFormat("RunTwiceExpectingCallCaching")
  case object RunTwiceExpectingNoCallCachingTest extends CentaurTestFormat("RunTwiceExpectingNoCallCaching")
  case object RunFailingTwiceExpectingNoCallCachingTest extends CentaurTestFormat("RunFailingTwiceExpectingNoCallCaching")
  case object CromwellRestartWithResume extends CentaurTestFormat("CromwellRestartWithResume")
  case object CromwellRestartWithoutResume extends CentaurTestFormat("CromwellRestartWithoutResume")

  def fromConfig(conf: Config): ErrorOr[CentaurTestFormat] = {
    conf.get[String]("testFormat") match {
      case Success(f) => CentaurTestFormat.fromString(f)
      case Failure(_) => invalidNel("No testFormat string provided")
    }
  }

  def fromString(testFormat: String): ErrorOr[CentaurTestFormat] = {
    val formats = List(
      WorkflowSuccessTest,
      WorkflowFailureTest,
      RunTwiceExpectingCallCachingTest,
      RunTwiceExpectingNoCallCachingTest,
      RunFailingTwiceExpectingNoCallCachingTest,
      CromwellRestartWithResume,
      CromwellRestartWithoutResume)
    formats collectFirst {
      case format if format.name.equalsIgnoreCase(testFormat) => Valid(format)
    } getOrElse invalidNel(s"No such test format: $testFormat")
  }
}
