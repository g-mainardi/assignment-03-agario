package it.unibo.agar.distributed

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import it.unibo.agar.controller.Main.massToWin
import it.unibo.agar.distributed.GameProtocol.*
import GameMessage.{ThereIsAWinner, WorldRequest}
import ListenerMessages.*
import it.unibo.agar.distributed.GameCoordinator.askManager

import scala.concurrent.duration.*

object GameOverActor:

  private val refreshingInterval: FiniteDuration = 200.millis

  def apply(): Behavior[GameOverMessage] = Behaviors.setup: ctx =>
    askManager(ctx)

    Behaviors.withTimers: timers =>
      timers startTimerAtFixedRate(RefreshTimer, refreshingInterval)
      handleMessages(None)

  private def handleMessages(
                        managerOpt: Option[ActorRef[GameMessage]],
                      ): Behavior[GameOverMessage] =
    Behaviors.receive: (ctx, msg) =>
      msg match
        case AvailableManagers(managers) =>
          handleMessages(managers.headOption orElse managerOpt)

        case RefreshTimer =>
          managerOpt foreach (_ ! WorldRequest(ctx.self))
          Behaviors.same

        case WorldUpdate(world) => world.players find(_.mass >= massToWin) match
          case Some(winner) =>
            ctx.log info s"[GameOverActor] Winner is ${winner.id}"
            managerOpt foreach (_ ! ThereIsAWinner(winner.id))
            Behaviors.stopped
          case None =>
            Behaviors.same