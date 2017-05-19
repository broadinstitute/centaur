package centaur.test.formulas

import java.util.UUID

import scala.collection.JavaConverters._
import centaur.test.CheckFiles
import cats.syntax.eq._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats._
import centaur.test.Operations._
import centaur.test.Test
import centaur.test.Test.testMonad
import centaur.test.workflow.Workflow
import centaur.test.workflow.Workflow.{WorkflowWithMetadata, WorkflowWithoutMetadata}
import com.google.cloud.storage.Storage.BucketListOption
import com.google.cloud.storage.{Storage, StorageOptions}
import cromwell.api.model.{Failed, SubmittedWorkflow, Succeeded, TerminalStatus}
import spray.json._
import io.circe._
import io.circe.parser._


/**
  * A collection of test formulas which can be used, building upon operations by chaining them together via a
  * for comprehension. These assembled formulas can then be run by a client
  */
object TestFormulas {
  private def runWorkflowUntilTerminalStatus(workflow: Workflow, status: TerminalStatus): Test[SubmittedWorkflow] = {
    for {
      s <- submitWorkflow(workflow)
      _ <- pollUntilStatus(s, status)
    } yield s
  }

  private def runSuccessfulWorkflow(workflow: Workflow): Test[SubmittedWorkflow] = runWorkflowUntilTerminalStatus(workflow, Succeeded)
  private def runFailingWorkflow(workflow: Workflow): Test[SubmittedWorkflow] = runWorkflowUntilTerminalStatus(workflow, Failed)

  def runSuccessfulWorkflowAndVerifyMetadata(workflowDefinition: Workflow): Test[Unit] = for {
    w <- runSuccessfulWorkflow(workflowDefinition)
    _ <- validateMetadata(w, workflowDefinition)
  } yield ()

  def runFailingWorkflowAndVerifyMetadata(workflowDefinition: Workflow): Test[Unit] = for {
    w <- runFailingWorkflow(workflowDefinition)
    _ <- validateMetadata(w, workflowDefinition)
  } yield ()

  def runWorkflowTwiceExpectingCaching(workflowDefinition: Workflow): Test[Unit] = {
    for {
      firstWF <- runSuccessfulWorkflow(workflowDefinition)
      secondWf <- runSuccessfulWorkflow(workflowDefinition)
      metadata <- validateMetadata(secondWf, workflowDefinition, Option(firstWF.id.id))
      _ <- validateNoCacheMisses(metadata, workflowDefinition.testName)
    } yield ()
  }

  def runWorkflowTwiceExpectingNoCaching(workflowDefinition: Workflow): Test[Unit] = {
    for {
      _ <- runSuccessfulWorkflow(workflowDefinition) // Build caches
      testWf <- runSuccessfulWorkflow(workflowDefinition)
      metadata <- validateMetadata(testWf, workflowDefinition)
      _ <- validateNoCacheHits(metadata, workflowDefinition.testName)
    } yield ()
  }

  def runFinalDirsWorkflow(wf: Workflow, dirOption: String, checkFiles: CheckFiles): Test[Unit] =  {
    val options = wf.data.options.get
    val outputDirectory = parse(options).toOption.flatMap(_.findAllByKey(dirOption).head.asString).get

    checkFiles.deleteExistingFiles(outputDirectory)

    for {
      terminatedWf <- runWorkflowUntilTerminalStatus(wf, Succeeded)
      _ = if (checkFiles.checkDirectorySize(outputDirectory) == 0)
            throw new RuntimeException("no files in output dir!")
          else ()
    } yield ()
  }

}
