package com.rarebooks.library

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.NotUsed

import scala.concurrent.Await
import scala.concurrent.duration.Duration

// 包含解析j将会用到的 utf8String 转换
import LibraryProtocol.BookCard

object Cataloging extends App {
  implicit val system = ActorSystem("catalog-loader")
  implicit val materializer = ActorMaterializer()

  val file = Paths.get("books.csv")

  private val framing: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(ByteString("\n"),
      maximumFrameLength = 256,
      allowTruncation = true)

  private val parsing: ByteString => Array[String] =
    _.utf8String.split(",")

  import LibraryProtocol._
  val topics = Set(Africa, Asia, Gilgamesh, Greece, Persia, Philosophy, Royalty, Tradition)
  val topic: String => Set[Topic] = s => Set(topics.find(s == _.toString).getOrElse(Unknown))
  private val conversion: Array[String] => BookCard =
    s => BookCard(
      isbn = s(0),
      author = s(1),
      title = s(2),
      description = s(3),
      dateOfOrigin = s(4),
      topic = topic(s(5)),
      publisher = s(6),
      language = s(7),
      pages = s(8).toInt
    )

  // 转换成 BookCard
  val result = FileIO.fromPath(file)
      .via(framing)  // 按行对 ByteString 进行帧式分段
      .map(parsing)  // 将每一行解析成 Array[String]
      .map(conversion)
      .to(Sink.foreach(println(_)))
      .run()

  Await.ready(result, Duration.Inf)
  system.terminate()
}
