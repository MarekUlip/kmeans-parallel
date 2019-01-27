import kotlin.random.Random
import kotlinx.coroutines.*

class KMeans(val points: List<Point>, val k: Int, val dimension: Int, val minChange: Double, val numOfThreads: Int) {
    fun euklideanDistance(a:Point, b:Point):Double{
        var sum = 0.0
        for (i in 0 until a.dimension){
            sum+=Math.pow(a.point[i]-b.point[i],2.0)
        }
        return Math.sqrt(sum)
    }

    fun getClosestCentroidIndex(centroids: MutableList<Point>, point: Point):Int{
        var closest = -1
        var distance = Double.MAX_VALUE
        for((index, centroid) in centroids.withIndex()){
            val newDistance = euklideanDistance(point,centroid)
            if (newDistance<distance){
                distance = newDistance
                closest = index
            }
        }
        return closest
    }

    fun countNewCentroidForCluster(cluster: MutableList<Point>):Point{
        var newCentroid = Point(MutableList(dimension){0.0}, dimension)
        for (point in cluster){
            for (i in 0 until dimension){
                newCentroid.point[i] += point.point[i]
            }
        }
        for (i in 0 until dimension){
            newCentroid.point[i] = newCentroid.point[i] / cluster.size
        }
        return newCentroid
    }

    fun createNewCentroids(clusters: MutableList<MutableList<Point>>):MutableList<Point>{
        val centroids = mutableListOf<Point>()
        val coroutines = mutableListOf<Job>()
        runBlocking {
            for (cluster in clusters){
                coroutines.add(async {
                    val newCentroid = countNewCentroidForCluster(cluster)
                    synchronized(centroids){
                        centroids.add(newCentroid)
                    }
                    })
            }
            for (coroutine in coroutines){
                coroutine.join()
            }
        }
        return centroids
    }
    fun createNewCentroidsThread(clusters: MutableList<MutableList<Point>>):MutableList<Point>{
        val centroids = mutableListOf<Point>()
        val threads = mutableListOf<Thread>()
        val chunkSize = (k / numOfThreads).toInt()
        if (chunkSize == 0){
            for (cluster in clusters){
                threads.add(Thread {
                    val newCentroid = countNewCentroidForCluster(cluster)
                    synchronized(centroids){
                        centroids.add(newCentroid)
                    }
                })
                threads[threads.size-1].start()
            }
        } else{
            for (i in 0 until numOfThreads){
                threads.add(Thread{
                    val p = i
                    val end = if (i==numOfThreads-1){
                        k
                    }else {
                        chunkSize*(p+1)
                    }
                    for (j in chunkSize*p until end){
                        val newCentroid = countNewCentroidForCluster(clusters[j])
                        synchronized(centroids){
                            centroids.add(newCentroid)
                        }
                    }
                })
                threads[i].start()
            }
        }
        for (thread in threads){
            thread.join()
        }

        return centroids
    }

    fun countNewCentroidForClusterSerial(cluster: MutableList<Point>):Point{
        var newCentroid = Point(MutableList(dimension){0.0}, dimension)
        for (point in cluster){
            for (i in 0 until dimension){
                newCentroid.point[i] += point.point[i]
            }
        }
        for (i in 0 until dimension){
            newCentroid.point[i] = newCentroid.point[i] / cluster.size
        }
        return newCentroid
    }

    fun createNewCentroidsSerial(clusters: MutableList<MutableList<Point>>):MutableList<Point>{
        val centroids = mutableListOf<Point>()
        for (cluster in clusters){
            centroids.add(countNewCentroidForCluster(cluster))
        }
        return centroids
    }

    fun initializeClusters(points: List<Point>,centroids: MutableList<Point>): MutableList<MutableList<Point>>{
        val clusters = MutableList(centroids.size){ mutableListOf<Point>()}
        val coroutines = mutableListOf<Job>()
        runBlocking {
            for (point in points) {
                coroutines.add(async {
                    val centroidIndex = getClosestCentroidIndex(centroids, point)
                    //synchronized(clusters[centroidIndex]){
                        clusters[centroidIndex].add(point)
                    //}
                })
            }
            for (thread in coroutines) {
                thread.join()
            }
        }
        return clusters
    }

