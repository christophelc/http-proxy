package com.proxy

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

object ProxyServer extends StreamApp[IO] with Http4sDsl[IO] {

  private def check(req: Request[IO]): Boolean = {
    true
  }

  /*
   * http://camel.465427.n5.nabble.com/How-to-follow-redirect-url-from-server-with-http4-in-camel-td5731967.html
   *
   * Redirect:
   * https://github.com/http4s/http4s/blob/master/client/src/main/scala/org/http4s/client/middleware/FollowRedirect.scala
   * pending until fixed:
   * https://github.com/http4s/http4s/blob/master/client/src/test/scala/org/http4s/client/middleware/FollowRedirectSpec.scala
   */
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

  // def proxyHtml proxy/html/
  // def proxyJson proxy/json/
  // def proxyBin  proxy/bin/
  // def proxyAdmin /admin/
  // profile:
  // GET /admin/profile/
  // POST /admin/profile
  // script:
  //  POST /admin/script?profile=
  //  GET /admin/script?profile=
  //  query:
  // POST /query/json/ => 
  // util:
  // POST /util/json/projection => met tous les champs en liste json simple
  // POST /util/json/invprojection => met tous les champs indentés
  // POST /util/json/count => j.projection.count
  // POST /util/json/diff => (projection (j1).sort, projection(j2).sort).diff.invprojection
  // POST /util/json/transform 
  //
  // Save:
  // PUT /record/start/tablename  <url template proxy/...>
  // GET proxy.../...
  // PUT /record/stop/tablename   <url template proxy/...>
  //  Log profile1 : get dossiers avec pagination => sauver dans bdd locale
  //
  //  Script:
  //  def getAll(profile) {
  //    start = 0
  //    size = 50
  //    count = url("/api/dossiers)[ "count" ]
  //    while (get(url("/api/dossiers).params("start=$start", "end=$end").size >0) {
  //     start = start + size
  //    })
  //  }
  //  def store() {
  //   env("local")
  //   profile("assureur_super")
  //   record.start(name="recuperer les dossiers", table= "dossiers", url = "/api/dossiers/*") 
  //   getAll(profile)
  //   record.stop(name="dossiers")
  //  }
  //  def ajouterPj(dossier, files) {
  //   pj =  url("/api/dossiers/$dossier/piecejointe)
  //   if (pj) {
  //   // multipart: increment
  //    url("/..../..", method= post, type = multipart(files)
  //   }
  //  }
  //
  //  aJson = db.get("no dossier") 
  //  aJson = db.get("no dossier").filter("a.b.c", "ouvert")
  //  max = aJson["refDossier"].max
  //  nDossier = max.inc(6, 10) // incremente le dossier en considerant la sous chaine
  //  ouvrirDossier(nDossier)
  //  pj = ajouterPj(nDossier, files(prefix="file", filenames=["/home/..../a.pdf", ":.../b.pdf"]))
  //  completerDossier(nDossier)

  // Http1Client[IO]() => IO[Client[IO]]; ici attendu: Client[IO]
  val client : Client[IO] = Http1Client[IO](BlazeClientConfig.insecure).unsafeRunSync()
  def serviceClient: HttpService[IO] = Http1Client[IO](BlazeClientConfig.insecure).unsafeRunSync().toHttpService
        
  def serviceProxy = HttpService[IO] {

    case req =>
      //val hostName = "www.google.com"
      val hostName = "localhost"
    //if(check(req)) {
    if (true) {
      val newHeaders = filterHeaders(req.headers).put(Header("host", hostName))
      val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjNzMwMzRlOC0xZDRkLTQ4NWUtOTc4Mi05Mjc0NGZjYTlmOGUiLCJuYmYiOjE1MjM0ODQwMDAsImlzcyI6ImVjcmFjIn0.XBCNNSA72jRE8BxIYQcDecvi4Ku3zr8txjwhNYJ7cME"
      println(newHeaders)
      println("**")
      // Why do we need to take care of redirection since it is the client responsability ?
      // But why do we need curl -L with proxy and why is there a redirection message only with proxy ?
      //client <- Http1Client[IO]().map(cl => FollowRedirect(100000)(cl))
      //client <- Http1Client[IO]()
      //newAuthority = Authority(host = RegName(hostName), port = Some(80))
      val newAuthority = Authority(host = RegName(hostName), port = Some(9000))
      val proxiedReq =
        req.withUri(req.uri.copy(authority = Some(newAuthority)))
          .withHeaders(req.headers
          .put(Header("host", hostName))
          .put(Header("Authorization", "Bearer " + token))
          .put(Header("X-Api-Version", "123"))
          .put(Header("X-Canal", "456"))
          )
       //response <- client.expect[Segment[Byte, Unit]](proxiedReq)
       //} yield {  println(proxiedReq); println("**");println(response); response //  io.toInputStream(client.streaming(request)(_.body)) } 
       //servClient(req)
       //cl.map(c => (for { resp <- (c.toHttpService)(req) } yield { resp} ))
       //val response: IO[Response[IO]] = serviceClient.map(c => (req))
       //response
    /*
    val newUri = Uri(scheme = Some(Scheme.http),
      authority = Some(Uri.Authority(host = Uri.IPv4(config.proxy.destination), port = Some(config.proxy.port))),
      path = request.uri.path,
      query = request.uri.query,
      fragment = request.uri.fragment)

    val newRequest = request.withUri(newUri)
*/

     // see org.http4s.client => toHttpService for example
     val response: IO[Response[IO]] = client.open
      .map {
        case DisposableResponse(response, dispose) =>
          response.copy(body = response.body.onFinalize(dispose))
      }.apply(proxiedReq)

    response
 

    } else {
      Forbidden("Some forbidden message....")
    }
  }

  def serviceAgg = ChunkAggregator(serviceProxy)

  def service1 = GZip[IO](serviceProxy)
  def stream(args: List[String], requestShutdown: IO[Unit]) =
    BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(service1, "/")
      //.mountService(serviceClient, "/")
      .serve
}
  //println(com.conf.Conf)
