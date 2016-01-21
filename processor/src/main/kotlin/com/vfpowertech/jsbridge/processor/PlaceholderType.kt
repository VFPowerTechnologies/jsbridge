package com.vfpowertech.jsbridge.processor

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVisitor

/**
 * Represents a yet-to-be type. Used for to-be-generated types.
 */
class PlaceholderType(private val fqn: String) : TypeMirror {
    override fun getKind(): TypeKind {
        return TypeKind.DECLARED
    }

    override fun toString(): String {
        return fqn
    }

    override fun <R : Any, P : Any> accept(v: TypeVisitor<R, P>, p: P): R {
        throw UnsupportedOperationException()
    }

    override fun <A : Annotation> getAnnotationsByType(annotationType: Class<A>): Array<out A> {
        throw UnsupportedOperationException()
    }

    override fun <A : Annotation> getAnnotation(annotationType: Class<A>): A {
        throw UnsupportedOperationException()
    }

    override fun getAnnotationMirrors(): MutableList<out AnnotationMirror> {
        throw UnsupportedOperationException()
    }
}