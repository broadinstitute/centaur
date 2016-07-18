package centaur.test.workflow

import java.nio.file.Path

import cats.data.Validated._
import centaur.CentaurConfig
import centaur.test._
import com.typesafe.config.Config
import configs.Result
import configs.syntax._
import spray.json._


case class WorkflowData(wdl: String, inputs: Option[String], options: Option[String])

case class WorkflowOptions(options: Option[String]) {
  import DefaultJsonProtocol._

  def insertSecrets(): WorkflowOptions = {
    val tokenKey = "refresh_token"

    def addToken(optionsMap: Map[String, JsValue]): Map[String, JsValue] = {
      CentaurConfig.optionalToken match {
        case Some(token) if optionsMap.get(tokenKey).isDefined => optionsMap - tokenKey + (tokenKey -> JsString(token))
        case _ => optionsMap
      }
    }

    options match {
      case Some(someOptions) =>
        val optionsMap = someOptions.parseJson.asJsObject.convertTo[Map[String, JsValue]]
        WorkflowOptions(Some(addToken(optionsMap).toJson.toString))
      case None => WorkflowOptions(options)
    }
  }
}

object WorkflowData {
  def fromConfig(conf: Config, basePath: Path): ErrorOr[WorkflowData] = {
    conf.get[Path]("wdl") match {
      case Result.Success(wdl) => Valid(WorkflowData(basePath.resolve(wdl), conf, basePath))
      case Result.Failure(_) => invalidNel("No wdl path provided")
    }
  }

  def apply(wdl: Path, conf: Config, basePath: Path): WorkflowData = {
    def getOptionalPath(name: String) = conf.get[Option[Path]](name) valueOrElse None map basePath.resolve
    // TODO: The slurps can throw - not a high priority but see #36
    WorkflowData(wdl.slurp, getOptionalPath("inputs") map { _.slurp }, getOptionalPath("options") map { _.slurp })
  }

  implicit class EnhancedPath(val path: Path) extends AnyVal {
    /** Read an entire file into a string, closing the underlying stream. */
    def slurp: String = {
      val source = io.Source.fromFile(path.toFile, "UTF-8")
      try source.mkString finally source.close()
    }
  }
}
