package com.proxy

import com.conf.{Env, User}

/*
 * TODO: Either[] 
 */
class Iptable(envs: Map[String, Env]) {
  // ip -> environment name
  private val ipEntries = scala.collection.mutable.Map[String, String]("*" -> "default")
  private val userEntries = scala.collection.mutable.Map[String, String]()

  def checkIp(oip: Option[String]) {
    oip match {
      case Some(ip) =>
        if (ip.length > 0)
          return
      case None =>
    }
    throw new Exception("Proxy cannot detect your source ip address.")
  }

  def checkUser(env: Env, username: String) {
    if (!env.users.contains(username))
      throw new Exception(s"No user $username defined in current environment")
  }

  def getEnv(ip: Option[String]) : Env = {
    checkIp(ip)
    val envName = ipEntries.getOrElse(ip.get, "default")
    try {
      if (ipEntries.keySet.contains(ip.get))
        envs(ipEntries(ip.get))
      else
        envs(ipEntries("*"))
    } catch {
      case e: Exception => throw new Exception(s"$envName environment undefined.")
    }
  }

  def showEnv(ip: Option[String]) : String = {
    checkIp(ip)
    val envName = ipEntries.getOrElse(ip.get, "default")
    s"env is $envName for ip adress ${ip.get}"
  }

  def setEnv(ip: Option[String], envName: String) {
    checkIp(ip)
    ipEntries.put(ip.get, envName)
  }

  def setUser(ip: Option[String], username: String) {
    checkIp(ip)
    checkUser(getEnv(ip), username)
    userEntries.put(ip.get, username)
  }

  def getUser(ip: Option[String]) : Option[User] = {
    val env = getEnv(ip)
    userEntries.get(ip.get) match {
      case Some(username) => env.users.find(u => u.name == username)
      case None => None
    }
  }

  override def toString = ipEntries.map { case (ip, envName) => s"$ip : $envName" }
    .mkString(util.Properties.lineSeparator)
}
