package it.unibo.agar.distributed

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.view.GlobalView
import it.unibo.agar.distributed.GameProtocol.{GameMessage, GetWorld, GlobalViewCommand}
import GameMessage.WorldRequest
import GlobalViewCommand.*
import akka.actor.typed.receptionist.Receptionist
import Receptionist.{Listing, Subscribe}
import it.unibo.agar.distributed.GameCoordinator.WorldServiceKey

import scala.concurrent.duration.DurationInt
object GlobalViewActor:
  private val refreshingInterval = 200.millis

  //  todo non fa => da errore e la global view non si aggiorna
//  def apply(globalView: GlobalView): Behavior[GlobalViewCommand] = setup[GlobalViewCommand]:
//    (m, wra) => handleMessages(globalView, m, wra)

  def apply(globalView: GlobalView): Behavior[GlobalViewCommand] = Behaviors.setup { ctx =>
    val worldUpdater = ctx.messageAdapter[GetWorld](res => WorldUpdate(res.world))
    ctx.system.receptionist ! Subscribe(WorldServiceKey, ctx.messageAdapter[Listing]: listing =>
      AvailableManagers(listing serviceInstances WorldServiceKey))

    Behaviors.withTimers: timers =>
      timers startTimerAtFixedRate (RefreshTimer, refreshingInterval)
      handleMessages(globalView, None, worldUpdater)
  }

  private def handleMessages(
                              view: GlobalView,
                              managerOpt: Option[ActorRef[GameMessage]],
                              worldUpdater: ActorRef[GetWorld]
                            ): Behavior[GlobalViewCommand] =
    Behaviors.receiveMessage:
      case AvailableManagers(managers) =>
        val newManager = managers.headOption orElse managerOpt
        handleMessages(view, newManager, worldUpdater)

      case RefreshTimer =>
        managerOpt foreach (_ ! WorldRequest(worldUpdater))
        Behaviors.same

      case WorldUpdate(world) =>
        view update world
        Behaviors.same
