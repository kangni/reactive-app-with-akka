package com.rarebooks.library

import akka.actor.{ Actor, ActorRef, ActorLogging, Props, Stash }
import scala.concurrent.duration.FiniteDuration

object Librarian {

  import Catalog._
  import RareBooksProtocol._

  final case class Done(e: Either[BookNotFound, BookFound], customer: ActorRef)

  def props(findBookDuration: FiniteDuration): Props =
    Props(new Librarian(findBookDuration))

  private def optToEither[T](v: T, f: T => Option[List[BookCard]]): 
    Either[BookNotFound, BookFound] =
    f(v) match {
      // 找到返回 Right, 失败返回 Left.
      // 由于 Option 具有子类型 None 和 Some, 因此 Either 也具有子类型 Left 和 Right.
      // 此处的规约是, 使用 Right 表示默认值, 使用 Left 表示失败, 因此 None 通常被映射为 Left.
      case b: Some[List[BookCard]] => Right(BookFound(b.get))
      case _                       => Left(BookNotFound(s"Book(s) not found based on $v"))
    }

  private def findByIsbn(fb: FindBookByIsbn) =
    optToEither[String](fb.isbn, findBookByIsbn)

  private def findByAuthor(fb: FindBookByAuthor) =
    optToEither[String](fb.author, findBookByAuthor)

  private def findByTitle(fb: FindBookByTitle) =
    optToEither[String](fb.title, findBookByTitle)

  private def findByTopic(fb: FindBookByTopic) =
    optToEither[Set[Topic]](fb.topic, findBookByTopic)
}

class Librarian(findBookDuration: FiniteDuration) extends Actor with ActorLogging with Stash {
  import context.dispatcher
  import Librarian._
  import RareBooksProtocol._

  override def receive: Receive = ready

  private def ready: Receive = {
    case m: Msg => m match {
      case c: Complain =>
        sender ! Credit()
        log.info(s"Credit issued to customer $sender()")
      case f: FindBookByIsbn =>
        research(Done(findByIsbn(f), sender()))
      case f: FindBookByAuthor =>
        research(Done(findByAuthor(f), sender()))
      case f: FindBookByTitle =>
        research(Done(findByTitle(f), sender()))
      case f: FindBookByTopic =>
        research(Done(findByTopic(f), sender()))
    }
  }

  private def busy: Receive = {
    case Done(e, s) =>
      process(e, s)
      unstashAll()
      context.unbecome()
    case _ =>
      stash()
  }

  private def research(d: Done): Unit = {
    context.system.scheduler.scheduleOnce(findBookDuration, self, d)
    context.become(busy)
  }

  private def process(r: Either[BookNotFound, BookFound], sender: ActorRef): Unit = {
    r fold (
      // 未找到.
      f => {
        sender ! f
        log.info(f.toString)
      },
      // 已找到.
      s => sender ! s)
  }
}
