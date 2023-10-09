package com.milkcocoa.info.clk.util.unit

object Size {
    fun Int.KB() = this.times(1000)
    fun Int.MB() = this.KB().KB()
    fun Int.GB() = this.MB().KB()

    fun Long.KB() = this.times(1000)
    fun Long.MB() = this.KB().KB()
    fun Long.GB() = this.MB().KB()

    fun Double.KB() = this.times(1000)
    fun Double.MB() = this.KB().KB()
    fun Double.GB() = this.MB().KB()


    fun Int.KiB() = this.times(1024)
    fun Int.MiB() = this.KiB().KiB()
    fun Int.GiB() = this.MiB().KiB()

    fun Long.KiB() = this.times(1024)
    fun Long.MiB() = this.KiB().KiB()
    fun Long.GiB() = this.MiB().KiB()

    fun Double.KiB() = this.times(1024)
    fun Double.MiB() = this.KiB().KiB()
    fun Double.GiB() = this.MiB().KiB()
}