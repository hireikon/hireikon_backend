package com.hireikon.hireikon_backend.service

import com.hireikon.hireikon_backend.shared.BadRequestException
import com.hireikon.hireikon_backend.shared.FileStorageException
import org.imgscalr.Scalr
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

@Service
class ImageStorageService(
    @Value("\${app.supabase.url}") private val supabaseUrl: String,
    @Value("\${app.supabase.anon-key}") private val supabaseAnonKey: String,
    @Value("\${app.supabase.profile-avatar-storage-bucket}") private val bucket: String
) {

    private val webClient = WebClient.builder()
        .baseUrl("$supabaseUrl/storage/v1")
        .defaultHeader("apiKey", supabaseAnonKey)
        .defaultHeader("Authorization", "Bearer $supabaseAnonKey")
        .build()

    companion object {
        private val ALLOWED_TYPES = setOf(
            "image/jpeg", "image/png", "image/webp"
        )
        private const val MAX_SIZE_BYTES = 5 * 1024 * 1024
        private const val TARGET_DIMENSION = 400
        private const val JPEG_QUALITY = 0.82f
    }

    fun uploadProfilePhoto(
        file: MultipartFile,
        userId: String,
        role: String
    ): String {
        validateImageFile(file)

        val compressed = compressImage(file)
        val filePath = "$role/$userId.jpg"

        try {
            webClient.put()
                .uri("/object/$bucket/$filePath")
                .header("x-upsert", "true")
                .contentType(MediaType.IMAGE_JPEG)
                .bodyValue(compressed)
                .retrieve()
                .onStatus({ it.isError }) { res ->
                    res.bodyToMono<String>().map { body ->
                        FileStorageException("Supabase error ${res.statusCode()}: $body")
                    }
                }
                .bodyToMono<String>()
                .block()
        } catch (ex: Exception) {
            throw FileStorageException("Failed to upload profile photo: ${ex.message}", ex)
        }

        return "$supabaseUrl/storage/v1/object/public/$bucket/$filePath"
    }

    fun deleteProfilePhoto(userId: String, role: String) {
        val filePath = "$role/$userId.jpg"
        try {
            webClient.delete()
                .uri("/object/$bucket/$filePath")
                .retrieve()
                .bodyToMono<String>()
                .block()
        } catch (ex: Exception) {
            println("Warning: Could not delete profile photo for $userId: ${ex.message}")
        }
    }

    private fun compressImage(file: MultipartFile): ByteArray {
        val original: BufferedImage = ImageIO.read(file.inputStream)
            ?: throw BadRequestException("Could not read image file — it may be corrupted")

        val resized: BufferedImage = if (
            original.width > TARGET_DIMENSION || original.height > TARGET_DIMENSION
        ) {
            Scalr.resize(
                original,
                Scalr.Method.QUALITY,
                Scalr.Mode.AUTOMATIC,
                TARGET_DIMENSION,
                TARGET_DIMENSION
            )
        } else original

        val rgb = BufferedImage(resized.width, resized.height, BufferedImage.TYPE_INT_RGB).also {
            val g = it.createGraphics()
            g.drawImage(resized, 0, 0, Color.WHITE, null)
            g.dispose()
        }

        val output = ByteArrayOutputStream()
        val writers = ImageIO.getImageWritersByFormatName("jpeg")
        if (!writers.hasNext()) throw FileStorageException("No JPEG writer available")

        val writer = writers.next()
        val params = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = JPEG_QUALITY
        }

        val ios = ImageIO.createImageOutputStream(output)
        writer.output = ios
        writer.write(null, IIOImage(rgb, null, null), params)
        writer.dispose()
        ios.close()

        return output.toByteArray()
    }

    private fun validateImageFile(file: MultipartFile) {
        if (file.isEmpty) throw BadRequestException("File is empty")

        if (file.contentType !in ALLOWED_TYPES) {
            throw BadRequestException(
                "Only JPEG, JPG, PNG and WebP images are allowed. Received: ${file.contentType}"
            )
        }

        if (file.size > MAX_SIZE_BYTES) {
            throw BadRequestException("Image size must not exceed 5MB")
        }
    }
}