package me.bytebeats.tools.ts.parser_ext

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.bytebeats.tools.ts.parser.ITaskParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by bytebeats on 2022/2/8 : 21:18
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
object JsonTaskParser : ITaskParser {
    override fun parse(inputStream: InputStream): List<ITaskParser.ProjectInfo> {
        return GsonBuilder().setPrettyPrinting().create()
            .fromJson(
                convertInputStreamToString(inputStream),
                object : TypeToken<ArrayList<ITaskParser.ProjectInfo>>() {}.type
            )
            ?: emptyList()
    }

    private fun convertInputStreamToString(inputStream: InputStream): String {
        val builder = StringBuilder()
        BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
            for (line in lines) {
                builder.append(line)
            }
        }
        return builder.toString()
    }

}