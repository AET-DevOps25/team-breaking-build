package com.recipefy.version.repository.mongo;

import com.recipefy.version.model.mongo.RecipeSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecipeSnapshotRepository extends MongoRepository<RecipeSnapshot, String> {

}
