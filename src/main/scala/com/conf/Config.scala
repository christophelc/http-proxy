
package com.conf

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration.FiniteDuration

object Conf {
  val client = ConfigFactory.load()
  println(client)
}

/*
trait ConfigSupport {
  val config: Config.type = Config
}

object Config extends ConfigurationSupport {

  import model._

  lazy val server: Server = pureconfig.loadConfigOrThrow[Server](config.getConfig("server"))
  lazy val client: Client = pureconfig.loadConfigOrThrow[Client](config.getConfig("client"))
  lazy val proxy: Proxy = pureconfig.loadConfigOrThrow[Proxy](config.getConfig("proxy"))


  object model {
    case class Server(port: Int,
                      host: String,
                      threads: Int)

    case class Client(maxTotalConnections: Int,
                      idleTimeout: FiniteDuration,
                      requestTimeout: FiniteDuration)

    case class Proxy(destination: String,
                     port: Int)
  }
}

*/
