package com.recipefy.recipe.mappers;

import com.recipefy.recipe.mapper.dto.RecipeMetadataDTOMapper;
import com.recipefy.recipe.model.dto.RecipeMetadataDTO;
import com.recipefy.recipe.model.entity.RecipeMetadata;
import com.recipefy.recipe.model.entity.RecipeTag;
import com.recipefy.recipe.model.enums.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class RecipeMetadataDTOMapperTests {

    @Test
    void toDTO_shouldMapCorrectly() {
        String tag1Name = Tag.VEGAN.name().toLowerCase(Locale.ROOT);
        String tag2Name = Tag.GLUTEN_FREE.name().toLowerCase(Locale.ROOT);
        RecipeTag tag1 = new RecipeTag(tag1Name, new HashSet<>());
        RecipeTag tag2 = new RecipeTag(tag2Name, new HashSet<>());

        RecipeMetadata recipe = new RecipeMetadata();
        recipe.setId(100L);
        recipe.setUserId(200L);
        recipe.setForkedFrom(300L);
        recipe.setTitle("Test Recipe");
        recipe.setDescription("Test Desc");
        recipe.setThumbnail("http://img.com/test.jpg");
        recipe.setCreatedAt(LocalDateTime.now().minusDays(1));
        recipe.setUpdatedAt(LocalDateTime.now());
        recipe.setTags(Set.of(tag1, tag2));

        RecipeMetadataDTO dto = RecipeMetadataDTOMapper.toDTO(recipe);

        assertThat(dto.getId()).isEqualTo(recipe.getId());
        assertThat(dto.getUserId()).isEqualTo(recipe.getUserId());
        assertThat(dto.getForkedFrom()).isEqualTo(recipe.getForkedFrom());
        assertThat(dto.getTitle()).isEqualTo(recipe.getTitle());
        assertThat(dto.getDescription()).isEqualTo(recipe.getDescription());
        assertThat(dto.getThumbnail().getUrl()).isEqualTo(recipe.getThumbnail());
        assertThat(dto.getTags()).hasSize(2);
        assertThat(dto.getTags()).extracting("name").containsExactlyInAnyOrder(tag1Name, tag2Name);
    }
}