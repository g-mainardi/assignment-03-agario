package it.unibo.agar.model

import it.unibo.agar.distributed.GameProtocol.{Direction, PlayerId}

/** Object responsible for AI movement logic, separate from the game state management */
object AIMovement:

  /** Finds the nearest food for a given player in the world
    * @param player
    *   the ID of the player for whom to find the nearest food
    * @param world
    *   the current game world containing players and food
    * @return
    */
  def nearestFood(player: PlayerId, world: World): Option[Food] =
    world.foods
      .sortBy(food => world.playerById(player).map(p => p.distanceTo(food)).getOrElse(Double.MaxValue))
      .headOption

  /** Get the AI movement towards the nearest food.
    *
    * @param id
   *    AI player id
   *  @param world
   *    The current world (with foods)
    */
  def getAIMove(id: PlayerId, world: World): Option[Direction] =
    val aiOpt = world playerById id
    val foodOpt = nearestFood(id, world)
    (aiOpt, foodOpt) match
      case (Some(ai), Some(food)) =>
        val dx = food.x - ai.x
        val dy = food.y - ai.y
        val distance = math.hypot(dx, dy)
        if distance > 0 then
          val normalizedDx = dx / distance
          val normalizedDy = dy / distance
          Some(normalizedDx, normalizedDy)
        else 
          None
      case _ => None // Do nothing if AI or food doesn't exist