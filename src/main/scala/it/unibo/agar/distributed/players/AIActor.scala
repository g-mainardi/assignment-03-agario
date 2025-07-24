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
  private var lastWorld: Option[World] = None

  override def say(msg: String)(using id: PlayerId): String = "AI" + super.say(msg)(using id)

  def apply(id: String): Behavior[AIPlayerMessage] = Behaviors.setup: ctx =>
    given PlayerId = id
    var managerOpt: Option[ActorRef[GameMessage]] = None
    var playing = false
    askManager(ctx)
    Behaviors.withTimers: timers =>
      timers startTimerAtFixedRate (Tick, interval)
      Behaviors.receiveMessage:
        case Start =>
          ctx.log info say (startMsg)
          playing = true
          Behaviors.same

        case WorldUpdate(world) =>
          if playing && !(world isPresent id) then
            ctx.log info say (eatenMsg)
            playing = false
            timers cancel Tick
            Behaviors.stopped
          else
            lastWorld = Some(world)
            Behaviors.same

        case End(winner) =>
          ctx.log info say (overMsg + winner)
          playing = false
          Behaviors.stopped

        case AvailableManagers(manager +: _) =>
          if managerOpt.isEmpty then
            ctx.log info say (newManagerMsg)
            managerOpt = Some(manager)
            manager ! PlayerJoined(id, ctx.self, ai = true)
          else
            ctx.log info say (alreadyRegisteredMsg)
          Behaviors.same

        case AvailableManagers(_) =>
          ctx.log info say (noManagersMsg)
          Behaviors.same

        case Tick =>
          for
            gm <- managerOpt
            world <- lastWorld
            move <- AIMovement.getAIMove(id, world)
          do
            gm ! PlayerMove(id, move)
          Behaviors.same