package com.examples

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import com.challenge.FieldValidationException

@Entity
@Table(name = "categories")
class Category(
    @Column(nullable = false)
    @NotBlank
    val name: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Category) return false

        if (name != other.name) return false

        return true
    }

    fun isNameOk() = throw FieldValidationException("name", "Name '${name}' is duplicated")
    fun isIdOk() = throw FieldValidationException("id", "Id '${id}' is duplicated")

    override fun hashCode(): Int = name.hashCode()
}
