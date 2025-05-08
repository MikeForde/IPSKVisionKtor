package com.example.serviceHelpers

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64

fun generateQrCodeBase64(text: String, size: Int = 300): String {
  val writer = QRCodeWriter()
  val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
  val bufferedImage: BufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix)
  val output = ByteArrayOutputStream()
  javax.imageio.ImageIO.write(bufferedImage, "png", output)
  val base64 = Base64.getEncoder().encodeToString(output.toByteArray())
  return "data:image/png;base64,$base64"
}
