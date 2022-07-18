package com.common.download.bean

import java.math.BigDecimal

class DownloadProgress {
    var downloadSize: Long = 0
    var totalSize: Long = 0
    /**
     * Return total size str. eg: 10M
     */
    fun totalSizeStr(): String {
        return totalSize.formatSize()
    }

    /**
     * Return download size str. eg: 3M
     */
    fun downloadSizeStr(): String {
        return downloadSize.formatSize()
    }

    /**
     * Return percent number.
     */
    fun percent(): Double {
        return downloadSize ratio totalSize
    }

    /**
     * Return percent string.
     */
    fun percentStr(): String {
        return "${percent()}%"
    }

    private fun Long.formatSize(): String {
        require(this >= 0) { "Size must larger than 0." }

        val byte = this.toDouble()
        val kb = byte / 1024.0
        val mb = byte / 1024.0 / 1024.0
        val gb = byte / 1024.0 / 1024.0 / 1024.0
        val tb = byte / 1024.0 / 1024.0 / 1024.0 / 1024.0

        return when {
            tb >= 1 -> "${tb.decimal(2)} TB"
            gb >= 1 -> "${gb.decimal(2)} GB"
            mb >= 1 -> "${mb.decimal(2)} MB"
            kb >= 1 -> "${kb.decimal(2)} KB"
            else -> "${byte.decimal(2)} B"
        }
    }

    private fun Double.decimal(digits: Int): Double {
        return this.toBigDecimal()
            .setScale(digits, BigDecimal.ROUND_HALF_UP)
            .toDouble()
    }

    private infix fun Long.ratio(bottom: Long): Double {
        if (bottom <= 0) {
            return 0.0
        }
        val result = (this * 100.0).toBigDecimal().divide((bottom * 1.0).toBigDecimal(), 2, BigDecimal.ROUND_HALF_UP)
        return result.toDouble()
    }

    override fun toString(): String {
        return "DownloadProgress(downloadSize=$downloadSize, totalSize=$totalSize, percent=${percentStr()})"
    }

}
