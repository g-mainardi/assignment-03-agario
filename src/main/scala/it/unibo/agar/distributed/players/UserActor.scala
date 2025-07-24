package it.unibo.agar.distributed.players

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.Utils.+:
import it.unibo.agar.distributed.GameProtocol.*
import GameMessage.{PlayerJoined, PlayerLeft, PlayerMove}
import StandardPlayerMessage.*
import it.unibo.agar.distributed.GameCoordinator.askManager
import it.unibo.agar.view.LocalView

object UserActor extends PlayerActor[UserPlayerMessage]:
  def apply(id: PlayerId): Behavior[UserPlayerMessage] = Behaviors.setup: ctx =>
    given PlayerId = id
    askManager(ctx)
    var managerOpt: Option[ActorRef[GameMessage]] = None
    var playing = false
    val view = new LocalView(
      id,
      movementAction = dir => if playing then managerOpt foreach{_ ! PlayerMove(id, dir)},
      onQuitting     = () => managerOpt foreach{_ ! PlayerLeft(id)}
    )
    Behaviors.receiveMessage:
      case Start =>
        ctx.log info say (startMsg)
        playing = true
        view.open()
        Behaviors.same

      case WorldUpdate(world) =>
        if playing && !(world isPresent id) then
          ctx.log info say (eatenMsg)
          view.showEaten()
          playing = false
        view updateWorld world
        Behaviors.same

      case End(winner) =>
        ctx.log info say (overMsg + winner)
        playing = false
        view showGameOver winner
        Behaviors.stopped

      case AvailableManagers(manager +: _) =>
        if managerOpt.isEmpty then
          ctx.log info say (newManagerMsg)
          managerOpt = Some(manager)
          manager ! PlayerJoined(id, ctx.self)
        else 
          ctx.log info say (alreadyRegisteredMsg)
        Behaviors.same

      case AvailableManagers(_) =>
        ctx.log info say (noManagersMsg)
        Behaviors.same
