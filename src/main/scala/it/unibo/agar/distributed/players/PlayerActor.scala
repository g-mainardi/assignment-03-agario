package it.unibo.agar.distributed.players

import akka.actor.typed.Behavior
import it.unibo.agar.distributed.GameProtocol.*

trait PlayerActor[T <: PlayerMessage]:
  export it.unibo.agar.model.PlayerId
  protected val startMsg: String = "game started"
  protected val overMsg: String = "game over, winner is "
  protected val newManagerMsg: String = "Game Manager found, I join"
  protected val alreadyRegisteredMsg: String = "already registered"
  protected val noManagersMsg: String = "No managers available"
  protected val eatenMsg = "was eaten"
  
  def apply(id: PlayerId): Behavior[T]

  protected def say(msg: String)(using id: PlayerId) = s"Player $id: $msg"
