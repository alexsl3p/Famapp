package com.kinly.famapp.features.storage

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Загрузка изображений в публичные бакеты Supabase Storage. */
@Singleton
class StorageRepository @Inject constructor(private val supabase: SupabaseClient) {

    /** Загружает аватар пользователя (перезаписывает по фиксированному пути) и возвращает public URL. */
    suspend fun uploadAvatar(userId: String, bytes: ByteArray): String {
        val path = "$userId/avatar.jpg"
        val bucket = supabase.storage.from(BUCKET_AVATARS)
        bucket.upload(path, bytes) { upsert = true }
        // cache-buster, чтобы Coil не показывал старую закешированную картинку
        return bucket.publicUrl(path) + "?v=" + System.currentTimeMillis()
    }

    /** Загружает фото для комментария к задаче и возвращает public URL. */
    suspend fun uploadTaskPhoto(familyId: String, bytes: ByteArray): String {
        val path = "$familyId/${UUID.randomUUID()}.jpg"
        val bucket = supabase.storage.from(BUCKET_TASK_PHOTOS)
        bucket.upload(path, bytes) { upsert = false }
        return bucket.publicUrl(path)
    }

    suspend fun uploadChatImage(familyId: String, bytes: ByteArray): String {
        val path = "$familyId/${UUID.randomUUID()}.jpg"
        val bucket = supabase.storage.from(BUCKET_CHAT)
        bucket.upload(path, bytes) { upsert = false }
        return bucket.publicUrl(path)
    }

    suspend fun uploadChatAudio(familyId: String, bytes: ByteArray): String {
        val path = "$familyId/${UUID.randomUUID()}.m4a"
        val bucket = supabase.storage.from(BUCKET_CHAT)
        bucket.upload(path, bytes) { upsert = false }
        return bucket.publicUrl(path)
    }

    private companion object {
        const val BUCKET_AVATARS = "avatars"
        const val BUCKET_TASK_PHOTOS = "task-photos"
        const val BUCKET_CHAT = "chat-media"
    }
}
