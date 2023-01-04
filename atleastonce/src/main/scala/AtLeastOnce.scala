import akka.actor.{ Actor, ActorSelection }
import akka.persistence.{ AtLeastOnceDelivery, PersistentActor }

sealed trait Cmd
// 定义 SendActor 和 ReceiveActor 之间交换的消息.
case class SayHello(deliveryId: Long, s: String) extends Cmd
case class ReceiveHello(deliveryId: Long) extends Cmd

sealed trait Evt
// 定义要跟踪状态的本地持久化对象.
case class HelloSaid(s: String) extends Evt
case class HelloReceived(deliveryId: Long) extends Evt

// 目标 Actor 是 ActorSelection 而非 ActorRef, 这样就可以进行持久化了.
class SendActor(destination: ActorSelection)
  // 发送 Actor 必须是 PersistentActor 并且还要扩展 AtLeastOnceDelivery.
  extends PersistentActor with AtLeastOnceDelivery {

    // 用来表示持久化层中的条目的唯一键的名称.
    override def persistenceId: String = "persistence-id"

    override def receiveCommand: Receive = {
      case s: String =>
        // 为了发送字符串, 需要创建持久化事件, 并且更新 Actor 的状态.
        persist(HelloSaid(s))(updateState)
      
      case ReceiveHello(deliveryId) =>
        // 为了处理确认响应, 需要创建持久化事件, 并且更新 Actor 的状态.
        persist(HelloReceived(deliveryId))(updateState)
    }

    // 用于在 Actor 恢复时重放事件.
    override def receiveRecover: Receive = {
      case evt: Evt => updateState(evt)
    }

    def updateState(evt: Evt): Unit = evt match {
      case HelloSaid(s) => 
        // 用于告知最多递送一次机制: 将消息递送到目的地.
        // 由递送函数使用以便将递送 ID 转换成一条消息.
        deliver(destination)(deliveryId => SayHello(deliveryId, s))

      case HelloReceived(deliveryId) =>
        // 用于告知最多递送一次机制: 确认消息已经被接收.
        confirmDelivery(deliveryId)
    }
  }


// ReceiverActor 使用一条包含 deliveryID 的 ReceiveHello 消息来确认每一条 SayHello 消息. 
class ReceiveActor extends Actor {
  def receive = {
    case SayHello(deliveryId, s) =>
      // do something with s
      sender() ! ReceiveHello(deliveryId)
  }
}
