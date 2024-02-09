package com.github.vharatian.refexpo.models

import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SequenceWriter
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.*
import kotlin.reflect.KClass

class CsvStreamWriter<T : Any>(private val file: File, private val type: KClass<T>): Closeable {
    private val fileWriter: BufferedWriter = file.bufferedWriter()
    private val csvMapper: CsvMapper = CsvMapper()
    private val schema: CsvSchema = csvMapper.schemaFor(type.java).withHeader()
    private val objectWriter: SequenceWriter? = csvMapper.writer(schema).writeValues(fileWriter)

    init {
        // Write the header
        if(objectWriter == null) {
            throw Exception("Failed to create object writer")
        }
    }

    fun write(data: T) {
        objectWriter?.write(data)
    }

    override fun close() {
        objectWriter?.flush()
        objectWriter?.close()
    }
}