package com.example.serviceHelpers

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

fun generateQrCodeBase64(text: String, moduleSize: Int = 10, maxSize: Int = 600): String {
  val writer = QRCodeWriter()
  val hints =
      mapOf(
          EncodeHintType.MARGIN to 0 // shrink white border
          )

  // 1. Generate matrix first (size unknown in advance)
  val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 1, 1, hints)
  val matrixSize = bitMatrix.width

  // 2. Scale image size to maintain moduleSize
  val imageSize = (matrixSize * moduleSize).coerceAtMost(maxSize)

  // 3. Re-encode matrix at full image size
  val scaledMatrix = writer.encode(text, BarcodeFormat.QR_CODE, imageSize, imageSize, hints)
  val image: BufferedImage = MatrixToImageWriter.toBufferedImage(scaledMatrix)

  // 4. Convert to base64 PNG
  val output = ByteArrayOutputStream()
  ImageIO.write(image, "png", output)
  val base64 = Base64.getEncoder().encodeToString(output.toByteArray())
  return "data:image/png;base64,$base64"
}
