package com.fakeflix

import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.nio.file.Paths

@RestController
@RequestMapping("/videos")
class VideoStreamController(
    private val videoRepository: VideoRepository
) {
    private val logger = LoggerFactory.getLogger(VideoStreamController::class.java)

    @GetMapping("/stream/{videoId}")
    suspend fun stream(
        @PathVariable videoId: Long,
        @RequestHeader(value = "Range", required = false) rangeHeader: String?
    ): ResponseEntity<Flux<DataBuffer>>  {
        logger.info("Received request to stream video with ID: $videoId")
        val video = videoRepository.findById(videoId).awaitFirst()

        if (video == null) {
            logger.info("Video not found with ID: $videoId")
            return ResponseEntity.notFound().build()
        }

        val videoTargetPath = Paths.get(video.url)
        val file = videoTargetPath.toFile()

        if (!file.exists()) {
            logger.info("Video file not found: ${file.name}")
            return ResponseEntity.notFound().build()
        }

        val fileLength = file.length()
        val range = rangeHeader?.removePrefix("bytes=")?.split("-")
        val start = range?.getOrNull(0)?.toLongOrNull() ?: 0L
        val end = range?.getOrNull(1)?.toLongOrNull() ?: (fileLength - 1)
        val contentLength = end - start + 1

        val inputStream = file.inputStream().apply { skip(start) }
        val flux = DataBufferUtils.readInputStream(
            { inputStream },
            DefaultDataBufferFactory(),
            4096
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("video/mp4")
        headers.setContentLength(contentLength)
        headers.add("Content-Range", "bytes $start-$end/$fileLength")
        headers.add("Accept-Ranges", "bytes")

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .headers(headers)
            .body(flux)
    }
}