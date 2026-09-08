package com.example.compositionhelper.model

/** Terminal point of the six Fibonacci arcs used by CompositionDrawing, including letterboxing. */
object SpiralGeometry {
    fun terminal(width: Float, height: Float, orientation: Int): PointF {
        require(width > 0 && height > 0)
        val portrait=height>width
        val w=if(portrait) 8f else 13f; val h=if(portrait) 13f else 8f
        val scale=minOf(width/w,height/h)
        var x=if(portrait) 2f else 10f; var y=if(portrait) 10f else 6f
        if(orientation==1 || orientation==3) x=w-x
        if(orientation==2 || orientation==3) y=h-y
        return PointF(((width-w*scale)/2+x*scale)/width,((height-h*scale)/2+y*scale)/height)
    }
}
