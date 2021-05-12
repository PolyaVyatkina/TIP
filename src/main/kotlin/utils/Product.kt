package utils


interface Product {
    object LastElement

    operator fun component1(): Any? = LastElement
    operator fun component2(): Any? = LastElement
    operator fun component3(): Any? = LastElement
    operator fun component4(): Any? = LastElement
    operator fun component5(): Any? = LastElement
    operator fun component6(): Any? = LastElement
    operator fun component7(): Any? = LastElement
}

fun Product.productIterator() = iterator {
    val element1 = component1()
    if (element1 !== Product.LastElement) yield(element1)
    else return@iterator
    val element2 = component2()
    if (element2 !== Product.LastElement) yield(element2)
    else return@iterator
    val element3 = component3()
    if (element3 !== Product.LastElement) yield(element3)
    else return@iterator
    val element4 = component4()
    if (element4 !== Product.LastElement) yield(element4)
    else return@iterator
    val element5 = component5()
    if (element5 !== Product.LastElement) yield(element5)
    else return@iterator
    val element6 = component6()
    if (element6 !== Product.LastElement) yield(element6)
    else return@iterator
    val element7 = component7()
    if (element7 !== Product.LastElement) yield(element7)
    else return@iterator
}
