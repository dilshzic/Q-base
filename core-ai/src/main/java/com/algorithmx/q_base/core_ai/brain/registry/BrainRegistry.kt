package com.algorithmx.q_base.core_ai.brain.registry

import com.algorithmx.q_base.core_ai.brain.models.BrainCategory
import com.algorithmx.q_base.core_ai.brain.models.BrainProvider

object BrainRegistry {
    // Valid Model IDs for Groq and Gemini as of 2025 Free Tier Limits
    val categoryMap = mapOf(
        BrainCategory.REASONING to listOf(
            // Gemini Flash Family (Fast text generation)
            "gemini-3-flash-preview",
            "gemini-2.5-flash",
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash-lite",

            // Gemma 3 Family (Google's open-weights hosted on Gemini API)
            "gemma-3-27b-it",
            "gemma-3-12b-it",
            "gemma-3-4b-it",
            "gemma-3-1b-it",

            // Groq High-Tier Models
            "llama-3.3-70b-versatile",
            "qwen/qwen3-32b",
            "openai/gpt-oss-120b",
            "meta-llama/llama-4-scout-17b-16e-instruct"
        ),
        BrainCategory.FUNCTION_CALLING to listOf(
            "gemini-3-flash-preview",
            "gemini-2.5-flash",
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "groq/compound"
        ),
        BrainCategory.TEXT_TO_TEXT to listOf(
            "gemini-3-flash-preview",
            "gemini-2.5-flash",
            "gemini-3.1-flash-lite-preview",
            "gemini-2.5-flash-lite",
            "gemma-3-27b-it",
            "gemma-3-12b-it",

            // Groq Models
            "allam-2-7b",
            "groq/compound",
            "groq/compound-mini",
            "llama-3.1-8b-instant",
            "llama-3.3-70b-versatile",
            "meta-llama/llama-4-scout-17b-16e-instruct",
            "meta-llama/llama-prompt-guard-2-22m",
            "meta-llama/llama-prompt-guard-2-86m",
            "moonshotai/kimi-k2-instruct",
            "moonshotai/kimi-k2-instruct-0905",
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b",
            "openai/gpt-oss-safeguard-20b",
            "qwen/qwen3-32b"
        )
    )

    fun getProviderForModel(modelName: String): BrainProvider {
        return when {
            modelName.startsWith("gem") -> BrainProvider.GEMINI
            modelName.startsWith("gemma-local") -> BrainProvider.LOCAL_GEMMA
            else -> BrainProvider.GROQ
        }
    }
}
