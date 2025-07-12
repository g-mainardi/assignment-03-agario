package it.unibo.agar.model

trait GameStateManager:

  def getWorld: World
  def movePlayerDirection(id: String, dx: Double, dy: Double): Unit

class MockGameStateManager(
    var world: World,
    val speed: Double = 10.0
) extends GameStateManager:

  private var directions: Map[String, (Double, Double)] = Map.empty
  def getWorld: World = world

  // Move a player in a given direction (dx, dy)
  def movePlayerDirection(id: String, dx: Double, dy: Double): Unit =
    directions = directions.updated(id, (dx, dy))

  def tick(): Unit =
    directions.foreach:
      case (id, (dx, dy)) =>
        world.playerById(id) match
          case Some(player) =>
            world = updateWorldAfterMovement(updatePlayerPosition(player, dx, dy))
          case None =>
          // Player not found, ignore movement

  private def updatePlayerPosition(player: Player, dx: Double, dy: Double): Player =
    val newX = (player.x + dx * speed).max(0).min(world.width)
    val newY = (player.y + dy * speed).max(0).min(world.height)
    player.copy(x = newX, y = newY)

  private def updateWorldAfterMovement(player: Player): World =
    val (foodEaten, playersEaten, playerEatPlayers) = GameWorld.getPlayerStats(world, player)
    world
      .updatePlayer(playerEatPlayers)
      .removePlayers(playersEaten)
      .removeFoods(foodEaten)