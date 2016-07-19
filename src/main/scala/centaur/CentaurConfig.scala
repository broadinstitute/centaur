package centaur

import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.concurrent.TimeUnit
import com.typesafe.config.ConfigFactory
import configs.syntax._
import scala.concurrent.duration.FiniteDuration

object CentaurConfig {
  lazy val conf = ConfigFactory.load()
  lazy val cromwellUrl = new URL(conf.getString("centaur.cromwellUrl"))
  lazy val sendReceiveTimeout = conf.getDuration("centaur.sendReceiveTimeout").toScala
  lazy val maxWorkflowLength = conf.getDuration("centaur.maxWorkflowLength").toScala
  lazy val metadataConsistencyTimeout = conf.getDuration("centaur.metadataConsistencyTimeout").toScala

  lazy val standardTestCasePath = Paths.get(conf.getString("centaur.standardTestCasePath"))
  lazy val callCacheTestCasePath = Paths.get(conf.getString("centaur.callCacheTestCasePath"))

  // If provided, any tests will be appended to the tests in standardTestCasePath
  lazy val optionalTestPath: Option[Path] = conf.get[Option[Path]]("centaur.optionalTestPath") valueOrElse None
  // If provided, the token will become the default value for the workflow option "refresh_token"
  lazy val optionalToken: Option[String] = conf.get[Option[String]]("centaur.optionalToken") valueOrElse None

  implicit class EnhancedJavaDuration(val javaDuration: java.time.Duration) extends AnyVal {
    def toScala: FiniteDuration = FiniteDuration(javaDuration.toMillis, TimeUnit.MILLISECONDS)
  }
}
