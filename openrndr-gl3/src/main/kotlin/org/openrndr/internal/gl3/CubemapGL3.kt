package org.openrndr.internal.gl3

import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL33C.*
import org.openrndr.draw.*
import org.openrndr.internal.gl3.dds.loadDDS
import java.net.URL
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.math.pow

class CubemapGL3(val texture: Int, override val width: Int, val sides: List<ColorBuffer>, override val type: ColorType, override val format: ColorFormat, override val session: Session?) : Cubemap {


    private var destroyed = false

    companion object {
        fun create(width: Int, format: ColorFormat, type: ColorType, session: Session?): CubemapGL3 {
            val textures = IntArray(1)
            glGenTextures(textures)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_CUBE_MAP, textures[0])

            val effectiveWidth = width
            val effectiveHeight = width
            val internalFormat = internalFormat(format, type)
            val sides = mutableListOf<ColorBufferGL3>()
            for (i in 0..5) {
                val nullBB: ByteBuffer? = null
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, internalFormat, effectiveWidth, effectiveHeight, 0, format.glFormat(), type.glType(), nullBB)
                sides.add(ColorBufferGL3(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, textures[0], width, width, 1.0, format, type, BufferMultisample.Disabled, session))
            }
            return CubemapGL3(textures[0], width, sides, type, format, session)
        }

        fun fromUrl(url: String, session: Session?): CubemapGL3 {
            glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS)
            if (url.endsWith(".dds")) {
                val textures = IntArray(1)
                glGenTextures(textures)
                checkGLErrors()
                glActiveTexture(GL_TEXTURE0)
                checkGLErrors()
                glBindTexture(GL_TEXTURE_CUBE_MAP, textures[0])
                checkGLErrors()

                val data = loadDDS(URL(url).openStream())
                val sides = (0..5).map { ColorBufferGL3(GL_TEXTURE_CUBE_MAP_POSITIVE_X + it, textures[0], data.width, data.height, 1.0, ColorFormat.RGB, ColorType.UINT8, BufferMultisample.Disabled, session) }
                for (level in 0 until data.mipmaps) {

                    val m = 2.0.pow(-level * 1.0)
                    val width = (data.width * m).toInt()
                    val height = (data.height * m).toInt()

                    (data.sidePX(level) as Buffer).rewind()
                    (data.sideNX(level) as Buffer).rewind()
                    (data.sidePY(level) as Buffer).rewind()
                    (data.sideNY(level) as Buffer).rewind()
                    (data.sidePZ(level) as Buffer).rewind()
                    (data.sideNZ(level) as Buffer).rewind()

                    if (data.type == ColorType.DXT1 || data.type == ColorType.DXT3 || data.type == ColorType.DXT5) {
                        val format = internalFormat(data.format, data.type)
                        glCompressedTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, level, format, data.width, data.height, 0, data.sidePX(level))
                        checkGLErrors()
                        glCompressedTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, level, format, data.width, data.height, 0, data.sideNX(level))
                        checkGLErrors()
                        glCompressedTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, level, format, data.width, data.height, 0, data.sidePY(level))
                        checkGLErrors()
                        glCompressedTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, level, format, data.width, data.height, 0, data.sideNY(level))
                        checkGLErrors()
                        glCompressedTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, level, format, data.width, data.height, 0, data.sidePZ(level))
                        checkGLErrors()
                        glCompressedTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, level, format, data.width, data.height, 0, data.sideNZ(level))
                    } else {
                        val format = data.format.glFormat()
                        val type = data.type.glType()
                        val internalFormat = internalFormat(data.format, data.type)
                        glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, level, internalFormat, width, height, 0, format, type, data.sidePX(level))
                        checkGLErrors()
                        glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, level, internalFormat, width, height, 0, format, type, data.sideNX(level))
                        checkGLErrors()
                        glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, level, internalFormat, width, height, 0, format, type, data.sidePY(level))
                        checkGLErrors()
                        glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, level, internalFormat, width, height, 0, format, type, data.sideNY(level))
                        checkGLErrors()
                        glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, level, internalFormat, width, height, 0, format, type, data.sidePZ(level))
                        checkGLErrors()
                        glTexImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, level, internalFormat, width, height, 0, format, type, data.sideNZ(level))
                    }
                    checkGLErrors()
                }
                if (data.mipmaps == 1) {
                    glGenerateMipmap(GL_TEXTURE_CUBE_MAP)
                    checkGLErrors()
                }
                return CubemapGL3(textures[0], data.width, sides, data.type, data.format, session)
            } else {
                throw RuntimeException("only dds files can be loaded through a single url")
            }
        }

        fun fromUrls(urls: List<String>, session: Session?): CubemapGL3 {
            if (urls.size != 6) {
                throw RuntimeException("6 urls are needed for a cubemap")
            }
            val textures = IntArray(1)
            glGenTextures(textures)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_CUBE_MAP, textures[0])
            val sides = mutableListOf<ColorBufferGL3>()

            urls.forEachIndexed { index, it ->
                val data = ColorBufferDataGL3.fromUrl(it)
                val internalFormat = internalFormat(data.format, data.type)
                val nullBB: ByteBuffer? = null

                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + index, 0, internalFormat, data.width, data.height, 0, data.format.glFormat(), data.type.glType(), nullBB)
                sides.add(ColorBufferGL3(GL_TEXTURE_CUBE_MAP_POSITIVE_X + index, textures[0], data.width, data.width, 1.0, data.format, data.type, BufferMultisample.Disabled, session))
            }
            return CubemapGL3(textures[0], sides[0].width, sides, sides[0].type, sides[0].format, session)
        }
    }

    internal fun format() : Int {
        return internalFormat(format, type)
    }

    override fun generateMipmaps() {
        bound {
            glGenerateMipmap(GL_TEXTURE_CUBE_MAP)
        }
    }

    override fun filter(min: MinifyingFilter, mag: MagnifyingFilter) {
        bound {
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, min.toGLFilter())
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, mag.toGLFilter())
        }
    }

    override fun side(side: CubemapSide): ColorBuffer {
        return sides[side.ordinal]
    }

    override fun bind(textureUnit: Int) {
        if (!destroyed) {
            glActiveTexture(GL_TEXTURE0 + textureUnit)
            glBindTexture(GL_TEXTURE_CUBE_MAP, texture)
        } else {
            throw IllegalStateException("attempting to bind destroyed cubemap")
        }
    }

    override fun destroy() {
        if (!destroyed) {
            GL11C.glDeleteTextures(texture)
            Session.active.untrack(this)
            destroyed = true
        }
    }

    private fun bound(f: CubemapGL3.() -> Unit) {
        glActiveTexture(GL_TEXTURE0)
        val current = glGetInteger(GL_TEXTURE_BINDING_CUBE_MAP)
        glBindTexture(GL_TEXTURE_CUBE_MAP, texture)
        this.f()
        glBindTexture(GL_TEXTURE_CUBE_MAP, current)
    }
}