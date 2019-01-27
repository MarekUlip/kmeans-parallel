import kotlin.random.Random

class KMeans(val points: List<Point>, val k: Int, val dimension: Int, val minChange: Double, val numOfThreads: Int) {
    private fun euklideanDistance(a:Point, b:Point):Double{
        var sum = 0.0
        for (i in 0 until a.dimension){
            sum+=Math.pow(a.point[i]-b.point[i],2.0)
        }
        return Math.sqrt(sum)
    }

    /**
     * Returns index of closest centroid based on euklidean distance
     */
    private fun getClosestCentroidIndex(centroids: MutableList<Point>, point: Point):Int{
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

    /**
     * Counts new centroid point based on average from points contained in provided cluster
     */
    private fun countNewCentroidForCluster(cluster: MutableList<Point>):Point{
        val newCentroid = Point(MutableList(dimension){0.0}, dimension)
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


    /**
     * Parallel creates new centroid for all clusters
     */
    private fun createNewCentroidsParallel(clusters: MutableList<MutableList<Point>>):MutableList<Point>{
        val centroids = mutableListOf<Point>()
        val threads = mutableListOf<Thread>()
        val chunkSize = (k / numOfThreads).toInt()
        if (chunkSize == 0){
            // No need to make some threads count more centroids
            // each thread will count one
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
            //Some threads may have to count more than one centroid
            for (i in 0 until numOfThreads){
                threads.add(Thread{
                    val p = i
                    val end = if (i==numOfThreads-1){
                        //make sure that last thread counts the rest of centroids
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

    private fun createNewCentroidsSerial(clusters: MutableList<MutableList<Point>>):MutableList<Point>{
        val centroids = mutableListOf<Point>()
        for (cluster in clusters){
            centroids.add(countNewCentroidForCluster(cluster))
        }
        return centroids
    }


    /**
     * Parallel assigns each point into cluster based on closest centroid
     */
    private fun initializeClustersParallel(points: List<Point>, centroids: MutableList<Point>): MutableList<MutableList<Point>>{
        val clustersRes = MutableList(centroids.size){ mutableListOf<Point>()}
        val threads = mutableListOf<Thread>()
        val chunkSize = points.size/numOfThreads
        for (i in 0 until numOfThreads){
            threads.add(Thread {
                //Create point assignment holder
                val clusters = MutableList(centroids.size){ mutableListOf<Point>()}
                val p = i
                val end = if (p==numOfThreads-1){
                    points.size
                } else {
                    chunkSize*(p+1)
                }
                for (j in chunkSize*p until end){
                    val centroidIndex = getClosestCentroidIndex(centroids, points[j])
                    clusters[centroidIndex].add(points[j])
                }
                synchronized(clustersRes){
                    //After all points were assigned synchronously add them to new clusters array
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

    private fun initializeClustersSerial(points: List<Point>,centroids: MutableList<Point>): MutableList<MutableList<Point>>{
        val clusters = MutableList(centroids.size){ mutableListOf<Point>()}
        for (point in points){
            clusters[getClosestCentroidIndex(centroids,point)].add(point)
        }
        return clusters
    }

    /**
     * Checks how much has centroid position changed since last iteration
     */
    private fun checkCentroidChange(centroids: MutableList<Point>, newCentroids: MutableList<Point>):Boolean{
        var centroid_change = 0.0
        for ((index, centroid) in centroids.withIndex()){
            centroid_change += euklideanDistance(centroid, newCentroids[index])
        }
        return centroid_change>minChange
    }


    fun doKmeansParallel(): MutableList<MutableList<Point>>{
        var centroids = mutableListOf<Point>()
        //Array that ensures that no point becomes centroid twice
        val occured = mutableListOf<Int>()
        for (i in 0 until k){
            var randNum = Random.nextInt(points.size)
            while (occured.contains(randNum)){
                randNum = Random.nextInt(points.size)
            }
            occured.add(randNum)
            centroids.add(points[randNum])
        }
        var clusters = initializeClustersParallel(points,centroids)
        var newCentroids = createNewCentroidsParallel(clusters)
        while (checkCentroidChange(centroids,newCentroids)){
            centroids = newCentroids
            clusters = initializeClustersParallel(points,centroids)
            newCentroids = createNewCentroidsParallel(clusters)
        }
        return clusters
    }

    fun doKmeansSerial():MutableList<MutableList<Point>>{
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