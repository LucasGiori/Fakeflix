package com.fakeflix

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.codec.multipart.FilePart
import java.time.Instant
import java.util.UUID
import java.nio.file.Paths

@RestController
@RequestMapping("/videos")
class VideoUploadController(
    private val videoRepository: VideoRepository
) {
    private val logger = LoggerFactory.getLogger(VideoUploadController::class.java)

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    suspend fun upload(
        @RequestPart("title") title: String,
        @RequestPart("description") description: String,
        @RequestPart("video") video: FilePart,
        @RequestPart("thumbnail") thumbnail: FilePart
    ): ResponseEntity<String> {
        val videoTargetPath = Paths.get("uploads/${generateUniqueFileName(video.filename())}")
        val thumbnailTargetPath = Paths.get("uploads/${generateUniqueFileName(thumbnail.filename())}")

        return try {
            coroutineScope {
                val videoJob = async { video.transferTo(videoTargetPath) }
                val thumbnailJob = async { thumbnail.transferTo(thumbnailTargetPath) }
                videoJob.await().awaitSingleOrNull()
                thumbnailJob.await().awaitSingleOrNull()
            }

            val videoEntity = Video(
                title = title,
                description = description,
                url = videoTargetPath.toString(),
                duration = 100,
                sizeInKb = (videoTargetPath.toFile().length() / 1024).toInt(),
                thumbnailUrl = thumbnailTargetPath.toString(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            videoRepository.save(videoEntity).awaitSingleOrNull()

            ResponseEntity.ok("Upload concluído!")
        } catch (ex: Throwable) {
            logger.error("Error: ${ex.message}")
            ResponseEntity.internalServerError().body("Error: ${ex.message}")
        }
    }

    private fun generateUniqueFileName(originalName: String): String {
        val timestamp = Instant.now().epochSecond
        val uuid = UUID.randomUUID().toString()
        val extension = originalName.substringAfterLast('.', "mp4")
        return "$timestamp-$uuid.$extension"
    }
}
