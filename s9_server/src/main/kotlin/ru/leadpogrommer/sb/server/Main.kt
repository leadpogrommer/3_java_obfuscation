package ru.leadpogrommer.sb.server
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.lang.StringBuilder
import java.net.ServerSocket
import java.net.Socket
import java.util.Random
import kotlin.math.absoluteValue


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
        print((v xor key[kp].toInt()).toChar())
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

class Bank(){
    class ClientData(val name: String, val password: String){
        var balance = 0
    }

    val clients = mutableMapOf<String, ClientData>()
    @Synchronized
    fun register(name: String, password: String): String{
        if(clients.keys.contains(name)){
            return "Name already taken"
        }
        clients[name] = ClientData(name, password).also { it.balance = 1000 }
        return "Ok, your starting balance is 1000"
    }
    @Synchronized
    fun login(name: String, password: String): Boolean{
        return clients[name]?.let { it.password == password } ?: false
    }
    @Synchronized
    fun transfer(from: String, to: String, amount: Int): String{
        if(clients[from] == null || clients[to] == null){
            return "Wrong client name"
        }
        if(amount < 1){
            return "Invalid amount"
        }
        if(clients[from]!!.balance < amount){
            return "You are too poor"
        }
        clients[from]!!.balance -= amount
        clients[to]!!.balance += amount
        println("Transaction from $from to $to, amount: $amount")
        return "Ok, you now have ${clients[from]!!.balance}"
    }

    @Synchronized
    fun balance(username: String): Int{
        return clients[username]!!.balance
    }
}

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

class ClientHandler(val socket: Socket, val bank: Bank): Thread(){
    val key = "45fdebf7eadc00a8498a37bf284a020a2b62f56cf7baf2248560e6188851".toByteArray()
    data class PreparedTransaction(
        val to: String,
        val amount: Int,
        val code: String,
    )
    var transaction: PreparedTransaction? = null
    var username: String? = null

    @OptIn(ExperimentalSerializationApi::class)
    override fun run() {
        val inStream = XorInputStream(socket.getInputStream(), key)
        val outStream = XorOutputStream(socket.getOutputStream(), key)
        while (socket.isConnected){
            println("Trying to read cmd:")
            val cmd = Json.decodeFromString<Request>(readString(inStream))
            println("Got cmd: $cmd")
            try {
                val resp = if(username == null){
                    when (cmd.command){
                        "register" -> bank.register(cmd.arg1!!, cmd.arg2!!)
                        "login" -> if(bank.login(cmd.arg1!!, cmd.arg2!!)){
                            username = cmd.arg1!!
                            "Login ok"
                        }else{
                            "Login error"
                        }
                        else -> "Unknown command"
                    }
                }else{
                    when(cmd.command){
                        "balance" -> {
                            "You have ${bank.balance(username!!)}"
                        }
                        "transfer" ->{
                            val code = Random().nextInt().absoluteValue % 1000000
                            transaction = PreparedTransaction(cmd.arg1!!, cmd.arg2!!.toInt(), code.toString())
                            println("Transaction code: $code")
                            "Please confirm your transaction with code"
                        }
                        "confirm" -> {
                            if(transaction == null){
                                "Nothing to confirm"
                            }else if(cmd.arg1!! == transaction!!.code.toString()){
                                bank.transfer(username!!, transaction!!.to, transaction!!.amount).also {
                                    transaction = null
                                }
                            }else{
                                "Wrong code"
                            }
                        }
                        else -> "Unknown command"
                    }
                }
                Json.encodeToStream(Response(resp), outStream)
                outStream.write(0)
            }catch (e: Exception){
                Json.encodeToStream(Response(e.toString() + e.stackTraceToString()), outStream)
                outStream.write(0)
            }
            socket.getOutputStream().flush()

        }
    }
}

class RealMain(){
    @OptIn(ExperimentalSerializationApi::class)
    fun main(){
        println("Hello from 'protected' code!")
        val socket = ServerSocket(1337)
        val bank = Bank()

        while (true){
            val client = socket.accept()
            println("Client connected")
            ClientHandler(client, bank).start()
        }
    }
}