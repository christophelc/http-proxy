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

class ApiService(iptable: com.proxy.Iptable) extends Http4sDsl[IO] {

  def service = HttpService[IO] {

    // return all defined environment
    case GET -> Root / "envs" =>
      Ok(com.conf.Conf.envs.toString)

    // current environment
    case req @ GET -> Root / "env" =>
      Ok(iptable.showEnv(req.remoteAddr))

    case req @ PUT -> Root / "env" / env =>
      iptable.setEnv(req.remoteAddr, env)
      Ok(s"env is now $env")

    // list of users for the current environment
    case req @ GET -> Root / "env" / "users" =>
      Ok(iptable.getEnv(req.remoteAddr).users.map(_.name).mkString(scala.util.Properties.lineSeparator))

    case req @ GET -> Root / "env" / "user" =>
      Ok(iptable.getUser(req.remoteAddr).toString)

    case req @ PUT -> Root / "user" / user =>
      iptable.setUser(req.remoteAddr, user)
      Ok(s"user is now $user for the current environment")

    // show the iptable entries
    case GET -> Root / "iptable" =>
      Ok(iptable.toString)

  }
}
