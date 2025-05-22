package com.recipefy.recipe.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "recipes")
@EqualsAndHashCode(callSuper = true)
@Entity
public class RecipeTag extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String name;

    @JsonIgnore
    @ManyToMany(mappedBy = "tags")
    private Set<RecipeMetadata> recipes = new HashSet<>();
}
