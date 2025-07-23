package it.unibo.agar.distributed

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.distributed.GameCoordinator.askManager
import it.unibo.agar.distributed.GameProtocol.*
import GameMessage.WorldRequest
import ListenerMessages.*
import it.unibo.agar.view.GlobalView

import scala.concurrent.duration.DurationInt
object GlobalViewActor:
  private val refreshingInterval = 200.millis

  def apply(globalView: GlobalView): Behavior[GlobalViewMessage] = Behaviors.setup: ctx =>
    askManager(ctx)
    Behaviors.withTimers: timers =>
      timers startTimerAtFixedRate (RefreshTimer, refreshingInterval)
      handleMessages(globalView, None)

  private def handleMessages(
                              view: GlobalView,
                              managerOpt: Option[ActorRef[GameMessage]],
                            ): Behavior[GlobalViewMessage] =
    Behaviors.receive: (ctx, msg) =>
      msg match
        case AvailableManagers(managers) =>
          val newManager = managers.headOption orElse managerOpt
          handleMessages(view, newManager)
  
        case RefreshTimer =>
          managerOpt foreach (_ ! WorldRequest(ctx.self))
          Behaviors.same
  
        case WorldUpdate(world) =>
          view update world
          Behaviors.same
