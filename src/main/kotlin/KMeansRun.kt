import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

fun main(args : Array<String>) {
    println(System.getProperty("user.dir"))
    val kMeans = KMeans(loadCSV(4),5,4,0.000000001,6)
    var start = System.currentTimeMillis()
    //Loop to constantly test speed of Kmeans
    /*while (true) {
        start = System.currentTimeMillis()
        val clusters = kMeans.doKmeansParallel()
        println("Elapsed: " + (System.currentTimeMillis() - start))
        /*for(cluster in clusters){
            print("Cluster: " + cluster.size)
            /*for(point in cluster){
                println("Point: "+point.toString())
            }*/
        }*/
    }*/
    var avgSerial = 0L
    var avgThreads = 0L
    val numOfRepeat = 10
    println("Point count "+kMeans.points.size)
    var clusters = mutableListOf<MutableList<Point>>()
    for (i in 0 until numOfRepeat){
        start = System.currentTimeMillis()
        clusters = kMeans.doKmeansSerial()
        avgSerial += (System.currentTimeMillis() - start)
        start = System.currentTimeMillis()
        clusters = kMeans.doKmeansParallel()
        avgThreads += (System.currentTimeMillis() - start)
        println(i)
    }
    avgSerial /= numOfRepeat
    avgThreads /= numOfRepeat
    println("Serial in: $avgSerial\nThreads in: $avgThreads")
    for(cluster in clusters){
        println("Cluster: "+cluster.size)
        //Uncomment to see points from cluster
        /*for(point in cluster){
            println("Point: "+point.toString())
        }*/
    }
}

fun loadCSV(dimension: Int):MutableList<Point>{
    var fileReader: BufferedReader? = null
    val points = mutableListOf<Point>()
    try {
        var line: String?
        fileReader = BufferedReader(FileReader("fakeDataBig.csv"))
        line = fileReader.readLine()
        while (line != null) {
            val tokens = line.split(";")
            val point = mutableListOf<Double>()
            for(i in 0 until dimension){
                point.add(tokens[i].toDouble())
            }
            points.add(Point(point,dimension))
            line = fileReader.readLine()
        }

    } catch (e: Exception) {
        println("Reading CSV Error!")
        e.printStackTrace()
    } finally {
        try {
            fileReader!!.close()
        } catch (e: IOException) {
            println("Closing fileReader Error!")
            e.printStackTrace()
        }
    }
    return points
}