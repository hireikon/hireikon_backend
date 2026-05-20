package com.hireikon.hireikon_backend.shared

data class CursorPage<T>(
    val data: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean,
    val pageSize: Int,
)

data class CursorRequest(
    val cursor: String? = null,    // ID of the last item on previous page (null = first page)
    val pageSize: Int = 20,
) {
    val validatedPageSize: Int
        get() = pageSize.coerceIn(1, 100)
}

// Fetches pageSize+1 in the repository, then call this to build the page
fun <Entity, Dto> List<Entity>.toCursorPage(
    pageSize: Int,
    idExtractor: (Entity) -> String,
    mapper: (Entity) -> Dto
): CursorPage<Dto> {
    val hasMore = size > pageSize
    val pageItems = if (hasMore) this.dropLast(1) else this
    val nextCursor = if (hasMore) idExtractor(pageItems.last()) else null

    return CursorPage(
        data = pageItems.map(mapper),
        nextCursor = nextCursor,
        hasMore = hasMore,
        pageSize = pageSize,
    )
}