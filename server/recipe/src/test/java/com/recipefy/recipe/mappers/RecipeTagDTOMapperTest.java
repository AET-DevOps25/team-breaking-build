package com.recipefy.recipe.mappers;

import com.recipefy.recipe.mapper.dto.RecipeTagDTOMapper;
import com.recipefy.recipe.model.dto.RecipeTagDTO;
import com.recipefy.recipe.model.entity.RecipeTag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RecipeTagDTOMapperTest {

    @Test
    void toDTO_shouldMapSetToListCorrectly() {
        Long tag1Id = 1L;
        Long tag2Id = 1L;
        String tag1Name = "vegan";
        String tag2Name = "high_protein";
        RecipeTag tag1 = new RecipeTag(tag1Name, new HashSet<>());
        tag1.setId(tag1Id);
        RecipeTag tag2 = new RecipeTag(tag2Name, new HashSet<>());
        tag2.setId(tag2Id);

        List<RecipeTagDTO> dtos = RecipeTagDTOMapper.toDTO(Set.of(tag1, tag2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos).extracting("id").containsExactlyInAnyOrder(tag1Id, tag2Id);
        assertThat(dtos).extracting("name").containsExactlyInAnyOrder(tag1Name, tag2Name);
    }

    @Test
    void toDTO_shouldMapCorrectly() {
        Long tagId = 1L;
        String tagName = "low_carb";
        RecipeTag tag = new RecipeTag(tagName, new HashSet<>());
        tag.setId(tagId);

        RecipeTagDTO dto = RecipeTagDTOMapper.toDTO(tag);

        assertThat(dto.getId()).isEqualTo(tagId);
        assertThat(dto.getName()).isEqualTo(tagName);
    }
}
