package com.recipefy.recipe.model.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "tags")
@EqualsAndHashCode(callSuper = true)
@Entity
public class RecipeMetadata extends BaseEntity {
    @Column(updatable = false, nullable = false)
    private Long userId;

    @Column(updatable = false)
    private Long forkedFrom;

    private String title;
    private String description;
    private String thumbnail;
    private Integer servingSize;

    @ManyToMany
    @JoinTable(
            name = "recipe_tags",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<RecipeTag> tags = new HashSet<>();
}
