package cheminotorg
package misc

import java.io.{ File, FileInputStream, FileOutputStream }

object Files {

  def read(from: String): Array[Byte] = {
    val fis = new FileInputStream(from)
    val data = org.apache.commons.io.IOUtils.toByteArray(fis)
    fis.close
    data
  }

  def write(data: Array[Byte], to: String) {
    val dest = new File(to)
    val parentDir = new File(dest.getParent)
    parentDir.mkdirs
    dest.createNewFile
    val fout = new FileOutputStream(dest);
    fout.write(data)
    fout.close
  }

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
