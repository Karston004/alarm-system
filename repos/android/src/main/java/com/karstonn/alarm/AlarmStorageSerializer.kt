package com.karstonn.alarm

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.karstonn.alarmsystem.proto.AlarmStorage
import java.io.InputStream
import java.io.OutputStream

class AlarmStorageSerializer : Serializer<AlarmStorage> {

    override val defaultValue: AlarmStorage =
        AlarmStorage.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AlarmStorage {
        try {
            return AlarmStorage.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException(
                "Could not read AlarmStorage",
                e
            )
        }
    }

    override suspend fun writeTo(
        t: AlarmStorage,
        output: OutputStream
    ) {
        t.writeTo(output)
    }
}