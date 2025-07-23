package it.unibo.agar.view

import it.unibo.agar.model.MockGameStateManager
import it.unibo.agar.model.World

import java.awt.Graphics2D
import scala.swing.*

class LocalView(manager: MockGameStateManager, playerId: String) extends MainFrame:

  title = s"Agar.io - Local View ($playerId)"
  preferredSize = new Dimension(400, 400)

  private var worldOpt: Option[World] = None

  def updateWorld(update: World): Unit =
    worldOpt = Some(update)
    repaint()

  contents = new Panel:
    listenTo(keys, mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit = worldOpt match
      case Some(world) =>
        val (offsetX, offsetY) = world.players find (_.id == playerId) match
          case Some(p) => (p.x - size.width / 2.0, p.y - size.height / 2.0)
          case None    => (0.0, 0.0)

        AgarViewUtils.drawWorld(g, world, offsetX, offsetY)
      case None =>
        g.drawString("Loading...", size.width / 2, size.height / 2)

    reactions += { case e: event.MouseMoved =>
      val mousePos = e.point
      val playerOpt = manager.getWorld.players.find(_.id == playerId)
      playerOpt.foreach: player =>
        val dx = (mousePos.x - size.width / 2) * 0.01
        val dy = (mousePos.y - size.height / 2) * 0.01
        manager.movePlayerDirection(playerId, dx, dy)
      repaint()
    }
