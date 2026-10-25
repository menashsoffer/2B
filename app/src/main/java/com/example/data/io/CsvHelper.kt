package com.example.data.io

object CsvHelper {
    fun parseCsv(content: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val lines = content.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            
            // Simple robust CSV line splitter supporting quoted values
            val tokens = mutableListOf<String>()
            val sb = java.lang.StringBuilder()
            var inQuotes = false
            for (i in trimmed.indices) {
                val c = trimmed[i]
                if (c == '\"') {
                    inQuotes = !inQuotes
                } else if (c == ',' && !inQuotes) {
                    tokens.add(sb.toString().trim().removeSurrounding("\""))
                    sb.setLength(0)
                } else {
                    sb.append(c)
                }
            }
            tokens.add(sb.toString().trim().removeSurrounding("\""))
            if (tokens.any { it.isNotEmpty() }) {
                result.add(tokens)
            }
        }
        return result
    }

    fun exportToCsv(headers: List<String>, rows: List<List<String>>): String {
        val sb = java.lang.StringBuilder()
        sb.append(headers.joinToString(",") { escapeCsvCell(it) }).append("\n")
        for (row in rows) {
            sb.append(row.joinToString(",") { escapeCsvCell(it) }).append("\n")
        }
        return sb.toString()
    }

    private fun escapeCsvCell(cell: String): String {
        return if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }
    }
}
