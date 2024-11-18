package mimasa7

import spinal.core._
import spinal.lib._

import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.{BoxLayout, ImageIcon, JFrame, JLabel, WindowConstants}

// GUI imports
import java.awt
import java.awt.Image
import java.awt.event.{ActionEvent, ActionListener, MouseEvent, MouseListener}
import javax.swing

class Screen {
  val bufferedImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)

  def clear(): Unit  =  {
    val raster = bufferedImage.getRaster
    val data = Array[Int](0xff, 0x0, 0xff, 0x0)

    for (y <- 0 until bufferedImage.getHeight)
      for (x <- 0 until bufferedImage.getWidth)
        raster.setPixel(x, y, data)
  }

  val frame = new JFrame {
    setLayout(new BoxLayout(getContentPane, BoxLayout.Y_AXIS))
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    setSize(256, 256)

    clear()

    add(new JLabel("Hello"))
    add(new JLabel(new ImageIcon(bufferedImage)))

    setVisible(true)

    //val outputFile = new File("test.png")
    //ImageIO.write(bufferedImage, "PNG", outputFile)


  }
}

object Screen {
  def main(args: Array[String]): Unit = {
    val screen = new Screen()
  }
}

case class HDMI() extends Bundle {
  // clock
  val clk_p = Bool()
  val clk_n = Bool()
  // red, green, blue
  val tmds_p = Bits(3 bits)
  val tmds_n = Bits(3 bits)
}

case class HDMISimConfig(width: Int, height: Int) {
  require(width > 0 && height > 0, "Invalid screen dimensions")
}

class HDMISim(config: HDMISimConfig) extends Component {
  val io = new Bundle {
    val hdmi = in(HDMI())
  }


}


