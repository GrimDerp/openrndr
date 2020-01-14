package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.math.Vector3

enum class CubemapSide(val forward: Vector3, val up: Vector3) {
    POSITIVE_X(Vector3.UNIT_X, -Vector3.UNIT_Y),
    NEGATIVE_X(-Vector3.UNIT_X, -Vector3.UNIT_Y),
    POSITIVE_Y(Vector3.UNIT_Y, Vector3.UNIT_Z),
    NEGATIVE_Y(-Vector3.UNIT_Y, -Vector3.UNIT_Z),
    POSITIVE_Z(Vector3.UNIT_Z, -Vector3.UNIT_Y),
    NEGATIVE_Z(-Vector3.UNIT_Z, -Vector3.UNIT_Y)
}

interface Cubemap {

    companion object {
        fun create(width: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, session: Session? = Session.active): Cubemap {
            val cubemap = Driver.instance.createCubemap(width, format, type)
            session?.track(cubemap)
            return cubemap
        }

        fun fromUrl(url: String, session: Session?): Cubemap {
            val cubemap = Driver.instance.createCubemapFromUrls(listOf(url))
            session?.track(cubemap)
            return cubemap
        }

        fun fromUrls(urls: List<String>, session: Session?): Cubemap {
            return Driver.instance.createCubemapFromUrls(urls)
        }
    }
    val session: Session?

    val width: Int
    val format: ColorFormat
    val type: ColorType

    fun filter(min: MinifyingFilter, mag: MagnifyingFilter)
    fun side(side: CubemapSide): ColorBuffer
    fun bind(textureUnit: Int = 0)
    fun generateMipmaps()
    fun destroy()
}

fun cubemap(width: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.UINT8, session: Session?): Cubemap {
    val cubemap = Cubemap.create(width, format, type)
    session?.track(cubemap)
    return cubemap
}