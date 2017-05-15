package centaur
import java.nio.file.{Path, Paths}

import cats.data.Validated.{Invalid, Valid}
import centaur.api.CentaurCromwellClient
import centaur.test.formulas.TestFormulas
import centaur.test.workflow.Workflow
import centaur.test.{CheckFiles, JesCheckFiles, LocalCheckFiles}
import com.google.cloud.storage._
import org.scalatest.{FlatSpec, Matchers, ParallelTestExecution}

object FinalDirsSpec {
  val FinalDir = Paths.get("src/main/resources/finalCopy")

  val OutputsDirLocalTest = FinalDir.resolve("final_workflow_outputs_local.test")
  val OutputsDirJesTest = FinalDir.resolve("final_workflow_outputs_jes.test")

  val LogDirLocalTest = FinalDir.resolve("final_workflow_log_dir_local.test")
  val LogDirJesTest = FinalDir.resolve("final_workflow_log_dir_jes.test")

  val CallLogsDirLocalTest = FinalDir.resolve("final_call_logs_dir_local.test")
  val CallLogsDirJesTest = FinalDir.resolve("final_call_logs_dir_jes.test")
}


class FinalDirsSpec extends FlatSpec with Matchers with ParallelTestExecution {
  import FinalDirsSpec._

  def testFinalOutputs(path: Path, option: String, backend: CheckFiles) = 
    Workflow.fromPath(path) match {
      case Valid(w) => TestFormulas.runFinalDirsWorkflow(w, option, backend).run.get
      case Invalid(e) => fail(s"Could not read logs test:\n -${e.toList.mkString("\n-")}")
    }

  List(
    // Local
    ("final outputs" should "place files in output dir [Local]", OutputsDirLocalTest, "final_workflow_outputs_dir", LocalCheckFiles()),
    ("final logs dir" should "place log files in output dir when requested [Local]", LogDirLocalTest, "final_workflow_log_dir", LocalCheckFiles()),
    ("final call logs dir" should "place call files in output dir when requested [Local]", CallLogsDirLocalTest, "final_call_logs_dir", LocalCheckFiles()),
    // Jes
    ("final outputs" should "place files in output dir [Jes]", OutputsDirJesTest, "final_workflow_outputs_dir", JesCheckFiles(StorageOptions.getDefaultInstance.getService)),
    ("final logs dir" should "place log files in output dir when requested [Jes]", LogDirJesTest, "final_workflow_log_dir", JesCheckFiles(StorageOptions.getDefaultInstance.getService)),
    ("final call logs dir" should "place call files in GCS dir when requested [Jes]", CallLogsDirJesTest, "final_call_logs_dir", JesCheckFiles(StorageOptions.getDefaultInstance.getService))
  ) foreach {
    case (verb, path, option, backend) => Workflow.fromPath(path) match {
      case Valid(w) if w.backends forall CentaurCromwellClient.backends.get.supportedBackends.contains =>
        verb in { TestFormulas.runFinalDirsWorkflow(w, option, backend).run.get }
      case Valid(w) => verb ignore {}
      case Invalid(e) => fail(s"Could not read logs test:\n -${e.toList.mkString("\n-")}")
    }
  }
}
