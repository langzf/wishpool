package com.wishpool.core.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
class S3StorageConfig(
    @Value("\${wishpool.storage.s3.endpoint}") private val endpoint: String,
    @Value("\${wishpool.storage.s3.access-key}") private val accessKey: String,
    @Value("\${wishpool.storage.s3.secret-key}") private val secretKey: String,
    @Value("\${wishpool.storage.s3.region}") private val region: String,
    @Value("\${wishpool.storage.s3.path-style-access}") private val pathStyleAccess: Boolean,
) {
    @Bean
    fun s3Client(): S3Client =
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(credentialsProvider())
            .region(Region.of(region))
            .serviceConfiguration(s3Configuration())
            .build()

    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner.builder()
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(credentialsProvider())
            .region(Region.of(region))
            .serviceConfiguration(s3Configuration())
            .build()

    private fun credentialsProvider(): StaticCredentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

    private fun s3Configuration(): S3Configuration =
        S3Configuration.builder()
            .pathStyleAccessEnabled(pathStyleAccess)
            .build()
}
