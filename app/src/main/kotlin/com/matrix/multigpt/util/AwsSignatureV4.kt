package com.matrix.multigpt.util

import io.ktor.http.HttpHeaders
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AwsSignatureV4 {
    
    private const val AWS4_REQUEST = "aws4_request"
    private const val AWS4_HMAC_SHA256 = "AWS4-HMAC-SHA256"
    private const val ALGORITHM = "HmacSHA256"
    private const val X_AMZ_DATE = "x-amz-date"
    private const val AUTHORIZATION = "Authorization"
    
    data class AwsCredentials(
        val accessKeyId: String,
        val secretAccessKey: String,
        val sessionToken: String? = null,
        val region: String
    )
    
    data class SignedRequest(
        val headers: Map<String, String>,
        val signedHeaders: String,
        val signature: String
    )
    
    fun signRequest(
        method: String,
        uri: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: MutableMap<String, String> = mutableMapOf(),
        payload: String,
        credentials: AwsCredentials,
        service: String = "bedrock"
    ): SignedRequest {
        
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val amzDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        val dateStamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        
        // Add required headers
        headers[X_AMZ_DATE] = amzDate
        headers[HttpHeaders.Host] = "${service}-runtime.${credentials.region}.amazonaws.com"
        
        if (credentials.sessionToken != null) {
            headers["x-amz-security-token"] = credentials.sessionToken
        }
        
        // Create canonical request
        val canonicalUri = uri.ifEmpty { "/" }
        val canonicalQueryString = createCanonicalQueryString(queryParams)
        val canonicalHeaders = createCanonicalHeaders(headers)
        val signedHeaders = createSignedHeaders(headers)
        val payloadHash = sha256Hex(payload)
        
        val canonicalRequest = listOf(
            method,
            canonicalUri,
            canonicalQueryString,
            canonicalHeaders,
            signedHeaders,
            payloadHash
        ).joinToString("\n")
        
        // Create string to sign
        val credentialScope = listOf(dateStamp, credentials.region, service, AWS4_REQUEST).joinToString("/")
        val stringToSign = listOf(
            AWS4_HMAC_SHA256,
            amzDate,
            credentialScope,
            sha256Hex(canonicalRequest)
        ).joinToString("\n")
        
        // Calculate signature
        val signature = calculateSignature(
            stringToSign,
            credentials.secretAccessKey,
            dateStamp,
            credentials.region,
            service
        )
        
        // Create authorization header
        val authorizationHeader = "$AWS4_HMAC_SHA256 Credential=${credentials.accessKeyId}/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
        headers[AUTHORIZATION] = authorizationHeader
        
        return SignedRequest(headers, signedHeaders, signature)
    }
    
    private fun createCanonicalQueryString(queryParams: Map<String, String>): String {
        return queryParams
            .toSortedMap()
            .map { "${urlEncode(it.key)}=${urlEncode(it.value)}" }
            .joinToString("&")
    }
    
    private fun createCanonicalHeaders(headers: Map<String, String>): String {
        return headers
            .map { it.key.lowercase() to it.value.trim() }
            .sortedBy { it.first }
            .joinToString("\n") { "${it.first}:${it.second}" } + "\n"
    }
    
    private fun createSignedHeaders(headers: Map<String, String>): String {
        return headers
            .keys
            .map { it.lowercase() }
            .sorted()
            .joinToString(";")
    }
    
    private fun calculateSignature(
        stringToSign: String,
        secretKey: String,
        dateStamp: String,
        region: String,
        service: String
    ): String {
        val kDate = hmacSHA256("AWS4$secretKey".toByteArray(), dateStamp)
        val kRegion = hmacSHA256(kDate, region)
        val kService = hmacSHA256(kRegion, service)
        val kSigning = hmacSHA256(kService, AWS4_REQUEST)
        
        return hmacSHA256(kSigning, stringToSign).joinToString("") { "%02x".format(it) }
    }
    
    private fun hmacSHA256(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(key, ALGORITHM))
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }
    
    private fun sha256Hex(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
    
    private fun urlEncode(value: String): String {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }
}
