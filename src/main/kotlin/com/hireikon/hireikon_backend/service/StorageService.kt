package com.hireikon.hireikon_backend.service

import com.hireikon.hireikon_backend.shared.FileStorageException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class StorageService(
    @Value("\${app.supabase.url}") private val supabaseUrl: String,
    @Value("\${app.supabase.anon-key}") private val supabaseAnonKey: String,
    @Value("\${app.supabase.storage-bucket}") private val bucket: String
) {
    private val webClient = WebClient.builder()
        .baseUrl("$supabaseUrl/storage/v1")
        .defaultHeader("apikey", supabaseAnonKey)
        .defaultHeader("Authorization", "Bearer $supabaseAnonKey")
        .build()

    fun uploadResume(file: MultipartFile, userId: String): String {
        validateResumeFile(file)

        val filePath = "resumes/$userId/resume.pdf"

        try {
            webClient.put()
                .uri("/object/$bucket/$filePath")
                .contentType(MediaType.APPLICATION_PDF)
                .bodyValue(file.bytes)
                .retrieve()
                .bodyToMono<String>()
                .block()
        } catch (ex: Exception) {
            throw FileStorageException("Failed to upload resume to storage: ${ex.message}", ex)
        }

        return "$supabaseUrl/storage/v1/object/public/$bucket/$filePath"
    }

    fun deleteResume(userId: String) {
        val filePath = "resumes/$userId/resume.pdf"
        try {
            webClient.delete()
                .uri("/object/$bucket/$filePath")
                .retrieve()
                .bodyToMono<String>()
                .block()
        } catch (ex: Exception) {
            println("Warning: Could not delete resume for user $userId: ${ex.message}")
        }
    }

    private fun validateResumeFile(file: MultipartFile) {
        if (file.isEmpty) throw FileStorageException("File is empty")
        if (file.contentType != MediaType.APPLICATION_PDF_VALUE) {
            throw FileStorageException("Only PDF files are allowed. Received: ${file.contentType}")
        }
        val maxSizeBytes = 10 * 1024 * 1024 // 10MB
        if (file.size > maxSizeBytes) throw FileStorageException("File size exceeds 10MB limit")
    }
}