package it.unibo.agar.distributed.players

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.Utils.+:
import it.unibo.agar.distributed.GameProtocol.*
import StandardPlayerMessage.*
import AIPlayerMessages.*
import GameMessage.{PlayerJoined, PlayerMove}
import it.unibo.agar.distributed.GameCoordinator.askManager
import it.unibo.agar.model.{AIMovement, World}

object AIActor extends PlayerActor[AIPlayerMessage]:
  import scala.concurrent.duration.*
  private val interval: FiniteDuration = 100.millis
  private var managerOpt: Option[ActorRef[GameMessage]] = None
  private var lastWorld: Option[World] = None

  override def say(msg: String)(using id: PlayerId): String = "AI" + super.say(msg)(using id)

  def apply(id: String): Behavior[AIPlayerMessage] = Behaviors.withTimers: timers =>
    given PlayerId = id
    timers startTimerAtFixedRate (Tick, interval)
    startReceiving

  private def startReceiving(using id: PlayerId): Behavior[AIPlayerMessage] =
    Behaviors.receive: (ctx, msg) =>
      askManager(ctx)
      msg match
        case Start =>
          ctx.log info say (startMsg)
          Behaviors.same

        case WorldUpdate(world) =>
          lastWorld = Some(world)
          Behaviors.same

        case End(winner) =>
          ctx.log info say (overMsg + winner)
          Behaviors.stopped

        case AvailableManagers(manager +: _) if managerOpt.isEmpty =>
          ctx.log info say (newManagerMsg)
          managerOpt = Some(manager)
          manager ! PlayerJoined(id, ctx.self, ai = true)
          Behaviors.same

        case AvailableManagers(_) =>
          Behaviors.same // già registrato o nessun GameManager

        case Tick =>
          for
            gm <- managerOpt
            world <- lastWorld
            move <- AIMovement.getAIMove(id, world)
          do
            gm ! PlayerMove(id, move)
          Behaviors.same