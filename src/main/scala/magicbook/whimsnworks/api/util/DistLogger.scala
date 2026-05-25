package magicbook.whimsnworks.api.util

import net.neoforged.fml.loading.FMLEnvironment
import org.slf4j.{Logger, LoggerFactory}

class DistLogger(name: String) extends Logger {
  private val serverLogger: Logger = LoggerFactory.getLogger(s"$name | Server")
  private val clientLogger: Logger = LoggerFactory.getLogger(s"$name | Client")

  private val active: Logger = if (FMLEnvironment.dist.isClient) clientLogger else serverLogger
  
  export active.*
}

object DistLogger {
  def apply(name: String): DistLogger = new DistLogger(name)
}