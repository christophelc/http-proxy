package com.conf

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration.FiniteDuration
import com.github.andr83.scalaconfig._

case class Client(
  https_insecure: Boolean,
  http_redirect: Boolean,
  maxTotalConnections: Int,
  idleTimeout : FiniteDuration,
  requestTimeout: FiniteDuration)

case class Proxy(
  host: String,
  port: Int,
  threads: Int)

case class Server(
  protocol: String,
  destination: String,
  port: Int)

case class Env(
  inherits: Option[String] = None,
  var users: List[User] = Nil,
  headers: Map[String, String] = Map[String, String](),
  server: Option[Server] = None) 

case class User(
  id: String,
  name: String,
  headers: Map[String, String] = Map[String, String]()
)

object Conf {

  private val config = ConfigFactory.load()
  private val hEnv = getPredefEnv
  val envs = getEnvs
  val proxy = getProxy
  val client = getClient

  // list of environment names
  private def getEnvNames : List[String] = {
    config.as[List[String]]("env") match {
      case Left(errors) => throw new Exception(errors.toString)
      case Right(l) => l
    }
  }

  private def getProxy : Proxy = {
    val envReader: Reader.Result[Proxy] = config.as[Proxy]("proxy")
    envReader match {
      case Left(errors) => throw new Exception(errors.toString)
      case Right(proxy) => proxy
    }
  }

  private def getClient : Client = {
    val envReader: Reader.Result[Client] = config.as[Client]("client")
    envReader match {
      case Left(errors) => throw new Exception(errors.toString)
      case Right(client) => client
    }
  }

  private def getEnv(name: String) : Env = {
    val envReader: Reader.Result[Env] = config.as[Env](name)
    envReader match {
      case Left(errors) => throw new Exception(errors.toString)
      case Right(env) => env
    }
  }

  /*
   * Get all users of an environment
   */
  private def getUsers(refUser: String): List[User] = {
    config.as[List[User]](refUser) match {
      case Left(errors) => throw new Exception(errors.toString)
      case Right(l) => l
    }
  }

  /*
   * Set users and return environments
   */
  private def getPredefEnv : Map[String, Env] = {
      getEnvNames.map(
        name => name -> getEnv(name)
      ).toMap
  }

  private def findPathToRoot(env: Env) : List[String] = {
    env.inherits match {
      case Some(envName) => List(envName) ++ findPathToRoot(hEnv(envName))
      case None => Nil
    }
  }

  private def overrideProperties(env: Env, parent: Env) : Env = {
    env.copy(
      users = if (env.users.size == 0) parent.users else env.users,
      headers = if (env.headers.size == 0) parent.headers else env.headers,
      server = if (env.server.isEmpty) parent.server else env.server)
    }
  /*
   * Return environment with inherited properties
   */
  def getEnvs : Map[String, Env] = {
    hEnv.map {
      case (envName, env) => envName -> {
        (List(envName) ++ findPathToRoot(env)).reverse.foldLeft(new Env()){
          case (parentEnv, childEnv) => overrideProperties(hEnv(childEnv), parentEnv)
        }
      }
    }.toMap
  }
} 
