package it.unibo.agar.distributed

import akka.actor.typed.ActorRef
import it.unibo.agar.model.{Food, World}

object GameProtocol:
  type PlayerId = String
  type FoodId = String
  type Position = (Double, Double)
  type Direction = (Double, Double)

  sealed trait Message

  case class GetWorld(world: World) extends Message

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

  trait GlobalViewMessage extends Message
  trait GameOverMessage extends Message
  enum ListenerMessages extends GameOverMessage, GlobalViewMessage:
    case RefreshTimer
    case WorldUpdate(world: World)
  val updateAdapter: GetWorld => ListenerMessages.WorldUpdate = msg => ListenerMessages.WorldUpdate(msg.world)

  // Game Coordinator protocol
  enum GameMessage extends Message:
    case PlayerMove(playerId: PlayerId, direction: Direction)
    case PlayerJoined(playerId: PlayerId, ref: ActorRef[StandardPlayerMessage], ai: Boolean = false)
    case PlayerLeft(playerId: PlayerId)
    case NewFood(food: Food)
    case ThereIsAWinner(winner: PlayerId)
    case WorldRequest(ref: ActorRef[GetWorld])

  // ReceptionistListingMessage
  final case class AvailableManagers(listings: Set[ActorRef[GameMessage]])
    extends GlobalViewMessage
      with FoodMessage
      with UserPlayerMessage
      with AIPlayerMessage
      with GameOverMessage
