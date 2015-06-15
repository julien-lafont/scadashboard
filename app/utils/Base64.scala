package utils

import java.nio.charset.Charset
import org.apache.commons.codec.binary.{Base64 => B64}

object Base64 {

  private lazy val UTF8 = Charset.forName("UTF8")

  def encode(from: String, charset: Charset = UTF8): String =
    new String(B64.encodeBase64(from.getBytes(charset)), charset)

  def decode(from: String, charset: Charset = UTF8): String =
    new String(B64.decodeBase64(from.getBytes(charset)), charset)
}
