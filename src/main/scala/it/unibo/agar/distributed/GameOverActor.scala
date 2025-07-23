package it.unibo.agar.distributed

import akka.actor.typed.receptionist.Receptionist
import Receptionist.{Listing, Subscribe}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.agar.controller.Main.massToWin
import it.unibo.agar.distributed.GameCoordinator.WorldServiceKey
import it.unibo.agar.distributed.GameProtocol.*
import it.unibo.agar.distributed.GameProtocol.GameMessage.{ThereIsAWinner, WorldRequest}
import it.unibo.agar.distributed.GameProtocol.GameOverCommand.{AvailableManagers, RefreshTimer, WorldUpdate}

import scala.concurrent.duration.*

object GameOverActor:

  def apply(): Behavior[GameOverCommand] = Behaviors.setup: ctx =>
    val worldUpdater = ctx.messageAdapter[GetWorld](res => WorldUpdate(res.world))
    ctx.system.receptionist ! Subscribe(WorldServiceKey, ctx.messageAdapter[Listing]: listing =>
      AvailableManagers(listing serviceInstances WorldServiceKey))

    Behaviors.withTimers: timers =>
      timers startTimerAtFixedRate(RefreshTimer, 200.millis)
      handleMessages(None, worldUpdater)

  private def handleMessages(
                        managerOpt: Option[ActorRef[GameMessage]],
                        worldUpdater: ActorRef[GetWorld]
                      ): Behavior[GameOverCommand] =
    Behaviors.receive { (ctx, msg) => msg match
      case AvailableManagers(managers) =>
        handleMessages(managers.headOption orElse managerOpt, worldUpdater)

      case RefreshTimer =>
        managerOpt foreach (_ ! WorldRequest(worldUpdater))
        Behaviors.same

      case WorldUpdate(world) => world.players find(_.mass >= massToWin) match
        case Some(winner) =>
          ctx.log info s"[GameOverActor] Winner is ${winner.id}"
          managerOpt foreach (_ ! ThereIsAWinner(winner.id))
          Behaviors.stopped
        case None =>
          Behaviors.same
    }