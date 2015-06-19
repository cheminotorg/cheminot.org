package cheminotorg
package misc

import java.io.{ File, FileInputStream, FileOutputStream }

object Files {

  def copy(from: String, to: String): Option[File] = {
    Option(new File(from)).filter(_.exists).map { inputFile =>
      val outputFile = new File(to)
      new File(outputFile.getParent()).mkdirs
      val fin = new FileInputStream(inputFile)
      val fout = new FileOutputStream(outputFile)
      fout.getChannel.transferFrom(fin.getChannel, 0, Long.MaxValue)
      outputFile
    }
  }
}
