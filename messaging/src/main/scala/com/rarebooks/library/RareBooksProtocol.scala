package com.rarebooks.library

// 用于时间戳.
import scala.compat.Platform

// 协议对象会确立所有 Actor 的基本消息.
object RareBooksProtocol {

  sealed trait Topic

  // 下面是枚举库中的 topic.
  case object Africa     extends Topic
  case object Asia       extends Topic
  case object Gilgamesh  extends Topic
  case object Greece     extends Topic
  case object Persia     extends Topic
  case object Philosophy extends Topic
  case object Royalty    extends Topic
  case object Tradition  extends Topic
  case object Unknown extends Topic

  val viableTopics: List[Topic] =
    List(Africa, Asia, Gilgamesh, Greece, Persia, Philosophy, Royalty, Tradition)

  sealed trait Card {
    def title: String
    def description: String
    def topic: Set[Topic]
  }

  // 书籍中的卡片目录.
  final case class BookCard(
      isbn: String,
      author: String,
      title: String,
      description: String,
      dateOfOrigin: String,
      topic: Set[Topic],
      publisher: String,
      language: String,
      pages: Int)
    extends Card

  trait Msg {
    // 所有的消息都有一个时间戳.
    def dateInMillis: Long
  }

  // 下面每个 case class 中的 require, 确保消息包含搜索条件.
  final case class BookFound(books: List[BookCard], dateInMillis: Long = Platform.currentTime) extends Msg {
    require(books.nonEmpty, "Book(s) required.")
  }

  final case class BookNotFound(reason: String, dateInMillis: Long = Platform.currentTime) extends Msg {
    require(reason.nonEmpty, "Reason is required.")
  }

  final case class Complain(dateInMillis: Long = Platform.currentTime) extends Msg

  final case class Credit(dateInMillis: Long = Platform.currentTime) extends Msg

  final case class FindBookByAuthor(author: String, dateInMillis: Long = Platform.currentTime) extends Msg {
    require(author.nonEmpty, "Author required.")
  }

  final case class FindBookByIsbn(isbn: String, dateInMillis: Long = Platform.currentTime) extends Msg {
    require(isbn.nonEmpty, "Isbn required.")
  }

  final case class FindBookByTopic(topic: Set[Topic], dateInMillis: Long = Platform.currentTime) extends Msg {
    require(topic.nonEmpty, "Topic required.")
  }

  final case class FindBookByTitle(title: String, dateInMillis: Long = Platform.currentTime) extends Msg {
    require(title.nonEmpty, "Title required.")
  }

  final case class GetCustomer(dateInMillis: Long = Platform.currentTime) extends Msg
}
