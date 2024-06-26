package com.rarebooks.library

import akka.actor.{ActorSelection, ActorSystem, Address, RootActorPath}
import akka.event.Logging

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration, MILLISECONDS => Millis}
import scala.io.StdIn

object CustomerApp {

  /**
    * Main loop for running program.
    *
    * @param args input arguments
    */
  def main(args: Array[String]): Unit = {
    val system: ActorSystem = ActorSystem("customer-system")
    val customerApp: CustomerApp = new CustomerApp(system)
    customerApp.run()
  }
}

/**
  * Customer bootstrap application.
  *
  * @param system Actor system
  */
class CustomerApp(system: ActorSystem) extends Console {

  import system.dispatcher

  private val log = Logging(system, getClass.getName)

  private val resolveTimeout: FiniteDuration =
    Duration(system.settings.config.getDuration("rare-books.resolve-timeout", Millis), Millis)

  // 获取远程 Actor 系统的主机名
  private val hostname: String = system.settings.config.getString("rare-books.hostname")
  private val port: Int = system.settings.config.getInt("rare-books.port")
  val rareBooksAddress: Address = Address("akka.tcp", "rare-books-system", hostname, port)

  def run(): Unit = {
    log.warning(f"{} running%nEnter commands [`q` = quit, `2c` = 2 customers, etc.]:", getClass.getSimpleName)
    commandLoop()
    Await.ready(system.whenTerminated, Duration.Inf)
  }

  @tailrec
  private def commandLoop(): Unit =
    Command(StdIn.readLine()) match {
      case Command.Customer(count, odds, tolerance) =>
        createCustomer(count, odds, tolerance)
        commandLoop()
      case Command.Quit =>
        system.terminate()
      case Command.Unknown(command) =>
        log.warning(s"Unknown command $command")
        commandLoop()
    }

  /**
    * Create customer factory method.
    *
    * @param count number of customers
    * @param odds chances customer will select a valid topic
    * @param tolerance maximum number of books not found before customer complains
    */
  protected def createCustomer(count: Int, odds: Int, tolerance: Int): Unit = {
    // 使用 Actor 系统执行选择
    val selection: ActorSelection  =
        system.actorSelection(
        RootActorPath(rareBooksAddress) /
        "user" / "rare-books")

    // 基于 rare-books 的主机名和端口创建的 Akka 地址.
    selection.resolveOne(resolveTimeout).onComplete {
      // 在成功完成时, 将返回 rareBooks ActorRef.
      case scala.util.Success(rareBooks) =>
        for (_ <- 1 to count)
          // Actor 在解析完毕后, 将被用来创建顾客.
          system.actorOf(Customer.props(rareBooks, odds, tolerance))
      case scala.util.Failure(ex) =>
        log.error(ex, ex.getMessage)
    }
  }
}
