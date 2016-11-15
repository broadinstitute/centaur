package centaur.test.workflow

import java.nio.file.Path

import cats.data.Validated._
import centaur.test._
import com.typesafe.config.Config
import configs.Result
import configs.syntax._

case class WorkflowData(wdl: String, inputs: Option[String], options: Option[String])

object WorkflowData {
  def fromConfig(conf: Config, basePath: Path): ErrorOr[WorkflowData] = {
    lazy val imports = conf.get[List[Path]]("imports") match {
      case Result.Success(importPaths) => importPaths map basePath.resolve
      case Result.Failure(_) => List.empty
    }
    
    conf.get[Path]("wdl") match {
      case Result.Success(wdl) => Valid(WorkflowData(basePath.resolve(wdl), conf, basePath, imports))
      case Result.Failure(_) => invalidNel("No wdl path provided")
    }
  }

  def apply(wdl: Path, conf: Config, basePath: Path, imports: List[Path]): WorkflowData = {
    def getOptionalPath(name: String) = conf.get[Option[Path]](name) valueOrElse None map basePath.resolve
    // TODO: The slurps can throw - not a high priority but see #36
    val wdlContent = wdl.slurp
    val postProcessedWdlContent = imports.foldLeft(wdlContent)((acc, importPath) => {
      acc.replaceFirst("<<IMPORT>>", importPath.toAbsolutePath.toString)
    })
    WorkflowData(postProcessedWdlContent, getOptionalPath("inputs") map { _.slurp }, getOptionalPath("options") map { _.slurp })
  }

  implicit class EnhancedPath(val path: Path) extends AnyVal {
    /** Read an entire file into a string, closing the underlying stream. */
    def slurp: String = {
      val source = io.Source.fromFile(path.toFile, "UTF-8")
      try source.mkString finally source.close()
    }
  }
}
