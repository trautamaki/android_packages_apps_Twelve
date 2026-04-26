/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ext

private val bracketedRegex = Regex("""\(.*?\)|\[.*?]""")
private val featuringRegex = Regex("""\b(feat|ft)\b\.?.*""")
private val nonAlphanumericRegex = Regex("""[^\p{L}\p{N} ]""")
private val whitespaceRegex = Regex("""\s+""")

/**
 * Normalize a track title for fuzzy matching: lowercase, drop bracketed parts and
 * feat./ft. suffixes, strip punctuation (keeping non-ASCII letters), collapse whitespace.
 */
fun String.normalizedTitle() = lowercase()
    .replace(bracketedRegex, " ")
    .replace(featuringRegex, "")
    .replace(nonAlphanumericRegex, "")
    .replace(whitespaceRegex, " ")
    .trim()
