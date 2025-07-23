package it.unibo.agar.view

import it.unibo.agar.model.World

import java.awt.Graphics2D
import scala.swing.*

class GlobalView extends MainFrame:

  title = "Agar.io - Global View"
  preferredSize = new Dimension(800, 800)
  private var current: Option[World] = None

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      current foreach:
        AgarViewUtils drawWorld (g, _)

  def update(world: World): Unit =
    current = Some(world)
    repaint()