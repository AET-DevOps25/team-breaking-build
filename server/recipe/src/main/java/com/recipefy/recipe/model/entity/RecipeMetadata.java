package com.recipefy.recipe.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
