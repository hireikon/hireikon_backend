package com.hireikon.hireikon_backend.shared

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val errors: List<String>? = null
) {
    companion object {
        fun <T> ok(data: T, message: String = "Success") =
            ApiResponse(success = true, message = message, data = data)

        fun <T> created(data: T, message: String = "Created successfully") =
            ApiResponse(success = true, message = message, data = data)

        fun error(message: String, errors: List<String>? = null) =
            ApiResponse<Nothing>(success = false, message = message, errors = errors)
    }
}

class ResourceNotFoundException(message: String) : RuntimeException(message)

class DuplicateResourceException(message: String) : RuntimeException(message)

class UnauthorizedException(message: String = "Access denied") : RuntimeException(message)

class BadRequestException(message: String) : RuntimeException(message)

class AiProcessingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class FileStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)