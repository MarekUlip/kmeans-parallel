import java.lang.StringBuilder

class Point(var point: MutableList<Double>, val dimension: Int) {
    override fun toString(): String {
        var string = StringBuilder("(")
        for(value in point){
            string.append(value.toString()).append(", ")
        }
        string.removeRange(string.length-3,string.length-1)
        string.append(")")
        return string.toString()
    }
}