    fun initializeClustersThread(points: List<Point>,centroids: MutableList<Point>): MutableList<MutableList<Point>>{
        val clustersRes = MutableList(centroids.size){ mutableListOf<Point>()}
        val threads = mutableListOf<Thread>()
        val chunkSize = points.size/numOfThreads
        for (i in 0 until numOfThreads){
            threads.add(Thread {
                val clusters = MutableList(centroids.size){ mutableListOf<Point>()}
                val p = i
                val end = if (p==numOfThreads-1){
                    points.size//TODO check
                } else {
                    chunkSize*(p+1)
                }
                for (j in chunkSize*p until end){
                    val centroidIndex = getClosestCentroidIndex(centroids, points[j])
                    /*synchronized(clusters[centroidIndex]){
                        clusters[centroidIndex].add(points[j])
                    }*/
                    clusters[centroidIndex].add(points[j])
                }
                synchronized(clustersRes){
                    for ((index, cluster) in clusters.withIndex()){
                        clustersRes[index].addAll(cluster)
                    }
                }
            })
            threads[i].start()
        }

        for (thread in threads) {
            thread.join()
        }
        return clustersRes
    }

    fun initializeClustersSerial(points: List<Point>,centroids: MutableList<Point>): MutableList<MutableList<Point>>{
        val clusters = MutableList(centroids.size){ mutableListOf<Point>()}
        for (point in points){
            clusters[getClosestCentroidIndex(centroids,point)].add(point)
        }
        return clusters
    }

    fun checkCentroidChange(centroids: MutableList<Point>, newCentroids: MutableList<Point>):Boolean{
        var centroid_change = 0.0
        for ((index, centroid) in centroids.withIndex()){
            centroid_change += euklideanDistance(centroid, newCentroids[index])
        }
        return centroid_change>minChange
    }



    fun doKmeans(): MutableList<MutableList<Point>>{
        var centroids = mutableListOf<Point>()
        val occured = mutableListOf<Int>()
        for (i in 0 until k){
            var randNum = Random.nextInt(points.size)
            while (occured.contains(randNum)){
                randNum = Random.nextInt(points.size)
            }
            occured.add(randNum)
            centroids.add(points[randNum])
        }
        var clusters = initializeClusters(points,centroids)
        var newCentroids = createNewCentroids(clusters)
        while (checkCentroidChange(centroids,newCentroids)){
            centroids = newCentroids
            clusters = initializeClusters(points,centroids)
            newCentroids = createNewCentroids(clusters)
        }
        return clusters
    }

    fun doKmeansThreads(): MutableList<MutableList<Point>>{
        var centroids = mutableListOf<Point>()
        val occured = mutableListOf<Int>()
        for (i in 0 until k){
            var randNum = Random.nextInt(points.size)
            while (occured.contains(randNum)){
                randNum = Random.nextInt(points.size)
            }
            occured.add(randNum)
            centroids.add(points[randNum])
        }
        var clusters = initializeClustersThread(points,centroids)
        var newCentroids = createNewCentroidsThread(clusters)
        while (checkCentroidChange(centroids,newCentroids)){
            centroids = newCentroids
            clusters = initializeClustersThread(points,centroids)
            newCentroids = createNewCentroidsThread(clusters)
        }
        return clusters
    }

    fun doKmeansSerial():MutableList<MutableList<Point>>{
        var centroids = mutableListOf<Point>()
        for (i in 0 until k){
            centroids.add(points[Random.nextInt(points.size)])
        }
        var clusters = initializeClustersSerial(points,centroids)
        var newCentroids = createNewCentroidsSerial(clusters)
        while (checkCentroidChange(centroids,newCentroids)){
            centroids = newCentroids
            clusters = initializeClustersSerial(points,centroids)
            newCentroids = createNewCentroidsSerial(clusters)
        }
        return clusters
    }
}