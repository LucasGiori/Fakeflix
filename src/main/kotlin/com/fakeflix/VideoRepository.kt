package com.fakeflix

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VideoRepository : ReactiveCrudRepository<Video, Long>
