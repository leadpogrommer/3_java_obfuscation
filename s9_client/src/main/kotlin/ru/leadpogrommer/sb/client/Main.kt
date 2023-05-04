package ru.leadpogrommer.sb.client
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.decodeToSequence
import kotlinx.serialization.json.encodeToStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.StringBuilder
import java.net.Socket

fun main() {
    RealMain().main()
}


class XorInputStream(val src: InputStream, val key: ByteArray): InputStream(){
    var kp = 0
    override fun read(): Int {
        val v = src.read()
        if(v == -1){
            return v;
        }
        return (v xor key[kp].toInt()).also {
            kp = (kp + 1) % key.size
        }
    }
}

class XorOutputStream(val dst: OutputStream, val key: ByteArray): OutputStream(){
    var kp = 0
    override fun write(v: Int) {
        dst.write(v xor key[kp].toInt())
        kp = (kp + 1) % key.size
    }
}


@kotlinx.serialization.Serializable()
data class Request(val command: String, val arg1: String?, val arg2: String?, val arg3: String?)

@kotlinx.serialization.Serializable()
data class Response(val resp: String)

fun readString(str: InputStream): String{
    val b = StringBuilder()
    while (true){
        val v = str.read()
        if(v < 0){
            throw IOException()
        }
        if(v == 0){
            break
        }
        b.append(v.toChar())
    }
    return b.toString()
}

class RealMain(){
    @OptIn(ExperimentalSerializationApi::class)
    fun main(){
        println("Hello from 'protected' code!")
        val socket = Socket("127.0.0.1", 1337)
        val key = "45fdebf7eadc00a8498a37bf284a020a2b62f56cf7baf2248560e6188851".toByteArray()
        val inStream = XorInputStream(socket.getInputStream(), key)
        val outStream = XorOutputStream(socket.getOutputStream(), key)
        while (true){
            print("[bank] ")
            val cmd = readln().split(Regex("\\s+"))
            Json.encodeToStream(Request(cmd[0], cmd.getOrNull(1), cmd.getOrNull(2), cmd.getOrNull(3)), outStream)
            outStream.write(0)
            socket.getOutputStream().flush()
            val resp = Json.decodeFromString<Response>(readString(inStream))
            println(resp.resp)
        }
    }
}