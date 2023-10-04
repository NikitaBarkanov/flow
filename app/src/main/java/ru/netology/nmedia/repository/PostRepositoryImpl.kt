package ru.netology.nmedia.repository

import android.accounts.NetworkErrorException
import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import ru.netology.nmedia.api.*
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAll()
        .map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)


    override suspend fun getAll() {
        try {
            val response = PostsApi.service.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity(true))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override fun getNewerCount(): Flow<Int> = flow {
        while (true) {
            try {
                delay(10_000)

                val postId = dao.getLatest().first().firstOrNull()?.id ?: 0

                val response = PostsApi.service.getNewer(postId)

                val posts = response.body().orEmpty()

                dao.insert(posts.toEntity(true))
                emit(posts.size)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun save(post: Post) {
        try {
            val response = PostsApi.service.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            val response = PostsApi.service.removeById(id)

            if (!response.isSuccessful) {
                throw RuntimeException(response.message())
            }
            dao.removeById(id)
        } catch (e: IOException) {
            throw NetworkErrorException()
        }
    }

    override suspend fun likeById(id: Long){
        try {

            dao.likeById(id)
            val response = PostsApi.service.likeById(id)

            if (!response.isSuccessful) {
                throw RuntimeException(response.message())
            }
            dao
            return (response.body() ?: throw RuntimeException("body is null")) as Unit
        } catch (e: IOException) {
            throw NetworkErrorException()
        }
    }

        override fun switchHidden() {
            dao.getAllInvisible()
        }
    }
