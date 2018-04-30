package com.proxy.service

import cats.effect.IO
import fs2.StreamApp
import io.circe._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.blaze.BlazeBuilder
import scala.concurrent.ExecutionContext.Implicits.global

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
import org.http4s.client.blaze.BlazeClientConfig
import java.io.InputStream
import org.http4s.EntityDecoder._
import org.http4s.client.Client
import org.http4s.client.{Client, DisposableResponse}

case class ProxyClient(clientConf: com.conf.Client) {
  private def getBlazeClientConfig(insecure: Boolean) : BlazeClientConfig = {
    if (insecure)
      BlazeClientConfig.insecure
    else
      BlazeClientConfig.defaultConfig
  }

  def getClient : Client[IO] = client

  private val clientConfig: BlazeClientConfig = getBlazeClientConfig(clientConf.https_insecure).copy(
     maxTotalConnections = clientConf.maxTotalConnections,
     idleTimeout = clientConf.idleTimeout,
     requestTimeout = clientConf.requestTimeout)

  // Http1Client[IO]() => IO[Client[IO]] : unsafeRunAsync return Client[IO]
  private val client: Client[IO] = Http1Client[IO](clientConfig).unsafeRunSync()
}

class ProxyService(URL_PROXY : String = "/proxy", 
  iptable: com.proxy.Iptable, 
  clientConf: com.conf.Client) extends Http4sDsl[IO] {

  val client: Client[IO] = FollowRedirect(
    if (clientConf.http_redirect) 10 else 0
  )(ProxyClient(clientConf).getClient)

  // TODO: check if request is valid
  private def check(req: Request[IO]): Boolean = {
    true
  }

  private def extractPath(url: String) = {
    val p = (URL_PROXY + "(.*)").r
    val p(url2) = url
    url2
  }

  private def filterHeaders(headers: Headers) : Headers = {
          
    headers.filterNot{h =>
      h.name == CaseInsensitiveString("Connection") ||
      h.name == CaseInsensitiveString("Keep-Alive") ||
      h.name == CaseInsensitiveString("Proxy-Authenticate") ||
      h.name == CaseInsensitiveString("Proxy-Authorization") ||
      h.name == CaseInsensitiveString("TE") ||
      h.name == CaseInsensitiveString("Trailer") ||
      h.name == CaseInsensitiveString("Transfer-Encoding") ||
      h.name == CaseInsensitiveString("Upgrade")
    }
  }

  private def getUserHeaders(oUser: Option[com.conf.User]): Seq[Header] = {

    oUser match {
      case Some(user) => 
        if (user.headers.isEmpty)
          Nil
        else
          user.headers.map { case (k,v) => Header(k, v) }.toSeq
      case None => Nil
    }
  }

  private def getHostHeader(host: String) : Seq[Header] = {
    Seq(Header("host", host))
  }

  def serviceClient: HttpService[IO] = client.toHttpService
  def serviceAgg = ChunkAggregator(service)
  def serviceGzip = GZip[IO](service)

  def service = HttpService[IO] {
    case req =>
      val env = iptable.getEnv(req.remoteAddr)
      val server: com.conf.Server = env.server match {
        case(Some(server)) => server
        case None =>
        throw new Exception("Destination not configured for current environment. See server {}")
      }
      val user: Option[com.conf.User] = iptable.getUser(req.remoteAddr)
      val newHeaders = filterHeaders(req.headers)
        .put(
          (getUserHeaders(user) ++ 
           getHostHeader(server.destination) ++
           env.headers.map { case (k, v) => Header(k, v) }.toSeq) : _*)

      if(check(req)) {
        val newAuthority = Authority(host = RegName(server.destination), port = Some(server.port))
        val proxiedReq =
        req.withUri(req.uri.copy(
          authority = Some(newAuthority), 
          path = extractPath(req.uri.path),
          scheme = Some(server.protocol match {
            case "https" => Scheme.https
            case "http" => Scheme.http
            case protocol: String => throw new Exception(s"Protocol $protocol is not supported.")
          }))
        )
          .withHeaders(newHeaders)
        // see org.http4s.client => toHttpService for example
        val response: IO[Response[IO]] = client.open
          .map {
            case DisposableResponse(response, dispose) =>
              response.copy(body = response.body.onFinalize(dispose))
          }.apply(proxiedReq)
        response
      } else {
        Forbidden("Url forbidden.")
      }
  }
}
