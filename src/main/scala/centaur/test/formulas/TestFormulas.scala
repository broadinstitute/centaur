package centaur.test.formulas

import cats.syntax.functor._
import cats.syntax.flatMap._
import centaur.{CentaurConfig, CromwellManager, ManagedCromwellServer}
import centaur.test.Operations._
import centaur.test.Test
import centaur.test.Test.testMonad
import centaur.test.workflow.Workflow
import cromwell.api.model.{Failed, SubmittedWorkflow, Succeeded, TerminalStatus}

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
    _ <- validateDirectoryContentsCounts(workflowDefinition, w)
  } yield ()

  def runFailingWorkflowAndVerifyMetadata(workflowDefinition: Workflow): Test[Unit] = for {
    w <- runFailingWorkflow(workflowDefinition)
    _ <- validateMetadata(w, workflowDefinition)
    _ <- validateDirectoryContentsCounts(workflowDefinition, w)
  } yield ()

  def runWorkflowTwiceExpectingCaching(workflowDefinition: Workflow): Test[Unit] = {
    for {
      firstWF <- runSuccessfulWorkflow(workflowDefinition)
      secondWf <- runSuccessfulWorkflow(workflowDefinition)
      metadata <- validateMetadata(secondWf, workflowDefinition, Option(firstWF.id.id))
      _ <- validateNoCacheMisses(metadata, workflowDefinition.testName)
      _ <- validateDirectoryContentsCounts(workflowDefinition, secondWf)
    } yield ()
  }

  def runWorkflowTwiceExpectingNoCaching(workflowDefinition: Workflow): Test[Unit] = {
    for {
      _ <- runSuccessfulWorkflow(workflowDefinition) // Build caches
      testWf <- runSuccessfulWorkflow(workflowDefinition)
      metadata <- validateMetadata(testWf, workflowDefinition)
      _ <- validateNoCacheHits(metadata, workflowDefinition.testName)
      _ <- validateDirectoryContentsCounts(workflowDefinition, testWf)
    } yield ()
  }

  def runFailingWorkflowTwiceExpectingNoCaching(workflowDefinition: Workflow): Test[Unit] = {
    for {
      _ <- runFailingWorkflow(workflowDefinition) // Build caches
      testWf <- runFailingWorkflow(workflowDefinition)
      metadata <- validateMetadata(testWf, workflowDefinition)
      _ <- validateNoCacheHits(metadata, workflowDefinition.testName)
      _ <- validateDirectoryContentsCounts(workflowDefinition, testWf)
    } yield ()
  }
  
  private def cromwellRestart(workflowDefinition: Workflow, testRecover: Boolean): Test[Unit] = CentaurConfig.runMode match {
    case ManagedCromwellServer(_, postRestart, withRestart) if withRestart =>
      for {
        w <- submitWorkflow(workflowDefinition)
        jobId <- pollUntilCallIsRunning(w, "cromwell_restart.cromwell_killer")
        _ = CromwellManager.stopCromwell()
        _ = CromwellManager.startCromwell(postRestart)
        _ <- pollUntilStatus(w, Succeeded)
        _ <- validateMetadata(w, workflowDefinition)
        _ <- if(testRecover) validateRecovered(w, "cromwell_restart.cromwell_killer", jobId) else Test.successful(())
        _ <- validateDirectoryContentsCounts(workflowDefinition, w)
      } yield ()
    case _ => runSuccessfulWorkflowAndVerifyMetadata(workflowDefinition)
  }
  
  def cromwellRestartWithRecover(workflowDefinition: Workflow): Test[Unit] = cromwellRestart(workflowDefinition, testRecover = true)
  def cromwellRestartWithoutRecover(workflowDefinition: Workflow): Test[Unit] = cromwellRestart(workflowDefinition, testRecover = false)
}
