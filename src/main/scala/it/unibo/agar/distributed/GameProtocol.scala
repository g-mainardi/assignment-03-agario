package it.unibo.agar.distributed

import akka.actor.typed.ActorRef
import it.unibo.agar.model.{Direction, Food, PlayerId, World}

object GameProtocol:
  sealed trait Message

  // Players protocol
  sealed trait PlayerMessage extends Message
  trait UserPlayerMessage extends PlayerMessage
  trait AIPlayerMessage extends PlayerMessage
  enum StandardPlayerMessage extends UserPlayerMessage, AIPlayerMessage:
    case Start
    case WorldUpdate(gameState: World)
    case End(winner: PlayerId)
  enum AIPlayerMessages extends AIPlayerMessage:
    case Tick

  // Food Manager protocol
  trait FoodMessage extends Message
  enum FoodMessages extends FoodMessage:
    case GenerateFood

  // Listener protocol
  trait GlobalViewMessage extends Message
  trait GameOverMessage extends Message
  enum ListenerMessages extends GameOverMessage, GlobalViewMessage:
    case RefreshTimer
    case WorldUpdate(world: World)

  // Game Coordinator protocol
  enum GameMessage extends Message:
    case PlayerMove(playerId: PlayerId, direction: Direction)
    case PlayerJoined(playerId: PlayerId, ref: ActorRef[StandardPlayerMessage], ai: Boolean = false)
    case PlayerLeft(playerId: PlayerId)
    case NewFood(food: Food)
    case ThereIsAWinner(winner: PlayerId)
    case WorldRequest(ref: ActorRef[ListenerMessages.WorldUpdate])

  // ReceptionistListingMessage
  final case class AvailableManagers(listings: Set[ActorRef[GameMessage]])
    extends GlobalViewMessage
      with FoodMessage
      with UserPlayerMessage
      with AIPlayerMessage
      with GameOverMessage
  
  enum SpawnPlayerMessage extends Message:
    case SpawnUser
    case SpawnAI
