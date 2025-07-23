package it.unibo.agar.distributed.players

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.Receptionist.{Listing, Subscribe}
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.agar.distributed.GameProtocol.*

trait PlayerActor[T <: PlayerMessage]:
  import it.unibo.agar.distributed.GameCoordinator.WorldServiceKey
  protected val startMsg: String = "game started"
  protected val overMsg: String = "game over, winner is "
  protected val newManagerMsg: String = "Game Manager found, I join"
  
  def apply(id: PlayerId): Behavior[T]

  protected def say(msg: String)(using id: PlayerId) = s"Player $id: $msg"

  protected def register(ctx: ActorContext[T]): Unit =
    ctx.system.receptionist ! Subscribe(
      WorldServiceKey,
      ctx.messageAdapter[Listing] : listing =>
        AvailableManagers(listing serviceInstances WorldServiceKey).asInstanceOf[T]
    )
