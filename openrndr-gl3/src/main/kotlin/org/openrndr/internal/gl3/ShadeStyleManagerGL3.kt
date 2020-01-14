package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*

private val logger = KotlinLogging.logger {}

class ShadeStyleManagerGL3(
        val vertexShaderGenerator: (ShadeStructure) -> String,
        val fragmentShaderGenerator: (ShadeStructure) -> String) : ShadeStyleManager() {

    private var defaultShader: Shader? = null
    private val shaders = mutableMapOf<ShadeStructure, Shader>()

    override fun shader(style: ShadeStyle?, vertexFormats: List<VertexFormat>, inputInstanceFormats: List<VertexFormat>): Shader {
        val instanceFormats = inputInstanceFormats + (style?.attributes
                ?: emptyList<VertexBuffer>()).map { it.vertexFormat }

        if (style == null) {
            if (defaultShader == null) {
                logger.debug { "creating default shader" }
                val structure = structureFromShadeStyle(style, vertexFormats, instanceFormats)
                defaultShader = Shader.createFromCode(vertexShaderGenerator(structure), fragmentShaderGenerator(structure), Session.root)
                (defaultShader as ShaderGL3).userShader = false
            }
            return defaultShader!!
        } else {
            val structure = structureFromShadeStyle(style, vertexFormats, instanceFormats)
            val shader = shaders.getOrPut(structure) {
                try {
                    Shader.createFromCode(vertexShaderGenerator(structure), fragmentShaderGenerator(structure), Session.root)
                } catch (e: Throwable) {
                    if (System.getProperties().containsKey("org.openrndr.ignoreShadeStyleErrors")) {
                        shader(null, vertexFormats, instanceFormats)
                    } else {
                        throw e
                    }
                }
            }
            (shader as ShaderGL3).userShader = false

            shader.begin()
            var textureIndex = 2
            style.parameterValues.entries.forEach {
                when (val value = it.value) {
                    is Int -> shader.uniform("p_${it.key}", value)
                    is Float -> shader.uniform("p_${it.key}", value)
                    is Double -> shader.uniform("p_${it.key}", value)
                    is Matrix44 -> shader.uniform("p_${it.key}", value)
                    is Matrix33 -> shader.uniform("p_${it.key}", value)
                    is Vector4 -> shader.uniform("p_${it.key}", value)
                    is Vector3 -> shader.uniform("p_${it.key}", value)
                    is Vector2 -> shader.uniform("p_${it.key}", value)
                    is ColorRGBa -> shader.uniform("p_${it.key}", value)
                    is ColorBuffer -> {
                        value.bind(textureIndex)
                        shader.uniform("p_${it.key}", textureIndex)
                        textureIndex++
                    }
                    is DepthBuffer -> {
                        value.bind(textureIndex)
                        shader.uniform("p_${it.key}", textureIndex)
                        textureIndex++
                    }
                    is BufferTexture -> {
                        value.bind(textureIndex)
                        shader.uniform("p_${it.key}", textureIndex)
                        textureIndex++
                    }
                    is Cubemap -> {
                        value.bind(textureIndex)
                        shader.uniform("p_${it.key}", textureIndex)
                        textureIndex++
                    }
                    is ArrayTexture -> {
                        value.bind(textureIndex)
                        shader.uniform("p_${it.key}", textureIndex)
                        textureIndex++
                    }
                    else -> {
                        throw RuntimeException("unsupported value type")
                    }
                }
            }
            return shader
        }
    }
}