package com.recipefy.recipe.model.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "tags")
@EqualsAndHashCode(callSuper = true, exclude = "tags")
@Entity
public class RecipeMetadata extends BaseEntity {
    @Column(updatable = false, nullable = false)
    private UUID userId;

    @Column(updatable = false)
    private Long forkedFrom;

    private String title;
    private String description;
    private byte[] thumbnail;
    private Integer servingSize;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "recipe_tags",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<RecipeTag> tags = new HashSet<>();
}
