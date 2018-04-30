package com.proxy

import com.proxy.service._

import cats.effect.IO
import cats.implicits._
import fs2.StreamApp
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import cats._
import cats.data.Kleisli

// client
import org.http4s._
import org.http4s.client.blaze.Http1Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io._

import org.http4s.Uri._
import org.http4s.client.middleware.FollowRedirect
import org.http4s.util._
import org.http4s.server.middleware._
import org.http4s.server.Router
import org.http4s.client.blaze.BlazeClientConfig
import java.io.InputStream
import org.http4s.EntityDecoder._
import org.http4s.client.Client
import org.http4s.client.{Client, DisposableResponse}
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
//import scala.concurrent.ExecutionContext.Implicits.global

object ProxyServer extends StreamApp[IO] with Http4sDsl[IO] {

  val URL_PROXY = "/proxy"
  val URL_ADMIN = "/admin"
  val URL_API = "/api"
  val iptable = new Iptable(com.conf.Conf.envs)
  val proxyConf = com.conf.Conf.proxy
  val clientConf = com.conf.Conf.client

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(proxyConf.threads))

  def serviceRouter: HttpService[IO] =
    Router[IO](
      "" -> rootService,
      URL_PROXY -> new ProxyService(URL_PROXY, iptable, clientConf).service,
      URL_ADMIN -> new AdminService().service,
      URL_API -> new ApiService(iptable).service
     )

  def rootService = HttpService[IO] {
      case _ -> Root => Ok("route")
  }  

  def stream(args: List[String], requestShutdown: IO[Unit]) =
    BlazeBuilder[IO]
      .bindHttp(proxyConf.port, "0.0.0.0")
      .mountService(serviceRouter, "/")
      .serve
}
// console sbt
//  println(com.conf.Conf)
//  println(com.conf.Config.envs)
