package centaur.test

import java.nio.file.{Path, Paths}

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import centaur.CentaurConfig
import centaur.api.CentaurCromwellClient
import centaur.test.standard.{CentaurTestCase, CentaurTestFormat}
import centaur.test.submit.{SubmitHttpResponse, SubmitWorkflowResponse}
import centaur.test.workflow.{AllBackendsRequired, Workflow, WorkflowData}
import cromwell.api.model.{Aborted, Failed, NonTerminalStatus, Succeeded, WorkflowId}

/**
  * Runs workflows in a "cwl-runner" friendly way.
  *
  * https://github.com/broadinstitute/cromwell/issues/2590
  * https://github.com/common-workflow-language/common-workflow-language/blob/v1.0.1/CONFORMANCE_TESTS.md
  * https://github.com/common-workflow-language/common-workflow-language/blob/v1.0.1/draft-3/cwl-runner.cwl#L5-L68
  * https://github.com/common-workflow-language/common-workflow-language/pull/278/files#diff-ee814a9c027fc9750beb075c283a973cR49
  */
object CentaurCwlRunner {

  case class CommandLineArguments(workflowSource: Option[Path] = None,
                                  workflowInputs: Option[Path] = None,
                                  quiet: Boolean = false,
                                  outdir: Path = Paths.get("."))

  // TODO: This would be cleaner with Enumeratum
  object ExitCode extends Enumeration {

    protected case class Val(status: Int) extends super.Val

    implicit class ValueToVal(val exitCodeValue: Value) extends AnyVal {
      def status: Int = exitCodeValue.asInstanceOf[Val].status
    }

    val Success = Val(0)
    val Failure = Val(1)
    val NotImplemented = Val(33)
  }

  private val parser = buildParser()

  private def showUsage(): ExitCode.Value = {
    parser.showUsage()
    ExitCode.Failure
  }

  private def buildParser(): scopt.OptionParser[CommandLineArguments] = {
    // TODO: Read real version from a config? Sync with build.sbt
    val centaurVersion = "1.0"

    new scopt.OptionParser[CommandLineArguments]("java -jar /path/to/centaur.jar") {
      head("centaur-cwl-runner", centaurVersion)

      help("help").text("Centaur CWL Runner - Cromwell integration testing environment")

      version("version").text("Print version and exit")

      arg[String]("workflow-source").text("Workflow source file.").required().
        action((s, c) => c.copy(workflowSource = Option(Paths.get(s))))

      arg[String]("inputs").text("Workflow inputs file.").optional().
        action((s, c) => c.copy(workflowInputs = Option(Paths.get(s))))

      opt[Unit]("quiet").text("Only print warnings and errors.").optional().
        action((_, c) => c.copy(quiet = true))

      // TODO: Use the outdir.
      opt[String]("outdir").text("Output directory, default current directory. Currently ignored.").optional().
        action((s, c) =>
          c.copy(outdir = Paths.get(s)))
    }
  }

  private def runCentaur(args: CommandLineArguments): ExitCode.Value = {
    import better.files._
    val workflowPath = File(args.workflowSource.get)
    val testName = workflowPath.name
    val workflowContents = workflowPath.contentAsString
    val inputContents = args.workflowInputs.map(File(_).contentAsString)
    val workflowType = workflowPath.extension(includeDot = false)
    val workflowTypeVersion = None
    val optionsContents = None
    val labels = List.empty
    val zippedImports = None
    val backends = AllBackendsRequired(List.empty)
    val workflowMetadata = None
    val notInMetadata = List.empty
    val directoryContentCounts = None
    val testFormat = CentaurTestFormat.WorkflowSuccessTest
    val testOptions = TestOptions(List.empty, ignore = false)
    val submitResponseOption = None

    val workflowData = WorkflowData(
      workflowContents, workflowType, workflowTypeVersion, inputContents, optionsContents, labels, zippedImports)
    val workflow = Workflow(testName, workflowData, workflowMetadata, notInMetadata, directoryContentCounts, backends)
    val testCase = CentaurTestCase(workflow, testFormat, testOptions, submitResponseOption)

    if (!args.quiet) {
      println(s"Starting test for $workflowPath")
    }

    try {
      testCase.testFunction.run.get match {
        case unexpected: SubmitHttpResponse =>
          println(s"Unexpected response: $unexpected")
          ExitCode.Failure
        case SubmitWorkflowResponse(submittedWorkflow) =>
          val status = CentaurCromwellClient.status(submittedWorkflow).get
          status match {
            case unexpected: NonTerminalStatus =>
              println(s"Unexpected status: $unexpected")
              ExitCode.Failure
            case Aborted =>
              println(s"Unexpected abort.")
              ExitCode.Failure
            case Failed =>
              println(s"Unexpected failure.")
              ExitCode.Failure
            case Succeeded =>
              /*
              TODO: Using our own HTTP request instead of the CentaurCromwellClient.
              The `WorkflowOutputs` currently tries to decode _all_ outputs to Map[String, String].
              The values need to be a _any_ JsValue, not just JsString, including JsNumber for three_step.
               */

              def workflowSpecificEndpoint(workflowId: WorkflowId, endpoint: String) =
                s"${CentaurCromwellClient.cromwellClient.submitEndpoint}/$workflowId/$endpoint"

              val outputsEndpoint = workflowSpecificEndpoint(submittedWorkflow.id, "outputs")

              def responseFuture() = {
                import CentaurCromwellClient._
                for {
                  response <- Http().singleRequest(HttpRequest(uri = outputsEndpoint))
                  strictEntity <- response.entity.toStrict(CentaurConfig.sendReceiveTimeout)
                  stringEntity = strictEntity.data.utf8String
                } yield stringEntity
              }

              val response = CentaurCromwellClient.sendReceiveFutureCompletion(responseFuture).get
              val outputs = {
                import spray.json._
                response.parseJson.asJsObject.getFields("outputs").head.prettyPrint
              }

              println(outputs)

              ExitCode.Success
          }
      }
    } finally {
      // Especially useful for when an exception occurs.
      CentaurCromwellClient.system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val parsedArgsOption = parser.parse(args, CommandLineArguments())
    val exitCode = parsedArgsOption match {
      case Some(parsedArgs) => runCentaur(parsedArgs)
      case None => showUsage()
    }
    System.exit(exitCode.status)
  }
}
