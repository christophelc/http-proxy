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
import org.http4s.util._
import java.io.InputStream
import org.http4s.EntityDecoder._

class AdminService extends Http4sDsl[IO] {

  def service = HttpService[IO] {

    case GET -> Root / "test" =>
      Ok("admin ok")
  }
}
